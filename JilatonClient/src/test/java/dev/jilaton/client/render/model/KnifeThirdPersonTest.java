package dev.jilaton.client.render.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnifeThirdPersonTest {
    private static final Transform WRIST = Transform.translation(1, 2, 3);
    private static final KnifeMotion.Pose REST = new KnifeMotion.Pose(0, 0, 0, 0, 0, 0, 0);

    @Test
    void aRestPoseLeavesTheKnifeExactlyAtTheWrist() {
        Transform knife = KnifeThirdPerson.hand(WRIST, REST);
        assertEquals(1, knife.originX(), 1e-9);
        assertEquals(2, knife.originY(), 1e-9);
        assertEquals(3, knife.originZ(), 1e-9);
        assertEquals(WRIST.rightX(), knife.rightX(), 1e-9);
        assertEquals(WRIST.forwardZ(), knife.forwardZ(), 1e-9);
    }

    @Test
    void firstPersonTranslationShrinksToHandSize() {
        Transform knife = KnifeThirdPerson.hand(WRIST, new KnifeMotion.Pose(0.4, -0.8, 0.2, 0, 0, 0, 0));
        assertEquals(1 + 0.4 * KnifeThirdPerson.TRANSLATION, knife.originX(), 1e-9);
        assertEquals(2 - 0.8 * KnifeThirdPerson.TRANSLATION, knife.originY(), 1e-9);
        assertEquals(3 + 0.2 * KnifeThirdPerson.TRANSLATION, knife.originZ(), 1e-9);
    }

    @Test
    void rotationsTurnTheBladeAwayFromTheWristForwardAndStayOrthonormal() {
        Transform knife = KnifeThirdPerson.hand(WRIST, new KnifeMotion.Pose(0, 0, 0, 30, 45, -60, 360));
        assertNotEquals(WRIST.forwardX(), knife.forwardX(), "yaw/pitch must change the blade direction");
        double rightLength = Math.sqrt(sq(knife.rightX()) + sq(knife.rightY()) + sq(knife.rightZ()));
        double upLength = Math.sqrt(sq(knife.upX()) + sq(knife.upY()) + sq(knife.upZ()));
        double forwardLength = Math.sqrt(sq(knife.forwardX()) + sq(knife.forwardY()) + sq(knife.forwardZ()));
        assertEquals(1, rightLength, 1e-9);
        assertEquals(1, upLength, 1e-9);
        assertEquals(1, forwardLength, 1e-9);
        double dot = knife.rightX() * knife.upX() + knife.rightY() * knife.upY() + knife.rightZ() * knife.upZ();
        assertEquals(0, dot, 1e-9);
    }

    @Test
    void aFullSpinLandsTheBladeBackOnTheSameAxis() {
        Transform spun = KnifeThirdPerson.hand(WRIST, new KnifeMotion.Pose(0, 0, 0, 0, 0, 0, 360));
        assertTrue(Math.abs(spun.forwardX() - WRIST.forwardX()) < 1e-9);
        assertTrue(Math.abs(spun.forwardY() - WRIST.forwardY()) < 1e-9);
        assertTrue(Math.abs(spun.forwardZ() - WRIST.forwardZ()) < 1e-9);
    }

    private static double sq(double value) {
        return value * value;
    }
}
