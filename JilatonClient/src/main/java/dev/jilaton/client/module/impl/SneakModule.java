package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.ChoiceSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/**
 * Sneaks for you. Vanilla presses the sneak key in the input; Packet only tells the server you sneak (movement
 * and interactions) while your client keeps walking upright. Ported from Anubis Client (SneakModule, GPL-3.0, Horcus and 4ldenz).
 */
public final class SneakModule extends ClientModule {
    public static volatile SneakModule active;
    private final ChoiceSetting mode = addSetting(new ChoiceSetting("mode", "Mode", List.of("Packet", "Vanilla"), "Vanilla"));

    public SneakModule() {
        super("sneak", "Sneak", "Sneaks for you.", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable(Minecraft client) {
        active = this;
    }

    @Override
    protected void onDisable(Minecraft client) {
        active = null;
    }

    public boolean doPacket() {
        return enabled() && mode.value().equals("Packet") && !isFlying();
    }

    public boolean doVanilla() {
        return enabled() && mode.value().equals("Vanilla") && !isFlying();
    }

    private static boolean isFlying() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null || player.getAbilities().flying;
    }
}
