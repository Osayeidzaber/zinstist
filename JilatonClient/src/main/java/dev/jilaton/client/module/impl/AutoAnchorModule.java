package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class AutoAnchorModule extends ClientModule {
    private final NumberSetting actionDelay;
    private final NumberSetting maximumSelfDamage;
    private final BooleanSetting switchBack;

    private Step step = Step.IDLE;
    private Level activeLevel;
    private BlockPos anchorPos;
    private BlockHitResult placementHit;
    private long nextActionTick;
    private long waitDeadline;
    private int originalSlot = -1;
    private int ownedSlot = -1;
    private boolean automatedUse;

    public AutoAnchorModule() {
        super("auto_anchor", "AutoAnchor", "Places, charges and safely detonates a respawn anchor after a right-click.",
                ModuleCategory.COMBAT);
        actionDelay = addSetting(new NumberSetting("delay", "Step delay (ticks)", 0.0, 5.0, 1.0, 0.0));
        maximumSelfDamage = addSetting(new NumberSetting("max_self_damage", "Maximum self damage", 0.0, 20.0, 0.5, 6.0));
        switchBack = addSetting(new BooleanSetting("switch_back", "Restore selected slot", true));
    }

    /** Called by Fabric's block-use callback; true means the macro took over this click. */
    public boolean handleManualUse(Minecraft client, Player player, Level world, InteractionHand hand,
                                   BlockHitResult hit) {
        if (!enabled() || automatedUse || player != client.player || world != client.level
                || !world.isClientSide() || hand != InteractionHand.MAIN_HAND || client.screen != null
                || client.gameMode == null || player.isUsingItem() || player.isSpectator() || player.isCreative()
                || world.environmentAttributes().getDimensionValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS)
                || step != Step.IDLE) {
            return false;
        }

        BlockPos clickedPos = hit.getBlockPos();
        BlockState clickedState = world.getBlockState(clickedPos);
        if (clickedState.is(Blocks.RESPAWN_ANCHOR)) {
            begin(world, clickedPos, null, player.getInventory().getSelectedSlot());
            return true;
        }

        if (!player.getMainHandItem().is(Items.RESPAWN_ANCHOR)) {
            return false;
        }
        BlockPos placementPos = clickedPos.relative(hit.getDirection());
        if (!world.isEmptyBlock(placementPos)) {
            return false;
        }
        begin(world, placementPos, hit, player.getInventory().getSelectedSlot());
        return true;
    }

    @Override
    protected void onEnable(Minecraft client) {
        reset();
    }

    @Override
    protected void onDisable(Minecraft client) {
        finish(client);
    }

    @Override
    protected void onTick(Minecraft client) {
        if (step == Step.IDLE) {
            return;
        }
        Player player = client.player;
        Level world = client.level;
        if (player == null || world == null || client.gameMode == null || world != activeLevel
                || client.screen != null || player.isSpectator() || player.isCreative()) {
            finish(client);
            return;
        }
        if (ownedSlot >= 0 && player.getInventory().getSelectedSlot() != ownedSlot) {
            reset();
            return;
        }

        long now = world.getGameTime();
        if (now < nextActionTick) {
            return;
        }
        switch (step) {
            case PLACE -> place(client, player, now);
            case WAIT_PLACE -> waitForPlacement(client, now);
            case CHARGE -> charge(client, player, now);
            case WAIT_CHARGE -> waitForCharge(client, now);
            case DETONATE -> detonate(client, player);
            case IDLE -> { }
        }
    }

    private void begin(Level world, BlockPos pos, BlockHitResult placement, int selectedSlot) {
        activeLevel = world;
        anchorPos = pos.immutable();
        placementHit = placement;
        originalSlot = selectedSlot;
        ownedSlot = selectedSlot;
        nextActionTick = world.getGameTime() + (long) actionDelay.value();
        waitDeadline = world.getGameTime() + 6L;
        step = placement == null ? Step.CHARGE : Step.PLACE;
    }

    private void place(Minecraft client, Player player, long now) {
        if (placementHit == null || !player.getMainHandItem().is(Items.RESPAWN_ANCHOR)
                || !client.level.isEmptyBlock(anchorPos)) {
            finish(client);
            return;
        }
        use(client, player, placementHit);
        ownedSlot = player.getInventory().getSelectedSlot();
        step = Step.WAIT_PLACE;
        waitDeadline = now + 6L;
        nextActionTick = now + 1L;
    }

    private void waitForPlacement(Minecraft client, long now) {
        if (isAnchor(client.level.getBlockState(anchorPos))) {
            step = Step.CHARGE;
            nextActionTick = now + (long) actionDelay.value();
        } else if (now >= waitDeadline) {
            finish(client);
        }
    }

    private void charge(Minecraft client, Player player, long now) {
        BlockState state = client.level.getBlockState(anchorPos);
        if (!isAnchor(state)) {
            finish(client);
            return;
        }
        if (state.getValue(RespawnAnchorBlock.CHARGE) > 0) {
            step = Step.DETONATE;
            nextActionTick = now + (long) actionDelay.value();
            return;
        }
        int glowstoneSlot = findHotbar(player, Items.GLOWSTONE);
        if (glowstoneSlot < 0) {
            finish(client);
            return;
        }
        select(player, glowstoneSlot);
        use(client, player, anchorHit(anchorPos));
        step = Step.WAIT_CHARGE;
        waitDeadline = now + 6L;
        nextActionTick = now + 1L;
    }

    private void waitForCharge(Minecraft client, long now) {
        BlockState state = client.level.getBlockState(anchorPos);
        if (!isAnchor(state)) {
            finish(client);
        } else if (state.getValue(RespawnAnchorBlock.CHARGE) > 0) {
            step = Step.DETONATE;
            nextActionTick = now + (long) actionDelay.value();
        } else if (now >= waitDeadline) {
            finish(client);
        }
    }

    private void detonate(Minecraft client, Player player) {
        BlockState state = client.level.getBlockState(anchorPos);
        if (!isAnchor(state) || state.getValue(RespawnAnchorBlock.CHARGE) == 0) {
            finish(client);
            return;
        }

        Vec3 center = anchorPos.getCenter();
        float selfDamage = new CrystalDamageEstimator(client.level, player, 10.0).estimate(center);
        boolean hasTotem = player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)
                || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                || findHotbar(player, Items.TOTEM_OF_UNDYING) >= 0;
        if (!AnchorSafetyPolicy.allowsDetonation(selfDamage, (float) maximumSelfDamage.value(),
                player.getHealth(), player.getAbsorptionAmount(), hasTotem)
                || wouldDamageFriend(client, center)) {
            notify(client, "AutoAnchor cancelled: blast safety check failed");
            finish(client);
            return;
        }

        int detonatorSlot = findDetonator(player);
        if (detonatorSlot < 0) {
            finish(client);
            return;
        }
        select(player, detonatorSlot);
        use(client, player, anchorHit(anchorPos));
        finish(client);
    }

    private boolean wouldDamageFriend(Minecraft client, Vec3 center) {
        for (Player candidate : client.level.players()) {
            if (candidate == client.player || !candidate.isAlive() || !FriendManager.contains(candidate)) {
                continue;
            }
            float damage = new CrystalDamageEstimator(client.level, candidate, 10.0).estimate(center);
            if (damage > 0.05F) {
                return true;
            }
        }
        return false;
    }

    private void use(Minecraft client, Player player, BlockHitResult hit) {
        automatedUse = true;
        try {
            InteractionResult result = client.gameMode.useItemOn((LocalPlayer) player, InteractionHand.MAIN_HAND, hit);
            if (result.consumesAction()) {
                player.swing(InteractionHand.MAIN_HAND);
            }
        } finally {
            automatedUse = false;
        }
    }

    private void select(Player player, int slot) {
        if (player.getInventory().getSelectedSlot() != slot) {
            player.getInventory().setSelectedSlot(slot);
            ownedSlot = slot;
        }
    }

    private void finish(Minecraft client) {
        Player player = client.player;
        if (switchBack.value() && player != null && originalSlot >= 0 && ownedSlot >= 0
                && player.getInventory().getSelectedSlot() == ownedSlot) {
            player.getInventory().setSelectedSlot(originalSlot);
        }
        reset();
    }

    private void reset() {
        step = Step.IDLE;
        activeLevel = null;
        anchorPos = null;
        placementHit = null;
        nextActionTick = 0L;
        waitDeadline = 0L;
        originalSlot = -1;
        ownedSlot = -1;
        automatedUse = false;
    }

    private static BlockHitResult anchorHit(BlockPos pos) {
        Vec3 point = new Vec3(pos.getX() + 0.5, pos.getY() + 0.99, pos.getZ() + 0.5);
        return new BlockHitResult(point, Direction.UP, pos, false);
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
        int totemSlot = findHotbar(player, Items.TOTEM_OF_UNDYING);
        if (totemSlot >= 0) {
            return totemSlot;
        }
        int selected = player.getInventory().getSelectedSlot();
        ItemStack selectedStack = player.getInventory().getItem(selected);
        if (!selectedStack.is(Items.GLOWSTONE)) {
            return selected;
        }
        for (int slot = 0; slot < 9; slot++) {
            if (isDetonator(player.getInventory().getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isDetonator(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(Items.GLOWSTONE) && !stack.is(Items.RESPAWN_ANCHOR);
    }

    private static void notify(Minecraft client, String message) {
        if (client.gui != null) {
            client.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("[Jilaton] " + message));
        }
    }

    private enum Step {
        IDLE,
        PLACE,
        WAIT_PLACE,
        CHARGE,
        WAIT_CHARGE,
        DETONATE
    }
}
