package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.rotation.RotationAngles;
import dev.jilaton.client.rotation.RotationPolicy;
import dev.jilaton.client.rotation.RotationPriorities;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Follows a visible player while fall-flying, with optional movement lead and firework boost. */
public final class ElytraTargetModule extends ClientModule {
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting turnSpeed;
    private final BooleanSetting sharpRotations;
    private final BooleanSetting predictMovement;
    private final BooleanSetting glidingTargetsOnly;
    private final NumberSetting predictionMultiplier;
    private final BooleanSetting gravityPrediction;
    private final ChoiceSetting rotateAt;
    private final BooleanSetting safeFlight;
    private final BooleanSetting autoFirework;
    private final NumberSetting extraFireworkDistance;
    private final NumberSetting closeCooldown;
    private final NumberSetting farCooldown;

    private Player currentTarget;
    private long lastFireworkTick = Long.MIN_VALUE;
    private Player sessionPlayer;
    private Object sessionLevel;

    public ElytraTargetModule() {
        super("elytra_target", "ElytraTarget",
                "Tracks visible players while fall-flying and can use firework rockets to keep pace.",
                ModuleCategory.COMBAT);
        range = addSetting(new NumberSetting("range", "Target range", 3.0, 128.0, 1.0, 64.0));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view", 5.0, 180.0, 1.0, 120.0));
        turnSpeed = addSetting(new NumberSetting("turn_speed", "Turn speed", 1.0, 90.0, 1.0, 35.0));
        sharpRotations = addSetting(new BooleanSetting("sharp_rotations", "Sharp rotations", false));
        predictMovement = addSetting(new BooleanSetting("predict_movement", "Predict target movement", true));
        glidingTargetsOnly = addSetting(new BooleanSetting("gliding_targets_only", "Predict gliding targets only", true));
        predictionMultiplier = addSetting(new NumberSetting("prediction_multiplier", "Movement lead", 0.5, 3.0, 0.1, 1.9));
        gravityPrediction = addSetting(new BooleanSetting("gravity_prediction", "Apply gravity lead", false));
        rotateAt = addSetting(new ChoiceSetting("rotate_at", "Aim point", List.of("Eyes", "Center"), "Eyes"));
        safeFlight = addSetting(new BooleanSetting("safe_flight", "Avoid predicted collisions", true));
        autoFirework = addSetting(new BooleanSetting("auto_firework", "Use firework rockets", true));
        extraFireworkDistance = addSetting(new NumberSetting("extra_firework_distance", "Far-target cooldown threshold",
                5.0, 100.0, 1.0, 50.0));
        closeCooldown = addSetting(new NumberSetting("close_cooldown", "Near-target rocket cooldown (ticks)",
                1.0, 50.0, 1.0, 8.0));
        farCooldown = addSetting(new NumberSetting("far_cooldown", "Far-target rocket cooldown (ticks)",
                1.0, 50.0, 1.0, 10.0));
    }

    @Override
    protected void onEnable(Minecraft client) {
        resetSession();
    }

    @Override
    protected void onDisable(Minecraft client) {
        resetSession();
    }

    private void resetSession() {
        currentTarget = null;
        lastFireworkTick = Long.MIN_VALUE;
        sessionPlayer = null;
        sessionLevel = null;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) { resetSession(); return; }
        Player player = client.player;
        if (player != sessionPlayer || client.level != sessionLevel) {
            resetSession();
            sessionPlayer = player;
            sessionLevel = client.level;
        }
        if (client.screen != null || !client.isWindowActive() || !player.isAlive() || !player.isFallFlying()) {
            currentTarget = null;
            return;
        }

        if (safeFlight.value() && !client.level.noCollision(player,
                player.getBoundingBox().move(player.getDeltaMovement()))) {
            player.push(0.0, 0.1, 0.0);
        }

        currentTarget = selectTarget(client, player);
        if (currentTarget == null) {
            return;
        }

        Vec3 targetPosition = targetPosition(currentTarget);
        Vec3 lookVector = targetPosition.subtract(player.getEyePosition());
        if (lookVector.lengthSqr() > 1.0E-8) {
            RotationAngles rotation = RotationAngles.lookingAt(lookVector.x, lookVector.y, lookVector.z);
            float speed = (float) Math.min(90.0, turnSpeed.value() * (sharpRotations.value() ? 1.5 : 1.0));
            JilatonClient.moduleManager().rotationManager().request(id(), RotationPriorities.ELYTRA_TARGET,
                    rotation.yaw(), rotation.pitch(), speed);
        }

