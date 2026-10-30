package dev.jilaton.client.module.impl;

import java.util.Arrays;
import java.util.Random;

/**
 * Distributes a bounded click rate across the client's 20 ticks per second.
 * A new one-second schedule is sampled only when the previous one is consumed.
 */
public final class ClickSchedule {
    private static final int TICKS_PER_SECOND = 20;

    private final Random random;
    private final int[] clicks = new int[TICKS_PER_SECOND];
    private int tickIndex = TICKS_PER_SECOND;
    private double fractionalClicks;
    private double previousMinimum = Double.NaN;
    private double previousMaximum = Double.NaN;
    private String previousPattern;

    public ClickSchedule() {
        this(new Random());
    }

    ClickSchedule(Random random) {
        this.random = random;
    }

    /** Advances the schedule by one client tick and reports whether this tick is due. */
    public boolean advance(double minimumCps, double maximumCps, String pattern) {
        return advanceClicks(minimumCps, maximumCps, pattern) > 0;
    }

    /** Advances one client tick and returns the number of scheduled clicks for this tick. */
    public int advanceClicks(double minimumCps, double maximumCps, String pattern) {
        String selectedPattern = pattern == null ? "Steady" : pattern;
        if (Double.compare(minimumCps, previousMinimum) != 0
                || Double.compare(maximumCps, previousMaximum) != 0
                || !selectedPattern.equals(previousPattern)) {
            previousMinimum = minimumCps;
            previousMaximum = maximumCps;
            previousPattern = selectedPattern;
            fractionalClicks = random.nextDouble();
            tickIndex = TICKS_PER_SECOND;
        }

        if (tickIndex >= TICKS_PER_SECOND) {
            refill(minimumCps, maximumCps, selectedPattern);
        }
        return clicks[tickIndex++];
    }

    /** Resets timing after the module is toggled. */
    public void reset() {
        Arrays.fill(clicks, 0);
        tickIndex = TICKS_PER_SECOND;
        fractionalClicks = random.nextDouble();
        previousMinimum = Double.NaN;
        previousMaximum = Double.NaN;
        previousPattern = null;
    }

    private void refill(double minimumCps, double maximumCps, String pattern) {
        Arrays.fill(clicks, 0);
        if (!"Steady".equals(pattern) && !"Normal distribution".equals(pattern)) {
            int cps = ClickTiming.sampleIntegerCps(minimumCps, maximumCps, random);
            ClickPatternDistribution.fill(clicks, cps, pattern, random);
            tickIndex = 0;
            return;
        }
        double cps = ClickTiming.sampleCps(minimumCps, maximumCps, pattern, random.nextGaussian());
        double clicksPerTick = cps / TICKS_PER_SECOND;
        for (int tick = 0; tick < TICKS_PER_SECOND; tick++) {
            fractionalClicks += clicksPerTick;
            int due = (int) fractionalClicks;
            clicks[tick] = due;
            fractionalClicks -= due;
        }
        tickIndex = 0;
    }
}
