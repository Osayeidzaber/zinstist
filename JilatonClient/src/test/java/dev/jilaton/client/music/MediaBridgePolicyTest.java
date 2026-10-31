package dev.jilaton.client.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The desktop media bridge must stay off until opted in, then retry a bounded number of times. */
class MediaBridgePolicyTest {
    private static final long SECOND = 1_000_000_000L;
    private static final long NOW = 1_000 * SECOND;

    @Test
    void disabledIsAlwaysOffEvenWhenTheProcessWouldOtherwiseBeAlive() {
        assertEquals(MediaBridgePolicy.Decision.OFF,
                MediaBridgePolicy.decide(false, true, true, NOW, 0, 0));
        assertEquals(MediaBridgePolicy.Decision.OFF,
                MediaBridgePolicy.decide(false, true, false, NOW, Long.MAX_VALUE, 99));
        assertEquals(MediaBridgePolicy.OFF_STATUS, MediaBridgePolicy.idleStatus(false, true));
        assertEquals(MediaBridgePolicy.OFF_STATUS, MediaBridgePolicy.idleStatus(false, false));
    }

    @Test
    void unsupportedPlatformsNeverSpawnAndExplainWhy() {
        assertEquals(MediaBridgePolicy.Decision.UNSUPPORTED,
                MediaBridgePolicy.decide(true, false, false, NOW, 0, 0));
        assertEquals(MediaBridgePolicy.UNSUPPORTED_STATUS, MediaBridgePolicy.idleStatus(true, false));
        assertEquals("", MediaBridgePolicy.idleStatus(true, true));
    }

    @Test
    void anAliveBridgeIsRunningAndADeadOneSpawnsOnlyAfterTheDelay() {
        assertEquals(MediaBridgePolicy.Decision.RUNNING,
                MediaBridgePolicy.decide(true, true, true, NOW, NOW + SECOND, 2));
        assertEquals(MediaBridgePolicy.Decision.WAITING,
                MediaBridgePolicy.decide(true, true, false, NOW, NOW + 1, 1));
        assertEquals(MediaBridgePolicy.Decision.SPAWN,
                MediaBridgePolicy.decide(true, true, false, NOW, NOW, 1));
    }

    @Test
    void startsAreCappedAtThree() {
        for (int starts = 0; starts < MediaBridgePolicy.MAX_CONSECUTIVE_STARTS; starts++) {
            assertEquals(MediaBridgePolicy.Decision.SPAWN,
                    MediaBridgePolicy.decide(true, true, false, NOW, 0, starts), "start " + starts);
        }
        assertEquals(MediaBridgePolicy.Decision.GAVE_UP,
                MediaBridgePolicy.decide(true, true, false, NOW, 0, MediaBridgePolicy.MAX_CONSECUTIVE_STARTS));
        assertEquals(MediaBridgePolicy.Decision.GAVE_UP,
                MediaBridgePolicy.decide(true, true, false, NOW, 0, 1_000));
    }

    @Test
    void backoffDoublesFromFiveSecondsAndCapsAtFiveMinutes() {
        assertEquals(5 * SECOND, MediaBridgePolicy.restartDelayNanos(0));
        assertEquals(10 * SECOND, MediaBridgePolicy.restartDelayNanos(1));
        assertEquals(160 * SECOND, MediaBridgePolicy.restartDelayNanos(5));
        assertEquals(300 * SECOND, MediaBridgePolicy.restartDelayNanos(6));
        assertEquals(300 * SECOND, MediaBridgePolicy.restartDelayNanos(1_000));
        assertEquals(5 * SECOND, MediaBridgePolicy.restartDelayNanos(-3));
        // The old entry point in the bridge keeps delegating to this policy.
        assertEquals(MediaBridgePolicy.restartDelayNanos(2), MediaSessionSource.restartDelayNanos(2));
    }

    @Test
    void aSilentBridgeIsStaleWhetherItNeverSpokeOrWentQuiet() {
        assertFalse(MediaBridgePolicy.stale(0, 0, NOW));
        assertFalse(MediaBridgePolicy.stale(0, NOW - 5 * SECOND, NOW));
        assertTrue(MediaBridgePolicy.stale(0, NOW - 11 * SECOND, NOW));
        assertFalse(MediaBridgePolicy.stale(NOW - 5 * SECOND, NOW - 60 * SECOND, NOW));
        assertTrue(MediaBridgePolicy.stale(NOW - 11 * SECOND, NOW - 60 * SECOND, NOW));
        assertNotEquals(MediaBridgePolicy.Decision.RUNNING,
                MediaBridgePolicy.decide(true, true, false, NOW, 0, MediaBridgePolicy.MAX_CONSECUTIVE_STARTS));
    }
}
