package dev.jilaton.client.render.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyAnchorTest {
    private static final double EPSILON = 1e-9;

    @Test
    void backAnchorFollowsTheBodyYawAndIgnoresTheHead() {
        Transform back = BodyAnchor.compose(BodyAnchor.Part.BACK, 10, 20, 30, 90, 0, 0);
        assertEquals(-1, back.forwardX(), EPSILON);
        assertEquals(0, back.forwardY(), EPSILON);
        assertEquals(0, back.forwardZ(), EPSILON);
        assertEquals(0, back.rightX(), EPSILON);
        assertEquals(-1, back.rightZ(), EPSILON);
        assertEquals(1, back.upY(), EPSILON);

        Transform turnedHead = BodyAnchor.compose(BodyAnchor.Part.BACK, 10, 20, 30, 90, 170, 40);
        assertEquals(back.forwardX(), turnedHead.forwardX(), EPSILON);
        assertEquals(back.forwardZ(), turnedHead.forwardZ(), EPSILON);
    }

    @Test
    void headAnchorUsesTheHeadYawAndPitch() {
        Transform level = BodyAnchor.compose(BodyAnchor.Part.HEAD, 0, 0, 0, 0, 90, 0);
        assertEquals(-1, level.forwardX(), EPSILON);
        assertEquals(0, level.forwardY(), EPSILON);

        Transform down = BodyAnchor.compose(BodyAnchor.Part.HEAD, 0, 0, 0, 0, 0, 90);
        assertEquals(0, down.forwardX(), 1e-12);
        assertEquals(-1, down.forwardY(), EPSILON);
        assertEquals(0, down.forwardZ(), 1e-12);

        Transform up = BodyAnchor.compose(BodyAnchor.Part.HEAD, 0, 0, 0, 0, 0, -90);
        assertEquals(1, up.forwardY(), EPSILON);
    }

    @Test
    void headYawIsIndependentOfTheBodyYaw() {
        Transform head = BodyAnchor.compose(BodyAnchor.Part.HEAD, 0, 0, 0, 180, 0, 0);
        assertEquals(0, head.forwardX(), 1e-12);
        assertEquals(1, head.forwardZ(), EPSILON);
    }

    @Test
    void composedChildOffsetsLandOnTheAnchorAxes() {
        Transform head = BodyAnchor.compose(BodyAnchor.Part.HEAD, 3, 4, 5, 0, 90, 0);
        Transform offset = head.compose(Transform.translation(0, 0, 2));
        assertEquals(3 - 2, offset.originX(), EPSILON);
        assertEquals(4, offset.originY(), EPSILON);
        assertEquals(5, offset.originZ(), EPSILON);
    }

    @Test
    void backAnchorSitsBehindTheSpine() {
        Transform back = BodyAnchor.compose(BodyAnchor.Part.BACK, 0, 0, 0, 0, 0, 0);
        assertTrue(back.originZ() < -0.1, "back anchor should sit behind the torso, was " + back.originZ());
        Transform shoulders = BodyAnchor.compose(BodyAnchor.Part.SHOULDERS, 0, 0, 0, 0, 0, 0);
        assertTrue(shoulders.originZ() < 0 && shoulders.originZ() > back.originZ());
    }

    @Test
    void frameStaysOrthonormalForArbitraryAngles() {
        for (int yaw = -180; yaw <= 180; yaw += 37) {
            for (int pitch = -80; pitch <= 80; pitch += 23) {
                Transform t = BodyAnchor.compose(BodyAnchor.Part.HEAD, 0, 0, 0, 0, yaw, pitch);
                assertEquals(1, length(t.rightX(), t.rightY(), t.rightZ()), 1e-9);
                assertEquals(1, length(t.upX(), t.upY(), t.upZ()), 1e-9);
                assertEquals(1, length(t.forwardX(), t.forwardY(), t.forwardZ()), 1e-9);
                assertEquals(0, dot(t.rightX(), t.rightY(), t.rightZ(), t.forwardX(), t.forwardY(), t.forwardZ()), 1e-9);
                assertEquals(0, dot(t.rightX(), t.rightY(), t.rightZ(), t.upX(), t.upY(), t.upZ()), 1e-9);
            }
        }
    }

    private static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static double dot(double ax, double ay, double az, double bx, double by, double bz) {
        return ax * bx + ay * by + az * bz;
    }
}
