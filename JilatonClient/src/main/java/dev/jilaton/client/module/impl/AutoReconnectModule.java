package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

/**
 * Adds a "Reconnect" button with a countdown to the disconnect screen and rejoins the last multiplayer server
 * when it reaches zero. Clicking the button rejoins now; turning the module off leaves only the manual button.
 */
public final class AutoReconnectModule extends ClientModule {
    private final NumberSetting delay = addSetting(new NumberSetting("delay", "Delay (seconds)", 1, 60, 1, 5));
    private static ServerData lastServer;

    public AutoReconnectModule() {
        super("auto_reconnect", "AutoReconnect", "Rejoins the server after a disconnect, with a countdown you can skip.",
                ModuleCategory.CLIENT);
    }

    @Override
    public boolean enabledByDefault() {
        return false;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.getCurrentServer() != null && client.getSingleplayerServer() == null) lastServer = client.getCurrentServer();
    }

    /** Hooks the disconnect screen once at startup; the button works even with the module off. */
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof DisconnectedScreen) || lastServer == null) return;
            ServerData server = lastServer;
            AutoReconnectModule module = JilatonClient.moduleManager() != null
                    && JilatonClient.moduleManager().get("auto_reconnect") instanceof AutoReconnectModule m && m.enabled() ? m : null;
            long deadline = module == null ? Long.MAX_VALUE : System.nanoTime() + (long) (module.delay.value() * 1e9);
            Button button = Button.builder(label(deadline), b -> reconnect(client, server))
                    .bounds(width / 2 - 100, height - 30, 200, 20).build();
            Screens.getButtons(screen).add(button);
            ScreenEvents.afterTick(screen).register(current -> {
                button.setMessage(label(deadline));
                if (System.nanoTime() >= deadline && client.screen == current) reconnect(client, server);
            });
        });
    }

    private static Component label(long deadline) {
        if (deadline == Long.MAX_VALUE) return Component.literal("Reconnect");
        long left = Math.max(0, (deadline - System.nanoTime() + 999_999_999L) / 1_000_000_000L);
        return Component.literal("Reconnect in " + left + "s");
    }

    private static void reconnect(Minecraft client, ServerData server) {
        ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), client,
                ServerAddress.parseString(server.ip), server, false, null);
    }
}
