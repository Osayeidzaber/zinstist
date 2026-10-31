package dev.jilaton.client.render.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-math coverage for frame composition, body pivots, pose transforms and hand attachment. These tests do not
 * touch Minecraft render state; they pin the frame convention down so a mirroring or double-rotation cannot sneak
 * back in.
 */
class TransformMathTest {
    private static final double EPS = 1e-9;
    private static final BodyAnchor.Part[] FRAME_PARTS = {
            BodyAnchor.Part.TORSO,
            BodyAnchor.Part.BACK,
            BodyAnchor.Part.SHOULDERS,
            BodyAnchor.Part.RIGHT_ARM,
            BodyAnchor.Part.LEFT_ARM,
            BodyAnchor.Part.RIGHT_HAND,
            BodyAnchor.Part.LEFT_HAND
    };

    @Test
    void localRotationIsAProperRotation() {
        double[][] angles = {{0, 0, 0}, {37, -22, 61}, {-150, 80, -179}, {90, 0, 0}, {0, 90, 0}, {0, 0, 90}};
        for (double[] a : angles) {
            Transform t = Transform.localRotation(a[0], a[1], a[2]);
            assertEquals(1.0, determinant(t), 1e-9, "local rotation must not mirror: " + a[0] + "," + a[1] + "," + a[2]);
            assertOrthonormal(t);
        }

        Transform identity = Transform.localRotation(0, 0, 0);
        assertBasis(Transform.identity(), identity, EPS);
        double[] up = Transform.localRotation(0, 90, 0).point(0, 1, 0);
        assertVector(0, 0, 1, up, 1e-9, "pitch 90 must tip up onto forward");
    }

    @Test
    void rotationBuildsTheMinecraftFacingFrame() {
        Transform level = Transform.rotation(0, 0, 0);
        assertVector(-1, 0, 0, new double[]{level.rightX(), level.rightY(), level.rightZ()}, EPS, "right");
        assertVector(0, 1, 0, new double[]{level.upX(), level.upY(), level.upZ()}, EPS, "up");
        assertVector(0, 0, 1, new double[]{level.forwardX(), level.forwardY(), level.forwardZ()}, EPS, "forward");
        assertEquals(-1.0, determinant(level), EPS, "the right/up/forward frame is left-handed");

        Transform west = Transform.rotation(90, 0, 0);
        assertVector(-1, 0, 0, new double[]{west.forwardX(), west.forwardY(), west.forwardZ()}, EPS, "yaw 90 faces -X");
        assertVector(0, 0, -1, new double[]{west.rightX(), west.rightY(), west.rightZ()}, EPS, "yaw 90 turns right to -Z");

        Transform down = Transform.rotation(0, 90, 0);
        assertVector(0, -1, 0, new double[]{down.forwardX(), down.forwardY(), down.forwardZ()}, EPS, "pitch 90 looks down");
    }

    @Test
    void compositionAppliesTheChildInTheParentFrame() {
        Transform parent = Transform.translation(10, 20, 30).rotate(90, 0, 0);
        Transform child = Transform.translation(2, 0, 0);
        Transform composed = parent.compose(child);

        double[] expected = parent.point(2, 0, 0);
        assertVector(expected[0], expected[1], expected[2],
            new double[]{composed.originX(), composed.originY(), composed.originZ()}, EPS, "composed origin");
        assertVector(0, 0, -1, new double[]{composed.rightX(), composed.rightY(), composed.rightZ()}, EPS,
            "a translated child keeps the parent axes");
    }

    @Test
    void restPoseFramesMatchTheDirectAnchorsAndStayLeftHanded() {
        for (BodyAnchor.Part part : FRAME_PARTS) {
            Transform direct = BodyAnchor.compose(part, 0, 0, 0, 0, 0, 0);
            Transform posed = BodyAnchor.composePose(part, 0, 0, 0, 1.8, 0, 0, 0, false, 0, 0, 0);
            assertBasis(direct, posed, EPS, part + " at rest");
            assertEquals(-1.0, determinant(posed), EPS, part + " lost the attachment frame convention");
        }
    }

