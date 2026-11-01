package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;

/** Presses Respawn for you after a short delay, and can print where you died first. */
public final class AutoRespawnModule extends ClientModule {
    private final NumberSetting delay = addSetting(new NumberSetting("delay", "Delay (ticks)", 0, 60, 1, 10));
    private final BooleanSetting coords = addSetting(new BooleanSetting("coords", "Print death coordinates", true));
    private int ticks = -1;

    public AutoRespawnModule() {
        super("auto_respawn", "AutoRespawn", "Respawns automatically and notes where you died.", ModuleCategory.PLAYER);
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || !(client.screen instanceof DeathScreen)) {
            ticks = -1;
            return;
        }
        if (ticks < 0) {
            ticks = 0;
            if (coords.value()) {
                var p = client.player.blockPosition();
                client.player.displayClientMessage(Component.literal("[Jilaton] You died at " + p.getX() + " " + p.getY() + " " + p.getZ()), false);
            }
        }
        if (++ticks >= delay.value() && client.player.isDeadOrDying()) {
            client.player.respawn();
            client.setScreen(null);
            ticks = -1;
        }
    }
}
