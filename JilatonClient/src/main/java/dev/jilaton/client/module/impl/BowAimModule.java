package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.rotation.RotationPriorities;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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
    private final BooleanSetting autoRelease;
    private final NumberSetting releaseTicks;
    private final NumberSetting delayBetweenShots;
    private final NumberSetting aimThreshold;
    private final BooleanSetting prechargedCrossbow;
    private final NumberSetting tridentReleaseTicks;
    private LivingEntity currentTarget;
    private long lastAutoReleaseNanos;

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
        autoRelease = addSetting(new BooleanSetting("auto_release", "Auto release bow", false));
        releaseTicks = addSetting(new NumberSetting("release_ticks", "Release after ticks", 3.0, 20.0, 1.0, 15.0));
        delayBetweenShots = addSetting(new NumberSetting("shot_delay", "Delay between shots", 0.0, 5.0, 0.05, 0.0));
        aimThreshold = addSetting(new NumberSetting("aim_threshold", "Release aim tolerance", 1.0, 4.0, 0.1, 1.5));
        prechargedCrossbow = addSetting(new BooleanSetting("precharged_crossbow", "Fire precharged crossbows", false));
        tridentReleaseTicks = addSetting(new NumberSetting("trident_release_ticks", "Trident release after ticks",
                TridentItem.THROW_THRESHOLD_TIME + 1.0, 40.0, 1.0, TridentItem.THROW_THRESHOLD_TIME + 1.0));
    }

    @Override
    protected void onEnable(Minecraft client) {
        currentTarget = null;
        lastAutoReleaseNanos = 0L;
    }

    @Override
    protected void onDisable(Minecraft client) {
        currentTarget = null;
        lastAutoReleaseNanos = 0L;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null) {
            currentTarget = null;
            return;
        }

        Player player = client.player;
        boolean crossbow = false;
        boolean precharged = false;
        int requiredReleaseTicks;
        InteractionHand hand;
        double projectileSpeed;
        if (player.isUsingItem()) {
            hand = player.getUsedItemHand();
            ItemStack usedItem = player.getUseItem();
            if (usedItem.getItem() instanceof BowItem
                    && usedItem.getUseAnimation() == ItemUseAnimation.BOW) {
                double drawPower = BowItem.getPowerForTime(player.getTicksUsingItem());
                if (drawPower < minimumDraw.value()) {
                    currentTarget = null;
                    return;
                }
                projectileSpeed = drawPower * 3.0;
                requiredReleaseTicks = (int) releaseTicks.value();
            } else if (usedItem.getItem() instanceof CrossbowItem
                    && usedItem.getUseAnimation() == ItemUseAnimation.CROSSBOW) {
                crossbow = true;
                projectileSpeed = 3.15;
                requiredReleaseTicks = 0;
            } else if (usedItem.getItem() instanceof TridentItem
                    && usedItem.getUseAnimation() == ItemUseAnimation.TRIDENT
                    && !hasRiptide(usedItem)) {
                projectileSpeed = TridentItem.PROJECTILE_SHOOT_POWER;
                requiredReleaseTicks = (int) tridentReleaseTicks.value();
            } else {
                currentTarget = null;
                return;
            }
        } else if (prechargedCrossbow.value()) {
            hand = findPrechargedArrowCrossbow(player);
            if (hand == null) {
                currentTarget = null;
                return;
            }
            crossbow = true;
            precharged = true;
            projectileSpeed = 3.15;
            requiredReleaseTicks = 0;
        } else {
            currentTarget = null;
            return;
        }

        AimedTarget aimedTarget = selectTarget(client, player, projectileSpeed);
        if (aimedTarget == null) {
            currentTarget = null;
            return;
        }

        BowAimMath.Solution angle = aimedTarget.solution();
        currentTarget = aimedTarget.target();
        JilatonClient.moduleManager().rotationManager().request(id(), RotationPriorities.BOW_AIM,
                angle.yaw(), angle.pitch(), (float) turnSpeed.value());
        if (crossbow) {
            if (precharged) {
                tryAutoFireCrossbow(client, player, aimedTarget, hand);
            }
        } else {
            tryAutoRelease(client, player, aimedTarget, requiredReleaseTicks);
        }
    }

    private InteractionHand findPrechargedArrowCrossbow(Player player) {
        if (supportsArrowShot(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (supportsArrowShot(player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean supportsArrowShot(ItemStack stack) {
        if (!(stack.getItem() instanceof CrossbowItem) || !CrossbowItem.isCharged(stack)) {
            return false;
        }
        ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
        if (projectiles == null || projectiles.isEmpty()) {
            return false;
        }
        return projectiles.getItems().stream().allMatch(projectile -> projectile.is(Items.ARROW)
                || projectile.is(Items.SPECTRAL_ARROW) || projectile.is(Items.TIPPED_ARROW));
    }

    private static boolean hasRiptide(ItemStack stack) {
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) {
            return false;
        }
        for (var enchantment : enchantments.keySet()) {
            if (enchantments.getLevel(enchantment) > 0
                    && enchantment.unwrapKey().filter(Enchantments.RIPTIDE::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private void tryAutoRelease(Minecraft client, Player player, AimedTarget aimedTarget, int requiredTicks) {
        if (!autoRelease.value() || client.gameMode == null || aimedTarget.target() == null
                || !aimedTarget.target().isAlive()) {
            return;
        }

        long now = System.nanoTime();
        double aimError = angularDistance(player, aimedTarget.solution());
        long delayNanos = (long) (delayBetweenShots.value() * 1_000_000_000.0);
        if (AutoBowReleasePolicy.shouldRelease(true, true, player.getTicksUsingItem(),
                requiredTicks, aimError, aimThreshold.value(), now,
                lastAutoReleaseNanos, delayNanos)) {
            client.gameMode.releaseUsingItem(player);
            lastAutoReleaseNanos = now;
        }
    }

    private void tryAutoFireCrossbow(Minecraft client, Player player, AimedTarget aimedTarget,
                                     InteractionHand hand) {
        if (!autoRelease.value() || client.gameMode == null || player.isUsingItem()
                || aimedTarget.target() == null || !aimedTarget.target().isAlive()) {
            return;
        }

        long now = System.nanoTime();
        double aimError = angularDistance(player, aimedTarget.solution());
        long delayNanos = (long) (delayBetweenShots.value() * 1_000_000_000.0);
        if (AutoBowReleasePolicy.shouldRelease(true, true, 0, 0,
                aimError, aimThreshold.value(), now, lastAutoReleaseNanos, delayNanos)) {
            client.gameMode.useItem(player, hand);
            lastAutoReleaseNanos = now;
        }
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
        float yaw = BowAimMath.yawDelta(player.getYRot(), angle.yaw());
        float pitch = angle.pitch() - player.getXRot();
        return Math.hypot(yaw, pitch);
    }

    private record AimedTarget(LivingEntity target, BowAimMath.Solution solution) {
    }
}
