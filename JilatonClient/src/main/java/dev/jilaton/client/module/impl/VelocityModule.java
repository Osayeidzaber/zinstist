/*
 * Adapted packet-scaling behavior from LiquidBounce's ModuleVelocity and VelocityModify.
 * Copyright (c) 2015 - 2026 CCBlueX. LiquidBounce is licensed under GPL-3.0-or-later.
 * Source: https://github.com/CCBlueX/LiquidBounce/tree/e033f5aaf05be802dd2ca51782f255119d7d5654
 */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;

public final class VelocityModule extends ClientModule {
    private final NumberSetting horizontal;
    private final NumberSetting vertical;

    public VelocityModule() {
        super("velocity", "Velocity", "Scales server velocity applied to your player.", ModuleCategory.COMBAT);
        horizontal = addSetting(new NumberSetting("horizontal", "Horizontal", 0.0, 100.0, 5.0, 0.0));
        vertical = addSetting(new NumberSetting("vertical", "Vertical", 0.0, 100.0, 5.0, 0.0));
    }

    public double horizontalScale() {
        return horizontal.value() / 100.0;
    }

    public double verticalScale() {
        return vertical.value() / 100.0;
    }
}
