/*
 * Adapted from LiquidBounce's ModuleNoSlow and MixinLocalPlayer.
 * Copyright (c) 2015 - 2026 CCBlueX. LiquidBounce is licensed under GPL-3.0-or-later.
 * Source: https://github.com/CCBlueX/LiquidBounce/tree/e033f5aaf05be802dd2ca51782f255119d7d5654
 */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;

public final class NoSlowModule extends ClientModule {
    private final BooleanSetting removeItemSlowdown;
    private final BooleanSetting allowSprintWhileUsing;

    public NoSlowModule() {
        super("no_slow", "NoSlow", "Removes movement slowdown from item use and optionally allows sprinting.",
                ModuleCategory.MOVEMENT);
        removeItemSlowdown = addSetting(new BooleanSetting("remove_item_slowdown", "Remove item slowdown", true));
        allowSprintWhileUsing = addSetting(new BooleanSetting("allow_sprint", "Allow sprint while using", true));
    }

    public boolean removeItemSlowdown() {
        return removeItemSlowdown.value();
    }

    public boolean allowSprintWhileUsing() {
        return allowSprintWhileUsing.value();
    }
}
