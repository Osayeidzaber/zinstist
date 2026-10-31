package dev.jilaton.client.gui.design;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.ClientTheme;
import dev.jilaton.client.gui.JilatonType;
import dev.jilaton.client.gui.JilatonUi;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Component kit for the redesign (development/GUI_REDESIGN.md §0). Colours are the design tokens; every size is in
 * board pixels and is meant to be drawn inside a {@link Canvas}.
 */
public final class Ui {
    // Neutral black and white (zaber.tech): no hue in any surface or text token.
    public static final int BACKDROP = 0xFF0A0A0A, DEEP = 0xFF060606, PANEL = 0xFF0E0E0E, SIDEBAR = 0xFF0A0A0A,
            CARD = 0xFF131313, RAISED = 0xFF1B1B1B, DRAWER = 0xFF0F0F0F, STROKE = 0xFF262626;
    public static final int TEXT = 0xFFF5F5F5, TEXT_2 = 0xFF8F8F8F, TEXT_3 = 0xFF5F5F5F, SOFT = 0xFFCBCBCB,
            FAINT = 0xFF3A3A3A;
    public static final int DANGER = 0xFFE5484D, ONLINE = 0xFF30A46C, WARN = 0xFFE5A13E;

    private Ui() { }

    /** The theme accent (white by default), from the user's theme setting. */
    public static int accent() {
        return ClientTheme.resolveAccent(JilatonClient.clientConfig().themeId(), JilatonClient.clientConfig().customAccent());
    }

    public static int alpha(int argb, double opacity) { return JilatonUi.alpha(argb, opacity); }

    public static int mix(int from, int to, double t) { return JilatonUi.mix(from, to, t); }

    public static void rect(GuiGraphics g, float x, float y, float w, float h, int color) {
        Paint.rect(g, x, y, w, h, color);
    }

    /** Anti-aliased rounded fill, snapped to physical pixels ({@link Paint}). */
    public static void round(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
        Paint.round(g, x, y, w, h, radius, color);
    }

    /** Anti-aliased one-pixel hairline outline. */
    public static void outline(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
        Paint.outline(g, x, y, w, h, radius, 0, color);
    }

    /** Card: fill + hairline, the design's default surface. */
    public static void card(GuiGraphics g, float x, float y, float w, float h, float radius, int fill, int stroke) {
        round(g, x, y, w, h, radius, fill);
        outline(g, x, y, w, h, radius, stroke);
    }

    /** Active surface: accent hairline at ~45% plus a soft halo, the only glow the design allows. */
    public static void glowRing(GuiGraphics g, float x, float y, float w, float h, float radius, int accent, double amount) {
        for (int i = 3; i >= 1; i--)
            outline(g, x - i * 2, y - i * 2, w + i * 4, h + i * 4, radius + i * 2, alpha(accent, 0.05 * amount / i));
        outline(g, x, y, w, h, radius, alpha(accent, 0.5 * amount));
    }

    public static void circle(GuiGraphics g, float cx, float cy, float r, int color) {
        Paint.circle(g, cx, cy, r, color);
    }

    /** Anti-aliased ring, {@code stroke} board px wide. */
    public static void ring(GuiGraphics g, float cx, float cy, float r, float stroke, int color) {
        Paint.ring(g, cx, cy, r, stroke, color);
    }

    /** Soft radial light, dithered so it never bands. */
    public static void glow(GuiGraphics g, float cx, float cy, float r, int rgb, double peak) {
        Paint.glow(g, cx, cy, r, rgb, peak);
    }

