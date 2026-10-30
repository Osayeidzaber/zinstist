package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Ray/target intersection policy for Velocity's optional OnlyFacing strafe gate. */
public final class VelocityFacingPolicy {
    private VelocityFacingPolicy() {
    }

    public static boolean rayIntersectsTarget(Vec3 origin, Vec3 direction, double maximumRange, AABB targetBox) {
        if (origin == null || direction == null || targetBox == null
                || !Double.isFinite(maximumRange) || maximumRange <= 0.0
                || !Double.isFinite(direction.lengthSqr()) || direction.lengthSqr() < 1.0E-12) {
            return false;
        }
        Vec3 end = origin.add(direction.normalize().scale(maximumRange));
        Optional<Vec3> hit = targetBox.clip(origin, end);
        return hit.isPresent() && origin.distanceToSqr(hit.get()) <= maximumRange * maximumRange + 1.0E-9;
    }
}
