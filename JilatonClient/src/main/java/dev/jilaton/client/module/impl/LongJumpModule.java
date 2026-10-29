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
import net.minecraft.world.phys.Vec3;

public final class LongJumpModule extends ClientModule {
    private final NumberSetting horizontalBoost;
    private final BooleanSetting autoJump;
    private final BooleanSetting autoDisable;
    private boolean movingInput;
    private boolean pendingBoost;
    private boolean jumpActive;
    private boolean leftGround;
    private boolean boosted;

    public LongJumpModule() {
        super("long_jump", "LongJump", "Applies a one-shot horizontal boost after a vanilla jump.",
                ModuleCategory.MOVEMENT);
        horizontalBoost = addSetting(new NumberSetting("horizontal_boost", "Horizontal boost", 1.0, 10.0, 0.05, 4.25));
        autoJump = addSetting(new BooleanSetting("auto_jump", "Auto jump while moving", false));
        autoDisable = addSetting(new BooleanSetting("auto_disable", "Disable after landing", false));
    }

    @Override
    protected void onEnable(Minecraft client) {
        clearState();
    }

    @Override
    protected void onDisable(Minecraft client) {
        clearState();
    }

    @Override
    protected void onMovementInput(MovementInputEvent event) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.level == null) {
            clearState();
            return;
        }

        Input input = event.input();
        Vec2 override = event.moveVectorOverride();
        movingInput = input.forward() || input.backward() || input.left() || input.right()
                || override != null && override.lengthSquared() > 1.0E-6F;

        if (client.screen != null || jumpActive || !autoJump.value() || !movingInput || !player.onGround() || input.jump()
                || player.isPassenger() || player.isFallFlying() || player.getAbilities().flying
                || player.isSpectator() || player.isInWater() || player.isInLava()) {
            return;
        }
        player.jumpFromGround();
    }

    @Override
    protected void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null || player.isPassenger() || player.isFallFlying()
                || player.getAbilities().flying || player.isSpectator() || player.isInWater() || player.isInLava()) {
            clearState();
            return;
        }

        if (pendingBoost) {
            Vec3 velocity = LongJumpBoostPolicy.apply(player.getDeltaMovement(), horizontalBoost.value());
            player.setDeltaMovement(velocity);
            pendingBoost = false;
            boosted = true;
        }

        if (jumpActive && !player.onGround()) {
            leftGround = true;
        } else if (jumpActive && leftGround && player.onGround()) {
            if (autoDisable.value() && boosted) {
                setEnabled(false);
            } else {
                clearState();
            }
        }
    }

    @Override
    protected void onPlayerMove(PlayerMoveEvent event) {
        if (event.type() != MoverType.SELF || !jumpActive || movingInput) {
            return;
        }
        Vec3 movement = event.movement();
        event.setMovement(new Vec3(0.0, movement.y, 0.0));
    }

    public void onSuccessfulLocalJump(Player player) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled() || player == null || player != client.player || client.level == null
                || player.getDeltaMovement().y <= 1.0E-5 || player.isPassenger()
                || player.isFallFlying() || player.getAbilities().flying || player.isSpectator()
                || player.isInWater() || player.isInLava()) {
            return;
        }
        jumpActive = true;
        pendingBoost = true;
        leftGround = false;
        boosted = false;
    }

    private void clearState() {
        movingInput = false;
        pendingBoost = false;
        jumpActive = false;
        leftGround = false;
        boosted = false;
    }

    public NumberSetting horizontalBoost() {
        return horizontalBoost;
    }

    public BooleanSetting autoJump() {
        return autoJump;
    }

    public BooleanSetting autoDisable() {
        return autoDisable;
    }
}
