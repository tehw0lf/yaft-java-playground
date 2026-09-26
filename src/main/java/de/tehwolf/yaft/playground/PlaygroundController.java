package de.tehwolf.yaft.playground;

import de.tehwolf.yaft.ApiFeatureProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** What the e2e suite reads: raw toggle answers, and the toggled beans at work. */
@RestController
public class PlaygroundController {

    /** The toggles scripts/seed.sh creates, plus one that does not exist. */
    static final List<String> SEEDED = List.of(
            "alwaysOn", "alwaysOff", "notYetActive", "alreadyDisabled", "insideWindow", "outsideWindow", "noSuchToggle");

    private final ApiFeatureProvider provider;
    private final Checkout checkout;
    private final Greeter greeter;

    PlaygroundController(ApiFeatureProvider provider, Checkout checkout, Greeter greeter) {
        this.provider = provider;
        this.checkout = checkout;
        this.greeter = greeter;
    }

    /** Every seeded toggle by its bare name, as {@code @FeatureToggle} would see it. */
    @GetMapping("/toggles")
    Map<String, Boolean> toggles() {
        Map<String, Boolean> answers = new LinkedHashMap<>();
        SEEDED.forEach(name -> answers.put(name, provider.isEnabled(name)));
        return answers;
    }

    @GetMapping("/demo")
    Map<String, String> demo() {
        return Map.of("checkout", checkout.flow(), "greeting", greeter.greet("playground"));
    }

    /** Pulls the group now instead of waiting for the next scheduled refresh. */
    @PostMapping("/refresh")
    Map<String, Boolean> refresh() throws Exception {
        return Map.of("changed", provider.refresh());
    }
}
