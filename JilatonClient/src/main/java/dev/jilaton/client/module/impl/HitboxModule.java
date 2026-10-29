package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;

public final class HitboxModule extends ClientModule {
    private final NumberSetting size;
    private final BooleanSetting playersOnly;

    public HitboxModule() {
        super("hitbox", "Hitboxes", "Expands eligible target pick margins without changing collision boxes.",
                ModuleCategory.COMBAT);
        size = addSetting(new NumberSetting("size", "Size", 0.0, 1.0, 0.05, 0.1));
        playersOnly = addSetting(new BooleanSetting("players_only", "Players only", true));
    }

    public NumberSetting size() {
        return size;
    }

    public BooleanSetting playersOnly() {
        return playersOnly;
    }
}
