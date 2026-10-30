package dev.jilaton.client.gui.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntUnaryOperator;

/**
 * Draws HUD numbers one glyph at a time at a fixed digit advance so values never shift horizontally.
 * Glyph strings for the BMP Latin-1 range, digits and the two HUD symbols are cached up front; the
 * per-frame path only reads them and the caller-owned value strings, so a frame with unchanged values
 * allocates nothing.
 */
public final class HudNumberPainter {
    /** Space between a value and its right-anchored unit suffix. */
    public static final int SUFFIX_GAP = 2;

    private static final String[] DIGITS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final String INFINITE = "\u221E";
    private static final String HEART = "\u2665";
    private static final String[] GLYPHS = new String[256];

    private final Font font;
    private final HudNumbers numbers;
    private final IntUnaryOperator measured;

    public HudNumberPainter(Font font) {
        if (font == null) {
            throw new IllegalArgumentException("Font is required");
        }
        this.font = font;
        this.numbers = new HudNumbers(character -> font.width(glyph((char) character)));
        this.measured = character -> numbers.advanceOf((char) character);
    }

    public HudNumbers numbers() {
        return numbers;
    }

    public int width(String text) {
        return numbers.width(text);
    }

    /** Word-boundary fit using the same fixed digit advance the painter draws with. */
    public String ellipsize(String text, int maxWidth) {
        return HudText.ellipsize(text, measured, maxWidth);
    }

    /** Draws left-aligned and returns the x cursor just past the last glyph. */
    public int drawLeft(GuiGraphics graphics, String text, int x, int y, int color) {
        int cursor = x;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            graphics.drawString(font, glyph(character), cursor, y, color);
            cursor += numbers.advanceOf(character);
        }
        return cursor;
    }

    /** Draws right-aligned so the value's right edge sits on {@code rightEdge}. */
    public int drawRight(GuiGraphics graphics, String text, int rightEdge, int y, int color) {
        return drawLeft(graphics, text, numbers.rightAlignedX(text, rightEdge), y, color);
    }

    public void drawFadeRight(GuiGraphics graphics, NumericCrossfade fade, long now, int rightEdge, int y,
                              int color) {
        String previous = fade.previous();
        if (!previous.isEmpty()) {
            double opacity = fade.previousOpacity(now);
            if (opacity > 0.01) {
                drawRight(graphics, previous, rightEdge, y, HudDesign.withAlpha(color, opacity));
            }
        }
        double opacity = fade.currentOpacity(now);
        if (opacity > 0.01) {
            drawRight(graphics, fade.current(), rightEdge, y, HudDesign.withAlpha(color, opacity));
        }
    }

    /** Left-aligned crossfade, for values whose left edge is the fixed one. */
    public void drawFadeLeft(GuiGraphics graphics, NumericCrossfade fade, long now, int x, int y, int color) {
        String previous = fade.previous();
        if (!previous.isEmpty()) {
            double opacity = fade.previousOpacity(now);
            if (opacity > 0.01) {
                drawLeft(graphics, previous, x, y, HudDesign.withAlpha(color, opacity));
            }
        }
        double opacity = fade.currentOpacity(now);
        if (opacity > 0.01) {
            drawLeft(graphics, fade.current(), x, y, HudDesign.withAlpha(color, opacity));
        }
    }

    /** Value right-aligned before a dim suffix whose own right edge stays on {@code rightEdge}. */
    public void drawSuffixedFade(GuiGraphics graphics, NumericCrossfade fade, long now, String suffix,
                                 int rightEdge, int y, int valueColor, int suffixColor) {
        int suffixX = rightEdge - numbers.width(suffix);
        graphics.drawString(font, suffix, suffixX, y, suffixColor);
        drawFadeRight(graphics, fade, now, suffixX - SUFFIX_GAP, y, valueColor);
    }

    private static String glyph(char character) {
        if (character >= '0' && character <= '9') {
            return DIGITS[character - '0'];
        }
        if (character == '\u221E') {
            return INFINITE;
        }
        if (character == '\u2665') {
            return HEART;
        }
        if (character < GLYPHS.length) {
            String cached = GLYPHS[character];
            if (cached == null) {
                cached = String.valueOf(character);
                GLYPHS[character] = cached;
            }
            return cached;
        }
        return String.valueOf(character);
    }
}
