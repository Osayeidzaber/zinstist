/*
 * Adapted from LiquidBounce's NoMissCooldown settings and Minecraft missTime hooks.
 * Copyright (c) 2015 - 2026 CCBlueX; GPL-3.0-or-later.
 */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;

public final class NoMissCooldownModule extends ClientModule {
    private final BooleanSetting removeAttackCooldown;
    private final BooleanSetting cancelAttackOnMiss;

    public NoMissCooldownModule() {
        super("no_miss_cooldown", "NoMissCooldown",
                "Removes the post-miss attack delay and can consume clicks when the crosshair misses.",
                ModuleCategory.COMBAT);
        removeAttackCooldown = addSetting(new BooleanSetting("remove_attack_cooldown", "Remove miss cooldown", true));
        cancelAttackOnMiss = addSetting(new BooleanSetting("cancel_attack_on_miss", "Consume click on miss", false));
    }

    public boolean removeAttackCooldown() {
        return removeAttackCooldown.value();
    }

    public boolean cancelAttackOnMiss() {
        return cancelAttackOnMiss.value();
    }
}
