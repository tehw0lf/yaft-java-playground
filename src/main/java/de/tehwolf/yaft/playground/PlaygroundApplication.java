package de.tehwolf.yaft.playground;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** A Spring Boot application using de.tehwolf:yaft from Maven Central against a real YaFT backend. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class PlaygroundApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlaygroundApplication.class, args);
    }
}
