package de.tehwolf.yaft.playground;

import de.tehwolf.yaft.FeatureToggle;

/** A method toggle: evaluated on every call (R15), so it follows the backend without a restart. */
public interface Greeter {

    /**
     * @param name who to greet
     * @return a greeting
     */
    String greet(String name);

    /** The fancy greeting is behind the seeded "alwaysOff" toggle. */
    final class ToggledGreeter implements Greeter {

        private final String punctuation = "!";

        @Override
        @FeatureToggle(key = "alwaysOff", fallbackMethod = "plain")
        public String greet(String name) {
            return "Welcome aboard, " + name + punctuation;
        }

        /** The fallback runs on the same instance, so it can read its fields (R19). */
        private String plain(String name) {
            return "Hello " + name + punctuation;
        }
    }
}
