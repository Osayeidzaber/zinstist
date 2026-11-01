/*
 * Adapted generic velocity modification and strafe behavior from LiquidBounce.
 * Copyright (c) 2015 - 2026 CCBlueX. LiquidBounce is licensed under GPL-3.0-or-later.
 * Source: https://github.com/CCBlueX/LiquidBounce/tree/e033f5aaf05be802dd2ca51782f255119d7d5654
 */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.MovementInputEvent;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class VelocityModule extends ClientModule {
    private final NumberSetting horizontal;
    private final NumberSetting horizontalRandomMin;
    private final NumberSetting horizontalRandomMax;
    private final NumberSetting vertical;
    private final NumberSetting motionHorizontal;
    private final NumberSetting motionVertical;
    private final NumberSetting chance;
    private final ChoiceSetting groundFilter;
    private final BooleanSetting onlyMove;
    private final ChoiceSetting responseMode;
    private final NumberSetting strafeDelay;
    private final NumberSetting strafeStrength;
    private final BooleanSetting untilGround;
    private final BooleanSetting strafeOnlyFacing;
    private final NumberSetting strafeFacingRange;
    private final BooleanSetting jumpReset;
    private final NumberSetting jumpChance;
    private final ChoiceSetting jumpCooldownMode;
    private final NumberSetting jumpCooldown;
    private final NumberSetting reverseDelay;
    private final NumberSetting reverseX;
    private final NumberSetting reverseZ;
    private final BooleanSetting reverseOnlyMoving;
    private int pendingStrafeTicks = -1;
    private int pendingReversalTicks = -1;
    private boolean strafeUntilGround;
    private boolean fallDamageVelocity;
    private int jumpResetProgress;
    private LocalPlayer lastPlayer;
    private Level lastLevel;

    public VelocityModule() {
        super("velocity", "Velocity", "Modifies knockback with Modify, Strafe, or Reversal; JumpReset is optional.", ModuleCategory.COMBAT);
        horizontal = addSetting(new NumberSetting("horizontal", "Modify horizontal", -100.0, 100.0, 5.0, 0.0));
        horizontalRandomMin = addSetting(new NumberSetting("horizontal_random_min", "Random horizontal min",
                -100.0, 100.0, 5.0, 0.0));
        horizontalRandomMax = addSetting(new NumberSetting("horizontal_random_max", "Random horizontal max",
                -100.0, 100.0, 5.0, 0.0));
        vertical = addSetting(new NumberSetting("vertical", "Modify vertical", -100.0, 100.0, 5.0, 0.0));
        motionHorizontal = addSetting(new NumberSetting("motion_horizontal", "Modify motion horizontal", 0.0, 100.0, 5.0, 0.0));
        motionVertical = addSetting(new NumberSetting("motion_vertical", "Modify motion vertical", 0.0, 100.0, 5.0, 0.0));
        chance = addSetting(new NumberSetting("chance", "Modify chance", 0.0, 100.0, 1.0, 100.0));
        groundFilter = addSetting(new ChoiceSetting("ground_filter", "Modify ground filter",
                List.of("Any", "On ground", "In air"), "Any"));
        onlyMove = addSetting(new BooleanSetting("only_move", "Modify only while moving", false));
        responseMode = addSetting(new ChoiceSetting("response_mode", "Velocity response",
                List.of("Modify", "Strafe", "Reversal"), "Modify"));
        strafeDelay = addSetting(new NumberSetting("strafe_delay", "Strafe delay", 0.0, 10.0, 1.0, 2.0));
        strafeStrength = addSetting(new NumberSetting("strafe_strength", "Strafe strength", 10.0, 200.0, 5.0, 100.0));
        untilGround = addSetting(new BooleanSetting("until_ground", "Continue until grounded", false));
        strafeOnlyFacing = addSetting(new BooleanSetting("strafe_only_facing", "Strafe only while facing a target", false));
        strafeFacingRange = addSetting(new NumberSetting("strafe_facing_range", "Facing target range", 0.1, 6.0, 0.1, 3.5));
        jumpReset = addSetting(new BooleanSetting("jump_reset", "Jump reset", false));
        jumpChance = addSetting(new NumberSetting("jump_chance", "Jump chance", 0.0, 100.0, 1.0, 100.0));
        jumpCooldownMode = addSetting(new ChoiceSetting("jump_cooldown_mode", "Jump cooldown mode",
                List.of("Delay", "Received hits"), "Delay"));
        jumpCooldown = addSetting(new NumberSetting("jump_cooldown", "Reset after", 0.0, 20.0, 1.0, 2.0));
        reverseDelay = addSetting(new NumberSetting("reverse_delay", "Reversal delay", 1.0, 5.0, 1.0, 2.0));
        reverseX = addSetting(new NumberSetting("reverse_x", "X modifier", 0.0, 100.0, 5.0, 50.0));
        reverseZ = addSetting(new NumberSetting("reverse_z", "Z modifier", 0.0, 100.0, 5.0, 50.0));
        reverseOnlyMoving = addSetting(new BooleanSetting("reverse_only_moving", "Reverse only while moving", false));
    }

    /**
     * Fixed horizontal scale, or one sample from the configured range when the two bounds differ.
     * The range arithmetic is original Jilaton code (see {@link VelocityRange}); a min/max pair set
     * equal leaves the fixed setting in charge, so existing configurations keep their behaviour.
     */
    public double horizontalScale() {
        double minimum = horizontalRandomMin.value();
        double maximum = horizontalRandomMax.value();
        if (VelocityRange.isRandom(minimum, maximum)) {
            return VelocityRange.sample(minimum, maximum, ThreadLocalRandom.current().nextDouble()) / 100.0;
        }
        return horizontal.value() / 100.0;
    }

    public double verticalScale() {
        return vertical.value() / 100.0;
    }

    public double motionHorizontalScale() {
        return motionHorizontal.value() / 100.0;
    }

    public double motionVerticalScale() {
        return motionVertical.value() / 100.0;
    }

    public int chancePercent() {
        return (int) chance.value();
    }

    public String groundFilter() {
        return groundFilter.value();
    }

    public boolean onlyMove() {
        return onlyMove.value();
    }

    public String responseMode() {
        return responseMode.value();
    }

    public void onAcceptedVelocityPacket() {
        if (responseMode.value().equals("Strafe")) {
            pendingStrafeTicks = (int) strafeDelay.value();
        }
    }

    public void onVelocityPacketReceived(Vec3 movement, boolean moving) {
        fallDamageVelocity = movement.x == 0.0 && movement.z == 0.0 && movement.y < 0.0;
        if (responseMode.value().equals("Reversal")
                && VelocityReversalPolicy.shouldSchedule(reverseOnlyMoving.value(), moving)) {
            pendingReversalTicks = (int) reverseDelay.value();
            clearStrafeState();
        }
    }

    @Override
    protected void onTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            clearStrafeState();
            clearReversalState();
            clearJumpResetState();
            lastPlayer = null;
            lastLevel = null;
            return;
        }
        if (lastPlayer != player || lastLevel != client.level) {
            clearStrafeState();
            clearReversalState();
            clearJumpResetState();
            lastPlayer = player;
            lastLevel = client.level;
        }
        if (!responseMode.value().equals("Strafe")) {
            clearStrafeState();
        }
        if (!responseMode.value().equals("Reversal")) {
            pendingReversalTicks = -1;
        }

        if (pendingReversalTicks >= 0) {
            if (pendingReversalTicks > 0) {
                pendingReversalTicks--;
            }
            if (pendingReversalTicks == 0) {
                applyReversal(player);
                pendingReversalTicks = -1;
            }
        }

        if (responseMode.value().equals("Strafe") && pendingStrafeTicks >= 0) {
            if (pendingStrafeTicks > 0) {
                pendingStrafeTicks--;
            }
            if (pendingStrafeTicks == 0) {
                if (shouldApplyStrafe(client, player)) {
                    applyStrafe(player);
                }
                pendingStrafeTicks = -1;
                strafeUntilGround = untilGround.value() && !player.onGround();
            }
        } else if (responseMode.value().equals("Strafe") && strafeUntilGround) {
            if (!untilGround.value() || player.onGround()) {
                strafeUntilGround = false;
            } else if (shouldApplyStrafe(client, player)) {
                applyStrafe(player);
            }
        }
    }

    @Override
    protected void onDisable(Minecraft client) {
        clearStrafeState();
        clearReversalState();
        clearJumpResetState();
        lastPlayer = null;
        lastLevel = null;
    }

    @Override
    protected void onMovementInput(MovementInputEvent event) {
        if (!jumpReset.value()) {
            jumpResetProgress = 0;
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            jumpResetProgress = 0;
            return;
        }

        boolean receivedHitMode = jumpCooldownMode.value().equals("Received hits");
        if (VelocityJumpResetPolicy.shouldJump(player.hurtTime, player.onGround(), player.isSprinting(),
                fallDamageVelocity, jumpResetProgress, (int) jumpCooldown.value(),
                ThreadLocalRandom.current().nextInt(100), (int) jumpChance.value())) {
            Input input = event.input();
            if (!input.jump()) {
                event.setInput(new Input(input.forward(), input.backward(), input.left(), input.right(),
                        true, input.shift(), input.sprint()));
            }
            jumpResetProgress = 0;
        } else if (VelocityJumpResetPolicy.shouldAdvanceCooldown(receivedHitMode, player.hurtTime)) {
            jumpResetProgress = Math.min(jumpResetProgress + 1, 1000);
        }
    }

    private void applyStrafe(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        Input input = player.input == null ? null : player.input.keyPresses;
        boolean forward = input != null && input.forward();
        boolean backward = input != null && input.backward();
        boolean left = input != null && input.left();
        boolean right = input != null && input.right();
        VelocityStrafeMath.Horizontal horizontal = VelocityStrafeMath.reorient(
                velocity.x, velocity.z, player.getYRot(), forward, backward, left, right,
                Math.hypot(velocity.x, velocity.z) * strafeStrength.value() / 100.0);
        player.setDeltaMovement(horizontal.x(), velocity.y, horizontal.z());
    }

    private boolean shouldApplyStrafe(Minecraft client, LocalPlayer player) {
        if (!strafeOnlyFacing.value() || client.level == null) {
            return true;
        }
        Vec3 origin = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F);
        double range = strafeFacingRange.value();
        for (Player candidate : client.level.players()) {
            if (candidate == player || !candidate.isAlive() || candidate.isSpectator()
                    || !PlayerTargetFilters.allowsPlayer(client, player, candidate)) {
                continue;
            }
            AABB targetBox = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            if (VelocityFacingPolicy.rayIntersectsTarget(origin, direction, range, targetBox)) {
                return true;
            }
        }
        return false;
    }

    private void applyReversal(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        VelocityReversalPolicy.Motion reversed = VelocityReversalPolicy.apply(velocity.x, velocity.y, velocity.z,
                reverseX.value() / 100.0, reverseZ.value() / 100.0);
        player.setDeltaMovement(reversed.x(), reversed.y(), reversed.z());
    }

    private void clearStrafeState() {
        pendingStrafeTicks = -1;
        strafeUntilGround = false;
    }

    private void clearReversalState() {
        pendingReversalTicks = -1;
    }

    private void clearJumpResetState() {
        fallDamageVelocity = false;
        jumpResetProgress = 0;
    }
}
