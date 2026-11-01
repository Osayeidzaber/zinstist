package dev.jilaton.client.gui.design;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.JilatonType;
import dev.jilaton.client.gui.JilatonUi;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.Map;

/**
 * Jilaton design system, second generation ("Kit"). One source for every screen and overlay:
 *
 * <ul>
 *   <li><b>Colour</b> — true-black base, three charcoal elevation steps, translucent "glass" for anything floating
 *   over the world, hairline separators instead of borders, labels in four strengths. Accent is the user's theme
 *   colour (white by default) and is used sparingly: selection, the primary button, switches that are on.</li>
 *   <li><b>Shape</b> — one radius scale: {@link #R_PANEL} sheets, {@link #R_TILE} tiles and grouped lists,
 *   {@link #R_CONTROL} buttons and fields, {@link #R_PILL} capsules. Elevation is a soft shadow, never a glow.</li>
 *   <li><b>Type</b> — Inter on a fixed scale ({@link #LARGE} .. {@link #CAPTION}); weight carries hierarchy, colour
 *   carries emphasis.</li>
 *   <li><b>Spacing</b> — multiples of 4 board px ({@link #S1} .. {@link #S8}).</li>
 *   <li><b>Motion</b> — {@link #spring} for every state change (hover, press, selection, panels): critically damped,
 *   ~140 ms in, ~220 ms out, scaled by the user's transition speed and instant with reduced motion.</li>
 * </ul>
 *
 * Sizes are board pixels on a {@link Canvas}. Drawing goes through {@link Ui}/{@link Paint} for anti-aliasing.
 */
public final class Kit {
    // ---------------------------------------------------------------- colour
    public static final int BLACK = 0xFF000000;
    /** Base background behind everything. */
    public static final int BASE = 0xFF050506;
    /** Elevation 1: sheets, sidebars. */
    public static final int SURFACE = 0xFF151515;
    /** Elevation 2: tiles, grouped lists, fields. */
    public static final int SURFACE_2 = 0xFF1E1E1E;
    /** Elevation 3: hovered rows, pressed controls, switch tracks. */
    public static final int SURFACE_3 = 0xFF2A2A2A;
    public static final int SEPARATOR = 0xFF2C2C2C;
    public static final int HAIRLINE = 0x24FFFFFF;

    public static final int LABEL = 0xFFF5F5F5;
    public static final int LABEL_2 = 0xFFA3A3A3;
    public static final int LABEL_3 = 0xFF6F6F6F;
    public static final int LABEL_4 = 0xFF494949;

    public static final int GREEN = 0xFF32D74B, RED = 0xFFFF453A, ORANGE = 0xFFFF9F0A;

    // ---------------------------------------------------------------- shape, spacing, type
    public static final float R_PANEL = 24, R_TILE = 16, R_CONTROL = 11, R_PILL = 999;
    public static final float S1 = 4, S2 = 8, S3 = 12, S4 = 16, S5 = 20, S6 = 24, S7 = 32, S8 = 48;
    public static final float LARGE = 40, TITLE = 26, TITLE_2 = 20, HEADLINE = 15, BODY = 14, CALLOUT = 13,
            FOOTNOTE = 12, CAPTION = 11;

    public static final float SWITCH_W = 44, SWITCH_H = 26;

    private Kit() { }

    public static int accent() {
        return Ui.accent();
    }

    /** Accent for large secondary type: the accent itself, or a soft grey when the accent is white. */
    public static int accentSoft() {
        int accent = accent();
        return luminance(accent) > 0.85 ? LABEL_3 : mix(accent, LABEL_2, 0.25);
    }

    /** Accent for thin indicators and rules: full strength. */
    public static int accentStrong() {
        return accent();
    }

    /** Colour drawn on top of the accent (text on the primary button, switch knob when on). */
    public static int onAccent() {
        return JilatonUi.onColor(accent());
    }

    public static int a(int argb, double opacity) {
        return Ui.alpha(argb, opacity);
    }

    public static int mix(int from, int to, double t) {
        return Ui.mix(from, to, t);
    }

    // ---------------------------------------------------------------- motion
    private static final Map<String, double[]> SPRINGS = new HashMap<>();

