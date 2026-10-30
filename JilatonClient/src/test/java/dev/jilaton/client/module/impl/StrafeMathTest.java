package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StrafeMathTest {
    @Test
    void alignsHorizontalVectorToForwardWithoutChangingItsSpeedOrVerticalMotion() {
        Vec3 adjusted = StrafeMath.apply(new Vec3(0.0, -0.2, 0.4), 0.0F,
                1.0, 0.0, 1.0, false);

        assertEquals(0.0, adjusted.x, 1.0E-9);
        assertEquals(-0.2, adjusted.y, 1.0E-9);
        assertEquals(0.4, adjusted.z, 1.0E-9);
    }

    @Test
    void interpolatesDirectionAndPreservesHorizontalSpeed() {
        Vec3 adjusted = StrafeMath.apply(new Vec3(0.4, 0.1, 0.0), 0.0F,
                1.0, 0.0, 0.5, false);

        assertEquals(0.2, adjusted.x, 1.0E-9);
        assertEquals(0.1, adjusted.y, 1.0E-9);
        assertEquals(Math.sqrt(0.08), Math.hypot(adjusted.x, adjusted.z), 1.0E-9);
    }

    @Test
    void strictMovementStopsHorizontalMotionWithoutInput() {
        Vec3 adjusted = StrafeMath.apply(new Vec3(0.3, -0.1, -0.4), 90.0F,
                0.0, 0.0, 1.0, true);

        assertEquals(Vec3.ZERO, new Vec3(adjusted.x, 0.0, adjusted.z));
        assertEquals(-0.1, adjusted.y, 1.0E-9);
    }

    @Test
    void nonStrictMovementLeavesVectorUnchangedWithoutInput() {
        Vec3 original = new Vec3(-0.3, 0.2, 0.5);

        assertEquals(original, StrafeMath.apply(original, 0.0F, 0.0, 0.0, 1.0, false));
    }

    @Test
    void sidewaysInputFollowsTheMinecraftLeftPositiveConvention() {
        double diagonal = 0.4 / Math.sqrt(2.0);
        Vec3 left = StrafeMath.apply(new Vec3(0.0, 0.0, 0.4), 0.0F, 1.0, 1.0, 1.0, false);
        Vec3 right = StrafeMath.apply(new Vec3(0.0, 0.0, 0.4), 0.0F, 1.0, -1.0, 1.0, false);
        Vec3 westFacingLeft = StrafeMath.apply(new Vec3(0.4, 0.0, 0.0), 90.0F, 1.0, 1.0, 1.0, false);

        assertEquals(diagonal, left.x, 1.0E-9);
        assertEquals(diagonal, left.z, 1.0E-9);
        assertEquals(-diagonal, right.x, 1.0E-9);
        assertEquals(diagonal, right.z, 1.0E-9);
        assertEquals(-diagonal, westFacingLeft.x, 1.0E-9);
        assertEquals(diagonal, westFacingLeft.z, 1.0E-9);
    }

    @Test
    void yawWrapKeepsTheSameWorldDirection() {
        Vec3 velocity = new Vec3(0.3, -0.1, 0.4);
        Vec3 atPositive = StrafeMath.apply(velocity, 180.0F, 1.0, 1.0, 1.0, false);
        Vec3 atNegative = StrafeMath.apply(velocity, -180.0F, 1.0, 1.0, 1.0, false);
        Vec3 justBelow = StrafeMath.apply(velocity, 179.9F, 1.0, 1.0, 1.0, false);
        Vec3 justAbove = StrafeMath.apply(velocity, -180.1F, 1.0, 1.0, 1.0, false);

        assertEquals(atPositive.x, atNegative.x, 1.0E-9);
        assertEquals(atPositive.z, atNegative.z, 1.0E-9);
        assertEquals(justBelow.x, justAbove.x, 1.0E-2);
        assertEquals(justBelow.z, justAbove.z, 1.0E-2);
        assertEquals(-0.1, atPositive.y, 1.0E-9);
    }

    @Test
    void targetStrafeLocalInputRoundTripsThroughThisRotation() {
        Vec3 orbitVelocity = new Vec3(0.0, 0.0, 0.3);

        for (float yaw : new float[]{0.0F, 90.0F, 179.5F, -179.5F}) {
            Vec2 local = TargetStrafeMath.movementInput(2.95, 0.0, 0.0, 0.0, yaw, 2.95, 1, 1.0);
            Vec3 adjusted = StrafeMath.apply(orbitVelocity, yaw, local.y, local.x, 1.0, false);

            assertEquals(0.0, adjusted.x, 1.0E-6);
            assertEquals(-0.3, adjusted.z, 1.0E-6);
        }
    }
}
