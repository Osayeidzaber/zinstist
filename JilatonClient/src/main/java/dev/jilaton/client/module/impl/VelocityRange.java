package dev.jilaton.client.module.impl;

/**
 * Original Jilaton arithmetic for Velocity's randomised horizontal scale. Nothing here is adapted
 * from LiquidBounce: it is a plain bounds sample whose input is supplied by the caller so it stays
 * deterministic under test.
 */
final class VelocityRange {
    private VelocityRange() {
    }

    /** True when the two configured bounds describe a randomised range rather than a fixed value. */
    static boolean isRandom(double minimum, double maximum) {
        return Double.isFinite(minimum) && Double.isFinite(maximum) && minimum != maximum;
    }

    /** Position {@code unit} (0..1) between the two bounds, independent of their order. */
    static double sample(double minimum, double maximum, double unit) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)) {
            return 0.0;
        }
        double low = Math.min(minimum, maximum);
        double high = Math.max(minimum, maximum);
        double position = Double.isFinite(unit) ? Math.max(0.0, Math.min(1.0, unit)) : 0.0;
        // Clamp after interpolation so the result is inside the range even for extreme magnitudes.
        return Math.max(low, Math.min(high, low + (high - low) * position));
    }
}
