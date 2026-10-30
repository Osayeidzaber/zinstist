package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OffhandTotemPolicyTest {
    @Test
    void defaultsToStoredInventoryAndLeavesHotbarTotemsAlone() {
        boolean[] slots = new boolean[36];
        slots[2] = true;
        slots[17] = true;

        assertEquals(17, OffhandTotemPolicy.findSource(slots, false));
    }

    @Test
    void optedInHotbarSourceTakesPriorityOverStoredInventory() {
        boolean[] slots = new boolean[36];
        slots[4] = true;
        slots[17] = true;

        assertEquals(4, OffhandTotemPolicy.findSource(slots, true));
    }

    @Test
    void optedInHotbarSourceFallsBackToStoredInventoryWhenEmpty() {
        boolean[] slots = new boolean[36];
        slots[17] = true;

        assertEquals(17, OffhandTotemPolicy.findSource(slots, true));
        assertEquals(-1, OffhandTotemPolicy.findSource(new boolean[36], true));
    }

    @Test
    void rejectsIncompleteSlotSnapshots() {
        assertThrows(IllegalArgumentException.class,
                () -> OffhandTotemPolicy.findSource(new boolean[9], false));
        assertThrows(IllegalArgumentException.class,
                () -> OffhandTotemPolicy.findSource(null, false));
    }

    @Test
    void storedOnlyScanStartsAtTheFirstStoredSlotAndSkipsTheHotbar() {
        boolean[] slots = new boolean[36];
        slots[8] = true;
        slots[9] = true;
        slots[20] = true;

        assertEquals(9, OffhandTotemPolicy.findSource(slots, false));
        assertEquals(8, OffhandTotemPolicy.findSource(slots, true));
    }

    @Test
    void hotbarOptInFallsBackToStoredSlotsWhenOnlyOffhandIsInTheHotbar() {
        boolean[] slots = new boolean[36];
        slots[9] = true;

        assertEquals(9, OffhandTotemPolicy.findSource(slots, true));
    }
}
