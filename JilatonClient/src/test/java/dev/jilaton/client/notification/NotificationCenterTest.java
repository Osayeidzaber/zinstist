package dev.jilaton.client.notification;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationCenterTest {
    @Test
    void coalescesModuleChangesAndExpiresAtDeadline() {
        NotificationCenter center = new NotificationCenter();
        center.push("hud", "HUD enabled", true, 0);
        center.push("hud", "HUD disabled", false, 1_000);
        assertEquals(1, center.current(1_000).size());
        assertFalse(center.current(1_000).getFirst().enabled());
        assertEquals(1, center.current(NotificationCenter.LIFETIME_NANOS).size());
        assertTrue(center.current(NotificationCenter.LIFETIME_NANOS + 1_000).isEmpty());
    }

    @Test
    void boundedQueueDropsOldestAndExposesImmutableSnapshot() {
        NotificationCenter center = new NotificationCenter();
        for (int index = 0; index < 50; index++) center.push("m" + index, "Module " + index, true, index);
        var snapshot = center.current(100);
        assertEquals(4, snapshot.size());
        assertEquals("m46", snapshot.getFirst().id());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        center.clear();
        assertTrue(center.current(101).isEmpty());
    }
}