    @Test
    void poseFramesFollowTheBodyYawInsteadOfMirroringIt() {
        for (float yaw : new float[]{-135, 45, 90}) {
            Transform direct = BodyAnchor.compose(BodyAnchor.Part.BACK, 0, 0, 0, yaw, 0, 0);
            Transform posed = BodyAnchor.composePose(
                BodyAnchor.Part.BACK, 0, 0, 0, 1.8, yaw, 0, 0, false, 0, 0, 0);
            assertBasis(direct, posed, 1e-9, "body yaw " + yaw);
        }
    }

    @Test
    void crouchedTorsoPivotsForwardAndKeepsTheBodyRightAxis() {
        Transform body = BodyAnchor.compose(BodyAnchor.Part.BACK, 0, 0, 0, 37, 0, 0);
        Transform torso = BodyAnchor.composePose(
            BodyAnchor.Part.TORSO, 0, 0, 0, 1.8, 37, 0, 0, true, 0, 0, 0);
        assertVector(body.rightX(), body.rightY(), body.rightZ(),
            new double[]{torso.rightX(), torso.rightY(), torso.rightZ()}, 1e-9,
            "a forward pitch must turn about the body's right axis");
        assertTrue(torso.forwardY() < -0.4, "crouching must pitch the torso forward/down");
        assertEquals(-1.0, determinant(torso), EPS);
    }

    @Test
    void handAnchorsHangBelowTheirShoulderOnTheCorrectSide() {
        Transform right = BodyAnchor.composePose(BodyAnchor.Part.RIGHT_HAND, 0, 0, 0, 1.8, 0, 0, 0, false, 0, 0, 0);
        Transform left = BodyAnchor.composePose(BodyAnchor.Part.LEFT_HAND, 0, 0, 0, 1.8, 0, 0, 0, false, 0, 0, 0);
        Transform rightArm = BodyAnchor.composePose(BodyAnchor.Part.RIGHT_ARM, 0, 0, 0, 1.8, 0, 0, 0, false, 0, 0, 0);
        assertTrue(right.originX() < 0, "right hand sits on -X while facing +Z");
        assertTrue(left.originX() > 0, "left hand sits on +X while facing +Z");
        assertEquals(rightArm.originY() - 0.62, right.originY(), 1e-9, "the hand hangs one arm below the shoulder");
        assertEquals(right.originX(), rightArm.originX(), 1e-9);
    }

    @Test
    void knifeAttachmentTranslatesInWristSpaceAndPreservesHandednessAndContinuity() {
        Transform wrist = BodyAnchor.compose(BodyAnchor.Part.RIGHT_HAND, 0, 0, 0, 35, 0, 0);
        KnifeMotion.Pose rest = new KnifeMotion.Pose(0, 0, 0, 0, 0, 0, 0);
        KnifeMotion.Pose offset = new KnifeMotion.Pose(0.4, -0.2, 0.0, 0, 0, 0, 0);

        Transform moved = KnifeThirdPerson.hand(wrist, offset);
        double k = KnifeThirdPerson.TRANSLATION;
        assertVector(
            wrist.originX() + wrist.rightX() * 0.4 * k + wrist.upX() * -0.2 * k,
            wrist.originY() + wrist.rightY() * 0.4 * k + wrist.upY() * -0.2 * k,
            wrist.originZ() + wrist.rightZ() * 0.4 * k + wrist.upZ() * -0.2 * k,
            new double[]{moved.originX(), moved.originY(), moved.originZ()}, EPS, "translation follows the wrist");

        Transform planted = KnifeThirdPerson.hand(wrist, rest);
        assertBasis(wrist, planted, EPS, "rest pose");

        KnifeMotion.Pose tinyRoll = new KnifeMotion.Pose(0, 0, 0, 0, 0, 0.001, 0);
        Transform almost = KnifeThirdPerson.hand(wrist, tinyRoll);
        assertBasis(planted, almost, 1e-4, "a tiny pose must not pop to the mirrored frame");

        KnifeMotion.Pose full = new KnifeMotion.Pose(0.1, 0.05, 0, 30, 20, 15, 360);
        Transform posed = KnifeThirdPerson.hand(wrist, full);
        assertEquals(-1.0, determinant(wrist), EPS);
        assertEquals(-1.0, determinant(posed), EPS, "the knife must not be mirrored by its pose");
    }

