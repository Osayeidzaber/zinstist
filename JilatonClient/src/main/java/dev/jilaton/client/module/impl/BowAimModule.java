package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class BowAimModule extends ClientModule {
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting turnSpeed;
    private final NumberSetting minimumDraw;
    private final NumberSetting targetHeight;
    private final BooleanSetting playersOnly;
    private final BooleanSetting predictMovement;
    private final BooleanSetting highArc;
    private final BooleanSetting stickyTarget;
    private LivingEntity currentTarget;

    public BowAimModule() {
        super("bow_aim", "BowAim", "Leads a valid target with a drag- and gravity-aware bow trajectory.",
                ModuleCategory.COMBAT);
        range = addSetting(new NumberSetting("range", "Target range", 4.0, 64.0, 1.0, 32.0));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view", 10.0, 180.0, 1.0, 120.0));
        turnSpeed = addSetting(new NumberSetting("turn_speed", "Turn speed", 1.0, 45.0, 0.5, 8.0));
        minimumDraw = addSetting(new NumberSetting("minimum_draw", "Minimum draw power", 0.1, 1.0, 0.05, 0.9));
        targetHeight = addSetting(new NumberSetting("target_height", "Target height", 0.2, 1.0, 0.05, 0.72));
        playersOnly = addSetting(new BooleanSetting("players_only", "Players only", true));
        predictMovement = addSetting(new BooleanSetting("predict_movement", "Predict movement", true));
        highArc = addSetting(new BooleanSetting("high_arc", "Prefer high arc", false));
        stickyTarget = addSetting(new BooleanSetting("sticky_target", "Keep current target", true));
    }

    @Override
    protected void onEnable(Minecraft client) {
        currentTarget = null;
    }

    @Override
    protected void onDisable(Minecraft client) {
        currentTarget = null;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null
                || !client.player.isUsingItem()
                || client.player.getUseItem().getUseAnimation() != ItemUseAnimation.BOW) {
            currentTarget = null;
            return;
        }

        Player player = client.player;
        int drawTicks = player.getTicksUsingItem();
        double drawPower = BowItem.getPowerForTime(drawTicks);
        if (drawPower < minimumDraw.value()) {
            currentTarget = null;
            return;
        }

        double projectileSpeed = drawPower * 3.0;
        AimedTarget aimedTarget = selectTarget(client, player, projectileSpeed);
        if (aimedTarget == null) {
            currentTarget = null;
            return;
        }

        BowAimMath.Solution angle = aimedTarget.solution();
        currentTarget = aimedTarget.target();
        float yawDelta = Mth.wrapDegrees(angle.yaw() - player.getYRot());
        float pitchDelta = Mth.clamp(angle.pitch() - player.getXRot(), -180.0F, 180.0F);
        float step = (float) turnSpeed.value();
        player.setYRot(player.getYRot() + Mth.clamp(yawDelta, -step, step));
        player.setXRot(Mth.clamp(player.getXRot() + Mth.clamp(pitchDelta, -step, step), -90.0F, 90.0F));
    }

    private AimedTarget selectTarget(Minecraft client, Player player, double projectileSpeed) {
        if (stickyTarget.value() && isValidTarget(client, player, currentTarget)) {
            BowAimMath.Solution stickySolution = solveTarget(player, currentTarget, projectileSpeed);
            if (isInFieldOfView(player, stickySolution)) {
                return new AimedTarget(currentTarget, stickySolution);
            }
        }

        List<LivingEntity> targets = client.level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range.value()), entity -> entity != player);
        AimedTarget bestTarget = null;
        double bestAngle = Double.POSITIVE_INFINITY;
        for (LivingEntity target : targets) {
            if (!isValidTarget(client, player, target)) {
                continue;
            }
            BowAimMath.Solution solution = solveTarget(player, target, projectileSpeed);
            double angle = angularDistance(player, solution);
            if (angle <= fieldOfView.value() && angle < bestAngle) {
                bestTarget = new AimedTarget(target, solution);
                bestAngle = angle;
            }
        }
        return bestTarget;
    }

    private boolean isValidTarget(Minecraft client, Player player, LivingEntity target) {
        if (target == null || target == player || target.level() != client.level || !target.isAlive()
                || target.isSpectator() || (playersOnly.value() && !(target instanceof Player))
                || !player.hasLineOfSight(target) || player.distanceToSqr(target) > range.value() * range.value()) {
            return false;
        }
        if (target instanceof Player candidate) {
            return PlayerTargetFilters.allowsPlayer(client, player, candidate);
        }
        return !player.isAlliedTo(target);
    }

    private BowAimMath.Solution solveTarget(Player player, LivingEntity target, double projectileSpeed) {
        if (target == null) {
            return null;
        }
        Vec3 origin = player.getEyePosition();
        Vec3 position = target.position().add(0.0, target.getBbHeight() * targetHeight.value(), 0.0);
        Vec3 velocity = predictMovement.value() ? target.getDeltaMovement() : Vec3.ZERO;
        return BowAimMath.solve(origin.x, origin.y, origin.z, position.x, position.y, position.z,
                        velocity.x, velocity.y, velocity.z, projectileSpeed, 0.05, 0.99,
                        predictMovement.value(), highArc.value())
                .orElse(null);
    }

    private boolean isInFieldOfView(Player player, BowAimMath.Solution angle) {
        return angle != null && angularDistance(player, angle) <= fieldOfView.value();
    }

    private static double angularDistance(Player player, BowAimMath.Solution angle) {
        if (angle == null) {
            return Double.POSITIVE_INFINITY;
        }
        float yaw = Mth.wrapDegrees(angle.yaw() - player.getYRot());
        float pitch = angle.pitch() - player.getXRot();
        return Math.hypot(yaw, pitch);
    }

    private record AimedTarget(LivingEntity target, BowAimMath.Solution solution) {
    }
}
