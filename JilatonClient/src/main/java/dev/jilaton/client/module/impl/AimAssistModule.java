package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AimAssistModule extends ClientModule {
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting turnSpeed;

    public AimAssistModule() {
        super("aim_assist", "AimAssist", "Smoothly corrects aim toward a visible nearby player while attack is held.", ModuleCategory.COMBAT);
        range = addSetting(new NumberSetting("range", "Range", 2.0, 6.0, 0.1, 4.2));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view", 5.0, 120.0, 1.0, 60.0));
        turnSpeed = addSetting(new NumberSetting("turn_speed", "Turn speed", 1.0, 18.0, 0.5, 6.0));
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null
                || !client.options.keyAttack.isDown()) {
            return;
        }

        Player player = client.player;
        double maxDistanceSquared = range.value() * range.value();
        double bestAngle = Double.POSITIVE_INFINITY;
        float bestYawDelta = 0.0F;
        float bestPitchDelta = 0.0F;

        for (Player candidate : client.level.players()) {
            if (candidate == player || !candidate.isAlive() || candidate.isSpectator()
                    || player.isAlliedTo(candidate) || !player.hasLineOfSight(candidate)) {
                continue;
            }

            Vec3 aimPoint = candidate.position().add(0.0, candidate.getBbHeight() * 0.55, 0.0);
            Vec3 difference = aimPoint.subtract(player.getEyePosition());
            if (difference.lengthSqr() > maxDistanceSquared) {
                continue;
            }

            double horizontal = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
            float targetYaw = (float) (Math.atan2(difference.z, difference.x) * (180.0 / Math.PI)) - 90.0F;
            float targetPitch = (float) (-Math.atan2(difference.y, horizontal) * (180.0 / Math.PI));
            float yawDelta = Mth.wrapDegrees(targetYaw - player.getYRot());
            float pitchDelta = targetPitch - player.getXRot();
            double angle = Math.hypot(yawDelta, pitchDelta);
            if (angle <= fieldOfView.value() && angle < bestAngle) {
                bestAngle = angle;
                bestYawDelta = yawDelta;
                bestPitchDelta = pitchDelta;
            }
        }

        if (bestAngle == Double.POSITIVE_INFINITY) {
            return;
        }

        float maxStep = (float) turnSpeed.value();
        player.setYRot(player.getYRot() + Mth.clamp(bestYawDelta, -maxStep, maxStep));
        player.setXRot(Mth.clamp(player.getXRot() + Mth.clamp(bestPitchDelta, -maxStep, maxStep), -90.0F, 90.0F));
    }
}
