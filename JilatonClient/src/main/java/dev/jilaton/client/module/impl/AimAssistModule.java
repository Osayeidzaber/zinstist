/*
 * Target persistence and range/FOV hysteresis adapted from the supplied Anubis 0.9.9.3
 * AimAssistModule decompilation, with the user's permission to reuse its code.
 */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.rotation.RotationPriorities;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AimAssistModule extends ClientModule {
    private static final double STICK_RANGE = 0.5;
    private static final double STICK_FOV_MULTIPLIER = 1.3;
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting turnSpeed;
    private final NumberSetting angleFactor;
    private final BooleanSetting horizontal;
    private final BooleanSetting vertical;
    private final BooleanSetting stickyTarget;
    private Player currentTarget;

    public AimAssistModule() {
        super("aim_assist", "AimAssist", "Smoothly corrects aim toward a visible nearby player while attack is held.", ModuleCategory.COMBAT);
        range = addSetting(new NumberSetting("range", "Range", 2.0, 6.0, 0.1, 4.2));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view", 5.0, 120.0, 1.0, 60.0));
        turnSpeed = addSetting(new NumberSetting("turn_speed", "Turn speed", 1.0, 18.0, 0.5, 6.0));
        angleFactor = addSetting(new NumberSetting("angle_factor", "Angle factor", 0.05, 1.0, 0.05, 0.35));
        horizontal = addSetting(new BooleanSetting("horizontal", "Horizontal aim", true));
        vertical = addSetting(new BooleanSetting("vertical", "Vertical aim", true));
        stickyTarget = addSetting(new BooleanSetting("sticky_target", "Keep current target", true));
    }

    @Override
    protected void onDisable(Minecraft client) {
        currentTarget = null;
    }

    @Override
    protected void onTick(Minecraft client) {
        ClientModule potionModule = JilatonClient.moduleManager().get("auto_pot");
        if (potionModule instanceof AutoPotModule autoPot && autoPot.enabled() && autoPot.isPreparingThrow()) {
            currentTarget = null;
            return;
        }
        if (client.player == null || client.level == null || client.screen != null
                || !client.options.keyAttack.isDown()) {
            currentTarget = null;
            return;
        }

        Player player = client.player;
        if (stickyTarget.value() && isValidTarget(client, player, currentTarget,
                range.value() + STICK_RANGE, fieldOfView.value() * STICK_FOV_MULTIPLIER)) {
            rotateToward(player, currentTarget);
            return;
        }

        currentTarget = null;
        double bestAngle = Double.POSITIVE_INFINITY;
        Player bestTarget = null;

        for (Player candidate : client.level.players()) {
            if (!isValidTarget(client, player, candidate, range.value(), fieldOfView.value())) {
                continue;
            }

            Vec3 aimPoint = candidate.position().add(0.0, candidate.getBbHeight() * 0.55, 0.0);
            Vec3 difference = aimPoint.subtract(player.getEyePosition());
            double horizontal = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
            float targetYaw = (float) (Math.atan2(difference.z, difference.x) * (180.0 / Math.PI)) - 90.0F;
            float targetPitch = (float) (-Math.atan2(difference.y, horizontal) * (180.0 / Math.PI));
            float yawDelta = Mth.wrapDegrees(targetYaw - player.getYRot());
            float pitchDelta = targetPitch - player.getXRot();
            double angle = Math.hypot(yawDelta, pitchDelta);
            if (angle < bestAngle) {
                bestAngle = angle;
                bestTarget = candidate;
            }
        }

        if (bestTarget == null) {
            return;
        }

        currentTarget = bestTarget;
        rotateToward(player, currentTarget);
    }

    private boolean isValidTarget(Minecraft client, Player player, Player candidate,
                                  double allowedRange, double allowedFov) {
        if (candidate == null || candidate == player || candidate.level() != client.level
                || !candidate.isAlive() || candidate.isSpectator()
                || !PlayerTargetFilters.allowsPlayer(client, player, candidate)
                || !player.hasLineOfSight(candidate)) {
            return false;
        }

        Vec3 aimPoint = candidate.position().add(0.0, candidate.getBbHeight() * 0.55, 0.0);
        Vec3 difference = aimPoint.subtract(player.getEyePosition());
        if (difference.lengthSqr() > allowedRange * allowedRange) {
            return false;
        }

        double horizontal = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
        float yaw = (float) (Math.atan2(difference.z, difference.x) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-Math.atan2(difference.y, horizontal) * (180.0 / Math.PI));
        float yawDelta = Mth.wrapDegrees(yaw - player.getYRot());
        float pitchDelta = pitch - player.getXRot();
        return Math.hypot(yawDelta, pitchDelta) <= allowedFov;
    }

    private void rotateToward(Player player, Player target) {
        Vec3 aimPoint = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
        Vec3 difference = aimPoint.subtract(player.getEyePosition());
        double horizontal = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
        float targetYaw = (float) (Math.atan2(difference.z, difference.x) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-Math.atan2(difference.y, horizontal) * (180.0 / Math.PI));
        AimAssistPolicy.Angles smoothed = AimAssistPolicy.apply(player.getYRot(), player.getXRot(),
                targetYaw, targetPitch, angleFactor.value(), this.horizontal.value(), vertical.value());
        JilatonClient.moduleManager().rotationManager().request(id(), RotationPriorities.AIM_ASSIST,
                smoothed.yaw(), smoothed.pitch(), (float) turnSpeed.value());
    }

    public NumberSetting angleFactor() {
        return angleFactor;
    }

    public BooleanSetting horizontal() {
        return horizontal;
    }

    public BooleanSetting vertical() {
        return vertical;
    }
}
