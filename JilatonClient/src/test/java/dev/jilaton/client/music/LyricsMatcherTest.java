package dev.jilaton.client.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LyricsMatcherTest {
    private static JsonObject candidate(String title, String artist, double duration, boolean synced) {
        JsonObject o = new JsonObject();
        o.addProperty("trackName", title);
        o.addProperty("artistName", artist);
        o.addProperty("duration", duration);
        if (synced) o.addProperty("syncedLyrics", "[00:01.00]line");
        else o.addProperty("plainLyrics", "line");
        return o;
    }

    @Test
    void foldsTitlesAndStripsVersionSuffixes() {
        assertEquals("young and dumb", LyricsMatcher.normalize("Young & Dumb"));
        assertEquals("Song", LyricsMatcher.baseTitle("Song (feat. Someone)"));
        assertEquals("Song", LyricsMatcher.baseTitle("Song - Remastered 2011"));
        assertEquals("Artist A", LyricsMatcher.primaryArtist("Artist A, Artist B"));
        assertEquals(30, LyricsMatcher.titleScore("Song - Remastered 2011", "Song"));
    }

    @Test
    void searchTermsIncludeAccentFoldedTitle() {
        // LRCLIB compares literal text: the accented title returns no rows, the folded one does.
        assertEquals("Money so big", LyricsMatcher.folded("Monëy so big"));
        assertEquals("Up 2 Me", LyricsMatcher.folded("Up 2 Më"));
        boolean folded = false;
        for (String[] term : LyricsMatcher.searchTerms("Monëy so big", "Yeat"))
            folded |= term[0].equals("Money so big") && term[1].equals("Yeat");
        assertTrue(folded);
        assertEquals(1, LyricsMatcher.searchTerms("Song", "Artist").size());
    }

    @Test
    void picksSyncedCandidateWithinDurationTolerance() {
        JsonArray list = new JsonArray();
        list.add(candidate("Young & Dumb", "Cigarettes After Sex", 280, true));   // too far
        list.add(candidate("Young & Dumb", "Cigarettes After Sex", 268.5, false)); // plain only
        list.add(candidate("Young & Dumb", "Cigarettes After Sex", 270, true));
        list.add(candidate("Young & Dumb", "Cigarettes After Sex", 267, true));
        JsonObject best = LyricsMatcher.best(list, "Young & Dumb", "Cigarettes After Sex", 268);
        assertNotNull(best);
        assertTrue(best.has("syncedLyrics"));
        assertTrue(Math.abs(best.get("duration").getAsDouble() - 268) <= 2);
        assertTrue(LyricsMatcher.score(candidate("Other", "Cigarettes After Sex", 268, true), "Young & Dumb", "Cigarettes After Sex", 268) < 0);
        assertTrue(LyricsMatcher.score(candidate("Young & Dumb", "Someone Else", 268, true), "Young & Dumb", "Cigarettes After Sex", 268) < 0);
    }
}
