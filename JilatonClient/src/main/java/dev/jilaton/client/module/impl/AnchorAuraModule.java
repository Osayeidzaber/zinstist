package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Anubis-style respawn-anchor planner adapted to Jilaton's tick/settings system. */
public final class AnchorAuraModule extends ClientModule {
    private static final double ANCHOR_DIAMETER = 10.0;
    private static final int SEARCH_INTERVAL_TICKS = 3;
    private static final int SERVER_UPDATE_TIMEOUT = 6;

    private final NumberSetting range;
    private final NumberSetting minimumDamage;
    private final NumberSetting maximumSelfDamage;
    private final NumberSetting turnSpeed;
    private final NumberSetting actionDelay;
    private final BooleanSetting autoSwitch;
    private final BooleanSetting switchBack;

    private Plan plan;
    private long nextEvaluationTick;

    public AnchorAuraModule() {
        super("anchor_aura", "AnchorAura", "Finds safe respawn-anchor blasts and stages place, charge, and detonate actions.",
                ModuleCategory.COMBAT);
        range = addSetting(new NumberSetting("range", "Anchor range", 2.0, 6.0, 0.1, 4.5));
        minimumDamage = addSetting(new NumberSetting("min_damage", "Minimum target damage", 0.0, 20.0, 0.5, 6.0));
        maximumSelfDamage = addSetting(new NumberSetting("max_self_damage", "Maximum self damage", 0.0, 20.0, 0.5, 6.0));
        turnSpeed = addSetting(new NumberSetting("turn_speed", "Turn speed", 1.0, 30.0, 0.5, 8.0));
        actionDelay = addSetting(new NumberSetting("action_delay", "Action delay (ticks)", 0.0, 5.0, 1.0, 0.0));
        autoSwitch = addSetting(new BooleanSetting("auto_switch", "Switch required items", true));
        switchBack = addSetting(new BooleanSetting("switch_back", "Restore selected slot", true));
    }

    @Override
    protected void onEnable(Minecraft client) {
        reset();
    }

    @Override
    protected void onDisable(Minecraft client) {
        finish(client, true);
        nextEvaluationTick = 0L;
    }

    @Override
    protected void onTick(Minecraft client) {
        Player self = client.player;
        Level level = client.level;
        if (self == null || level == null || client.gameMode == null || client.screen != null
                || self.isSpectator() || self.isCreative()
                || level.environmentAttributes().getDimensionValue(
                        net.minecraft.world.attribute.EnvironmentAttributes.RESPAWN_ANCHOR_WORKS)
                || hasRotationOrInventoryConflict(client)) {
            if (plan != null) {
                finish(client, true);
            }
            return;
        }

        if (plan != null) {
            advancePlan(client, self, level.getGameTime());
            return;
        }
        if (level.getGameTime() < nextEvaluationTick || self.isUsingItem()
                || client.options.keyUse.isDown() || client.options.keyAttack.isDown()) {
            return;
        }
        nextEvaluationTick = level.getGameTime() + SEARCH_INTERVAL_TICKS;
        plan = findPlan(client, self, level);
        if (plan != null) {
            plan.originalSlot = self.getInventory().getSelectedSlot();
            plan.ownedSlot = plan.originalSlot;
            plan.nextActionTick = level.getGameTime();
        }
    }

    private boolean hasRotationOrInventoryConflict(Minecraft client) {
        ClientModule crystal = JilatonClient.moduleManager().get("crystal_aura");
        ClientModule aura = JilatonClient.moduleManager().get("kill_aura");
        ClientModule aim = JilatonClient.moduleManager().get("aim_assist");
        if (crystal != null && crystal.enabled() || aura != null && aura.enabled()
                || aim != null && aim.enabled()) {
            return true;
        }
        ClientModule autoAnchor = JilatonClient.moduleManager().get("auto_anchor");
        if (autoAnchor instanceof AutoAnchorModule manual && manual.busy()) {
            return true;
        }
        ClientModule autoPot = JilatonClient.moduleManager().get("auto_pot");
        if (autoPot instanceof AutoPotModule pot && pot.isPreparingThrow()) {
            return true;
        }
        ClientModule autoTotem = JilatonClient.moduleManager().get("auto_totem");
        return autoTotem != null && autoTotem.enabled() && client.player != null
                && !client.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                && hasInventoryItem(client.player, Items.TOTEM_OF_UNDYING);
    }