        if (autoFirework.value()) {
            tryUseFirework(client, player);
        }
    }

    public Player currentTarget() {
        return currentTarget;
    }

    private Player selectTarget(Minecraft client, Player player) {
        ClientModule killAura = JilatonClient.moduleManager().get("kill_aura");
        if (killAura instanceof KillAuraModule aura && aura.enabled()
                && aura.currentTarget() instanceof Player target && isEligible(client, player, target)) {
            return target;
        }
        if (isEligible(client, player, currentTarget)) {
            return currentTarget;
        }

        Player best = null;
        double bestAngle = Double.POSITIVE_INFINITY;
        double maxRangeSquared = range.value() * range.value();
        for (Player candidate : client.level.players()) {
            if (!isEligible(client, player, candidate)
                    || player.distanceToSqr(candidate) > maxRangeSquared) {
                continue;
            }
            double angle = angularDistance(player, targetPosition(candidate));
            if (angle < bestAngle) {
                best = candidate;
                bestAngle = angle;
            }
        }
        return best;
    }

    private boolean isEligible(Minecraft client, Player player, Player candidate) {
        if (candidate == null || candidate == player || candidate.level() != client.level || !candidate.isAlive() || candidate.isSpectator()
                || !PlayerTargetFilters.allowsPlayer(client, player, candidate)
                || !player.hasLineOfSight(candidate)) {
            return false;
        }
        double distanceSquared = player.distanceToSqr(candidate);
        double angularError = angularDistance(player, targetPosition(candidate));
        return ElytraTargetPolicy.isEligible(true, true, distanceSquared,
                range.value(), angularError, fieldOfView.value());
    }

    private Vec3 targetPosition(Player target) {
        Vec3 position = rotateAt.value().equals("Center")
                ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
                : target.getEyePosition();
        boolean shouldPredict = predictMovement.value()
                && (!glidingTargetsOnly.value() || target.isFallFlying());
        return ElytraTargetPolicy.predictPosition(position, target.getDeltaMovement(), shouldPredict,
                predictionMultiplier.value(), gravityPrediction.value());
    }

    private static double angularDistance(Player player, Vec3 target) {
        Vec3 look = target.subtract(player.getEyePosition());
        if (look.lengthSqr() < 1.0E-8) {
            return 0.0;
        }
        RotationAngles targetRotation = RotationAngles.lookingAt(look.x, look.y, look.z);
        return RotationPolicy.angularError(player.getYRot(), player.getXRot(), targetRotation);
    }

    private void tryUseFirework(Minecraft client, Player player) {
        if (client.gameMode == null || client.options.keyUse.isDown() || player.isUsingItem()) {
            return;
        }
        long now = client.level.getGameTime();
        double distanceSquared = player.distanceToSqr(currentTarget);
        double thresholdSquared = extraFireworkDistance.value() * extraFireworkDistance.value();
        long cooldown = (long) (distanceSquared > thresholdSquared
                ? farCooldown.value() : closeCooldown.value());
        if (lastFireworkTick != Long.MIN_VALUE && now - lastFireworkTick < cooldown) {
            return;
        }

        InteractionHand hand;
        int originalSlot = -1;
        int rocketSlot = -1;
        if (player.getOffhandItem().is(Items.FIREWORK_ROCKET)) {
            hand = InteractionHand.OFF_HAND;
        } else if (player.getMainHandItem().is(Items.FIREWORK_ROCKET)) {
            hand = InteractionHand.MAIN_HAND;
        } else {
            int selected = player.getInventory().getSelectedSlot();
            for (int slot = 0; slot < 9; slot++) {
                if (player.getInventory().getItem(slot).is(Items.FIREWORK_ROCKET)) {
                    rocketSlot = slot;
                    break;
                }
            }
            if (rocketSlot < 0) {
                return;
            }
            originalSlot = selected;
            player.getInventory().setSelectedSlot(rocketSlot);
            hand = InteractionHand.MAIN_HAND;
        }

        InteractionResult result = client.gameMode.useItem(player, hand);
        if (result.consumesAction()) {
            player.swing(hand);
            lastFireworkTick = now;
        }
        if (originalSlot >= 0 && player.getInventory().getSelectedSlot() == rocketSlot) {
            player.getInventory().setSelectedSlot(originalSlot);
        }
    }
}
