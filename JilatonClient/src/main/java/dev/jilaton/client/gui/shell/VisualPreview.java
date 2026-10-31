package dev.jilaton.client.gui.shell;

import dev.jilaton.client.gui.AnimatedValue;
import dev.jilaton.client.gui.JilatonUi;
import dev.jilaton.client.gui.Motion;
import dev.jilaton.client.render.model.WingMotion;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.Map;

/**
 * Animated preview art for the Visuals cards. Every preview is drawn from a {@link PreviewSpec}
 * produced by {@link PreviewSpecs} for the module's current settings, so the picture always tracks
 * the module. Animation runs through {@link AnimatedValue} and freezes when reduced motion is on.
 */
public final class VisualPreview {
    private static final int BACKDROP = 0xFF07070A;
    private static final int INK = 0xFF000000;

    // SkyThemes.Feature ordinals, mirrored here so the renderer does not depend on the sky renderer.
    private static final int SKY_MILKY = 1 << 1;
    private static final int SKY_AURORA = 1 << 2;
    private static final int SKY_PLANET = 1 << 3;
    private static final int SKY_BLACK_HOLE = 1 << 4;
    private static final int SKY_MOON = 1 << 5;
    private static final int SKY_SUNSET = 1 << 6;
    private static final int SKY_SHOOTING = 1 << 7;
    private static final int SKY_NEBULA = 1 << 8;
    private static final int SKY_STREAKS = 1 << 9;
    private static final int SKY_ALL_DAY = 1 << 10;

    private final Map<String, Channels> channels = new HashMap<>();

    public void render(GuiGraphics graphics, Font font, int x, int y, int w, int h, int radius, String key,
                       PreviewSpec spec, boolean enabled, double hover, boolean reducedMotion, long nowNanos) {
        if (graphics == null || spec == null || w <= 6 || h <= 6) return;
        Channels channel = channels.computeIfAbsent(key == null ? "" : key, ignored -> new Channels());
        double elapsed = channel.lastNanos == 0L ? 0
                : Math.min(0.25, Math.max(0, (nowNanos - channel.lastNanos) / 1_000_000_000.0));
        channel.lastNanos = nowNanos;
        double amount = channel.amount.update(spec.amount(), nowNanos, reducedMotion);
        double density = channel.density.update(spec.density(), nowNanos, reducedMotion);
        double size = channel.size.update(spec.size(), nowNanos, reducedMotion);
        double speed = channel.speed.update(spec.speed(), nowNanos, reducedMotion);
        double spread = channel.spread.update(spec.spread(), nowNanos, reducedMotion);
        double opacity = channel.opacity.update(spec.opacity(), nowNanos, reducedMotion);
        channel.phase = advancePhase(channel.phase, elapsed, speed, reducedMotion);
        channel.sheen = advanceSheen(channel.sheen, elapsed, reducedMotion);

        JilatonUi.fillRounded(graphics, x, y, w, h, radius, BACKDROP);
        Canvas canvas = new Canvas(graphics, x + 2, y + 2, w - 4, h - 4, enabled);
        Params params = new Params(amount, density, size, speed, spread, opacity, channel.phase, channel.sheen);
        switch (spec.motif()) {
            case SKY -> sky(canvas, spec, params);
            case MOTES -> motes(canvas, spec, params);
            case CROSSHAIR -> crosshair(canvas, spec, params);
            case LENS -> lens(canvas, spec, params);
            case WASH -> wash(canvas, spec, params);
            case STEADY -> steady(canvas, spec, params);
            case BURST -> burst(canvas, spec, params);
            case RINGS -> rings(canvas, spec, params);
            case TRAIL -> trail(canvas, spec, params);
            case TARGET -> target(canvas, spec, params);
            case BOX -> box(canvas, spec, params);
            case ORBIT -> orbit(canvas, spec, params);
            case FIGURE -> figure(canvas, spec, params);
            case CROWN -> crown(canvas, spec, params);
            case TOTEM -> totem(canvas, spec, params);
            case PANEL -> panel(canvas, font, spec, params);
            case LYRICS -> lyrics(canvas, spec, params);
            case SWING -> swing(canvas, spec, params);
            case HAND -> hand(canvas, spec, params);
            case CAMERA -> camera(canvas, spec, params);
            case PEARL -> pearl(canvas, spec, params);
            case CORE -> core(canvas, spec, params);
            case DAMAGE -> damage(canvas, font, spec, params);
            case FIRE -> fire(canvas, spec, params);
            case SHARDS -> shards(canvas, spec, params);
            case GLOW -> glow(canvas, spec, params);
            case WINGS -> wings(canvas, spec, params);
            case HALO -> halo(canvas, spec, params);
            case COMPANION -> companion(canvas, spec, params);
            case KNIFE -> knife(canvas, spec, params);
        }

        if (!reducedMotion && params.sheen() > 0) {
            int sweep = canvas.left + (int) Math.round(params.sheen() * Math.max(1, canvas.width()));
            canvas.fill(sweep, canvas.top, sweep + 14, canvas.bottom, 0xFFFFFF, 0.035);
        }
        if (hover > 0.01) canvas.fill(canvas.left, canvas.top, canvas.right, canvas.bottom, 0xFFFFFF, 0.035 * hover);

        JilatonUi.strokeRounded(graphics, x, y, w, h, radius,
                enabled ? ShellPalette.accentBorder(spec.glow() & 0xFFFFFF) : ShellPalette.BORDER);
        if (enabled) {
            int inset = Math.min(radius, w / 2);
            graphics.fill(x + inset, y + h - 2, x + w - inset, y + h - 1,
                    JilatonUi.alpha(spec.glow(), 0.55));
        }
    }

    /** Advancing phase is a pure function so the reduced-motion contract is testable headlessly. */
    static double advancePhase(double phase, double elapsedSeconds, double speed, boolean reducedMotion) {
        double current = Double.isFinite(phase) ? phase : 0;
        if (reducedMotion) return wrap(current);
        double step = Double.isFinite(elapsedSeconds) ? Math.max(0, Math.min(0.25, elapsedSeconds)) : 0;
        return wrap(current + step * (0.35 + 1.65 * Motion.clamp01(speed)));
    }

    static double advanceSheen(double sheen, double elapsedSeconds, boolean reducedMotion) {
        double current = Double.isFinite(sheen) ? sheen : 0;
        if (reducedMotion) return wrap(current);
        double step = Double.isFinite(elapsedSeconds) ? Math.max(0, Math.min(0.25, elapsedSeconds)) : 0;
        return wrap(current + step * 0.11);
    }

    private static double wrap(double value) {
        if (!Double.isFinite(value)) return 0;
        double wrapped = value % 1.0;
        return wrapped < 0 ? wrapped + 1 : wrapped;
    }

    private static final class Channels {
        final AnimatedValue amount = new AnimatedValue();
        final AnimatedValue density = new AnimatedValue();
        final AnimatedValue size = new AnimatedValue();
        final AnimatedValue speed = new AnimatedValue();
        final AnimatedValue spread = new AnimatedValue();
        final AnimatedValue opacity = new AnimatedValue();
        double phase;
        double sheen;
        long lastNanos;
    }

    private record Params(double amount, double density, double size, double speed, double spread,
                          double opacity, double phase, double sheen) { }

    // ---------------------------------------------------------------- motifs

    private static void sky(Canvas c, PreviewSpec s, Params p) {
        double strength = p.opacity();
        int zenith = desaturate(s.primary(), strength);
        int horizon = desaturate(s.secondary(), strength);
        int accent = desaturate(s.glow(), strength);
        int accent2 = desaturate(s.accent2(), strength);
        int starTint = desaturate(s.tint(), strength);
        int detail = s.detail();
        int rows = c.height();
        for (int row = 0; row < rows; row++) {
            double t = rows <= 1 ? 0 : row / (double) (rows - 1);
            int color = JilatonUi.mix(zenith, horizon, Math.pow(t, 0.8));
            if (has(detail, SKY_ALL_DAY)) color = JilatonUi.mix(color, starTint, 0.28);
            c.fill(c.left, c.top + row, c.right, c.top + row + 1, color, 1);
        }
        if (has(detail, SKY_NEBULA)) {
            for (int i = 0; i < 5; i++) {
                int nx = c.left + (int) Math.round(c.width() * (0.15 + 0.17 * i));
                int ny = c.top + (int) Math.round(c.height() * (0.25 + 0.12 * Math.sin(i * 1.7)));
                c.disc(nx, ny, 5 + i, accent2, 0.07 + 0.02 * i);
            }
        }
        if (has(detail, SKY_MILKY)) {
            for (int i = 0; i < 26; i++) {
                double t = i / 25.0;
                int sx = c.left + (int) Math.round(c.width() * (0.08 + 0.84 * t));
                int sy = c.bottom - (int) Math.round(c.height() * (0.18 + 0.62 * t))
                        + (int) Math.round(Math.sin(t * 9) * 2);
                c.fill(sx, sy, sx + 1, sy + 1, JilatonUi.mix(starTint, accent2, 0.35), 0.18);
            }
        }
        if (has(detail, SKY_AURORA)) {
            for (int band = 0; band < 3; band++) {
                int color = JilatonUi.mix(accent, accent2, 0.25 + band * 0.25);
                int base = c.top + (int) Math.round(c.height() * (0.28 + band * 0.13));
                for (int px = 0; px < c.width(); px += 2) {
                    double wave = Math.sin(px / (double) c.width() * 6.283 + p.phase() * 2 + band * 1.3);
                    int py = base + (int) Math.round(wave * c.height() * 0.09);
                    c.fill(c.left + px, py, c.left + px + 2, py + 2, color, 0.14 + 0.05 * band);
                }
            }
        }
        if (has(detail, SKY_MOON)) {
            int mx = c.left + (int) Math.round(c.width() * 0.76);
            int my = c.top + (int) Math.round(c.height() * 0.28);
            int r = 3 + (int) Math.round(p.size() * 3);
            c.disc(mx, my, r, starTint, 0.85);
            c.disc(mx + r / 2, my - 1, r, JilatonUi.mix(zenith, horizon, 0.4), 0.75);
        }
        if (has(detail, SKY_PLANET)) {
            int px = c.left + (int) Math.round(c.width() * 0.74);
            int py = c.top + (int) Math.round(c.height() * 0.36);
            int r = 4 + (int) Math.round(p.size() * 3);
            c.disc(px, py, r, accent2, 0.7);
            c.ellipse(px, py, r + 5, Math.max(1, r / 2), starTint, 0.35, 1);
        }
        if (has(detail, SKY_BLACK_HOLE)) {
            int bx = c.left + (int) Math.round(c.width() * 0.72);
            int by = c.top + (int) Math.round(c.height() * 0.4);
            int r = 5 + (int) Math.round(p.size() * 3);
            c.disc(bx, by, r, INK, 0.95);
            c.ellipse(bx, by, r + 2, r + 2, accent, 0.55, 1);
        }
        if (has(detail, SKY_SUNSET)) {
            int warm = JilatonUi.mix(accent, 0xFFE6B8, 0.55);
            for (int i = 0; i < 5; i++) {
                c.fill(c.left, c.bottom - i - 1, c.right, c.bottom - i, warm, 0.06 + 0.03 * i);
            }
        }
        if (has(detail, SKY_STREAKS)) {
            for (int i = 0; i < 4; i++) {
                int sx = c.left + (int) Math.round(c.width() * (0.18 + 0.2 * i));
                c.fill(sx, c.top + 2, sx + 1, c.top + 2 + c.height() / 3, accent2, 0.16);
            }
        }
        if (has(detail, PreviewSpec.SKY_DOME)) {
            for (int i = 0; i < 4; i++) {
                c.fill(c.left, c.bottom - i - 1, c.right, c.bottom - i,
                        JilatonUi.mix(horizon, INK, 0.4), 0.10 + 0.05 * i);
            }
        }
        if (has(detail, PreviewSpec.SKY_TWINKLE)) {
            int count = (int) Math.round(c.width() * c.height() / 110.0 * (0.3 + 1.4 * p.amount()));
            for (int i = 0; i < count; i++) {
                int sx = c.left + Math.floorMod(hash(i * 7349 + 11), Math.max(1, c.width()));
                int sy = c.top + Math.floorMod(hash(i * 9151 + 29), Math.max(1, (int) (c.height() * 0.9)));
                double twinkle = 0.55 + 0.45 * Math.abs(Math.sin(p.phase() * 2.2 + i * 0.7));
                double alpha = (0.18 + 0.55 * p.density()) * twinkle;
                int dot = i % 9 == 0 ? 2 : 1;
                c.fill(sx, sy, sx + dot, sy + dot, starTint, alpha);
            }
        }
        if (has(detail, SKY_SHOOTING)) {
            double t = frac(p.phase() * 0.35);
            int sx = c.left + (int) Math.round(c.width() * (0.15 + 0.7 * t));
            int sy = c.top + (int) Math.round(c.height() * (0.12 + 0.25 * t));
            c.line(sx, sy, sx - 8, sy - 5, starTint, 0.7 * (1 - t), 1);
        }
    }

