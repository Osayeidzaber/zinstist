package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Replaces the local display of the player's own name. Display-only: the server, chat packets
 * and other players always see the real name.
 */
public final class NickHiderModule extends ClientModule {
    private final TextSetting replacement;

    public NickHiderModule() {
        super("nick_hider", "Nick Hider",
                "Replaces your own name on this client only - chat, tab list, nametags and book pages. "
                        + "Servers and other players still see your real name.",
                ModuleCategory.CLIENT);
        replacement = addSetting(new TextSetting("replacement", "Replacement name", 32, "You",
                NickHiderModule::sanitize));
    }

    public String replacement() {
        return replacement.value();
    }

    public static NickHiderModule active() {
        return JilatonClient.moduleManager().get("nick_hider") instanceof NickHiderModule module
                && module.enabled() ? module : null;
    }

    public static String localName(Minecraft client) {
        if (client.player != null) {
            return client.player.getGameProfile().name();
        }
        return client.getUser().getName();
    }

    public Component displayName(Component name, String realName) {
        return NickHiderPolicy.replace(name, realName, replacement());
    }

    private static String sanitize(String value) {
        String trimmed = value == null ? "" : value.strip();
        StringBuilder safe = new StringBuilder(trimmed.length());
        for (int index = 0; index < trimmed.length(); index++) {
            char character = trimmed.charAt(index);
            if (!Character.isISOControl(character)) {
                safe.append(character);
            }
        }
        return safe.toString();
    }
}
