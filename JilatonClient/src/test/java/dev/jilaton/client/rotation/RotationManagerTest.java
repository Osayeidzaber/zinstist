package dev.jilaton.client.rotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotationManagerTest {
    @Test void mouseCorrectionUsesTheSamePriorityAndSuspensionRules() {
        RotationManager manager = new RotationManager();
        assertTrue(manager.permitsMouseCorrection(20, owner -> true));
        manager.request("aura", 70, 30, 10, 4);
        assertFalse(manager.permitsMouseCorrection(20, owner -> true));
        assertTrue(manager.permitsMouseCorrection(20, owner -> false));
        manager.beginTick();
        manager.request("aim", 20, 30, 10, 4);
        assertTrue(manager.permitsMouseCorrection(20, owner -> true));
        manager.suspendRequestsForTick();
        assertFalse(manager.permitsMouseCorrection(20, owner -> true));
    }

    @Test
    void requestsAreScopedToATickAndCanBeSuspendedByAnOwningAction() {
        RotationManager manager = new RotationManager();

        manager.beginTick();
        assertTrue(manager.request("aim", 1, 30.0F, 10.0F, 4.0F));
        manager.suspendRequestsForTick();
        assertFalse(manager.request("rod", 2, 40.0F, 10.0F, 4.0F));

        manager.beginTick();
        assertTrue(manager.request("rod", 2, 40.0F, 10.0F, 4.0F));
    }
}
