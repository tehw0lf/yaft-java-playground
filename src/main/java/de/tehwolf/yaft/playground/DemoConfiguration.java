package de.tehwolf.yaft.playground;

import de.tehwolf.yaft.ApiFeatureProvider;
import de.tehwolf.yaft.YaFT;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The toggled beans. The {@link ApiFeatureProvider} parameter is not used; it
 * makes Spring create and load the provider first.
 */
@Configuration(proxyBeanMethods = false)
public class DemoConfiguration {

    @Bean
    Checkout checkout(ApiFeatureProvider provider) {
        return YaFT.create(Checkout.class, Checkout.NewCheckout.class);
    }

    @Bean
    Greeter greeter(ApiFeatureProvider provider) {
        return YaFT.wrap(Greeter.class, new Greeter.ToggledGreeter());
    }
}
