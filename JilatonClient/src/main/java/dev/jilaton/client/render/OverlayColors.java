package dev.jilaton.client.render;

/** Color behavior adapted from LiquidBounce RenderConfigurables/ESP/Tracers; GPL-3.0-or-later. */
public final class OverlayColors {
    private OverlayColors() { }

    public static int resolve(String mode, int theme, double health, double maximumHealth,
                              double distance, double seconds, boolean friend, boolean hurt, boolean invisible) {
        if (hurt) return 0xFFFF556C;
        if (invisible) return 0xFFFFAD52;
        if (friend) return 0xFF4B8DFF;
        return switch (mode) {
            case "Health" -> {
                double ratio = Double.isFinite(health) && maximumHealth > 0 ? clamp(health / maximumHealth) : 0;
                yield 0xFF000000 | (int) Math.round(255 * (1 - ratio)) << 16 | (int) Math.round(255 * ratio) << 8;
            }
            case "Distance" -> hsv(Math.min(100, Math.max(0, distance)) / 300.0);
            case "Rainbow" -> hsv(seconds / 6.0);
            default -> 0xFF000000 | theme & 0xFFFFFF;
        };
    }

    private static int hsv(double hue) {
        if (!Double.isFinite(hue)) hue = 0;
        double sector = (hue - Math.floor(hue)) * 6;
        int index = (int) sector;
        int up = (int) Math.round((sector - index) * 255);
        int down = 255 - up;
        return 0xFF000000 | switch (index) {
            case 0 -> 0xFF0000 | up << 8;
            case 1 -> down << 16 | 0xFF00;
            case 2 -> 0xFF00 | up;
            case 3 -> down << 8 | 0xFF;
            case 4 -> up << 16 | 0xFF;
            default -> 0xFF0000 | down;
        };
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
}
