package dev.jilaton.client.module.impl;

import net.minecraft.world.item.component.AttackRange;

public final class ReachMath {
    /**
     * Padding a vanilla 1.21.11 server adds before every entity range check:
     * {@code ServerGamePacketListenerImpl.handleInteract} calls
     * {@code ServerboundInteractPacket.isWithinRange(player, box, 3.0)}. Attack checks use the
     * player's own attack range plus this padding; interaction checks use the
     * {@code ENTITY_INTERACTION_RANGE} attribute plus this padding.
     */
    public static final double ENTITY_RANGE_PADDING = 3.0;
    /**
     * Padding for block use/pick checks: {@code ServerGamePacketListenerImpl.handleUseItemOn}
     * calls {@code isWithinBlockInteractionRange(pos, 1.0)}.
     */
    public static final double BLOCK_RANGE_PADDING = 1.0;
    /**
     * Default survival entity ceiling: {@code DEFAULT_ENTITY_INTERACTION_RANGE} (3.0) + padding (3.0).
     * Creative adds the 2.0 creative modifier to the base, so its ceiling is 8.0.
     */
    public static final double ENTITY_INTERACTION_LIMIT = 3.0 + ENTITY_RANGE_PADDING;
    /**
     * Default block ceiling: {@code DEFAULT_BLOCK_INTERACTION_RANGE} (4.5) + padding (1.0).
     */
    public static final double BLOCK_INTERACTION_LIMIT = 4.5 + BLOCK_RANGE_PADDING;
    /** Largest bonus worth adding for entity reach; the entity slider and the server padding are both 3.0. */
    public static final double ENTITY_BONUS_LIMIT = ENTITY_RANGE_PADDING;

    private ReachMath() {
    }

    /**
     * Extends {@code Player.entityInteractionRange()} for the local player. The cap is the server's own
     * padding, so a creative player's 5.0 base can select out to 8.0 locally instead of being
     * mistaken for a block range (which the old heuristic did for any base of 4.0 or more).
     */
    public static double extendEntityInteractionRange(double vanillaRange, boolean activeLocalPlayer, double bonus) {
        return addBonus(vanillaRange, activeLocalPlayer, bonus, ENTITY_RANGE_PADDING);
    }

    /** Extends the local block range within vanilla's attribute-plus-1.0 padding; servers can reject interactions. */
    public static double extendBlockInteractionRange(double vanillaRange, boolean activeLocalPlayer, double bonus) {
        return addBonus(vanillaRange, activeLocalPlayer, bonus, BLOCK_RANGE_PADDING);
    }

    private static double addBonus(double vanillaRange, boolean activeLocalPlayer, double bonus, double serverPadding) {
        if (!activeLocalPlayer || !Double.isFinite(vanillaRange) || !Double.isFinite(bonus)) {
            return vanillaRange;
        }
        double extended = vanillaRange + Math.max(0.0, Math.min(serverPadding, bonus));
        return Math.min(extended, vanillaRange + serverPadding);
    }

    /**
     * Extends both maxima of the player's attack range by at most the server padding. The server compares
     * the attack hitbox against {@code effectiveMaxRange + hitboxMargin + 3.0}, where the effective maximum
     * is {@code maxRange} in survival and {@code maxCreativeRange} in creative; extending each by that same
     * 3.0 follows that vanilla padding, including the creative 5.0 base. Server-side attributes, plugins and
     * interaction rules still determine acceptance; this does not grant server-side reach.
     */
    public static AttackRange extendAttackRange(AttackRange vanillaRange, boolean activeLocalPlayer, double bonus) {
        if (!activeLocalPlayer || !Double.isFinite(bonus)) {
            return vanillaRange;
        }
        float increase = (float) Math.max(0.0, Math.min(ENTITY_BONUS_LIMIT, bonus));
        return new AttackRange(
                Math.max(0.0F, vanillaRange.minRange()),
                cap(vanillaRange.maxRange(), increase),
                Math.max(0.0F, vanillaRange.minCreativeRange()),
                cap(vanillaRange.maxCreativeRange(), increase),
                vanillaRange.hitboxMargin(),
                vanillaRange.mobFactor());
    }

    private static float cap(float vanillaRange, float increase) {
        return vanillaRange + Math.min(increase, (float) ENTITY_RANGE_PADDING);
    }
}
