package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;

public final class HudModule extends ClientModule {
    private final BooleanSetting watermark;
    private final BooleanSetting activeModules;
    private final BooleanSetting performance;
    private final BooleanSetting coordinates;
    private final BooleanSetting keystrokes;
    private final BooleanSetting targetHud;

    public HudModule() {
        super("hud", "HUD", "Controls Jilaton's on-screen status elements.", ModuleCategory.CLIENT);
        watermark = addSetting(new BooleanSetting("watermark", "Watermark", true));
        activeModules = addSetting(new BooleanSetting("active_modules", "Active modules", true));
        performance = addSetting(new BooleanSetting("performance", "FPS and ping", true));
        coordinates = addSetting(new BooleanSetting("coordinates", "Coordinates", true));
        keystrokes = addSetting(new BooleanSetting("keystrokes", "Keystrokes", false));
        targetHud = addSetting(new BooleanSetting("target_hud", "KillAura target HUD", true));
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    public boolean watermark() {
        return watermark.value();
    }

    public boolean activeModules() {
        return activeModules.value();
    }

    public boolean performance() {
        return performance.value();
    }

    public boolean coordinates() {
        return coordinates.value();
    }

    public boolean keystrokes() {
        return keystrokes.value();
    }

    public boolean targetHud() {
        return targetHud.value();
    }
}
