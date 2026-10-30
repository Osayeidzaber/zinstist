package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.MovementInputEvent;
import dev.jilaton.client.module.PlayerMoveEvent;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;

public final class StrafeModule extends ClientModule {
    private final NumberSetting strengthInAir;
    private final NumberSetting strengthOnGround;
    private final BooleanSetting strictMovement;
    private double forward;
    private double sideways;

    public StrafeModule() {
        super("strafe", "Strafe", "Aligns horizontal movement to directional input while retaining its speed.",
                ModuleCategory.MOVEMENT);
        strengthInAir = addSetting(new NumberSetting("strength_in_air", "Strength in air", 0.0, 1.0, 0.05, 1.0));
        strengthOnGround = addSetting(new NumberSetting("strength_on_ground", "Strength on ground", 0.0, 1.0, 0.05, 1.0));
        strictMovement = addSetting(new BooleanSetting("strict_movement", "Stop without movement input", false));
    }

    @Override
    protected void onEnable(Minecraft client) {
        clearInput();
    }

    @Override
    protected void onDisable(Minecraft client) {
        clearInput();
    }

    @Override
    protected void onMovementInput(MovementInputEvent event) {
        Input input = event.input();
        Vec2 override = event.moveVectorOverride();
        if (override != null) {
            sideways = override.x;
            forward = override.y;
        } else {
            forward = (input.forward() ? 1.0 : 0.0) - (input.backward() ? 1.0 : 0.0);
            sideways = (input.left() ? 1.0 : 0.0) - (input.right() ? 1.0 : 0.0);
        }
    }

    @Override
    protected void onPlayerMove(PlayerMoveEvent event) {
        if (event.type() != MoverType.SELF) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.level == null || player.isPassenger() || player.isFallFlying()
                || player.getAbilities().flying || player.isSpectator() || player.isInWater() || player.isInLava()) {
            return;
        }

        double strength = player.onGround() ? strengthOnGround.value() : strengthInAir.value();
        event.setMovement(StrafeMath.apply(event.movement(), player.getYRot(), forward, sideways,
                strength, strictMovement.value()));
    }

    private void clearInput() {
        forward = 0.0;
        sideways = 0.0;
    }

    public NumberSetting strengthInAir() {
        return strengthInAir;
    }

    public NumberSetting strengthOnGround() {
        return strengthOnGround;
    }

    public BooleanSetting strictMovement() {
        return strictMovement;
    }
}
