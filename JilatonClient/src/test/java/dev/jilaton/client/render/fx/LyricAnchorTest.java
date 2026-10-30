package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for the world-fixed lyric anchor: where a stack is born, that camera and line changes
 * never move it, when it is allowed to re-anchor, and the first-person distance and height guarantees.
 */
class LyricAnchorTest {
    private static final double EPS = 1e-9;

    @Test
    void birthPutsTheArcOnTheGazeAndTheAnchorDoesNotGlide() {
        double yaw = Math.toRadians(37);
        double r = LyricLayout.MIN_FIRST_PERSON_RADIUS;
        LyricLayout.Anchor anchor = LyricLayout.Anchor.birth(-Math.sin(yaw) * r, 72, Math.cos(yaw) * r, yaw, 0);

        assertEquals(-Math.sin(yaw) * r, anchor.x(0), EPS);
        assertEquals(72, anchor.y(0), EPS);
        assertEquals(Math.cos(yaw) * r, anchor.z(0), EPS);
        assertEquals(yaw, anchor.bearing(0), EPS);
        assertFalse(anchor.gliding(0));

        // The active slot is placed on the frozen bearing, offset by the side angle, not on a later gaze.
        LyricLayout.Params params = new LyricLayout.Params("Floating Side", 2.3, 0.3, 26, 16, true, 0, yaw);
        LyricLayout.Slot active = LyricLayout.slot(params, 0, anchor.bearing(0));
        assertEquals(yaw + Math.toRadians(26), LyricLayout.bearingOf(active), EPS);
    }

    @Test
    void lineChangesAndCameraPansWithinLimitsNeverMoveTheAnchor() {
        LyricLayout.Anchor anchor = LyricLayout.Anchor.birth(4, 64, 9, 0, 0);
        for (double t : new double[]{0.5, 4, 30, 90}) {
            assertEquals(4, anchor.x(t), EPS);
            assertEquals(64, anchor.y(t), EPS);
            assertEquals(9, anchor.z(t), EPS);
            assertEquals(0, anchor.bearing(t), EPS);
            assertFalse(anchor.gliding(t));
        }
        // Panning to just inside the 55 degree limit, while standing on the anchor, is still "in view".
        assertFalse(anchor.wantsReanchor(30, Math.toRadians(54.9), 4, 64, 9));
        assertFalse(anchor.wantsReanchor(60, Math.toRadians(-54.9), 4, 64, 9));
    }

    @Test
    void reanchorWaitsForTheLostConditionToPersist() {
        LyricLayout.Anchor anchor = LyricLayout.Anchor.birth(0, 64, 0, 0, 0);
        double lost = Math.toRadians(60);
        assertFalse(anchor.wantsReanchor(2.0, lost, 0, 64, 0), "first lost frame must not move the stack");
        assertFalse(anchor.wantsReanchor(2.9, lost, 0, 64, 0), "0.9 s lost is below the 1 s window");
        assertTrue(anchor.wantsReanchor(3.1, lost, 0, 64, 0), "must move once the stack stays lost for 1 s");
    }

    @Test
    void lookingBackResetsTheLostTimer() {
        LyricLayout.Anchor anchor = LyricLayout.Anchor.birth(0, 64, 0, 0, 0);
        double lost = Math.toRadians(60);
        assertFalse(anchor.wantsReanchor(2.0, lost, 0, 64, 0));
        assertFalse(anchor.wantsReanchor(2.4, 0, 0, 64, 0));
        assertFalse(anchor.wantsReanchor(2.6, lost, 0, 64, 0));
        assertFalse(anchor.wantsReanchor(3.4, lost, 0, 64, 0), "0.8 s of the new lost streak is not enough");
        assertTrue(anchor.wantsReanchor(3.7, lost, 0, 64, 0));
    }

    @Test
    void reanchorsAreRateLimitedToOnePerTwoAndAHalfSeconds() {
        LyricLayout.Anchor anchor = LyricLayout.Anchor.birth(0, 64, 0, 0, 0).reanchor(0, 64, -3, 0, 10);
        double lost = Math.PI / 2;
        assertFalse(anchor.wantsReanchor(10.01, lost, 0, 64, 0));
        assertFalse(anchor.wantsReanchor(11.5, lost, 0, 64, 0), "1.5 s after the last placement is too soon");
        assertTrue(anchor.wantsReanchor(12.6, lost, 0, 64, 0));
    }

    @Test
    void distanceAlsoLosesTheStack() {
        LyricLayout.Anchor anchor = LyricLayout.Anchor.birth(0, 64, 0, 0, 0);
        assertFalse(anchor.wantsReanchor(10, 0, 7.1, 64, 0));
        assertTrue(anchor.wantsReanchor(11.05, 0, 7.1, 64, 0));
    }

    @Test
    void reanchorGlidesFromTheOldSpotInsteadOfSnapping() {
        LyricLayout.Anchor anchor = LyricLayout.Anchor.birth(0, 64, 0, 0, 0);
        LyricLayout.Anchor moved = anchor.reanchor(6, 67, 6, Math.PI / 2, 0.1);

        assertEquals(0, moved.x(0.1), EPS);
        assertEquals(0, moved.z(0.1), EPS);
        assertTrue(moved.gliding(0.2));
        double mid = moved.x(0.4);
        assertTrue(mid > 0 && mid < 6, "the glide is in progress halfway through");

        assertEquals(6, moved.x(0.7), EPS);
        assertEquals(67, moved.y(0.7), EPS);
        assertEquals(6, moved.z(0.7), EPS);
        assertEquals(Math.PI / 2, moved.bearing(0.7), EPS);
        assertFalse(moved.gliding(0.7));
        assertEquals(6, moved.targetX(), EPS);
        assertEquals(67, moved.targetY(), EPS);
        assertEquals(6, moved.targetZ(), EPS);
        assertEquals(Math.PI / 2, moved.targetBearing(), EPS);
    }

