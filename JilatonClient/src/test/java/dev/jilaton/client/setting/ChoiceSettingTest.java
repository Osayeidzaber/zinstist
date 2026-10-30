package dev.jilaton.client.setting;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChoiceSettingTest {
    @Test
    void strictSettingsStillRejectUnknownValues() {
        ChoiceSetting setting = new ChoiceSetting("mode", "Mode", List.of("A", "B"), "A");
        assertThrows(IllegalArgumentException.class, () -> setting.setValue("C"));
        assertEquals("A", setting.value());
    }

    @Test
    void aDeferredValueIsKeptForSavingAndSelectedOnceOffered() {
        ChoiceSetting setting = new ChoiceSetting("character", "Character", List.of("Sakura", "Luna"), "Sakura")
                .deferUnknownValues();
        setting.setValue("bacon-hair");
        assertEquals("Sakura", setting.value());
        assertEquals("bacon-hair", setting.persistedValue());

        setting.setChoices(List.of("Sakura", "Luna", "bacon-hair"));
        assertEquals("bacon-hair", setting.value());
        assertEquals("bacon-hair", setting.persistedValue());
        assertEquals(List.of("Sakura", "Luna", "bacon-hair"), setting.choices());
    }

    @Test
    void refreshingChoicesKeepsASurvivingValueAndFallsBackWhenItDisappears() {
        ChoiceSetting setting = new ChoiceSetting("character", "Character", List.of("Sakura", "Luna", "custom"), "Sakura")
                .deferUnknownValues();
        setting.setValue("Luna");
        setting.setChoices(List.of("Sakura", "Luna"));
        assertEquals("Luna", setting.value());

        setting.setValue("custom");
        setting.setChoices(List.of("Sakura"));
        assertEquals("Sakura", setting.value());
    }

    @Test
    void choosingAnotherValueDropsTheDeferredOne() {
        ChoiceSetting setting = new ChoiceSetting("character", "Character", List.of("Sakura", "Luna"), "Sakura")
                .deferUnknownValues();
        setting.setValue("gone");
        setting.cycle(1);
        assertEquals("Luna", setting.value());
        assertEquals("Luna", setting.persistedValue());
    }
}
