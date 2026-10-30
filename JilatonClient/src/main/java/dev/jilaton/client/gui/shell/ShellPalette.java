package dev.jilaton.client.gui.shell;

import dev.jilaton.client.gui.JilatonUi;

/**
 * Shell design tokens, taken from {@code reference/ui/DIRECTION.md}. Near-black graphite surfaces,
 * one violet accent that is reserved for state, and hairlines instead of borders. Never pure black,
 * never a white hairline.
 */
public final class ShellPalette {
    /** #08070A at 78%, drawn over the blurred world behind the panel. */
    public static final int SCRIM = 0xC708070A;
    /** Ink/panel — the content column. */
    public static final int PANEL = 0xFF0E0C12;
    /** Ink/sidebar — one step deeper than the content column, not black. */
    public static final int SIDEBAR = 0xFF0A090F;
    /** Ink/card. */
    public static final int CARD = 0xFF14121A;
    /** Ink/card hover. */
    public static final int CARD_HOVER = 0xFF1A1722;
    /** Inline expansion well, a shade under the card colour. */
    public static final int EXPANSION = 0xFF100E16;
    /** Ink/stroke, 1 px hairlines. */
    public static final int STROKE = 0xFF262231;
    public static final int STROKE_HOVER = 0xFF3A3348;
    /** Controls inside cards: keycaps, choice pills, toggle tracks. */
    public static final int CONTROL = 0xFF1C1924;
    public static final int CONTROL_HOVER = 0xFF24202E;
    /** Text ramp. */
    public static final int TEXT = 0xFFF2F0F5;
    public static final int TEXT_2 = 0xFF8E8A9A;
    public static final int TEXT_3 = 0xFF5E5A69;
    /** Single accent plus its deep partner, used only for state. */
    public static final int ACCENT = 0xFFA855F7;
    public static final int ACCENT_DEEP = 0xFF7C3AED;
    public static final int DANGER = 0xFFE5484D;
    public static final int ONLINE = 0xFF30A46C;

    /* Legacy names kept so the preview renderer keeps compiling; they map onto the tokens above. */
    public static final int SURFACE = CARD;
    public static final int SURFACE_HOVER = CARD_HOVER;
    public static final int BORDER = STROKE;
    public static final int BORDER_HOVER = STROKE_HOVER;
    public static final int MAGENTA = ACCENT_DEEP;

    private ShellPalette() { }

    /** Faint accent tint for a selected surface, well under the 10% accent budget. */
    public static int accentSurface(int accent) {
        return JilatonUi.mix(CARD, accent, 0.12);
    }

    public static int accentBorder(int accent) {
        return JilatonUi.mix(STROKE, accent, 0.45);
    }

    public static int dimmed(int color) {
        return JilatonUi.mix(color, SIDEBAR, 0.55);
    }
}
