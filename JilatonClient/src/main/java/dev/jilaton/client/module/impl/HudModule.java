package dev.jilaton.client.module.impl;

import dev.jilaton.client.gui.hud.HudDesign;
import dev.jilaton.client.gui.hud.HudPreset;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import java.util.List;

public final class HudModule extends ClientModule {
    private final ChoiceSetting preset;
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
    private final BooleanSetting keybinds;
    private final BooleanSetting serverPanel;
    private final BooleanSetting aboveHead;
    private final BooleanSetting heldItem;
    private final BooleanSetting staff;
    private final TextSetting staffPatterns;
    private final NumberSetting staffRows;
    private final BooleanSetting radar;
    private final NumberSetting radarSize;
    private final NumberSetting radarRange;
    private final ChoiceSetting radarRotation;
    private final NumberSetting vanishWindow;
    private final ChoiceSetting targetPlacement = addSetting(new ChoiceSetting("target_placement", "Target card position", List.of("HUD", "Follow target"), "HUD"));
    private final ChoiceSetting targetStyle = addSetting(new ChoiceSetting("target_style", "Target card style", List.of("Detailed", "Compact"), "Detailed"));

    public HudModule() {
        super("hud", "HUD", "Controls Jilaton's on-screen status elements.", ModuleCategory.CLIENT);
        preset = addSetting(new ChoiceSetting("preset", "HUD preset", HudPreset.CHOICES, HudPreset.MINIMAL.label()));
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
        keybinds = addSetting(new BooleanSetting("keybinds", "Key bind panel", false));
        serverPanel = addSetting(new BooleanSetting("server", "Server panel", false));
        aboveHead = addSetting(new BooleanSetting("above_head", "Above-head target", false));
        heldItem = addSetting(new BooleanSetting("held_item", "Held item display", false));
        staff = addSetting(new BooleanSetting("staff", "Staff list", false));
        staffPatterns = addSetting(new TextSetting("staff_patterns",
                "Staff patterns (heuristic, server-name/team dependent)", 160,
                "ADMIN,OWNER,MODERATOR,HELPER,tag:STAFF", HudModule::sanitizePatterns));
        staffRows = addSetting(new NumberSetting("staff_rows", "Staff list rows", 1, HudDesign.STAFF_MAX_ROWS, 1, 5));
        radar = addSetting(new BooleanSetting("radar", "Radar", false));
        radarSize = addSetting(new NumberSetting("radar_size", "Radar size", HudDesign.RADAR_MIN_SIZE,
                HudDesign.RADAR_MAX_SIZE, 4, HudDesign.RADAR_DEFAULT_SIZE));
        radarRange = addSetting(new NumberSetting("radar_range", "Radar range (blocks)", HudDesign.RADAR_MIN_RANGE,
                HudDesign.RADAR_MAX_RANGE, 4, HudDesign.RADAR_DEFAULT_RANGE));
        radarRotation = addSetting(new ChoiceSetting("radar_rotation", "Radar rotation",
                List.of("North up", "Follow yaw"), "North up"));
        vanishWindow = addSetting(new NumberSetting("vanish_window", "Vanish retention (seconds)", 1, 30, 1, 5));
    }

    /** Applies the preset's own widgets; every other toggle and every saved layout is left alone. */
    public void selectPreset(String value) {
        preset.setValue(value);
        HudPreset selection = HudPreset.from(value);
        keystrokes.setValue(selection.enabled("keystrokes"));
        performance.setValue(selection.enabled("performance"));
        coordinates.setValue(selection.enabled("coordinates"));
        armor.setValue(selection.enabled("armor"));
        effects.setValue(selection.enabled("effects"));
        keybinds.setValue(selection.enabled("keybinds"));
        serverPanel.setValue(selection.enabled("server"));
        aboveHead.setValue(selection.enabled("above_head"));
        heldItem.setValue(selection.enabled("held_item"));
        staff.setValue(selection.enabled("staff"));
        radar.setValue(selection.enabled("radar"));
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
    public String preset() { return preset.value(); }
    public boolean keybinds() { return keybinds.value(); }
    public boolean serverPanel() { return serverPanel.value(); }
    public boolean aboveHead() { return aboveHead.value(); }
    public boolean heldItem() { return heldItem.value(); }
    public boolean staff() { return staff.value(); }
    public String staffPatterns() { return staffPatterns.value(); }
    public int staffRows() { return (int) Math.round(staffRows.value()); }
    public boolean radar() { return radar.value(); }
    public int radarSize() { return HudDesign.radarSize(radarSize.value()); }
    public double radarRange() { return radarRange.value(); }
    public boolean radarNorthUp() { return radarRotation.value().equals("North up"); }
    public long vanishWindowMillis() { return Math.round(vanishWindow.value() * 1000.0); }
    public boolean followTarget() { return targetPlacement.value().equals("Follow target"); }
    public boolean compactTarget() { return targetStyle.value().equals("Compact"); }

    private static String sanitizePatterns(String value) {
        String trimmed = value == null ? "" : value.strip();
        StringBuilder safe = new StringBuilder(trimmed.length());
        for (int index = 0; index < trimmed.length(); index++) {
            char character = trimmed.charAt(index);
            if (!Character.isISOControl(character)) {
                safe.append(character);
            }
        }
        return safe.toString();
    }
}
