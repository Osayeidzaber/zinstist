package dev.jilaton.client.rotation;

import java.util.Collection;
import java.util.Objects;

/** Pure arbitration and smoothing rules, separated for deterministic testing. */
public final class RotationPolicy {
    private RotationPolicy() {
    }

    /** Highest priority wins; equal priorities retain the first request in tick order. */
    public static RotationRequest select(Collection<RotationRequest> requests) {
        Objects.requireNonNull(requests, "requests");
        RotationRequest winner = null;
        for (RotationRequest request : requests) {
            if (request != null && (winner == null || request.priority() > winner.priority())) {
                winner = request;
            }
        }
        return winner;
    }

    public static RotationAngles stepToward(float currentYaw, float currentPitch,
                                            RotationAngles target, float maxDegreesPerTick) {
        if (!Float.isFinite(currentYaw) || !Float.isFinite(currentPitch)) {
            throw new IllegalArgumentException("Current rotation must be finite");
        }
        Objects.requireNonNull(target, "target");
        if (!Float.isFinite(maxDegreesPerTick) || maxDegreesPerTick <= 0.0F) {
            throw new IllegalArgumentException("Maximum turn speed must be finite and positive");
        }

        float yawDelta = wrapDegrees(target.yaw() - currentYaw);
        float pitchDelta = target.pitch() - currentPitch;
        float nextYaw = wrapDegrees(currentYaw + clamp(yawDelta, -maxDegreesPerTick, maxDegreesPerTick));
        float nextPitch = clamp(currentPitch + clamp(pitchDelta, -maxDegreesPerTick, maxDegreesPerTick),
                -90.0F, 90.0F);
        return new RotationAngles(nextYaw, nextPitch);
    }

    public static float angularError(float currentYaw, float currentPitch, RotationAngles target) {
        Objects.requireNonNull(target, "target");
        float yaw = wrapDegrees(target.yaw() - currentYaw);
        float pitch = target.pitch() - currentPitch;
        return (float) Math.hypot(yaw, pitch);
    }

    public static float wrapDegrees(float degrees) {
        if (!Float.isFinite(degrees)) {
            throw new IllegalArgumentException("Angle must be finite");
        }
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
