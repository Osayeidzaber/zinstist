package dev.jilaton.client.module.impl;

import java.util.List;
import java.util.Set;

/** Pure potion-selection policy used before AutoPot changes player state. */
final class AutoPotPolicy {
    private AutoPotPolicy() {
    }

    static boolean isEligible(List<EffectStatus> effects, boolean requireHealing,
                              boolean healingEnabled, boolean healingNeeded,
                              Set<EffectType> enabledBuffs, int refreshWindowTicks) {
        if (effects.isEmpty()) {
            return false;
        }

        boolean hasHealing = false;
        boolean useful = false;
        for (EffectStatus effect : effects) {
            if (effect.type() == EffectType.HEALING) {
                hasHealing = true;
                if (!healingEnabled || !healingNeeded) {
                    return false;
                }
                useful = true;
                continue;
            }

            if (effect.type() == EffectType.UNSUPPORTED || !enabledBuffs.contains(effect.type())) {
                return false;
            }
            if (shouldRefresh(true, effect.remainingTicks(), refreshWindowTicks)) {
                useful = true;
            }
        }
        return useful && (!requireHealing || hasHealing);
    }

    static boolean shouldRefresh(boolean enabled, Integer remainingTicks, int refreshWindowTicks) {
        if (!enabled) {
            return false;
        }
        return remainingTicks == null || remainingTicks <= Math.max(0, refreshWindowTicks);
    }

    enum EffectType {
        HEALING,
        SPEED,
        STRENGTH,
        REGENERATION,
        FIRE_RESISTANCE,
        JUMP_BOOST,
        WATER_BREATHING,
        UNSUPPORTED
    }

    record EffectStatus(EffectType type, Integer remainingTicks) {
    }
}
