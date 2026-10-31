package dev.jilaton.client.gui.shell;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.Setting;

/**
 * The live number printed on a Visuals card: how many of the module's boolean switches are on, out of
 * how many switches it has. Sliders and choices are not on/off state, so they never enter the ratio;
 * the "SW" unit is part of the label so the number can be read without consulting the source.
 */
public final class CardMetric {
    private CardMetric() { }

    /** Boolean switches owned by the module: the denominator, in the same unit as {@link #on}. */
    public static int total(ClientModule module) {
        int switches = 0;
        for (Setting setting : module.settings()) {
            if (setting instanceof BooleanSetting) switches++;
        }
        return switches;
    }

    /** Boolean switches currently on. Choices and sliders do not count as on/off state. */
    public static int on(ClientModule module) {
        int on = 0;
        for (Setting setting : module.settings()) {
            if (setting instanceof BooleanSetting flag && flag.value()) on++;
        }
        return on;
    }

    public static String label(ClientModule module) {
        if (module.settings().isEmpty()) return "NO SETTINGS";
        int total = total(module);
        return total == 0 ? "0 SWITCHES" : on(module) + "/" + total + " SW ON";
    }
}
