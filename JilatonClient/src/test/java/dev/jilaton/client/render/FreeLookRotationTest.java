package dev.jilaton.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeLookRotationTest {
    /** Documented smoothness bound: 540 deg/s over a 1/60 s frame is 9 degrees. */
    private static final float FRAME_STEP_DEGREES = 9.0F;
    private static final float STEP_EPSILON = 1.0E-4F;

    @Test
    void mouseTurnKeepsTheExistingScaleAndWraps() {
        FreeLookRotation rotation = new FreeLookRotation();
        rotation.reset(170, 0);
        rotation.turn(600, 0, 1.0, true);
        assertEquals(-100.0F, rotation.yaw(), STEP_EPSILON);
    }

    @Test
    void rejoinNeverMovesMoreThanTheFrameBudget() {
        FreeLookRotation rotation = new FreeLookRotation();
        rotation.reset(90, 0);
        float previousYaw = rotation.yaw();
        float previousError = rotation.angularError(0, 0);
        int frames = 0;
        while (rotation.angularError(0, 0) > STEP_EPSILON && frames < 100) {
            rotation.stepToward(0, 0, FRAME_STEP_DEGREES);
            float moved = Math.abs(FreeLookRotation.wrap(rotation.yaw() - previousYaw));
            assertTrue(moved <= FRAME_STEP_DEGREES + STEP_EPSILON, "frame " + frames + " moved " + moved);
            assertTrue(rotation.angularError(0, 0) < previousError, "error must shrink every frame");
            previousError = rotation.angularError(0, 0);
            previousYaw = rotation.yaw();
            frames++;
        }
        assertEquals(0.0F, rotation.angularError(0, 0), STEP_EPSILON);
        assertEquals(10, frames);
    }

    @Test
    void rejoinAtTwentyTpsStaysUnderTheDocumentedTickEpsilon() {
        FreeLookRotation rotation = new FreeLookRotation();
        rotation.reset(90, 0);
        float previousYaw = rotation.yaw();
        rotation.stepToward(-90, 0, 27.0F);
        float moved = Math.abs(FreeLookRotation.wrap(rotation.yaw() - previousYaw));
        assertEquals(27.0F, moved, STEP_EPSILON);
    }

    @Test
    void rejoinCrossesTheWrapBoundaryOnTheShortArc() {
        FreeLookRotation rotation = new FreeLookRotation();
        rotation.reset(179, 0);
        rotation.stepToward(-170, 0, 3.0F);
        assertEquals(-178.0F, rotation.yaw(), STEP_EPSILON);
        assertTrue(Math.abs(FreeLookRotation.wrap(rotation.yaw() - 179.0F)) <= 3.0F + STEP_EPSILON);
    }

    @Test
    void rejoinClampsPitchToTheVanillaRange() {
        FreeLookRotation rotation = new FreeLookRotation();
        rotation.reset(0, 80);
        float remaining = rotation.stepToward(0, 200, 200);
        assertEquals(0.0F, remaining, STEP_EPSILON);
        assertEquals(90.0F, rotation.pitch(), STEP_EPSILON);
    }

    @Test
    void nonFiniteRejoinTargetsLeaveTheCameraUntouched() {
        FreeLookRotation rotation = new FreeLookRotation();
        rotation.reset(30, 10);
        assertEquals(Float.POSITIVE_INFINITY, rotation.stepToward(Float.NaN, 0, 5));
        assertEquals(30.0F, rotation.yaw(), STEP_EPSILON);
        assertEquals(10.0F, rotation.pitch(), STEP_EPSILON);
        assertFalse(Float.isFinite(rotation.stepToward(0, Float.POSITIVE_INFINITY, 5)));
    }

    @Test
    void wrapUsesTheClosedRangeMinusOneEightyToOneEighty() {
        assertEquals(-180.0F, FreeLookRotation.wrap(180.0F));
        assertEquals(-180.0F, FreeLookRotation.wrap(-180.0F));
        assertEquals(179.0F, FreeLookRotation.wrap(539.0F));
    }
}
