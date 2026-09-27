package de.tehwolf.yaft.playground;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the toggles come from. scripts/seed.sh writes api-url and group to
 * seed.properties, which application.properties imports.
 *
 * @param apiUrl the YaFT backend
 * @param group the toggle group's UUID
 */
@ConfigurationProperties("yaft")
public record YaftProperties(URI apiUrl, String group) {}
