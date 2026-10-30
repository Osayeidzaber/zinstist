package dev.jilaton.client.render.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HaloMotionTest {
    private static final double HEIGHT = 1.8;

    @Test
    void ringAlwaysStaysBelowTheNameTagAnchor() {
        double limit = HEIGHT + HaloMotion.TAG_ANCHOR - HaloMotion.TAG_GAP;
        for (double radius = 0.16; radius <= 0.4001; radius += 0.02) {
            for (double thickness = 0.008; thickness <= 0.0501; thickness += 0.006) {
                for (double bob = 0; bob <= 0.0401; bob += 0.008) {
                    for (double tilt = -30; tilt <= 30.0001; tilt += 5) {
                        double clamped = HaloMotion.clampTilt(tilt, radius, thickness, bob);
                        double top = HaloMotion.topHeight(HEIGHT, radius, clamped, thickness, bob);
                        assertTrue(top <= limit + 1e-9,
                                "halo top " + top + " reached the tag layer at r=" + radius + " t=" + thickness
                                        + " bob=" + bob + " tilt=" + tilt);
                    }
                }
            }
        }
    }

    @Test
    void theTiltBudgetShrinksAsTheRingGrows() {
        double small = HaloMotion.maxTilt(0.18, 0.018, 0.012);
        double large = HaloMotion.maxTilt(0.40, 0.018, 0.012);
        assertTrue(small > large, "a larger ring must have less room before it touches the tag");
        assertTrue(large >= 0);
    }

    @Test
    void aFlatRingHasNoTiltLimit() {
        assertEquals(90, HaloMotion.maxTilt(0, 0.02, 0.01), 1e-9);
    }

    @Test
    void clampTiltIsSymmetric() {
        double max = HaloMotion.maxTilt(0.24, 0.018, 0.012);
        assertEquals(max, HaloMotion.clampTilt(80, 0.24, 0.018, 0.012), 1e-9);
        assertEquals(-max, HaloMotion.clampTilt(-80, 0.24, 0.018, 0.012), 1e-9);
        assertEquals(5, HaloMotion.clampTilt(5, 0.24, 0.018, 0.012), 1e-9);
    }

    @Test
    void spinAndBobFreezeUnderReducedMotion() {
        assertEquals(1.25, HaloMotion.advancePhase(1.25, 0.5, 2, true), 0);
        assertEquals(0, HaloMotion.bob(1.1, 0.03, true), 0);
        assertTrue(HaloMotion.advancePhase(0, 0.5, 2, false) > 0);
    }

    @Test
    void bobStaysInsideItsAmplitude() {
        for (int step = 0; step < 400; step++) {
            double value = HaloMotion.bob(step * 0.05, 0.03, false);
            assertTrue(Math.abs(value) <= 0.03 + 1e-9);
        }
    }

    @Test
    void centreSitsAboveTheHeadButBelowTheTag() {
        double centre = HaloMotion.centreHeight(HEIGHT);
        assertTrue(centre > HEIGHT);
        assertTrue(centre < HEIGHT + HaloMotion.TAG_ANCHOR);
    }

    @Test
    void ringPointsLieOnTheRequestedRadius() {
        for (int step = 0; step < 24; step++) {
            double angle = step * Math.PI / 12;
            double[] point = HaloMotion.ringPoint(0.24, angle);
            assertEquals(0.24, Math.hypot(point[0], point[2]), 1e-9);
            assertEquals(0, point[1], 1e-9);
        }
    }

    @Test
    void pulseStaysInsideItsBand() {
        for (int step = 0; step < 400; step++) {
            double value = HaloMotion.pulse(step * 0.05, false);
            assertTrue(value >= 0 && value <= 1, "pulse out of range: " + value);
        }
    }

    @Test
    void pulseFreezesUnderReducedMotion() {
        assertEquals(1.4, HaloMotion.advancePulse(1.4, 0.5, true), 0);
        assertEquals(0.5, HaloMotion.pulse(2.2, true), 0);
        assertTrue(HaloMotion.advancePulse(0, 0.5, false) > 0);
    }

    @Test
    void pulseIsFrameRateIndependent() {
        double oneStep = HaloMotion.advancePulse(0, 0.25, false);
        double manySteps = 0;
        for (int i = 0; i < 100; i++) manySteps = HaloMotion.advancePulse(manySteps, 0.0025, false);
        assertEquals(oneStep, manySteps, 1e-9);
    }

    @Test
    void qualityRaisesRingResolution() {
        assertTrue(HaloMotion.majorSegments(0) < HaloMotion.majorSegments(3));
        assertTrue(HaloMotion.minorSegments(0) < HaloMotion.minorSegments(3));
    }
}
