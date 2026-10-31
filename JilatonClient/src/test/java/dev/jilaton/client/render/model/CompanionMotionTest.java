package dev.jilaton.client.render.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionMotionTest {
    @Test
    void springSettlesOnItsTarget() {
        double[] state = new double[6];
        double[] target = {1, 0.5, -0.25};
        double stiffness = 11;
        double damping = CompanionMotion.dampingFor(stiffness, 0.9);
        for (int step = 0; step < 600; step++) {
            state = CompanionMotion.springStep(state[0], state[1], state[2], state[3], state[4], state[5],
                    target[0], target[1], target[2], stiffness, damping, 1.0 / 120);
        }
        assertEquals(target[0], state[0], 1e-3);
        assertEquals(target[1], state[1], 1e-3);
        assertEquals(target[2], state[2], 1e-3);
        assertEquals(0, state[3], 1e-2);
    }

    @Test
    void springDoesNotOvershootWildly() {
        double[] state = {0, 0, 0, 0, 0, 0};
        double stiffness = 11;
        double damping = CompanionMotion.dampingFor(stiffness, 0.9);
        double furthest = 0;
        for (int step = 0; step < 300; step++) {
            state = CompanionMotion.springStep(state[0], state[1], state[2], state[3], state[4], state[5],
                    1, 0, 0, stiffness, damping, 1.0 / 120);
            furthest = Math.max(furthest, Math.abs(state[0]));
        }
        assertTrue(furthest < 1.2, "overshoot was " + furthest);
    }

    @Test
    void springApproximatesTheSameMotionAcrossStepSizes() {
        double stiffness = 11;
        double damping = CompanionMotion.dampingFor(stiffness, 0.9);
        double[] one = CompanionMotion.springStep(0, 0, 0, 0, 0, 0, 1, 0, 0, stiffness, damping, 1.0 / 120);
        double[] half = CompanionMotion.springStep(0, 0, 0, 0, 0, 0, 1, 0, 0, stiffness, damping, 1.0 / 240);
        double[] two = CompanionMotion.springStep(half[0], half[1], half[2], half[3], half[4], half[5],
                1, 0, 0, stiffness, damping, 1.0 / 240);
        assertEquals(one[0], two[0], 1e-3);
        assertEquals(one[3], two[3], 1e-2);
    }

    @Test
    void bankingIsBoundedAndSigned() {
        assertEquals(8, CompanionMotion.bankDegrees(0.5, 1, 16), 1e-9);
        assertEquals(16, CompanionMotion.bankDegrees(2, 1, 16), 1e-9);
        assertEquals(-8, CompanionMotion.bankDegrees(-0.5, 1, 16), 1e-9);
        assertEquals(0, CompanionMotion.bankDegrees(0.5, 0, 16), 0);
    }

    @Test
    void orbitAdvanceIsDtDrivenAndRespectsReducedMotion() {
        double oneStep = CompanionMotion.advanceOrbit(0, 0.25, 0.9, false);
        double manySteps = 0;
        for (int i = 0; i < 100; i++) manySteps = CompanionMotion.advanceOrbit(manySteps, 0.0025, 0.9, false);
        assertEquals(oneStep, manySteps, 1e-9);
        assertEquals(1.4, CompanionMotion.advanceOrbit(1.4, 0.5, 2, true), 0);
    }

    @Test
    void eachShapeUsesMoreThanOnePrimitive() {
        for (String shape : CompanionMotion.SHAPES) {
            assertTrue(CompanionMotion.shards(shape) >= 2, shape + " needs orbiting shards");
        }
        assertFalse(CompanionMotion.hasRing("Wisp"));
        assertTrue(CompanionMotion.hasRing("Sentinel"));
        assertTrue(CompanionMotion.hasRing("Comet"));
        assertTrue(CompanionMotion.hasTail("Comet"));
        assertFalse(CompanionMotion.hasTail("Sentinel"));
    }

    @Test
    void brokenInputsCannotPoisonTheOrbitOrTheSpring() {
        assertEquals(1.4, CompanionMotion.advanceOrbit(1.4, Double.NaN, 2, false), 0);
        assertEquals(CompanionMotion.advanceOrbit(0, 0.016, 2, false),
                CompanionMotion.advanceOrbit(Double.NaN, 0.016, 2, false), 0);

        double[] still = CompanionMotion.springStep(1, 2, 3, 4, 5, 6, 0, 0, 0, 11, 8, Double.NaN);
        assertEquals(1, still[0], 0);
        assertEquals(6, still[5], 0);

        assertEquals(0, CompanionMotion.bankDegrees(Double.NaN, 1, 16), 0);
        assertEquals(0, CompanionMotion.bankDegrees(0.5, Double.NaN, 16), 0);
        assertEquals(0, CompanionMotion.bankDegrees(0.5, 1, Double.NaN), 0);
    }

    @Test
    void criticalDampingIsTwiceTheRootStiffness() {
        assertEquals(8, CompanionMotion.dampingFor(16, 1), 1e-9);
        assertTrue(CompanionMotion.dampingFor(16, 1.4) > CompanionMotion.dampingFor(16, 0.4));
    }

    @Test
    void orbitTargetKeepsTheRequestedRadiusAndHeight() {
        double[] target = CompanionMotion.orbitTarget(3, 64, -2, 0.7, 1.35, 0.95);
        assertEquals(1.35, Math.hypot(target[0] - 3, target[2] + 2), 1e-9);
        assertEquals(64.95, target[1], 1e-9);
    }
}
