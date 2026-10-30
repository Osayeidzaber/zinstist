package dev.jilaton.client.gui.shell;

import java.util.function.ToIntFunction;

/**
 * Two-line card copy. Lines break on spaces so a word is never cut mid-token; the second line is
 * ellipsised whenever the copy does not fit. A pixel cut is used only when no whole word fits beside
 * the ellipsis, which no registered description needs at the reference width.
 */
public final class TextWrap {
    public static final String ELLIPSIS = "...";

    private TextWrap() { }

    public static String[] twoLines(String text, int width, ToIntFunction<String> measure) {
        String[] lines = {"", ""};
        if (text == null || width <= 0) return lines;
        String source = text.strip();
        if (source.isEmpty()) return lines;
        if (measure.applyAsInt(source) <= width) {
            lines[0] = source;
            return lines;
        }
        int split = wordEnd(source, width, measure);
        if (split <= 0) split = pixelCut(source, width, measure);
        lines[0] = source.substring(0, split).stripTrailing();
        String rest = source.substring(split).stripLeading();
        if (rest.isEmpty()) return lines;
        if (measure.applyAsInt(rest) <= width) {
            lines[1] = rest;
            return lines;
        }
        int budget = width - measure.applyAsInt(ELLIPSIS);
        int cut = wordEnd(rest, budget, measure);
        if (cut > 0) {
            lines[1] = rest.substring(0, cut).stripTrailing() + ELLIPSIS;
            return lines;
        }
        lines[1] = rest.substring(0, pixelCut(rest, budget, measure)).stripTrailing() + ELLIPSIS;
        return lines;
    }

    /** Index just past the last word that fits {@code width}, or 0 when not even one does. */
    private static int wordEnd(String text, int width, ToIntFunction<String> measure) {
        int fit = pixelCut(text, width, measure);
        if (fit >= text.length()) return text.length();
        int space = text.lastIndexOf(' ', fit);
        return space > 0 ? space : 0;
    }

    /** Largest prefix whose measured width fits, measured the way the renderer measures a string. */
    private static int pixelCut(String text, int width, ToIntFunction<String> measure) {
        int fit = 0;
        for (int index = 1; index <= text.length(); index++) {
            if (measure.applyAsInt(text.substring(0, index)) > width) break;
            fit = index;
        }
        return fit;
    }
}
