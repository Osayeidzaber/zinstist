package dev.jilaton.client.module.impl;

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
}
