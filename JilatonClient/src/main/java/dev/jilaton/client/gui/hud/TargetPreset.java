package dev.jilaton.client.gui.hud;

import java.util.List;

/**
 * Named target-card shapes. Every field maps to a real {@code HudModule} setting, so selecting a
 * preset is the same edit the user could make by hand and survives a config round trip.
 */
public enum TargetPreset {
    COMPACT("Compact", 0.85, 232, false, false, true),
    CLASSIC("Classic", 1.0, 0, true, true, true),
    WIDE("Wide", 1.0, 320, true, true, true);

    public static final List<String> CHOICES = List.of("Compact", "Classic", "Wide");

    private final String label;
    private final double scale;
    /** Requested screen width, or 0 to size from the viewport. */
    private final int width;
    private final boolean chips;
    private final boolean held;
    private final boolean distance;

    TargetPreset(String label, double scale, int width, boolean chips, boolean held, boolean distance) {
        this.label = label;
        this.scale = scale;
        this.width = width;
        this.chips = chips;
        this.held = held;
        this.distance = distance;
    }

    public String label() { return label; }
    public double scale() { return scale; }
    public int width() { return width; }
    public boolean chips() { return chips; }
    public boolean held() { return held; }
    public boolean distance() { return distance; }

    public static TargetPreset from(String value) {
        for (TargetPreset preset : values()) {
            if (preset.label.equalsIgnoreCase(value == null ? "" : value.strip())) {
                return preset;
            }
        }
        return CLASSIC;
    }
}
