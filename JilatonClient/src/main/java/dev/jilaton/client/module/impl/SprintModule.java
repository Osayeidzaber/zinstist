package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

public final class SprintModule extends ClientModule {
    private final BooleanSetting forwardOnly;

    public SprintModule() {
        super("sprint", "Sprint", "Automatically sprint while moving.", ModuleCategory.MOVEMENT);
        forwardOnly = addSetting(new BooleanSetting("forward_only", "Forward input only", true));
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.options == null) {
            return;
        }

        boolean moving = forwardOnly.value()
                ? client.options.keyUp.isDown()
                : client.options.keyUp.isDown() || client.options.keyDown.isDown()
                    || client.options.keyLeft.isDown() || client.options.keyRight.isDown();
        if (moving && client.player.getFoodData().getFoodLevel() > 6 && !client.player.horizontalCollision) {
            client.player.setSprinting(true);
        }
    }
}
