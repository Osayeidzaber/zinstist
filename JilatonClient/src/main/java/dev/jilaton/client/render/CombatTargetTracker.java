package dev.jilaton.client.render;

import net.minecraft.world.entity.LivingEntity;

/** Remembers the entity the local player most recently attacked, for visuals that follow combat. */
public final class CombatTargetTracker {
    private static LivingEntity lastTarget;
    private static long lastAttackNanos;

    private CombatTargetTracker() { }

    public static void record(LivingEntity target, long now) {
        lastTarget = target;
        lastAttackNanos = now;
    }

    /** The last attacked entity if it was hit within {@code windowSeconds} and is still alive. */
    public static LivingEntity lastHit(double windowSeconds, long now) {
        LivingEntity target = lastTarget;
        if (target == null || !target.isAlive() || target.isRemoved()) return null;
        return (now - lastAttackNanos) / 1_000_000_000.0 <= windowSeconds ? target : null;
    }

    /**
     * Entity id of the current combat target for target-aware visuals: what you hit in the last few seconds, or
     * -1 when there is none.
     */
    public static int targetId() {
        LivingEntity target = lastHit(4.0, System.nanoTime());
        return target == null ? -1 : target.getId();
    }

    public static void clear() {
        lastTarget = null;
        lastAttackNanos = 0L;
    }
}
