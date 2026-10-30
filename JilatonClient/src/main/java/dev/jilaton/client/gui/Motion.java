package dev.jilaton.client.gui;

/**
 * Shared easing curves and motion scaling. Curves follow two timing profiles: an exponential
 * "settle" for entrances (fast response, long soft tail) and a gentler standard curve for
 * reversible state changes. All animation is driven by elapsed time, never frame count.
 */
public final class Motion {
    /** Entrance/settle curve, equivalent to cubic-bezier(0.16, 1, 0.3, 1). */
    public static final Easing SETTLE = t -> bezier(0.16, 1.0, 0.3, 1.0, t);
    /** Reversible state curve, equivalent to cubic-bezier(0.4, 0, 0.2, 1). */
    public static final Easing STANDARD = t -> bezier(0.4, 0.0, 0.2, 1.0, t);
    public static final Easing LINEAR = Motion::clamp01;

    private Motion() { }

    @FunctionalInterface
    public interface Easing {
        double apply(double t);
    }

    public static double clamp01(double value) {
        return value <= 0 ? 0 : value >= 1 ? 1 : value;
    }

    /** Scales a spatial offset (pixels, scale delta) by the configured animation intensity. */
    public static double amount(double value, double intensity, boolean reducedMotion) {
        return reducedMotion ? 0 : value * Math.max(0, Math.min(1.5, intensity));
    }

    /** Progress of an element that starts {@code delaySeconds} after {@code start} and lasts {@code duration}. */
    public static double staggered(long start, long now, double delaySeconds, double duration, Easing easing) {
        double elapsed = (now - start) / 1_000_000_000.0 - delaySeconds;
        return easing.apply(duration <= 0 ? 1 : elapsed / duration);
    }

    /** Evaluates a CSS-style cubic bezier with endpoints (0,0) and (1,1) at x = t. */
    public static double bezier(double x1, double y1, double x2, double y2, double t) {
        double x = clamp01(t);
        if (x == 0 || x == 1) return x;
        double u = x;
        for (int i = 0; i < 8; i++) {
            double error = sample(x1, x2, u) - x;
            if (Math.abs(error) < 1.0E-6) break;
            double slope = slope(x1, x2, u);
            if (Math.abs(slope) < 1.0E-6) break;
            u = clamp01(u - error / slope);
        }
        double low = 0, high = 1;
        for (int i = 0; i < 20 && Math.abs(sample(x1, x2, u) - x) > 1.0E-6; i++) {
            if (sample(x1, x2, u) < x) low = u; else high = u;
            u = (low + high) / 2;
        }
        return sample(y1, y2, u);
    }

    private static double sample(double a, double b, double u) {
        double inverse = 1 - u;
        return 3 * inverse * inverse * u * a + 3 * inverse * u * u * b + u * u * u;
    }

    private static double slope(double a, double b, double u) {
        double inverse = 1 - u;
        return 3 * inverse * inverse * a + 6 * inverse * u * (b - a) + 3 * u * u * (1 - b);
    }

    /**
     * A reversible 0..1 transition. Raw progress moves linearly toward its target at
     * 1/duration per second, so reversing mid-way never jumps; the eased value is derived from it.
     */
    public static final class Transition {
        private final double duration;
        private final Easing easing;
        private double progress;
        private long lastTime;
        private boolean initialized;

        public Transition(double durationSeconds, Easing easing) {
            if (!(durationSeconds > 0)) throw new IllegalArgumentException("Duration must be positive");
            this.duration = durationSeconds;
            this.easing = easing;
        }

        public double update(boolean target, long now, boolean immediate) {
            double goal = target ? 1 : 0;
            if (!initialized || immediate) {
                progress = immediate ? goal : progress;
                initialized = true;
            } else {
                double step = Math.max(0, Math.min(0.1, (now - lastTime) / 1_000_000_000.0)) / duration;
                progress = progress < goal ? Math.min(goal, progress + step) : Math.max(goal, progress - step);
            }
            lastTime = now;
            return value();
        }

        public double value() { return easing.apply(progress); }
        public double raw() { return progress; }
        public boolean settled(boolean target) { return progress == (target ? 1 : 0); }

        public void set(double rawProgress) {
            progress = clamp01(rawProgress);
            initialized = false;
        }
    }
}
