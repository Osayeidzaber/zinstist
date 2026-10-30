package dev.jilaton.client.config;

/** Saved appearance of one HUD widget. A null accent follows the active theme. */
public record HudStyle(double scale, double panelOpacity, Integer accent, Alignment alignment, boolean visible) {
    public static final HudStyle DEFAULT = new HudStyle(1.0, 176.0 / 255.0, null, Alignment.FREE, true);

    public enum Alignment { FREE, LEFT, CENTER, RIGHT }

    public HudStyle {
        if (!Double.isFinite(scale) || !Double.isFinite(panelOpacity) || alignment == null) {
            throw new IllegalArgumentException("Invalid HUD appearance");
        }
        scale = Math.max(0.5, Math.min(2.0, scale));
        panelOpacity = Math.max(0.0, Math.min(1.0, panelOpacity));
        if (accent != null && (accent < 0 || accent > 0xFFFFFF)) {
            throw new IllegalArgumentException("HUD accent must be a 24-bit RGB color");
        }
    }

    public int accentColor(int themeAccent) {
        return accent == null ? themeAccent : 0xFF000000 | accent;
    }

    public int panelColor() {
        return ((int) Math.round(panelOpacity * 255.0) << 24) | 0x101118;
    }
}
