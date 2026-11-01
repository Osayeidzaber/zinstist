package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Velocity's randomised horizontal scale. The arithmetic is original to Jilaton (no LiquidBounce
 * code is copied); the contract is simply that a configured range is honoured and that a range
 * entered backwards, or with non-finite values, can never produce an out-of-range result.
 */
class VelocityRangeTest {
    private static final double EPSILON = 1.0E-9;

    @Test
    void sampledValueAlwaysLandsInsideTheConfiguredRange() {
        for (int i = 0; i <= 1000; i++) {
            double unit = i / 1000.0;
            double value = VelocityRange.sample(-100.0, 100.0, unit);
            assertTrue(value >= -100.0 && value <= 100.0, "out of range at " + unit + ": " + value);
            assertEquals(-100.0 + 200.0 * unit, value, EPSILON);
        }
    }

    @Test
    void reversedBoundsAreOrderedInsteadOfProducingNegativeRanges() {
        assertEquals(20.0, VelocityRange.sample(80.0, 20.0, 0.0), EPSILON);
        assertEquals(50.0, VelocityRange.sample(80.0, 20.0, 0.5), EPSILON);
        assertEquals(80.0, VelocityRange.sample(80.0, 20.0, 1.0), EPSILON);
        for (int i = 0; i <= 1000; i++) {
            double value = VelocityRange.sample(80.0, 20.0, i / 1000.0);
            assertTrue(value >= 20.0 && value <= 80.0, "out of reversed range: " + value);
        }
    }

    @Test
    void clampedUnitPositionAndNonFiniteBoundsCannotEscapeTheContract() {
        assertEquals(-100.0, VelocityRange.sample(-100.0, 100.0, -3.0), EPSILON);
        assertEquals(100.0, VelocityRange.sample(-100.0, 100.0, 4.0), EPSILON);
        assertEquals(-100.0, VelocityRange.sample(-100.0, 100.0, Double.NaN), EPSILON);
        assertEquals(20.0, VelocityRange.sample(80.0, 20.0, Double.NaN), EPSILON);
        assertEquals(0.0, VelocityRange.sample(Double.NaN, 100.0, 0.5), EPSILON);
        assertEquals(0.0, VelocityRange.sample(-100.0, Double.POSITIVE_INFINITY, 0.5), EPSILON);
    }

    @Test
    void onlyDistinctFiniteBoundsCountAsRandom() {
        assertFalse(VelocityRange.isRandom(0.0, 0.0));
        assertFalse(VelocityRange.isRandom(50.0, 50.0));
        assertTrue(VelocityRange.isRandom(0.0, 100.0));
        assertTrue(VelocityRange.isRandom(80.0, 20.0));
        assertFalse(VelocityRange.isRandom(Double.NaN, 100.0));
    }

    @Test
    void moduleFallsBackToTheFixedScaleAndStaysInsideARandomRange() {
        VelocityModule module = new VelocityModule();

        set(module, "horizontal", 40.0);
        assertEquals(0.4, module.horizontalScale(), EPSILON);

        set(module, "horizontal_random_min", -60.0);
        set(module, "horizontal_random_max", 60.0);
        for (int i = 0; i < 1000; i++) {
            double scale = module.horizontalScale();
            assertTrue(scale >= -0.6 && scale <= 0.6, "module scale escaped its range: " + scale);
        }

        set(module, "horizontal_random_min", 80.0);
        set(module, "horizontal_random_max", 20.0);
        for (int i = 0; i < 1000; i++) {
            double scale = module.horizontalScale();
            assertTrue(scale >= 0.2 && scale <= 0.8, "module scale escaped its reversed range: " + scale);
        }
    }

    private static void set(ClientModule module, String id, double value) {
        module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .map(NumberSetting.class::cast)
                .findFirst()
                .orElseThrow()
                .setValue(value);
    }
}