    @Test
    void firstPersonRadiusIsClampedAwayFromTheEye() {
        assertEquals(LyricLayout.MIN_FIRST_PERSON_RADIUS, LyricLayout.effectiveRadius(0.5, true), EPS);
        assertEquals(LyricLayout.MIN_RADIUS, LyricLayout.effectiveRadius(0.5, false), EPS);
        assertEquals(3.0, LyricLayout.effectiveRadius(3.0, true), EPS);

        // Anchor on the gaze at the clamped radius; the visible arc stays beyond the clamp from the eye for
        // every side angle. "Behind Player" intentionally trails behind the anchor and is excluded.
        double r = LyricLayout.effectiveRadius(0.6, true);
        for (String mode : List.of("Curved Arc", "Floating Side", "Circular Orbit", "Cinematic Floating", "Minimal")) {
            for (int degrees = -90; degrees <= 90; degrees += 15) {
                LyricLayout.Params params = new LyricLayout.Params(mode, 0.6, 0.3, degrees, 16, true, 0, 0);
                LyricLayout.Slot slot = LyricLayout.slot(params, 0, 0);
                // Anchor sits at (0, r) when the gaze is north; the eye is the origin.
                double distance = Math.hypot(slot.dx(), r + slot.dz());
                assertTrue(distance >= LyricLayout.MIN_FIRST_PERSON_RADIUS - 1e-9, mode + " at " + degrees + "deg");
            }
        }
    }

    @Test
    void firstPersonActiveLineNeverFallsBelowTheEyeHeightGuarantee() {
        for (String mode : LyricLayout.MODES) {
            LyricLayout.Params params = new LyricLayout.Params(mode, 2.3, 0.3, 26, 16, true, 0, 0);
            double activeHeight = LyricLayout.slot(params, 0, 0).dy() + LyricLayout.verticalShift(params, 0);
            assertTrue(activeHeight >= LyricLayout.FIRST_PERSON_LIFT - EPS, mode);
        }
        LyricLayout.Params side = new LyricLayout.Params("Floating Side", 2.3, 0.3, 26, 16, true, 0, 0);
        assertEquals(LyricLayout.FIRST_PERSON_LIFT,
                LyricLayout.slot(side, 0, 0).dy() + LyricLayout.verticalShift(side, 0), EPS);
        LyricLayout.Params thirdPerson = new LyricLayout.Params("Floating Side", 2.3, 0.3, 26, 16, false, 0, 0);
        assertEquals(0, LyricLayout.verticalShift(thirdPerson, 0), EPS);
    }

    @Test
    void heightSettingStillRaisesTheStackInFirstPerson() {
        LyricLayout.Params low = new LyricLayout.Params("Floating Side", 2.3, -1, 26, 16, true, 0, 0);
        assertEquals(1.35, LyricLayout.verticalShift(low, 0), 1e-12);

        LyricLayout.Params high = new LyricLayout.Params("Floating Side", 2.3, 2, 26, 16, true, 0, 0);
        assertEquals(0, LyricLayout.verticalShift(high, 0), EPS);
        assertEquals(2, LyricLayout.slot(high, 0, 0).dy() + LyricLayout.verticalShift(high, 0), EPS);

        LyricLayout.Params raised = new LyricLayout.Params("Floating Side", 2.3, 1, 26, 16, true, 0, 0);
        assertEquals(0, LyricLayout.verticalShift(raised, 0), EPS);
        assertEquals(1, LyricLayout.slot(raised, 0, 0).dy() + LyricLayout.verticalShift(raised, 0), EPS);

        LyricLayout.Params thirdPerson = new LyricLayout.Params("Floating Side", 2.3, 2, 26, 16, false, 0, 0);
        assertEquals(0, LyricLayout.verticalShift(thirdPerson, 0), EPS);
    }

    @Test
    void verticalShiftPinsBothEndsOfTheHeightRange() {
        LyricLayout.Params floor = new LyricLayout.Params("Floating Side", 2.3, 0, 26, 16, true, 0, 0);
        assertEquals(LyricLayout.FIRST_PERSON_LIFT,
                LyricLayout.slot(floor, 0, 0).dy() + LyricLayout.verticalShift(floor, 0), 1e-9);

        LyricLayout.Params raised = new LyricLayout.Params("Floating Side", 2.3, 1.4, 26, 16, true, 0, 0);
        assertEquals(0.0, LyricLayout.verticalShift(raised, 0));
        assertEquals(1.4, LyricLayout.slot(raised, 0, 0).dy());

        for (double height : new double[]{-1, 0, 0.35, 1.4, 2}) {
            LyricLayout.Params thirdPerson = new LyricLayout.Params("Floating Side", 2.3, height, 26, 16, false, 0, 0);
            assertEquals(0.0, LyricLayout.verticalShift(thirdPerson, 0));
        }
    }

    @Test
    void effectiveRadiusNeverReturnsBelowItsFloor() {
        for (double radius : new double[]{-5, 0, 0.1, 1.1, 1.19, 1.2, 2.19, 2.2, 4}) {
            assertTrue(LyricLayout.effectiveRadius(radius, true) >= LyricLayout.MIN_FIRST_PERSON_RADIUS, "fp " + radius);
            assertTrue(LyricLayout.effectiveRadius(radius, false) >= LyricLayout.MIN_RADIUS, "tp " + radius);
        }
    }
}