    private static void motes(Canvas c, PreviewSpec s, Params p) {
        if (s.variant() >= 10) {
            weather(c, s, p);
            return;
        }
        int style = s.variant();
        int detail = s.detail();
        int count = (int) Math.round(16 + p.amount() * 48);
        boolean updraft = style == 3 || style == 6;
        double fallSpeed = style == 5 ? 0.55 : 0.12;
        for (int i = 0; i < count; i++) {
            double rx = (hash(i * 7349 + 11) & 0xFFFF) / 65535.0;
            double ry = (hash(i * 9151 + 29) & 0xFFFF) / 65535.0;
            double rz = (hash(i * 3571 + 3) & 0xFFFF) / 65535.0;
            double travel = frac(ry + p.phase() * (fallSpeed + 0.4 * p.speed()) * (updraft ? -1 : 1));
            int px = c.left + (int) Math.round(rx * Math.max(1, c.width()));
            int py = c.top + (int) Math.round(travel * Math.max(1, c.height()));
            double sway = style == 0 || style == 1 ? Math.sin(p.phase() * 1.4 + rz * 6.283) * 1.5 : 0;
            px += (int) Math.round(sway);
            int dot = 1 + (int) Math.round(p.size() * 1.4 * rz);
            switch (style) {
                case 2 -> {
                    double pulse = 0.35 + 0.65 * Math.abs(Math.sin(p.phase() * 2 + i));
                    c.fill(px, py, px + dot, py + dot, JilatonUi.mix(s.primary(), s.glow(), 0.45), pulse * p.opacity());
                }
                case 3 -> c.fill(px, py, px + dot, py + dot + 1, s.primary(), 0.55 * p.opacity());
                case 4 -> {
                    c.fill(px, py, px + dot, py + 1, s.primary(), 0.55 * p.opacity());
                    c.fill(px, py - dot, px + 1, py + dot, s.primary(), 0.35 * p.opacity());
                }
                case 5 -> c.fill(px, py, px + 1, py + 3, s.primary(), 0.5 * p.opacity());
                case 6 -> c.line(px, py, px + 4 + (int) Math.round(rz * 4), py, s.primary(), 0.5 * p.opacity(), 1);
                default -> c.fill(px, py, px + dot, py + dot, s.primary(), 0.45 * p.opacity());
            }
        }
        if (has(detail, PreviewSpec.MOTES_OVERWORLD)) {
            c.fill(c.left, c.bottom - 3, c.right, c.bottom - 2, JilatonUi.mix(s.primary(), 0x4C7A4C, 0.5), 0.3);
        }
        if (has(detail, PreviewSpec.MOTES_NIGHT)) {
            int mx = c.right - 6;
            int my = c.top + 5;
            c.disc(mx, my, 3, 0xE8EEFF, 0.75);
            c.disc(mx + 1, my - 1, 3, BACKDROP, 0.75);
        }
    }

    private static void crosshair(Canvas c, PreviewSpec s, Params p) {
        int detail = s.detail();
        int cx = c.cx();
        int cy = c.cy();
        int arm = 3 + (int) Math.round(p.amount() * 12);
        int gap = 1 + (int) Math.round(p.spread() * 5)
                + (int) Math.round(Math.abs(Math.sin(p.phase() * 4)) * p.speed() * 3);
        int thickness = 1 + (int) Math.round(p.size() * 2);
        int color = s.primary();
        double alpha = 0.95 * p.opacity();
        if (s.variant() == 2) {
            c.fill(cx - 1, cy - 1, cx + 2, cy + 2, color, alpha);
            c.ellipse(cx, cy, 4, 4, color, alpha * 0.45, 1);
            return;
        }
        if (s.variant() == 1) {
            c.ellipse(cx, cy, arm + gap, arm + gap, color, alpha, thickness);
            c.fill(cx - 1, cy - 1, cx + 1, cy + 1, color, alpha);
            return;
        }
        for (int side = 0; side < 4; side++) {
            if (has(detail, PreviewSpec.CROSSHAIR_OUTLINE)) {
                int ox0 = side < 2 ? cx - gap - arm - 1 : cx + gap - 1;
                int oy0 = side < 2 ? cy - thickness / 2 - 1 : cy - gap - arm - 1;
                int ox1 = side < 2 ? cx - gap + 1 : cx + gap + arm + 1;
                int oy1 = side < 2 ? cy + thickness / 2 + 1 : cy - gap + 1;
                if (side == 2 || side == 3) {
                    oy1 = side == 2 ? cy - gap + 1 : cy + gap + arm + 1;
                }
                c.fill(ox0 - 1, oy0 - 1, ox1 + 1, oy1 + 1, INK, alpha * 0.55);
            }
            switch (side) {
                case 0 -> c.fill(cx - gap - arm, cy - thickness / 2, cx - gap, cy + thickness - thickness / 2, color, alpha);
                case 1 -> c.fill(cx + gap, cy - thickness / 2, cx + gap + arm, cy + thickness - thickness / 2, color, alpha);
                case 2 -> c.fill(cx - thickness / 2, cy - gap - arm, cx + thickness - thickness / 2, cy - gap, color, alpha);
                default -> c.fill(cx - thickness / 2, cy + gap, cx + thickness - thickness / 2, cy + gap + arm, color, alpha);
            }
        }
        if (has(detail, PreviewSpec.CROSSHAIR_DOT)) c.fill(cx - 1, cy - 1, cx + 1, cy + 1, color, alpha);
        if (has(detail, PreviewSpec.CROSSHAIR_COOLDOWN)) {
            int y = cy + gap + arm + 4;
            int width = 26;
            c.fill(cx - width / 2, y, cx + width / 2, y + 1, color, 0.22);
            double charge = 0.5 + 0.5 * Math.sin(p.phase() * 2);
            c.fill(cx - width / 2, y, cx - width / 2 + (int) Math.round(width * charge), y + 1, color, 0.85);
        }
        if (has(detail, PreviewSpec.CROSSHAIR_THIRD_PERSON)) {
            c.fill(c.right - 5, c.top + 3, c.right - 2, c.top + 5, color, 0.4);
        }
    }

    private static void lens(Canvas c, PreviewSpec s, Params p) {
        int cx = c.cx();
        int cy = c.cy();
        if (s.variant() == 1) {
            int r = 5 + (int) Math.round(p.amount() * 11);
            c.ellipse(cx, cy, r, r, s.primary(), 0.85 * p.opacity(), 2);
            c.line(cx - r + 3, cy, cx + r - 3, cy, s.primary(), 0.5, 1);
            c.line(cx, cy - r + 3, cx, cy + r - 3, s.primary(), 0.5, 1);
            for (int bit = 0; bit < 4; bit++) {
                if (has(s.detail(), 1 << bit)) {
                    c.fill(c.left + 4 + bit * 5, c.bottom - 4, c.left + 6 + bit * 5, c.bottom - 3,
                            s.primary(), 0.55);
                }
            }
        } else {
            double half = Math.toRadians(10 + p.amount() * 42);
            int len = Math.max(8, c.width() - 8);
            for (int side = -1; side <= 1; side += 2) {
                int ex = c.left + 4 + (int) Math.round(Math.cos(half) * len);
                int ey = cy + (int) Math.round(Math.sin(half) * len * side * 0.6);
                c.line(c.left + 4, cy, ex, ey, s.primary(), 0.7 * p.opacity(), 1);
            }
            c.ellipse(c.left + 6, cy, 3, 3, s.primary(), 0.85, 1);
            c.fill(c.left + 4, cy + 1, c.right - 4, cy + 2, s.primary(), 0.18);
            int marker = c.left + 4 + (int) Math.round((c.width() - 8) * p.amount());
            c.fill(marker - 1, cy + 5, marker + 1, cy + 9, s.primary(), 0.8);
        }
    }

    private static void wash(Canvas c, PreviewSpec s, Params p) {
        int rows = c.height();
        for (int row = 0; row < rows; row++) {
            double t = rows <= 1 ? 0 : row / (double) (rows - 1);
            int color = JilatonUi.mix(INK, JilatonUi.mix(s.primary(), 0xFFFFFF, 0.65), t * t * 0.6);
            c.fill(c.left, c.top + row, c.right, c.top + row + 1, color, 1);
        }
        int glow = (int) Math.round(4 + p.amount() * 12);
        for (int i = 0; i < 4; i++) {
            int inset = 6 + i * 5;
            c.fill(c.left + inset, c.top + inset / 2, c.right - inset, c.bottom - inset / 3,
                    JilatonUi.mix(s.primary(), 0xFFFFFF, 0.6), (0.05 + 0.06 * (4 - i)) * (0.35 + p.opacity()));
        }
        c.disc(c.cx(), c.cy() + 2, glow, JilatonUi.mix(s.primary(), 0xFFFFFF, 0.8), 0.10 + 0.30 * p.opacity());
        c.fill(c.left, c.bottom - 3, c.right, c.bottom - 2, s.secondary(), 0.16);
        c.fill(c.left + 6, c.bottom - 7, c.left + 10, c.bottom - 3, 0x0B0B0F, 0.5);
        c.fill(c.right - 10, c.bottom - 7, c.right - 6, c.bottom - 3, 0x0B0B0F, 0.5);
    }

    private static void steady(Canvas c, PreviewSpec s, Params p) {
        c.fill(c.left, c.bottom - 4, c.right, c.bottom - 3, s.primary(), 0.35);
        int cx = c.cx();
        int cy = c.cy() + 2;
        int item = 6;
        c.fill(cx - item, cy - item, cx + item, cy + item, s.primary(), 0.22 * p.opacity());
        c.stroke(cx - item, cy - item, cx + item, cy + item, s.primary(), 0.8);
        c.line(cx + 3, cy + 3, cx + 9, cy + 10, s.primary(), 0.5, 1);
        double amp = p.amount() > 0.5 ? 0.0 : 1.0;
        for (int i = 0; i <= 12; i++) {
            double t = i / 12.0;
            int px = c.left + 4 + (int) Math.round(t * (c.width() - 8));
            int py = c.top + 5 + (int) Math.round(amp * Math.sin(t * 6.283 * 2) * 3);
            c.fill(px, py, px + 1, py + 1, s.secondary(), 0.18);
        }
    }