    @Test
    void mirrorXFlipsHandednessWithoutTouchingTheFacingOrUpAxes() {
        Transform oriented = Transform.translation(1, 2, 3)
                .compose(Transform.lookAlong(0.3, 0.2, 1, 0, 1, 0));
        Transform mirrored = oriented.compose(Transform.mirrorX());

        assertEquals(-determinant(oriented), determinant(mirrored), EPS, "one mirror flips the handedness");
        assertVector(oriented.originX(), oriented.originY(), oriented.originZ(),
                new double[]{mirrored.originX(), mirrored.originY(), mirrored.originZ()}, EPS, "origin");
        assertVector(oriented.upX(), oriented.upY(), oriented.upZ(),
                new double[]{mirrored.upX(), mirrored.upY(), mirrored.upZ()}, EPS, "up");
        assertVector(oriented.forwardX(), oriented.forwardY(), oriented.forwardZ(),
                new double[]{mirrored.forwardX(), mirrored.forwardY(), mirrored.forwardZ()}, EPS, "forward");
        assertVector(-oriented.rightX(), -oriented.rightY(), -oriented.rightZ(),
                new double[]{mirrored.rightX(), mirrored.rightY(), mirrored.rightZ()}, EPS, "right");
        assertBasis(oriented, oriented.compose(Transform.mirrorX()).compose(Transform.mirrorX()), EPS,
                "two mirrors cancel");
    }

    private static void assertBasis(Transform expected, Transform actual, double eps) {
        assertBasis(expected, actual, eps, "basis");
    }

    private static void assertBasis(Transform expected, Transform actual, double eps, String what) {
        assertVector(expected.rightX(), expected.rightY(), expected.rightZ(),
            new double[]{actual.rightX(), actual.rightY(), actual.rightZ()}, eps, what + " right");
        assertVector(expected.upX(), expected.upY(), expected.upZ(),
            new double[]{actual.upX(), actual.upY(), actual.upZ()}, eps, what + " up");
        assertVector(expected.forwardX(), expected.forwardY(), expected.forwardZ(),
            new double[]{actual.forwardX(), actual.forwardY(), actual.forwardZ()}, eps, what + " forward");
    }

    private static void assertVector(double x, double y, double z, double[] actual, double eps, String what) {
        assertEquals(x, actual[0], eps, what + " x");
        assertEquals(y, actual[1], eps, what + " y");
        assertEquals(z, actual[2], eps, what + " z");
    }

    private static void assertOrthonormal(Transform t) {
        double[] r = {t.rightX(), t.rightY(), t.rightZ()};
        double[] u = {t.upX(), t.upY(), t.upZ()};
        double[] f = {t.forwardX(), t.forwardY(), t.forwardZ()};
        assertEquals(1, dot(r, r), 1e-9);
        assertEquals(1, dot(u, u), 1e-9);
        assertEquals(1, dot(f, f), 1e-9);
        assertEquals(0, dot(r, u), 1e-9);
        assertEquals(0, dot(r, f), 1e-9);
        assertEquals(0, dot(u, f), 1e-9);
    }

    private static double determinant(Transform t) {
        return t.rightX() * (t.upY() * t.forwardZ() - t.upZ() * t.forwardY())
            - t.upX() * (t.rightY() * t.forwardZ() - t.rightZ() * t.forwardY())
            + t.forwardX() * (t.rightY() * t.upZ() - t.rightZ() * t.upY());
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
}
