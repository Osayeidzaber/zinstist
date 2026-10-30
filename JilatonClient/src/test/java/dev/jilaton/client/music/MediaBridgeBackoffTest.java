package dev.jilaton.client.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaBridgeBackoffTest {
    private static final long SECOND = 1_000_000_000L;

    @Test
    void restartDelayDoublesFromFiveSecondsAndCapsAtFiveMinutes() {
        assertEquals(5 * SECOND, MediaSessionSource.restartDelayNanos(0));
        assertEquals(10 * SECOND, MediaSessionSource.restartDelayNanos(1));
        assertEquals(160 * SECOND, MediaSessionSource.restartDelayNanos(5));
        assertEquals(300 * SECOND, MediaSessionSource.restartDelayNanos(6));
        assertEquals(300 * SECOND, MediaSessionSource.restartDelayNanos(1_000));
        assertEquals(5 * SECOND, MediaSessionSource.restartDelayNanos(-3));
    }
}
