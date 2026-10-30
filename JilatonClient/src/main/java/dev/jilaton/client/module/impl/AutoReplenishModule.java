package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Refills low hotbar/offhand stacks by swapping a matching stack from main inventory. */
public final class AutoReplenishModule extends ClientModule {
    private static final int TRACKED_SLOTS = 10;
    private static final int MAIN_INVENTORY_FIRST = 9;
    private static final int MAIN_INVENTORY_LAST = 35;
    private static final int QUIET_TICKS_BEFORE_LEGIT = 10;
    private static final int MAX_SAFE_CLICK_WAIT = 10;
    private static final double FAST_MOVEMENT_SQUARED = 0.06;
    private static final String MODE_INSTANT = "Instant";
    private static final String MODE_LEGIT = "Legit";

    private final ChoiceSetting mode;
    private final NumberSetting delayTicks;
    private final NumberSetting threshold;
    private final BooleanSetting offhand;
    private final ItemStack[] lastKnown = new ItemStack[TRACKED_SLOTS];
    private final ItemStack[] pending = new ItemStack[TRACKED_SLOTS];

    private Player trackedPlayer;
    private ClientLevel trackedLevel;
    private int quietTicks;
    private int delayTimer;
    private int stage;
    private int waitTicks;
    private int activeSlot = -1;
    private ItemStack activeReference;
    private boolean screenWasAlreadyOpen;

    public AutoReplenishModule() {
        super("auto_replenish", "Auto Replenish",
                "Swaps a matching main-inventory stack into a low hotbar or offhand slot.", ModuleCategory.PLAYER);
        mode = addSetting(new ChoiceSetting("mode", "Mode", List.of(MODE_INSTANT, MODE_LEGIT), MODE_INSTANT));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Instant delay", 0.0, 20.0, 1.0, 1.0));
        threshold = addSetting(new NumberSetting("threshold", "Refill threshold", 1.0, 64.0, 1.0, 8.0));
        offhand = addSetting(new BooleanSetting("offhand", "Track offhand", true));
    }

    @Override
    protected void onEnable(Minecraft client) {
        clearTracking();
    }

    @Override
    protected void onDisable(Minecraft client) {
        if (stage != 0 && !screenWasAlreadyOpen && client.screen instanceof InventoryScreen) {
            client.setScreen(null);
        }
        clearTracking();
    }

