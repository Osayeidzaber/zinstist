package dev.jilaton.client.module.impl;

/** Pure decisions for NoSlow's vanilla item-use movement and sprint hooks. */
public final class NoSlowPolicy {
    private NoSlowPolicy() {
    }

    public static float itemUseMultiplier(boolean enabled, boolean removeSlowdown, double vanillaScale,
                                          float originalMultiplier) {
        if (!enabled) {
            return originalMultiplier;
        }
        if (removeSlowdown) {
            return 1.0F;
        }
        double boundedScale = Math.max(0.0, Math.min(2.0, vanillaScale));
        return (float) (originalMultiplier * boundedScale);
    }

    public static boolean slowDueToItemUse(boolean enabled, boolean allowSprint, boolean originalValue) {
        return enabled && allowSprint ? false : originalValue;
    }
}
