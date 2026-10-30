package dev.jilaton.client.module.impl;

/** Timing and eligibility policy for Triggerbot; independent of Minecraft runtime objects. */
public final class TriggerbotPolicy {
    private TriggerbotPolicy() {
    }

    public static long reactionReadyAt(long currentTick, int reactionTicks, int randomExtraTicks) {
        if (reactionTicks < 0 || randomExtraTicks < 0) {
            throw new IllegalArgumentException("Reaction delays must not be negative");
        }
        return Math.addExact(currentTick, Math.addExact(reactionTicks, randomExtraTicks));
    }

    public static boolean shouldForgetTarget(long currentTick, long lastSeenTick, int graceTicks) {
        if (graceTicks < 0) {
            throw new IllegalArgumentException("Target grace must not be negative");
        }
        return currentTick < lastSeenTick || currentTick - lastSeenTick > graceTicks;
    }

    public static boolean mayAttack(long currentTick, long reactionReadyTick, long chargedReadyTick,
                                    float attackCharge, boolean holdAttack, boolean attackHeld,
                                    boolean weaponsOnly, boolean holdingMeleeWeapon) {
        return currentTick >= reactionReadyTick
                && chargedReadyTick >= 0L
                && currentTick >= chargedReadyTick
                && Float.isFinite(attackCharge)
                && attackCharge >= 1.0F
                && (!holdAttack || attackHeld)
                && (!weaponsOnly || holdingMeleeWeapon);
    }
}
