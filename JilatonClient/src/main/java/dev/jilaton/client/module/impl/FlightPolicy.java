package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec3;

/** Deterministic motion cap used by the generic Creative flight mode. */
public final class FlightPolicy {
    private FlightPolicy() {
    }

    public static Vec3 capVelocity(Vec3 velocity, double maximumSpeed) {
        if (velocity == null || !Double.isFinite(maximumSpeed) || maximumSpeed <= 0.0) {
            return Vec3.ZERO;
        }
        double speedSquared = velocity.lengthSqr();
        if (!Double.isFinite(speedSquared)) {
            return Vec3.ZERO;
        }
        double maximumSquared = maximumSpeed * maximumSpeed;
        if (speedSquared <= maximumSquared) {
            return velocity;
        }
        return velocity.scale(maximumSpeed / Math.sqrt(speedSquared));
    }

    public static boolean shouldSendOpenAirHeartbeat(int tickCount, boolean hasBlockBelow) {
        return tickCount % 40 == 0 && !hasBlockBelow;
    }
}
