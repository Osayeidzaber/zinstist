package dev.jilaton.client.rotation;

/** A one-client-tick request to rotate the local view toward a target. */
public record RotationRequest(String owner, int priority, RotationAngles target, float maxDegreesPerTick) {
    public RotationRequest {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Rotation request owner must not be blank");
        }
        if (target == null) {
            throw new IllegalArgumentException("Rotation request target must not be null");
        }
        if (!Float.isFinite(maxDegreesPerTick) || maxDegreesPerTick <= 0.0F) {
            throw new IllegalArgumentException("Maximum turn speed must be finite and positive");
        }
    }
}
