package dev.jilaton.client.module.impl;

import java.util.Random;

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

    public static int sampleIntegerCps(double minimumCps, double maximumCps, Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random source is required");
        }
        int lower = (int) Math.round(clamp(Double.isFinite(minimumCps) ? minimumCps : 1.0, 1.0, 20.0));
        int upper = (int) Math.round(clamp(Double.isFinite(maximumCps) ? maximumCps : 20.0, 1.0, 20.0));
        if (lower > upper) {
            int swap = lower;
            lower = upper;
            upper = swap;
        }
        return random.nextInt(lower, upper + 1);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
