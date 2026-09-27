package de.tehwolf.yaft.playground;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * The check this repository exists for: de.tehwolf:yaft, as Maven Central
 * serves it, inside a Spring Boot application, against a real YaFT backend
 * seeded by scripts/seed.sh.
 *
 * <p>The library's own suite runs against a local HTTP stub and the
 * conformance suite against case data; neither can see the library and the
 * backend -- or the library and Spring -- disagree.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        // Refreshes are triggered by the tests, so a scheduled one cannot race them.
        properties = "yaft.refresh-interval=1h")
class PlaygroundE2ETest {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};

    @LocalServerPort
    private int port;

    @Value("${yaft.api-url}")
    private URI apiUrl;

    @Value("${yaft.group}")
    private String group;

    @Value("${yaft.secret}")
    private String secret;

    private RestClient app;
    private RestClient backend;

    @BeforeEach
    void clients() {
        app = RestClient.create("http://localhost:" + port);
        backend = RestClient.create(apiUrl.toString());
    }

    @Test
    void everySeededToggleAnswersAsExpected() {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("alwaysOn", true);
        expected.put("alwaysOff", false);
        // The bounds are evaluated by the library itself and must agree with
        // what the backend stored.
        expected.put("notYetActive", false);
        expected.put("alreadyDisabled", false);
        expected.put("insideWindow", true);
        expected.put("outsideWindow", false);
        expected.put("noSuchToggle", false);

        assertEquals(expected, app.get().uri("/toggles").retrieve().body(MAP));
    }

    @Test
    void toggledBeansFollowTheirToggles() {
        Map<String, Object> demo = app.get().uri("/demo").retrieve().body(MAP);

        assertEquals("new", demo.get("checkout"), "insideWindow is on");
        // alwaysOff is off: the fallback runs, on the same instance, reading its field.
        assertEquals("Hello playground!", demo.get("greeting"));
    }

    /**
     * R14 and R15 against the real backend: after both toggles flip, the
     * method follows and the class does not, because the class was decided
     * when its bean was created.
     */
    @Test
    void aMethodFollowsTheBackendAndAClassDoesNot() {
        try {
            put("activate", "alwaysOff");
            put("deactivate", "insideWindow");
            app.post().uri("/refresh").retrieve().toBodilessEntity();

            Map<String, Object> toggles = app.get().uri("/toggles").retrieve().body(MAP);
            assertEquals(true, toggles.get("alwaysOff"));
            assertEquals(false, toggles.get("insideWindow"));

            Map<String, Object> demo = app.get().uri("/demo").retrieve().body(MAP);
            assertEquals("Welcome aboard, playground!", demo.get("greeting"), "method: evaluated per call");
            assertEquals("new", demo.get("checkout"), "class: decided once, at startup");
        } finally {
            put("deactivate", "alwaysOff");
            put("activate", "insideWindow");
            app.post().uri("/refresh").retrieve().toBodilessEntity();
        }
    }

    /**
     * Flips a toggle in the backend. Retries on 429: yaft.tehwolf.de allows
     * five writes a minute, and e2e-live.yml runs this suite against it.
     */
    private void put(String action, String name) {
        for (int attempt = 1; ; attempt++) {
            try {
                backend.put()
                        .uri("/features/{action}/{key}/{secret}", action, group + "|" + name, secret)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == 6) throw e;
                sleep(Duration.ofSeconds(15));
            }
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
