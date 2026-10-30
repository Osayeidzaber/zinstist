package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec3;

/** Horizontal boost transform used by the adapted LongJump NCPBoost mode. */
public final class LongJumpBoostPolicy {
    private LongJumpBoostPolicy() {
    }

    public static Vec3 apply(Vec3 movement, double boost) {
        if (!Double.isFinite(boost) || boost < 1.0 || !Double.isFinite(movement.x)
                || !Double.isFinite(movement.y) || !Double.isFinite(movement.z)) {
            return movement;
        }
        return new Vec3(movement.x * boost, movement.y, movement.z * boost);
    }
}
