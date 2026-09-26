package de.tehwolf.yaft.playground;

import de.tehwolf.yaft.FeatureToggle;

/**
 * A class toggle: which checkout implementation serves the application is
 * decided once, when the bean is created (R14).
 */
public interface Checkout {

    /** @return which implementation answered */
    String flow();

    /** On while the seeded "insideWindow" toggle is inside its time window. */
    @FeatureToggle(key = "insideWindow", fallback = ClassicCheckout.class)
    final class NewCheckout implements Checkout {
        @Override
        public String flow() {
            return "new";
        }
    }

    /** What serves while the toggle is off. */
    final class ClassicCheckout implements Checkout {
        @Override
        public String flow() {
            return "classic";
        }
    }
}
