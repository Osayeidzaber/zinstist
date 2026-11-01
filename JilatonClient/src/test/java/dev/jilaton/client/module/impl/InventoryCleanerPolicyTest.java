package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryCleanerPolicyTest {
    @Test
    void findsJunkInTheMainInventoryAndLeavesTheHotbarAlone() {
        String[] ids = new String[45];
        Arrays.fill(ids, "");
        ids[40] = "rotten_flesh";
        Set<String> junk = InventoryCleanerModule.parse("Rotten_Flesh, minecraft:string");
        assertEquals(-1, InventoryCleanerPolicy.firstJunk(ids, junk, false));
        assertEquals(40, InventoryCleanerPolicy.firstJunk(ids, junk, true));
        ids[20] = "string";
        assertEquals(20, InventoryCleanerPolicy.firstJunk(ids, junk, false));
        ids[20] = "diamond";
        assertEquals(-1, InventoryCleanerPolicy.firstJunk(ids, junk, false));
    }
}
