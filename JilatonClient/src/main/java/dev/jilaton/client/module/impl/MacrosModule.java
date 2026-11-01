package dev.jilaton.client.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * Up to five key macros. Each one sends a chat message, or a command when the text starts with "/". Several
 * lines can be chained with " && ". Keys are named like the controls screen (G, F6, KP_1, LEFT_ALT...).
 */
public final class MacrosModule extends ClientModule {
    private static final int COUNT = 5;

    private final TextSetting[] keys = new TextSetting[COUNT];
    private final TextSetting[] texts = new TextSetting[COUNT];
    private final boolean[] down = new boolean[COUNT];

    public MacrosModule() {
        super("macros", "Macros", "Bind keys to chat messages or commands; chain lines with &&.", ModuleCategory.CLIENT);
        for (int i = 0; i < COUNT; i++) {
            keys[i] = addSetting(new TextSetting("key_" + (i + 1), "Macro " + (i + 1) + " key", 16, "",
                    value -> value.strip().toUpperCase(Locale.ROOT)));
            texts[i] = addSetting(new TextSetting("text_" + (i + 1), "Macro " + (i + 1) + " text", 256, "", String::strip));
        }
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.getConnection() == null) return;
        for (int i = 0; i < COUNT; i++) {
            int code = keyCode(keys[i].value());
            boolean pressed = code >= 0 && client.screen == null
                    && InputConstants.isKeyDown(client.getWindow(), code);
            if (pressed && !down[i] && !texts[i].value().isEmpty()) run(client, texts[i].value());
            down[i] = pressed;
        }
    }

    private static void run(Minecraft client, String text) {
        for (String line : MacroText.lines(text)) {
            if (line.startsWith("/")) client.getConnection().sendCommand(line.substring(1));
            else client.getConnection().sendChat(line);
        }
    }

    /** GLFW key code for a name like "G", "F6" or "KP_1"; -1 if unknown or empty. */
    static int keyCode(String name) {
        if (name == null || name.isEmpty()) return -1;
        try {
            InputConstants.Key key = InputConstants.getKey("key.keyboard." + name.toLowerCase(Locale.ROOT).replace('_', '.'));
            return key == InputConstants.UNKNOWN ? -1 : key.getValue();
        } catch (IllegalArgumentException unknown) {
            return -1;
        }
    }
}
