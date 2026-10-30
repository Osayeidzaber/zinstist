package dev.jilaton.client.module;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModuleNotificationsTest {
    @Test
    void stateChangesNotifyOnceAndRepeatedAssignmentDoesNotAddNotice() {
        ModuleManager modules = new ModuleManager();
        ClientModule hud = modules.get("hud");
        hud.setEnabled(true);
        var enabled = modules.notifications().current(System.nanoTime());
        assertEquals(1, enabled.size());
        assertTrue(enabled.getFirst().enabled());
        long createdAt = enabled.getFirst().createdAt();
        hud.setEnabled(true);
        assertEquals(createdAt, modules.notifications().current(System.nanoTime()).getFirst().createdAt());
        hud.setEnabled(false);
        var disabled = modules.notifications().current(System.nanoTime());
        assertEquals(1, disabled.size());
        assertFalse(disabled.getFirst().enabled());
    }
}
