package dev.jilaton.client.render.model;

import dev.jilaton.client.render.model.asset.ModelAssets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnifePoseTest {
    /** Mirrors CosmeticKnifeModule's drawn length and scale range. */
    private static final double DRAWN_LENGTH = 0.34;
    private static final double MIN_SCALE = 0.70;
    private static final double MAX_SCALE = 1.30;

    @Test
    void equipCompletesInTheRequestedTime() {
        double progress = 0;
        for (int step = 0; step < 120; step++) {
            progress = KnifePose.equip(progress, 0.28 / 120, 0.28, false);
        }
        assertEquals(1, progress, 1e-9);
    }

    @Test
    void equipIsFrameRateIndependent() {
        double coarse = KnifePose.equip(0, 0.05, 0.3, false);
        double fine = 0;
        for (int step = 0; step < 10; step++) fine = KnifePose.equip(fine, 0.005, 0.3, false);
        assertEquals(coarse, fine, 1e-12);
    }

    @Test
    void reducedMotionEquipsImmediately() {
        assertEquals(1, KnifePose.equip(0, 0.016, 0.3, true), 0);
    }

    @Test
    void knifeRisesWhileItIsEquipped() {
        KnifePose.Pose stowed = KnifePose.pose(0, 0, 0, KnifePose.DEFAULT_SIDE, -0.16, 0, 0);
        KnifePose.Pose held = KnifePose.pose(0, 0, 1, KnifePose.DEFAULT_SIDE, -0.16, 0, 0);
        assertTrue(held.up() > stowed.up(), "the knife should lift into view as it is equipped");
        assertTrue(held.forward() <= stowed.forward(), "the stowed knife sits further from the eye");
    }

    @Test
    void defaultPoseClearsTheCrosshair() {
        assertTrue(KnifePose.clearsCrosshair(KnifePose.DEFAULT_SIDE, KnifePose.DEFAULT_HALF_WIDTH,
                KnifePose.FORWARD));
    }

    @Test
    void aCentredKnifeWouldCoverTheCrosshair() {
        assertFalse(KnifePose.clearsCrosshair(0.02, 0.035, KnifePose.FORWARD));
    }

    @Test
    void worstCaseSwayBobAndScaleStillClearTheCrosshair() {
        double offset = 0.16 - 0.006 * 2 - 0.009 * 2;
        double halfWidth = 0.026 * 1.40;
        assertTrue(KnifePose.clearsCrosshair(offset, halfWidth, KnifePose.FORWARD + 0.04));
    }

    @Test
    void swayAndBobStayInsideTheirBounds() {
        for (int step = 0; step < 200; step++) {
            double phase = step * 0.05;
            KnifePose.Pose pose = KnifePose.pose(phase, phase * 1.7, 1, 0.26, -0.16, 2, 2);
            assertTrue(Math.abs(pose.right() - 0.26) <= 0.006 * 2 + 0.009 * 2 + 1e-9);
            assertTrue(Math.abs(pose.up() - (-0.16)) <= 0.004 * 2 + 0.011 * 2 + 1e-9);
        }
    }

    @Test
    void everyVariantClearsTheCrosshairAtTheWorstSetting() {
        double offset = 0.18 - 0.006 * 2 - 0.009 * 2;
        for (String id : ModelAssets.knifeIds()) {
            AssetMesh.Fitted fitted = AssetMesh.fit(ModelAssets.knife(id), DRAWN_LENGTH);
            double lateral = fitted.lateralHalf(KnifePose.HELD_ROLL) * fitted.scaleFor(MAX_SCALE);
            assertTrue(KnifePose.clearsCrosshair(offset, lateral, KnifePose.FORWARD + 0.04,
                    Math.abs(KnifePose.HELD_YAW)),
                    id + " reaches the crosshair at min side / max scale");
        }
    }

    @Test
    void everyVariantDrawsInsideTheConfiguredScaleBand() {
        for (String id : ModelAssets.knifeIds()) {
            var data = ModelAssets.knife(id);
            AssetMesh.Fitted fitted = AssetMesh.fit(data, DRAWN_LENGTH);
            double assetLongest = Math.max(data.width(), Math.max(data.height(), data.depth()));
            for (double userScale : new double[] {MIN_SCALE, 1.0, MAX_SCALE}) {
                double worldLongest = assetLongest * fitted.scaleFor(userScale);
                assertEquals(DRAWN_LENGTH * userScale, worldLongest, 1e-9,
                        id + " drawn length ignores the scale setting");
            }
            assertTrue(fitted.lateralHalf(KnifePose.HELD_ROLL) * fitted.scaleFor(MAX_SCALE)
                    < KnifePose.FORWARD, id + " a blade that wide cannot sit in front of the camera");
        }
    }

    @Test
    void aKnifeHeldOnTheViewAxisIsRejected() {
        AssetMesh.Fitted fitted = AssetMesh.fit(
                ModelAssets.knife("bayonet"), 0.34);
        double lateral = fitted.lateralHalf(KnifePose.HELD_ROLL) * fitted.scaleFor(1.30);
        assertFalse(KnifePose.clearsCrosshair(0.10, lateral, KnifePose.FORWARD + 0.04,
                Math.abs(KnifePose.HELD_YAW)));
    }

    @Test
    void swayPhaseFreezesUnderReducedMotion() {
        assertEquals(2.5, KnifePose.advance(2.5, 0.5, 3, true), 0);
        assertTrue(KnifePose.advance(0, 0.5, 3, false) > 0);
    }
}
