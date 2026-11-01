package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.ModuleManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-9 acceptance: the ClickGUI's page, category, selection, expansion and scroll offsets are written by
 * {@code ClickGuiScreen.syncStateToConfig()} / {@code JilatonClickGui.remember()} into {@link ClientConfig} and read
 * back by the screen constructors. A screen cannot be constructed headless (Screen's constructor reads
 * Minecraft.getInstance().font), so this test drives the exact {@link ClientConfig} field set those two methods
 * touch, saves through the real file format into an explicit temp directory, and reloads into a fresh config.
 */
class ClickGuiPersistenceRoundTripTest {
    @TempDir
    Path directory;

    @Test
    void everyClickGuiStateFieldSurvivesARestart() throws IOException {
        ModuleManager modules = new ModuleManager();
        ClientConfig config = new ClientConfig(directory);
        List<String> moduleOrder = rotatedIds(modules, null, 1);
        List<String> visualOrder = rotatedIds(modules, ModuleCategory.RENDER, 2);
        String selected = "aim_assist";
        String expanded = "auto_clicker";

        // Exactly what syncStateToConfig()/remember() write for a resumed session.
        config.setGuiPage("THEME");
        config.setGuiCategory("Movement");
        config.setGuiSelectedModule(selected);
        config.setGuiExpandedModule(expanded);
        config.setGuiEnabledOnly(true);
        config.setGuiSearchQuery("aura");
        config.setGuiSelectedProfile("pvp");
        config.setGuiScroll(137.5);
        config.setCategoryScroll("All", 42.5);
        config.setCategoryScroll("Movement", 137.5);
        config.setCategoryScroll("Visuals", 9.25);
        config.setModuleOrder(moduleOrder);
        config.setVisualOrder(visualOrder);
        config.save(modules);

        // The state is really on disk, not only in this instance's memory.
        String raw = Files.readString(directory.resolve("jilaton.properties"));
        assertTrue(raw.contains("ui.last_page=THEME"), raw);
        assertTrue(raw.contains("ui.last_category=Movement"), raw);
        assertTrue(raw.contains("ui.selected_module=" + selected), raw);
        assertTrue(raw.contains("ui.expanded_module=" + expanded), raw);
        assertTrue(raw.contains("ui.enabled_only=true"), raw);
        assertTrue(raw.contains("ui.search_query=aura"), raw);
        assertTrue(raw.contains("ui.selected_profile=pvp"), raw);
        assertTrue(raw.contains("ui.scroll.all=42.5"), raw);
        assertTrue(raw.contains("ui.scroll.movement=137.5"), raw);
        assertTrue(raw.contains("ui.module_order=" + String.join(",", moduleOrder)), raw);
        assertTrue(raw.contains("ui.visual_order=" + String.join(",", visualOrder)), raw);

        ClientConfig restored = new ClientConfig(directory);
        restored.load(new ModuleManager());

        assertEquals("THEME", restored.guiPage());
        assertEquals("Movement", restored.guiCategory());
        assertEquals(selected, restored.guiSelectedModule());
        assertEquals(expanded, restored.guiExpandedModule());
        assertTrue(restored.guiEnabledOnly());
        assertEquals("aura", restored.guiSearchQuery());
        assertEquals("pvp", restored.guiSelectedProfile());
        assertEquals(137.5, restored.guiScroll(), 1e-9);
        assertEquals(42.5, restored.categoryScroll("All"), 1e-9);
        assertEquals(137.5, restored.categoryScroll("Movement"), 1e-9);
        assertEquals(9.25, restored.categoryScroll("Visuals"), 1e-9);
        assertEquals(0.0, restored.categoryScroll("Combat"), 1e-9);
        assertEquals(moduleOrder, restored.moduleOrder());
        assertEquals(visualOrder, restored.visualOrder());
    }

