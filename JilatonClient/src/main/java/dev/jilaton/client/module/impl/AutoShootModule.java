package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.rotation.RotationAngles;
import dev.jilaton.client.rotation.RotationPriorities;
import dev.jilaton.client.rotation.RotationPolicy;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** A target-aware throw module for eggs and snowballs, with an opt-in generic held-item mode. */
public final class AutoShootModule extends ClientModule {
    private final ChoiceSetting throwableType;
    private final ChoiceSetting aimMode;
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting turnSpeed;
    private final NumberSetting aimTolerance;
    private final NumberSetting targetHeight;
    private final BooleanSetting predictMovement;
    private final BooleanSetting highArc;
    private final BooleanSetting stickyTarget;
    private final BooleanSetting selectSlotAutomatically;
    private final NumberSetting projectileSpeed;
    private final NumberSetting projectileGravity;
    private final NumberSetting projectileDrag;
    private final NumberSetting minimumCps;
    private final NumberSetting maximumCps;
    private final ChoiceSetting clickPattern;
    private final BooleanSetting requiresKillAura;

    private Player currentTarget;
    private int originalSlot = -1;
    private int ownedSlot = -1;
    private boolean manualSlotOverride;
    private Player slotPlayer;
    private Object slotWorld;
    private final ClickSchedule throwSchedule = new ClickSchedule();

    public AutoShootModule() {
        super("auto_shoot", "AutoShoot", "Aims and throws configured projectiles at visible players.",
                ModuleCategory.COMBAT);
        throwableType = addSetting(new ChoiceSetting("throwable_type", "Item selection",
                List.of("Eggs and snowballs", "Any held item"), "Eggs and snowballs"));
        aimMode = addSetting(new ChoiceSetting("aim_mode", "Aim mode",
                List.of("Auto", "Direct", "Ballistic"), "Auto"));
        range = addSetting(new NumberSetting("range", "Target range", 3.0, 32.0, 0.5, 6.0));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view", 10.0, 180.0, 1.0, 120.0));
        turnSpeed = addSetting(new NumberSetting("turn_speed", "Turn speed", 1.0, 45.0, 0.5, 10.0));
        aimTolerance = addSetting(new NumberSetting("aim_tolerance", "Throw aim tolerance", 0.5, 10.0, 0.1, 2.0));
        targetHeight = addSetting(new NumberSetting("target_height", "Target height", 0.2, 1.0, 0.05, 0.68));
        predictMovement = addSetting(new BooleanSetting("predict_movement", "Predict target movement", true));
        highArc = addSetting(new BooleanSetting("high_arc", "Prefer high arc", false));
        stickyTarget = addSetting(new BooleanSetting("sticky_target", "Keep current target", true));
        selectSlotAutomatically = addSetting(new BooleanSetting("select_slot_automatically", "Select projectile slot", true));
        projectileSpeed = addSetting(new NumberSetting("projectile_speed", "Projectile speed", 0.5, 4.0, 0.05, 1.5));
        projectileGravity = addSetting(new NumberSetting("projectile_gravity", "Projectile gravity", 0.0, 0.2, 0.005, 0.03));
        projectileDrag = addSetting(new NumberSetting("projectile_drag", "Air drag", 0.8, 1.0, 0.005, 0.99));
        minimumCps = addSetting(new NumberSetting("minimum_cps", "Minimum throws per second", 1.0, 20.0, 1.0, 5.0));
        maximumCps = addSetting(new NumberSetting("maximum_cps", "Maximum throws per second", 1.0, 20.0, 1.0, 8.0));
        clickPattern = addSetting(new ChoiceSetting("click_pattern", "Throw timing",
                ClickPatternDistribution.OPTIONS, "Normal distribution"));
        requiresKillAura = addSetting(new BooleanSetting("requires_kill_aura", "Require KillAura enabled", false));
    }

