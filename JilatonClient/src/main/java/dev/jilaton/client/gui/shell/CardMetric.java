package dev.jilaton.client.gui.shell;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.Setting;

/**
 * The live number printed on a Visuals card. It is read from the module on every frame: how many of
 * its boolean switches are on, out of how many settings it owns. A card with a placeholder string
 * would fail the direction's "live data" rule, so this stays a pure function of the module.
 */
public final class CardMetric {
    private CardMetric() { }

    public static int total(ClientModule module) {
        return module.settings().size();
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
        int total = total(module);
        return total == 0 ? "NO SETTINGS" : on(module) + "/" + total + " ON";
    }
}
