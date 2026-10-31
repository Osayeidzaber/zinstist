package dev.jilaton.client.render.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-math coverage for the two framed effects that assemble their parts by composing relative rotations.
 *
 * <p>Attachment frames from {@link BodyAnchor} are Minecraft-shaped (right, up, forward) triples and therefore
 * left-handed; {@code Transform.rotation} is a frame constructor of that same shape and is documented as
 * unusable as a relative rotation. The wing fan and the companion parts must keep their authored facing and
 * handedness: the fan opens outward on both sides, and every companion primitive shares one handedness so the
 * submit path shades (and culls) all of them the same way.
 */
class EffectFrameOrientationTest {
    private static final double EPS = 1e-9;
    private static final double[] FANS = {78, 40, 0, -8};
    private static final double[] FLAPS = {-10.3, 0, 9.2};

    @Test
    void wingFeathersFanOutwardOnBothSides() {
        for (float yaw : new float[]{0, 90, -135}) {
            Transform body = BodyAnchor.compose(BodyAnchor.Part.BACK, 0, 0, 0, yaw, 0, 0);
            for (int side : new int[]{-1, 1}) {
                Transform pivot = body.translate(side * 0.10, 0.12, 0.02);
                double[] outward = {body.rightX() * side, body.rightY() * side, body.rightZ() * side};
                for (double fan : FANS) {
                    for (double flap : FLAPS) {
                        Transform feather = WingMotion.featherFrame(pivot, side, fan, flap);
                        double[] forward = forward(feather);
                        double outwardDot = dot(forward, outward);
                        assertTrue(outwardDot > 0.05,
                                "side " + side + " fan " + fan + " flap " + flap
                                        + " points across the spine: outward=" + outwardDot);
                        if (fan < 0) {
                            assertTrue(outwardDot > 0.9, "the tip feather must be the most lateral one");
                        }
                        assertEquals(1.0, determinant(feather), EPS,
                                "handedness must stay at +1 so winding and lighting are unchanged");
                    }
                }
            }
        }
    }

    @Test
    void wingFanRunsFromNearlyVerticalToBelowHorizontal() {
        Transform body = BodyAnchor.compose(BodyAnchor.Part.BACK, 0, 0, 0, 0, 0, 0);
        Transform pivot = body.translate(0.10, 0.12, 0.02);
        double baseFlap = Math.toDegrees(WingMotion.baseFlap("Feather"));
        Transform leading = WingMotion.featherFrame(pivot, 1, WingMotion.fanDegrees(0, 6), baseFlap);
        Transform tip = WingMotion.featherFrame(pivot, 1, WingMotion.fanDegrees(5, 6), baseFlap);
        assertTrue(forward(leading)[1] > 0.5, "the leading feather must be nearly vertical");
        assertTrue(forward(tip)[1] < 0, "the tip feather must sit below horizontal");
    }

    @Test
    void wingFlapSweepsContinuouslyWithoutAPop() {
        Transform pivot = BodyAnchor.compose(BodyAnchor.Part.BACK, 0, 0, 0, 0, 0, 0)
                .translate(0.10, 0.12, 0.02);
        double[] previous = null;
        for (int step = -60; step <= 60; step++) {
            double[] current = forward(WingMotion.featherFrame(pivot, 1, 40, step * 1.0));
            if (previous != null) {
                assertTrue(dot(previous, current) > 0.9, "the flap must not flip the feather at step " + step);
            }
            previous = current;
        }
    }

    @Test
    void leftAndRightFeathersAreMirrorImages() {
        Transform pivot = BodyAnchor.compose(BodyAnchor.Part.BACK, 0, 0, 0, 0, 0, 0);
        for (double fan : FANS) {
            for (double flap : FLAPS) {
                double[] right = forward(WingMotion.featherFrame(pivot.translate(0.10, 0, 0), 1, fan, flap));
                double[] left = forward(WingMotion.featherFrame(pivot.translate(-0.10, 0, 0), -1, fan, flap));
                assertEquals(-right[0], left[0], 1e-9, "fan " + fan + " flap " + flap + " x");
                assertEquals(right[1], left[1], 1e-9, "fan " + fan + " flap " + flap + " y");
                assertEquals(right[2], left[2], 1e-9, "fan " + fan + " flap " + flap + " z");
            }
        }
    }

