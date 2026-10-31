package dev.jilaton.client.music;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LRCLIB lookup policy without the network: retries are bounded per track, the negative cache expires, a
 * found lyric clears the .none marker, and a pending lookup for one track never prevents another from being
 * scheduled. All work runs on an injected executor/clock so nothing waits on a render thread.
 */
class MusicPrivacyLyricsTest {
    @TempDir
    Path directory;

    private static final class MutableClock extends Clock {
        private long millis;
        MutableClock(long millis) { this.millis = millis; }
        void advance(long delta) { millis += delta; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        @Override public long millis() { return millis; }
    }

    private static NowPlaying track(String title, String artist) {
        return new NowPlaying("Local", title, artist, "Album", 0, 120_000, false, null, "This PC", "Playing",
                true, true, true, System.nanoTime());
    }

    @Test
    void negativeCacheAndRetryBoundaries() {
        long ttl = OnlineLyrics.NEGATIVE_TTL_MILLIS;
        assertTrue(OnlineLyrics.negativeCacheFresh(1_000, 1_000));
        assertTrue(OnlineLyrics.negativeCacheFresh(1_000, 1_000 + ttl - 1));
        assertFalse(OnlineLyrics.negativeCacheFresh(1_000, 1_000 + ttl), "the marker expires exactly at the TTL");

        assertTrue(OnlineLyrics.negativeCacheAlive(1_000 + ttl, 1_000));
        assertFalse(OnlineLyrics.negativeCacheAlive(1_000 + ttl, 1_000 + ttl), "an in-memory entry expires at its TTL, not twice it");

        assertTrue(OnlineLyrics.retryExpired(1_000, 1_000));
        assertFalse(OnlineLyrics.retryExpired(1_000, 999));
        assertTrue(OnlineLyrics.retryExpired(1_000, 1_000 + OnlineLyrics.RETRY_MILLIS));

        assertTrue(OnlineLyrics.retryAllowed(0));
        assertTrue(OnlineLyrics.retryAllowed(OnlineLyrics.MAX_LOOKUP_FAILURES - 1));
        assertFalse(OnlineLyrics.retryAllowed(OnlineLyrics.MAX_LOOKUP_FAILURES));
        assertFalse(OnlineLyrics.retryAllowed(99));
    }

    @Test
    void failedLookupsAreBoundedAndNeverSleepOnTheCaller() {
        MutableClock clock = new MutableClock(System.currentTimeMillis());
        AtomicInteger calls = new AtomicInteger();
        OnlineLyrics online = new OnlineLyrics(directory, clock, Runnable::run, (title, artist, album, duration) -> {
            calls.incrementAndGet();
            throw new IOException("offline");
        });
        NowPlaying track = track("Song", "Artist");

        assertTrue(online.get(track).isEmpty());
        assertEquals(1, calls.get());
        assertEquals("Lyrics lookup failed (offline?)", online.status(track));

        online.get(track);
        assertEquals(1, calls.get(), "an immediate retry must wait for the back-off");

        clock.advance(OnlineLyrics.RETRY_MILLIS);
        online.get(track);
        assertEquals(2, calls.get());
        clock.advance(OnlineLyrics.RETRY_MILLIS);
        online.get(track);
        assertEquals(3, calls.get());

        clock.advance(OnlineLyrics.RETRY_MILLIS * 100);
        online.get(track);
        assertEquals(3, calls.get(), "the retry cap is terminal for this track");
        online.shutdown();
    }

    @Test
    void negativeCacheExpiresAndFoundLyricsClearTheMarker() throws Exception {
        MutableClock clock = new MutableClock(System.currentTimeMillis());
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> answer = new AtomicReference<>();
        OnlineLyrics.SyncedFetcher fetcher = (title, artist, album, duration) -> {
            calls.incrementAndGet();
            return answer.get();
        };
        NowPlaying track = track("Song", "Artist");

        OnlineLyrics first = new OnlineLyrics(directory, clock, Runnable::run, fetcher);
        assertTrue(first.get(track).isEmpty());
        assertEquals(1, calls.get());
        assertEquals("No synced lyrics on LRCLIB", first.status(track));
        first.get(track);
        assertEquals(1, calls.get(), "a fresh negative result must not refetch");
        first.shutdown();

        // A new session still honours the fresh on-disk .none marker.
        OnlineLyrics second = new OnlineLyrics(directory, clock, Runnable::run, fetcher);
        second.get(track);
        assertEquals(1, calls.get(), "the .none marker is still fresh on disk");

        // One hour later the same track is looked up again, and this time lyrics exist.
        clock.advance(OnlineLyrics.NEGATIVE_TTL_MILLIS + 60_000L);
        answer.set("[00:01.00]Hello\n[00:03.00]World");
        second.get(track);
        assertEquals(2, calls.get(), "the expired negative cache must allow a new lookup");
        Lyrics found = second.get(track);
        assertEquals(2, found.lines().size());
        assertEquals("Hello", found.lines().get(0).text());
        try (var files = Files.list(directory.resolve("jilaton").resolve("lyrics-cache"))) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".none")),
                    "found lyrics must remove the negative marker");
        }
        second.shutdown();
    }

    @Test
    void onePendingLookupDoesNotPreventAnotherTrackFromScheduling() {
        MutableClock clock = new MutableClock(System.currentTimeMillis());
        List<Runnable> queue = new ArrayList<>();
        OnlineLyrics online = new OnlineLyrics(directory, clock, queue::add,
                (title, artist, album, duration) -> { throw new AssertionError("not run in this test"); });
        NowPlaying first = track("First", "One");
        NowPlaying second = track("Second", "Two");

        online.get(first);
        online.get(second);

        assertEquals(2, queue.size(), "both tracks must be accepted for lookup independently");
        assertEquals("Searching LRCLIB…", online.status(first));
        assertEquals("Searching LRCLIB…", online.status(second));
        online.shutdown();
    }
}