    public static void vGradient(GuiGraphics g, float x, float y, float w, float h, int top, int bottom) {
        g.fillGradient(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), top, bottom);
    }

    public static void hline(GuiGraphics g, float x, float y, float w, int color) { Paint.hline(g, x, y, w, color); }

    public static void text(GuiGraphics g, String s, float x, float y, float size, JilatonType.Face face, int color) {
        JilatonType.draw(g, s, face, x, y, size, color);
    }

    public static void text(GuiGraphics g, String s, float x, float y, float size, int color) {
        text(g, s, x, y, size, JilatonType.Face.REGULAR, color);
    }

    public static float width(String s, float size, JilatonType.Face face) { return JilatonType.width(s, face, size); }

    /** Right-aligned text ending at {@code right}. */
    public static void textRight(GuiGraphics g, String s, float right, float y, float size, JilatonType.Face face, int color) {
        text(g, s, right - width(s, size, face), y, size, face, color);
    }

    public static void textCentre(GuiGraphics g, String s, float cx, float y, float size, JilatonType.Face face, int color) {
        text(g, s, cx - width(s, size, face) / 2, y, size, face, color);
    }

    /** Letter-spaced uppercase label (tertiary group labels, wordmarks). */
    public static void spaced(GuiGraphics g, String s, float x, float y, float size, float tracking,
                              JilatonType.Face face, int color) {
        float cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = s.substring(i, i + 1);
            text(g, ch, cx, y, size, face, color);
            cx += width(ch, size, face) + tracking;
        }
    }

    public static float spacedWidth(String s, float size, float tracking, JilatonType.Face face) {
        float w = 0;
        for (int i = 0; i < s.length(); i++) w += width(s.substring(i, i + 1), size, face) + tracking;
        return w - tracking;
    }

    /** Cuts {@code s} with an ellipsis to fit {@code max} board pixels. */
    public static String fit(String s, float max, float size, JilatonType.Face face) {
        if (width(s, size, face) <= max) return s;
        String out = s;
        while (!out.isEmpty() && width(out + "…", size, face) > max) out = out.substring(0, out.length() - 1);
        return out.isEmpty() ? "" : out.stripTrailing() + "…";
    }

    /** 40x22 switch; {@code t} 0..1 slides the knob and fades the track to the accent. */
    public static void toggle(GuiGraphics g, float x, float y, double t, int accent) {
        round(g, x, y, 40, 22, 11, mix(STROKE, accent, t));
        float knob = x + 3 + (float) (18 * t);
        round(g, knob, y + 3, 16, 16, 8, mix(TEXT_2, TEXT, t));
    }

    /** Small toggle for drawers: 32x18. */
    public static void smallToggle(GuiGraphics g, float x, float y, double t, int accent) {
        round(g, x, y, 32, 18, 9, mix(STROKE, accent, t));
        round(g, x + 2 + (float) (14 * t), y + 2, 14, 14, 7, mix(TEXT_2, TEXT, t));
    }

    /** Chip / segmented option. Selected = 14% accent fill + 50% accent hairline. */
    public static void chip(GuiGraphics g, String label, float x, float y, float w, float h, boolean selected,
                            boolean hovered, int accent) {
        int fill = selected ? alpha(accent, 0.14) : hovered ? RAISED : 0;
        if ((fill >>> 24) != 0) round(g, x, y, w, h, 7, fill);
        outline(g, x, y, w, h, 7, selected ? alpha(accent, 0.55) : STROKE);
        float size = 12;
        text(g, label, x + (w - width(label, size, JilatonType.Face.MEDIUM)) / 2, y + (h - size) / 2 + 1, size,
                JilatonType.Face.MEDIUM, selected ? TEXT : TEXT_2);
    }

    /** Button with centred label whose hover eases in and out; hit-testing is the caller's. */
    public static void button(GuiGraphics g, String label, float x, float y, float w, float h, boolean hovered,
                              boolean primary, int accent) {
        button(g, label, x, y, w, h, ease("button:" + label + ":" + Math.round(x) + ":" + Math.round(y), hovered), primary, accent);
    }

    /** Button drawn at hover amount {@code t} (0..1): fill lifts, hairline brightens, the button rises 1 px. */
    public static void button(GuiGraphics g, String label, float x, float y, float w, float h, double t,
                              boolean primary, int accent) {
        y -= (float) t;
        if (primary) {
            round(g, x, y, w, h, 10, mix(accent, 0xFFFFFFFF, 0.14 * t));
        } else {
            if (t > 0.01) round(g, x, y, w, h, 10, alpha(RAISED, t));
            outline(g, x, y, w, h, 10, mix(STROKE, mix(STROKE, accent, 0.45), t));
        }
        float size = 13;
        JilatonType.Face face = primary ? JilatonType.Face.SEMIBOLD : JilatonType.Face.MEDIUM;
        textCentre(g, label, x + w / 2, y + (h - size) / 2 + 1, size, face, primary ? DEEP : mix(SOFT, TEXT, t));
    }

    private static final java.util.Map<String, double[]> EASE = new java.util.HashMap<>();

    /**
     * Eased 0..1 hover amount for any control, keyed by a stable id: rises in ~120 ms, falls in ~200 ms.
     * Frame-rate independent, and instant with reduced motion.
     */
    public static double ease(String key, boolean on) {
        long now = System.nanoTime();
        double[] state = EASE.computeIfAbsent(key, k -> new double[]{0, now});
        double dt = Math.min(0.1, (now - state[1]) / 1e9);
        state[1] = now;
        boolean reduced = JilatonClient.clientConfig().reducedMotion();
        state[0] = approach(state[0], on ? 1 : 0, dt, on ? 22 : 13, reduced);
        if (EASE.size() > 512) EASE.clear();
        return state[0];
    }

    /** The Jilaton mark (the angular J of the brand icon, {@link dev.jilaton.client.gui.JilatonLogo}), {@code size} px. */
    public static void mark(GuiGraphics g, float x, float y, float size, int color) {
        float u = size / 24F;
        rect(g, x + 4 * u, y + 3 * u, 17 * u, 4 * u, color);
        rect(g, x + 17 * u, y + 7 * u, 4 * u, 10 * u, color);
        rect(g, x + 14 * u, y + 17 * u, 6 * u, 3 * u, color);
        rect(g, x + 7 * u, y + 20 * u, 10 * u, 3 * u, color);
        rect(g, x + 3 * u, y + 13 * u, 4 * u, 8 * u, color);
    }

    /** The mark in a round badge: dark disc, hairline ring, and an optional soft halo ring around it. */
    public static void markTile(GuiGraphics g, float x, float y, float size, float radius, boolean ringed, int accent) {
        float r = size / 2F, cx = x + r, cy = y + r;
        if (ringed) {
            ring(g, cx, cy, r + 8, 1, alpha(accent, 0.18));
            ring(g, cx, cy, r + 16, 1, alpha(accent, 0.07));
        }
        circle(g, cx, cy, r, CARD);
        ring(g, cx, cy, r, 1, alpha(TEXT, 0.16));
        float m = size * 0.52F;
        mark(g, cx - m / 2, cy - m / 2, m, TEXT);
    }

    private static java.util.function.Supplier<net.minecraft.world.entity.player.PlayerSkin> skin;

    /** The signed-in player's face (with hat layer) in a rounded tile of {@code size} board px. */
    public static void face(GuiGraphics g, float x, float y, int size) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (skin == null) skin = minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), false);
        round(g, x, y, size, size, Math.max(4, size / 5F), RAISED);
        int inset = Math.max(3, size / 8);
        net.minecraft.client.gui.components.PlayerFaceRenderer.draw(g, skin.get(), Math.round(x) + inset,
                Math.round(y) + inset, size - inset * 2);
    }

    /** Hit test in board coordinates. */
    public static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    /** Frame-rate independent approach toward {@code target}. */
    public static double approach(double value, double target, double dtSeconds, double rate, boolean reduced) {
        if (reduced) return target;
        return value + (target - value) * (1 - Math.exp(-rate * Math.max(0, dtSeconds)));
    }
}
