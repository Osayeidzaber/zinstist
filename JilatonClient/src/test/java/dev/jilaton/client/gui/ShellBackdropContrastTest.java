package dev.jilaton.client.gui;

import dev.jilaton.client.gui.design.Ui;
import dev.jilaton.client.gui.shell.ShellPalette;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The zaber.tech background rules as invariants: every surface and text token is a pure neutral, no
 * accent is purple, the scrim keeps some world visible, and type keeps its contrast floor even over the
 * brightest scene the game can put behind the panel.
 */
class ShellBackdropContrastTest {
    private static final List<String> SEMANTIC = List.of("DANGER", "ONLINE", "WARN");

    private static final Map<String, Integer> TOKENS = Map.ofEntries(
            Map.entry("ShellPalette.SCRIM", ShellPalette.SCRIM),
            Map.entry("ShellPalette.PANEL", ShellPalette.PANEL),
            Map.entry("ShellPalette.SIDEBAR", ShellPalette.SIDEBAR),
            Map.entry("ShellPalette.CARD", ShellPalette.CARD),
            Map.entry("ShellPalette.CARD_HOVER", ShellPalette.CARD_HOVER),
            Map.entry("ShellPalette.EXPANSION", ShellPalette.EXPANSION),
            Map.entry("ShellPalette.STROKE", ShellPalette.STROKE),
            Map.entry("ShellPalette.STROKE_HOVER", ShellPalette.STROKE_HOVER),
            Map.entry("ShellPalette.CONTROL", ShellPalette.CONTROL),
            Map.entry("ShellPalette.CONTROL_HOVER", ShellPalette.CONTROL_HOVER),
            Map.entry("ShellPalette.TEXT", ShellPalette.TEXT),
            Map.entry("ShellPalette.TEXT_2", ShellPalette.TEXT_2),
            Map.entry("ShellPalette.TEXT_3", ShellPalette.TEXT_3),
            Map.entry("ShellPalette.ACCENT", ShellPalette.ACCENT),
            Map.entry("ShellPalette.ACCENT_DEEP", ShellPalette.ACCENT_DEEP),
            Map.entry("JilatonUi.INK", JilatonUi.INK),
            Map.entry("JilatonUi.SURFACE", JilatonUi.SURFACE),
            Map.entry("JilatonUi.SURFACE_2", JilatonUi.SURFACE_2),
            Map.entry("JilatonUi.RAISED", JilatonUi.RAISED),
            Map.entry("JilatonUi.RAISED_HOVER", JilatonUi.RAISED_HOVER),
            Map.entry("JilatonUi.HAIRLINE", JilatonUi.HAIRLINE),
            Map.entry("JilatonUi.HAIRLINE_STRONG", JilatonUi.HAIRLINE_STRONG),
            Map.entry("JilatonUi.TEXT", JilatonUi.TEXT),
            Map.entry("JilatonUi.TEXT_2", JilatonUi.TEXT_2),
            Map.entry("JilatonUi.TEXT_3", JilatonUi.TEXT_3),
            Map.entry("JilatonUi.TEXT_4", JilatonUi.TEXT_4),
            Map.entry("Ui.BACKDROP", Ui.BACKDROP),
            Map.entry("Ui.DEEP", Ui.DEEP),
            Map.entry("Ui.PANEL", Ui.PANEL),
            Map.entry("Ui.SIDEBAR", Ui.SIDEBAR),
            Map.entry("Ui.CARD", Ui.CARD),
            Map.entry("Ui.RAISED", Ui.RAISED),
            Map.entry("Ui.DRAWER", Ui.DRAWER),
            Map.entry("Ui.STROKE", Ui.STROKE),
            Map.entry("Ui.TEXT", Ui.TEXT),
            Map.entry("Ui.TEXT_2", Ui.TEXT_2),
            Map.entry("Ui.TEXT_3", Ui.TEXT_3),
            Map.entry("Ui.SOFT", Ui.SOFT),
            Map.entry("Ui.FAINT", Ui.FAINT),
            Map.entry("Ui.DANGER", Ui.DANGER),
            Map.entry("Ui.ONLINE", Ui.ONLINE),
            Map.entry("Ui.WARN", Ui.WARN),
            Map.entry("ShellPalette.DANGER", ShellPalette.DANGER),
            Map.entry("ShellPalette.ONLINE", ShellPalette.ONLINE));

    @Test void surfacesAndTextTokensArePureNeutrals() {
        TOKENS.forEach((name, color) -> {
            if (isSemantic(name)) return;
            int red = color >> 16 & 0xFF, green = color >> 8 & 0xFF, blue = color & 0xFF;
            assertTrue(red == green && green == blue, name + " must be a pure neutral, got #" + hex(color));
        });
    }

