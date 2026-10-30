package dev.jilaton.client.render;

public final class NametagLayout {
    private NametagLayout() { }

    public static int iconsWidth(int count) { return count <= 0 ? 0 : count * 12 - 2; }
    public static int append(int total, int width, int gap) {
        return width <= 0 ? total : total <= 0 ? width : total + gap + width;
    }
    public static float scale(double distance, double base, double minimum) {
        if (!Double.isFinite(distance) || distance < 0) return 0;
        return (float) (Math.max(0.5, Math.min(2, base))
                * Math.min(1, Math.max(Math.max(0.3, Math.min(1, minimum)), 8 / Math.max(1, distance))));
    }
    public static int healthColor(float health, float maximum) {
        if (!Float.isFinite(health) || !Float.isFinite(maximum) || maximum <= 0) return 0xFF9098AD;
        float fraction = health / maximum;
        return fraction > 0.6 ? 0xFF69E59B : fraction > 0.3 ? 0xFFFFBB68 : 0xFFFF788A;
    }
    public static int pingColor(int ping) {
        return ping < 0 ? 0xFF9098AD : ping < 60 ? 0xFF69E59B : ping < 130 ? 0xFFFFBB68 : 0xFFFF788A;
    }

    public static String healthText(float health, boolean hearts) {
        if (!Float.isFinite(health) || health < 0) return "";
        double value = Math.round(health * (hearts ? 0.5 : 1) * 10) / 10.0;
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