    private static void burst(Canvas c, PreviewSpec s, Params p) {
        int cx = c.cx();
        int cy = c.cy();
        int detail = s.detail();
        if (has(detail, PreviewSpec.BURST_EFFECTS)) {
            burstEffects(c, s, p, cx, cy);
            return;
        }
        int count = 6 + (int) Math.round(p.amount() * 20);
        double progress = frac(p.phase() * (0.6 + 0.8 * p.speed()));
        int reach = (int) Math.round((4 + p.spread() * 14) * (0.25 + progress));
        for (int i = 0; i < count; i++) {
            double angle = i * 6.283 / Math.max(1, count) + hash(i * 131 + 7) % 100 / 100.0;
            int dx = (int) Math.round(Math.cos(angle) * reach);
            int dy = (int) Math.round(Math.sin(angle) * reach);
            double alpha = (1 - progress) * 0.8 * p.opacity();
            int dot = 1 + (int) Math.round(p.size() * 1.5);
            switch (s.variant()) {
                case 1 -> c.fill(cx + dx, cy + dy, cx + dx + dot, cy + dy + 1, s.primary(), alpha);
                case 2 -> c.line(cx + dx / 2, cy + dy / 2, cx + dx, cy + dy, s.primary(), alpha, 1);
                case 4 -> c.fill(cx + dx / 2, cy + dy / 2 - 1, cx + dx / 2 + dot, cy + dy / 2 + 1, s.primary(), alpha);
                case 5 -> c.fill(cx + dx, cy + dy, cx + 1 + dot, cy + dy + 3, s.primary(), alpha);
                case 6 -> {
                    c.fill(cx + dx - 1, cy + dy, cx + dx + 2, cy + dy + 1, s.primary(), alpha);
                    c.fill(cx + dx, cy + dy - 1, cx + dx + 1, cy + dy + 2, s.primary(), alpha);
                }
                case 7 -> c.fill(cx + dx - 1, cy + dy - 2, cx + dx + 2, cy + dy + 1, s.primary(), alpha);
                case 8 -> {
                    c.disc(cx + dx, cy + dy, 1 + (int) Math.round(p.size()), s.primary(), alpha * 0.7);
                }
                default -> c.fill(cx + dx, cy + dy, cx + dx + dot, cy + dy + dot, s.primary(), alpha);
            }
        }
        if (has(detail, PreviewSpec.BURST_ONLY_CRITS)) c.fill(c.left + 3, c.top + 3, c.left + 5, c.top + 5, s.primary(), 0.5);
        if (has(detail, PreviewSpec.BURST_BODY)) c.ellipse(cx, cy + 8, 9, 3, s.primary(), 0.25, 1);
    }

    private static void burstEffects(Canvas c, PreviewSpec s, Params p, int cx, int cy) {
        int detail = s.detail();
        double progress = frac(p.phase() * (0.5 + 0.7 * p.speed()));
        double fade = 1 - progress;
        int reach = (int) Math.round((5 + p.spread() * 10) * (0.3 + progress));
        if (has(detail, PreviewSpec.BURST_STYLE_BURST)) {
            c.disc(cx, cy, 2 + (int) Math.round(reach * 0.45), s.primary(), 0.25 * fade * p.opacity());
        }
        if (has(detail, PreviewSpec.BURST_STYLE_SMOKE)) {
            for (int i = 0; i < 4; i++) {
                c.disc(cx - 5 + i * 3, cy - 2 - i * 2, 3 + i, s.secondary(), 0.10 * fade);
            }
        }
        if (has(detail, PreviewSpec.BURST_STYLE_INK)) {
            for (int i = 0; i < 6; i++) {
                double angle = i * 1.047;
                int px = cx + (int) Math.round(Math.cos(angle) * reach * 0.4);
                int py = cy + (int) Math.round(Math.sin(angle) * reach * 0.4);
                c.disc(px, py, 2 + i % 2, s.primary(), 0.3 * fade);
            }
        }
        if (has(detail, PreviewSpec.BURST_STYLE_SPARKS)) {
            for (int i = 0; i < 10; i++) {
                double angle = i * 0.628 + hash(i * 97 + 3) % 60 / 60.0;
                c.line(cx, cy, cx + (int) Math.round(Math.cos(angle) * reach),
                        cy + (int) Math.round(Math.sin(angle) * reach), s.primary(), 0.7 * fade, 1);
            }
        }
        if (has(detail, PreviewSpec.BURST_STYLE_STARS)) {
            for (int i = 0; i < 5; i++) {
                double angle = i * 1.256 + 0.4;
                int px = cx + (int) Math.round(Math.cos(angle) * reach * 0.7);
                int py = cy + (int) Math.round(Math.sin(angle) * reach * 0.7);
                c.fill(px - 1, py, px + 2, py + 1, s.primary(), 0.65 * fade);
                c.fill(px, py - 1, px + 1, py + 2, s.primary(), 0.65 * fade);
            }
        }
        if (has(detail, PreviewSpec.BURST_STYLE_ENERGY)) {
            for (int i = 0; i < 14; i++) {
                double t = i / 14.0;
                double angle = t * 8 + p.phase() * 3;
                int px = cx + (int) Math.round(Math.cos(angle) * reach * t);
                int py = cy + (int) Math.round(Math.sin(angle) * reach * t);
                c.fill(px, py, px + 1, py + 1, s.primary(), 0.55 * fade);
            }
        }
        if (has(detail, PreviewSpec.BURST_STYLE_RIPPLE)) {
            c.ellipse(cx, cy, Math.max(2, reach), Math.max(1, reach / 2), s.primary(), 0.5 * fade, 1);
        }
        if (has(detail, PreviewSpec.BURST_STYLE_SHOCKWAVE)) {
            int ry = 3 + (int) Math.round(progress * 4);
            c.ellipse(cx, c.bottom - 4, Math.max(3, reach), ry, s.primary(), 0.45 * fade, 1);
        }
        if (has(detail, PreviewSpec.BURST_STYLE_RINGS)) {
            for (int i = 0; i < 3; i++) {
                double t = frac(progress + i * 0.33);
                c.ellipse(cx, cy, Math.max(2, (int) Math.round((4 + p.spread() * 12) * t)),
                        Math.max(1, (int) Math.round((4 + p.spread() * 12) * t * 0.35)),
                        JilatonUi.mix(s.primary(), s.secondary(), i / 3.0), 0.35 * (1 - t), 1);
            }
        }
        if (has(detail, PreviewSpec.BURST_STYLE_MARKERS)) {
            for (int i = 0; i < 3; i++) {
                int y = cy - 6 + i * 5;
                c.fill(cx + 6, y, cx + 6 + (int) Math.round(8 * (1 - i * 0.2)), y + 1, s.secondary(), 0.35 * fade);
            }
        }
        if (has(detail, PreviewSpec.BURST_STYLE_DOME)) {
            int radius = Math.max(4, reach);
            for (int i = 0; i < 5; i++) {
                double angle = Math.PI + i * Math.PI / 4;
                int px = cx + (int) Math.round(Math.cos(angle) * radius);
                int py = c.bottom - 3 + (int) Math.round(Math.sin(angle) * radius * 0.7);
                c.fill(px, py, px + 1, py + 1, s.primary(), 0.4 * fade);
            }
        }
        if (has(detail, PreviewSpec.BURST_GLOW)) {
            c.disc(cx, cy, 2 + (int) Math.round(reach * 0.25), s.secondary(), 0.12 * fade);
        }
    }

    private static void damage(Canvas c, Font font, PreviewSpec s, Params p) {
        boolean healing = has(s.detail(), PreviewSpec.DAMAGE_HEALING);
        String[] values = healing ? new String[]{"+2.0", "+1.5", "+3.0"} : new String[]{"-4.0", "-2.5", "-6.0"};
        double scale = 0.45 + 0.35 * p.amount();
        for (int i = 0; i < values.length; i++) {
            double progress = frac(p.phase() * (0.6 + 0.5 * p.speed()) + i * 0.37);
            int px = c.left + (int) Math.round(c.width() * (0.22 + 0.26 * i));
            int py = c.bottom - 6 - (int) Math.round(p.spread() * c.height() * 0.55 * progress);
            double alpha = (1 - progress) * p.opacity();
            c.text(font, values[i], px, py, s.primary(), alpha, scale);
        }
    }

    private static void rings(Canvas c, PreviewSpec s, Params p) {
        int detail = s.detail();
        int cx = c.cx();
        int base = c.bottom - 4;
        int count = s.variant() == 1 ? 2 : 1;
        for (int ring = 0; ring < count; ring++) {
            double progress = frac(p.phase() * 0.8 + ring * 0.5);
            int rx = 4 + (int) Math.round((6 + p.spread() * 18) * progress);
            int ry = Math.max(1, rx / 3);
            int thickness = 1 + (int) Math.round(p.size() * 2);
            if (has(detail, PreviewSpec.RINGS_FILL) && progress < 0.7) {
                c.ellipse(cx, base, rx, ry, s.primary(), 0.10 * (1 - progress), Math.max(2, ry));
            }
            c.ellipse(cx, base, rx, ry, s.primary(), (1 - progress) * 0.8 * p.opacity(), thickness);
        }
        if (has(detail, PreviewSpec.RINGS_JUMP)) {
            c.fill(cx - 1, base - 9, cx + 2, base - 5, s.secondary(), 0.4);
        }
        if (has(detail, PreviewSpec.RINGS_LAND)) {
            c.fill(c.left, base + 1, c.right, base + 2, s.primary(), 0.25);
        }
    }

    private static void trail(Canvas c, PreviewSpec s, Params p) {
        boolean ground = s.variant() == 2;
        int width = 1 + (int) Math.round(p.size() * 2);
        int points = 18;
        int startY = c.bottom - 3;
        int headY = ground ? startY : c.top + (int) Math.round((1 - p.amount()) * c.height() * 0.6) + 4;
        int headX = c.right - 8;
        int startX = c.left + 4;
        for (int i = 0; i < points; i++) {
            double t = i / (double) (points - 1);
            int px = startX + (int) Math.round((headX - startX) * t);
            int py = startY + (int) Math.round((headY - startY) * t);
            double alpha = 0.10 + 0.55 * t;
            if (s.variant() == 0) {
                c.fill(px, py - width, px + 3, py + width, s.primary(), alpha * p.opacity());
            } else {
                c.fill(px, py, px + 1, py + (width > 1 ? width : 1), s.primary(), alpha * p.opacity());
            }
        }
        c.disc(headX, headY, 2 + (int) Math.round(p.size()), s.primary(), 0.9 * p.opacity());
        if (has(s.detail(), PreviewSpec.TRAIL_FIRST_PERSON)) {
            c.fill(c.left + 3, c.top + 3, c.left + 6, c.top + 6, s.secondary(), 0.35);
        }
    }

