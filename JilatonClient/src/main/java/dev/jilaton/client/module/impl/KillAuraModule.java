package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.rotation.RotationPriorities;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class KillAuraModule extends ClientModule {
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting turnSpeed;
    private final NumberSetting clicksPerSecond;
    private final NumberSetting minimumClicksPerSecond;
    private final ChoiceSetting clickPattern;
    private final NumberSetting attackAngle;
    private final BooleanSetting respectAttackCooldown;
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting requireAttackKey;
    private final BooleanSetting avoidWastedShieldAttacks;
    private final ChoiceSetting raycastMode;
    private final BooleanSetting showTargetIndicator;
    private final ChoiceSetting highlightStyle;
    private final ChoiceSetting highlightColor;
    private final NumberSetting highlightOpacity;
    private final NumberSetting highlightWidth;
    private final ChoiceSetting priority;
    private final ClickSchedule clickSchedule = new ClickSchedule();
    private LivingEntity currentTarget;
    private LivingEntity lastAttackTarget;
    private long lastAttackAtNanos;

    public KillAuraModule() {
        super("kill_aura", "KillAura", "Rotates toward and attacks the best visible target in range.", ModuleCategory.COMBAT);
        range = addSetting(new NumberSetting("range", "Range", 2.5, 6.0, 0.1, 3.2));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view", 10.0, 180.0, 1.0, 90.0));
        turnSpeed = addSetting(new NumberSetting("turn_speed", "Turn speed", 1.0, 30.0, 0.5, 6.0));
        minimumClicksPerSecond = addSetting(new NumberSetting("minimum_cps", "Minimum CPS", 1.0, 20.0, 1.0, 6.0));
        clicksPerSecond = addSetting(new NumberSetting("cps", "Maximum CPS", 1.0, 20.0, 1.0, 8.0));
        clickPattern = addSetting(new ChoiceSetting("click_pattern", "Click pattern",
                ClickPatternDistribution.OPTIONS, "Normal distribution"));
        attackAngle = addSetting(new NumberSetting("attack_angle", "Attack angle", 1.0, 20.0, 0.5, 7.0));
        respectAttackCooldown = addSetting(new BooleanSetting("respect_attack_cooldown", "Respect attack cooldown", true));
        players = addSetting(new BooleanSetting("players", "Players", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostile mobs", false));
        priority = addSetting(new ChoiceSetting("priority", "Target priority",
                List.of(KillAuraTargeting.DISTANCE, KillAuraTargeting.HEALTH, KillAuraTargeting.ANGLE),
                KillAuraTargeting.ANGLE));
        requireAttackKey = addSetting(new BooleanSetting("require_attack_key", "Require attack key", false));
        avoidWastedShieldAttacks = addSetting(new BooleanSetting("avoid_wasted_shield_attacks",
                "Skip shield-blocked attacks without an axe", true));
        raycastMode = addSetting(new ChoiceSetting("raycast_mode", "Raycast mode",
                List.of("None", "Enemy", "All"), "None"));
        showTargetIndicator = addSetting(new BooleanSetting("show_target_indicator", "Target highlight", true));
        highlightStyle = addSetting(new ChoiceSetting("highlight_style", "Highlight style", List.of("Halo", "Corners", "Halo + corners"), "Halo"));
        highlightColor = addSetting(new ChoiceSetting("highlight_color", "Highlight color", List.of("Monochrome", "Theme"), "Monochrome"));
        highlightOpacity = addSetting(new NumberSetting("highlight_opacity", "Highlight opacity", 0.1, 1, 0.05, 0.85));
        highlightWidth = addSetting(new NumberSetting("highlight_width", "Highlight line width", 1, 4, 0.5, 1.5));
    }

    @Override
    protected void onEnable(Minecraft client) {
        clickSchedule.reset();
        lastAttackTarget = null;
        lastAttackAtNanos = 0L;
    }

    @Override
    protected void onDisable(Minecraft client) {
        clickSchedule.reset();
        lastAttackTarget = null;
        lastAttackAtNanos = 0L;
        currentTarget = null;
    }

    @Override
    protected void onTick(Minecraft client) {
        int scheduledClicks = clickSchedule.advanceClicks(minimumClicksPerSecond.value(),
                clicksPerSecond.value(), clickPattern.value());
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
                || (respectAttackCooldown.value() && player.getAttackStrengthScale(0.0F) < 0.95F)) {
            return;
        }

        if (!raycastAllows(client, player, target.entity())) {
            return;
        }

        ClientModule criticalsModule = JilatonClient.moduleManager().get("criticals");
        if (criticalsModule instanceof CriticalsModule criticals && criticals.hasPendingAttack()) {
            return;
        }

        if (scheduledClicks <= 0) {
            return;
        }

        for (int click = 0; click < scheduledClicks; click++) {
            if (!target.entity().isAlive()
                    || (respectAttackCooldown.value() && player.getAttackStrengthScale(0.0F) < 0.95F)) {
                break;
            }
            long now = System.nanoTime();
            client.gameMode.attack(player, target.entity());
            if (criticalsModule instanceof CriticalsModule criticals && criticals.hasPendingAttack()) {
                return;
            }
            player.swing(InteractionHand.MAIN_HAND);
            lastAttackTarget = target.entity();
            lastAttackAtNanos = now;
        }
    }

    public LivingEntity currentTarget() {
        return currentTarget;
    }

    public boolean showTargetIndicator() {
        return showTargetIndicator.value();
    }

    public boolean highlightHalo() { return !highlightStyle.value().equals("Corners"); }
    public boolean highlightCorners() { return !highlightStyle.value().equals("Halo"); }
    public int highlightColor(int theme) { return highlightColor.value().equals("Theme") ? theme : 0xFFE1E5EC; }
    public double highlightOpacity() { return highlightOpacity.value(); }
    public float highlightWidth() { return (float) highlightWidth.value(); }

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

    public BooleanSetting respectAttackCooldown() {
        return respectAttackCooldown;
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
            float yawDelta = KillAuraTargeting.yawDelta(player.getYRot(), yaw);
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
            boolean blocking = other.isBlocking();
            boolean facing = blocking && ShieldTargetPolicy.shieldFacesAttacker(player.getX(), player.getZ(),
                    other.getX(), other.getZ(), other.getYHeadRot());
            return !avoidWastedShieldAttacks.value()
                    || !ShieldTargetPolicy.shieldAbsorbsAttack(blocking, facing,
                    facing && attackerCanDisableBlocking(client, player));
        }
        if (!hostiles.value() || !(candidate instanceof Monster monster) || player.isAlliedTo(candidate)) {
            return false;
        }
        return !(monster instanceof NeutralMob) || monster.isAggressive();
    }

    private boolean attackerCanDisableBlocking(Minecraft client, Player player) {
        if (player.getMainHandItem().is(net.minecraft.tags.ItemTags.AXES)) {
            return true;
        }
        ClientModule weaponModule = JilatonClient.moduleManager().get("auto_weapon");
        return weaponModule instanceof AutoWeaponModule autoWeapon
                && autoWeapon.canSelectShieldBreaker(client, player);
    }

    private boolean raycastAllows(Minecraft client, Player player, LivingEntity target) {
        String mode = raycastMode.value();
        if (mode.equals("None")) {
            return true;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = eye.add(direction.scale(range.value()));
        HitResult blockHit = client.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double blockDistance = blockHit.getType() == HitResult.Type.MISS
                ? Double.POSITIVE_INFINITY : eye.distanceTo(blockHit.getLocation());
        AABB targetBox = target.getBoundingBox().inflate(0.1);
        List<AABB> candidateBoxes = new ArrayList<>();

        if (mode.equals("Enemy")) {
            for (LivingEntity candidate : client.level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(eye, end).inflate(1.0), entity -> entity != player && wanted(client, player, entity))) {
                candidateBoxes.add(candidate.getBoundingBox().inflate(0.1));
            }
        } else {
            for (Entity candidate : client.level.getEntities(player, new AABB(eye, end).inflate(1.0),
                    Entity::isPickable)) {
                candidateBoxes.add(candidate.getBoundingBox().inflate(0.1));
            }
        }

        return KillAuraRaycastPolicy.targetIsFirstOnRay(eye, direction, range.value(), targetBox,
                blockDistance, candidateBoxes);
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
        return KillAuraTargeting.compare(left.rank(), left.entity().hurtTime, left.distance(),
                right.rank(), right.entity().hurtTime, right.distance());
    }

    private record Target(LivingEntity entity, float yaw, float pitch, float yawDelta, float pitchDelta,
                          double rank, double distance) {
    }
}
