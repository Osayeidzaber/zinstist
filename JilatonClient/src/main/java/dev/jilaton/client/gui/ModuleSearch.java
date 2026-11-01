package dev.jilaton.client.gui;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.setting.Setting;

import java.util.Locale;

/**
 * ClickGUI search: a module matches when every word of the query appears in its name, id, description or the
 * label of one of its settings, so "esp color" finds the ESP modules that have a colour setting.
 */
public final class ModuleSearch {
    private ModuleSearch() {
    }

    /** {@code query} is already trimmed and lower-cased. */
    public static boolean matches(ClientModule module, String query) {
        StringBuilder text = new StringBuilder();
        text.append(module.name()).append(' ').append(module.id().replace('_', ' ')).append(' ').append(module.description());
        for (Setting setting : module.settings()) text.append(' ').append(setting.label());
        String haystack = text.toString().toLowerCase(Locale.ROOT);
        for (String word : query.split("\\s+")) {
            if (!word.isEmpty() && !haystack.contains(word)) return false;
        }
        return true;
    }
}