    @Override
    protected void onEnable(Minecraft client) {
        currentTarget = null;
        throwSchedule.reset();
        manualSlotOverride = false;
    }

    @Override
    protected void onDisable(Minecraft client) {
        restoreOwnedSlot(client != null && client.player == slotPlayer && client.level == slotWorld ? client.player : null);
        currentTarget = null;
        throwSchedule.reset();
        manualSlotOverride = false;
        slotPlayer = null;
        slotWorld = null;
    }

    @Override
    protected void onTick(Minecraft client) {
        int scheduledThrows = throwSchedule.advanceClicks(
                minimumCps.value(), maximumCps.value(), clickPattern.value());
        Player player = client.player;
        if (slotPlayer != player || slotWorld != client.level) {
            // A respawn/dimension transition must not apply another inventory's restore selection.
            originalSlot = ownedSlot = -1;
            manualSlotOverride = false;
            currentTarget = null;
            slotPlayer = player;
            slotWorld = client.level;
        }
        if (player == null || client.level == null || client.gameMode == null || client.screen != null
                || player.isUsingItem() || !killAuraRequirementMet() || shouldYieldToOtherActions(client)) {
            resetTargetAndSlot(player);
            return;
        }

        ThrowableChoice choice = findThrowable(player);
        if (choice == null) {
            if (manualSlotOverride) {
                currentTarget = null;
            } else {
                resetTargetAndSlot(player);
            }
            return;
        }

        AimedTarget aimedTarget = selectTarget(client, player, choice.stack());
        if (aimedTarget == null) {
            resetTargetAndSlot(player);
            return;
        }
        Player target = aimedTarget.target();
        currentTarget = target;

        BowAimMath.Solution solution = aimedTarget.solution();

        int rotationPriority = requiresKillAura.value()
                ? RotationPriorities.AUTO_SHOOT_WITH_KILL_AURA : RotationPriorities.AUTO_SHOOT;
        JilatonClient.moduleManager().rotationManager().request(id(), rotationPriority,
                solution.yaw(), solution.pitch(), (float) turnSpeed.value());

        float aimError = RotationPolicy.angularError(player.getYRot(), player.getXRot(),
                new RotationAngles(solution.yaw(), solution.pitch()));
        boolean ready = AutoShootPolicy.canShoot(new AutoShootPolicy.Conditions(
                client.screen == null && !player.isUsingItem(), target.isAlive(), !choice.stack().isEmpty(),
                solution != null, scheduledThrows > 0,
                aimError <= aimTolerance.value() && !player.getCooldowns().isOnCooldown(choice.stack())));
        if (!ready) {
            return;
        }

        int throwsThisTick = AutoShootPolicy.throwsThisTick(scheduledThrows);
        boolean consumedAction = false;
        for (int attempt = 0; attempt < throwsThisTick; attempt++) {
            if (player.isUsingItem() || choice.stack().isEmpty()
                    || player.getCooldowns().isOnCooldown(choice.stack())) {
                break;
            }
            InteractionResult result = client.gameMode.useItem(player, choice.hand());
            if (!result.consumesAction()) {
                break;
            }
            player.swing(choice.hand());
            consumedAction = true;
        }
        if (consumedAction) {
            restoreOwnedSlot(player);
        }
    }

    public Player currentTarget() {
        return currentTarget;
    }

    private boolean killAuraRequirementMet() {
        if (!requiresKillAura.value()) {
            return true;
        }
        ClientModule killAura = JilatonClient.moduleManager().get("kill_aura");
        return killAura != null && killAura.enabled();
    }