    private Plan findPlan(Minecraft client, Player self, Level level) {
        List<Player> enemies = level.players().stream()
                .map(candidate -> (Player) candidate)
                .filter(candidate -> candidate != self && candidate.isAlive() && !candidate.isSpectator()
                        && !candidate.isCreative() && self.distanceToSqr(candidate) <= square(range.value() + 3.0)
                        && PlayerTargetFilters.allowsPlayer(client, self, candidate))
                .sorted(Comparator.comparingDouble(self::distanceToSqr))
                .limit(8)
                .toList();
        if (enemies.isEmpty()) {
            return null;
        }

        int anchorSlot = findHotbar(self, Items.RESPAWN_ANCHOR);
        int glowstoneSlot = findHotbar(self, Items.GLOWSTONE);
        int detonatorSlot = findDetonator(self);

        List<Player> friends = level.players().stream()
                .map(friend -> (Player) friend)
                .filter(friend -> friend != self && friend.isAlive() && FriendManager.contains(friend))
                .toList();
        Candidate existing = findExistingAnchor(self, level, enemies, friends);
        if (existing != null) {
            BlockState state = level.getBlockState(existing.pos);
            boolean charged = state.getValue(RespawnAnchorBlock.CHARGE) > 0;
            int requiredSlot = charged ? detonatorSlot : glowstoneSlot;
            if (requiredSlot < 0 || !charged && detonatorSlot < 0) {
                return null;
            }
            if (!autoSwitch.value() && (requiredSlot != self.getInventory().getSelectedSlot()
                    || !charged && detonatorSlot != requiredSlot)) {
                return null;
            }
            return new Plan(existing.pos, null, existing.target.getUUID(),
                    charged ? Stage.AIM_DETONATE : Stage.AIM_CHARGE,
                    requiredSlot, existing.score, level);
        }

        if (anchorSlot < 0 || glowstoneSlot < 0 || detonatorSlot < 0) {
            return null;
        }
        Candidate placement = findPlacement(self, level, enemies, friends);
        if (placement == null) {
            return null;
        }
        if (!autoSwitch.value() && anchorSlot != self.getInventory().getSelectedSlot()) {
            return null;
        }
        if (!autoSwitch.value() && (glowstoneSlot != anchorSlot || detonatorSlot != anchorSlot)) {
            return null;
        }
        return new Plan(placement.pos, placement.hit, placement.target.getUUID(), Stage.AIM_PLACE,
                anchorSlot, placement.score, level);
    }

