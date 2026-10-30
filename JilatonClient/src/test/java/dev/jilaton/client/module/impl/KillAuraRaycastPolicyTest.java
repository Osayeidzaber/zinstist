package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillAuraRaycastPolicyTest {
    private static final Vec3 ORIGIN = Vec3.ZERO;
    private static final Vec3 DIRECTION = new Vec3(1.0, 0.0, 0.0);
    private static final AABB TARGET = new AABB(3.0, -0.5, -0.5, 4.0, 0.5, 0.5);

    @Test
    void acceptsTargetIntersectedBeforeBlockAndOtherEntities() {
        assertTrue(KillAuraRaycastPolicy.targetIsFirstOnRay(ORIGIN, DIRECTION, 5.0, TARGET,
                Double.POSITIVE_INFINITY, List.of(TARGET)));
    }

    @Test
    void rejectsTargetOutsideViewRayOrRange() {
        AABB besideRay = new AABB(3.0, 1.0, -0.5, 4.0, 2.0, 0.5);
        assertFalse(KillAuraRaycastPolicy.targetIsFirstOnRay(ORIGIN, DIRECTION, 5.0, besideRay,
                Double.POSITIVE_INFINITY, List.of(besideRay)));
        assertFalse(KillAuraRaycastPolicy.targetIsFirstOnRay(ORIGIN, DIRECTION, 2.0, TARGET,
                Double.POSITIVE_INFINITY, List.of(TARGET)));
    }

    @Test
    void rejectsTargetBehindBlockOrAnotherEntity() {
        AABB nearerEntity = new AABB(1.0, -0.5, -0.5, 2.0, 0.5, 0.5);
        assertFalse(KillAuraRaycastPolicy.targetIsFirstOnRay(ORIGIN, DIRECTION, 5.0, TARGET,
                1.5, List.of(TARGET)));
        assertFalse(KillAuraRaycastPolicy.targetIsFirstOnRay(ORIGIN, DIRECTION, 5.0, TARGET,
                Double.POSITIVE_INFINITY, List.of(nearerEntity, TARGET)));
    }

    @Test
    void rejectsInvalidRayInputs() {
        assertFalse(KillAuraRaycastPolicy.targetIsFirstOnRay(ORIGIN, Vec3.ZERO, 5.0, TARGET,
                Double.POSITIVE_INFINITY, List.of(TARGET)));
        assertFalse(KillAuraRaycastPolicy.targetIsFirstOnRay(ORIGIN, DIRECTION, Double.NaN, TARGET,
                Double.POSITIVE_INFINITY, List.of(TARGET)));
    }
}