    private boolean shouldYieldToOtherActions(Minecraft client) {
        if (client.options.keyUse.isDown()) {
            return true;
        }

        ClientModule anchorAura = JilatonClient.moduleManager().get("anchor_aura");
        ClientModule autoRod = JilatonClient.moduleManager().get("auto_rod");
        if ((anchorAura != null && anchorAura.enabled()) || (autoRod != null && autoRod.enabled())) {
            return true;
        }

        ClientModule killAura = JilatonClient.moduleManager().get("kill_aura");
        if (killAura != null && killAura.enabled() && !requiresKillAura.value()) {
            return true;
        }

        ClientModule potion = JilatonClient.moduleManager().get("auto_pot");
        if (potion instanceof AutoPotModule autoPot && autoPot.enabled()
                && autoPot.blocksAutomaticFoodUse(client)) {
            return true;
        }
        ClientModule gapple = JilatonClient.moduleManager().get("auto_gapple");
        if (gapple instanceof AutoGappleModule autoGapple && autoGapple.enabled()
                && autoGapple.willUseNow(client)) {
            return true;
        }
        ClientModule soup = JilatonClient.moduleManager().get("auto_soup");
        if (soup instanceof AutoSoupModule autoSoup && autoSoup.enabled()
                && (autoSoup.isManagingHotbar() || autoSoup.willUseNow(client))) {
            return true;
        }
        ClientModule head = JilatonClient.moduleManager().get("auto_head");
        return head instanceof AutoHeadModule autoHead && autoHead.enabled() && autoHead.isManagingHotbar();
    }

    private AimedTarget selectTarget(Minecraft client, Player player, ItemStack projectile) {
        ClientModule killAura = JilatonClient.moduleManager().get("kill_aura");
        if (requiresKillAura.value() && killAura instanceof KillAuraModule module
                && module.currentTarget() instanceof Player target && isValidTarget(client, player, target)) {
            AimedTarget aimed = solveTarget(player, target, projectile);
            if (aimed != null && angularDistance(player, aimed.solution()) <= fieldOfView.value()) {
                return aimed;
            }
        }
        if (stickyTarget.value() && isValidTarget(client, player, currentTarget)) {
            AimedTarget aimed = solveTarget(player, currentTarget, projectile);
            if (aimed != null && angularDistance(player, aimed.solution()) <= fieldOfView.value()) {
                return aimed;
            }
        }

        AimedTarget best = null;
        double bestAngle = Double.POSITIVE_INFINITY;
        double maxDistanceSquared = range.value() * range.value();
        for (Player candidate : client.level.players()) {
            if (!isValidTarget(client, player, candidate)
                    || player.distanceToSqr(candidate) > maxDistanceSquared) {
                continue;
            }
            AimedTarget aimed = solveTarget(player, candidate, projectile);
            if (aimed == null) {
                continue;
            }
            double angle = angularDistance(player, aimed.solution());
            if (angle <= fieldOfView.value() && angle < bestAngle) {
                best = aimed;
                bestAngle = angle;
            }
        }
        return best;
    }

    private AimedTarget solveTarget(Player player, Player target, ItemStack projectile) {
        Vec3 origin = player.getEyePosition();
        Vec3 point = target.position().add(0.0, target.getBbHeight() * targetHeight.value(), 0.0);
        Vec3 velocity = target.getDeltaMovement();
        BowAimMath.Solution solution = usesBallisticAim(projectile)
                ? BowAimMath.solve(origin.x, origin.y, origin.z,
                        point.x, point.y, point.z, velocity.x, velocity.y, velocity.z,
                        projectileSpeed.value(), projectileGravity.value(), projectileDrag.value(),
                        predictMovement.value(), highArc.value()).orElse(null)
                : directSolution(origin, point);
        return solution == null ? null : new AimedTarget(target, solution);
    }

    private boolean isValidTarget(Minecraft client, Player player, Player candidate) {
        return candidate != null && candidate != player && candidate.level() == client.level
                && candidate.isAlive() && !candidate.isSpectator()
                && player.distanceToSqr(candidate) <= range.value() * range.value()
                && player.hasLineOfSight(candidate)
                && PlayerTargetFilters.allowsPlayer(client, player, candidate);
    }

