package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.rotation.RotationPriorities;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class KillAuraModule extends ClientModule {
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting turnSpeed;
    private final NumberSetting clicksPerSecond;
    private final NumberSetting minimumClicksPerSecond;
    private final ChoiceSetting clickPattern;
    private final NumberSetting attackAngle;
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting requireAttackKey;
    private final BooleanSetting avoidWastedShieldAttacks;
    private final BooleanSetting showTargetIndicator;
    private final ChoiceSetting priority;
    private LivingEntity currentTarget;
    private LivingEntity lastAttackTarget;
    private long nextAttackAtNanos;
    private long lastAttackAtNanos;

    public KillAuraModule() {
        super("kill_aura", "KillAura", "Rotates toward and attacks the best visible target in range.", ModuleCategory.COMBAT);
        range = addSetting(new NumberSetting("range", "Range", 2.5, 6.0, 0.1, 3.2));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view", 10.0, 180.0, 1.0, 90.0));
        turnSpeed = addSetting(new NumberSetting("turn_speed", "Turn speed", 1.0, 30.0, 0.5, 6.0));
        minimumClicksPerSecond = addSetting(new NumberSetting("minimum_cps", "Minimum CPS", 1.0, 20.0, 1.0, 6.0));
        clicksPerSecond = addSetting(new NumberSetting("cps", "Maximum CPS", 1.0, 20.0, 1.0, 8.0));
        clickPattern = addSetting(new ChoiceSetting("click_pattern", "Click pattern",
                List.of("Steady", "Normal distribution"), "Normal distribution"));
        attackAngle = addSetting(new NumberSetting("attack_angle", "Attack angle", 1.0, 20.0, 0.5, 7.0));
        players = addSetting(new BooleanSetting("players", "Players", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostile mobs", false));
        priority = addSetting(new ChoiceSetting("priority", "Target priority",
                List.of(KillAuraTargeting.DISTANCE, KillAuraTargeting.HEALTH, KillAuraTargeting.ANGLE),
                KillAuraTargeting.ANGLE));
        requireAttackKey = addSetting(new BooleanSetting("require_attack_key", "Require attack key", false));
        avoidWastedShieldAttacks = addSetting(new BooleanSetting("avoid_wasted_shield_attacks",
                "Skip shield-blocked attacks without an axe", true));
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
                || client.player.isUsingItem()
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

        JilatonClient.moduleManager().rotationManager().request(id(), RotationPriorities.KILL_AURA,
                target.yaw(), target.pitch(), (float) turnSpeed.value());

        if (Math.hypot(target.yawDelta(), target.pitchDelta()) > attackAngle.value()
                || player.getAttackStrengthScale(0.0F) < 0.95F) {
            return;
        }

        ClientModule criticalsModule = JilatonClient.moduleManager().get("criticals");
        if (criticalsModule instanceof CriticalsModule criticals && criticals.hasPendingAttack()) {
            return;
        }

        long now = System.nanoTime();
        if (now < nextAttackAtNanos) {
            return;
        }

        client.gameMode.attack(player, target.entity());
        if (criticalsModule instanceof CriticalsModule criticals && criticals.hasPendingAttack()) {
            return;
        }
        player.swing(InteractionHand.MAIN_HAND);
        lastAttackTarget = target.entity();
        lastAttackAtNanos = now;
        double sampledCps = ClickTiming.sampleCps(minimumClicksPerSecond.value(), clicksPerSecond.value(),
                clickPattern.value(), ThreadLocalRandom.current().nextGaussian());
        nextAttackAtNanos = now + ClickTiming.intervalNanos(sampledCps);
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

    public ChoiceSetting priority() {
        return priority;
    }

    public NumberSetting minimumClicksPerSecond() {
        return minimumClicksPerSecond;
    }

    public NumberSetting clicksPerSecond() {
        return clicksPerSecond;
    }

    public ChoiceSetting clickPattern() {
        return clickPattern;
    }

    private Target findTarget(Minecraft client, Player player) {
        double maxRange = range.value();
        double maxRangeSquared = maxRange * maxRange;
        Target bestTarget = null;
        Target incumbent = null;
        Vec3 eye = player.getEyePosition();

        for (LivingEntity candidate : client.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(maxRange),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator()
                        && wanted(client, player, entity))) {
            AABB bounds = candidate.getBoundingBox();
            Vec3 closest = closestPoint(eye, bounds);
            Vec3 difference = closest.subtract(eye);
            double distanceSquared = difference.lengthSqr();
            if (distanceSquared > maxRangeSquared || distanceSquared < 1.0E-6) {
                continue;
            }

            Vec3 point = aimPoint(client, player, eye, bounds);
            if (point == null) {
                continue;
            }
            difference = point.subtract(eye);
            double horizontalDistance = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
            float yaw = (float) (Math.atan2(difference.z, difference.x) * (180.0 / Math.PI)) - 90.0F;
            float pitch = (float) (-Math.atan2(difference.y, horizontalDistance) * (180.0 / Math.PI));
            float yawDelta = Mth.wrapDegrees(yaw - player.getYRot());
            float pitchDelta = pitch - player.getXRot();
            double angle = Math.hypot(yawDelta, pitchDelta);
            if (angle > fieldOfView.value()) {
                continue;
            }

            double distance = Math.sqrt(distanceSquared);
            double effectiveHealth = candidate.getHealth() + candidate.getAbsorptionAmount();
            double rank = KillAuraTargeting.rank(priority.value(), distance, effectiveHealth, angle);
            Target selected = new Target(candidate, yaw, pitch, yawDelta, pitchDelta, rank, distance);
            if (candidate == currentTarget) {
                incumbent = selected;
            }
            if (bestTarget == null || compare(selected, bestTarget) < 0) {
                bestTarget = selected;
            }
        }
        return incumbent == null ? bestTarget : incumbent;
    }

    private boolean wanted(Minecraft client, Player player, LivingEntity candidate) {
        if (candidate instanceof Player other) {
            if (!players.value() || !PlayerTargetFilters.allowsPlayer(client, player, other)) {
                return false;
            }
            if (!avoidWastedShieldAttacks.value() || !other.isBlocking()
                    || !ShieldTargetPolicy.shieldFacesAttacker(player.getX(), player.getZ(),
                    other.getX(), other.getZ(), other.getYHeadRot())) {
                return true;
            }
            boolean axeEquipped = player.getMainHandItem().is(net.minecraft.tags.ItemTags.AXES);
            if (axeEquipped) {
                return true;
            }
            ClientModule weaponModule = JilatonClient.moduleManager().get("auto_weapon");
            boolean autoWeaponCanSelectAxe = weaponModule instanceof AutoWeaponModule autoWeapon
                    && autoWeapon.canSelectShieldBreaker(client, player);
            return !KillAuraTargeting.shouldSkipShield(true, true, false, autoWeaponCanSelectAxe);
        }
        if (!hostiles.value() || !(candidate instanceof Monster monster) || player.isAlliedTo(candidate)) {
            return false;
        }
        return !(monster instanceof NeutralMob) || monster.isAggressive();
    }

    private static Vec3 aimPoint(Minecraft client, Player player, Vec3 eye, AABB box) {
        double side = Math.min(0.15, box.getXsize() * 0.3);
        double vertical = Math.min(0.2, box.getYsize() * 0.25);
        Vec3 near = closestPoint(eye, box.inflate(-side, -vertical, -side));
        if (clearPath(client, player, eye, near)) {
            return near;
        }
        Vec3 center = box.getCenter();
        for (double height : new double[]{0.85, 0.5, 0.15}) {
            Vec3 point = new Vec3(center.x, box.minY + box.getYsize() * height, center.z);
            if (clearPath(client, player, eye, point)) {
                return point;
            }
        }
        return null;
    }

    private static boolean clearPath(Minecraft client, Player player, Vec3 start, Vec3 end) {
        return client.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    private static Vec3 closestPoint(Vec3 point, AABB box) {
        return new Vec3(Mth.clamp(point.x, box.minX, box.maxX),
                Mth.clamp(point.y, box.minY, box.maxY), Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    private static int compare(Target left, Target right) {
        int order = Double.compare(left.rank(), right.rank());
        if (order == 0) {
            order = Integer.compare(left.entity().hurtTime, right.entity().hurtTime);
        }
        if (order == 0) {
            order = Double.compare(left.distance(), right.distance());
        }
        return order;
    }

    private record Target(LivingEntity entity, float yaw, float pitch, float yawDelta, float pitchDelta,
                          double rank, double distance) {
    }
}
