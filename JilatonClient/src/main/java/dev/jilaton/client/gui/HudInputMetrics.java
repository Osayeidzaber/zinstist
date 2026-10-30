package dev.jilaton.client.gui;

/** Only mouse event counts are retained, on Minecraft's client thread. */
public final class HudInputMetrics {
    private static final ClickRate LEFT = new ClickRate();
    private static final ClickRate RIGHT = new ClickRate();

    private HudInputMetrics() { }

    public static void press(int button) {
        long now = System.nanoTime();
        if (button == 0) LEFT.press(now);
        if (button == 1) RIGHT.press(now);
    }

    public static int left(long now) { return LEFT.count(now); }
    public static int right(long now) { return RIGHT.count(now); }
    public static void clear() { LEFT.clear(); RIGHT.clear(); }
}
