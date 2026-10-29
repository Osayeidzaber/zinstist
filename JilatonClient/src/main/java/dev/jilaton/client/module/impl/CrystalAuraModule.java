package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class CrystalAuraModule extends ClientModule {
    private final NumberSetting range;
    private final NumberSetting minimumDamage;
    private final NumberSetting maximumSelfDamage;
    private final NumberSetting actionDelay;
    private final BooleanSetting placeCrystals;
    private final BooleanSetting breakCrystals;
    private final BooleanSetting autoSwitch;
    private BlockPos pendingBase;
    private UUID pendingTarget;
    private long pendingUseTick;
    private long restoreSlotTick = Long.MAX_VALUE;
    private long nextPlaceTick;
    private long nextBreakTick;
    private long nextEvaluationTick;
    private int originalSlot = -1;
    private int crystalSlot = -1;

    public CrystalAuraModule() {
        super("crystal_aura", "CrystalAura", "Places and breaks end crystals when estimated damage is favorable.",
                ModuleCategory.COMBAT);
        range = addSetting(new NumberSetting("range", "Range", 2.5, 6.0, 0.1, 4.5));
        minimumDamage = addSetting(new NumberSetting("minimum_damage", "Minimum target damage", 1.0, 20.0, 0.5, 4.0));
        maximumSelfDamage = addSetting(new NumberSetting("maximum_self_damage", "Maximum self damage", 1.0, 20.0, 0.5, 8.0));
        actionDelay = addSetting(new NumberSetting("action_delay", "Action delay (ticks)", 1.0, 5.0, 1.0, 1.0));
        placeCrystals = addSetting(new BooleanSetting("place", "Place crystals", true));
        breakCrystals = addSetting(new BooleanSetting("break", "Break crystals", true));
        autoSwitch = addSetting(new BooleanSetting("auto_switch", "Switch to crystals", true));
    }

    @Override
    protected void onEnable(Minecraft client) {
        resetState();
    }

    @Override
    protected void onDisable(Minecraft client) {
        restoreSelectedSlot(client);
        resetState();
    }

    @Override
    protected void onTick(Minecraft client) {
        Player self = client.player;
        if (self == null || client.level == null || client.gameMode == null || client.screen != null
                || self.isSpectator() || self.isCreative()) {
            cancelPendingPlacement(client);
            restoreWhenDue(client);
            return;
        }

        long gameTick = client.level.getGameTime();
        if (pendingBase != null) {
            usePendingPlacement(client, gameTick);
            return;
        }
        restoreWhenDue(client);
        if (client.options.keyUse.isDown()) {
            return;
        }
        if (gameTick < nextEvaluationTick) {
            return;
        }
        nextEvaluationTick = gameTick + 2L;

        Player target = findTarget(client, self);
        if (target == null) {
            return;
        }

        if (breakCrystals.value() && gameTick >= nextBreakTick) {
            EndCrystal crystal = findBestCrystal(client, self, target);
            if (crystal != null) {
                client.gameMode.attack(self, crystal);
                self.swing(InteractionHand.MAIN_HAND);
                nextBreakTick = gameTick + (long) actionDelay.value();
                return;
            }
        }

        if (!placeCrystals.value() || gameTick < nextPlaceTick) {
            return;
        }

        BlockPos base = findBestPlacement(client, self, target);
        if (base == null) {
            return;
        }

        int slot = findCrystalSlot(self);
        if (slot < 0) {
            return;
        }
        if (slot != self.getInventory().getSelectedSlot()) {
            if (!autoSwitch.value()) {
                return;
            }
            originalSlot = self.getInventory().getSelectedSlot();
            crystalSlot = slot;
            pendingBase = base;
            pendingTarget = target.getUUID();
            pendingUseTick = gameTick + 1L;
            self.getInventory().setSelectedSlot(slot);
            return;
        }

        place(client, self, target, base, gameTick);
    }

    private Player findTarget(Minecraft client, Player self) {
        double maximumDistance = Math.max(8.0, range.value() + 3.0);
        double bestDistance = maximumDistance * maximumDistance;
        Player best = null;
        for (Player candidate : client.level.players()) {
            if (candidate == self || !candidate.isAlive() || candidate.isSpectator() || candidate.isCreative()
                    || !PlayerTargetFilters.allowsPlayer(client, self, candidate)) {
                continue;
            }
            double distance = self.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private EndCrystal findBestCrystal(Minecraft client, Player self, Player target) {
        double rangeSquared = range.value() * range.value();
        CrystalDamageEstimator selfDamage = new CrystalDamageEstimator(client.level, self);
        CrystalDamageEstimator targetDamage = new CrystalDamageEstimator(client.level, target);
        EndCrystal bestCrystal = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (EndCrystal crystal : client.level.getEntitiesOfClass(EndCrystal.class,
                self.getBoundingBox().inflate(range.value()), Entity::isAlive)) {
            if (self.getEyePosition().distanceToSqr(crystal.position()) > rangeSquared
                    || !self.hasLineOfSight(crystal)) {
                continue;
            }
            float damageToTarget = targetDamage.estimate(crystal.position());
            float damageToSelf = selfDamage.estimate(crystal.position());
            if (!isSafe(damageToTarget, damageToSelf, self)) {
                continue;
            }
            float score = damageToTarget - damageToSelf * 0.15F;
            if (score > bestScore) {
                bestScore = score;
                bestCrystal = crystal;
            }
        }
        return bestCrystal;
    }

    private BlockPos findBestPlacement(Minecraft client, Player self, Player target) {
        double rangeSquared = range.value() * range.value();
        BlockPos targetPosition = target.blockPosition();
        CrystalDamageEstimator selfDamage = new CrystalDamageEstimator(client.level, self);
        CrystalDamageEstimator targetDamage = new CrystalDamageEstimator(client.level, target);
        BlockPos bestBase = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (int y = -1; y <= 1; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) + Math.abs(z) > 3) {
                        continue;
                    }
                    BlockPos base = targetPosition.offset(x, y, z);
                    if (!canHostCrystal(client, base)) {
                        continue;
                    }
                    Vec3 center = base.above().getCenter();
                    if (self.getEyePosition().distanceToSqr(center) > rangeSquared) {
                        continue;
                    }

                    float damageToTarget = targetDamage.estimate(center);
                    float damageToSelf = selfDamage.estimate(center);
                    if (!isSafe(damageToTarget, damageToSelf, self)) {
                        continue;
                    }
                    float score = damageToTarget - damageToSelf * 0.15F;
                    if (score > bestScore) {
                        bestScore = score;
                        bestBase = base;
                    }
                }
            }
        }
        return bestBase;
    }

    private boolean isSafe(float targetDamage, float selfDamage, Player self) {
        float effectiveHealth = self.getHealth() + self.getAbsorptionAmount();
        return targetDamage >= minimumDamage.value()
                && selfDamage <= maximumSelfDamage.value()
                && selfDamage * 1.15F + 2.0F < effectiveHealth;
    }

    private boolean canHostCrystal(Minecraft client, BlockPos base) {
        if (!client.level.getBlockState(base).is(Blocks.OBSIDIAN)
                && !client.level.getBlockState(base).is(Blocks.BEDROCK)) {
            return false;
        }
        BlockPos above = base.above();
        if (!client.level.isEmptyBlock(above) || !client.level.isEmptyBlock(above.above())) {
            return false;
        }
        AABB space = new AABB(base.getX(), base.getY() + 1.0, base.getZ(),
                base.getX() + 1.0, base.getY() + 3.0, base.getZ() + 1.0);
        return client.level.getEntitiesOfClass(Entity.class, space, Entity::isAlive).isEmpty();
    }

    private static int findCrystalSlot(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(Items.END_CRYSTAL)) {
                return slot;
            }
        }
        return -1;
    }

    private void usePendingPlacement(Minecraft client, long gameTick) {
        Player self = client.player;
        if (self == null || client.level == null || client.gameMode == null || gameTick < pendingUseTick) {
            return;
        }
        if (self.getInventory().getSelectedSlot() != crystalSlot || !canHostCrystal(client, pendingBase)) {
            cancelPendingPlacement(client);
            return;
        }
        Player target = client.level.getPlayerByUUID(pendingTarget);
        if (target != null && target.isAlive() && PlayerTargetFilters.allowsPlayer(client, self, target)) {
            place(client, self, target, pendingBase, gameTick);
        }
        clearPendingPlacement();
        restoreSlotTick = gameTick + 1L;
    }

    private void place(Minecraft client, Player self, Player target, BlockPos base, long gameTick) {
        if (!canHostCrystal(client, base)) {
            return;
        }
        Vec3 center = base.above().getCenter();
        if (self.getEyePosition().distanceToSqr(center) > range.value() * range.value()) {
            return;
        }
        CrystalDamageEstimator selfDamage = new CrystalDamageEstimator(client.level, self);
        CrystalDamageEstimator targetDamage = new CrystalDamageEstimator(client.level, target);
        if (!isSafe(targetDamage.estimate(center), selfDamage.estimate(center), self)) {
            return;
        }

        Vec3 hitPosition = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(hitPosition, Direction.UP, base, false);
        InteractionResult result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        if (result.consumesAction()) {
            self.swing(InteractionHand.MAIN_HAND);
        }
        nextPlaceTick = gameTick + (long) actionDelay.value();
    }

    private void restoreWhenDue(Minecraft client) {
        if (restoreSlotTick > (client.level == null ? Long.MAX_VALUE : client.level.getGameTime())) {
            return;
        }
        restoreSelectedSlot(client);
    }

    private void restoreSelectedSlot(Minecraft client) {
        if (client.player != null && originalSlot >= 0 && crystalSlot >= 0
                && client.player.getInventory().getSelectedSlot() == crystalSlot) {
            client.player.getInventory().setSelectedSlot(originalSlot);
        }
        originalSlot = -1;
        crystalSlot = -1;
        restoreSlotTick = Long.MAX_VALUE;
    }

    private void cancelPendingPlacement(Minecraft client) {
        if (pendingBase != null) {
            clearPendingPlacement();
            restoreSlotTick = 0L;
        }
        if (client.level != null) {
            restoreWhenDue(client);
        }
    }

    private void clearPendingPlacement() {
        pendingBase = null;
        pendingTarget = null;
        pendingUseTick = 0L;
    }

    private void resetState() {
        clearPendingPlacement();
        restoreSlotTick = Long.MAX_VALUE;
        nextPlaceTick = 0L;
        nextBreakTick = 0L;
        nextEvaluationTick = 0L;
        originalSlot = -1;
        crystalSlot = -1;
    }
}
