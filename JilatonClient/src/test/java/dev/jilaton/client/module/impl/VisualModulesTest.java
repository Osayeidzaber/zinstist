package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.render.fx.FxEmitter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VisualModulesTest {
    @Test
    void presetsAreDistinctCombinations() {
        var minimal = HitEffectsModule.presetStyles("Minimal");
        var splash = HitEffectsModule.presetStyles("Splash");
        assertNotNull(minimal);
        assertTrue(splash.contains(HitEffectsModule.Style.INK));
        assertNotEquals(minimal, splash);
        assertNull(HitEffectsModule.presetStyles("Custom"));
        assertTrue(HitEffectsModule.presetStyles("Impact").contains(HitEffectsModule.Style.DOME));
    }

    @Test
    void newVisualModulesRegisterAsEmitters() {
        ModuleManager manager = new ModuleManager();
        for (String id : List.of("hit_effects", "player_effects", "ambience", "custom_sky", "spatial_lyrics", "music_visuals", "explosive_highlights"))
            assertInstanceOf(FxEmitter.class, manager.get(id), id);
        assertNotNull(manager.get("visual_engine"));
        assertNotNull(manager.get("music_center"));
        assertNotNull(manager.get("now_playing"));
    }
}