    /**
     * Eased 0..1 toward {@code on ? 1 : 0} for a stable key. Rises quickly and settles more slowly, the way a
     * press or hover should feel; frame-rate independent.
     */
    public static double spring(String key, boolean on) {
        return spring(key, on ? 1 : 0);
    }

    /** Like {@link #spring(String, double)} but a new key starts at {@code from}, so the first frames animate in. */
    public static double springFrom(String key, double from, double target) {
        SPRINGS.computeIfAbsent(key, k -> new double[]{from, System.nanoTime()});
        return spring(key, target);
    }

    /** Eased value toward any {@code target} (used for sliding thumbs and panel positions). */
    public static double spring(String key, double target) {
        long now = System.nanoTime();
        double[] s = SPRINGS.computeIfAbsent(key, k -> new double[]{target, now});
        double dt = Math.min(0.1, (now - s[1]) / 1e9);
        s[1] = now;
        double speed = JilatonClient.clientConfig() == null ? 1 : JilatonClient.clientConfig().transitionSpeed();
        double rate = (target > s[0] ? 20 : 13) * speed;
        s[0] = Ui.approach(s[0], target, dt, rate, reduced());
        if (SPRINGS.size() > 2048) SPRINGS.clear();
        return s[0];
    }

    public static boolean reduced() {
        return JilatonClient.clientConfig() != null && JilatonClient.clientConfig().reducedMotion();
    }

    /** Smooth 0..1 ease-out for entrance timelines ({@code t} in 0..1). */
    public static double easeOut(double t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - Math.pow(1 - t, 3);
    }

    /** Entrance progress for an element that starts {@code delay} s after {@code openedNanos}, lasting 0.45 s. */
    public static double enter(long openedNanos, double delay) {
        if (reduced()) return 1;
        double speed = JilatonClient.clientConfig().transitionSpeed();
        double t = ((System.nanoTime() - openedNanos) / 1e9 - delay / speed) / (0.45 / speed);
        return easeOut(t);
    }

    // ---------------------------------------------------------------- surfaces

    /** Soft drop shadow under a floating surface; {@code depth} 0..1. */
    public static void shadow(GuiGraphics g, float x, float y, float w, float h, float r, double depth) {
        if (depth <= 0.01) return;
        for (int i = 1; i <= 6; i++) {
            float grow = i * 2.2F;
            Ui.round(g, x - grow, y - grow + i * 1.6F, w + grow * 2, h + grow * 2, r + grow, a(BLACK, 0.06 * depth));
        }
    }

    /** Opaque elevated sheet with a hairline top light. */
    public static void sheet(GuiGraphics g, float x, float y, float w, float h, float r, int fill) {
        Ui.round(g, x, y, w, h, r, fill);
        Paint.outline(g, x, y, w, h, r, 0, HAIRLINE);
    }

    /**
     * Glass: a translucent surface for anything that floats over the world or a backdrop. {@code lift} 0..1 adds
     * the hover elevation (brighter fill, deeper shadow).
     */
    public static void glass(GuiGraphics g, float x, float y, float w, float h, float r, double lift) {
        shadow(g, x, y, w, h, r, 0.5 + 0.5 * lift);
        Ui.round(g, x, y, w, h, r, a(mix(SURFACE, SURFACE_3, lift), 0.78 + 0.1 * lift));
        Paint.outline(g, x, y, w, h, r, 0, a(0xFFFFFFFF, 0.08 + 0.06 * lift));
    }

    /** Grouped inset list background (iOS Settings style); rows inside are separated with {@link #separator}. */
    public static void group(GuiGraphics g, float x, float y, float w, float h) {
        Ui.round(g, x, y, w, h, R_TILE, SURFACE_2);
    }

    /** Separator between rows of a group, inset from the leading edge. */
    public static void separator(GuiGraphics g, float x, float y, float w) {
        Ui.rect(g, x, y, w, 1, SEPARATOR);
    }

    /** Small uppercase section header above a group. */
    public static void sectionHeader(GuiGraphics g, String text, float x, float y) {
        Ui.spaced(g, text.toUpperCase(java.util.Locale.ROOT), x, y, CAPTION, 1.2F, JilatonType.Face.MEDIUM, LABEL_3);
    }

    // ---------------------------------------------------------------- type helpers

    public static void text(GuiGraphics g, String s, float x, float y, float size, JilatonType.Face face, int color) {
        Ui.text(g, s, x, y, size, face, color);
    }

