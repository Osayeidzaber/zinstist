package dev.jilaton.client.gui.hud;

import java.util.List;
import java.util.Set;

/** The two shipped HUD presets. A preset only flips its own widgets; every toggle stays editable. */
public enum HudPreset {
    MINIMAL("Minimal", Set.of()),
    FULL("Full", Set.of("keystrokes", "performance", "coordinates", "armor", "effects",
            "keybinds", "server", "above_head", "held_item", "staff", "radar"));

    public static final List<String> CHOICES = List.of("Minimal", "Full");

    private final String label;
    private final Set<String> extraWidgets;

    HudPreset(String label, Set<String> extraWidgets) {
        this.label = label;
        this.extraWidgets = extraWidgets;
    }

    public String label() {
        return label;
    }

    public Set<String> extraWidgets() {
        return extraWidgets;
    }

    public boolean enabled(String widgetId) {
        return extraWidgets.contains(widgetId);
    }

    public static HudPreset from(String value) {
        for (HudPreset preset : values()) {
            if (preset.label.equalsIgnoreCase(value)) {
                return preset;
            }
        }
        return MINIMAL;
    }
}
