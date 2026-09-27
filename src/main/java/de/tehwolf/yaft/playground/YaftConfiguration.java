package de.tehwolf.yaft.playground;

import de.tehwolf.yaft.ApiFeatureProvider;
import de.tehwolf.yaft.YaFT;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.json.JsonMapper;

/**
 * Wires YaFT into Spring.
 *
 * <p>The provider parses with the application's own Jackson {@link JsonMapper}
 * -- the one Spring Boot already configured -- so the library brings no JSON
 * dependency of its own. It is loaded before any toggled bean is created:
 * {@link YaFT#create} decides a class once, at creation, and must not see an
 * empty provider.
 */
@Configuration(proxyBeanMethods = false)
public class YaftConfiguration {

    @Bean
    ApiFeatureProvider featureProvider(YaftProperties properties, JsonMapper json)
            throws IOException, InterruptedException {
        ApiFeatureProvider provider = ApiFeatureProvider.builder(
                        properties.apiUrl(), properties.group(), body -> json.readValue(body, Object.class))
                .build();
        // Fail the start rather than run with every toggle off.
        provider.refresh();
        YaFT.setProvider(provider);
        return provider;
    }

    /** Keeps the provider current; a failed refresh keeps the previous data. */
    @Bean
    Refresher refresher(ApiFeatureProvider provider) {
        return new Refresher(provider);
    }

    /** Separate from the configuration class so scheduling does not need a proxy of it. */
    static final class Refresher {

        private final ApiFeatureProvider provider;

        Refresher(ApiFeatureProvider provider) {
            this.provider = provider;
        }

        @Scheduled(fixedDelayString = "${yaft.refresh-interval:30s}")
        void refresh() {
            provider.refreshQuietly();
        }
    }
}
