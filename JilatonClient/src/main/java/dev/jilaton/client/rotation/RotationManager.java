package dev.jilaton.client.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Collects module rotation requests and applies one smoothed, visible rotation per tick. */
public final class RotationManager {
    private final Map<String, RotationRequest> requests = new LinkedHashMap<>();
    private boolean requestsSuspended;
    private RotationRequest lastApplied;

    public void beginTick() {
        requests.clear();
        requestsSuspended = false;
    }

    public boolean request(String owner, int priority, float yaw, float pitch, float maxDegreesPerTick) {
        if (requestsSuspended) {
            return false;
        }
        try {
            RotationRequest request = new RotationRequest(owner, priority,
                    new RotationAngles(yaw, pitch), maxDegreesPerTick);
            requests.put(owner, request);
            return true;
        } catch (IllegalArgumentException invalidRequest) {
            return false;
        }
    }

    /** Clears pending requests when a higher-level action takes ownership of view rotation. */
    public void cancelAll() {
        requests.clear();
    }

    /** Lets an action such as a potion throw retain its required view rotation for this tick. */
    public void suspendRequestsForTick() {
        requests.clear();
        requestsSuspended = true;
    }

    /** Mouse assistance yields to the same higher-priority owners as tick-driven visible rotations. */
    public boolean permitsMouseCorrection(int priority, Predicate<String> ownerEnabled) {
        if (requestsSuspended) return false;
        if (lastApplied != null && lastApplied.priority() > priority && ownerEnabled.test(lastApplied.owner())) return false;
        for (RotationRequest request : requests.values()) {
            if (request.priority() > priority && ownerEnabled.test(request.owner())) return false;
        }
        return true;
    }

    public void apply(Minecraft client, Predicate<String> ownerEnabled) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(ownerEnabled, "ownerEnabled");
        Player player = client.player;
        if (player == null) {
            requests.clear();
            lastApplied = null;
            return;
        }

        RotationRequest winner = null;
        for (RotationRequest request : requests.values()) {
            if (ownerEnabled.test(request.owner())
                    && (winner == null || request.priority() > winner.priority())) {
                winner = request;
            }
        }
        requests.clear();
        requestsSuspended = false;
        lastApplied = winner;
        if (winner == null) {
            return;
        }

        RotationAngles next = RotationPolicy.stepToward(player.getYRot(), player.getXRot(),
                winner.target(), winner.maxDegreesPerTick());
        player.setYRot(next.yaw());
        player.setXRot(next.pitch());
    }
}
