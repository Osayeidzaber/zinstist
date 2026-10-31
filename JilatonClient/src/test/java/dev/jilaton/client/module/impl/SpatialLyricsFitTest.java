package dev.jilaton.client.module.impl;

import dev.jilaton.client.gui.hud.HudText;
import dev.jilaton.client.music.Lyrics;
import org.junit.jupiter.api.Test;

import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The draw path itself, not just the fitter: {@link SpatialLyricsModule#fittedLine} is what the emit loop calls
 * for every visible slot, so these tests pin the line the client actually renders.
 */
class SpatialLyricsFitTest {
    private static final IntUnaryOperator SIX_PX = character -> 6;

    @Test
    void overlongLineEllipsisesOnAWordBoundaryWithinTheSlot() {
        Lyrics lyrics = Lyrics.parse("[00:00.00]alpha beta gamma delta epsilon zeta eta theta");
        String fitted = SpatialLyricsModule.fittedLine(lyrics, 0, 0, SIX_PX);
        assertEquals("alpha beta gamma delta epsilon\u2026", fitted);
        assertTrue(HudText.width(fitted, SIX_PX) <= SpatialLyricsModule.MAX_LINE_PIXELS);
        // 0.020 world units per font pixel: the slot is 4 units wide per font-size unit.
        assertEquals(4.0, SpatialLyricsModule.MAX_LINE_PIXELS * 0.020, 1e-9);
        String kept = fitted.substring(0, fitted.length() - 1);
        assertTrue(lyrics.lines().get(0).text().startsWith(kept));
        assertTrue(Character.isWhitespace(lyrics.lines().get(0).text().charAt(kept.length())));
        // Fitting is a copy: the lyric itself is untouched, so the stack key and anchor do not churn.
        assertEquals("alpha beta gamma delta epsilon zeta eta theta", lyrics.lines().get(0).text());
    }

    @Test
    void unbrokenTokenWiderThanTheSlotIsCutAndNeverEmpty() {
        Lyrics lyrics = Lyrics.parse("[00:00.00]" + "x".repeat(500));
        String fitted = SpatialLyricsModule.fittedLine(lyrics, 0, 0, SIX_PX);
        assertNotNull(fitted);
        assertFalse(fitted.isEmpty());
        assertTrue(HudText.width(fitted, SIX_PX) <= SpatialLyricsModule.MAX_LINE_PIXELS);
        assertTrue(fitted.endsWith(HudText.ELLIPSIS));
        assertFalse(fitted.substring(0, fitted.length() - 1).isEmpty());
    }

    @Test
    void lineThatFitsIsReturnedUnchanged() {
        Lyrics lyrics = Lyrics.parse("[00:01.00]short line");
        String fitted = SpatialLyricsModule.fittedLine(lyrics, 0, 0, SIX_PX);
        assertEquals("short line", fitted);
        assertEquals(60, HudText.width(fitted, SIX_PX));
    }

    @Test
    void blankLinesAndOutOfRangeSlotsDrawNothing() {
        Lyrics lyrics = Lyrics.parse("[00:00.00]one\n[00:01.00]\n[00:02.00]   ");
        assertEquals(3, lyrics.lines().size());
        assertEquals("one", SpatialLyricsModule.fittedLine(lyrics, 0, 0, SIX_PX));
        assertNull(SpatialLyricsModule.fittedLine(lyrics, 0, 1, SIX_PX));
        assertNull(SpatialLyricsModule.fittedLine(lyrics, 0, 2, SIX_PX));
        assertNull(SpatialLyricsModule.fittedLine(lyrics, 0, -1, SIX_PX));
        assertNull(SpatialLyricsModule.fittedLine(lyrics, -1, 0, SIX_PX));
        assertNull(SpatialLyricsModule.fittedLine(lyrics, 0, 3, SIX_PX));
        assertNull(SpatialLyricsModule.fittedLine(lyrics, 99, 0, SIX_PX));
        assertNull(SpatialLyricsModule.fittedLine(Lyrics.EMPTY, 0, 0, SIX_PX));
        assertNull(SpatialLyricsModule.fittedLine(null, 0, 0, SIX_PX));
    }

    @Test
    void timedWordsAreDroppedWhenTheLineIsEllipsised() {
        Lyrics lyrics = Lyrics.parse("[00:00.00]<00:00.00>alpha <00:00.50>beta <00:01.00>gamma "
                + "<00:01.50>delta <00:02.00>epsilon <00:02.50>zeta <00:03.00>eta <00:03.50>theta");
        Lyrics.Line active = lyrics.lines().get(0);
        assertTrue(active.timedWords());
        String fitted = SpatialLyricsModule.fittedLine(lyrics, 0, 0, SIX_PX);
        // The emit loop only uses the timed words when the fitted text is still the whole line.
        assertNotEquals(active.text(), fitted);
    }
}
