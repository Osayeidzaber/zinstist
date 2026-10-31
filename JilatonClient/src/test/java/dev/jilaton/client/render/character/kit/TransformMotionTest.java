package dev.jilaton.client.render.character.kit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure spring/pose-target math used by the layered character motion. */
class TransformMotionTest {
    @Test
    void aSpringConvergesOnTheTargetAndStaysFinite() {
        Motion motion = new Motion().spring(80.0F, 10.0F);
        float[] state = {0, 0, 0, 0, 0, 0};
        float[] target = {1.0F, -0.5F, 0.25F};

        for (int i = 0; i < 240; i++) {
            motion.step(state, 0, target, 1.0F / 120.0F);
        }

        for (int i = 0; i < 3; i++) {
            assertTrue(Float.isFinite(state[i]), "axis " + i + " went non-finite");
            assertTrue(Float.isFinite(state[i + 3]), "axis " + i + " velocity went non-finite");
            assertEquals(target[i], state[i], 0.02F, "axis " + i + " did not settle on its target");
        }
    }

    @Test
    void aLongFrameOrAnUnseededStateSnapsToTheTarget() {
        Motion motion = new Motion().spring(80.0F, 10.0F);
        float[] state = {9, 9, 9, 1, 1, 1};
        float[] target = {1.0F, 2.0F, 3.0F};

        motion.step(state, 0, target, 0.5F);
        for (int i = 0; i < 3; i++) {
            assertEquals(target[i], state[i], 0.0F, "a frame longer than 250 ms must snap");
            assertEquals(0.0F, state[i + 3], 0.0F, "a snap must clear the velocity");
        }

        float[] unseeded = {Float.NaN, 0, 0, 0, 0, 0};
        motion.step(unseeded, 0, target, 0.01F);
        assertEquals(1.0F, unseeded[0], 0.0F, "a NaN spring must recover by snapping");
    }

    @Test
    void softLimitsCompressButNeverExceedTheirAngle() {
        assertEquals(1.25F, Motion.soft(1.25F, 0.0F), 0.0F, "no limit passes the angle through");
        float limit = (float) Math.toRadians(30.0);
        float out = Motion.soft(1.0F, 30.0F);
        assertTrue(out > 0.0F && out < limit, "soft() must approach but stay below the limit: " + out);
        assertEquals(-out, Motion.soft(-1.0F, 30.0F), 1e-6F, "soft() must stay symmetric");
    }

    @Test
    void targetsAreClampedAndRestAnglesAreInRadians() {
        Motion motion = new Motion().rest(10.0F, 20.0F, 30.0F).limit(25.0F);
        Motion.Input input = new Motion.Input();
        float[] out = new float[3];

        motion.target(input, Bone.HEAD, out);
        assertEquals(Math.toRadians(10.0), out[0], 1e-6, "pitch uses the rest angle");
        assertEquals(Math.toRadians(20.0), out[1], 1e-6, "yaw uses the rest angle");
        assertEquals(Math.toRadians(25.0), out[2], 1e-6, "roll must be clamped to the limit");

        motion.rest(80.0F, -80.0F, 0.0F);
        motion.target(input, Bone.HEAD, out);
        assertEquals(Math.toRadians(25.0), out[0], 1e-6);
        assertEquals(Math.toRadians(-25.0), out[1], 1e-6);
    }

    @Test
    void followingALimbStaysWithinItsSoftLimit() {
        Motion motion = new Motion().follow(Bone.LEFT_LEG, 1.0F, 10.0F);
        Motion.Input input = new Motion.Input();
        input.pitch[Bone.LEFT_LEG.ordinal()] = (float) Math.toRadians(80.0);
        float[] out = new float[3];

        motion.target(input, Bone.RIGHT_LEG, out);
        assertTrue(Math.abs(out[0]) <= Math.toRadians(10.0) + 1e-6, "follow must respect its soft limit");
        assertTrue(out[0] > 0.0F, "the follower must move toward the followed limb");
    }
}
