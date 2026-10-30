package dev.jilaton.client.module.impl;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

/** Pure input sampling and eligibility gate for the Strafe module. */
final class StrafePolicy {
    private StrafePolicy() {
    }

    /** Vanilla axes: left is positive sideways, forward is positive forward. */
    static Sampling sample(Input input, Vec2 moveVectorOverride) {
        if (moveVectorOverride != null) {
            return new Sampling(moveVectorOverride.y, moveVectorOverride.x);
        }
        return new Sampling(axis(input.forward(), input.backward()), axis(input.left(), input.right()));
    }

    static boolean shouldApply(boolean hasLevel, boolean passenger, boolean fallFlying, boolean flying,
                               boolean spectator, boolean inWater, boolean inLava) {
        return hasLevel && !passenger && !fallFlying && !flying && !spectator && !inWater && !inLava;
    }

    static double strength(boolean onGround, double groundStrength, double airStrength) {
        return onGround ? groundStrength : airStrength;
    }

    private static double axis(boolean positive, boolean negative) {
        return (positive ? 1.0 : 0.0) - (negative ? 1.0 : 0.0);
    }

    record Sampling(double forward, double sideways) {
    }
}
