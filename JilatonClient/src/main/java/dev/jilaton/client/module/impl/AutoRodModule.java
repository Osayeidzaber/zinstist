package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.rotation.RotationAngles;
import dev.jilaton.client.rotation.RotationPriorities;
import dev.jilaton.client.rotation.RotationPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Target-gated rod cast/reel sequence using the shared visible-rotation arbiter. */
public final class AutoRodModule extends ClientModule {
    private final NumberSetting minimumRange;
    private final NumberSetting maximumRange;
    private final NumberSetting minimumSelfHealth;
    private final NumberSetting minimumTargetHealth;
    private final NumberSetting aimTolerance;
    private final NumberSetting fieldOfView;
    private final NumberSetting turnSpeed;
    private final NumberSetting maximumNearbyEnemies;
    private final NumberSetting hitTimeout;
    private final NumberSetting cooldownMinimum;
    private final NumberSetting cooldownMaximum;
    private final NumberSetting slotRestoreDelay;
    private final BooleanSetting requireAttackKey;
    private final BooleanSetting pauseWhileUsingItem;
    private final BooleanSetting pullOnOutOfRange;
    private final BooleanSetting autoSwitch;

    private UUID pendingTarget;
    private FishingHook pendingHook;
    private long pullDeadline;
    private long nextCastTick;
    private long restoreSlotTick = Long.MAX_VALUE;
    private int originalSlot = -1;
    private int rodSlot = -1;
    private boolean offhandRod;

