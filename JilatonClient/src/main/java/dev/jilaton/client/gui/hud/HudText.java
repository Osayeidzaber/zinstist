package dev.jilaton.client.gui.hud;

import java.util.function.IntUnaryOperator;

/**
 * Word-boundary fitting for HUD labels. Cutting a real item name mid-word ("IRON INGO") reads as a
 * bug even when the layout is correct, so an overlong label loses whole words and gains an ellipsis.
 */
public final class HudText {
    public static final String ELLIPSIS = "\u2026";

    private HudText() { }

    public static int width(String text, IntUnaryOperator advance) {
        if (text == null) {
            return 0;
        }
        int width = 0;
        for (int index = 0; index < text.length(); index++) {
            width += Math.max(0, advance.applyAsInt(text.charAt(index)));
        }
        return width;
    }

    /** Returns {@code text} itself when it fits; otherwise the longest whole-word prefix plus an ellipsis. */
    public static String ellipsize(String text, IntUnaryOperator advance, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (width(text, advance) <= maxWidth) {
            return text;
        }
        int ellipsisWidth = Math.max(0, advance.applyAsInt(ELLIPSIS.charAt(0)));
        int budget = maxWidth - ellipsisWidth;
        if (budget <= 0) {
            return ELLIPSIS;
        }
        int used = 0;
        int fit = 0;
        int lastSpace = -1;
        for (int index = 0; index < text.length(); index++) {
            int step = Math.max(0, advance.applyAsInt(text.charAt(index)));
            if (used + step > budget) {
                break;
            }
            used += step;
            fit = index + 1;
            if (Character.isWhitespace(text.charAt(index))) {
                lastSpace = index;
            }
        }
        int end = lastSpace > 0 ? lastSpace : fit;
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return end <= 0 ? ELLIPSIS : text.substring(0, end) + ELLIPSIS;
    }
}
