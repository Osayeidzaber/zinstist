package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

/** Removes first-person view bobbing without changing Minecraft's saved video option. */
public final class NoBobModule extends ClientModule {
    private final BooleanSetting movingOnly = addSetting(new BooleanSetting(
            "moving_only", "Only while moving", false));

    public NoBobModule() {
        super("no_bob", "NoBob", "Removes first-person camera bobbing.", ModuleCategory.RENDER);
    }

    public static boolean shouldSuppress(Minecraft client) {
        ClientModule registered = JilatonClient.moduleManager().get("no_bob");
        if (!(registered instanceof NoBobModule module) || !module.enabled()) {
            return false;
        }
        if (!module.movingOnly.value()) {
            return true;
        }
        return client.player != null && client.player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4;
    }
}
