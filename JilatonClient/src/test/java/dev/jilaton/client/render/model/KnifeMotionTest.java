package dev.jilaton.client.render.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnifeMotionTest {
    private static final long S = 1_000_000_000L;

    @Test
    void everyActionStartsAndEndsAtRestSoSwitchingNeverPops() {
        for (int dir : new int[]{-1, 1}) {
            assertRest(KnifeMotion.slash(0, dir));
            assertRest(KnifeMotion.slash(1, dir));
        }
        assertRest(KnifeMotion.inspect(0));
        KnifeMotion.Pose end = KnifeMotion.inspect(1);
        assertEquals(0, end.yaw(), 1e-9);
        assertEquals(0, end.roll(), 1e-9);
        assertEquals(360, end.spin(), 1e-9, "a full flip lands the blade back in the grip");
        KnifeMotion.Pose drawn = KnifeMotion.draw(1);
        assertEquals(0, drawn.y(), 1e-9);
        assertEquals(0, drawn.pitch(), 1e-9);
        assertEquals(0, drawn.spin(), 1e-9);
    }

    @Test
    void theDrawRisesFromBelowWhileTheBladeTwirls() {
        KnifeMotion.Pose start = KnifeMotion.draw(0);
        assertTrue(start.y() < -0.5);
        assertEquals(360, start.spin(), 1e-9);
        assertTrue(KnifeMotion.draw(0.5).y() > start.y());
    }

    @Test
    void consecutiveSlashesComeFromOppositeSides() {
        KnifeMotion motion = new KnifeMotion();
        motion.startSlash(0);
        int first = motion.slashDirection();
        motion.startSlash(S);
        assertEquals(-first, motion.slashDirection());
        assertTrue(KnifeMotion.slash(0.2, 1).yaw() * KnifeMotion.slash(0.2, -1).yaw() < 0);
    }

    @Test
    void inspectWaitsForTheSlashToFinish() {
        KnifeMotion motion = new KnifeMotion();
        motion.startSlash(0);
        assertFalse(motion.startInspect(S / 10, 0.5));
        assertTrue(motion.startInspect(S, 0.5));
    }

    @Test
    void turningDragsTheArmBackAndItSettlesWhenTheViewStops() {
        KnifeMotion motion = new KnifeMotion();
        motion.look(0, 0, 0.016, 1);
        motion.look(5, 0, 0.016, 1);
        double dragged = motion.pose(0, 0.5, 1, 0, 0, 0, false).yaw();
        assertTrue(dragged < 0, "turning right lags the arm to the left");
        for (int i = 0; i < 120; i++) motion.look(5, 0, 0.016, 1);
        assertEquals(0, motion.pose(0, 0.5, 1, 0, 0, 0, false).yaw(), 1e-3);
        // A turn across the ±180 seam is a small turn, not a full revolution.
        KnifeMotion seam = new KnifeMotion();
        seam.look(179, 0, 0.016, 1);
        seam.look(-179, 0, 0.016, 1);
        assertTrue(Math.abs(seam.pose(0, 0.5, 1, 0, 0, 0, false).yaw()) < KnifeMotion.INERTIA_LIMIT);
    }

    @Test
    void reducedMotionHoldsTheRestPose() {
        KnifeMotion motion = new KnifeMotion();
        motion.startSlash(0);
        assertRest(motion.pose(S / 10, 0.5, 1, 1, 0.5, 1, true));
    }

    @Test
    void classifiesAndBlendsAllElevenCosmeticAnimationStates() {
        KnifeMotion motion = new KnifeMotion();
        assertEquals(KnifeMotion.State.IDLE, motion.classifyState(0, 0.5, false, false, true, 0.0, 0.0));
        assertEquals(KnifeMotion.State.WALKING, motion.classifyState(0, 0.5, false, false, true, 0.0, 0.6));
        assertEquals(KnifeMotion.State.SPRINTING, motion.classifyState(0, 0.5, true, false, true, 0.0, 0.9));
        assertEquals(KnifeMotion.State.CROUCHING, motion.classifyState(0, 0.5, false, true, true, 0.0, 0.2));
        assertEquals(KnifeMotion.State.JUMPING, motion.classifyState(0, 0.5, false, false, false, 0.42, 0.5));
        assertEquals(KnifeMotion.State.FALLING, motion.classifyState(0, 0.5, false, false, false, -0.55, 0.5));

        motion.startDraw(S);
        assertEquals(KnifeMotion.State.EQUIPPING, motion.classifyState(S + S / 10, 0.5, false, false, true, 0.0, 0.0));

        motion.startSwitch(2 * S);
        assertEquals(KnifeMotion.State.SWITCHING, motion.classifyState(2 * S + S / 10, 0.5, false, false, true, 0.0, 0.0));

        motion.startInspect(3 * S, 0.5);
        assertEquals(KnifeMotion.State.INSPECTING, motion.classifyState(3 * S + S / 2, 0.5, false, false, true, 0.0, 0.0));

        motion.startSlash(4 * S);
        assertEquals(KnifeMotion.State.ATTACKING, motion.classifyState(4 * S + S / 10, 0.5, false, false, true, 0.0, 0.0));

        motion.startUnequip(5 * S);
        assertEquals(KnifeMotion.State.UNEQUIPPING, motion.classifyState(5 * S + S / 10, 0.5, false, false, true, 0.0, 0.0));

        KnifeMotion.Pose jumping = motion.pose(6 * S, 0.5, 1, 1, 0.5, 0.5, false, false, false, 0.42, false);
        KnifeMotion.Pose falling = motion.pose(6 * S + S / 10, 0.5, 1, 1, 0.5, 0.5, false, false, false, -0.60, false);
        assertTrue(jumping.y() < falling.y(), "jumping lags downward while falling lifts upward");
    }

    private static void assertRest(KnifeMotion.Pose pose) {
        assertEquals(0, pose.x(), 1e-9);
        assertEquals(0, pose.y(), 1e-9);
        assertEquals(0, pose.z(), 1e-9);
        assertEquals(0, pose.pitch(), 1e-9);
        assertEquals(0, pose.yaw(), 1e-9);
        assertEquals(0, pose.roll(), 1e-9);
    }
}
