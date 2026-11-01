package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consume modules swap the hotbar selection to their food slot. Disabling the module while the item
 * is still being used must still give the player their selection back: after a disable no tick runs
 * any more, so a restore deferred to the end of the use would never happen.
 */
class HotbarSlotRestoreTest {
    @Test
    void restoresWhileTheSwappedItemIsStillInUse() {
        // Regression: disabling mid-eat used to skip the restore and then drop ownership, leaving
        // the player stuck on the food slot after the module was gone.
        assertTrue(HotbarSlotRestore.shouldRestore(3, 3, 1));
    }

    @Test
    void restoresWhenTheSelectionIsStillTheOwnedSlot() {
        assertTrue(HotbarSlotRestore.shouldRestore(3, 3, 1));
    }

    @Test
    void restoresHotbarSlotZeroLikeAnyOtherOwnedSlot() {
        assertTrue(HotbarSlotRestore.shouldRestore(0, 0, 4));
    }

    @Test
    void leavesTheSelectionAloneAfterThePlayerMovedIt() {
        assertFalse(HotbarSlotRestore.shouldRestore(5, 3, 1));
    }

    @Test
    void doesNothingWhenTheModuleNeverOwnedSelection() {
        assertFalse(HotbarSlotRestore.shouldRestore(0, -1, -1));
        assertFalse(HotbarSlotRestore.shouldRestore(0, 0, -1));
        assertFalse(HotbarSlotRestore.shouldRestore(0, -1, 0));
        // Offhand uses (AutoHead) never swap the selection, so there is nothing to give back.
        assertFalse(HotbarSlotRestore.shouldRestore(0, -1, -1));
    }

    @Test
    void everyConsumeModuleDelegatesTheOwnershipCheckToThisRule() throws Exception {
        for (String name : List.of("AutoGappleModule", "AutoEatModule", "AutoHeadModule", "AutoSoupModule")) {
            String source = Files.readString(Path.of(
                    "src/main/java/dev/jilaton/client/module/impl/" + name + ".java"));
            assertTrue(source.contains("HotbarSlotRestore.shouldRestore("),
                    name + " must use the shared hotbar ownership rule");
            assertFalse(source.contains("previousSlot >= 0"),
                    name + " must not keep its own copy of the ownership check");
        }
    }
}
