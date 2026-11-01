package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaypointStoreTest {
    @TempDir
    Path dir;

    @Test
    void waypointsPersistPerWorldAndDimension() {
        Path file = dir.resolve("waypoints.tsv");
        WaypointStore store = new WaypointStore(file);
        store.put(new WaypointStore.Waypoint("sp:World", "minecraft:overworld", "Base", 10, 64, -20, 0xF2F2F2));
        store.put(new WaypointStore.Waypoint("sp:World", "minecraft:the_nether", "Portal", 1, 70, 2, 0x6FD3FF));
        store.put(new WaypointStore.Waypoint("mp:example.org", "minecraft:overworld", "Shop", 5, 5, 5, 0xFFD166));
        WaypointStore reloaded = new WaypointStore(file);
        assertEquals(1, reloaded.in("sp:World", "minecraft:overworld").size());
        assertEquals("Base", reloaded.in("sp:World", "minecraft:overworld").get(0).name());
        assertEquals(-20, reloaded.in("sp:World", "minecraft:overworld").get(0).z());
        assertEquals(0x6FD3FF, reloaded.in("sp:World", "minecraft:the_nether").get(0).rgb());
    }

    @Test
    void addingTheSameNameMovesItAndRemoveIgnoresCase() {
        WaypointStore store = new WaypointStore(dir.resolve("w.tsv"));
        store.put(new WaypointStore.Waypoint("w", "d", "Home", 0, 0, 0, 0));
        store.put(new WaypointStore.Waypoint("w", "d", "home", 9, 9, 9, 0));
        assertEquals(1, store.in("w", "d").size());
        assertEquals(9, store.in("w", "d").get(0).x());
        assertTrue(store.remove("w", "d", "HOME"));
        assertFalse(store.remove("w", "d", "HOME"));
    }
}
