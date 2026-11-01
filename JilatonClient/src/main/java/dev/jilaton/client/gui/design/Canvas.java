package dev.jilaton.client.gui.design;

import dev.jilaton.client.JilatonClient;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Maps the redesign boards' 1280x720 coordinate space onto the current screen. Every redesigned screen draws in
 * board coordinates inside {@link #begin}/{@link #end}, so its proportions match the Claude Design boards at any
 * window size or GUI scale; input is mapped back with {@link #x}/{@link #y}.
 */
public final class Canvas {
    public static final float W = 1280, H = 720;

    private float scale = 1, offsetX, offsetY;
    private int width, height;
    private long openedAt;
    private float rise;
    private float previousTypeScale = 1;

    /** Fits the board inside a {@code width}x{@code height} GUI-pixel screen, centred. */
    public Canvas fit(int width, int height) {
        this.width = width;
        this.height = height;
        double user = JilatonClient.clientConfig() == null ? 1.0 : JilatonClient.clientConfig().guiScale();
        scale = (float) (Math.min(width / W, height / H) * user);
        offsetX = (width - W * scale) / 2F;
        offsetY = (height - H * scale) / 2F;
        return this;
    }

    /** Restarts the entrance (fade up from black, content rising 14 board px) on the next frame. */
    public void restart() { openedAt = 0; }

    /** Entrance progress 0..1 (ease-out cubic over 0.42 s); 1 with reduced motion. */
    public double entrance() {
        if (JilatonClient.clientConfig().reducedMotion()) return 1;
        long now = System.nanoTime();
        if (openedAt == 0) openedAt = now;
        double t = Math.min(1, (now - openedAt) / (0.42e9 / JilatonClient.clientConfig().transitionSpeed()));
        return 1 - Math.pow(1 - t, 3);
    }

    public void begin(GuiGraphics graphics) {
        rise = (float) (14 * (1 - entrance()));
        graphics.pose().pushMatrix();
        graphics.pose().translate(offsetX, offsetY + rise * scale);
        graphics.pose().scale(scale, scale);
        previousTypeScale = dev.jilaton.client.gui.JilatonType.enterScale(scale);
    }

    /** Pops the board transform and veils the frame while the entrance is still fading in. */
    public void end(GuiGraphics graphics) {
        graphics.pose().popMatrix();
        dev.jilaton.client.gui.JilatonType.restoreScale(previousTypeScale);
        double e = entrance();
        if (e < 1) {
            // Masked reveal: a dark curtain lifts off the screen with a bright hairline on its edge, plus a light
            // veil, so every screen arrives with the same directional motion instead of a plain fade.
            int edge = (int) Math.round(height * (1 - e));
            if (edge > 0) {
                graphics.fill(0, 0, width, edge, 0xFF050505);
                graphics.fill(0, edge, width, edge + 1, ((int) Math.round(255 * (1 - e) * 0.9) << 24) | 0xFFFFFF);
            }
            graphics.fill(0, 0, width, height, ((int) Math.round((1 - e) * 120) << 24) | 0x050505);
        }
    }

    /** Screen x to board x. */
    public double x(double screenX) { return (screenX - offsetX) / scale; }

    /** Screen y to board y. */
    public double y(double screenY) { return (screenY - offsetY - rise * scale) / scale; }

    /** Board x to screen x. */
    public int screenX(double boardX) { return (int) Math.round(offsetX + boardX * scale); }

    /** Board y to screen y. */
    public int screenY(double boardY) { return (int) Math.round(offsetY + rise * scale + boardY * scale); }

    public float scale() { return scale; }
}