    @Test
    void companionFramesShareOneHandednessAndFaceTheMotion() {
        double vx = 0.4, vz = 0.9, bank = 12;
        Transform base = CompanionMotion.bankedFrame(1, 2, 3, vx, 0, vz, bank);
        assertEquals(1.0, determinant(base), EPS, "the banked core must not be mirrored");
        double length = Math.hypot(vx, vz);
        assertEquals(vx / length, base.forwardX(), 1e-9, "forward follows the velocity x");
        assertEquals(0.0, base.forwardY(), 1e-9, "forward follows the velocity y");
        assertEquals(vz / length, base.forwardZ(), 1e-9, "forward follows the velocity z");

        double cos = Math.cos(Math.toRadians(bank));
        double[] up = {base.upX(), base.upY(), base.upZ()};
        double[] right = {base.rightX(), base.rightY(), base.rightZ()};
        double[] facing = forward(base);
        assertEquals(cos, up[1], 1e-9, "the bank only rolls about the facing axis");
        assertTrue(up[0] > 0.1, "a positive bank (sliding to its own right) must lift the right side");
        assertTrue(crossDot(right, up, facing) > 0.99, "the base must be a proper right-handed frame");

        assertEquals(1.0, determinant(CompanionMotion.ringFrame(base, 0.7)), EPS,
                "the ring shares the core's handedness");

        double angle = 1.1, orbit = 0.21, bob = 0.01;
        Transform shard = CompanionMotion.shardFrame(base, angle, orbit, bob);
        assertEquals(1.0, determinant(shard), EPS, "the shard shares the core's handedness");
        double[] expected = base.point(Math.cos(angle) * orbit, bob, Math.sin(angle) * orbit);
        assertEquals(expected[0], shard.originX(), 1e-9, "shard orbit x");
        assertEquals(expected[1], shard.originY(), 1e-9, "shard orbit y");
        assertEquals(expected[2], shard.originZ(), 1e-9, "shard orbit z");
    }

    @Test
    void companionRingTiltsAboutTheBodyRightAxis() {
        Transform base = CompanionMotion.bankedFrame(0, 0, 0, 0.4, 0, 0.9, 0);
        Transform ring = CompanionMotion.ringFrame(base, 0.7);
        double cos72 = Math.cos(Math.toRadians(72)), sin72 = Math.sin(Math.toRadians(72));
        assertEquals(cos72, dot(new double[]{ring.upX(), ring.upY(), ring.upZ()},
                new double[]{base.upX(), base.upY(), base.upZ()}), 1e-9, "the ring keeps 72 degrees of tilt");
        assertEquals(sin72, dot(new double[]{ring.upX(), ring.upY(), ring.upZ()}, forward(base)), 1e-9,
                "the tilt tips the ring axis toward the body's forward");
    }

    private static double[] forward(Transform transform) {
        return new double[]{transform.forwardX(), transform.forwardY(), transform.forwardZ()};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double crossDot(double[] a, double[] b, double[] c) {
        return (a[1] * b[2] - a[2] * b[1]) * c[0]
                + (a[2] * b[0] - a[0] * b[2]) * c[1]
                + (a[0] * b[1] - a[1] * b[0]) * c[2];
    }

    private static double determinant(Transform t) {
        return t.rightX() * (t.upY() * t.forwardZ() - t.upZ() * t.forwardY())
                - t.upX() * (t.rightY() * t.forwardZ() - t.rightZ() * t.forwardY())
                + t.forwardX() * (t.rightY() * t.upZ() - t.rightZ() * t.upY());
    }
}
