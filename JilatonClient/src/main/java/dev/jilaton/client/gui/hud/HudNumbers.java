package dev.jilaton.client.gui.hud;

import java.util.function.IntUnaryOperator;

/**
 * Fixed-advance measurement for the numerals printed by the HUD. Minecraft's font is proportional, so a
 * value crossing a digit boundary (9.9 to 10.1, 99 to 100) shifts every glyph after it. Every digit is
 * measured at the widest digit's advance plus {@link #TRACKING}, while signs, decimal points, separators
 * and suffixes keep their natural advance. Callers right-align with {@link #rightAlignedX} so the field's
 * right edge never moves.
 */
public final class HudNumbers {
    /** Extra pixels of tracking applied to every digit so tabular columns do not touch. */
    public static final int TRACKING = 1;

    private final IntUnaryOperator advance;
    private final int digitAdvance;

    public HudNumbers(IntUnaryOperator advance) {
        if (advance == null) {
            throw new IllegalArgumentException("Digit advance source is required");
        }
        this.advance = advance;
        int widest = 0;
        for (char digit = '0'; digit <= '9'; digit++) {
            widest = Math.max(widest, Math.max(0, advance.applyAsInt(digit)));
        }
        digitAdvance = widest + TRACKING;
    }

    public int digitAdvance() {
        return digitAdvance;
    }

    /** Advance of one character: fixed for digits, natural for everything else. */
    public int advanceOf(char character) {
        return isDigit(character) ? digitAdvance : Math.max(0, advance.applyAsInt(character));
    }

    public int width(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int width = 0;
        for (int index = 0; index < text.length(); index++) {
            width += advanceOf(text.charAt(index));
        }
        return width;
    }

    /** Left edge that keeps {@code text} ending exactly at {@code rightEdge}. */
    public int rightAlignedX(String text, int rightEdge) {
        return rightEdge - width(text);
    }

    /** Width of a column reserved for up to {@code digits} digits. */
    public int columnWidth(int digits) {
        return Math.max(0, digits) * digitAdvance;
    }

    public static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }
}