    public static float width(String s, float size, JilatonType.Face face) {
        return Ui.width(s, size, face);
    }

    public static void largeTitle(GuiGraphics g, String s, float x, float y, int color) {
        Ui.text(g, s, x, y, LARGE, JilatonType.Face.BOLD, color);
    }

    /**
     * A light travelling around a rectangle border: a bright head with a fading tail. {@code phase} 0..1 is the
     * head position along the perimeter, {@code amount} 0..1 its strength. Used for hovered and selected surfaces.
     */
    public static void borderSweep(GuiGraphics g, float x, float y, float w, float h, double phase, int color, double amount) {
        if (amount <= 0.01) return;
        float per = 2 * (w + h);
        float tail = Math.min(per * 0.28F, 220);
        int steps = 36;
        for (int i = 0; i < steps; i++) {
            float k = i / (float) steps;
            float d = (float) (((phase * per - k * tail) % per + per) % per);
            float[] p = perimeter(x, y, w, h, d);
            float a = (1 - k) * (1 - k);
            Ui.rect(g, p[0] - 1, p[1] - 1, 2, 2, a(color, a * amount));
        }
    }

    private static float[] perimeter(float x, float y, float w, float h, float d) {
        if (d < w) return new float[]{x + d, y};
        d -= w;
        if (d < h) return new float[]{x + w, y + d};
        d -= h;
        if (d < w) return new float[]{x + w - d, y + h};
        d -= w;
        return new float[]{x, y + h - d};
    }

    /** Seconds since start, frozen with reduced motion; the shared clock for continuous UI motion. */
    public static double clock() {
        return reduced() ? 0 : System.nanoTime() / 1e9;
    }

    // ---------------------------------------------------------------- controls

    /**
     * Switch, {@link #SWITCH_W}x{@link #SWITCH_H}. {@code t} 0..1 is the eased on-amount; {@code press} 0..1
     * stretches the knob like a held thumb.
     */
    public static void toggle(GuiGraphics g, float x, float y, double t, double press) {
        int accent = accent();
        Ui.round(g, x, y, SWITCH_W, SWITCH_H, SWITCH_H / 2, mix(SURFACE_3, accent, t));
        float knob = SWITCH_H - 4;
        float stretch = (float) (5 * press);
        float kx = x + 2 + (float) ((SWITCH_W - knob - 4 - stretch) * t);
        Ui.round(g, kx, y + 3, knob + stretch, knob, knob / 2, a(BLACK, 0.25));
        Ui.round(g, kx, y + 2, knob + stretch, knob, knob / 2, mix(0xFFE8E8E8, onAccentKnob(accent), t));
    }

    /** The knob stays light unless the track is light too (white accent), then it turns dark for contrast. */
    private static int onAccentKnob(int accent) {
        return luminance(accent) > 0.6 ? 0xFF0A0A0A : 0xFFFFFFFF;
    }

    private static double luminance(int argb) {
        int r = (argb >> 16) & 255, gr = (argb >> 8) & 255, b = argb & 255;
        return (0.2126 * r + 0.7152 * gr + 0.0722 * b) / 255.0;
    }

    /**
     * Segmented control: a track with a thumb that slides to the selected segment. Returns nothing; hit-test with
     * {@link #segmentAt}.
     */
    public static void segmented(GuiGraphics g, String key, String[] labels, int selected, float x, float y, float w, float h,
                                 double mx, double my) {
        Ui.round(g, x, y, w, h, R_CONTROL, SURFACE_2);
        float seg = w / labels.length;
        double pos = spring("seg:" + key, selected);
        float tx = x + 2 + (float) (seg * pos);
        shadow(g, tx, y + 2, seg - 4, h - 4, R_CONTROL - 2, 0.35);
        Ui.round(g, tx, y + 2, seg - 4, h - 4, R_CONTROL - 2, SURFACE_3);
        for (int i = 0; i < labels.length; i++) {
            boolean on = i == selected;
            boolean over = !on && Ui.inside(mx, my, x + seg * i, y, seg, h);
            int color = on ? LABEL : over ? mix(LABEL_2, LABEL, 0.5) : LABEL_2;
            JilatonType.Face face = on ? JilatonType.Face.SEMIBOLD : JilatonType.Face.MEDIUM;
            Ui.textCentre(g, labels[i], x + seg * i + seg / 2, y + (h - CALLOUT) / 2 + 1, CALLOUT, face, color);
        }
    }

