package dev.jilaton.client.module.impl;

import dev.jilaton.client.music.Lyrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** The stack key decides when a lyric stack gets a fresh anchor at the player's gaze. */
class SpatialLyricsStackKeyTest {
    private static final Lyrics LYRICS = Lyrics.parse("[00:01]one\n[00:05]two\n");

    private static SpatialLyricsModule.StackKey key(String title, Lyrics lyrics, String mode, double radius,
                                                    double spread) {
        return new SpatialLyricsModule.StackKey("spotify", title, "artist", lyrics, mode, radius, spread);
    }

    @Test
    void identicalStacksShareOneAnchor() {
        assertEquals(key("song", LYRICS, "Floating Side", 2.3, 16),
                key("song", LYRICS, "Floating Side", 2.3, 16));
    }

    @Test
    void trackLyricsOrLayoutChangesStartANewAnchor() {
        SpatialLyricsModule.StackKey base = key("song", LYRICS, "Floating Side", 2.3, 16);
        assertNotEquals(base, key("other song", LYRICS, "Floating Side", 2.3, 16));
        assertNotEquals(base, key("song", Lyrics.EMPTY, "Floating Side", 2.3, 16));
        assertNotEquals(base, key("song", Lyrics.parse("[00:01]one\n"), "Floating Side", 2.3, 16));
        assertNotEquals(base, key("song", LYRICS, "Curved Arc", 2.3, 16));
        assertNotEquals(base, key("song", LYRICS, "Floating Side", 2.4, 16));
        assertNotEquals(base, key("song", LYRICS, "Floating Side", 2.3, 20));
    }
}