    private Candidate findExistingAnchor(Player self, Level level, List<Player> enemies, List<Player> friends) {
        Set<BlockPos> seen = new HashSet<>();
        Candidate best = null;
        double maximumRangeSqr = Math.min(range.value(), self.blockInteractionRange())
                * Math.min(range.value(), self.blockInteractionRange());
        for (Player enemy : enemies) {
            BlockPos feet = enemy.blockPosition();
            for (int y = -2; y <= 3; y++) {
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        BlockPos pos = feet.offset(x, y, z);
                        if (!seen.add(pos) || !level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)
                                || self.getEyePosition().distanceToSqr(pos.getCenter()) > maximumRangeSqr) {
                            continue;
                        }
                        Candidate scored = scoreCell(level, self, enemies, friends, pos, pos);
                        if (scored != null && (best == null || scored.score > best.score)) {
                            best = scored;
                        }
                    }
                }
            }
        }
        return best;
    }

    private Candidate findPlacement(Player self, Level level, List<Player> enemies, List<Player> friends) {
        Set<BlockPos> seen = new HashSet<>();
        List<PositionBound> candidates = new ArrayList<>();
        DamageContext damage = new DamageContext(level, self, enemies, friends, null);
        double maximumRangeSqr = Math.min(range.value(), self.blockInteractionRange())
                * Math.min(range.value(), self.blockInteractionRange());
        for (Player enemy : enemies) {
            BlockPos feet = enemy.blockPosition();
            for (int y = -1; y <= 2; y++) {
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        BlockPos cell = feet.offset(x, y, z);
                        if (!seen.add(cell) || !level.isEmptyBlock(cell)
                                || self.getEyePosition().distanceToSqr(cell.getCenter()) > maximumRangeSqr
                                || intersectsEntity(level, cell)) {
                            continue;
                        }
                        BlockHitResult supportHit = supportHit(level, cell);
                        if (supportHit == null) {
                            continue;
                        }
                        float upper = 0.0F;
                        for (Player target : enemies) {
                            float targetUpperBound = damage.enemies.get(target).upperBound(cell.getCenter());
                            if (targetUpperBound >= neededDamage(target)) {
                                upper = Math.max(upper, targetUpperBound);
                            }
                        }
                        if (upper > 0.0F) {
                            candidates.add(new PositionBound(cell, supportHit, upper));
                        }
                    }
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(PositionBound::upperBound).reversed());

        Candidate best = null;
        for (PositionBound candidate : candidates) {
            if (best != null && candidate.upperBound <= best.score) {
                break;
            }
            Candidate scored = scoreCell(self, enemies, damage, candidate.pos);
            if (scored != null && (best == null || scored.score > best.score)) {
                scored.hit = candidate.hit;
                best = scored;
            }
        }
        return best;
    }

    private Candidate scoreCell(Level level, Player self, List<Player> enemies, List<Player> friends,
                                BlockPos pos, BlockPos ignoredSourceBlock) {
        return scoreCell(self, enemies, new DamageContext(level, self, enemies, friends, ignoredSourceBlock), pos);
    }

    private Candidate scoreCell(Player self, List<Player> enemies, DamageContext damage, BlockPos pos) {
        Vec3 center = pos.getCenter();
        float selfDamage = damage.self.estimate(center);
        boolean hasTotem = self.getMainHandItem().is(Items.TOTEM_OF_UNDYING)
                || self.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                || findHotbar(self, Items.TOTEM_OF_UNDYING) >= 0;
        if (!AnchorSafetyPolicy.allowsDetonation(selfDamage, (float) maximumSelfDamage.value(),
                self.getHealth(), self.getAbsorptionAmount(), hasTotem)) {
            return null;
        }

        for (CrystalDamageEstimator friend : damage.friends) {
            if (friend.estimate(center) > 0.05F) {
                return null;
            }
        }

        Player bestTarget = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (Player enemy : enemies) {
            float targetDamage = damage.enemies.get(enemy).estimate(center);
            if (targetDamage <= 0.0F || targetDamage < neededDamage(enemy)) {
                continue;
            }
            float score = targetDamage - selfDamage * 0.5F;
            if (score > bestScore) {
                bestTarget = enemy;
                bestScore = score;
            }
        }
        return bestTarget == null ? null : new Candidate(pos.immutable(), null, bestTarget, bestScore);
    }

    private void advancePlan(Minecraft client, Player self, long now) {
        Plan current = plan;
        if (current == null || self.getInventory().getSelectedSlot() != current.ownedSlot
                || client.level == null || client.level != current.level) {
            finish(client, false);
            return;
        }
        if (now < current.nextActionTick) {
            return;
        }
        Player target = client.level.getPlayerByUUID(current.targetId);
        if (target == null || !target.isAlive() || !PlayerTargetFilters.allowsPlayer(client, self, target)
                || self.distanceToSqr(target) > square(range.value() + 3.0)) {
            finish(client, true);
            return;
        }

        BlockState state = client.level.getBlockState(current.pos);
        switch (current.stage) {
            case AIM_PLACE -> act(client, self, current, current.requiredSlot,
                    current.placementHit, Stage.WAIT_PLACE, now);
            case WAIT_PLACE -> {
                if (isAnchor(state)) {
                    current.stage = Stage.AIM_CHARGE;
                    current.requiredSlot = findHotbar(self, Items.GLOWSTONE);
                    current.nextActionTick = now + (long) actionDelay.value();
                } else if (now >= current.deadline) {
                    finish(client, true);
                }
            }
            case AIM_CHARGE -> {
                if (!isAnchor(state)) {
                    finish(client, true);
                } else if (state.getValue(RespawnAnchorBlock.CHARGE) > 0) {
                    current.stage = Stage.AIM_DETONATE;
                    current.requiredSlot = findDetonator(self);
                    current.nextActionTick = now + (long) actionDelay.value();
                } else {
                    act(client, self, current, current.requiredSlot, anchorHit(current.pos),
                            Stage.WAIT_CHARGE, now);
                }
            }
            case WAIT_CHARGE -> {
                if (!isAnchor(state)) {
                    finish(client, true);
                } else if (state.getValue(RespawnAnchorBlock.CHARGE) > 0) {
                    current.stage = Stage.AIM_DETONATE;
                    current.requiredSlot = findDetonator(self);
                    current.nextActionTick = now + (long) actionDelay.value();
                } else if (now >= current.deadline) {
                    finish(client, true);
                }
            }
            case AIM_DETONATE -> {
                if (!isAnchor(state) || state.getValue(RespawnAnchorBlock.CHARGE) == 0
                        || !stillSafe(client, self, current.pos)) {
                    finish(client, true);
                } else {
                    current.requiredSlot = findDetonator(self);
                    act(client, self, current, current.requiredSlot, anchorHit(current.pos), null, now);
                }
            }
        }
    }

    private void act(Minecraft client, Player self, Plan current, int slot, BlockHitResult hit,
                     Stage after, long now) {
        if (hit == null || slot < 0 || !select(client, self, slot)) {
            finish(client, true);
            return;
        }
        if (!rotateToward(self, hit.getLocation())) {
            current.nextActionTick = now + 1L;
            return;
        }

        InteractionResult result = AutomatedBlockUse.use(client, (LocalPlayer) self, hit);
        if (result.consumesAction()) {
            self.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        if (after == null) {
            finish(client, true);
            return;
        }
        current.stage = after;
        current.deadline = now + SERVER_UPDATE_TIMEOUT;
        current.nextActionTick = now + 1L;
    }

    private boolean select(Minecraft client, Player player, int slot) {
        if (player.getInventory().getSelectedSlot() != slot && !autoSwitch.value()) {
            return false;
        }
        if (player.getInventory().getSelectedSlot() != slot) {
            player.getInventory().setSelectedSlot(slot);
            plan.ownedSlot = slot;
        }
        return true;
    }

    private boolean rotateToward(Player player, Vec3 point) {
        Vec3 direction = point.subtract(player.getEyePosition());
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (direction.lengthSqr() < 1.0E-8) {
            return true;
        }
        float desiredYaw = (float) (Math.atan2(direction.z, direction.x) * 180.0 / Math.PI) - 90.0F;
        float desiredPitch = (float) (-Math.atan2(direction.y, horizontal) * 180.0 / Math.PI);
        float yawDelta = Mth.wrapDegrees(desiredYaw - player.getYRot());
        float pitchDelta = desiredPitch - player.getXRot();
        player.setYRot(Mth.wrapDegrees(player.getYRot() + Mth.clamp(yawDelta,
                (float) -turnSpeed.value(), (float) turnSpeed.value())));
        player.setXRot(Mth.clamp(player.getXRot() + Mth.clamp(pitchDelta,
                (float) -turnSpeed.value(), (float) turnSpeed.value()), -90.0F, 90.0F));
        return Math.abs(Mth.wrapDegrees(desiredYaw - player.getYRot())) < 0.6F
                && Math.abs(desiredPitch - player.getXRot()) < 0.6F;
    }

    private boolean stillSafe(Minecraft client, Player self, BlockPos pos) {
        return scoreCell(client.level, self,
                client.level.players().stream()
                        .map(candidate -> (Player) candidate)
                        .filter(candidate -> candidate != self && candidate.isAlive() && !candidate.isSpectator()
                                && !candidate.isCreative() && PlayerTargetFilters.allowsPlayer(client, self, candidate))
                        .toList(),
                client.level.players().stream()
                        .map(friend -> (Player) friend)
                        .filter(friend -> friend != self && friend.isAlive() && FriendManager.contains(friend))
                        .toList(), pos, pos) != null;
    }

    private float neededDamage(Player enemy) {
        return (float) Math.min(minimumDamage.value(), enemy.getHealth() + enemy.getAbsorptionAmount());
    }

    private static boolean intersectsEntity(Level level, BlockPos pos) {
        AABB box = new AABB(pos);
        return !level.getEntitiesOfClass(Entity.class, box,
                entity -> entity.isAlive() && !entity.isSpectator()).isEmpty();
    }

    private static BlockHitResult supportHit(Level level, BlockPos anchorCell) {
        BlockPos supportPos = anchorCell.below();
        BlockState support = level.getBlockState(supportPos);
        if (!support.isCollisionShapeFullBlock(level, supportPos)) {
            return null;
        }
        double top = support.getCollisionShape(level, supportPos).bounds().maxY + supportPos.getY();
        Vec3 point = new Vec3(supportPos.getX() + 0.5, top - 1.0E-3, supportPos.getZ() + 0.5);
        return new BlockHitResult(point, Direction.UP, supportPos, false);
    }

    private static BlockHitResult anchorHit(BlockPos pos) {
        return AutomatedBlockUse.hitOn(pos, Direction.UP, 0.01);
    }

    private static boolean isAnchor(BlockState state) {
        return state.is(Blocks.RESPAWN_ANCHOR);
    }

    private static int findHotbar(Player player, Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    private static int findDetonator(Player player) {
        int totem = findHotbar(player, Items.TOTEM_OF_UNDYING);
        if (totem >= 0) {
            return totem;
        }
        int selected = player.getInventory().getSelectedSlot();
        if (!player.getInventory().getItem(selected).is(Items.GLOWSTONE)) {
            return selected;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && !stack.is(Items.GLOWSTONE) && !stack.is(Items.RESPAWN_ANCHOR)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean hasInventoryItem(Player player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static double square(double value) {
        return value * value;
    }

    private void finish(Minecraft client, boolean restore) {
        if (plan != null && restore && switchBack.value() && client.player != null
                && plan.originalSlot >= 0 && plan.ownedSlot >= 0
                && client.player.getInventory().getSelectedSlot() == plan.ownedSlot) {
            client.player.getInventory().setSelectedSlot(plan.originalSlot);
        }
        plan = null;
        if (client.level != null) {
            nextEvaluationTick = client.level.getGameTime() + SEARCH_INTERVAL_TICKS;
        }
    }

    private void reset() {
        plan = null;
        nextEvaluationTick = 0L;
    }

    private static final class Plan {
        private final BlockPos pos;
        private final BlockHitResult placementHit;
        private final UUID targetId;
        private final Level level;
        private final float score;
        private Stage stage;
        private int requiredSlot;
        private int originalSlot = -1;
        private int ownedSlot = -1;
        private long nextActionTick;
        private long deadline;

        private Plan(BlockPos pos, BlockHitResult placementHit, UUID targetId,
                     Stage stage, int requiredSlot, float score, Level level) {
            this.pos = pos.immutable();
            this.placementHit = placementHit;
            this.targetId = targetId;
            this.stage = stage;
            this.requiredSlot = requiredSlot;
            this.score = score;
            this.level = level;
        }
    }

    private static final class DamageContext {
        private final CrystalDamageEstimator self;
        private final Map<Player, CrystalDamageEstimator> enemies = new java.util.IdentityHashMap<>();
        private final List<CrystalDamageEstimator> friends = new ArrayList<>();

        private DamageContext(Level level, Player self, List<Player> enemies, List<Player> friends,
                              BlockPos ignoredSourceBlock) {
            this.self = new CrystalDamageEstimator(level, self, ANCHOR_DIAMETER, ignoredSourceBlock);
            for (Player enemy : enemies) {
                this.enemies.put(enemy,
                        new CrystalDamageEstimator(level, enemy, ANCHOR_DIAMETER, ignoredSourceBlock));
            }
            for (Player friend : friends) {
                this.friends.add(new CrystalDamageEstimator(level, friend, ANCHOR_DIAMETER, ignoredSourceBlock));
            }
        }
    }

    private static final class Candidate {
        private final BlockPos pos;
        private BlockHitResult hit;
        private final Player target;
        private final float score;

        private Candidate(BlockPos pos, BlockHitResult hit, Player target, float score) {
            this.pos = pos;
            this.hit = hit;
            this.target = target;
            this.score = score;
        }
    }

    private record PositionBound(BlockPos pos, BlockHitResult hit, float upperBound) {
    }

    private enum Stage {
        AIM_PLACE,
        WAIT_PLACE,
        AIM_CHARGE,
        WAIT_CHARGE,
        AIM_DETONATE
    }
}
