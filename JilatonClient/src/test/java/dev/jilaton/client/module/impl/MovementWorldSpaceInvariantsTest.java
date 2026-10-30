package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Behavioural invariants for the world-space movement policies at every cardinal and near-wrap yaw. */
class MovementWorldSpaceInvariantsTest {
    private static final double[] YAWS = {0.0, 90.0, 180.0, 270.0, 179.9, -179.9};
    private static final double EPSILON = 1.0E-9;

    /** Vanilla forward for a yaw: 0 = +Z, 90 = -X, 180 = -Z, 270 = +X. */
    private static Vec3 look(float yawDegrees) {
        double radians = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
    }

    @Test
    void reversalAppliedTwiceReturnsTheOriginalVectorAtEveryYaw() {
        for (double yaw : YAWS) {
            Vec3 original = look((float) yaw).scale(0.42).add(0.0, 0.3, 0.0);
            VelocityReversalPolicy.Motion once =
                    VelocityReversalPolicy.apply(original.x, original.y, original.z, 1.0, 1.0);
            VelocityReversalPolicy.Motion twice =
                    VelocityReversalPolicy.apply(once.x(), once.y(), once.z(), 1.0, 1.0);

            assertEquals(-original.x, once.x(), EPSILON, "first reversal x at yaw " + yaw);
            assertEquals(-original.z, once.z(), EPSILON, "first reversal z at yaw " + yaw);
            assertEquals(original.x, twice.x(), EPSILON, "double reversal x at yaw " + yaw);
            assertEquals(original.z, twice.z(), EPSILON, "double reversal z at yaw " + yaw);
            assertEquals(original.y, twice.y(), EPSILON, "vertical motion must survive at yaw " + yaw);
        }
    }

    @Test
    void jumpResetFiresOnlyOnTheGroundedHurtWindowTransition() {
        // The hurt window is a tick decision, so it must not depend on the direction the player faces.
        for (double yaw : YAWS) {
            int firingTicks = 0;
            for (int hurtTime = 10; hurtTime >= 0; hurtTime--) {
                if (VelocityJumpResetPolicy.shouldJump(hurtTime, true, true, false, 2, 2, 0, 100)) {
                    firingTicks++;
                }
            }

            assertEquals(1, firingTicks, "exactly one grounded tick in the hurt window at yaw " + yaw);
            assertFalse(VelocityJumpResetPolicy.shouldJump(9, false, true, false, 2, 2, 0, 100),
                    "still airborne at the window tick must not jump at yaw " + yaw);
            assertFalse(VelocityJumpResetPolicy.shouldJump(7, true, true, false, 2, 2, 0, 100),
                    "landing outside the window must not jump at yaw " + yaw);
        }
    }

    @Test
    void facingGateIsWrapInvariantAndSignCorrectAtQuarterTurns() {
        for (double yaw : YAWS) {
            Vec3 direction = look((float) yaw);
            Vec3 wrapped = look((float) (yaw + 360.0));
            AABB ahead = boxAround(direction.scale(3.0), 0.3);
            AABB behind = boxAround(direction.scale(-3.0), 0.3);

            assertTrue(VelocityFacingPolicy.rayIntersectsTarget(Vec3.ZERO, direction, 3.5, ahead),
                    "target ahead at yaw " + yaw);
            assertFalse(VelocityFacingPolicy.rayIntersectsTarget(Vec3.ZERO, direction, 3.5, behind),
                    "target behind at yaw " + yaw);
            assertEquals(VelocityFacingPolicy.rayIntersectsTarget(Vec3.ZERO, direction, 3.5, ahead),
                    VelocityFacingPolicy.rayIntersectsTarget(Vec3.ZERO, wrapped, 3.5, ahead),
                    "a 360 degree wrap must not change the gate at yaw " + yaw);
        }

        AABB west = boxAround(new Vec3(-3.0, 0.0, 0.0), 0.3);
        AABB east = boxAround(new Vec3(3.0, 0.0, 0.0), 0.3);
        assertTrue(VelocityFacingPolicy.rayIntersectsTarget(Vec3.ZERO, look(90.0F), 3.5, west));
        assertFalse(VelocityFacingPolicy.rayIntersectsTarget(Vec3.ZERO, look(90.0F), 3.5, east));
        assertFalse(VelocityFacingPolicy.rayIntersectsTarget(Vec3.ZERO, look(270.0F), 3.5, west));
        assertTrue(VelocityFacingPolicy.rayIntersectsTarget(Vec3.ZERO, look(270.0F), 3.5, east));
    }