    @Override
    protected void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            clearTracking();
            return;
        }
        if (player != trackedPlayer || client.level != trackedLevel) {
            resync(player, client.level);
        }

        updateTracking(client, player);
        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        if (MODE_INSTANT.equals(mode.value())) {
            refillInstant(client, player);
        } else {
            stepLegit(client, player);
        }
    }

    private void refillInstant(Minecraft client, Player player) {
        if (stage != 0) {
            restoreActivePending();
            if (!screenWasAlreadyOpen && client.screen instanceof InventoryScreen) {
                client.setScreen(null);
            }
            resetJob();
        }
        if (!safeToSwap(client, player, false)) {
            return;
        }
        int slot = nextPending(player);
        if (slot < 0) {
            return;
        }
        ItemStack target = trackedStack(player, slot);
        int source = findSource(player, target, pending[slot]);
        if (source < 0 || !safeToSwap(client, player, false)) {
            return;
        }
        swap(client, player, source, slot);
        pending[slot] = null;
        delayTimer = (int) delayTicks.value();
    }

    private void stepLegit(Minecraft client, Player player) {
        switch (stage) {
            case 0 -> {
                if (!canBeginLegit(client, player) || quietTicks < QUIET_TICKS_BEFORE_LEGIT) {
                    return;
                }
                int slot = nextPending(player);
                if (slot < 0) {
                    return;
                }
                activeSlot = slot;
                activeReference = pending[slot].copy();
                pending[slot] = null;
                screenWasAlreadyOpen = client.screen instanceof InventoryScreen;
                if (screenWasAlreadyOpen) {
                    stage = 2;
                } else {
                    client.setScreen(new InventoryScreen(player));
                    stage = 1;
                    delayTimer = ThreadLocalRandom.current().nextInt(4, 7);
                }
            }
            case 1 -> {
                if (!(client.screen instanceof InventoryScreen)) {
                    restoreActivePending();
                    resetJob();
                    return;
                }
                stage = 2;
            }
            case 2 -> {
                if (!(client.screen instanceof InventoryScreen)) {
                    restoreActivePending();
                    resetJob();
                    return;
                }
                if (!safeToSwap(client, player, true)) {
                    if (++waitTicks <= MAX_SAFE_CLICK_WAIT) {
                        return;
                    }
                    restoreActivePending();
                    stage = 3;
                    return;
                }
                ItemStack target = trackedStack(player, activeSlot);
                int source = needsRefill(target, activeReference)
                        ? findSource(player, target, activeReference) : -1;
                if (source >= 0) {
                    swap(client, player, source, activeSlot);
                } else {
                    restoreActivePending();
                }
                stage = 3;
                delayTimer = ThreadLocalRandom.current().nextInt(2, 4);
            }
            case 3 -> {
                if (!screenWasAlreadyOpen && client.screen instanceof InventoryScreen) {
                    client.setScreen(null);
                }
                resetJob();
            }
            default -> resetJob();
        }
    }

    private boolean canBeginLegit(Minecraft client, Player player) {
        if (client.screen != null && !(client.screen instanceof InventoryScreen)) {
            return false;
        }
        AutoTotemModule totem = autoTotem();
        return (totem == null || !totem.busy()) && !player.isUsingItem()
                && !client.options.keyUse.isDown() && !client.options.keyAttack.isDown()
                && !client.options.keyUp.isDown() && !client.options.keyDown.isDown()
                && !client.options.keyLeft.isDown() && !client.options.keyRight.isDown()
                && !client.options.keyJump.isDown()
                && !isMovingFast(player);
    }

    private boolean safeToSwap(Minecraft client, Player player, boolean inventoryMayBeOpen) {
        if (!player.isAlive() || player.isSpectator() || client.gameMode.getPlayerMode() != GameType.SURVIVAL
                || player.isUsingItem() || !(player.containerMenu instanceof InventoryMenu menu)
                || player.containerMenu != player.inventoryMenu || !menu.getCarried().isEmpty()) {
            return false;
        }
        if (inventoryMayBeOpen) {
            if (!(client.screen instanceof InventoryScreen)) {
                return false;
            }
        } else if (client.screen != null) {
            return false;
        }
        AutoTotemModule totem = autoTotem();
        return totem == null || !totem.busy();
    }

    private static AutoTotemModule autoTotem() {
        ClientModule module = JilatonClient.moduleManager().get("auto_totem");
        return module instanceof AutoTotemModule totem ? totem : null;
    }

    private void resync(Player player, ClientLevel level) {
        trackedPlayer = player;
        trackedLevel = level;
        for (int slot = 0; slot < TRACKED_SLOTS; slot++) {
            ItemStack current = trackedStack(player, slot);
            lastKnown[slot] = current.copy();
            pending[slot] = isLowStack(current) ? current.copy() : null;
        }
        quietTicks = 0;
        resetJob();
    }

    private void updateTracking(Minecraft client, Player player) {
        boolean changed = false;
        boolean foreignContainer = client.screen != null && !(client.screen instanceof InventoryScreen);
        for (int slot = 0; slot < TRACKED_SLOTS; slot++) {
            ItemStack current = trackedStack(player, slot);
            ItemStack previous = lastKnown[slot] == null ? current : lastKnown[slot];
            if (slot == 9 && !offhand.value()) {
                pending[slot] = null;
            } else if (player.isCreative() || foreignContainer || isTotem(current) || isTotem(previous)) {
                pending[slot] = null;
            } else if (current.isEmpty() && !previous.isEmpty()) {
                pending[slot] = previous.copy();
            } else if (!current.isEmpty() && isLowStack(current)) {
                if (!ItemStack.isSameItemSameComponents(current, previous)
                        || pending[slot] == null || pending[slot].isEmpty()
                        || !ItemStack.isSameItemSameComponents(current, pending[slot])
                        || current.getCount() != previous.getCount()) {
                    pending[slot] = current.copy();
                    changed = true;
                }
            } else if (current.isEmpty() || current.getCount() > threshold.value()) {
                pending[slot] = null;
            }
            if (!ItemStack.isSameItemSameComponents(current, previous)
                    || current.getCount() != previous.getCount()) {
                changed = true;
            }
            lastKnown[slot] = current.copy();
        }
        if (player.getInventory().getSelectedSlot() != lastSelectedSlot) {
            lastSelectedSlot = player.getInventory().getSelectedSlot();
            changed = true;
        }
        quietTicks = changed ? 0 : Math.min(quietTicks + 1, 1000);
    }

    private int lastSelectedSlot = -1;

    private int nextPending(Player player) {
        for (int slot = 0; slot < TRACKED_SLOTS; slot++) {
            if (slot == 9 && !offhand.value()) {
                continue;
            }
            ItemStack reference = pending[slot];
            if (reference != null && !reference.isEmpty() && hasSource(player, slot, reference)) {
                return slot;
            }
        }
        return -1;
    }

    private int findSource(Player player, ItemStack target, ItemStack reference) {
        int currentCount = ItemStack.isSameItemSameComponents(target, reference) ? target.getCount() : 0;
        int bestCount = currentCount;
        int bestSlot = -1;
        for (int inventorySlot = MAIN_INVENTORY_FIRST; inventorySlot <= MAIN_INVENTORY_LAST; inventorySlot++) {
            ItemStack candidate = player.getInventory().getItem(inventorySlot);
            if (ItemStack.isSameItemSameComponents(candidate, reference)
                    && AutoReplenishPolicy.sourceImproves(candidate.getCount(), bestCount)) {
                bestCount = candidate.getCount();
                bestSlot = inventorySlot;
            }
        }
        return bestSlot;
    }

    private boolean hasSource(Player player, int slot, ItemStack reference) {
        return findSource(player, trackedStack(player, slot), reference) >= 0;
    }

    private boolean needsRefill(ItemStack current, ItemStack reference) {
        return AutoReplenishPolicy.targetNeedsRefill(current.isEmpty(),
                !current.isEmpty() && ItemStack.isSameItemSameComponents(current, reference),
                current.getCount(), (int) threshold.value());
    }

    private boolean isLowStack(ItemStack stack) {
        return !stack.isEmpty() && !isTotem(stack)
                && AutoReplenishPolicy.needsRefill(stack.getCount(), (int) threshold.value());
    }

    private static boolean isTotem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING);
    }

    private static ItemStack trackedStack(Player player, int slot) {
        return slot == 9 ? player.getOffhandItem() : player.getInventory().getItem(slot);
    }

    private static boolean isMovingFast(Player player) {
        if (player.isPassenger()) {
            return true;
        }
        double dx = player.getDeltaMovement().x;
        double dz = player.getDeltaMovement().z;
        return dx * dx + dz * dz > FAST_MOVEMENT_SQUARED;
    }

    private void swap(Minecraft client, Player player, int sourceSlot, int trackedSlot) {
        InventoryMenu menu = player.inventoryMenu;
        int button = AutoReplenishPolicy.swapButton(trackedSlot);
        client.gameMode.handleInventoryMouseClick(menu.containerId, sourceSlot, button, ClickType.SWAP, player);
    }

    private void restoreActivePending() {
        if (activeSlot >= 0 && activeReference != null && !activeReference.isEmpty()
                && (pending[activeSlot] == null || pending[activeSlot].isEmpty())) {
            pending[activeSlot] = activeReference.copy();
        }
    }

    private void resetJob() {
        stage = 0;
        waitTicks = 0;
        activeSlot = -1;
        activeReference = null;
        screenWasAlreadyOpen = false;
    }

    public boolean busy() {
        return enabled() && stage != 0;
    }

    private void clearTracking() {
        trackedPlayer = null;
        trackedLevel = null;
        quietTicks = 0;
        delayTimer = 0;
        lastSelectedSlot = -1;
        for (int slot = 0; slot < TRACKED_SLOTS; slot++) {
            lastKnown[slot] = null;
            pending[slot] = null;
        }
        resetJob();
    }
}