    private ThrowableChoice findThrowable(Player player) {
        if (ownedSlot >= 0 && player.getInventory().getSelectedSlot() != ownedSlot) {
            manualSlotOverride = true;
            ownedSlot = -1;
            originalSlot = -1;
        }
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean anyItem = throwableType.value().equals("Any held item");
        if (anyItem) {
            if (!mainHand.isEmpty()) {
                return new ThrowableChoice(InteractionHand.MAIN_HAND, player.getInventory().getSelectedSlot(), mainHand);
            }
            return offHand.isEmpty() ? null : new ThrowableChoice(InteractionHand.OFF_HAND, -1, offHand);
        }
        if (isEggOrSnowball(offHand)) {
            return new ThrowableChoice(InteractionHand.OFF_HAND, -1, offHand);
        }
        if (isEggOrSnowball(mainHand)) {
            return new ThrowableChoice(InteractionHand.MAIN_HAND, player.getInventory().getSelectedSlot(), mainHand);
        }
        if (!selectSlotAutomatically.value() || manualSlotOverride) {
            return null;
        }

        int selected = player.getInventory().getSelectedSlot();
        int bestSlot = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int slot = 0; slot < 9; slot++) {
            if (!isEggOrSnowball(player.getInventory().getItem(slot))) {
                continue;
            }
            int distance = Math.min(Math.floorMod(slot - selected, 9), Math.floorMod(selected - slot, 9));
            if (distance < bestDistance) {
                bestSlot = slot;
                bestDistance = distance;
            }
        }
        if (bestSlot < 0) {
            return null;
        }
        if (bestSlot != selected) {
            if (originalSlot < 0) originalSlot = selected;
            ownedSlot = bestSlot;
            player.getInventory().setSelectedSlot(bestSlot);
        }
        return new ThrowableChoice(InteractionHand.MAIN_HAND, bestSlot,
                player.getInventory().getItem(bestSlot));
    }

    private boolean usesBallisticAim(ItemStack stack) {
        return switch (aimMode.value()) {
            case "Direct" -> false;
            case "Ballistic" -> true;
            default -> isEggOrSnowball(stack);
        };
    }

    private void restoreOwnedSlot(Player player) {
        if (player != null && ownedSlot >= 0 && originalSlot >= 0
                && player.getInventory().getSelectedSlot() == ownedSlot) {
            player.getInventory().setSelectedSlot(originalSlot);
        } else if (player != null && ownedSlot >= 0
                && player.getInventory().getSelectedSlot() != ownedSlot) {
            manualSlotOverride = true;
        }
        ownedSlot = -1;
        originalSlot = -1;
    }

    private void resetTargetAndSlot(Player player) {
        currentTarget = null;
        restoreOwnedSlot(player);
        manualSlotOverride = false;
    }

    private static boolean isEggOrSnowball(ItemStack stack) {
        return stack.is(Items.EGG) || stack.is(Items.SNOWBALL);
    }

    private static BowAimMath.Solution directSolution(Vec3 origin, Vec3 target) {
        Vec3 delta = target.subtract(origin);
        double horizontal = Math.hypot(delta.x, delta.z);
        if (!Double.isFinite(horizontal) || horizontal < 1.0E-6 || !Double.isFinite(delta.y)) {
            return null;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        return Float.isFinite(yaw) && Float.isFinite(pitch)
                ? new BowAimMath.Solution(yaw, pitch, 0.0, 0.0) : null;
    }

    private static double angularDistance(Player player, BowAimMath.Solution solution) {
        if (solution == null) {
            return Double.POSITIVE_INFINITY;
        }
        float yaw = RotationPolicy.wrapDegrees(solution.yaw() - player.getYRot());
        float pitch = solution.pitch() - player.getXRot();
        return Math.hypot(yaw, pitch);
    }

    private record ThrowableChoice(InteractionHand hand, int slot, ItemStack stack) {
    }

    private record AimedTarget(Player target, BowAimMath.Solution solution) {
    }
}
