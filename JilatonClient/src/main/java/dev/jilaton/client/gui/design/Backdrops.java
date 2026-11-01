package dev.jilaton.client.gui.design;

import dev.jilaton.client.JilatonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Background system for full-screen menus. Every mode is calm and dark so foreground glass reads on top of it:
 * <ul>
 *   <li><b>Ambient</b> — two very soft lights drifting slowly over black (default).</li>
 *   <li><b>Particles</b> — sparse motes rising slowly, fading with depth.</li>
 *   <li><b>Panorama</b> — Minecraft's own title panorama, blurred and darkened.</li>
 *   <li><b>Dark</b> — flat black with a faint vignette, for the most minimal look.</li>
 * </ul>
 * Drawn in board coordinates on a {@link Canvas}; motion stops with reduced motion.
 */
public final class Backdrops {
    public static final List<String> MODES = List.of("Dark Veil", "Aurora", "Threads", "Beams", "Galaxy", "Liquid Chrome",
            "Ambient", "Particles", "Panorama", "Dark");
    private static final long EPOCH = System.nanoTime();

    private Backdrops() {
    }

    public static String mode() {
        String m = JilatonClient.clientConfig() == null ? "Dark Veil" : JilatonClient.clientConfig().menuBackground();
        return MODES.contains(m) ? m : "Dark Veil";
    }

    /** Full-screen backdrop for a screen of {@code width}x{@code height} GUI px; call before {@link Canvas#begin}. */
    public static void drawScreen(GuiGraphics g, int width, int height, float partial) {
        Minecraft client = Minecraft.getInstance();
        double scale = client.getWindow().getGuiScale();
        double mx = client.mouseHandler.xpos() / scale, my = client.mouseHandler.ypos() / scale;
        g.fill(0, 0, width, height, Kit.BASE);
        if (ShaderBackdrop.isShaderMode(mode()) && ShaderBackdrop.draw(g, mode(), width, height, mx, my)) return;
        if (mode().equals("Panorama")) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameRenderer != null && mc.level == null) {
                mc.gameRenderer.getPanorama().render(g, width, height, true);
                g.fill(0, 0, width, height, 0xB0000000);
                return;
            }
        }
        g.fill(0, 0, width, height, Kit.BASE);
    }

    /** The animated layer, in board coordinates inside {@link Canvas#begin}. */
    public static void drawBoard(GuiGraphics g) {
        double t = Kit.reduced() ? 0 : (System.nanoTime() - EPOCH) / 1e9;
        if (ShaderBackdrop.isShaderMode(mode())) return;
        switch (mode()) {
            case "Ambient" -> {
                float ax = (float) (980 + Math.sin(t * 0.07) * 120), ay = (float) (230 + Math.cos(t * 0.05) * 60);
                float bx = (float) (220 + Math.cos(t * 0.06) * 140), by = (float) (620 + Math.sin(t * 0.045) * 50);
                Ui.glow(g, ax, ay, 560, Kit.accent() & 0xFFFFFF, 0.075);
                Ui.glow(g, bx, by, 520, 0xBFBFBF, 0.05);
                Ui.glow(g, 640, 360, 900, 0xFFFFFF, 0.018);
            }
            case "Particles" -> {
                Ui.glow(g, 640, 820, 760, 0xFFFFFF, 0.04);
                for (int i = 0; i < 70; i++) {
                    double seed = hash(i);
                    double speed = 6 + 18 * hash(i + 99);
                    double y = 760 - ((t * speed + seed * 900) % 840);
                    double x = 40 + 1200 * hash(i + 7) + Math.sin(t * 0.3 + i) * 10;
                    double depth = 0.3 + 0.7 * hash(i + 31);
                    float r = (float) (0.8 + 1.4 * depth);
                    double fade = Math.min(1, Math.min((760 - y) / 120, (y + 80) / 160));
                    Ui.circle(g, (float) x, (float) y, r, Kit.a(0xFFFFFFFF, 0.08 + 0.32 * depth * Math.max(0, fade)));
                }
            }
            case "Panorama" -> Ui.vGradient(g, 0, 420, Canvas.W, 300, 0x00000000, 0xCC000000);
            default -> {
                Ui.glow(g, 640, 300, 760, 0xFFFFFF, 0.025);
            }
        }
        // A shared vignette grounds every mode.
        Ui.vGradient(g, 0, 520, Canvas.W, 200, 0x00000000, 0x99000000);
    }

    private static double hash(int seed) {
        long x = seed * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) / (double) (1L << 53);
    }
}
