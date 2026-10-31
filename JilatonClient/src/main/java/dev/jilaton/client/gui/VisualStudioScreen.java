package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.SkyThemes;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.Setting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Live preview studio for the custom sky and player cosmetics. It paints a stylised night scene with the
 * selected sky theme and the real cosmetic settings (halo, wings, relics, aura, companion), so presets can
 * be compared before they ever appear in the world. Every control writes straight into the owning module
 * settings through the normal settings objects; nothing here is a mock.
 */
public final class VisualStudioScreen extends Screen {
    /** Inter instead of the vanilla pixel font for everything this screen draws ({@link JilatonType}). */
    private final Font font = JilatonType.font();
    private static final int CARD = 0xFF111112;
    private static final int HAIRLINE = 0xFF232323;

    private final Screen parent;
    private ChoiceSetting skyTheme, skyColorMode, fxColorMode;
    private ChoiceSetting halo, wings, relics, aura, companion;
    private NumberSetting opacity, haloTilt, wingSize;
    private TextSetting skyColor, fxColor;
    private ClientModule skyModule, fxModule;

    private int margin, headerY, previewX, previewY, previewW, previewH;
    private int rightX, rightW;
    private int themeRowY, themeL, themeR;
    private final int[] cellX = new int[6];
    private final int[] cellY = new int[6];
    private int cellW, cellH, cellGap;
    private int slidersY, sliderW, sliderGap;
    private int togglesY, toggleH, toggleGap;
    private boolean tight;
    private int dragging = -1;
    private long openedAt;
    private long lastNanos;
    private double clock;

