package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityFacingPolicyTest {
    private static final Vec3 ORIGIN = Vec3.ZERO;
    private static final Vec3 FORWARD = new Vec3(0.0, 0.0, 1.0);

    @Test
    void acceptsTargetsOnTheForwardRayWithinRange() {
        assertTrue(VelocityFacingPolicy.rayIntersectsTarget(ORIGIN, FORWARD, 3.5,
                new AABB(-0.3, -0.3, 2.0, 0.3, 0.3, 2.6)));
    }

    @Test
    void rejectsOffRayOutOfRangeAndInvalidInputs() {
        assertFalse(VelocityFacingPolicy.rayIntersectsTarget(ORIGIN, FORWARD, 3.5,
                new AABB(1.0, -0.3, 2.0, 1.6, 0.3, 2.6)));
        assertFalse(VelocityFacingPolicy.rayIntersectsTarget(ORIGIN, FORWARD, 3.5,
                new AABB(-0.3, -0.3, 4.0, 0.3, 0.3, 4.6)));
        assertFalse(VelocityFacingPolicy.rayIntersectsTarget(ORIGIN, Vec3.ZERO, 3.5,
                new AABB(-0.3, -0.3, 2.0, 0.3, 0.3, 2.6)));
    }
}
