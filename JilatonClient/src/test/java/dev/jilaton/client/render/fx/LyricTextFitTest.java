package dev.jilaton.client.render.fx;

import dev.jilaton.client.gui.hud.HudText;
import org.junit.jupiter.api.Test;

import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A lyric line wider than its slot must be fitted on a word boundary, never overflow, and never turn into an
 * empty line. {@link LyricLayout#fitText} delegates to the HUD's existing fitter, so these tests pin that
 * reuse rather than a second implementation.
 */
class LyricTextFitTest {
    private static final IntUnaryOperator SIX_PX = character -> 6;

    @Test
    void fittingLineIsReturnedUnchanged() {
        String text = "short line";
        assertEquals(60, HudText.width(text, SIX_PX));
        assertSame(text, LyricLayout.fitText(text, SIX_PX, 600));
    }

    @Test
    void overlongLineEllipsisesOnAWordBoundary() {
        String text = "alpha beta gamma delta epsilon zeta eta theta";
        String fitted = LyricLayout.fitText(text, SIX_PX, 100);
        assertEquals("alpha beta\u2026", fitted);
        assertTrue(HudText.width(fitted, SIX_PX) <= 100);
        String kept = fitted.substring(0, fitted.length() - 1);
        assertEquals("alpha beta", kept);
        assertTrue(text.startsWith(kept));
        assertTrue(Character.isWhitespace(text.charAt(kept.length())));
    }

    @Test
    void unbrokenWordCannotOverflowOrBecomeAnEmptyLine() {
        String word = "x".repeat(500);
        String fitted = LyricLayout.fitText(word, SIX_PX, 100);
        assertFalse(fitted.isEmpty());
        assertTrue(HudText.width(fitted, SIX_PX) <= 100);
        assertTrue(fitted.endsWith(HudText.ELLIPSIS));
        String kept = fitted.substring(0, fitted.length() - 1);
        assertFalse(kept.isEmpty());
        assertTrue(word.startsWith(kept));
    }

    @Test
    void nothingToFitStaysEmptyAndZeroWidthNeverReturnsBlank() {
        assertEquals("", LyricLayout.fitText(null, SIX_PX, 100));
        assertEquals("", LyricLayout.fitText("", SIX_PX, 100));
        assertEquals(HudText.ELLIPSIS, LyricLayout.fitText("line", SIX_PX, 0));
        assertEquals(HudText.ELLIPSIS, LyricLayout.fitText("line", SIX_PX, -10));
    }
}
