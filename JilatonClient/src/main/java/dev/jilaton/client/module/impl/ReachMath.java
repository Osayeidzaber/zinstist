package dev.jilaton.client.module.impl;

import net.minecraft.world.item.component.AttackRange;

public final class ReachMath {
    /**
     * Entity attack/interact ceiling accepted by a vanilla 1.21.11 server:
     * {@code ServerboundInteractPacket.isWithinRange} calls {@code isWithinAttackRange(box, 3.0)};
     * {@code AttackRange} uses {@code ENTITY_INTERACTION_RANGE} (3.0) plus the 3.0 padding.
     * Eye-to-hitbox beyond 6.0 is silently ignored server-side.
     */
    public static final double ENTITY_INTERACTION_LIMIT = 6.0;
    /**
     * Block use/pick ceiling: {@code ServerGamePacketListenerImpl.handleUseItemOn} calls
     * {@code isWithinBlockInteractionRange(pos, 1.0)}, i.e. {@code BLOCK_INTERACTION_RANGE} (4.5) + 1.0.
     */
    public static final double BLOCK_INTERACTION_LIMIT = 5.5;
    /** Largest bonus worth adding for entity reach; beyond 3.0 the sum hits the ceiling. */
    public static final double ENTITY_BONUS_LIMIT = ENTITY_INTERACTION_LIMIT - 3.0;

    private ReachMath() {
    }

    public static double addBonus(double vanillaRange, boolean activeLocalPlayer, double bonus, double maximum) {
        if (!activeLocalPlayer || !Double.isFinite(bonus) || !Double.isFinite(maximum)) {
            return vanillaRange;
        }
        double extended = vanillaRange + Math.max(0.0, Math.min(maximum, bonus));
        return Math.min(extended, Math.max(vanillaRange, protocolLimit(vanillaRange)));
    }

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
        float limit = (float) Math.max(vanillaRange, ENTITY_INTERACTION_LIMIT);
        return Math.min(vanillaRange + increase, limit);
    }

    /** The caller only ever requests the entity or block range attribute; 3.0/4.5 are their vanilla defaults. */
    private static double protocolLimit(double vanillaRange) {
        return vanillaRange < 4.0 ? ENTITY_INTERACTION_LIMIT : BLOCK_INTERACTION_LIMIT;
    }
}
