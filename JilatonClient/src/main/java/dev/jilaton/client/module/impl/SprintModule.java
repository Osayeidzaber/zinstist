package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

public final class SprintModule extends ClientModule {
    private final BooleanSetting forwardOnly;
    private final BooleanSetting requireFood;
    private final BooleanSetting stopAtCollision;
    private final BooleanSetting stopWhileSneaking;
    private final BooleanSetting stopWhileUsingItem;

    public SprintModule() {
        super("sprint", "Sprint", "Automatically sprint while moving.", ModuleCategory.MOVEMENT);
        forwardOnly = addSetting(new BooleanSetting("forward_only", "Forward input only", true));
        requireFood = addSetting(new BooleanSetting("require_food", "Require food", true));
        stopAtCollision = addSetting(new BooleanSetting("stop_at_collision", "Stop at collision", true));
        stopWhileSneaking = addSetting(new BooleanSetting("stop_while_sneaking", "Stop while sneaking", true));
        stopWhileUsingItem = addSetting(new BooleanSetting("stop_while_using_item", "Stop while using item", true));
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.options == null) {
            return;
        }

        ClientModule tapModule = JilatonClient.moduleManager().get("wtap");
        if (tapModule instanceof WTapModule wTap && wTap.enabled() && wTap.isTapActive(client)) {
            client.player.setSprinting(false);
            return;
        }

        boolean enoughFood = client.player.getFoodData().getFoodLevel() > 6;
        boolean shouldSprint = SprintPolicy.shouldSprint(forwardOnly.value(),
                client.options.keyUp.isDown(), client.options.keyDown.isDown(),
                client.options.keyLeft.isDown(), client.options.keyRight.isDown(),
                requireFood.value(), enoughFood, stopAtCollision.value(), client.player.horizontalCollision,
                stopWhileSneaking.value(), client.player.isCrouching(),
                stopWhileUsingItem.value(), client.player.isUsingItem());
        if (shouldSprint) {
            client.player.setSprinting(true);
        }
    }
}
