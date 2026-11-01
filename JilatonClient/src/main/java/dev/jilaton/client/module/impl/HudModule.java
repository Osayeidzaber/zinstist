package dev.jilaton.client.module.impl;

import dev.jilaton.client.gui.hud.HudDesign;
import dev.jilaton.client.gui.hud.HudPreset;
import dev.jilaton.client.gui.hud.TargetPreset;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import java.util.List;

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
    private final ChoiceSetting targetPlacement = addSetting(new ChoiceSetting("target_placement", "Target card position", List.of("HUD", "Follow target"), "Follow target"));
    private final ChoiceSetting targetStyle = addSetting(new ChoiceSetting("target_style", "Target card style", List.of("Detailed", "Compact"), "Detailed"));
    private final BooleanSetting targetSideBar = addSetting(new BooleanSetting("target_side_bar", "Health bar beside target", true));
    private final NumberSetting followScale = addSetting(new NumberSetting("target_follow_scale", "Pinned target card scale", 0.3, 1.5, 0.05, 0.6));
    private final NumberSetting targetScale = addSetting(new NumberSetting("target_scale", "Target card scale", 0.5, 2.0, 0.05, 1.0));
    private final NumberSetting targetWidth = addSetting(new NumberSetting("target_width", "Target card width (0 = auto)", 0, 320, 8, 0));
    private final BooleanSetting targetChips = addSetting(new BooleanSetting("target_chips", "Target card chips", true));
    private final BooleanSetting targetHeld = addSetting(new BooleanSetting("target_held", "Target card held item", true));
    private final BooleanSetting targetDistance = addSetting(new BooleanSetting("target_distance", "Target card distance", true));

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

    /** Applies a named target-card shape to the settings that shape it. */
    public void selectTargetPreset(String value) {
        TargetPreset selection = TargetPreset.from(value);
        targetScale.setValue(selection.scale());
        targetWidth.setValue(selection.width());
        targetChips.setValue(selection.chips());
        targetHeld.setValue(selection.held());
        targetDistance.setValue(selection.distance());
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
    /** Derived, so selecting a preset in the editor and the drawn widget set can never disagree. */
    public String preset() {
        int on = 0;
        for (String widget : HudPreset.FULL.extraWidgets()) {
            if (extraWidgetEnabled(widget)) on++;
        }
        if (on == 0) return HudPreset.MINIMAL.label();
        return on == HudPreset.FULL.extraWidgets().size() ? HudPreset.FULL.label() : "Custom";
    }
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
    /** Extra scale for the card while it is pinned beside the target, where it shares the view with them. */
    public double followScale() { return followScale.value(); }
    public boolean targetSideBar() { return targetSideBar.value(); }
    public boolean followTarget() { return targetPlacement.value().equals("Follow target"); }
    public boolean compactTarget() { return targetStyle.value().equals("Compact"); }
    /** The preset whose exact shape is on screen, or "Custom" once any shaping setting is edited. */
    public String targetPreset() {
        for (TargetPreset preset : TargetPreset.values()) {
            if (Math.abs(targetScale() - preset.scale()) < 1e-6
                    && targetWidth() == preset.width()
                    && targetChips() == preset.chips()
                    && targetHeld() == preset.held()
                    && targetDistance() == preset.distance()) {
                return preset.label();
            }
        }
        return "Custom";
    }

    private boolean extraWidgetEnabled(String widget) {
        return switch (widget) {
            case "keystrokes" -> keystrokes();
            case "performance" -> performance();
            case "coordinates" -> coordinates();
            case "armor" -> armor();
            case "effects" -> effects();
            case "keybinds" -> keybinds();
            case "server" -> serverPanel();
            case "above_head" -> aboveHead();
            case "held_item" -> heldItem();
            case "staff" -> staff();
            case "radar" -> radar();
            default -> false;
        };
    }
    public double targetScale() { return targetScale.value(); }
    /** Requested screen width in pixels, or 0 for the viewport-derived width. */
    public int targetWidth() { return (int) Math.round(targetWidth.value()); }
    public boolean targetChips() { return targetChips.value(); }
    public boolean targetHeld() { return targetHeld.value(); }
    public boolean targetDistance() { return targetDistance.value(); }

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