    public AutoRodModule() {
        super("auto_rod", "AutoRod", "Casts and reels a fishing rod against a focused opponent.",
                ModuleCategory.COMBAT);
        minimumRange = addSetting(new NumberSetting("minimum_range", "Minimum range", 1.0, 10.0, 0.1, 3.5));
        maximumRange = addSetting(new NumberSetting("maximum_range", "Maximum range", 2.0, 10.0, 0.1, 5.0));
        minimumSelfHealth = addSetting(new NumberSetting("minimum_self_health", "Minimum self health", 1.0,
                20.0, 0.5, 10.0));
        minimumTargetHealth = addSetting(new NumberSetting("minimum_target_health", "Minimum target health",
                1.0, 20.0, 0.5, 4.0));
        aimTolerance = addSetting(new NumberSetting("aim_tolerance", "Aim tolerance", 1.0, 20.0, 0.5, 5.0));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Target field of view", 5.0, 180.0,
                1.0, 90.0));
        turnSpeed = addSetting(new NumberSetting("turn_speed", "Aim turn speed", 1.0, 30.0, 0.5, 8.0));
        maximumNearbyEnemies = addSetting(new NumberSetting("maximum_nearby_enemies", "Maximum nearby enemies",
                0.0, 10.0, 1.0, 1.0));
        hitTimeout = addSetting(new NumberSetting("hit_timeout", "Hit timeout (ticks)", 5.0, 200.0, 1.0, 30.0));
        cooldownMinimum = addSetting(new NumberSetting("cooldown_minimum", "Minimum cooldown (ticks)",
                1.0, 50.0, 1.0, 4.0));
        cooldownMaximum = addSetting(new NumberSetting("cooldown_maximum", "Maximum cooldown (ticks)",
                1.0, 50.0, 1.0, 8.0));
        slotRestoreDelay = addSetting(new NumberSetting("slot_restore_delay", "Slot restore delay (ticks)",
                0.0, 20.0, 1.0, 0.0));
        requireAttackKey = addSetting(new BooleanSetting("require_attack_key", "Require attack key", true));
        pauseWhileUsingItem = addSetting(new BooleanSetting("pause_while_using_item", "Pause while using item", true));
        pullOnOutOfRange = addSetting(new BooleanSetting("pull_on_out_of_range", "Pull on target leaving range", true));
        autoSwitch = addSetting(new BooleanSetting("auto_switch", "Switch to fishing rod", true));
    }

    @Override
    protected void onEnable(Minecraft client) {
        clearSequence();
        nextCastTick = 0L;
    }

    @Override
    protected void onDisable(Minecraft client) {
        restoreRodSlot(client);
        clearSequence();
        nextCastTick = 0L;
    }

    @Override
    protected void onTick(Minecraft client) {
        Player self = client.player;
        if (self == null || client.level == null || client.gameMode == null || client.screen != null
                || self.isSpectator() || self.isCreative()) {
            restoreRodSlot(client);
            clearSequence();
            return;
        }

        long tick = client.level.getGameTime();
        restoreWhenDue(client, tick);
        if (pendingTarget != null) {
            handlePendingCast(client, self, tick);
            return;
        }
        if (tick < nextCastTick || restoreSlotTick != Long.MAX_VALUE
                || !mayStart(client, self)) {
            return;
        }

        Player target = findTarget(client, self);
        if (target == null) {
            return;
        }
        RotationAngles aim = aimRotation(self, target);
        JilatonClient.moduleManager().rotationManager().request(id(), RotationPriorities.AUTO_ROD,
                aim.yaw(), aim.pitch(), (float) turnSpeed.value());
        if (RotationPolicy.angularError(self.getYRot(), self.getXRot(), aim) > aimTolerance.value()) {
            return;
        }

        int slot = findRodSlot(self);
        boolean inOffhand = self.getOffhandItem().is(Items.FISHING_ROD);
        if (slot < 0 && !inOffhand) {
            return;
        }
        int selected = self.getInventory().getSelectedSlot();
        if (slot >= 0 && selected != slot) {
            if (!autoSwitch.value() || hotbarReserved(client)) {
                return;
            }
            originalSlot = selected;
            rodSlot = slot;
            self.getInventory().setSelectedSlot(slot);
        } else if (slot >= 0) {
            rodSlot = slot;
            originalSlot = selected;
        } else {
            originalSlot = selected;
            offhandRod = true;
        }

        InteractionHand hand = offhandRod ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        InteractionResult result = client.gameMode.useItem(self, hand);
        if (!result.consumesAction()) {
            restoreRodSlot(client);
            return;
        }
        self.swing(hand);
        pendingTarget = target.getUUID();
        pendingHook = findOwnedHook(client, self);
        pullDeadline = tick + (long) hitTimeout.value();
    }

    private boolean mayStart(Minecraft client, Player self) {
        if (pauseWhileUsingItem.value() && self.isUsingItem()) {
            return false;
        }
        int nearby = 0;
        double maximumDistance = maximumRange.value() * maximumRange.value();
        for (Player candidate : client.level.players()) {
            if (candidate != self && candidate.isAlive() && !candidate.isSpectator() && !candidate.isCreative()
                    && PlayerTargetFilters.allowsPlayer(client, self, candidate)
                    && self.distanceToSqr(candidate) <= maximumDistance) {
                nearby++;
            }
        }
        boolean rodAvailable = findRodSlot(self) >= 0 || self.getOffhandItem().is(Items.FISHING_ROD);
        return AutoRodPolicy.mayStart(self.getHealth() + self.getAbsorptionAmount(),
                (float) minimumSelfHealth.value(), nearby, (int) maximumNearbyEnemies.value(), rodAvailable,
                requireAttackKey.value(), client.options != null && client.options.keyAttack.isDown(),
                self.isUsingItem(), pauseWhileUsingItem.value());
    }

    private Player findTarget(Minecraft client, Player self) {
        Player best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (Player candidate : client.level.players()) {
            if (candidate == self || !candidate.isAlive() || candidate.isSpectator() || candidate.isCreative()
                    || !PlayerTargetFilters.allowsPlayer(client, self, candidate)) {
                continue;
            }
            double distance = self.distanceToSqr(candidate);
            if (distance < minimumRange.value() * minimumRange.value()
                    || distance > maximumRange.value() * maximumRange.value()) {
                continue;
            }
            float aimError = aimErrorDegrees(self, candidate);
            // Cheap range/health/aim checks first; the line-of-sight raycast only runs for survivors.
            if (!AutoRodPolicy.targetEligible(distance, minimumRange.value(), maximumRange.value(),
                    candidate.getHealth(), (float) minimumTargetHealth.value(), true,
                    aimError, (float) fieldOfView.value()) || !self.hasLineOfSight(candidate)) {
                continue;
            }
            double score = aimError * 4.0 + distance;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private void handlePendingCast(Minecraft client, Player self, long tick) {
        Player target = client.level.getPlayerByUUID(pendingTarget);
        if (target == null || !target.isAlive()) {
            pullAndFinish(client, self, tick);
            return;
        }
        if (!rodStillOwned(self)) {
            clearSequence();
            return;
        }
        if (pendingHook == null || pendingHook.isRemoved()) {
            pendingHook = findOwnedHook(client, self);
        }
        boolean hooked = pendingHook != null && pendingHook.getHookedIn() == target;
        boolean inRange = self.distanceToSqr(target) >= minimumRange.value() * minimumRange.value()
                && self.distanceToSqr(target) <= maximumRange.value() * maximumRange.value();
        boolean timedOut = tick >= pullDeadline;
        if (AutoRodPolicy.shouldPull(hooked, timedOut, pullOnOutOfRange.value(), inRange)) {
            pullAndFinish(client, self, tick);
        }
    }

    private void pullAndFinish(Minecraft client, Player self, long tick) {
        if (rodStillOwned(self)) {
            InteractionHand hand = offhandRod ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            InteractionResult result = client.gameMode.useItem(self, hand);
            if (result.consumesAction()) {
                self.swing(hand);
            }
        }
        nextCastTick = tick + randomCooldown();
        restoreSlotTick = tick + (long) slotRestoreDelay.value();
        if (restoreSlotTick <= tick) {
            restoreRodSlot(client);
        }
        clearSequence();
    }

    private long randomCooldown() {
        int minimum = (int) Math.min(cooldownMinimum.value(), cooldownMaximum.value());
        int maximum = (int) Math.max(cooldownMinimum.value(), cooldownMaximum.value());
        return ThreadLocalRandom.current().nextInt(minimum, maximum + 1);
    }

    private int findRodSlot(Player player) {
        int selected = player.getInventory().getSelectedSlot();
        if (player.getInventory().getItem(selected).is(Items.FISHING_ROD)) {
            return selected;
        }
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(Items.FISHING_ROD)) {
                return slot;
            }
        }
        return -1;
    }

    private FishingHook findOwnedHook(Minecraft client, Player owner) {
        return client.level.getEntitiesOfClass(FishingHook.class,
                        owner.getBoundingBox().inflate(maximumRange.value() + 6.0), hook -> hook.getOwner() == owner)
                .stream().findFirst().orElse(null);
    }

    private boolean rodStillOwned(Player player) {
        if (offhandRod) {
            return player.getOffhandItem().is(Items.FISHING_ROD);
        }
        return rodSlot >= 0 && player.getInventory().getSelectedSlot() == rodSlot
                && player.getInventory().getItem(rodSlot).is(Items.FISHING_ROD);
    }

    private boolean hotbarReserved(Minecraft client) {
        ClientModule pot = JilatonClient.moduleManager().get("auto_pot");
        if (pot instanceof AutoPotModule autoPot && autoPot.enabled()
                && autoPot.blocksAutomaticFoodUse(client)) {
            return true;
        }
        ClientModule gapple = JilatonClient.moduleManager().get("auto_gapple");
        return gapple instanceof AutoGappleModule autoGapple && autoGapple.enabled()
                && (autoGapple.isManagingHotbar() || autoGapple.willUseNow(client));
    }

    private void restoreWhenDue(Minecraft client, long tick) {
        if (restoreSlotTick != Long.MAX_VALUE && tick >= restoreSlotTick) {
            restoreRodSlot(client);
        }
    }

    private void restoreRodSlot(Minecraft client) {
        if (client.player != null && originalSlot >= 0 && rodSlot >= 0
                && client.player.getInventory().getSelectedSlot() == rodSlot) {
            client.player.getInventory().setSelectedSlot(originalSlot);
        }
        originalSlot = -1;
        rodSlot = -1;
        offhandRod = false;
        restoreSlotTick = Long.MAX_VALUE;
    }

    private void clearSequence() {
        pendingTarget = null;
        pendingHook = null;
        pullDeadline = 0L;
    }

    public boolean isManagingHotbar() {
        return originalSlot >= 0 || offhandRod || pendingTarget != null;
    }

    static float aimErrorDegrees(Player self, Player target) {
        RotationAngles desired = aimRotation(self, target);
        return AutoRodPolicy.aimError(self.getYRot(), self.getXRot(), desired.yaw(), desired.pitch());
    }

    private static RotationAngles aimRotation(Player self, Player target) {
        Vec3 direction = target.getBoundingBox().getCenter().subtract(self.getEyePosition());
        return RotationAngles.lookingAt(direction.x, direction.y, direction.z);
    }
}
