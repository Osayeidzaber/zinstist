package dev.jilaton.client.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaybackClockTest {
    private static final long MS = 1_000_000L;

    private static NowPlaying at(String title, long positionMs, boolean playing, long capturedMs) {
        return new NowPlaying("Spotify", title, "Artist", "", positionMs, 200_000, playing, null, "", "", true, true,
                false, capturedMs * MS);
    }

    @Test
    void pollJitterNeverMovesThePositionBackwards() {
        PlaybackClock clock = new PlaybackClock();
        clock.smooth(at("Song", 10_000, true, 0), 0);
        long previous = 0;
        long[] jitter = {0, -180, 150, -220, 90, -60};
        for (int i = 1; i < jitter.length; i++) {
            long t = i * 1000L;
            long now = clock.smooth(at("Song", 10_000 + t + jitter[i], true, t), t * MS).positionNow(t * MS);
            assertTrue(now >= previous, "position went back at poll " + i);
            assertTrue(Math.abs(now - (10_000 + t)) < 200, "drift " + (now - (10_000 + t)));
            previous = now;
        }
    }

    @Test
    void seekIsAppliedImmediately() {
        PlaybackClock clock = new PlaybackClock();
        clock.smooth(at("Song", 10_000, true, 0), 0);
        NowPlaying sought = clock.smooth(at("Song", 60_000, true, 1000), 1000 * MS);
        assertEquals(60_000, sought.positionNow(1000 * MS));
    }

    @Test
    void newTrackAndPauseTakeTheReportedPosition() {
        PlaybackClock clock = new PlaybackClock();
        clock.smooth(at("Song", 10_000, true, 0), 0);
        assertEquals(500, clock.smooth(at("Other", 500, true, 1000), 1000 * MS).positionNow(1000 * MS));
        assertEquals(700, clock.smooth(at("Other", 700, false, 1200), 1200 * MS).positionNow(1300 * MS));
    }
}