    @Test
    void anUnrelatedSaveKeepsTheClickGuiStateThatWasCapturedOnClose() {
        // The GUI writes its state into the live ClientConfig and module toggles save the same file later, so a
        // toggle must not wipe the page/category/scroll that were captured when the screen closed.
        ModuleManager modules = new ModuleManager();
        ClientConfig config = new ClientConfig(directory);
        config.setGuiPage("CONFIGS");
        config.setGuiCategory("Client");
        config.setGuiSelectedModule("kill_aura");
        config.setGuiExpandedModule("kill_aura");
        config.setCategoryScroll("Client", 64.0);

        modules.get("kill_aura").toggle();
        config.save(modules);

        ClientConfig restored = new ClientConfig(directory);
        restored.load(new ModuleManager());
        assertEquals("CONFIGS", restored.guiPage());
        assertEquals("Client", restored.guiCategory());
        assertEquals("kill_aura", restored.guiSelectedModule());
        assertEquals("kill_aura", restored.guiExpandedModule());
        assertEquals(64.0, restored.categoryScroll("Client"), 1e-9);
        assertTrue(restored.guiSearchQuery().isEmpty());
    }

    @Test
    void collapsingTheExpansionAndScrollingBackToTheTopArePersistedToo() {
        ModuleManager modules = new ModuleManager();
        ClientConfig config = new ClientConfig(directory);
        config.setGuiExpandedModule("auto_clicker");
        config.setCategoryScroll("Combat", 120.0);
        config.save(modules);

        config.setGuiExpandedModule("");
        config.setGuiSelectedModule("");
        config.setCategoryScroll("Combat", 0.0);
        config.setGuiScroll(0.0);
        config.save(modules);

        ClientConfig restored = new ClientConfig(directory);
        restored.load(new ModuleManager());
        assertEquals("", restored.guiExpandedModule(), "closing the expansion must not come back after a restart");
        assertEquals("", restored.guiSelectedModule());
        assertEquals(0.0, restored.categoryScroll("Combat"), 1e-9);
        assertEquals(0.0, restored.guiScroll(), 1e-9);
    }

    @Test
    void theRestoredCategoryOffsetWinsOverTheLastGlobalOffset() {
        // ClickGuiScreen's constructor: scroll = categoryScroll(category) > 0 ? it : guiScroll.
        ModuleManager modules = new ModuleManager();
        ClientConfig config = new ClientConfig(directory);
        config.setGuiCategory("Visuals");
        config.setGuiScroll(210.0);
        config.setCategoryScroll("Visuals", 36.5);
        config.setCategoryScroll("All", 210.0);
        config.save(modules);

        ClientConfig restored = new ClientConfig(directory);
        restored.load(new ModuleManager());
        String category = restored.guiCategory();
        double scroll = restored.categoryScroll(category) > 0.0 ? restored.categoryScroll(category) : restored.guiScroll();
        assertEquals(36.5, scroll, 1e-9);
        assertEquals(210.0, restored.guiScroll(), 1e-9);
    }

    @Test
    void onlyKnownPagesCategoriesAndModuleIdsAreAccepted() {
        ClientConfig config = new ClientConfig(directory);
        config.setGuiPage("NOPE");
        config.setGuiCategory("Render");
        config.setGuiSelectedModule("Kill Aura");
        config.setGuiExpandedModule("../escape");
        assertEquals("MODULES", config.guiPage());
        assertEquals("All", config.guiCategory());
        assertEquals("", config.guiSelectedModule());
        assertEquals("", config.guiExpandedModule());
        assertFalse(config.guiEnabledOnly());
    }

    /** Registration ids for one category (or all), rotated so the persisted order is not the default one. */
    private static List<String> rotatedIds(ModuleManager modules, ModuleCategory only, int shift) {
        List<String> ids = new ArrayList<>();
        for (ClientModule module : modules.all()) {
            if (only == null || module.category() == only) ids.add(module.id());
        }
        assertFalse(ids.isEmpty(), "the registry must expose modules to order");
        Collections.rotate(ids, shift % ids.size());
        return List.copyOf(ids);
    }
}