    @Test
    void strafeMathMatchesVanillasInputVectorRotationAtEveryYaw() {
        boolean[][] inputs = {
                {true, false, false, false},
                {false, true, false, false},
                {false, false, true, false},
                {false, false, false, true},
                {true, false, true, false},
                {true, false, false, true}
        };
        double speed = 0.42;
        for (double yaw : YAWS) {
            float castYaw = (float) yaw;
            double sin = Math.sin(Math.toRadians(castYaw));
            double cos = Math.cos(Math.toRadians(castYaw));
            Vec3 heading = look(castYaw);
            Vec3 movement = new Vec3(heading.x * speed, -0.08, heading.z * speed);
            for (boolean[] keys : inputs) {
                double xxa = (keys[2] ? 1.0 : 0.0) - (keys[3] ? 1.0 : 0.0);
                double zza = (keys[0] ? 1.0 : 0.0) - (keys[1] ? 1.0 : 0.0);
                double length = Math.hypot(xxa, zza);
                double vanillaX = (xxa / length * cos - zza / length * sin) * speed;
                double vanillaZ = (zza / length * cos + xxa / length * sin) * speed;

                Vec3 aligned = StrafeMath.apply(movement, (float) yaw, zza, xxa, 1.0, false);
                assertEquals(vanillaX, aligned.x, EPSILON, "StrafeMath x at yaw " + yaw);
                assertEquals(vanillaZ, aligned.z, EPSILON, "StrafeMath z at yaw " + yaw);
                assertEquals(-0.08, aligned.y, EPSILON, "StrafeMath must preserve vertical motion");
                assertEquals(speed, Math.hypot(aligned.x, aligned.z), EPSILON,
                        "StrafeMath must keep the horizontal speed at yaw " + yaw);

                VelocityStrafeMath.Horizontal reoriented = VelocityStrafeMath.reorient(0.0, 0.0, (float) yaw,
                        keys[0], keys[1], keys[2], keys[3], speed);
                assertEquals(vanillaX, reoriented.x(), EPSILON, "VelocityStrafeMath x at yaw " + yaw);
                assertEquals(vanillaZ, reoriented.z(), EPSILON, "VelocityStrafeMath z at yaw " + yaw);
            }
        }
    }

    @Test
    void quarterTurnSignsAreCorrectForBothStrafePaths() {
        Vec3 heading = new Vec3(0.0, 0.0, 0.42);
        assertHorizontal(-0.42, 0.0, StrafeMath.apply(heading, 90.0F, 1.0, 0.0, 1.0, false));
        assertHorizontal(0.42, 0.0, StrafeMath.apply(heading, 270.0F, 1.0, 0.0, 1.0, false));
        assertHorizontal(0.0, 0.42, StrafeMath.apply(heading, 90.0F, 0.0, 1.0, 1.0, false));
        assertHorizontal(0.0, -0.42, StrafeMath.apply(heading, 90.0F, 0.0, -1.0, 1.0, false));

        assertHorizontal(-0.42, 0.0,
                VelocityStrafeMath.reorient(0.0, 1.0, 90.0F, true, false, false, false, 0.42));
        assertHorizontal(0.42, 0.0,
                VelocityStrafeMath.reorient(0.0, 1.0, 270.0F, true, false, false, false, 0.42));
        assertHorizontal(0.0, 0.42,
                VelocityStrafeMath.reorient(0.0, 1.0, 90.0F, false, false, true, false, 0.42));
        assertHorizontal(0.0, -0.42,
                VelocityStrafeMath.reorient(0.0, 1.0, 90.0F, false, false, false, true, 0.42));
    }

    private static AABB boxAround(Vec3 center, double halfWidth) {
        return new AABB(center.x - halfWidth, center.y - 0.5, center.z - halfWidth,
                center.x + halfWidth, center.y + 0.5, center.z + halfWidth);
    }

    private static void assertHorizontal(double expectedX, double expectedZ, Vec3 actual) {
        assertEquals(expectedX, actual.x, EPSILON);
        assertEquals(expectedZ, actual.z, EPSILON);
    }

    private static void assertHorizontal(double expectedX, double expectedZ, VelocityStrafeMath.Horizontal actual) {
        assertEquals(expectedX, actual.x(), EPSILON);
        assertEquals(expectedZ, actual.z(), EPSILON);
    }
}
