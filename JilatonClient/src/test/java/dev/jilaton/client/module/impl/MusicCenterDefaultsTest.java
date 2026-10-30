package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MusicCenterDefaultsTest {
    @Test
    void aFreshInstallStartsNoBridgeAndSendsNothingUntilASourceIsChosen() {
        ChoiceSetting source = null;
        for (Setting setting : new ModuleManager().get("music_center").settings()) {
            if (setting.id().equals("source") && setting instanceof ChoiceSetting choice) source = choice;
        }
        assertEquals("Off", source == null ? null : source.value());
    }
}
