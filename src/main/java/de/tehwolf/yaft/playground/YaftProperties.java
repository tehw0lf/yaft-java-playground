package de.tehwolf.yaft.playground;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Where the toggles come from. scripts/seed.sh writes api-url and group to
 * seed.properties, which application.properties imports.
 *
 * @param apiUrl the YaFT backend
 * @param group the toggle group's UUID
 * @param refreshInterval how often to ask the backend whether the group changed
 */
@ConfigurationProperties("yaft")
public record YaftProperties(URI apiUrl, String group, @DefaultValue("30s") Duration refreshInterval) {}
