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
    private final BooleanSetting armor;
    private final BooleanSetting effects;
    private final BooleanSetting cps;
    private final BooleanSetting notifications;

    public HudModule() {
        super("hud", "HUD", "Controls Jilaton's on-screen status elements.", ModuleCategory.CLIENT);
        watermark = addSetting(new BooleanSetting("watermark", "Watermark", true));
        activeModules = addSetting(new BooleanSetting("active_modules", "Active modules", true));
        performance = addSetting(new BooleanSetting("performance", "FPS and ping", true));
        coordinates = addSetting(new BooleanSetting("coordinates", "Coordinates", true));
        keystrokes = addSetting(new BooleanSetting("keystrokes", "Keystrokes", false));
        targetHud = addSetting(new BooleanSetting("target_hud", "KillAura target HUD", true));
        armor = addSetting(new BooleanSetting("armor", "Armor status", true));
        effects = addSetting(new BooleanSetting("effects", "Potion effects", true));
        cps = addSetting(new BooleanSetting("cps", "Physical click CPS", false));
        notifications = addSetting(new BooleanSetting("notifications", "Module notifications", true));
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

    public boolean armor() { return armor.value(); }
    public boolean effects() { return effects.value(); }
    public boolean cps() { return cps.value(); }
    public boolean notifications() { return notifications.value(); }
}
