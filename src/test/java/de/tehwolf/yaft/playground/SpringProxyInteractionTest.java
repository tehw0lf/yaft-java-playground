package de.tehwolf.yaft.playground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.tehwolf.yaft.FeatureProvider;
import de.tehwolf.yaft.FeatureToggle;
import de.tehwolf.yaft.YaFT;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Whether YaFT's JDK proxies get along with Spring's own proxies, in both
 * directions. No backend needed; a local provider stands in.
 */
class SpringProxyInteractionTest {

    interface Service {
        String run(String input);
    }

    /** A toggled class, as an application would write it. */
    static class ToggledService implements Service {

        private final String suffix = "!";

        @Override
        @FeatureToggle(key = "fancy", fallbackMethod = "plain")
        public String run(String input) {
            return "fancy " + input + suffix;
        }

        String plain(String input) {
            return "plain " + input + suffix;
        }
    }

    /** Stands for any Spring proxy: @Transactional, @Cacheable, security, logging. */
    @Aspect
    static class Recording {

        final List<String> calls = new ArrayList<>();

        @Around("execution(* de.tehwolf.yaft.playground.SpringProxyInteractionTest.Service.run(..))")
        Object record(ProceedingJoinPoint call) throws Throwable {
            calls.add("run");
            return call.proceed();
        }
    }

    private FeatureProvider saved;
    private final Map<String, Boolean> toggles = new HashMap<>();

    @BeforeEach
    void provider() {
        saved = YaFT.provider().orElse(null);
        YaFT.setProvider(key -> Boolean.TRUE.equals(toggles.get(key)));
    }

    @AfterEach
    void restore() {
        YaFT.setProvider(saved);
    }

    /** A: YaFT wraps a plain object; Spring then advises the resulting bean. */
    @Nested
    class SpringAroundYaft {

        @Configuration(proxyBeanMethods = false)
        @EnableAspectJAutoProxy(proxyTargetClass = true)
        static class Config {
            @Bean
            Recording recording() {
                return new Recording();
            }

            @Bean
            Service service() {
                return YaFT.wrap(Service.class, new ToggledService());
            }
        }

        @Test
        void theAspectRunsAndTheToggleStillSwitches() {
            try (var context = new AnnotationConfigApplicationContext(Config.class)) {
                Service service = context.getBean(Service.class);
                Recording recording = context.getBean(Recording.class);

                assertTrue(AopUtils.isAopProxy(service), "Spring advised the YaFT proxy");
                assertEquals("plain a!", service.run("a"));
                toggles.put("fancy", true);
                assertEquals("fancy a!", service.run("a"));
                assertEquals(List.of("run", "run"), recording.calls);
            }
        }
    }

    /**
     * B: Spring proxies the bean first and the application then wraps that
     * proxy. Up to yaft 0.2.2 every toggle was then silently ignored -- the
     * proxy class carries none of the annotations -- and even a found
     * annotation would have run the fallback on the proxy instance, with empty
     * fields. Since 0.2.3 YaFT refuses it and points at A.
     */
    @Nested
    class YaftAroundSpring {

        @Configuration(proxyBeanMethods = false)
        @EnableAspectJAutoProxy(proxyTargetClass = true)
        static class CglibConfig {
            @Bean
            Recording recording() {
                return new Recording();
            }

            @Bean
            ToggledService toggledService() {
                return new ToggledService();
            }
        }

        @Configuration(proxyBeanMethods = false)
        @EnableAspectJAutoProxy(proxyTargetClass = false)
        static class JdkConfig {
            @Bean
            Recording recording() {
                return new Recording();
            }

            @Bean
            Service toggledService() {
                return new ToggledService();
            }
        }

        @Test
        void aCglibProxiedBeanIsRefused() {
            try (var context = new AnnotationConfigApplicationContext(CglibConfig.class)) {
                Service springProxy = context.getBean(ToggledService.class);
                assertTrue(AopUtils.isCglibProxy(springProxy));
                assertRefusedPointingAtTheFix(springProxy, "hidden by");
            }
        }

        @Test
        void aJdkProxiedBeanIsRefused() {
            try (var context = new AnnotationConfigApplicationContext(JdkConfig.class)) {
                Service springProxy = context.getBean(Service.class);
                assertTrue(AopUtils.isJdkDynamicProxy(springProxy));
                assertRefusedPointingAtTheFix(springProxy, "JDK proxy");
            }
        }

        private void assertRefusedPointingAtTheFix(Service springProxy, String cause) {
            IllegalArgumentException error =
                    assertThrows(IllegalArgumentException.class, () -> YaFT.wrap(Service.class, springProxy));
            assertTrue(error.getMessage().contains(cause), error.getMessage());
            assertTrue(error.getMessage().contains("call YaFT.wrap in the @Bean method"), error.getMessage());
        }
    }
}