    private static void target(Canvas c, PreviewSpec s, Params p) {
        int cx = c.cx();
        int base = c.bottom - 5;
        figureSilhouette(c, cx, base, s.primary(), 0.75);
        int headY = base - 19;
        switch (s.variant()) {
            case 0 -> {
                c.ellipse(cx, headY - 6, 7, 3, s.primary(), 0.55 * p.opacity(), 1);
                c.ellipse(cx, headY - 6, 9, 4, s.primary(), 0.22 * p.opacity(), 1);
            }
            case 1 -> {
                for (int i = 0; i < 4; i++) {
                    double angle = p.phase() * 1.6 + i * 1.571;
                    int px = cx + (int) Math.round(Math.cos(angle) * 10);
                    int py = base - 9 + (int) Math.round(Math.sin(angle) * 10);
                    c.fill(px, py, px + 2, py + 2, s.primary(), 0.6 * p.opacity());
                }
            }
            case 2 -> {
                for (int i = 0; i < 8; i++) {
                    double t = i / 7.0;
                    double angle = p.phase() * 3 + t * 6.283;
                    int px = cx + (int) Math.round(Math.cos(angle) * 7);
                    int py = base - 2 - (int) Math.round(t * 18);
                    c.fill(px, py, px + 1, py + 1, s.primary(), 0.55 * p.opacity());
                }
            }
            default -> {
                int size = 10 + (int) Math.round(p.amount() * 4);
                int[] xs = {cx - size, cx + size - 3, cx - size, cx + size - 3};
                int[] ys = {base - 24, base - 24, base - 2, base - 2};
                for (int i = 0; i < 4; i++) {
                    int dx = i % 2 == 0 ? 2 : -2;
                    int dy = i < 2 ? 2 : -2;
                    c.fill(xs[i], ys[i], xs[i] + dx, ys[i] + dy, s.primary(), 0.75 * p.opacity());
                }
            }
        }
        if (has(s.detail(), PreviewSpec.TARGET_FLASH)) {
            double flash = Math.max(0, 1 - frac(p.phase()) * 4);
            if (flash > 0) c.ellipse(cx, base - 10, 12, 12, s.secondary(), 0.5 * flash, 1);
        }
        if (has(s.detail(), PreviewSpec.TARGET_WALLS)) {
            outline(c, c.left + 3, c.top + 3, c.right - 3, c.bottom - 3, s.primary(), 0.16, true);
        }
        if (has(s.detail(), PreviewSpec.TARGET_PLAYERS)) {
            c.fill(c.right - 7, c.top + 5, c.right - 5, c.top + 7, s.primary(), 0.5);
        }
    }

    private static void box(Canvas c, PreviewSpec s, Params p) {
        int detail = s.detail();
        boolean item = has(detail, PreviewSpec.BOX_ITEM);
        int size = 16 + (int) Math.round(p.amount() * 6);
        int cx = c.cx();
        int cy = c.cy() - 3;
        int dx = 5;
        int dy = 4;
        int left = cx - size / 2;
        int right = cx + size / 2;
        int top = cy - size / 2;
        int bottom = cy + size / 2;
        int color = s.primary();
        if (has(detail, PreviewSpec.BOX_GLOW)) {
            for (int i = 3; i >= 1; i--) {
                outline(c, left - i, top - i, right + i, bottom + i, color, 0.05 * i, false);
            }
        }
        if (has(detail, PreviewSpec.BOX_BOXES) || has(detail, PreviewSpec.BOX_ITEM)
                || has(detail, PreviewSpec.BOX_OUTLINE)) {
            if (p.amount() > 0.02) c.fill(left, top, right, bottom, color, 0.18 * p.amount());
            outline(c, left, top, right, bottom, color, 0.8, has(detail, PreviewSpec.BOX_THROUGH));
            c.line(right, top, right + dx, top - dy, color, 0.55, 1);
            c.line(right, bottom, right + dx, bottom - dy, color, 0.55, 1);
            c.line(left, top, left + dx, top - dy, color, 0.55, 1);
            outline(c, left + dx, top - dy, right + dx, bottom - dy, color, 0.45, has(detail, PreviewSpec.BOX_THROUGH));
        }
        if (item) {
            int ix = cx - 2;
            int iy = cy - 2;
            c.fill(ix, iy - 2, ix + 4, iy + 3, s.secondary(), 0.7);
            c.fill(ix - 2, iy, ix + 6, iy + 1, s.secondary(), 0.5);
        }
        if (has(detail, PreviewSpec.BOX_TRACERS)) {
            c.line(c.left + 4, c.bottom - 3, left, bottom, color, 0.5 * p.opacity(), 1);
        }
        if (has(detail, PreviewSpec.BOX_STEM)) {
            c.line(cx, bottom, cx, c.bottom - 2, color, 0.5, 1);
        }
    }

    private static void orbit(Canvas c, PreviewSpec s, Params p) {
        int cx = c.cx();
        int cy = c.cy();
        int count = 3 + (int) Math.round(p.amount() * 13);
        int rx = 8 + (int) Math.round(p.spread() * 22);
        int ry = Math.max(3, rx / 3);
        if (has(s.detail(), PreviewSpec.ORBIT_WALLS)) c.ellipse(cx, cy, rx + 2, ry + 2, s.primary(), 0.2, 1);
        for (int i = 0; i < count; i++) {
            double angle = p.phase() * (0.6 + p.speed()) + i * 6.283 / count;
            int px = cx + (int) Math.round(Math.cos(angle) * rx);
            int py = cy + (int) Math.round(Math.sin(angle) * ry)
                    + (int) Math.round(Math.sin(p.phase() * 2 + i) * 3 * p.density());
            int color = i % 2 == 0 ? s.primary() : s.secondary();
            int dot = 1 + (int) Math.round(p.size() * 3);
            if (has(s.detail(), PreviewSpec.ORBIT_TRAIL)) {
                for (int k = 1; k <= 3; k++) {
                    double behind = angle - k * 0.12;
                    int tx = cx + (int) Math.round(Math.cos(behind) * rx);
                    int ty = cy + (int) Math.round(Math.sin(behind) * ry);
                    c.fill(tx, ty, tx + 1, ty + 1, color, 0.22 / k);
                }
            }
            switch (s.variant()) {
                case 2 -> c.fill(px, py, px + dot + 1, py + dot, color, 0.8);
                case 4 -> {
                    c.fill(px - 1, py, px + 2, py + 1, color, 0.8);
                    c.fill(px, py - 1, px + 1, py + 2, color, 0.8);
                }
                case 5 -> c.fill(px, py, px + 2, py + 1, color, 0.75);
                case 7 -> c.ellipse(px, py, dot, dot, color, 0.75, 1);
                default -> c.disc(px, py, dot, color, 0.8);
            }
        }
    }