    public static int segmentAt(int count, float x, float y, float w, float h, double mx, double my) {
        if (!Ui.inside(mx, my, x, y, w, h)) return -1;
        return Math.min(count - 1, (int) ((mx - x) / (w / count)));
    }

    /**
     * Button. {@code style}: 0 primary (accent capsule), 1 secondary (glass), 2 plain (text only). Hover lifts,
     * press sinks; both eased by key.
     */
    public static void button(GuiGraphics g, String key, String label, float x, float y, float w, float h, int style,
                              boolean hovered, boolean pressed) {
        double hv = spring("btn:h:" + key, hovered);
        double pr = spring("btn:p:" + key, pressed);
        float sink = (float) (1.2 * pr);
        y += sink;
        int accent = accent();
        switch (style) {
            case 0 -> {
                shadow(g, x, y, w, h, h / 2, 0.4 + 0.4 * hv - 0.3 * pr);
                Ui.round(g, x, y, w, h, h / 2, mix(accent, mix(accent, 0xFFFFFFFF, 0.5), 0.18 * hv - 0.1 * pr));
                Ui.textCentre(g, label, x + w / 2, y + (h - HEADLINE) / 2 + 1, HEADLINE, JilatonType.Face.SEMIBOLD, onAccent());
            }
            case 1 -> {
                glass(g, x, y, w, h, h / 2, hv * (1 - pr));
                Ui.textCentre(g, label, x + w / 2, y + (h - HEADLINE) / 2 + 1, HEADLINE, JilatonType.Face.MEDIUM, LABEL);
            }
            default -> Ui.textCentre(g, label, x + w / 2, y + (h - CALLOUT) / 2 + 1, CALLOUT, JilatonType.Face.MEDIUM,
                    mix(LABEL_2, LABEL, hv));
        }
    }

    /** Slider track with a round thumb; {@code f} 0..1. */
    public static void slider(GuiGraphics g, String key, float x, float y, float w, double f, boolean active) {
        f = Math.max(0, Math.min(1, f));
        double grab = spring("slider:" + key, active);
        float cy = y + 11;
        Ui.round(g, x, cy - 2, w, 4, 2, SURFACE_3);
        Ui.round(g, x, cy - 2, (float) (w * f), 4, 2, accent());
        float r = (float) (9 + 2 * grab);
        float cx = x + (float) (w * f);
        Ui.circle(g, cx, cy + 1, r, a(BLACK, 0.3));
        Ui.circle(g, cx, cy, r, 0xFFF2F2F2);
    }

    /** Text field background; {@code focus} 0..1 eases the accent ring in. */
    public static void field(GuiGraphics g, float x, float y, float w, float h, double focus) {
        Ui.round(g, x, y, w, h, R_CONTROL, SURFACE_2);
        if (focus > 0.01) Paint.outline(g, x, y, w, h, R_CONTROL, 0, a(accent(), 0.6 * focus));
    }

    /** Small rounded tag (key names, counts, status). */
    public static void tag(GuiGraphics g, String s, float x, float y, int fill, int color) {
        float w = width(s, CAPTION, JilatonType.Face.MEDIUM) + 14;
        Ui.round(g, x, y, w, 20, 10, fill);
        Ui.text(g, s, x + 7, y + 5, CAPTION, JilatonType.Face.MEDIUM, color);
    }

    public static float tagWidth(String s) {
        return width(s, CAPTION, JilatonType.Face.MEDIUM) + 14;
    }

    /** Toast/notification capsule: glass pill with a status dot and a line of text. */
    public static void toast(GuiGraphics g, float x, float y, float w, String text, int dot, double in) {
        float h = 38;
        y -= (float) (8 * (1 - in));
        glass(g, x, y, w, h, h / 2, 0.3);
        Ui.circle(g, x + 20, y + h / 2, 4, a(dot, in));
        Ui.text(g, Ui.fit(text, w - 46, CALLOUT, JilatonType.Face.MEDIUM), x + 34, y + 13, CALLOUT,
                JilatonType.Face.MEDIUM, a(LABEL, in));
    }

    // ---------------------------------------------------------------- icons

