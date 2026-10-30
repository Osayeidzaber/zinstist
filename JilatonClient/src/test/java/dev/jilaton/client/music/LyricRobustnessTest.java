package dev.jilaton.client.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Degenerate LRCLIB payloads must not break the lyric pipeline: no text, one line, duplicate stamps,
 * shuffled stamps, very large files, and missing track metadata.
 */
class LyricRobustnessTest {

    @Test
    void degenerateLyricTextYieldsNoActiveLine() {
        for (String text : new String[]{"", "   ", "\t \r\n", "\n\n\n", "\r\n\r\n"}) {
            Lyrics lyrics = assertDoesNotThrow(() -> Lyrics.parse(text), "parse must not throw for " + text.length() + " chars");
            assertTrue(lyrics.isEmpty());
            assertEquals(0, lyrics.lines().size());
            assertEquals(-1, lyrics.indexAt(-1));
            assertEquals(-1, lyrics.indexAt(0));
            assertEquals(-1, lyrics.indexAt(600_000));
        }
    }

    @Test
    void singleLineIsSilentBeforeItsTimestampAndActiveAfter() {
        Lyrics lyrics = Lyrics.parse("[00:05.00]only line");
        assertEquals(1, lyrics.lines().size());
        assertEquals("only line", lyrics.lines().get(0).text());
        assertEquals(5_000, lyrics.lines().get(0).startMs());
        assertEquals(-1, lyrics.indexAt(0));
        assertEquals(-1, lyrics.indexAt(4_999));
        assertEquals(0, lyrics.indexAt(5_000));
        assertEquals(0, lyrics.indexAt(600_000));
    }

    @Test
    void duplicateTimestampsKeepFileOrderAndDrawEachLineOnce() {
        Lyrics lyrics = Lyrics.parse("[00:01.00]first\n[00:01.00]second\n[00:02.00]third");
        // Stable sort: equal stamps keep file order; indexAt (upper bound) makes the later duplicate active.
        assertEquals(List.of("first", "second", "third"), texts(lyrics));
        assertEquals(-1, lyrics.indexAt(999));
        assertEquals(1, lyrics.indexAt(1_000));
        assertEquals(1, lyrics.indexAt(1_999));
        assertEquals(2, lyrics.indexAt(2_000));
        // The module draws slots -1..+1 around the active index; every line index appears at most once.
        List<Integer> drawn = new ArrayList<>();
        for (int slot = -1; slot <= 1; slot++) {
            int index = 1 + slot;
            if (index >= 0 && index < lyrics.lines().size()) drawn.add(index);
        }
        assertEquals(3, drawn.size());
        assertEquals(3, new HashSet<>(drawn).size());
        assertEquals(1, texts(lyrics).stream().filter("first"::equals).count());
        assertEquals(1, texts(lyrics).stream().filter("second"::equals).count());
    }

    @Test
    void outOfOrderTimestampsNeverResolveToAFutureLine() {
        Lyrics lyrics = Lyrics.parse("[00:10.00]late\n[00:01.00]early\n[00:05.00]mid");
        assertEquals(List.of("early", "mid", "late"), texts(lyrics));
        assertEquals(-1, lyrics.indexAt(999));
        assertEquals(0, lyrics.indexAt(1_000));
        assertEquals(1, lyrics.indexAt(5_000));
        assertEquals(2, lyrics.indexAt(10_000));
        for (long position = -1_000; position <= 12_000; position += 50) {
            int index = lyrics.indexAt(position);
            if (index >= 0) assertTrue(lyrics.lines().get(index).startMs() <= position, "future line at " + position);
            if (index + 1 < lyrics.lines().size())
                assertTrue(lyrics.lines().get(index + 1).startMs() > position, "skipped line at " + position);
        }
    }

    @Test
    void tenThousandLinesParseAndLookUpWellUnderASecond() {
        int count = 10_000;
        StringBuilder text = new StringBuilder(count * 24);
        long[] stamps = new long[count];
        for (int i = 0; i < count; i++) {
            long ms = i * 250L;
            stamps[i] = ms;
            String stamp = String.format("%02d:%02d.%02d", ms / 60_000, (ms / 1_000) % 60, (ms % 1_000) / 10);
            text.append('[').append(stamp).append("]line ").append(i).append('\n');
        }
        long started = System.nanoTime();
        Lyrics lyrics = Lyrics.parse(text.toString());
        long parseNanos = System.nanoTime() - started;
        assertEquals(count, lyrics.lines().size());
        assertTrue(parseNanos < 1_000_000_000L, "parse took " + parseNanos / 1_000_000 + " ms");
        assertEquals(0, lyrics.indexAt(0));
        assertEquals(count - 1, lyrics.indexAt(stamps[count - 1]));

        started = System.nanoTime();
        for (int i = 0; i < 100_000; i++) {
            int expected = i % count;
            assertEquals(expected, lyrics.indexAt(stamps[expected]));
        }
        long lookupNanos = System.nanoTime() - started;
        // One binary search per frame is O(log n); 100k lookups over 10k lines must stay far below a second.
        assertTrue(lookupNanos < 1_000_000_000L, "100k lookups took " + lookupNanos / 1_000_000 + " ms");
    }

    @Test
    void nullOrBlankMetadataDegradesToUnsynced() {
        JsonObject candidate = new JsonObject();
        candidate.addProperty("trackName", "Song");
        candidate.addProperty("artistName", "Artist");
        candidate.addProperty("duration", 100);
        candidate.addProperty("syncedLyrics", "[00:01.00]one");
        assertEquals(-1, LyricsMatcher.score(candidate, null, null, 100));
        assertEquals(-1, LyricsMatcher.score(candidate, "  ", "\t", 100));
        JsonArray candidates = new JsonArray();
        candidates.add(candidate);
        assertNull(LyricsMatcher.best(candidates, null, "", 100));
        assertTrue(LyricsMatcher.searchTerms(null, null).isEmpty());
        assertTrue(LyricsMatcher.searchTerms("", "  ").isEmpty());
        assertEquals("", LyricsMatcher.primaryArtist(null));
        assertEquals("", LyricsMatcher.baseTitle(null));
        assertEquals("", LyricsMatcher.folded(null));

        NowPlaying untitled = new NowPlaying("Spotify", null, null, null, 0, 0, false, null, "", "No track",
                true, true, true, 0);
        assertFalse(untitled.hasTrack());
        NowPlaying blankTitle = new NowPlaying("Spotify", "", null, null, 0, 0, false, null, "", "No track",
                true, true, true, 0);
        assertFalse(blankTitle.hasTrack());
        NowPlaying noArtist = new NowPlaying("Spotify", "Song", null, null, 0, 0, false, null, "", "Playing",
                true, true, true, 0);
        assertTrue(noArtist.hasTrack());
        assertEquals("song\u0000", OnlineLyrics.key("Song", null));
        assertEquals(OnlineLyrics.key("", ""), OnlineLyrics.key(null, null));

        OnlineLyrics online = new OnlineLyrics(Path.of("build", "unused-lyrics"));
        try {
            assertEquals(Lyrics.EMPTY, online.get(untitled));
            assertEquals("", online.status(untitled));
        } finally {
            online.shutdown();
        }
    }

    private static List<String> texts(Lyrics lyrics) {
        return lyrics.lines().stream().map(Lyrics.Line::text).collect(Collectors.toList());
    }
}
