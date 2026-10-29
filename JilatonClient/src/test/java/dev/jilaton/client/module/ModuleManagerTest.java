package dev.jilaton.client.module;

import dev.jilaton.client.module.impl.FullbrightModule;
import dev.jilaton.client.module.impl.WTapModule;
import dev.jilaton.client.module.impl.SwingAnimationsModule;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
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

    @Test
    void fullbrightGammaSettingStaysWithinMinecraftUnitRange() {
        FullbrightModule fullbright = new FullbrightModule();
        NumberSetting brightness = fullbright.brightness();

        assertEquals(0.0, brightness.minimum());
        assertEquals(1.0, brightness.maximum());
        assertEquals(1.0, brightness.value());

        brightness.setValue(12.0);
        assertEquals(1.0, brightness.value());
    }

    @Test
    void wtapExposesBoundedReleaseAndPlayerFilterSettings() {
        WTapModule wtap = new WTapModule();

        assertEquals(1.0, wtap.releaseTicks().minimum());
        assertEquals(3.0, wtap.releaseTicks().maximum());
        assertEquals(1.0, wtap.releaseTicks().value());
        assertTrue(wtap.playersOnly().value());

        assertTrue(new ModuleManager().get("wtap") instanceof WTapModule);
    }

    @Test
    void swingAnimationsExposeLiveStyleAndScaleSettings() {
        SwingAnimationsModule module = new SwingAnimationsModule();
        ChoiceSetting mode = (ChoiceSetting) module.settings().stream()
                .filter(setting -> setting.id().equals("mode"))
                .findFirst().orElseThrow();
        assertEquals("Smooth", mode.value());
        mode.cycle(1);
        assertEquals("Power", mode.value());
        mode.setValue("Feast");
        assertEquals("Feast", mode.value());
        assertTrue(new ModuleManager().get("swing_animations") instanceof SwingAnimationsModule);
    }

    @Test
    void choiceSettingRejectsUnknownModesAndCyclesInBothDirections() {
        ChoiceSetting setting = new ChoiceSetting("mode", "Mode", java.util.List.of("A", "B", "C"), "A");
        setting.cycle(-1);
        assertEquals("C", setting.value());
        setting.cycle(1);
        assertEquals("A", setting.value());
        assertThrows(IllegalArgumentException.class, () -> setting.setValue("unknown"));
    }
}
