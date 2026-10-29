package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.MovementInputEvent;
import dev.jilaton.client.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;

/** LiquidBounce-inspired grounded LegitHop mode using normal Minecraft jump input. */
public final class SpeedModule extends ClientModule {
    private final BooleanSetting requireSprint;
    private final BooleanSetting respectItemUse;
    private final BooleanSetting respectSneak;
    private final BooleanSetting respectFallFlying;
    private final BooleanSetting respectLiquids;

    public SpeedModule() {
        super("speed", "Speed", "Automatically hops while moving on the ground (LegitHop mode).",
                ModuleCategory.MOVEMENT);
        requireSprint = addSetting(new BooleanSetting("require_sprint", "Require sprinting", false));
        respectItemUse = addSetting(new BooleanSetting("respect_item_use", "Pause while using items", true));
        respectSneak = addSetting(new BooleanSetting("respect_sneak", "Pause while sneaking", true));
        respectFallFlying = addSetting(new BooleanSetting("respect_fall_flying", "Pause while fall-flying", true));
        respectLiquids = addSetting(new BooleanSetting("respect_liquids", "Pause in liquids", true));
    }

    @Override
    protected void onMovementInput(MovementInputEvent event) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.level == null) {
            return;
        }

        Input input = event.input();
        Vec2 movement = event.moveVectorOverride();
        boolean moving = input.forward() || input.backward() || input.left() || input.right()
                || movement != null && movement.lengthSquared() > 1.0E-6F;
        boolean useItem = respectItemUse.value() && player.isUsingItem();
        boolean sneaking = respectSneak.value() && input.shift();
        boolean fallFlying = respectFallFlying.value() && player.isFallFlying();
        boolean inLiquid = respectLiquids.value() && (player.isInWater() || player.isInLava());
        if (!SpeedPolicy.shouldHop(player.onGround(), moving, useItem, sneaking, fallFlying, inLiquid,
                client.screen != null, requireSprint.value(), player.isSprinting()) || input.jump()) {
            return;
        }

        event.setInput(new Input(input.forward(), input.backward(), input.left(), input.right(),
                true, input.shift(), input.sprint()));
    }

    public BooleanSetting requireSprint() {
        return requireSprint;
    }

    public BooleanSetting respectItemUse() {
        return respectItemUse;
    }

    public BooleanSetting respectSneak() {
        return respectSneak;
    }

    public BooleanSetting respectFallFlying() {
        return respectFallFlying;
    }

    public BooleanSetting respectLiquids() {
        return respectLiquids;
    }
}
