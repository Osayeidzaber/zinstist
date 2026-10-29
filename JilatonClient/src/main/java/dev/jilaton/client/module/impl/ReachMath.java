package dev.jilaton.client.module.impl;

import net.minecraft.world.item.component.AttackRange;

public final class ReachMath {
    private ReachMath() {
    }

    public static double addBonus(double vanillaRange, boolean activeLocalPlayer, double bonus, double maximum) {
        if (!activeLocalPlayer || !Double.isFinite(bonus) || !Double.isFinite(maximum)) {
            return vanillaRange;
        }
        return vanillaRange + Math.max(0.0, Math.min(maximum, bonus));
    }

    public static AttackRange extendAttackRange(AttackRange vanillaRange, boolean activeLocalPlayer, double bonus) {
        if (!activeLocalPlayer || !Double.isFinite(bonus)) {
            return vanillaRange;
        }
        float increase = (float) Math.max(0.0, Math.min(5.0, bonus));
        return new AttackRange(
                Math.max(0.0F, vanillaRange.minRange()),
                vanillaRange.maxRange() + increase,
                Math.max(0.0F, vanillaRange.minCreativeRange()),
                vanillaRange.maxCreativeRange() + increase,
                vanillaRange.hitboxMargin(),
                vanillaRange.mobFactor());
    }
}