    public VisualStudioScreen(Screen parent) {
        super(Component.literal("Visual Studio"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModuleManager modules = JilatonClient.moduleManager();
        skyModule = modules.get("custom_sky");
        fxModule = modules.get("player_effects");
        skyTheme = choice("custom_sky", "theme");
        skyColorMode = choice("custom_sky", "color_mode");
        skyColor = text("custom_sky", "color");
        fxColorMode = choice("player_effects", "color_mode");
        fxColor = text("player_effects", "color");
        halo = choice("player_effects", "halo");
        wings = choice("player_effects", "wings");
        relics = choice("player_effects", "relics");
        aura = choice("player_effects", "aura");
        companion = choice("player_effects", "companion");
        opacity = number("player_effects", "opacity");
        haloTilt = number("player_effects", "halo_tilt");
        wingSize = number("player_effects", "wing_size");

        margin = Math.max(16, Math.min(36, width / 24));
        tight = height < 430;
        headerY = margin + (tight ? 44 : 56);
        previewX = margin;
        previewY = headerY;
        previewW = Math.max(260, (int) Math.round((width - margin * 3) * 0.60));
        previewH = height - headerY - margin;
        rightX = previewX + previewW + Math.max(14, margin / 2);
        rightW = width - margin - rightX;
        themeRowY = headerY + (tight ? 14 : 18);
        themeL = rightX + 8;
        themeR = rightX + rightW - 26;
        cellW = (rightW - 6) / 2;
        cellH = tight ? 26 : 30;
        cellGap = tight ? 3 : 4;
        int rowsY = themeRowY + (tight ? 46 : 62);
        for (int i = 0; i < cellX.length; i++) {
            cellX[i] = rightX + (i % 2) * (cellW + 6);
            cellY[i] = rowsY + (i / 2) * (cellH + cellGap);
        }
        sliderGap = tight ? 25 : 29;
        slidersY = rowsY + 3 * (cellH + cellGap) + (tight ? 14 : 18);
        sliderW = rightW - 8;
        toggleH = tight ? 20 : 24;
        toggleGap = tight ? 4 : 4;
        togglesY = slidersY + 2 * sliderGap + (tight ? 22 : 32);
        openedAt = System.nanoTime();
        lastNanos = openedAt;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        double dt = lastNanos == 0L ? 0 : Math.min(0.08, (now - lastNanos) / 1_000_000_000.0);
        lastNanos = now;
        clock += dt * (JilatonWidgets.reducedMotion() ? 0.25 : 1.0);
        boolean reduced = JilatonWidgets.reducedMotion();
        JilatonBackdrop.render(graphics, width, height, JilatonBackdrop.clock(reduced), 0, 0, 1);
        double vis = reduced ? 1 : Math.min(1, (now - openedAt) / 260_000_000.0);
        vis = Motion.SETTLE.apply(vis);

        renderHeader(graphics, vis);
        renderPreview(graphics, vis);
        renderControls(graphics, mouseX, mouseY, vis);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The studio paints its own canvas.
    }

    // ------------------------------------------------------------------ chrome

    private void renderHeader(GuiGraphics graphics, double vis) {
        int top = margin / 2 + 4;
        JilatonLogo.draw(graphics, margin, top, 9, JilatonUi.alpha(JilatonUi.TEXT_2, vis));
        graphics.drawString(font, "JILATON", margin + 14, top + 1, JilatonUi.alpha(JilatonUi.TEXT_2, vis), false);
        graphics.drawString(font, "— VISUAL STUDIO", margin + 18 + font.width("JILATON"), top + 1,
                JilatonUi.alpha(JilatonUi.TEXT_4, vis), false);
        String out = "ESC — BACK";
        graphics.drawString(font, out, width - margin - font.width(out), top + 1, JilatonUi.alpha(JilatonUi.TEXT_4, vis), false);
        graphics.pose().pushMatrix();
        graphics.pose().translate(margin, margin + (tight ? 4 : 6));
        graphics.pose().scale(tight ? 2 : 3, tight ? 2 : 3);
        graphics.drawString(font, Component.literal("Visual Studio").withStyle(net.minecraft.ChatFormatting.BOLD), 0, 0,
                JilatonUi.alpha(JilatonUi.TEXT, vis), false);
        graphics.pose().popMatrix();
        graphics.drawString(font, "Compare skies, halos, wings and effects before they touch the world.", margin + 1,
                margin + (tight ? 28 : 34), JilatonUi.alpha(JilatonUi.TEXT_3, vis), false);
    }

    private void renderPreview(GuiGraphics graphics, double vis) {
        JilatonUi.shadow(graphics, previewX, previewY, previewW, previewH, 8, vis);
        JilatonUi.fillRounded(graphics, previewX, previewY, previewW, previewH, 8, JilatonUi.alpha(0xFF05060A, vis));
        int x0 = previewX + 1, y0 = previewY + 1, x1 = previewX + previewW - 1, y1 = previewY + previewH - 1;
        graphics.enableScissor(x0, y0, x1, y1);
        SkyThemes.Theme theme = currentTheme();
        int horizon = y0 + (int) (previewH * 0.63);
        paintSky(graphics, theme, x0, y0, x1, y1, horizon, vis);
        paintScene(graphics, theme, x0, y0, x1, y1, horizon, vis);
        graphics.disableScissor();
        JilatonUi.strokeRounded(graphics, previewX, previewY, previewW, previewH, 8, JilatonUi.alpha(HAIRLINE, vis));

        String badge = "LIVE PREVIEW";
        int bw = font.width(badge) + 14;
        JilatonUi.fillRounded(graphics, previewX + 14, previewY + 14, bw, 16, 4, JilatonUi.alpha(0xCC08090C, vis));
        JilatonUi.strokeRounded(graphics, previewX + 14, previewY + 14, bw, 16, 4, JilatonUi.alpha(0xFF2C2C2C, vis));
        graphics.drawString(font, badge, previewX + 21, previewY + 18, JilatonUi.alpha(JilatonUi.TEXT_2, vis), false);
        String skyName = theme.name().toUpperCase(Locale.ROOT);
        int sw = font.width(skyName);
        graphics.drawString(font, skyName, previewX + previewW - 14 - sw, previewY + 18, JilatonUi.alpha(JilatonUi.TEXT_3, vis), false);
        String footer = "Sky theme · player cosmetics · real module settings";
        graphics.drawString(font, footer, previewX + 14, previewY + previewH - 18, JilatonUi.alpha(JilatonUi.TEXT_4, vis), false);
    }

    private void renderControls(GuiGraphics graphics, int mouseX, int mouseY, double vis) {
        int y = themeRowY;
        section(graphics, "SKY THEME", rightX, y - 12, vis);
        JilatonUi.fillRounded(graphics, rightX, y, rightW, 26, 5, JilatonUi.alpha(CARD, vis));
        JilatonUi.strokeRounded(graphics, rightX, y, rightW, 26, 5, JilatonUi.alpha(HAIRLINE, vis));
        arrow(graphics, themeL + 6, y + 7, false, hovered(mouseX, mouseY, themeL, y, 22, 12) ? JilatonUi.TEXT : JilatonUi.TEXT_3, vis);
        arrow(graphics, themeR + 2, y + 7, true, hovered(mouseX, mouseY, themeR, y, 22, 12) ? JilatonUi.TEXT : JilatonUi.TEXT_3, vis);
        String name = skyTheme == null ? "Unavailable" : skyTheme.value();
        graphics.drawString(font, name, rightX + (rightW - font.width(name)) / 2, y + 9, JilatonUi.alpha(JilatonUi.TEXT, vis), false);
        if (!tight) {
            String features = themeFeatures();
            graphics.drawString(font, fit(features, rightW - 4), rightX + 1, y + 33, JilatonUi.alpha(JilatonUi.TEXT_3, vis), false);
        }

        int rowsY = cellY[0] - 14;
        section(graphics, "COSMETICS", rightX, rowsY, vis);
        String[] labels = {"Halo", "Wings", "Relics", "Aura", "Companion", "Colour"};
        for (int i = 0; i < cellX.length; i++) {
            int cx = cellX[i], cy = cellY[i];
            boolean over = hovered(mouseX, mouseY, cx, cy, cellW, cellH);
            JilatonUi.fillRounded(graphics, cx, cy, cellW, cellH, 5, JilatonUi.alpha(over ? 0xFF171718 : CARD, vis));
            JilatonUi.strokeRounded(graphics, cx, cy, cellW, cellH, 5, JilatonUi.alpha(HAIRLINE, vis));
            graphics.drawString(font, labels[i].toUpperCase(Locale.ROOT), cx + 9, cy + 4, JilatonUi.alpha(JilatonUi.TEXT_4, vis), false);
            String value = cellValue(i);
            graphics.drawString(font, fit(value, cellW - 34), cx + 9, cy + 16, JilatonUi.alpha(JilatonUi.TEXT, vis), false);
            arrow(graphics, cx + cellW - 22, cy + 10, false, JilatonUi.alpha(JilatonUi.TEXT_3, vis), vis);
            arrow(graphics, cx + cellW - 11, cy + 10, true, JilatonUi.alpha(JilatonUi.TEXT_3, vis), vis);
        }

        section(graphics, "SHAPE", rightX, slidersY - 12, vis);
        slider(graphics, 0, "Opacity", opacity, rightX, slidersY, sliderW, vis);
        slider(graphics, 1, "Halo tilt", haloTilt, rightX, slidersY + sliderGap, sliderW, vis);
        slider(graphics, 2, "Wing size", wingSize, rightX, slidersY + 2 * sliderGap, sliderW, vis);

        section(graphics, "APPLY", rightX, togglesY - 12, vis);
        toggleRow(graphics, "Custom Sky", skyModule != null && skyModule.enabled(), rightX, togglesY, rightW, mouseX, mouseY, vis);
        toggleRow(graphics, "Player Effects", fxModule != null && fxModule.enabled(), rightX, togglesY + toggleH + toggleGap,
                rightW, mouseX, mouseY, vis);
        if (togglesY + 2 * (toggleH + toggleGap) + 16 < height - margin) {
            String hint = "Changes save when you close.";
            graphics.drawString(font, hint, rightX + 1, togglesY + 2 * (toggleH + toggleGap) + 6,
                    JilatonUi.alpha(JilatonUi.TEXT_4, vis), false);
        }
    }

    private void section(GuiGraphics graphics, String label, int x, int y, double vis) {
        graphics.drawString(font, label, x, y, JilatonUi.alpha(JilatonUi.TEXT_3, vis), false);
        graphics.fill(x, y + 11, x + rightW - 2, y + 12, JilatonUi.alpha(HAIRLINE, vis));
    }

    private void slider(GuiGraphics graphics, int index, String label, NumberSetting setting, int x, int y, int w, double vis) {
        double fraction = setting == null ? 0 : (setting.value() - setting.minimum()) / (setting.maximum() - setting.minimum());
        String value = setting == null ? "—" : switch (setting.id()) {
            case "halo_tilt" -> Math.round(setting.value()) + "°";
            case "wing_size" -> String.format(Locale.ROOT, "%.2f", setting.value());
            default -> Math.round(setting.value() * 100) + "%";
        };
        graphics.drawString(font, label, x, y, JilatonUi.alpha(JilatonUi.TEXT_2, vis), false);
        graphics.drawString(font, value, x + w - font.width(value), y, JilatonUi.alpha(JilatonUi.TEXT_3, vis), false);
        int ty = y + 14;
        graphics.fill(x, ty, x + w, ty + 2, JilatonUi.alpha(0xFF2A2A2A, vis));
        graphics.fill(x, ty, x + (int) Math.round(w * fraction), ty + 2, JilatonUi.alpha(accent(), vis));
        graphics.fill(x + (int) Math.round(w * fraction) - 2, ty - 3, x + (int) Math.round(w * fraction) + 3, ty + 5,
                JilatonUi.alpha(JilatonUi.TEXT, vis));
    }

    private void toggleRow(GuiGraphics graphics, String label, boolean on, int x, int y, int w, int mouseX, int mouseY, double vis) {
        boolean over = hovered(mouseX, mouseY, x, y, w, toggleH);
        JilatonUi.fillRounded(graphics, x, y, w, toggleH, 5, JilatonUi.alpha(over ? 0xFF171718 : CARD, vis));
        JilatonUi.strokeRounded(graphics, x, y, w, toggleH, 5, JilatonUi.alpha(HAIRLINE, vis));
        graphics.drawString(font, label, x + 9, y + (toggleH - 8) / 2, JilatonUi.alpha(JilatonUi.TEXT, vis), false);
        int tx = x + w - 42, ty = y + (toggleH - 12) / 2;
        JilatonUi.fillRounded(graphics, tx, ty, 32, 12, 6, JilatonUi.alpha(on ? accent() : 0xFF2A2A2A, vis));
        int knob = tx + (on ? 21 : 3);
        JilatonUi.fillRounded(graphics, knob, ty + 2, 8, 8, 4, JilatonUi.alpha(on ? 0xFF0A0A0A : 0xFF8A8A8A, vis));
    }

    private void arrow(GuiGraphics graphics, int x, int y, boolean right, int color, double vis) {
        int c = JilatonUi.alpha(color, vis);
        if (right) {
            line(graphics, x, y, x + 4, y + 4, c, 1);
            line(graphics, x + 4, y + 4, x, y + 8, c, 1);
        } else {
            line(graphics, x + 4, y, x, y + 4, c, 1);
            line(graphics, x, y + 4, x + 4, y + 8, c, 1);
        }
    }

    // ------------------------------------------------------------------ preview scene

    private void paintSky(GuiGraphics graphics, SkyThemes.Theme theme, int x0, int y0, int x1, int y1, int horizon, double vis) {
        int zenith = 0xFF000000 | theme.zenith();
        int edge = 0xFF000000 | theme.horizon();
        int accent = 0xFF000000 | theme.accent();
        int skyH = Math.max(1, horizon - y0);
        int lift = EffectBatch.mixRgb(edge, 0xFFFFFF, 0.12);
        for (int y = y0; y < horizon; y++) {
            double t = (y - y0) / (double) skyH;
            int color = EffectBatch.mixRgb(zenith, edge, Math.pow(t, 0.85));
            double band = Math.exp(-Math.pow((t - 0.94) * 6.0, 2));
            color = EffectBatch.mixRgb(color, accent, band * 0.42);
            color = EffectBatch.mixRgb(color, lift, 0.22 * Math.pow(t, 2.4));
            graphics.fill(x0, y, x1, y + 1, JilatonUi.alpha(color, vis));
        }
        // Wide atmospheric bloom hugging the horizon so the canvas never reads as flat black.
        int glowH = Math.max(40, (int) (skyH * 0.34));
        int glowTint = EffectBatch.mixRgb(accent, edge, 0.35);
        for (int i = 0; i < glowH; i++) {
            double t = i / (double) glowH;
            graphics.fill(x0, horizon - glowH + i, x1, horizon - glowH + i + 1,
                    JilatonUi.alpha(glowTint, Math.pow(t, 2.3) * 0.16 * vis));
        }
        graphics.fill(x0, horizon - 1, x1, horizon,
                JilatonUi.alpha(EffectBatch.mixRgb(accent, 0xFFFFFF, 0.45), 0.22 * vis));
        for (int y = horizon; y < y1; y++) {
            double t = (y - horizon) / (double) Math.max(1, y1 - horizon);
            int color = EffectBatch.mixRgb(edge, 0xFF040406, 0.30 + 0.70 * t);
            graphics.fill(x0, y, x1, y + 1, JilatonUi.alpha(color, vis));
        }
    }

    private void paintScene(GuiGraphics graphics, SkyThemes.Theme theme, int x0, int y0, int x1, int y1, int horizon, double vis) {
        int w = x1 - x0, h = y1 - y0;
        int accent = 0xFF000000 | theme.accent();
        int accent2 = 0xFF000000 | theme.accent2();
        int white = 0xFFF4F6FF;
        List<SkyThemes.Feature> features = theme.features();
        boolean reduced = JilatonWidgets.reducedMotion();
        double t = reduced ? clock * 0.35 : clock;

        if (features.contains(SkyThemes.Feature.NEBULA)) nebula(graphics, x0, y0, w, h, accent, accent2, white, vis);
        if (features.contains(SkyThemes.Feature.MILKY_WAY)) milkyWay(graphics, x0, y0, w, h, white, accent, vis);
        if (features.contains(SkyThemes.Feature.AURORA)) aurora(graphics, x0, y0, w, h, horizon, accent, accent2, t, vis);
        if (features.contains(SkyThemes.Feature.SUNSET_GLOW)) sunset(graphics, x0, w, horizon, accent, accent2, vis);
        stars(graphics, x0, y0, w, h, theme, t, vis);
        if (features.contains(SkyThemes.Feature.PLANET)) planet(graphics, x0 + (int) (w * 0.75), y0 + (int) (h * 0.25), accent, accent2, vis);
        if (features.contains(SkyThemes.Feature.BLACK_HOLE)) blackHole(graphics, x0 + (int) (w * 0.24), y0 + (int) (h * 0.22), accent, accent2, vis);
        if (features.contains(SkyThemes.Feature.MOON)) moon(graphics, x0 + (int) (w * 0.80), y0 + (int) (h * 0.18), vis);
        if (features.contains(SkyThemes.Feature.LIGHT_STREAKS)) lightStreaks(graphics, x0, y0, w, h, accent, white, t, vis);
        horizonMist(graphics, x0, w, horizon, accent, vis);
        terrain(graphics, x0, horizon, w, h, accent2, vis);
        backlight(graphics, x0 + w / 2, horizon - (int) (h * 0.16), accent2, vis);
        player(graphics, x0 + w / 2, y0 + (int) (h * 0.82), horizon, t, vis);
    }

    /** Two dark ridges below the horizon give the composition a grounded foreground. */
    private void terrain(GuiGraphics graphics, int x0, int horizon, int w, int h, int tint, double vis) {
        for (int layer = 0; layer < 2; layer++) {
            int base = horizon + (int) (h * (0.02 + layer * 0.03));
            int color = EffectBatch.mixRgb(0xFF080A0F, tint, 0.05 + layer * 0.04);
            int prevY = base;
            for (int x = x0; x < x0 + w; x += 2) {
                double u = (x - x0) / (double) w;
                double ridge = Math.sin(u * (6.1 + layer * 2.3) + layer * 1.7) * 0.5
                        + Math.sin(u * (15.7 - layer * 3.1) - layer) * 0.22;
                int y = base - (int) (Math.max(0, ridge + 0.55) * h * 0.034 * (1 - layer * 0.3));
                prevY = Math.min(prevY, y);
                graphics.fill(x, y, x + 2, horizon + (int) (h * 0.05), JilatonUi.alpha(color, (0.94 - layer * 0.10) * vis));
            }
            if (prevY < base) {
                graphics.fill(x0, prevY, x0 + w, prevY + 1, JilatonUi.alpha(EffectBatch.mixRgb(color, tint, 0.35), 0.5 * vis));
            }
        }
    }

    private void backlight(GuiGraphics graphics, int cx, int cy, int color, double vis) {
        for (int i = 12; i >= 1; i--) {
            circle(graphics, cx, cy, i * 10, JilatonUi.alpha(color, 0.006 * vis));
        }
    }

    private void stars(GuiGraphics graphics, int x0, int y0, int w, int h, SkyThemes.Theme theme, double t, double vis) {
        int n = (int) (130 * Math.min(1.4, theme.stars()));
        int tint = 0xFF000000 | theme.starTint();
        for (int i = 0; i < n; i++) {
            double hx = hash(i, 11), hy = hash(i, 29), hs = hash(i, 47), hp = hash(i, 71);
            int x = x0 + 4 + (int) (hx * (w - 8));
            int y = y0 + 4 + (int) (hy * (h * 0.60));
            double twinkle = 0.55 + 0.45 * Math.sin(t * (1.1 + hs * 1.6) + hp * 6.28);
            int a = (int) Math.round(40 + 170 * twinkle * (0.4 + hs * 0.6));
            int color = JilatonUi.alpha(EffectBatch.mixRgb(tint, 0xFFFFFF, hs * 0.35), (a / 255.0) * vis);
            if (hs > 0.93) {
                graphics.fill(x - 2, y, x + 3, y + 1, color);
                graphics.fill(x, y - 2, x + 1, y + 3, color);
            } else {
                graphics.fill(x, y, x + 1 + (hs > 0.65 ? 1 : 0), y + 1 + (hs > 0.65 ? 1 : 0), color);
            }
        }
    }

    private void nebula(GuiGraphics graphics, int x0, int y0, int w, int h, int accent, int accent2, int white, double vis) {
        for (int i = 0; i < 34; i++) {
            double hx = hash(i, 131), hy = hash(i, 173), hs = hash(i, 211);
            int cx = x0 + (int) (w * (0.10 + hx * 0.80));
            int cy = y0 + (int) (h * (0.08 + hy * 0.42));
            int size = (int) (26 + hs * 64);
            int color = (i % 3 == 0) ? accent : accent2;
            circle(graphics, cx, cy, size, JilatonUi.alpha(color, (0.022 + hs * 0.028) * vis));
            if (hs > 0.82) circle(graphics, cx, cy, size / 3, JilatonUi.alpha(white, 0.035 * vis));
        }
        for (int i = 0; i < 26; i++) {
            double hx = hash(i, 307), hy = hash(i, 331);
            int x = x0 + (int) (w * (0.15 + hx * 0.70));
            int y = y0 + (int) (h * (0.10 + hy * 0.40));
            graphics.fill(x, y, x + 1, y + 1, JilatonUi.alpha(0xFFFFFF, 0.45 * vis));
        }
    }

    private void milkyWay(GuiGraphics graphics, int x0, int y0, int w, int h, int white, int accent, double vis) {
        for (int i = 0; i < 46; i++) {
            double t = i / 45.0;
            double nx = 0.14 + t * 0.72 + (hash(i, 401) - 0.5) * 0.10;
            double ny = 0.80 - t * 0.55 + (hash(i, 419) - 0.5) * 0.10;
            int cx = x0 + (int) (w * nx), cy = y0 + (int) (h * ny);
            int size = (int) (10 + hash(i, 431) * 20);
            circle(graphics, cx, cy, size, JilatonUi.alpha(EffectBatch.mixRgb(white, accent, hash(i, 443) * 0.5), 0.030 * vis));
        }
    }

    private void aurora(GuiGraphics graphics, int x0, int y0, int w, int h, int horizon, int accent, int accent2, double t, double vis) {
        for (int layer = 0; layer < 3; layer++) {
            int baseY = horizon - (int) (h * (0.08 + layer * 0.045));
            for (int x = x0; x < x0 + w; x += 3) {
                double u = (x - x0) / (double) w;
                double az = u * Math.PI * 2;
                double fold = 0.5 + 0.5 * Math.sin(az * 3.1 + t * 0.35 + layer * 2.1) * Math.sin(az * 6.7 - t * 0.28 + layer);
                int height = (int) (h * (0.06 + 0.16 * fold) * (1 - layer * 0.12));
                int top = baseY - height;
                int chunks = 8;
                for (int c = 0; c < chunks; c++) {
                    double f = c / (double) chunks;
                    int y = top + (int) (height * f);
                    int color = EffectBatch.mixRgb(accent2, accent, 1 - f);
                    double alpha = 0.16 * (1 - f) * (0.55 + 0.45 * fold);
                    graphics.fill(x, y, x + 3, y + height / chunks + 1, JilatonUi.alpha(color, alpha * vis));
                }
                graphics.fill(x, baseY - 1, x + 3, baseY + 1, JilatonUi.alpha(accent, 0.18 * fold * vis));
            }
        }
    }

    private void sunset(GuiGraphics graphics, int x0, int w, int horizon, int accent, int accent2, double vis) {
        for (int y = horizon - 90; y < horizon + 4; y++) {
            double t = (horizon - y) / 90.0;
            int color = EffectBatch.mixRgb(accent, accent2, 1 - t);
            graphics.fill(x0, y, x0 + w, y + 1, JilatonUi.alpha(color, 0.035 * Math.pow(t, 0.7) * vis));
        }
    }

    private void horizonMist(GuiGraphics graphics, int x0, int w, int horizon, int accent, double vis) {
        int height = Math.max(24, (int) (horizon * 0.20));
        for (int i = 0; i < height; i++) {
            double t = i / (double) height;
            int y = horizon - height + i;
            graphics.fill(x0, y, x0 + w, y + 1, JilatonUi.alpha(accent, 0.030 * Math.pow(t, 1.7) * vis));
        }
        graphics.fill(x0, horizon, x0 + w, horizon + 1, JilatonUi.alpha(accent, 0.05 * vis));
    }

    private void planet(GuiGraphics graphics, int cx, int cy, int accent, int accent2, double vis) {
        int body = 52;
        double scale = body / 36.0;
        circle(graphics, cx, cy, (int) (body * 3), JilatonUi.alpha(EffectBatch.mixRgb(accent, 0xFF000000, 0.25), 0.05 * vis));

        int[] colors = {
                EffectBatch.mixRgb(accent, 0xFFF3E2C4, 0.55),
                EffectBatch.mixRgb(accent, 0xFFD8C6AC, 0.25),
                accent2,
                EffectBatch.mixRgb(accent2, 0xFFFFFF, 0.30)
        };
        double[] radii = {1.34, 1.50, 1.55, 1.88, 1.93, 2.14, 2.19, 2.25};
        double[] alpha = {0.52, 0.40, 0.22, 0.10};
        ringHalf(graphics, cx, cy, body, radii, colors, alpha, false, vis);

        int night = 0xFF06070C;
        int lit = EffectBatch.mixRgb(accent, 0xFFEFDCBB, 0.60);
        for (int r = body; r >= 1; r--) {
            double f = 1 - r / (double) body;
            double l = Math.pow(f, 0.9);
            int surface = EffectBatch.mixRgb(night, lit, 0.10 + 0.88 * l);
            circle(graphics, cx - (int) Math.round(2.5 * l), cy - (int) Math.round(2.0 * l), r,
                    JilatonUi.alpha(surface, vis));
        }
        // Latitude bands: chords across the disc, alternating warm and dusty tints.
        for (int i = 0; i < 7; i++) {
            int dy = (int) Math.round((-19 + i * 6) * scale);
            int half = (int) Math.sqrt(Math.max(0, (body - 1) * (body - 1) - dy * dy));
            if (half <= 2) continue;
            int tint = i % 2 == 0
                    ? JilatonUi.alpha(0xFFF2E3C8, 0.06 * vis)
                    : JilatonUi.alpha(0xFF1B1710, 0.08 * vis);
            graphics.fill(cx - half + 1, cy + dy, cx + half - 3, cy + dy + (i == 3 ? 4 : 3), tint);
        }
        // Soft terminator: the night side falls away in steps rather than a hard cut.
        for (int i = 1; i <= 6; i++) {
            circle(graphics, cx + 4 + (int) (i * 3 * scale), cy + 1, Math.max(4, body - 1 - i),
                    JilatonUi.alpha(0xFF04050A, 0.085 * vis));
        }
        // Lit limb on the sunward edge.
        for (int i = 0; i < 60; i++) {
            double a = Math.toRadians(118 + i * 2.0);
            double ca = Math.cos(a), sa = Math.sin(a);
            int x = cx - 2 + (int) Math.round(ca * (body - 1));
            int y = cy - 1 + (int) Math.round(sa * (body - 1));
            graphics.fill(x, y, x + 1, y + 1, JilatonUi.alpha(0xFFF6E9CF, 0.34 * vis));
        }

        ringHalf(graphics, cx, cy, body, radii, colors, alpha, true, vis);

        circle(graphics, cx - (int) (46 * scale), cy - (int) (26 * scale), (int) (12 * scale), JilatonUi.alpha(0xFF0A0B10, 0.55 * vis));
        circle(graphics, cx - (int) (46 * scale), cy - (int) (27 * scale), (int) (4 * scale), JilatonUi.alpha(0xFFD9DEE9, 0.9 * vis));
    }

    /** One half of a banded planetary ring; {@code front} selects the near (lower) half of the ellipse. */
    private static void ringHalf(GuiGraphics graphics, int cx, int cy, int body, double[] radii, int[] colors,
                                 double[] alpha, boolean front, double vis) {
        double flatten = 0.33;
        for (int band = 0; band < colors.length; band++) {
            double r0 = radii[band * 2] * body, r1 = radii[band * 2 + 1] * body;
            int ry = (int) Math.round(r1 * flatten);
            int ry0 = (int) Math.round(r0 * flatten);
            int from = front ? 0 : -ry;
            int to = front ? ry : 0;
            for (int dy = from; dy < to; dy++) {
                double outerK = Math.sqrt(Math.max(0, 1 - (dy * dy) / (double) (ry * ry)));
                int outer = (int) Math.round(r1 * outerK);
                int inner = 0;
                if (Math.abs(dy) < ry0) {
                    double innerK = Math.sqrt(Math.max(0, 1 - (dy * dy) / (double) (ry0 * ry0)));
                    inner = (int) Math.round(r0 * innerK);
                }
                if (outer <= inner) continue;
                double shade = 0.80 + 0.20 * Math.cos(dy * 0.16);
                int color = JilatonUi.alpha(colors[band], alpha[band] * vis * shade);
                int y = cy + dy;
                graphics.fill(cx + inner, y, cx + outer, y + 1, color);
                graphics.fill(cx - outer, y, cx - inner, y + 1, color);
            }
        }
    }

    private void blackHole(GuiGraphics graphics, int cx, int cy, int accent, int accent2, double vis) {
        for (int i = 5; i >= 1; i--)
            circle(graphics, cx, cy, 20 + i * 8, JilatonUi.alpha(accent, 0.020 * i * vis));
        circle(graphics, cx, cy, 17, JilatonUi.alpha(0xFF000000, vis));
        ellipse(graphics, cx, cy, 30, 10, 2, JilatonUi.alpha(EffectBatch.mixRgb(accent, 0xFFFFFF, 0.35), 0.75 * vis));
        ellipse(graphics, cx, cy, 36, 13, 1, JilatonUi.alpha(accent2, 0.28 * vis));
    }

    private void moon(GuiGraphics graphics, int cx, int cy, double vis) {
        circle(graphics, cx, cy, 42, JilatonUi.alpha(0xE8EEFF, 0.035 * vis));
        circle(graphics, cx, cy, 15, JilatonUi.alpha(0xFFE9EDF6, vis));
        circle(graphics, cx - 5, cy - 3, 4, JilatonUi.alpha(0xFFC9CFDC, 0.9 * vis));
        circle(graphics, cx + 4, cy + 5, 3, JilatonUi.alpha(0xFFC9CFDC, 0.75 * vis));
        circle(graphics, cx + 2, cy - 7, 2, JilatonUi.alpha(0xFFC9CFDC, 0.6 * vis));
    }

    private void lightStreaks(GuiGraphics graphics, int x0, int y0, int w, int h, int accent, int white, double t, double vis) {
        for (int i = 0; i < 7; i++) {
            double phase = (t * 0.06 + i * 0.137) % 1.0;
            int x = x0 + (int) (w * (0.15 + 0.7 * hash(i, 601)));
            int y = y0 + (int) (h * (0.15 + 0.4 * hash(i, 607))) + (int) (phase * h * 0.35);
            int color = JilatonUi.alpha(i % 2 == 0 ? accent : white, 0.30 * Math.sin(phase * Math.PI) * vis);
            line(graphics, x, y, x + 18, y + 46, color, 1);
        }
    }

    // ------------------------------------------------------------------ preview figure & cosmetics

    private void player(GuiGraphics graphics, int cx, int baseY, int horizon, double t, double vis) {
        int bodyW = 20, bodyH = 26, head = 16, legH = 22;
        int bodyTop = baseY - legH - bodyH;
        int headTop = bodyTop - head + 1;
        int shoulderY = bodyTop + 2;
        int waistY = bodyTop + 15;
        int color = cosmeticColor();
        double alpha = opacityFraction();

        // The figure is authored small and scaled up so the whole cosmetic stack stays legible.
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, baseY);
        graphics.pose().scale(1.7F, 1.7F);
        graphics.pose().translate(-cx, -baseY);

        // Ground glow and contact shadow.
        ellipseFilled(graphics, cx, baseY + 2, 74, 16, JilatonUi.alpha(color, 0.055 * vis));
        ellipse(graphics, cx, baseY + 2, 46, 10, 1, JilatonUi.alpha(color, 0.18 * vis));
        auraFloor(graphics, cx, baseY, t, color, alpha, vis);
        wings(graphics, cx, shoulderY, (int) (46 * wingScale()), t, color, alpha, vis);
        companion(graphics, cx + 48, shoulderY - 4, t, color, alpha, vis);

        int silhouette = JilatonUi.alpha(0xFF090A0C, 0.97 * vis);
        JilatonUi.fillRounded(graphics, cx - 13, bodyTop + 2, 5, 20, 2, silhouette);
        JilatonUi.fillRounded(graphics, cx + 8, bodyTop + 2, 5, 20, 2, silhouette);
        JilatonUi.fillRounded(graphics, cx - 10, bodyTop, bodyW, bodyH, 3, silhouette);
        JilatonUi.fillRounded(graphics, cx - 8, headTop, head, head, 4, silhouette);
        JilatonUi.fillRounded(graphics, cx - 8, bodyTop + bodyH - 1, 7, legH + 1, 2, silhouette);
        JilatonUi.fillRounded(graphics, cx + 1, bodyTop + bodyH - 1, 7, legH + 1, 2, silhouette);
        // Rim light: thin accent edge on the lit side.
        int rim = JilatonUi.alpha(EffectBatch.mixRgb(color, 0xFFFFFF, 0.25), 0.55 * vis);
        graphics.fill(cx + 7, headTop + 2, cx + 8, headTop + head - 2, rim);
        graphics.fill(cx + 9, bodyTop + 2, cx + 10, bodyTop + bodyH - 2, JilatonUi.alpha(color, 0.28 * vis));
        graphics.fill(cx - 8, headTop + 2, cx + 8, headTop + 4, JilatonUi.alpha(0xFF2A2C33, 0.8 * vis));
        // Eyes: two faint emissive dots.
        graphics.fill(cx - 4, headTop + 8, cx - 2, headTop + 10, JilatonUi.alpha(color, 0.55 * vis));
        graphics.fill(cx + 2, headTop + 8, cx + 4, headTop + 10, JilatonUi.alpha(color, 0.55 * vis));

        halo(graphics, cx, headTop - 9, t, color, alpha, vis);
        relics(graphics, cx, waistY, t, color, alpha, vis);
        auraBody(graphics, cx, bodyTop, headTop, baseY, t, color, alpha, vis);
        graphics.pose().popMatrix();
    }

    private void halo(GuiGraphics graphics, int cx, int cy, double t, int color, double alpha, double vis) {
        String style = halo == null ? "Off" : halo.value();
        if (style.equals("Off")) return;
        double a = 0.85 * alpha * vis;
        int soft = JilatonUi.alpha(color, 0.16 * a);
        int core = JilatonUi.alpha(EffectBatch.mixRgb(color, 0xFFFFFF, 0.35), a);
        switch (style) {
            case "Ring" -> {
                ellipse(graphics, cx, cy + 3, 22, 6, 3, soft);
                ellipse(graphics, cx, cy, 22, 6, 2, core);
            }
            case "Double" -> {
                ellipse(graphics, cx, cy + 3, 22, 6, 3, soft);
                ellipse(graphics, cx, cy, 22, 6, 2, core);
                ellipse(graphics, cx, cy - 8, 15, 4, 2, JilatonUi.alpha(color, 0.55 * a));
            }
            case "Aureole" -> {
                circle(graphics, cx, cy, 34, JilatonUi.alpha(color, 0.09 * a));
                ellipse(graphics, cx, cy + 3, 24, 7, 4, soft);
                ellipse(graphics, cx, cy, 22, 6, 2, core);
                ellipse(graphics, cx, cy - 2, 16, 4, 1, JilatonUi.alpha(color, 0.45 * a));
                for (int i = 0; i < 4; i++) {
                    double ang = t * 1.4 + i * Math.PI / 2;
                    int dx = cx + (int) (Math.cos(ang) * 26), dy = cy + (int) (Math.sin(ang) * 7) - 2;
                    circle(graphics, dx, dy, 5, JilatonUi.alpha(color, 0.28 * a));
                    graphics.fill(dx, dy, dx + 1, dy + 1, core);
                }
            }
            case "Sigil" -> {
                ellipse(graphics, cx, cy, 25, 7, 2, core);
                ellipse(graphics, cx, cy, 18, 5, 1, JilatonUi.alpha(color, 0.45 * a));
                for (int i = 0; i < 12; i++) {
                    double ang = t * 0.5 + i * Math.PI * 2 / 12;
                    int x0 = cx + (int) (Math.cos(ang) * 23), y0 = cy + (int) (Math.sin(ang) * 6.4);
                    int x1 = cx + (int) (Math.cos(ang) * 30), y1 = cy + (int) (Math.sin(ang) * 8.4);
                    line(graphics, x0, y0, x1, y1, JilatonUi.alpha(color, (i % 3 == 0 ? 0.8 : 0.35) * a), 1);
                }
            }
            default -> {
                ellipse(graphics, cx, cy + 2, 23, 6, 2, JilatonUi.alpha(color, 0.55 * a));
                for (int i = 0; i < 7; i++) {
                    double ang = t * 0.5 + i * Math.PI * 2 / 7;
                    int x = cx + (int) (Math.cos(ang) * 23);
                    int y = cy + (int) (Math.sin(ang) * 6.4) - 3 - (int) (Math.abs(Math.sin(t * 1.6 + i)) * 2);
                    diamond(graphics, x, y, 4, core, JilatonUi.alpha(color, 0.22 * a));
                }
            }
        }
    }

    private void relics(GuiGraphics graphics, int cx, int cy, double t, int color, double alpha, double vis) {
        String style = relics == null ? "Off" : relics.value();
        if (style.equals("Off")) return;
        double a = 0.9 * alpha * vis;
        int count = switch (style) {
            case "Stars", "Orbs" -> 5;
            case "Runes" -> 4;
            default -> 3;
        };
        for (int i = 0; i < count; i++) {
            double phase = i * Math.PI * 2 / count;
            double ang = t * 1.1 + phase;
            int x = cx + (int) (Math.cos(ang) * 38);
            int y = cy + (int) (Math.sin(ang) * 12) - 4;
            for (int k = 1; k <= 3; k++) {
                double ta = ang - k * 0.10;
                int tx = cx + (int) (Math.cos(ta) * 38), ty = cy + (int) (Math.sin(ta) * 12) - 4;
                graphics.fill(tx, ty, tx + 1, ty + 1, JilatonUi.alpha(color, 0.16 * (4 - k) / 3 * a));
            }
            switch (style) {
                case "Stars" -> {
                    line(graphics, x - 4, y, x + 5, y, JilatonUi.alpha(color, a), 1);
                    line(graphics, x, y - 4, x, y + 5, JilatonUi.alpha(color, a), 1);
                    line(graphics, x - 2, y - 2, x + 3, y + 3, JilatonUi.alpha(color, 0.6 * a), 1);
                    line(graphics, x + 3, y - 2, x - 2, y + 3, JilatonUi.alpha(color, 0.6 * a), 1);
                }
                case "Orbs" -> {
                    circle(graphics, x, y, 8, JilatonUi.alpha(color, 0.18 * a));
                    circle(graphics, x, y, 3, JilatonUi.alpha(EffectBatch.mixRgb(color, 0xFFFFFF, 0.4), a));
                }
                case "Blades" -> {
                    line(graphics, x, y - 9, x, y + 7, JilatonUi.alpha(color, a), 2);
                    line(graphics, x - 2, y - 5, x + 2, y - 5, JilatonUi.alpha(color, 0.5 * a), 1);
                }
                case "Runes" -> {
                    ellipse(graphics, x, y, 7, 4, 1, JilatonUi.alpha(color, 0.8 * a));
                    graphics.fill(x, y, x + 1, y + 1, JilatonUi.alpha(0xFFFFFF, 0.8 * a));
                }
                default -> diamond(graphics, x, y, 5, JilatonUi.alpha(color, a), JilatonUi.alpha(color, 0.2 * a));
            }
        }
    }

    private void auraFloor(GuiGraphics graphics, int cx, int baseY, double t, int color, double alpha, double vis) {
        String style = aura == null ? "Off" : aura.value();
        if (style.equals("Off")) return;
        double a = 0.8 * alpha * vis;
        ellipse(graphics, cx, baseY + 2, 46, 10, 1, JilatonUi.alpha(color, 0.30 * a));
        if (style.equals("Rising") || style.equals("Radiance")) {
            for (int i = 0; i < 12; i++) {
                double p = (t * 0.35 + hash(i, 701)) % 1.0;
                int x = cx + (int) ((hash(i, 709) - 0.5) * 70);
                int y = baseY - (int) (p * 46);
                graphics.fill(x, y, x + 1, y + 1, JilatonUi.alpha(color, (1 - p) * 0.55 * a));
            }
        }
    }

    private void auraBody(GuiGraphics graphics, int cx, int bodyTop, int headTop, int baseY, double t, int color, double alpha, double vis) {
        String style = aura == null ? "Off" : aura.value();
        if (style.equals("Off")) return;
        double a = 0.55 * alpha * vis;
        if (style.equals("Shroud")) {
            for (int i = 0; i < 3; i++) {
                int y = bodyTop + 4 + i * 12;
                ellipse(graphics, cx, y, 20 - i * 2, 6, 1, JilatonUi.alpha(color, 0.22 * a));
            }
        } else if (style.equals("Radiance")) {
            for (int i = 0; i < 10; i++) {
                double ang = t * 0.25 + i * Math.PI * 2 / 10;
                int x0 = cx + (int) (Math.cos(ang) * 24);
                int x1 = cx + (int) (Math.cos(ang) * 38);
                int y0 = baseY;
                int y1 = headTop - 4;
                line(graphics, x0, y0, x1, y1, JilatonUi.alpha(color, 0.055 * a), 1);
            }
            circle(graphics, cx, (bodyTop + baseY) / 2, 34, JilatonUi.alpha(color, 0.020 * a));
        }
    }

    private void wings(GuiGraphics graphics, int cx, int shoulderY, int reach, double t, int color, double alpha, double vis) {
        String style = wings == null ? "Off" : wings.value();
        if (style.equals("Off")) return;
        double flap = JilatonWidgets.reducedMotion() ? 0 : Math.sin(t * 1.7) * 0.15;
        double a = 0.9 * alpha * vis;
        int n = 20;
        for (int side = -1; side <= 1; side += 2) {
            int bx = cx + side * 4;
            int tipX = bx + side * reach;
            int tipY = shoulderY - (int) (reach * 0.55) + (int) (flap * 14);
            int leadX = bx + side * (int) (reach * 0.52), leadY = shoulderY - (int) (reach * 0.95);
            int botX = bx + side * (int) (reach * 0.72), botY = shoulderY + (int) (reach * 0.40);
            int trailX = bx + side * (int) (reach * 0.40), trailY = shoulderY + (int) (reach * 0.14);
            int[] topX = new int[n + 1], topY = new int[n + 1], lowX = new int[n + 1], lowY = new int[n + 1];
            for (int i = 0; i <= n; i++) {
                double u = i / (double) n;
                int[] lead = quad(bx, shoulderY, leadX, leadY, tipX, tipY, u);
                int[] trail = quad(bx, shoulderY, trailX, trailY, botX, botY, u);
                topX[i] = lead[0]; topY[i] = lead[1];
                lowX[i] = trail[0]; lowY[i] = trail[1];
            }
            if (!style.equals("Particle")) {
                int membrane = style.equals("Dark Angel") ? EffectBatch.mixRgb(0x12151E, color, 0.42) : color;
                double membraneAlpha = style.equals("Dark Angel") ? 0.52 : style.equals("Geometric") ? 0.30 : style.equals("Glow") ? 0.12 : 0.06;
                for (int i = 0; i < n; i++) {
                    double fade = 1 - i / (double) n;
                    int x = (topX[i] + lowX[i]) / 2;
                    int y0 = Math.min(topY[i], lowY[i]);
                    int y1 = Math.max(topY[i], lowY[i]);
                    graphics.fill(x, y0, x + 2, y1, JilatonUi.alpha(membrane, membraneAlpha * fade * a));
                }
            }
            for (int i = 0; i < n; i++) {
                double fade = 1 - i / (double) n;
                if (style.equals("Particle")) {
                    if (i % 2 == 0) {
                        graphics.fill(topX[i], topY[i], topX[i] + 2, topY[i] + 2, JilatonUi.alpha(color, 0.55 * fade * a));
                        graphics.fill(lowX[i], lowY[i], lowX[i] + 1, lowY[i] + 1, JilatonUi.alpha(color, 0.28 * fade * a));
                    }
                } else if (i % 3 == 0) {
                    line(graphics, topX[i], topY[i], lowX[i], lowY[i], JilatonUi.alpha(color, 0.30 * fade * a), 1);
                }
            }
            for (int i = 0; i < n; i++) {
                double fade = 1 - i / (double) n;
                line(graphics, topX[i], topY[i], topX[i + 1], topY[i + 1], JilatonUi.alpha(color, 0.85 * fade * a), 1);
                if (style.equals("Glow")) {
                    line(graphics, topX[i], topY[i] - 1, topX[i + 1], topY[i + 1] - 1, JilatonUi.alpha(color, 0.20 * fade * a), 1);
                    line(graphics, topX[i], topY[i] + 1, topX[i + 1], topY[i + 1] + 1, JilatonUi.alpha(color, 0.20 * fade * a), 1);
                } else if (style.equals("Minimal") || style.equals("Geometric")) {
                    line(graphics, lowX[i], lowY[i], lowX[i + 1], lowY[i + 1], JilatonUi.alpha(color, 0.35 * fade * a), 1);
                }
            }
            if (style.equals("Dark Angel")) {
                for (int i = 6; i < n; i += 6)
                    diamond(graphics, lowX[i], lowY[i], 4, JilatonUi.alpha(color, 0.75 * a), JilatonUi.alpha(color, 0.18 * a));
            }
            circle(graphics, tipX, tipY, 7, JilatonUi.alpha(color, 0.18 * a));
            graphics.fill(tipX - 1, tipY - 1, tipX + 2, tipY + 2, JilatonUi.alpha(color, 0.85 * a));
        }
    }

    private void companion(GuiGraphics graphics, int cx, int cy, double t, int color, double alpha, double vis) {
        String style = companion == null ? "Off" : companion.value();
        if (style.equals("Off")) return;
        double a = 0.9 * alpha * vis;
        int bob = (int) (Math.sin(t * 1.3) * 3);
        if (style.equals("Ember")) {
            int warm = EffectBatch.mixRgb(color, 0xFFB060, 0.55);
            circle(graphics, cx, cy + bob, 12, JilatonUi.alpha(warm, 0.20 * a));
            circle(graphics, cx, cy + bob, 4, JilatonUi.alpha(warm, 0.95 * a));
            for (int i = 0; i < 4; i++) {
                int y = cy + bob - 4 - (int) ((t * 14 + i * 4) % 16);
                graphics.fill(cx - 1 + i % 2, y, cx + 1 + i % 2, y + 1, JilatonUi.alpha(warm, 0.4 * a));
            }
        } else if (style.equals("Sentinel")) {
            diamond(graphics, cx, cy + bob, 7, JilatonUi.alpha(color, 0.9 * a), JilatonUi.alpha(color, 0.2 * a));
            ellipse(graphics, cx, cy + bob, 12, 4, 1, JilatonUi.alpha(color, 0.5 * a));
        } else {
            circle(graphics, cx, cy + bob, 14, JilatonUi.alpha(color, 0.12 * a));
            circle(graphics, cx, cy + bob, 4, JilatonUi.alpha(EffectBatch.mixRgb(color, 0xFFFFFF, 0.35), a));
            for (int i = 1; i <= 4; i++) {
                int tx = cx - i * 3 + (int) (Math.sin(t * 2 - i) * 2);
                int ty = cy + bob + i * 2;
                graphics.fill(tx, ty, tx + 1, ty + 1, JilatonUi.alpha(color, 0.28 * a / i));
            }
        }
    }

    // ------------------------------------------------------------------ settings & interaction

    private SkyThemes.Theme currentTheme() {
        int accent = accent();
        if (skyColorMode != null && skyColor != null) {
            accent = EffectPalette.resolve(skyColorMode.value(), accent(), skyColor.value());
        }
        String name = skyTheme == null ? "Midnight" : skyTheme.value();
        return SkyThemes.resolve(name, 0x05030B, 0x1B1226, accent);
    }

    private String themeFeatures() {
        List<SkyThemes.Feature> features = currentTheme().features();
        StringBuilder out = new StringBuilder();
        for (SkyThemes.Feature feature : features) {
            if (out.length() > 0) out.append(" · ");
            out.append(feature.name().replace('_', ' '));
        }
        return out.isEmpty() ? "No atmospheric layers" : out.toString();
    }

    private int cosmeticColor() {
        int accent = accent();
        if (fxColorMode != null && fxColor != null) return EffectPalette.resolve(fxColorMode.value(), accent, fxColor.value());
        return accent;
    }

    private double opacityFraction() {
        if (opacity == null) return 0.85;
        return Math.max(0.1, (opacity.value() - opacity.minimum()) / (opacity.maximum() - opacity.minimum()));
    }

    private double wingScale() {
        if (wingSize == null) return 1;
        return 0.6 + 0.4 * (wingSize.value() - wingSize.minimum()) / (wingSize.maximum() - wingSize.minimum()) * 2;
    }

    private int accent() {
        return ClientTheme.resolveAccent(JilatonClient.clientConfig().themeId(), JilatonClient.clientConfig().customAccent());
    }

    private String cellValue(int index) {
        return switch (index) {
            case 0 -> halo == null ? "—" : halo.value();
            case 1 -> wings == null ? "—" : wings.value();
            case 2 -> relics == null ? "—" : relics.value();
            case 3 -> aura == null ? "—" : aura.value();
            case 4 -> companion == null ? "—" : companion.value();
            default -> fxColorMode == null ? "—" : fxColorMode.value();
        };
    }

    private ChoiceSetting cellChoice(int index) {
        return switch (index) {
            case 0 -> halo;
            case 1 -> wings;
            case 2 -> relics;
            case 3 -> aura;
            case 4 -> companion;
            default -> fxColorMode;
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        if (skyTheme != null) {
            if (hovered(mx, my, themeL, themeRowY, 22, 26)) { skyTheme.cycle(-1); save(); return true; }
            if (hovered(mx, my, themeR, themeRowY, 22, 26)) { skyTheme.cycle(1); save(); return true; }
        }
        for (int i = 0; i < cellX.length; i++) {
            if (!hovered(mx, my, cellX[i], cellY[i], cellW, cellH)) continue;
            ChoiceSetting setting = cellChoice(i);
            if (setting == null) return true;
            setting.cycle(mx < cellX[i] + cellW / 2.0 ? -1 : 1);
            save();
            return true;
        }
        for (int i = 0; i < 3; i++) {
            int y = slidersY + i * sliderGap;
            if (hovered(mx, my, rightX, y + 10, sliderW, 14)) {
                dragging = i;
                updateSlider(i, mx);
                return true;
            }
        }
        if (hovered(mx, my, rightX, togglesY, rightW, toggleH)) {
            toggle(skyModule);
            return true;
        }
        if (hovered(mx, my, rightX, togglesY + toggleH + toggleGap, rightW, toggleH)) {
            toggle(fxModule);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging >= 0 && event.button() == 0) {
            updateSlider(dragging, event.x());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging >= 0) {
            dragging = -1;
            save();
            return true;
        }
        return super.mouseReleased(event);
    }

    private void updateSlider(int index, double mouseX) {
        NumberSetting setting = switch (index) {
            case 0 -> opacity;
            case 1 -> haloTilt;
            default -> wingSize;
        };
        if (setting == null) return;
        double fraction = Math.max(0, Math.min(1, (mouseX - rightX) / sliderW));
        setting.setValue(setting.minimum() + fraction * (setting.maximum() - setting.minimum()));
    }

    private void toggle(ClientModule module) {
        if (module == null) return;
        module.setEnabled(!module.enabled());
        save();
    }

    private void save() {
        JilatonClient.clientConfig().save(JilatonClient.moduleManager());
    }

    @Override
    public void onClose() {
        save();
        minecraft.setScreen(parent);
    }

    private ChoiceSetting choice(String moduleId, String settingId) {
        Setting setting = find(moduleId, settingId);
        return setting instanceof ChoiceSetting value ? value : null;
    }

    private NumberSetting number(String moduleId, String settingId) {
        Setting setting = find(moduleId, settingId);
        return setting instanceof NumberSetting value ? value : null;
    }

    private TextSetting text(String moduleId, String settingId) {
        Setting setting = find(moduleId, settingId);
        return setting instanceof TextSetting value ? value : null;
    }

    private static Setting find(String moduleId, String settingId) {
        ClientModule module = JilatonClient.moduleManager().get(moduleId);
        if (module == null) return null;
        for (Setting setting : module.settings()) if (setting.id().equals(settingId)) return setting;
        return null;
    }

    // ------------------------------------------------------------------ drawing helpers

    private static boolean hovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static double hash(int index, int salt) {
        int h = index * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0x7FFFFFFF) / (double) 0x7FFFFFFF;
    }

    private static int[] quad(int x0, int y0, int x1, int y1, int x2, int y2, double t) {
        double u = 1 - t;
        return new int[]{(int) (u * u * x0 + 2 * u * t * x1 + t * t * x2), (int) (u * u * y0 + 2 * u * t * y1 + t * t * y2)};
    }

    private static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color, int thickness) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            graphics.fill(x0, y0, x0 + thickness, y0 + thickness, color);
            if (x0 == x1 && y0 == y1) break;
            int doubled = error * 2;
            if (doubled >= dy) { error += dy; x0 += sx; }
            if (doubled <= dx) { error += dx; y0 += sy; }
        }
    }

    private static void circle(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        if (radius <= 0 || color >>> 24 == 0) return;
        if (radius <= 3) {
            graphics.fill(cx - radius, cy - radius, cx + radius, cy + radius, color);
            return;
        }
        for (int dy = -radius; dy < radius; dy++) {
            double k = Math.sqrt(Math.max(0, 1 - (dy * dy) / (double) (radius * radius)));
            int half = (int) Math.round(radius * k);
            if (half <= 0) continue;
            graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    private static void ellipse(GuiGraphics graphics, int cx, int cy, int rx, int ry, int thickness, int color) {
        int steps = Math.max(18, (int) (rx * 2.4));
        for (int i = 0; i < steps; i++) {
            double a = i * Math.PI * 2 / steps;
            int x = cx + (int) Math.round(Math.cos(a) * rx);
            int y = cy + (int) Math.round(Math.sin(a) * ry);
            graphics.fill(x, y, x + thickness, y + thickness, color);
        }
    }

    private static void ellipseFilled(GuiGraphics graphics, int cx, int cy, int rx, int ry, int color) {
        for (int y = -ry; y < ry; y++) {
            double k = Math.sqrt(Math.max(0, 1 - (y * y) / (double) (ry * ry)));
            int half = (int) Math.round(rx * k);
            graphics.fill(cx - half, cy + y, cx + half, cy + y + 1, color);
        }
    }

    private static void diamond(GuiGraphics graphics, int cx, int cy, int size, int color, int glow) {
        if (glow >>> 24 != 0) circle(graphics, cx, cy, size * 2, glow);
        graphics.fill(cx, cy - size, cx + 1, cy + size + 1, color);
        graphics.fill(cx - size / 2, cy - size / 2, cx + size / 2 + 1, cy + size / 2 + 1, color);
        graphics.fill(cx - size + 1, cy - 1, cx + size, cy + 2, color);
    }

    private String fit(String text, int width) {
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
    }
}
