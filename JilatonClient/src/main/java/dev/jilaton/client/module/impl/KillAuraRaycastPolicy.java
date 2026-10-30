package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Geometric target-ray gate used by KillAura's Enemy and All raycast modes. */
public final class KillAuraRaycastPolicy {
    private static final double EPSILON = 1.0E-6;

    private KillAuraRaycastPolicy() {
    }

    public static boolean targetIsFirstOnRay(Vec3 origin, Vec3 direction, double maximumRange,
                                             AABB targetBox, double nearestBlockDistance,
                                             Iterable<AABB> entityBoxes) {
        if (origin == null || direction == null || targetBox == null || entityBoxes == null
                || !Double.isFinite(maximumRange) || maximumRange <= 0.0
                || !Double.isFinite(direction.lengthSqr()) || direction.lengthSqr() < EPSILON
                || Double.isNaN(nearestBlockDistance) || nearestBlockDistance < 0.0) {
            return false;
        }

        Vec3 end = origin.add(direction.normalize().scale(maximumRange));
        Optional<Vec3> targetHit = targetBox.clip(origin, end);
        if (targetHit.isEmpty()) {
            return false;
        }

        double targetDistance = origin.distanceTo(targetHit.get());
        if (targetDistance > maximumRange + EPSILON
                || nearestBlockDistance + EPSILON < targetDistance) {
            return false;
        }

        for (AABB entityBox : entityBoxes) {
            if (entityBox == null) {
                continue;
            }
            Optional<Vec3> entityHit = entityBox.clip(origin, end);
            if (entityHit.isPresent()
                    && origin.distanceTo(entityHit.get()) + EPSILON < targetDistance) {
                return false;
            }
        }
        return true;
    }
}
