package dev.jilaton.client.gui;

import dev.jilaton.client.module.ModuleManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleSearchTest {
    @Test
    void matchesSettingLabelsAndEveryWord() {
        ModuleManager modules = new ModuleManager();
        var storage = modules.get("storage_esp");
        assertTrue(ModuleSearch.matches(storage, "shulker"), "setting label");
        assertTrue(ModuleSearch.matches(storage, "esp shulker"), "two words");
        assertFalse(ModuleSearch.matches(storage, "esp banana"));
        assertTrue(ModuleSearch.matches(modules.get("auto_walk"), "auto walk"), "id words");
    }
}
