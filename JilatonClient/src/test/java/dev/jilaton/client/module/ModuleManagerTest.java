package dev.jilaton.client.module;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleManagerTest {
    @Test
    void registryIsUniqueAndCannotBeModifiedThroughItsPublicView() {
        ModuleManager manager = new ModuleManager();
        Set<String> ids = new HashSet<>();
        manager.all().forEach(module -> assertTrue(ids.add(module.id()), "Duplicate module id: " + module.id()));

        int originalSize = manager.all().size();
        assertThrows(UnsupportedOperationException.class, manager.all()::clear);
        assertEquals(originalSize, manager.all().size());
        assertEquals(originalSize, ids.size());
    }
}
