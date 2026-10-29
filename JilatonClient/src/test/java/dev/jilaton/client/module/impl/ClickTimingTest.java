package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClickTimingTest {
    @Test
    void steadyPatternUsesTheCpsRangeMidpoint() {
        assertEquals(12.0, ClickTiming.sampleCps(8.0, 16.0, "Steady", 3.0));
    }

    @Test
    void normalDistributionStaysWithinTheConfiguredRange() {
        assertEquals(8.0, ClickTiming.sampleCps(8.0, 16.0, "Normal distribution", -9.0));
        assertEquals(12.0, ClickTiming.sampleCps(8.0, 16.0, "Normal distribution", 0.0));
        assertEquals(16.0, ClickTiming.sampleCps(8.0, 16.0, "Normal distribution", 9.0));
    }

    @Test
    void reversedAndOutOfRangeLimitsAreNormalized() {
        assertEquals(10.5, ClickTiming.sampleCps(40.0, -4.0, "Steady", 0.0));
        assertEquals(20.0, ClickTiming.sampleCps(25.0, 30.0, "Steady", 0.0));
    }

    @Test
    void cpsConvertsToNanosecondIntervalsWithinSupportedBounds() {
        assertEquals(100_000_000L, ClickTiming.intervalNanos(10.0));
        assertEquals(1_000_000_000L, ClickTiming.intervalNanos(0.0));
        assertEquals(50_000_000L, ClickTiming.intervalNanos(30.0));
    }
}
