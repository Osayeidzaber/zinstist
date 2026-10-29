package dev.jilaton.client.module.impl;

public final class ClickTiming {
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private ClickTiming() {
    }

    public static double sampleCps(double minimumCps, double maximumCps, String pattern, double normalSample) {
        double lower = clamp(Double.isFinite(minimumCps) ? minimumCps : 1.0, 1.0, 20.0);
        double upper = clamp(Double.isFinite(maximumCps) ? maximumCps : 20.0, 1.0, 20.0);
        if (lower > upper) {
            double swap = lower;
            lower = upper;
            upper = swap;
        }

        double midpoint = (lower + upper) * 0.5;
        if (!"Normal distribution".equals(pattern) || upper == lower || !Double.isFinite(normalSample)) {
            return midpoint;
        }

        double standardDeviation = (upper - lower) / 6.0;
        return clamp(midpoint + normalSample * standardDeviation, lower, upper);
    }

    public static long intervalNanos(double cps) {
        double boundedCps = clamp(Double.isFinite(cps) ? cps : 1.0, 1.0, 20.0);
        return Math.round(NANOS_PER_SECOND / boundedCps);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
