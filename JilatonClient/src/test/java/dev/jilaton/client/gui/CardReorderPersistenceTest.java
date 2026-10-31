package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.ModuleManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shipping GUI's drop path: a reorder rewrites the persisted order and survives a reload. */
class CardReorderPersistenceTest {
    @TempDir
    Path directory;

    @Test void droppingACardPersistsItsNewOrderAndSurvivesAReload() {
        ModuleManager modules = new ModuleManager();
        ClientConfig config = new ClientConfig(directory);
        List<ClientModule> all = new ArrayList<>(modules.all());
        ClientModule moved = all.get(2);

        JilatonClickGui.applyReorder(config, modules, all, all, moved, 0, false);

        assertEquals(moved.id(), config.moduleOrder().get(0));
        assertEquals(all.size(), config.moduleOrder().size(), "every module must stay in the order");

        ClientConfig restored = new ClientConfig(directory);
        restored.load(new ModuleManager());
        assertEquals(moved.id(), restored.moduleOrder().get(0));
        assertEquals(all.size(), restored.moduleOrder().size());
    }

    @Test void aVisualsDropAlsoRewritesTheVisualOrderAndKeepsFilteredCards() {
        ModuleManager modules = new ModuleManager();
        ClientConfig config = new ClientConfig(directory);
        List<ClientModule> all = new ArrayList<>(modules.all());
        ClientModule render = all.stream().filter(m -> m.category() == ModuleCategory.RENDER).findFirst().orElseThrow();
        int renderCount = (int) all.stream().filter(m -> m.category() == ModuleCategory.RENDER).count();
        // The visible list is filtered: two cards only, as a search would show.
        List<ClientModule> visible = List.of(render, all.get(0));

        JilatonClickGui.applyReorder(config, modules, all, visible, render, 0, true);

        assertEquals(render.id(), config.moduleOrder().get(0));
        assertEquals(all.size(), config.moduleOrder().size(), "filtered-out modules must not be dropped");
        assertEquals(renderCount, config.visualOrder().size(), "the visual order must keep every render module");
        assertEquals(render.id(), config.visualOrder().get(0));

        ClientConfig restored = new ClientConfig(directory);
        restored.load(new ModuleManager());
        assertEquals(render.id(), restored.moduleOrder().get(0));
        assertEquals(render.id(), restored.visualOrder().get(0));
        assertTrue(restored.visualOrder().size() == renderCount);
    }

    @Test void theGripStaysClearOfTheSwitchAndTheCardBody() {
        float x = 400, y = 200, w = 300;
        for (float h : new float[]{62, 128}) {
            for (float px = x; px < x + w; px += 0.5F) {
                for (float py = y; py < y + h; py += 0.5F) {
                    if (JilatonClickGui.gripHit(px, py, x, y, h)) {
                        assertFalse(JilatonClickGui.toggleHit(px, py, x, y, w, h == 128),
                                "the grip must never swallow the switch at " + px + "," + py);
                    }
                }
            }
        }
    }
}
