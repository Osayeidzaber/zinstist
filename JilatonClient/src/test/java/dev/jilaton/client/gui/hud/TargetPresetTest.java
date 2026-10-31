package dev.jilaton.client.gui.hud;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.module.impl.HudModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Presets are shortcuts over ordinary settings: the label is derived from the shape that is on screen. */
class TargetPresetTest {
    @TempDir
    Path directory;

    @Test
    void presetsSetScaleWidthAndVisibleElements() {
        HudModule hud = hud();
        hud.selectTargetPreset("Compact");
        assertEquals("Compact", hud.targetPreset());
        assertEquals(0.85, hud.targetScale(), 1e-9);
        assertEquals(232, hud.targetWidth());
        assertFalse(hud.targetChips());
        assertFalse(hud.targetHeld());
        assertTrue(hud.targetDistance());

        hud.selectTargetPreset("Wide");
        assertEquals("Wide", hud.targetPreset());
        assertEquals(1.0, hud.targetScale(), 1e-9);
        assertEquals(320, hud.targetWidth());
        assertTrue(hud.targetChips());
        assertTrue(hud.targetHeld());

        hud.selectTargetPreset("Classic");
        assertEquals(0, hud.targetWidth(), "Classic sizes itself from the viewport");
        assertTrue(hud.targetChips() && hud.targetHeld() && hud.targetDistance());
    }

    @Test
    void editedShapeIsReportedAsCustomInsteadOfADeadPresetLabel() {
        HudModule hud = hud();
        assertEquals("Classic", hud.targetPreset());
        hud.selectTargetPreset("Compact");
        ((dev.jilaton.client.setting.NumberSetting) setting(hud, "target_scale")).setValue(1.25);
        assertEquals("Custom", hud.targetPreset());
        hud.selectTargetPreset("Wide");
        assertEquals("Wide", hud.targetPreset());
        assertEquals("Custom", hud.preset(), "shipped defaults already turn on four Full widgets");
        hud.selectPreset("Minimal");
        assertEquals("Minimal", hud.preset());
        hud.selectPreset("Full");
        assertEquals("Full", hud.preset());
        ((dev.jilaton.client.setting.BooleanSetting) setting(hud, "radar")).setValue(false);
        assertEquals("Custom", hud.preset());
    }

    @Test
    void everyPresetIsReachableAndUnknownNamesFallBackToClassic() {
        assertEquals(3, TargetPreset.CHOICES.size());
        assertNotNull(TargetPreset.from("Wide"));
        assertEquals(TargetPreset.CLASSIC, TargetPreset.from("not a preset"));
        assertEquals(TargetPreset.CLASSIC, TargetPreset.from(null));
        for (String label : TargetPreset.CHOICES) {
            assertEquals(label, TargetPreset.from(label).label());
            assertTrue(TargetPreset.from(label).scale() > 0);
        }
    }

    @Test
    void targetSettingsSurviveAConfigSaveAndLoad() {
        ModuleManager modules = new ModuleManager();
        HudModule hud = (HudModule) modules.get("hud");
        hud.selectTargetPreset("Wide");
        hud.selectPreset("Full");

        ClientConfig config = new ClientConfig(directory);
        config.save(modules);

        ModuleManager restoredModules = new ModuleManager();
        new ClientConfig(directory).load(restoredModules);
        HudModule restored = (HudModule) restoredModules.get("hud");

        assertEquals("Wide", restored.targetPreset());
        assertEquals(1.0, restored.targetScale(), 1e-9);
        assertEquals(320, restored.targetWidth());
        assertTrue(restored.targetChips());
        assertTrue(restored.targetHeld());
        assertTrue(restored.targetDistance());
        assertEquals("Full", restored.preset());
    }

    private static dev.jilaton.client.setting.Setting setting(HudModule hud, String id) {
        return hud.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static HudModule hud() {
        return (HudModule) new ModuleManager().get("hud");
    }
}