    @Test void noAccentThemeOrTokenSitsInThePurpleBand() {
        TOKENS.forEach((name, color) -> assertTrue(!isPurple(color), name + " must not be purple: #" + hex(color)));
        for (ClientTheme theme : ClientTheme.values()) {
            assertTrue(!isPurple(theme.accent()), theme + " accent must not be purple");
        }
    }

    @Test void typeKeepsItsContrastFloorOnEveryShellSurface() {
        for (int surface : new int[]{ShellPalette.PANEL, ShellPalette.SIDEBAR, ShellPalette.CARD, Ui.PANEL, Ui.BACKDROP}) {
            assertTrue(contrast(ShellPalette.TEXT, surface) >= 7.0, "TEXT on " + hex(surface) + " below 7:1");
            assertTrue(contrast(ShellPalette.TEXT_2, surface) >= 4.5, "TEXT_2 on " + hex(surface) + " below 4.5:1");
            assertTrue(contrast(ShellPalette.TEXT_3, surface) >= 4.5, "TEXT_3 on " + hex(surface) + " below 4.5:1");
            assertTrue(contrast(ShellPalette.TEXT_3, ShellPalette.CARD) >= 4.5, "TEXT_3 on CARD below 4.5:1");
            assertTrue(contrast(Ui.FAINT, surface) >= 3.0, "faint labels on " + hex(surface) + " below 3:1");
        }
    }

    @Test void scrimKeepsTheWorldFaintlyVisibleButNeverBright() {
        double alpha = (ShellPalette.SCRIM >>> 24) / 255.0;
        assertTrue(alpha >= 0.60 && alpha <= 0.90, "the scrim must stay subtle translucency, was " + alpha);
        // Worst case: a pure white, unblurred scene behind the scrim.
        int scene = 0xFF000000 | whiteThroughScrim();
        assertTrue(JilatonUi.luminance(scene) <= 0.30, "a white scene through the scrim must stay dark, got " + hex(scene));
        assertTrue(contrast(JilatonUi.TEXT, scene) >= 3.0, "type must survive even on the scrim itself");
    }

    private static boolean isSemantic(String name) {
        return SEMANTIC.stream().anyMatch(name::endsWith);
    }

    /** Purple and violet sit around 250-315 degrees; low-saturation colors are not "purple" by intent. */
    private static boolean isPurple(int color) {
        double hue = hue(color), saturation = saturation(color), value = value(color);
        return saturation >= 0.15 && value >= 0.15 && hue >= 250 && hue <= 315;
    }

    private static double hue(int color) {
        double red = (color >> 16 & 0xFF) / 255.0, green = (color >> 8 & 0xFF) / 255.0, blue = (color & 0xFF) / 255.0;
        double max = Math.max(red, Math.max(green, blue)), min = Math.min(red, Math.min(green, blue));
        if (max == min) return 0;
        double delta = max - min;
        double hue;
        if (max == red) hue = ((green - blue) / delta) % 6;
        else if (max == green) hue = (blue - red) / delta + 2;
        else hue = (red - green) / delta + 4;
        hue *= 60;
        return hue < 0 ? hue + 360 : hue;
    }

    private static double saturation(int color) {
        double red = (color >> 16 & 0xFF) / 255.0, green = (color >> 8 & 0xFF) / 255.0, blue = (color & 0xFF) / 255.0;
        double max = Math.max(red, Math.max(green, blue)), min = Math.min(red, Math.min(green, blue));
        return max == 0 ? 0 : (max - min) / max;
    }

    private static double value(int color) {
        return Math.max((color >> 16 & 0xFF) / 255.0, Math.max((color >> 8 & 0xFF) / 255.0, (color & 0xFF) / 255.0));
    }

    /** The scrim color composited at its own alpha over white. */
    private static int whiteThroughScrim() {
        double alpha = (ShellPalette.SCRIM >>> 24) / 255.0;
        int scrim = ShellPalette.SCRIM & 0xFF;
        int channel = (int) Math.round(scrim * alpha + 255 * (1 - alpha));
        return 0xFF000000 | channel << 16 | channel << 8 | channel;
    }

    private static double contrast(int foreground, int background) {
        double a = JilatonUi.luminance(foreground), b = JilatonUi.luminance(background);
        double lighter = Math.max(a, b), darker = Math.min(a, b);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static String hex(int color) {
        return String.format("%06X", color & 0xFFFFFF);
    }
}
