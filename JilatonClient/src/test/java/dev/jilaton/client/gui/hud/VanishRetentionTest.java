package dev.jilaton.client.gui.hud;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanishRetentionTest {
    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test void retainedAtFourSecondsAndExpiredAtSix() {
        VanishRetention retention = new VanishRetention(5_000);
        retention.seen(ALEX, "Alex", 10, 64, -4, 1_000);

        assertTrue(retention.retain(ALEX, 5_000));
        assertTrue(retention.isRetained(ALEX));
        VanishRetention.Snapshot kept = retention.get(ALEX, 5_000).orElseThrow();
        assertTrue(kept.retained());
        assertEquals(4_000, kept.ageMillis());
        assertEquals(10, kept.x());
        assertEquals(64, kept.y());
        assertEquals(-4, kept.z());

        assertFalse(retention.retain(ALEX, 7_000));
        assertFalse(retention.isRetained(ALEX));
        assertTrue(retention.get(ALEX, 7_000).isEmpty());
        assertTrue(retention.snapshot(7_000).isEmpty());
        assertTrue(retention.ids().isEmpty());
    }

    @Test void returningPlayerDropsTheRetainedEntry() {
        VanishRetention retention = new VanishRetention(5_000);
        retention.seen(ALEX, "Alex", 10, 64, -4, 1_000);
        assertTrue(retention.retain(ALEX, 3_000));

        retention.seen(ALEX, "Alex", 12, 64, -2, 4_000);

        assertFalse(retention.isRetained(ALEX));
        VanishRetention.Snapshot current = retention.get(ALEX, 4_000).orElseThrow();
        assertFalse(current.retained());
        assertEquals(0, current.ageMillis());
        assertEquals(12, current.x());
        assertEquals(-2, current.z());
    }

    @Test void sweepOnlyRemovesExpiredVanishedEntries() {
        VanishRetention retention = new VanishRetention(5_000);
        retention.seen(ALEX, "Alex", 0, 0, 0, 1_000);
        retention.seen(BOB, "Bob", 1, 0, 1, 1_000);
        retention.retain(ALEX, 4_000);

        retention.sweep(7_000);

        assertFalse(retention.ids().contains(ALEX));
        assertTrue(retention.ids().contains(BOB));
        assertTrue(retention.snapshot(7_000).stream().anyMatch(entry -> entry.id().equals(BOB)));
    }

    @Test void windowChangesReEvaluateAge() {
        VanishRetention retention = new VanishRetention(5_000);
        retention.seen(ALEX, "Alex", 0, 0, 0, 0);
        assertTrue(retention.retain(ALEX, 5_000));

        retention.setWindowMillis(10_000);
        assertEquals(10_000, retention.windowMillis());
        VanishRetention.Snapshot kept = retention.get(ALEX, 5_500).orElseThrow();
        assertTrue(kept.retained());
        assertEquals(5_500, kept.ageMillis());

        assertFalse(retention.snapshot(10_001).stream().anyMatch(entry -> entry.id().equals(ALEX)));
        assertThrows(IllegalArgumentException.class, () -> retention.setWindowMillis(0));
        assertThrows(IllegalArgumentException.class, () -> new VanishRetention(-1));
    }

    @Test void unseenIdsAndInvalidPositionsAreHandled() {
        VanishRetention retention = new VanishRetention(5_000);
        assertFalse(retention.retain(UUID.randomUUID(), 0));
        assertTrue(retention.get(UUID.randomUUID(), 0).isEmpty());
        assertFalse(retention.isRetained(UUID.randomUUID()));

        assertThrows(IllegalArgumentException.class,
                () -> retention.seen(ALEX, "Alex", Double.NaN, 0, 0, 0));
        assertThrows(NullPointerException.class, () -> retention.seen(null, "Alex", 0, 0, 0, 0));

        retention.seen(ALEX, "Alex", 0, 0, 0, 0);
        retention.remove(ALEX);
        assertTrue(retention.ids().isEmpty());
    }
}
