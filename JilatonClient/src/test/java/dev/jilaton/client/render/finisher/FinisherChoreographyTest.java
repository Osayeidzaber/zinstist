package dev.jilaton.client.render.finisher;

import dev.jilaton.client.render.finisher.FinisherChoreography.Variant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinisherChoreographyTest {
    private static final int WIDTH = FinisherChoreography.SPIN + 1;

    @Test
    void everyMoveIsContinuousWithNoSnaps() {
        for (Variant v : Variant.values()) {
            float[] prev = FinisherChoreography.sample(v, 0, new float[WIDTH]);
            float dt = 1F / 240F;
            for (float t = dt; t <= v.length; t += dt) {
                float[] now = FinisherChoreography.sample(v, t, new float[WIDTH]);
                for (int j = 0; j < WIDTH; j++) {
                    // 240 fps: any joint moving more than ~0.12 rad per frame would read as a pop.
                    assertTrue(Math.abs(now[j] - prev[j]) < 0.12F, v + " channel " + j + " jumps at " + t);
                }
                prev = now;
            }
        }
    }

    @Test
    void keysAreInOrderAndImpactLandsInsideTheMove() {
        for (Variant v : Variant.values()) {
            float[][] keys = FinisherChoreography.keys(v);
            for (int i = 1; i < keys.length; i++) assertTrue(keys[i][0] > keys[i - 1][0], v + " key " + i);
            assertEquals(v.length, keys[keys.length - 1][0], 1e-6, v + " last key ends the move");
            assertTrue(v.impact > FinisherChoreography.APPEAR && v.impact < v.length - FinisherChoreography.FADE, v.name());
        }
    }

    @Test
    void figureIsGroundedAtStartAndEndAndNeverSinks() {
        for (Variant v : Variant.values()) {
            float[] pose = new float[WIDTH];
            assertEquals(0F, FinisherChoreography.sample(v, 0, pose)[FinisherChoreography.RISE], 1e-6);
            assertEquals(0F, FinisherChoreography.sample(v, v.length, pose)[FinisherChoreography.RISE], 1e-6);
        }
    }

    @Test
    void fadesInAndOutAndIsFullyVisibleAtTheHit() {
        for (Variant v : Variant.values()) {
            assertEquals(0F, FinisherChoreography.alpha(v, 0F));
            assertEquals(0F, FinisherChoreography.alpha(v, v.length));
            assertEquals(1F, FinisherChoreography.alpha(v, v.impact), 1e-4);
            assertTrue(FinisherChoreography.alpha(v, 0.05F) < 0.2F);
        }
    }

    @Test
    void shakeOnlyAfterTheHit() {
        Variant v = Variant.GROUND_SLAM;
        assertEquals(0F, FinisherChoreography.shake(v, v.impact - 0.01F));
        assertEquals(1F, FinisherChoreography.shake(v, v.impact), 1e-4);
        assertEquals(0F, FinisherChoreography.shake(v, v.impact + 0.5F));
    }

    @Test
    void spinKickEndsFacingTheBodyAgain() {
        float[] end = FinisherChoreography.sample(Variant.SPIN_KICK, Variant.SPIN_KICK.length, new float[WIDTH]);
        assertEquals(0, Math.sin(end[FinisherChoreography.SPIN]), 1e-5);
        assertEquals(1, Math.cos(end[FinisherChoreography.SPIN]), 1e-5);
    }

    @Test
    void gateAdmitsEachDefeatOnce() {
        FinisherGate gate = new FinisherGate();
        assertTrue(gate.admit(7, 0));
        assertFalse(gate.admit(7, 500_000_000L));
        assertTrue(gate.admit(8, 500_000_000L));
        assertTrue(gate.admit(7, FinisherGate.COOLDOWN_NANOS + 1));
    }

    @Test
    void closeDefeatsPutTheCharacterBehindTheBody() {
        double[] sides = FinisherPlacement.sides(0, 0, 0, 2);
        assertArrayEquals(new double[]{0, 1}, new double[]{sides[0], sides[1]}, 1e-9);
    }

    @Test
    void placementIsBesideTheBodyNeverInFrontOfTheViewer() {
        double[] sides = FinisherPlacement.sides(0, 0, 0, 6);
        for (int i = 0; i < 4; i += 2) {
            assertEquals(1, Math.hypot(sides[i], sides[i + 1]), 1e-9);
            assertTrue(Math.abs(sides[i]) > 0.9, "mostly sideways");
        }
        assertArrayEquals(new double[]{0, 1}, new double[]{sides[4], sides[5]}, 1e-9);
        assertTrue(sides[0] * sides[2] < 0, "two candidate sides are mirrored");
    }

    @Test
    void crowdWalksInStopsAndCheersWithoutJumps() {
        float[] prev = FinisherChoreography.crowd(0, 0, new float[WIDTH]);
        assertEquals(FinisherChoreography.CROWD_FROM, prev[FinisherChoreography.FORWARD], 1e-4);
        for (float t = 1F / 240F; t < 3F; t += 1F / 240F) {
            float[] now = FinisherChoreography.crowd(t, 0, new float[WIDTH]);
            for (int j = 0; j < WIDTH; j++) assertTrue(Math.abs(now[j] - prev[j]) < 0.12F, "crowd channel " + j + " at " + t);
            assertTrue(now[FinisherChoreography.RISE] >= 0F);
            prev = now;
        }
        float[] cheering = FinisherChoreography.crowd(2F, 0, new float[WIDTH]);
        assertEquals(FinisherChoreography.CROWD_TO, cheering[FinisherChoreography.FORWARD], 1e-4);
        assertTrue(cheering[2 * 3] < -2.3F && cheering[3 * 3] < -2.3F, "both arms up");
    }

    @Test
    void killEffectLabelsCoverEveryEffect() {
        assertEquals(KillEffects.Effect.values().length + 2, KillEffects.LABELS.size());
        for (KillEffects.Effect e : KillEffects.Effect.values()) assertEquals(e, KillEffects.pick(e.label));
        assertEquals(null, KillEffects.pick("None"));
        assertTrue(KillEffects.pick("Random") != null);
    }
}