    private static void figure(Canvas c, PreviewSpec s, Params p) {
        int cx = c.cx();
        int base = c.bottom - 5;
        int detail = s.detail();
        int halo = detail & PreviewSpec.FIGURE_HALO;
        int relics = detail >> PreviewSpec.FIGURE_RELIC_SHIFT & 0x7;
        int wings = detail >> PreviewSpec.FIGURE_WING_SHIFT & 0x7;
        int companion = detail >> PreviewSpec.FIGURE_COMPANION_SHIFT & 0x3;
        int aura = detail >> PreviewSpec.FIGURE_AURA_SHIFT & 0x3;
        int color = s.primary();
        int headY = base - 18;
        if (aura > 0) {
            for (int i = aura; i >= 1; i--) {
                c.disc(cx, base - 8, 6 + i * 3, color, 0.05 * i * p.opacity());
            }
        }
        figureSilhouette(c, cx, base, JilatonUi.mix(color, 0xF2F2F2, 0.45), 0.7);
        if (wings > 0) {
            int span = (int) Math.round((6 + p.size() * 6) * (0.6 + p.spread()));
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 3; i++) {
                    int tipX = cx + side * (span + i * 3);
                    int tipY = base - 16 - i * 3;
                    if (wings == 1) c.line(cx, base - 16, tipX, tipY, color, 0.35, 1);
                    else if (wings == 3 || wings == 5) {
                        c.line(cx, base - 16, tipX, tipY, color, 0.3 + 0.1 * i, 1 + (wings == 5 ? 1 : 0));
                    } else c.line(cx, base - 16, tipX, tipY, color, 0.45, 1);
                }
            }
        }
        if (halo > 0) {
            switch (halo) {
                case 1 -> c.ellipse(cx, headY - 5, 5, 2, color, 0.7 * p.opacity(), 1);
                case 2 -> {
                    c.ellipse(cx, headY - 5, 5, 2, color, 0.7 * p.opacity(), 1);
                    c.ellipse(cx, headY - 8, 7, 2, color, 0.4 * p.opacity(), 1);
                }
                case 3 -> {
                    c.disc(cx, headY - 6, 5, color, 0.25 * p.opacity());
                    c.ellipse(cx, headY - 6, 6, 6, color, 0.65 * p.opacity(), 1);
                }
                case 4 -> {
                    c.ellipse(cx, headY - 6, 6, 6, color, 0.45 * p.opacity(), 1);
                    c.line(cx - 3, headY - 6, cx + 3, headY - 6, color, 0.45, 1);
                    c.line(cx, headY - 9, cx, headY - 3, color, 0.45, 1);
                }
                default -> {
                    c.line(cx - 5, headY - 4, cx - 3, headY - 8, color, 0.7, 1);
                    c.line(cx - 3, headY - 8, cx - 1, headY - 4, color, 0.7, 1);
                    c.line(cx - 1, headY - 4, cx + 1, headY - 8, color, 0.7, 1);
                    c.line(cx + 1, headY - 8, cx + 3, headY - 4, color, 0.7, 1);
                    c.line(cx + 3, headY - 4, cx + 5, headY - 8, color, 0.7, 1);
                }
            }
        }
        int relicCount = 1 + (int) Math.round(p.amount() * 7);
        if (relics > 0) {
            for (int i = 0; i < relicCount; i++) {
                double angle = p.phase() * (0.5 + p.speed()) + i * 6.283 / relicCount;
                int px = cx + (int) Math.round(Math.cos(angle) * 11);
                int py = base - 9 + (int) Math.round(Math.sin(angle) * 5);
                if (has(detail, PreviewSpec.FIGURE_RELIC_TRAIL)) {
                    for (int k = 1; k <= 2; k++) {
                        double behind = angle - k * 0.2;
                        c.fill(cx + (int) Math.round(Math.cos(behind) * 11),
                                base - 9 + (int) Math.round(Math.sin(behind) * 5), 1, 1, color, 0.2 / k);
                    }
                }
                c.fill(px, py, px + 2, py + 2, color, 0.7);
            }
        }
        if (companion > 0) {
            int px = cx + (int) Math.round(9 + Math.sin(p.phase() * 1.5) * 2);
            int py = base - 22 + (int) Math.round(Math.cos(p.phase() * 1.5) * 2);
            c.disc(px, py, companion == 3 ? 2 : 1, s.secondary(), 0.75);
        }
        if (has(detail, PreviewSpec.FIGURE_FIRST_PERSON)) {
            c.fill(c.left + 3, c.top + 3, c.left + 6, c.top + 6, s.secondary(), 0.3);
        }
    }

    private static void crown(Canvas c, PreviewSpec s, Params p) {
        int cx = c.cx();
        int base = c.bottom - 4;
        int headW = 10 + (int) Math.round(p.size() * 6);
        int headTop = c.top + 8;
        int headBottom = base - 8;
        int gold = s.primary();
        int inlay = s.secondary();
        int gem = s.glow();
        c.fill(cx - headW / 2, headTop + 6, cx + headW / 2, headBottom, inlay, 0.65 * p.opacity());
        c.disc(cx, headTop + 4, headW / 2, inlay, 0.8 * p.opacity());
        switch (s.variant()) {
            case 1 -> {
                c.ellipse(cx, headTop, headW / 2 + 2, 6, gold, 0.75 * p.opacity(), 2);
                c.fill(cx - headW / 2, headTop, cx + headW / 2, headTop + 3, gold, 0.55 * p.opacity());
            }
            case 2 -> {
                c.disc(cx, headTop - 4, 6, gold, 0.45 * p.opacity());
                c.ellipse(cx, headTop - 4, 7, 7, gold, 0.8 * p.opacity(), 1);
            }
            default -> {
                int stripes = 2 + (int) Math.round(p.amount() * 6);
                for (int i = 0; i < stripes; i++) {
                    int px = cx - headW / 2 + (int) Math.round(i * headW / (double) stripes);
                    c.fill(px, headTop, px + 1, headBottom, gold, 0.5 * p.opacity());
                }
                c.fill(cx - headW / 2 - 2, headTop, cx + headW / 2 + 2, headTop + 2, gold, 0.7 * p.opacity());
            }
        }
        if (has(s.detail(), PreviewSpec.CROWN_COLLAR)) {
            c.ellipse(cx, headBottom + 1, headW / 2 + 2, 3, gold, 0.45 * p.opacity(), 1);
        }
        if (has(s.detail(), PreviewSpec.CROWN_GEM_GLOW)) {
            c.disc(cx, headTop + 7, 3, gem, 0.35 * p.opacity());
        }
        c.fill(cx - 1, headTop + 6, cx + 2, headTop + 9, gem, 0.9 * p.opacity());
        if (has(s.detail(), PreviewSpec.CROWN_GLINT)) {
            double t = frac(p.phase() * 0.5);
            int gx = c.left + (int) Math.round(t * c.width());
            c.fill(gx, headTop + 6, gx + 2, headTop + 7, 0xFFFFFF, 0.5);
        }
    }

    private static void totem(Canvas c, PreviewSpec s, Params p) {
        int cx = c.cx();
        int base = c.bottom - 5;
        double floatOffset = Math.sin(p.phase() * 1.4) * (2 + p.amount() * 3);
        int foot = base - (int) Math.round(floatOffset);
        int body = s.primary();
        int aura = s.secondary();
        boolean wire = s.variant() == 1;
        int detail = s.detail();
        if (!wire) {
            c.fill(cx - 4, foot - 14, cx + 5, foot, body, 0.55 * p.opacity());
            c.fill(cx - 3, foot - 20, cx + 4, foot - 14, body, 0.75 * p.opacity());
        }
        outline(c, cx - 4, foot - 14, cx + 5, foot, aura, 0.8 * p.opacity(), false);
        outline(c, cx - 3, foot - 20, cx + 4, foot - 14, aura, 0.8 * p.opacity(), false);
        c.fill(cx - 12, foot - 12, cx - 4, foot - 11, aura, 0.6);
        c.fill(cx + 5, foot - 12, cx + 13, foot - 11, aura, 0.6);
        if (has(detail, PreviewSpec.TOTEM_POSE)) {
            c.line(cx - 12, foot - 12, cx - 9, foot - 20, aura, 0.6, 1);
            c.line(cx + 12, foot - 12, cx + 9, foot - 20, aura, 0.6, 1);
        }
        if (has(detail, PreviewSpec.TOTEM_CREST)) {
            int cy = foot - 27;
            c.fill(cx, cy - 4, cx + 2, cy + 5, aura, 0.7);
            c.fill(cx - 3, cy - 1, cx + 5, cy + 1, aura, 0.7);
        }
        if (has(detail, PreviewSpec.TOTEM_SEAL)) {
            c.ellipse(cx, base - 1, 10, 3, aura, 0.4 * p.opacity(), 1);
        }
        if (has(detail, PreviewSpec.TOTEM_WALLS)) {
            outline(c, c.left + 3, c.top + 3, c.right - 3, c.bottom - 3, aura, 0.15, true);
        }
    }

    private static void panel(Canvas c, Font font, PreviewSpec s, Params p) {
        int variant = s.variant();
        int left = c.left + 4;
        int right = c.right - 4;
        int top = c.top + 5;
        int bottom = c.bottom - 5;
        int width = Math.max(8, right - left);
        int cardW = Math.min(width, 18 + (int) Math.round(width * 0.75));
        int cardX = c.cx() - cardW / 2;
        if (variant >= 16) {
            int notes = variant - 16;
            boolean bars = has(s.detail(), PreviewSpec.PANEL_BARS);
            skewPanel(c, cardX, top, cardX + cardW, bottom, (int) Math.round((p.spread() - 0.5) * 6), s.primary());
            if (has(s.detail(), PreviewSpec.PANEL_ART)) {
                c.fill(cardX + 3, top + 3, cardX + 3 + 10, top + 13, s.primary(), 0.6);
            }
            if (has(s.detail(), PreviewSpec.PANEL_DEVICE)) {
                c.fill(cardX + 16, top + 4, cardX + cardW - 4, top + 6, s.primary(), 0.5);
                c.fill(cardX + 16, top + 8, cardX + cardW - 8, top + 9, s.primary(), 0.3);
            }
            if (bars) {
                int barCount = 7;
                for (int i = 0; i < barCount; i++) {
                    double level = 0.25 + 0.75 * Math.abs(Math.sin(p.phase() * 3 + i * 0.9));
                    int bx = cardX + 3 + i * Math.max(2, (cardW - 6) / barCount);
                    int height = (int) Math.round((bottom - top - 6) * level);
                    c.fill(bx, bottom - 3 - height, bx + 2, bottom - 3, s.primary(), 0.45);
                }
            }
            if (notes > 0) {
                for (int i = 0; i < 4; i++) {
                    double t = frac(p.phase() * 0.4 + i * 0.25);
                    int nx = cardX + 4 + (int) Math.round(t * (cardW - 8));
                    int ny = bottom - 4 - (int) Math.round(t * (bottom - top) * 0.7);
                    if (notes == 1) {
                        c.fill(nx - 1, ny, nx + 2, ny + 1, s.secondary(), 0.5);
                        c.fill(nx, ny - 1, nx + 1, ny + 2, s.secondary(), 0.5);
                    } else c.disc(nx, ny, 1, s.secondary(), 0.55);
                }
            }
            return;
        }
        if (variant == 8) {
            int head = 8 + (int) Math.round(p.amount() * 4);
            c.fill(c.cx() - head / 2, bottom - head, c.cx() + head / 2, bottom, s.secondary(), 0.35);
            int nameW = 20 + (int) Math.round(p.size() * 12);
            boolean compact = has(s.detail(), PreviewSpec.PANEL_COMPACT);
            int plateH = compact ? 9 : 14;
            int plateTop = bottom - head - plateH - 2;
            if (has(s.detail(), PreviewSpec.PANEL_BACKGROUND)) {
                c.fill(c.cx() - nameW / 2 - 2, plateTop - 1, c.cx() + nameW / 2 + 2, plateTop + plateH + 1,
                        INK, 0.55 * p.opacity());
            }
            c.fill(c.cx() - nameW / 2, plateTop, c.cx() + nameW / 2, plateTop + 2, s.secondary(), 0.8);
            if (!compact) {
                c.fill(c.cx() - nameW / 2, plateTop + 4, c.cx() + nameW / 2, plateTop + 5, s.primary(), 0.35);
            }
            int barY = plateTop + plateH - 3;
            if (has(s.detail(), PreviewSpec.PANEL_HEALTH)) {
                c.fill(c.cx() - nameW / 2, barY, c.cx() + nameW / 2, barY + 2, INK, 0.6);
                double health = 0.55 + 0.25 * Math.sin(p.phase() * 0.7);
                c.fill(c.cx() - nameW / 2, barY, c.cx() - nameW / 2 + (int) Math.round(nameW * health), barY + 2,
                        JilatonUi.mix(0xFF5A6A, 0x69E59B, health), 0.9);
            }
            if (has(s.detail(), PreviewSpec.PANEL_DURABILITY)) {
                c.fill(c.cx() - nameW / 2, barY + 3, c.cx() - nameW / 2 + nameW / 2, barY + 4, s.primary(), 0.45);
            }
            if (has(s.detail(), PreviewSpec.PANEL_ART)) {
                c.fill(c.cx() - nameW / 2, plateTop + 7, c.cx() - nameW / 2 + 3, plateTop + 9, s.primary(), 0.5);
                c.fill(c.cx() - nameW / 2 + 4, plateTop + 7, c.cx() - nameW / 2 + 7, plateTop + 9, s.primary(), 0.4);
            }
            if (has(s.detail(), PreviewSpec.PANEL_DEVICE)) {
                c.disc(c.cx() + nameW / 2 - 2, plateTop + 2, 1, 0x69E59B, 0.8);
            }
            return;
        }
        int[] widths = {60, 78, 96};
        int wPercent = widths[Math.min(2, Math.max(0, variant))];
        int panelW = Math.max(14, width * wPercent / 100);
        int panelX = c.cx() - panelW / 2;
        c.fill(panelX, top, panelX + panelW, bottom, INK, 0.45);
        int art = has(s.detail(), PreviewSpec.PANEL_ART) ? Math.min(14, panelW / 3) : 0;
        if (art > 0) c.fill(panelX + 2, top + 2, panelX + art, top + art, s.primary(), 0.65);
        int textX = panelX + art + 4;
        c.fill(textX, top + 3, textX + (panelW - art - 8), top + 5, s.secondary(), 0.75);
        c.fill(textX, top + 7, textX + (panelW - art - 14), top + 8, s.primary(), 0.4);
        for (int i = 0; i < 3; i++) {
            double level = 0.25 + 0.75 * Math.abs(Math.sin(p.phase() * 3 + i * 1.1));
            int height = (int) Math.round((bottom - top - 8) * level);
            int bx = textX + i * 3;
            c.fill(bx, bottom - 2 - height, bx + 2, bottom - 2, s.primary(), 0.35);
        }
        if (has(s.detail(), PreviewSpec.PANEL_DEVICE)) {
            c.fill(panelX + panelW - 5, bottom - 4, panelX + panelW - 3, bottom - 2, s.primary(), 0.55);
        }
    }

    private static void lyrics(Canvas c, PreviewSpec s, Params p) {
        int align = s.detail() >> PreviewSpec.LYRICS_ALIGN_SHIFT & 0x3;
        int lines = 1 + (int) Math.round(p.density() * 4);
        int barHeight = 1 + (int) Math.round(p.size() * 2);
        int maxWidth = (int) Math.round(c.width() * (0.45 + p.spread() * 0.45));
        int blockY = c.cy() - lines * 4 + (int) Math.round(Math.sin(p.phase() * 0.8) * 2);
        int offsetX = (s.variant() % 3 - 1) * (c.width() / 6);
        if (has(s.detail(), PreviewSpec.LYRICS_PANEL)) {
            c.fill(c.left + 2, blockY - 3, c.right - 2, blockY + lines * 4 + 2, INK, 0.5);
        }
        for (int i = 0; i < lines; i++) {
            boolean current = i == lines / 2;
            int w = (int) Math.round(maxWidth * (current ? 1.0 : 0.6));
            int x = switch (align) {
                case 1 -> c.left + 5 + offsetX;
                case 2 -> c.right - 5 - w + offsetX;
                default -> c.cx() - w / 2 + offsetX;
            };
            int y = blockY + i * 4;
            double alpha = (current ? 0.85 : 0.3) * p.opacity();
            c.fill(x, y, x + w, y + barHeight, current ? s.primary() : s.secondary(), alpha);
            if (has(s.detail(), PreviewSpec.LYRICS_ACCENTS) && current) {
                c.fill(x - 3, y, x - 2, y + barHeight, s.glow(), 0.6);
                c.fill(x + w + 2, y, x + w + 3, y + barHeight, s.glow(), 0.6);
            }
        }
        if (has(s.detail(), PreviewSpec.LYRICS_OCCLUSION)) {
            outline(c, c.left + 2, c.top + 3, c.right - 2, c.bottom - 3, s.secondary(), 0.14, true);
        }
        if (has(s.detail(), PreviewSpec.LYRICS_THIRD_PERSON)) {
            c.fill(c.right - 7, c.top + 4, c.right - 4, c.top + 6, s.secondary(), 0.3);
        }
    }

    private static void swing(Canvas c, PreviewSpec s, Params p) {
        double progress = frac(p.phase() * 0.9);
        double arc = Math.sin(progress * Math.PI);
        int cx = c.cx();
        int cy = c.cy() + 6;
        double angle;
        switch (s.variant()) {
            case 1 -> angle = progress * Math.PI * 2;
            case 2, 6 -> angle = -0.7 + arc * 0.9;
            case 4 -> angle = -0.3 + arc * 0.35;
            case 5, 9 -> angle = arc * 0.25;
            default -> angle = -0.6 + arc * 1.1;
        }
        int offset = (int) Math.round(arc * 10 * (0.4 + p.amount()));
        int itemX = cx + 4 + offset;
        int itemY = cy - offset / 2;
        int itemW = 6 + (int) Math.round(p.amount() * 5);
        int itemH = itemW * 2;
        for (int i = 1; i <= 3; i++) {
            double trail = progress - i * 0.05;
            if (trail < 0) continue;
            int tx = cx + 4 + (int) Math.round(Math.sin(trail * Math.PI) * 10 * (0.4 + p.amount()));
            int ty = cy - (int) Math.round(Math.sin(trail * Math.PI) * 5);
            c.fill(tx, ty, tx + 2, ty + 2, s.primary(), 0.12 * (4 - i));
        }
        rotatedRect(c, itemX, itemY, itemW, itemH, angle, s.primary(), 0.55 * p.opacity() + 0.1);
        c.line(cx, c.bottom - 2, itemX, itemY + itemH / 2, s.secondary(), 0.3, 1);
    }

    private static void hand(Canvas c, PreviewSpec s, Params p) {
        int detail = s.detail();
        int baseY = c.bottom - 6;
        if (has(detail, PreviewSpec.HAND_MAIN)) {
            int x = c.right - 12 - (int) Math.round(p.spread() * 6);
            int w = 5 + (int) Math.round(p.amount() * 4);
            int h = w * 2;
            double angle = (p.size() - 0.5) * 1.6;
            rotatedRect(c, x, baseY - h, w, h, angle, s.primary(), 0.7);
            c.fill(x + 1, baseY, x + w - 1, baseY + 2, s.secondary(), 0.4);
        }
        if (has(detail, PreviewSpec.HAND_OFF)) {
            int x = c.left + 8 + (int) Math.round(p.spread() * 3);
            int w = 4 + (int) Math.round(p.amount() * 3);
            int h = w * 2;
            rotatedRect(c, x, baseY - h, w, h, -0.4, s.primary(), 0.45);
        }
    }

    private static void camera(Canvas c, PreviewSpec s, Params p) {
        int horizon = c.top + (int) Math.round(c.height() * 0.62);
        c.fill(c.left, horizon, c.right, horizon + 1, s.secondary(), 0.3);
        for (int i = 0; i < 5; i++) {
            int bx = c.left + 4 + i * Math.max(4, c.width() / 5);
            c.fill(bx, horizon - 3 - i % 2 * 2, bx + 3, horizon, s.secondary(), 0.18);
        }
        if (s.variant() == 0) {
            int bodyX = c.cx() - 1;
            int bodyY = c.bottom - 4;
            c.disc(bodyX, bodyY, 2, s.primary(), 0.7);
            int camX = c.left + 10;
            int camY = c.top + 8 + (int) Math.round(Math.sin(p.phase()) * 2 * p.spread());
            boolean smooth = has(s.detail(), PreviewSpec.CAMERA_SMOOTH);
            if (smooth) {
                c.line(bodyX, bodyY, camX, camY, s.primary(), 0.3, 1);
            } else {
                for (int i = 1; i < 5; i++) {
                    double t = i / 5.0;
                    c.fill(bodyX + (int) Math.round((camX - bodyX) * t), bodyY + (int) Math.round((camY - bodyY) * t),
                            1, 1, s.primary(), 0.3);
                }
            }
            c.fill(camX - 4, camY - 3, camX + 4, camY + 2, s.primary(), 0.6);
            c.fill(camX + 4, camY - 1, camX + 6, camY + 1, s.primary(), 0.7);
            if (has(s.detail(), PreviewSpec.CAMERA_BODY)) c.fill(bodyX - 1, bodyY - 4, bodyX + 1, bodyY, s.primary(), 0.45);
            if (has(s.detail(), PreviewSpec.CAMERA_HANDS)) c.fill(camX - 5, camY + 2, camX - 3, camY + 4, s.secondary(), 0.4);
        } else {
            int cx = c.cx();
            int cy = c.cy();
            boolean front = p.spread() > 0.6;
            int dir = front ? 1 : -1;
            c.disc(cx - dir * 8, cy, 4, s.primary(), 0.6);
            double limit = Math.toRadians(25 + p.opacity() * 45);
            for (int side = -1; side <= 1; side += 2) {
                int ex = cx - dir * 8 + dir * 14;
                int ey = cy + (int) Math.round(Math.sin(limit) * 14 * side);
                c.line(cx - dir * 8, cy, ex, ey, s.primary(), 0.45, 1);
            }
            c.fill(cx - 1, cy - 1, cx + 1, cy + 1, s.secondary(), 0.6);
        }
    }

    private static void pearl(Canvas c, PreviewSpec s, Params p) {
        int detail = s.detail();
        int startX = c.left + 5;
        int startY = c.bottom - 4;
        int endX = c.right - 8;
        int endY = c.bottom - 6;
        int peak = Math.max(6, (int) Math.round(c.height() * (0.35 + 0.3 * p.spread())));
        int width = 1 + (int) Math.round(p.size() * 2);
        int steps = 16;
        double head = frac(p.phase() * 0.7);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            int px = startX + (int) Math.round((endX - startX) * t);
            int py = startY - (int) Math.round(Math.sin(t * Math.PI) * peak);
            if (has(detail, PreviewSpec.PEARL_TRAIL) || t <= head) {
                int color = has(detail, PreviewSpec.PEARL_GRADIENT) ? JilatonUi.mix(s.primary(), s.secondary(), t) : s.primary();
                c.fill(px, py, px + width, py + width, color, 0.25 + 0.5 * t);
            }
        }
        double tHead = head;
        int hx = startX + (int) Math.round((endX - startX) * tHead);
        int hy = startY - (int) Math.round(Math.sin(tHead * Math.PI) * peak);
        if (has(detail, PreviewSpec.PEARL_GLOW)) c.disc(hx, hy, 3, s.glow(), 0.25);
        c.disc(hx, hy, 2, s.primary(), 0.95);
        int landing = detail >> PreviewSpec.PEARL_LANDING_SHIFT & 0x3;
        if (landing == 1) c.ellipse(endX, endY, 5, 2, s.glow(), 0.7, 1);
        if (landing == 2) outline(c, endX - 4, endY - 4, endX + 4, endY + 2, s.glow(), 0.7, false);
        if (has(detail, PreviewSpec.PEARL_OTHERS)) {
            c.line(startX, startY, endX - 4, endY - 6, s.secondary(), 0.2, 1);
        }
        if (has(detail, PreviewSpec.PEARL_INFO)) {
            c.fill(c.left + 3, c.top + 3, c.left + 14, c.top + 4, s.secondary(), 0.35);
        }
    }

    private static void core(Canvas c, PreviewSpec s, Params p) {
        int cx = c.cx();
        int cy = c.cy();
        int detail = s.detail();
        int size = 4 + (int) Math.round(p.size() * 4);
        if (has(detail, PreviewSpec.CORE_SPARKLES)) {
            for (int i = 0; i < 6; i++) {
                double t = frac(p.phase() * 0.4 + i * 0.17);
                int px = cx + (int) Math.round(Math.sin(i * 2.1 + p.phase()) * 10);
                int py = cy - (int) Math.round(t * c.height() * 0.6);
                c.fill(px, py, px + 1, py + 1, s.primary(), 0.6 * (1 - t));
            }
        }
        switch (s.variant()) {
            case 1 -> {
                c.ellipse(cx, cy, size + 5, size / 2 + 3, s.primary(), 0.65 * p.opacity(), 1);
                c.ellipse(cx, cy, size + 8, size / 2 + 6, s.primary(), 0.25 * p.opacity(), 1);
            }
            case 2 -> {
                c.ellipse(cx, cy, size + 3, size + 3, s.primary(), 0.55 * p.opacity(), 1);
            }
            default -> {
                for (int i = 0; i < 2; i++) {
                    double t = frac(p.phase() * 0.6 + i * 0.5);
                    c.ellipse(cx, cy, size + 4 + (int) Math.round(t * 8), size + 2 + (int) Math.round(t * 4),
                            s.primary(), 0.5 * (1 - t), 1);
                }
            }
        }
        diamond(c, cx, cy, size, s.primary(), 0.85 * p.opacity());
        c.disc(cx, cy, 1 + size / 3, s.secondary(), 0.8);
        if (has(detail, PreviewSpec.CORE_CRYSTALS)) {
            diamond(c, cx - 9, cy + 5, 3, s.primary(), 0.5);
            diamond(c, cx + 9, cy - 4, 3, s.primary(), 0.5);
        }
        if (has(detail, PreviewSpec.CORE_ANCHORS)) {
            c.fill(cx - 6, c.bottom - 4, cx - 3, c.bottom - 2, s.primary(), 0.4);
            c.fill(cx, c.bottom - 2, cx + 3, c.bottom, s.primary(), 0.4);
        }
    }

    private static void weather(Canvas c, PreviewSpec s, Params p) {
        int mode = s.variant() - 10;
        boolean glow = has(s.detail(), PreviewSpec.WEATHER_GLOW);
        boolean covered = has(s.detail(), PreviewSpec.WEATHER_COVERED);
        int count = (int) Math.round(20 + p.amount() * 90);
        double direction = mode == 2 ? -1 : 1;
        for (int i = 0; i < count; i++) {
            double rx = (hash(i * 7349 + 11) & 0xFFFF) / 65535.0;
            double ry = (hash(i * 9151 + 29) & 0xFFFF) / 65535.0;
            double rz = (hash(i * 3571 + 3) & 0xFFFF) / 65535.0;
            if (covered && rx > 0.55 && ry < 0.35) continue;
            double travel = frac(ry + p.phase() * direction * (0.18 + 0.55 * p.speed()) * (0.7 + rz * 0.6));
            int px = c.left + (int) Math.round(rx * Math.max(1, c.width()));
            int py = c.top + (int) Math.round(travel * Math.max(1, c.height()));
            if (mode == 0) {
                px += (int) Math.round(travel * 3);
                c.fill(px, py, px + 1, py + 3, s.primary(), 0.45 * p.opacity());
            } else if (mode == 1) {
                px += (int) Math.round(Math.sin(p.phase() * 1.6 + rz * 6.283) * 2);
                c.fill(px, py, px + (rz > 0.6 ? 2 : 1), py + 1, s.primary(), 0.6 * p.opacity());
            } else {
                c.fill(px, py, px + 1, py + 1, s.primary(), 0.65 * p.opacity());
            }
            if (glow) c.disc(px, py, 1, s.primary(), 0.13 * p.opacity());
        }
        if (covered) c.fill(c.right - 16, c.top, c.right, c.top + 2, s.primary(), 0.22);
    }

    private static void fire(Canvas c, PreviewSpec s, Params p) {
        int base = c.bottom - (int) Math.round(p.amount() * c.height() * 0.3);
        int height = (int) Math.round(c.height() * (0.30 + 0.45 * p.size()));
        int color = s.primary();
        int hot = JilatonUi.mix(color, 0xFFFFFF, 0.55);
        for (int x = 0; x < Math.max(1, c.width()); x++) {
            double t = x / (double) Math.max(1, c.width());
            double flicker = 0.5 + 0.3 * Math.sin(t * 11 + p.phase() * 6)
                    + 0.2 * Math.sin(t * 27 - p.phase() * 9);
            int tall = (int) Math.round(height * (0.35 + 0.55 * flicker));
            int top = base - tall;
            c.fill(c.left + x, top, c.left + x + 1, base, color, 0.45 * p.opacity());
            c.fill(c.left + x, base - tall / 2, c.left + x + 1, base, hot, 0.30 * p.opacity());
        }
        for (int i = 0; i < 3; i++) {
            int sx = c.left + (int) Math.round(c.width() * (0.2 + 0.3 * i));
            int sy = base - height - 2 - (int) Math.round(frac(p.phase() * 0.6 + i * 0.3) * c.height() * 0.3);
            c.fill(sx, sy, sx + 1, sy + 1, hot, 0.35 * p.opacity());
        }
    }

    private static void shards(Canvas c, PreviewSpec s, Params p) {
        int base = c.bottom - 3;
        double intro = Math.max(0, 1 - frac(p.phase() * (0.4 + p.density())) * 2);
        double body = 0.45 * (1 - intro);
        if (body > 0.01) figureSilhouette(c, c.cx(), base, s.primary(), body);
        int count = 8 + (int) Math.round(p.amount() * 26);
        for (int i = 0; i < count; i++) {
            double rx = (hash(i * 7349 + 17) & 0xFFFF) / 65535.0;
            double rz = (hash(i * 3571 + 5) & 0xFFFF) / 65535.0;
            double progress = frac(p.phase() * (0.25 + 0.5 * p.speed()) + rz);
            int px = c.cx() + (int) Math.round((rx - 0.5) * c.width() * p.spread() * 1.6);
            int py = base - 6 - (int) Math.round(progress * c.height() * 0.8);
            double alpha = (1 - progress) * 0.8 * p.opacity();
            int shard = 1 + (int) Math.round(p.size() * 2);
            c.fill(px, py, px + shard, py + shard, s.primary(), alpha);
            if (has(s.detail(), PreviewSpec.SHARDS_GLOW)) c.disc(px, py, 1, s.primary(), alpha * 0.25);
        }
        if (has(s.detail(), PreviewSpec.SHARDS_PLAYERS)) {
            c.fill(c.left + 3, c.top + 3, c.left + 5, c.top + 7, s.secondary(), 0.22);
        }
        if (has(s.detail(), PreviewSpec.SHARDS_MOBS)) {
            c.fill(c.left + 7, c.top + 4, c.left + 9, c.top + 7, s.secondary(), 0.18);
        }
    }

    private static void glow(Canvas c, PreviewSpec s, Params p) {
        double breathe = has(s.detail(), PreviewSpec.GLOW_PULSE)
                ? 0.72 + 0.28 * Math.sin(p.phase() * 2 * Math.max(0.15, p.speed())) : 1;
        int pad = 2 + (int) Math.round(p.amount() * 4);
        int layers = 3;
        for (int i = layers; i >= 1; i--) {
            int inset = pad * i / 2;
            double alpha = (0.04 + 0.035 * i) * (0.4 + p.size()) * breathe;
            outline(c, c.left + 1 + inset, c.top + 1 + inset / 2, c.right - 1 - inset, c.bottom - 1 - inset / 3,
                    s.primary(), alpha, false);
        }
        c.disc(c.cx(), c.cy() - 2, 5 + (int) Math.round(p.density() * 6),
                s.primary(), 0.06 * breathe);
        figureSilhouette(c, c.cx(), c.bottom - 4, JilatonUi.mix(s.primary(), 0xF2F2F2, 0.45), 0.55);
        if (has(s.detail(), PreviewSpec.GLOW_ITEMS)) {
            c.fill(c.right - 8, c.bottom - 8, c.right - 4, c.bottom - 4, s.secondary(), 0.5);
        }
        if (has(s.detail(), PreviewSpec.GLOW_MOBS)) {
            c.fill(c.left + 4, c.bottom - 8, c.left + 7, c.bottom - 4, s.secondary(), 0.4);
        }
    }

    /**
     * Back view of the wings: fan angles, feather counts and flap offsets come straight from
     * {@link WingMotion}, so the card cannot drift from the geometry the module actually builds.
     */
    private static void wings(Canvas c, PreviewSpec s, Params p) {
        String style = WingMotion.STYLES.get(Math.max(0, Math.min(WingMotion.STYLES.size() - 1, s.variant())));
        boolean animate = has(s.detail(), PreviewSpec.WINGS_ANIMATE);
        int cx = c.cx();
        int base = c.bottom - 4;
        if (has(s.detail(), PreviewSpec.WINGS_OWN)) {
            figureSilhouette(c, cx, base, JilatonUi.mix(s.glow(), 0xF2F2F2, 0.45), 0.3 * p.opacity());
        }
        wingPair(c, s, p, style, animate, cx, c.top + (int) Math.round(c.height() * 0.44),
                Math.min(c.width(), c.height()) * 0.52 * (0.6 + 0.55 * p.size()), 1.0);
        if (has(s.detail(), PreviewSpec.WINGS_OTHERS)) {
            wingPair(c, s, p, style, animate, c.left + 6, c.top + 7,
                    Math.min(c.width(), c.height()) * 0.22, 0.45);
        }
    }

    private static void wingPair(Canvas c, PreviewSpec s, Params p, String style, boolean animate,
                                 int cx, int shoulderY, double unit, double strength) {
        double amplitude = animate ? WingMotion.amplitude(style) : 0;
        boolean energy = style.equals("Energy");
        int layers = WingMotion.layers(style);
        for (int side = -1; side <= 1; side += 2) {
            int featherIndex = 0;
            for (int layer = 0; layer < layers; layer++) {
                int count = WingMotion.feathers(style, layer, 2);
                int rootX = cx + side * (1 + layer);
                int rootY = shoulderY - layer * 2;
                for (int i = 0; i < count; i++) {
                    double fan = Math.toRadians(WingMotion.fanDegrees(i, count));
                    double flap = WingMotion.flapAngle(p.phase(), featherIndex++,
                            WingMotion.baseFlap(style), amplitude);
                    double angle = fan + flap;
                    double length = WingMotion.featherLength(layer, i, count) * unit;
                    int midX = rootX + (int) Math.round(side * Math.cos(angle) * length * 0.55);
                    int midY = rootY - (int) Math.round(Math.sin(angle) * length * 0.55);
                    int tipX = rootX + (int) Math.round(side * Math.cos(angle) * length);
                    int tipY = rootY - (int) Math.round(Math.sin(angle) * length);
                    double fade = strength * p.opacity() * (0.45 + 0.4 * (layer + 1) / layers);
                    int width = style.equals("Blade") ? 1 : 2;
                    c.line(rootX, rootY, midX, midY, s.primary(), fade * 0.7, width);
                    c.line(midX, midY, tipX, tipY, s.secondary(), fade, width);
                    if (energy) c.disc(tipX, tipY, 1, s.glow(), fade * 0.5);
                }
            }
        }
    }

    // ---------------------------------------------------------------- primitives

    /** Tilted torus above a head with the seal bead riding the ring, matching HaloModule's layout. */
    private static void halo(Canvas c, PreviewSpec s, Params p) {
        int cx = c.cx();
        double tilt = s.spread() * 2 - 1;
        int rx = (int) Math.round(c.width() * (0.13 + 0.17 * p.amount()));
        int ry = (int) Math.max(2, Math.round(rx * (0.24 + 0.20 * Math.abs(tilt))));
        int cy = c.top + (int) Math.round(c.height() * 0.42)
                + (int) Math.round(Math.sin(p.phase() * 1.3) * 5 * p.size());
        int weight = Math.max(1, (int) Math.round(1 + p.density() * 30));
        c.disc(cx, cy + ry + 5, 5, s.secondary(), 0.28 * p.opacity());
        c.ellipse(cx, cy, rx, ry, s.primary(), 0.8 * p.opacity(), weight);
        int bx = cx + (int) Math.round(Math.cos(p.phase()) * rx);
        int by = cy + (int) Math.round(Math.sin(p.phase()) * ry);
        c.disc(bx, by, 3, s.glow(), 0.16 * p.opacity());
        c.disc(bx, by, 1, s.glow(), 0.95 * p.opacity());
        if (has(s.detail(), PreviewSpec.HALO_OTHERS)) {
            c.fill(c.left + 4, c.bottom - 9, c.left + 7, c.bottom - 5, s.secondary(), 0.25);
            c.ellipse(c.left + 6, c.bottom - 11, 4, 1, s.primary(), 0.4, 1);
        }
    }

    /** The companion's faceted core on its orbit, with the ring (Sentinel) or tail (Comet). */
    private static void companion(Canvas c, PreviewSpec s, Params p) {
        int cx = c.cx();
        int cy = c.top + (int) Math.round(c.height() * 0.40)
                - (int) Math.round(p.spread() * 4);
        int orbitX = (int) Math.round(c.width() * 0.12 + 8 * p.amount());
        int orbitY = (int) Math.round(orbitX * 0.42);
        c.ellipse(cx, cy, orbitX, orbitY, s.primary(), 0.16 * p.opacity(), 1);
        figureSilhouette(c, cx, c.bottom - 4, JilatonUi.mix(s.primary(), 0xF2F2F2, 0.45), 0.3 * p.opacity());
        int px = cx + (int) Math.round(Math.cos(p.phase()) * orbitX);
        int py = cy + (int) Math.round(Math.sin(p.phase()) * orbitY);
        if (has(s.detail(), PreviewSpec.COMPANION_TAIL)) {
            for (int k = 1; k <= 4; k++) {
                double behind = p.phase() - k * 0.22;
                c.fill(cx + (int) Math.round(Math.cos(behind) * orbitX) - 1,
                        cy + (int) Math.round(Math.sin(behind) * orbitY),
                        2, 2, s.secondary(), 0.28 / k);
            }
        }
        int radius = 2 + (int) Math.round(p.size() * 3);
        c.disc(px, py, radius + 3, s.glow(), 0.10 * p.opacity());
        if (has(s.detail(), PreviewSpec.COMPANION_RING)) {
            c.ellipse(px, py, radius + 3, Math.max(1, radius / 2 + 1), s.primary(), 0.75 * p.opacity(), 1);
        }
        diamond(c, px, py, radius, s.secondary(), 0.9 * p.opacity());
    }

    /** First-person blade entering from the lower right: grip, guard, tapered blade, pommel gem. */
    private static void knife(Canvas c, PreviewSpec s, Params p) {
        int size = (int) Math.round(Math.min(c.width(), c.height()) * (0.34 + 0.42 * p.size()));
        int gripX = c.right - (int) Math.round(c.width() * (0.10 + 0.12 * p.amount()));
        int gripY = c.bottom - (int) Math.round(c.height() * (0.10 + 0.14 * p.density()));
        double sway = Math.sin(p.phase() * 1.6) * 2.5 * p.speed();
        double angle = Math.toRadians(-58 + sway);
        int tipX = gripX + (int) Math.round(Math.cos(angle) * size);
        int tipY = gripY + (int) Math.round(Math.sin(angle) * size);
        double nx = -Math.sin(angle), ny = Math.cos(angle);
        int guardX = gripX + (int) Math.round(nx * 5 * (s.variant() == 2 ? 1.4 : 1));
        int guardY = gripY + (int) Math.round(ny * 5 * (s.variant() == 2 ? 1.4 : 1));
        int segments = 6;
        for (int i = 0; i < segments; i++) {
            double t0 = i / (double) segments, t1 = (i + 1) / (double) segments;
            int x0 = (int) Math.round(gripX + (tipX - gripX) * t0);
            int y0 = (int) Math.round(gripY + (tipY - gripY) * t0);
            int x1 = (int) Math.round(gripX + (tipX - gripX) * t1);
            int y1 = (int) Math.round(gripY + (tipY - gripY) * t1);
            double taper = 1 - t0;
            if (s.variant() == 1 && i >= segments - 1) taper = 0.9;
            int thickness = Math.max(1, (int) Math.round(4 * taper * (s.variant() == 2 ? 1.3 : 1)));
            double alpha = (0.45 + 0.45 * t0) * p.opacity();
            c.line(x0, y0, x1, y1, s.secondary(), alpha, thickness);
            if (i == 2) c.line(x0, y0, x1, y1, s.primary(), 0.3 * p.opacity(), 1);
        }
        c.line(guardX - nx * 5, guardY - ny * 5, guardX + nx * 5, guardY + ny * 5, s.primary(), 0.85 * p.opacity(), 2);
        c.disc(gripX - (int) Math.round(nx * 3), gripY - (int) Math.round(ny * 3), 2, s.glow(), 0.8 * p.opacity());
        c.line(gripX, gripY, gripX - (int) Math.round(nx * 7), gripY - (int) Math.round(ny * 7),
                s.primary(), 0.6 * p.opacity(), 3);
    }

    private static void figureSilhouette(Canvas c, int cx, int base, int color, double alpha) {
        c.disc(cx, base - 22, 2, color, alpha);
        c.fill(cx - 2, base - 19, cx + 2, base - 9, color, alpha);
        c.line(cx - 2, base - 17, cx + 3, base - 17, color, alpha * 0.8, 1);
        c.line(cx - 1, base - 9, cx - 3, base - 2, color, alpha, 1);
        c.line(cx + 1, base - 9, cx + 3, base - 2, color, alpha, 1);
    }

    private static void diamond(Canvas c, int cx, int cy, int size, int color, double alpha) {
        for (int dy = -size; dy <= size; dy++) {
            int span = size - Math.abs(dy);
            c.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, color, alpha);
        }
    }

    private static void rotatedRect(Canvas c, int cx, int cy, int halfW, int halfH, double angle, int color, double alpha) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        int[][] corners = new int[4][2];
        int[][] offsets = {{-halfW, -halfH}, {halfW, -halfH}, {halfW, halfH}, {-halfW, halfH}};
        for (int i = 0; i < 4; i++) {
            corners[i][0] = cx + (int) Math.round(offsets[i][0] * cos - offsets[i][1] * sin);
            corners[i][1] = cy + (int) Math.round(offsets[i][0] * sin + offsets[i][1] * cos);
        }
        for (int i = 0; i < 4; i++) {
            int[] a = corners[i];
            int[] b = corners[(i + 1) % 4];
            c.line(a[0], a[1], b[0], b[1], color, alpha, 1);
        }
        int midX = (corners[0][0] + corners[2][0]) / 2;
        int midY = (corners[0][1] + corners[2][1]) / 2;
        c.fill(midX, midY, midX + 1, midY + 1, color, alpha * 0.6);
    }

    private static void skewPanel(Canvas c, int x0, int y0, int x1, int y1, int skew, int color) {
        int rows = Math.max(1, y1 - y0);
        for (int row = 0; row < rows; row++) {
            double t = rows <= 1 ? 0 : row / (double) (rows - 1);
            int shift = (int) Math.round(skew * (t - 0.5));
            c.fill(x0 + shift, y0 + row, x1 + shift, y0 + row + 1, INK, 0.55);
            c.fill(x0 + shift, y0 + row, x0 + shift + 1, y0 + row + 1, color, 0.35);
        }
    }

    private static void outline(Canvas c, int x0, int y0, int x1, int y1, int color, double alpha, boolean dashed) {
        if (dashed) {
            for (int x = x0; x < x1; x += 3) {
                c.fill(x, y0, Math.min(x + 2, x1), y0 + 1, color, alpha);
                c.fill(x, y1 - 1, Math.min(x + 2, x1), y1, color, alpha);
            }
            for (int y = y0; y < y1; y += 3) {
                c.fill(x0, y, x0 + 1, Math.min(y + 2, y1), color, alpha);
                c.fill(x1 - 1, y, x1, Math.min(y + 2, y1), color, alpha);
            }
        } else {
            c.fill(x0, y0, x1, y0 + 1, color, alpha);
            c.fill(x0, y1 - 1, x1, y1, color, alpha);
            c.fill(x0, y0 + 1, x0 + 1, y1 - 1, color, alpha);
            c.fill(x1 - 1, y0 + 1, x1, y1 - 1, color, alpha);
        }
    }

    private static int desaturate(int rgb, double keep) {
        int grey = (int) Math.round(JilatonUi.luminance(rgb) * 255);
        int base = 0xFF000000 | grey << 16 | grey << 8 | grey;
        return JilatonUi.mix(base, 0xFF000000 | rgb & 0xFFFFFF, Motion.clamp01(keep));
    }

    private static boolean has(int detail, int mask) {
        return (detail & mask) != 0;
    }

    private static double frac(double value) {
        if (!Double.isFinite(value)) return 0;
        double wrapped = value % 1.0;
        return wrapped < 0 ? wrapped + 1 : wrapped;
    }

    private static int hash(int value) {
        int h = value * 0x9E3779B9;
        h ^= h >>> 16;
        h *= 0x7FEB352D;
        h ^= h >>> 15;
        h *= 0x846CA68B;
        h ^= h >>> 16;
        return h & 0x7FFFFFFF;
    }

    private static final class Canvas {
        private final GuiGraphics graphics;
        final int left;
        final int top;
        final int right;
        final int bottom;
        private final boolean enabled;

        Canvas(GuiGraphics graphics, int x, int y, int width, int height, boolean enabled) {
            this.graphics = graphics;
            this.left = x;
            this.top = y;
            this.right = x + Math.max(1, width);
            this.bottom = y + Math.max(1, height);
            this.enabled = enabled;
        }

        int width() { return right - left; }

        int height() { return bottom - top; }

        int cx() { return (left + right) / 2; }

        int cy() { return (top + bottom) / 2; }

        private int color(int rgb) {
            return enabled ? rgb : ShellPalette.dimmed(rgb);
        }

        void fill(int x0, int y0, int x1, int y1, int rgb, double alpha) {
            int a = (int) Math.round(255 * Motion.clamp01(alpha));
            if (a <= 0) return;
            int l = Math.max(left, Math.min(x0, x1));
            int t = Math.max(top, Math.min(y0, y1));
            int r = Math.min(right, Math.max(x0, x1));
            int b = Math.min(bottom, Math.max(y0, y1));
            if (r <= l || b <= t) return;
            graphics.fill(l, t, r, b, a << 24 | color(rgb) & 0xFFFFFF);
        }

        void stroke(int x0, int y0, int x1, int y1, int rgb, double alpha) {
            fill(x0, y0, x1, y0 + 1, rgb, alpha);
            fill(x0, y1 - 1, x1, y1, rgb, alpha);
            fill(x0, y0, x0 + 1, y1, rgb, alpha);
            fill(x1 - 1, y0, x1, y1, rgb, alpha);
        }

        void line(double x0, double y0, double x1, double y1, int rgb, double alpha, int thickness) {
            int px0 = (int) Math.round(x0);
            int py0 = (int) Math.round(y0);
            int px1 = (int) Math.round(x1);
            int py1 = (int) Math.round(y1);
            int dx = Math.abs(px1 - px0);
            int sx = px0 < px1 ? 1 : -1;
            int dy = -Math.abs(py1 - py0);
            int sy = py0 < py1 ? 1 : -1;
            int error = dx + dy;
            int guard = 0;
            while (guard++ < 256) {
                fill(px0, py0, px0 + Math.max(1, thickness), py0 + Math.max(1, thickness), rgb, alpha);
                if (px0 == px1 && py0 == py1) break;
                int doubled = error * 2;
                if (doubled >= dy) {
                    error += dy;
                    px0 += sx;
                }
                if (doubled <= dx) {
                    error += dx;
                    py0 += sy;
                }
            }
        }

        void disc(int cx, int cy, int radius, int rgb, double alpha) {
            if (radius <= 0) return;
            for (int dy = -radius; dy <= radius; dy++) {
                int span = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
                fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, rgb, alpha);
            }
        }

        void ellipse(int cx, int cy, int rx, int ry, int rgb, double alpha, int thickness) {
            if (rx <= 0 || ry <= 0) return;
            int th = Math.max(1, thickness);
            for (int dy = -ry; dy <= ry; dy++) {
                double ratio = dy / (double) ry;
                int span = (int) Math.round(rx * Math.sqrt(Math.max(0, 1 - ratio * ratio)));
                fill(cx - span, cy + dy, cx - span + th, cy + dy + 1, rgb, alpha);
                fill(cx + span - th + 1, cy + dy, cx + span + 1, cy + dy + 1, rgb, alpha);
            }
            for (int dx = -rx; dx <= rx; dx++) {
                double ratio = dx / (double) rx;
                int span = (int) Math.round(ry * Math.sqrt(Math.max(0, 1 - ratio * ratio)));
                fill(cx + dx, cy - span, cx + dx + 1, cy - span + th, rgb, alpha);
                fill(cx + dx, cy + span - th + 1, cx + dx + 1, cy + span + 1, rgb, alpha);
            }
        }

        void text(Font font, String text, int px, int py, int rgb, double alpha, double scale) {
            if (font == null || text.isEmpty()) return;
            int a = (int) Math.round(255 * Motion.clamp01(alpha));
            if (a <= 0) return;
            int color = a << 24 | color(rgb) & 0xFFFFFF;
            if (scale >= 0.999) {
                graphics.drawString(font, text, px, py, color);
            } else {
                graphics.pose().pushMatrix();
                graphics.pose().translate(px, py);
                graphics.pose().scale((float) scale, (float) scale);
                graphics.drawString(font, text, 0, 0, color);
                graphics.pose().popMatrix();
            }
        }
    }
}
