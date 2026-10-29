package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class KillAuraModule extends ClientModule {
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting turnSpeed;
    private final NumberSetting clicksPerSecond;
    private final NumberSetting attackAngle;
    private final BooleanSetting playersOnly;
    private final BooleanSetting requireAttackKey;
    private final BooleanSetting showTargetIndicator;
    private LivingEntity currentTarget;
    private LivingEntity lastAttackTarget;
    private long nextAttackAtNanos;
    private long lastAttackAtNanos;

    public KillAuraModule() {
        super("kill_aura", "KillAura", "Rotates toward and attacks the best visible target in range.", ModuleCategory.COMBAT);
        range = addSetting(new NumberSetting("range", "Range", 2.5, 6.0, 0.1, 3.2));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view", 10.0, 180.0, 1.0, 90.0));
        turnSpeed = addSetting(new NumberSetting("turn_speed", "Turn speed", 1.0, 30.0, 0.5, 6.0));
        clicksPerSecond = addSetting(new NumberSetting("cps", "Maximum CPS", 1.0, 20.0, 1.0, 8.0));
        attackAngle = addSetting(new NumberSetting("attack_angle", "Attack angle", 1.0, 20.0, 0.5, 7.0));
        playersOnly = addSetting(new BooleanSetting("players_only", "Players only", true));
        requireAttackKey = addSetting(new BooleanSetting("require_attack_key", "Require attack key", false));
        showTargetIndicator = addSetting(new BooleanSetting("show_target_indicator", "Show target ring", true));
    }

    @Override
    protected void onEnable(Minecraft client) {
        nextAttackAtNanos = 0L;
        lastAttackTarget = null;
        lastAttackAtNanos = 0L;
    }

    @Override
    protected void onDisable(Minecraft client) {
        nextAttackAtNanos = 0L;
        lastAttackTarget = null;
        lastAttackAtNanos = 0L;
        currentTarget = null;
    }

    @Override
    protected void onTick(Minecraft client) {
        ClientModule potionModule = JilatonClient.moduleManager().get("auto_pot");
        if (potionModule instanceof AutoPotModule autoPot && autoPot.enabled() && autoPot.isPreparingThrow()) {
            currentTarget = null;
            return;
        }
        if (client.player == null || client.level == null || client.gameMode == null || client.screen != null
                || (requireAttackKey.value() && !client.options.keyAttack.isDown())) {
            currentTarget = null;
            return;
        }

        // AimAssist owns the player's rotation while the attack key is held.
        ClientModule aimAssist = JilatonClient.moduleManager().get("aim_assist");
        if (client.options.keyAttack.isDown() && aimAssist != null && aimAssist.enabled()) {
            currentTarget = null;
            return;
        }

        Player player = client.player;
        Target target = findTarget(client, player);
        currentTarget = target == null ? null : target.entity();
        if (target == null) {
            return;
        }

        float yawStep = Mth.clamp(target.yawDelta(), -(float) turnSpeed.value(), (float) turnSpeed.value());
        float pitchStep = Mth.clamp(target.pitchDelta(), -(float) turnSpeed.value(), (float) turnSpeed.value());
        player.setYRot(Mth.wrapDegrees(player.getYRot() + yawStep));
        player.setXRot(Mth.clamp(player.getXRot() + pitchStep, -90.0F, 90.0F));

        float remainingYaw = Mth.wrapDegrees(target.yaw() - player.getYRot());
        float remainingPitch = target.pitch() - player.getXRot();
        if (Math.hypot(remainingYaw, remainingPitch) > attackAngle.value()
                || player.getAttackStrengthScale(0.0F) < 0.95F) {
            return;
        }

        long now = System.nanoTime();
        if (now < nextAttackAtNanos) {
            return;
        }

        client.gameMode.attack(player, target.entity());
        player.swing(InteractionHand.MAIN_HAND);
        lastAttackTarget = target.entity();
        lastAttackAtNanos = now;
        nextAttackAtNanos = now + (long) (1_000_000_000.0 / clicksPerSecond.value());
    }

    public LivingEntity currentTarget() {
        return currentTarget;
    }

    public boolean showTargetIndicator() {
        return showTargetIndicator.value();
    }

    public boolean recentlyAttacked(LivingEntity target) {
        return target != null && target == lastAttackTarget
                && System.nanoTime() - lastAttackAtNanos < 450_000_000L;
    }

    private Target findTarget(Minecraft client, Player player) {
        double maxRange = range.value();
        double maxRangeSquared = maxRange * maxRange;
        double bestScore = Double.POSITIVE_INFINITY;
        Target bestTarget = null;

        for (LivingEntity candidate : client.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(maxRange),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator()
                        && (!playersOnly.value() || entity instanceof Player)
                        && (!(entity instanceof Player candidate)
                                ? !player.isAlliedTo(entity)
                                : PlayerTargetFilters.allowsPlayer(client, player, candidate))
                        && player.hasLineOfSight(entity))) {
            Vec3 difference = candidate.getBoundingBox().getCenter().subtract(player.getEyePosition());
            double distanceSquared = difference.lengthSqr();
            if (distanceSquared > maxRangeSquared || distanceSquared < 1.0E-6) {
                continue;
            }

            double horizontalDistance = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
            float yaw = (float) (Math.atan2(difference.z, difference.x) * (180.0 / Math.PI)) - 90.0F;
            float pitch = (float) (-Math.atan2(difference.y, horizontalDistance) * (180.0 / Math.PI));
            float yawDelta = Mth.wrapDegrees(yaw - player.getYRot());
            float pitchDelta = pitch - player.getXRot();
            double angle = Math.hypot(yawDelta, pitchDelta);
            if (angle > fieldOfView.value()) {
                continue;
            }

            double score = angle + Math.sqrt(distanceSquared) * 0.25;
            if (score < bestScore) {
                bestScore = score;
                bestTarget = new Target(candidate, yaw, pitch, yawDelta, pitchDelta);
            }
        }

        return bestTarget;
    }

    private record Target(LivingEntity entity, float yaw, float pitch, float yawDelta, float pitchDelta) {
    }
}
