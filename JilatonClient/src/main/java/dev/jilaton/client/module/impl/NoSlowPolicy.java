package dev.jilaton.client.module.impl;

import net.minecraft.world.item.ItemUseAnimation;

/** Pure decisions for NoSlow's vanilla item-use movement and sprint hooks. */
public final class NoSlowPolicy {
    private NoSlowPolicy() {
    }

    public static float itemUseMultiplier(boolean enabled, boolean removeSlowdown, double vanillaScale,
                                          float originalMultiplier) {
        return itemUseMultiplier(enabled, removeSlowdown, vanillaScale, 1.0, originalMultiplier);
    }

    public static float itemUseMultiplier(boolean enabled, boolean removeSlowdown, double vanillaScale,
                                          double actionScale, float originalMultiplier) {
        if (!enabled) {
            return originalMultiplier;
        }
        if (removeSlowdown) {
            return 1.0F;
        }
        double global = boundScale(vanillaScale);
        double action = boundScale(actionScale);
        double boundedScale = Math.max(0.0, Math.min(2.0, global * action));
        return (float) (originalMultiplier * boundedScale);
    }

    private static double boundScale(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(2.0, value)) : 1.0;
    }

    public static boolean slowDueToItemUse(boolean enabled, boolean allowSprint, boolean originalValue) {
        return enabled && allowSprint ? false : originalValue;
    }

    public static double actionScale(ItemUseAnimation animation, double consume, double block, double bow,
                                     double spear, double crossbow, double other) {
        if (animation == null) {
            return other;
        }
        return switch (animation) {
            case EAT, DRINK -> consume;
            case BLOCK -> block;
            case BOW -> bow;
            case TRIDENT, SPEAR -> spear;
            case CROSSBOW -> crossbow;
            case NONE, SPYGLASS, TOOT_HORN, BRUSH, BUNDLE -> other;
        };
    }
}