    /**
     * Line icons drawn from primitives so they stay crisp at any scale: play, globe, grid, gear, person, music,
     * power, search, folder, sparkle, chevron. {@code size} board px, centred on ({@code cx}, {@code cy}).
     */
    public static void icon(GuiGraphics g, String name, float cx, float cy, float size, int color) {
        float u = size / 24F, s = 1.6F * u;
        switch (name) {
            case "play" -> {
                for (int i = 0; i < 12; i++) {
                    float t = i / 11F;
                    Ui.rect(g, cx - 5 * u + t * 0, cy - 7 * u + t * 7 * u, (12 * u) * (1 - Math.abs(t - 0.5F) * 2) + s, s, color);
                }
            }
            case "globe" -> {
                Ui.ring(g, cx, cy, 9 * u, s, color);
                Ui.rect(g, cx - 9 * u, cy - s / 2, 18 * u, s, color);
                Ui.rect(g, cx - s / 2, cy - 9 * u, s, 18 * u, color);
                Ui.ring(g, cx, cy, 4.5F * u, s * 0.8F, a(color, 0.0));
                Paint.outline(g, cx - 4.5F * u, cy - 9 * u, 9 * u, 18 * u, 4.5F * u, 0, color);
            }
            case "grid" -> {
                float k = 7 * u, gap = 3 * u;
                for (int i = 0; i < 4; i++) {
                    float x = cx - k - gap / 2 + (i % 2) * (k + gap), y = cy - k - gap / 2 + (i / 2) * (k + gap);
                    Ui.round(g, x, y, k, k, 2 * u, color);
                }
            }
            case "gear" -> {
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4;
                    Ui.circle(g, cx + (float) Math.cos(ang) * 8 * u, cy + (float) Math.sin(ang) * 8 * u, 2.2F * u, color);
                }
                Ui.circle(g, cx, cy, 7.5F * u, color);
                Ui.circle(g, cx, cy, 3 * u, SURFACE_2);
            }
            case "person" -> {
                Ui.circle(g, cx, cy - 4 * u, 4 * u, color);
                Ui.round(g, cx - 7 * u, cy + 2 * u, 14 * u, 8 * u, 4 * u, color);
            }
            case "music" -> {
                Ui.circle(g, cx - 4 * u, cy + 5 * u, 3.2F * u, color);
                Ui.circle(g, cx + 5 * u, cy + 3 * u, 3.2F * u, color);
                Ui.rect(g, cx - 1.6F * u, cy - 7 * u, s, 12 * u, color);
                Ui.rect(g, cx + 7.4F * u, cy - 9 * u, s, 12 * u, color);
                Ui.rect(g, cx - 1.6F * u, cy - 9 * u, 9 * u + s, 2.4F * u, color);
            }
            case "power" -> {
                Ui.ring(g, cx, cy + u, 8 * u, s, color);
                Ui.rect(g, cx - 2.2F * u, cy - 10 * u, 4.4F * u, 6 * u, SURFACE_2);
                Ui.round(g, cx - s / 2, cy - 10 * u, s, 9 * u, s / 2, color);
            }
            case "search" -> {
                Ui.ring(g, cx - 2 * u, cy - 2 * u, 6 * u, s, color);
                for (int i = 0; i < 6; i++) Ui.rect(g, cx + 2.5F * u + i * u, cy + 2.5F * u + i * u, s, s, color);
            }
            case "folder" -> {
                Ui.round(g, cx - 9 * u, cy - 6 * u, 8 * u, 4 * u, u, color);
                Ui.round(g, cx - 9 * u, cy - 4 * u, 18 * u, 12 * u, 2 * u, color);
            }
            case "sparkle" -> {
                Ui.round(g, cx - s / 2, cy - 9 * u, s, 18 * u, s / 2, color);
                Ui.round(g, cx - 9 * u, cy - s / 2, 18 * u, s, s / 2, color);
                Ui.circle(g, cx, cy, 3 * u, color);
            }
            case "chevron" -> {
                for (int i = 0; i < 6; i++) {
                    Ui.rect(g, cx - 2 * u + i * 0.8F * u, cy - 5 * u + i * u, s, s, color);
                    Ui.rect(g, cx - 2 * u + i * 0.8F * u, cy + 5 * u - i * u, s, s, color);
                }
            }
            default -> Ui.circle(g, cx, cy, 3 * u, color);
        }
    }
}
