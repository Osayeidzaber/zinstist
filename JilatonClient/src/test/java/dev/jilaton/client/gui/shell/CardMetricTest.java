package dev.jilaton.client.gui.shell;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Visuals card metric counts switches, and says so: numerator and denominator are both boolean
 * switches, so a module with sliders or choices never reads as if some of its switches were off.
 */
class CardMetricTest {
    @Test void everySwitchOffReadsZeroOutOfTheSwitches() {
        Probe module = new Probe("all-off").flag(false).flag(false).slider().choice();
        assertEquals(2, CardMetric.total(module));
        assertEquals(0, CardMetric.on(module));
        assertEquals("0/2 SW ON", CardMetric.label(module));
    }

    @Test void oneSwitchOnReadsOneOutOfTheSwitches() {
        Probe module = new Probe("one-on").flag(true).flag(false).flag(false).slider();
        assertEquals(3, CardMetric.total(module));
        assertEquals(1, CardMetric.on(module));
        assertEquals("1/3 SW ON", CardMetric.label(module));
    }

    @Test void slidersAndChoicesAloneFallBackInsteadOfReadingZeroOfZero() {
        Probe module = new Probe("no-switches").slider().choice();
        assertEquals(0, CardMetric.total(module));
        assertEquals("0 SWITCHES", CardMetric.label(module));
    }

    @Test void aModuleWithoutSettingsSaysNoSettings() {
        Probe module = new Probe("empty");
        assertEquals("NO SETTINGS", CardMetric.label(module));
    }

    @Test void theLabelFollowsALiveToggle() {
        BooleanSetting flag = new BooleanSetting("glow", "Glow", false);
        Probe module = new Probe("toggle").with(flag);
        assertEquals("0/1 SW ON", CardMetric.label(module));
        flag.toggle();
        assertEquals("1/1 SW ON", CardMetric.label(module));
        flag.toggle();
        assertEquals("0/1 SW ON", CardMetric.label(module));
    }

    private static final class Probe extends ClientModule {
        Probe(String id) {
            super(id, id, "synthetic module", ModuleCategory.RENDER);
        }

        Probe with(BooleanSetting setting) {
            addSetting(setting);
            return this;
        }

        Probe flag(boolean value) {
            return with(new BooleanSetting("flag" + settings().size(), "Flag", value));
        }

        Probe slider() {
            addSetting(new NumberSetting("slider" + settings().size(), "Slider", 0, 100, 1, 50));
            return this;
        }

        Probe choice() {
            addSetting(new ChoiceSetting("choice" + settings().size(), "Choice", List.of("A", "B"), "A"));
            return this;
        }
    }
}
