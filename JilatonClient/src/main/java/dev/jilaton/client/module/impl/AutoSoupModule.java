package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Uses a hotbar mushroom stew at low health and restores the previous selection afterward. */
public final class AutoSoupModule extends ClientModule {
    private final NumberSetting healthPercent;
    private final BooleanSetting considerAbsorption;
    private final BooleanSetting dropBowl;
    private final NumberSetting cooldownTicks;
    private Player usingPlayer;
    private int previousSlot = -1;
    private int soupSlot = -1;
    private int soupCountBeforeUse;
    private long nextAttemptTick;
    private boolean restorePending;

    public AutoSoupModule() {
        super("auto_soup", "AutoSoup", "Uses hotbar mushroom stew at the configured health threshold.",
                ModuleCategory.COMBAT);
        healthPercent = addSetting(new NumberSetting("health_percent", "Health threshold", 1.0, 100.0, 1.0, 40.0));
        considerAbsorption = addSetting(new BooleanSetting("consider_absorption", "Count absorption", true));
        dropBowl = addSetting(new BooleanSetting("drop_bowl", "Drop empty bowl after use", false));
        cooldownTicks = addSetting(new NumberSetting("cooldown_ticks", "Retry cooldown", 0.0, 100.0, 1.0, 20.0));
    }

    @Override
    protected void onEnable(Minecraft client) {
        clearUseState();
        nextAttemptTick = 0L;
    }

    @Override
    protected void onDisable(Minecraft client) {
        restoreSlotIfSafe();
        clearUseState();
        nextAttemptTick = 0L;
    }

    @Override
    protected void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            restoreSlotIfSafe();
            clearUseState();
            nextAttemptTick = 0L;
            return;
        }

        if (restorePending) {
            if (player == usingPlayer && player.isUsingItem()) {
                return;
            }
            finishUse(client);
            return;
        }

        ClientModule autoPot = JilatonClient.moduleManager().get("auto_pot");
        if (autoPot instanceof AutoPotModule potion && potion.enabled() && potion.blocksAutomaticFoodUse(client)) {
            return;
        }
        ClientModule autoGapple = JilatonClient.moduleManager().get("auto_gapple");
        if (autoGapple instanceof AutoGappleModule gapple && gapple.enabled()
                && (gapple.isManagingHotbar() || gapple.willUseNow(client))) {
            return;
        }
        ClientModule autoHead = JilatonClient.moduleManager().get("auto_head");
        if (autoHead instanceof AutoHeadModule head && head.enabled() && head.isManagingHotbar()) {
            return;
        }

        if (client.screen != null || player.isUsingItem() || client.options.keyUse.isDown()
                || client.level.getGameTime() < nextAttemptTick) {
            return;
        }

        int slot = findSoupSlot(player);
        boolean safeContext = client.screen == null && !player.isUsingItem() && !client.options.keyUse.isDown();
        if (!AutoSoupPolicy.shouldUse(player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount(),
                healthPercent.value(), considerAbsorption.value(), slot >= 0, safeContext,
                client.level.getGameTime() >= nextAttemptTick)) {
            return;
        }

        usingPlayer = player;
        previousSlot = player.getInventory().getSelectedSlot();
        soupSlot = slot;
        soupCountBeforeUse = player.getInventory().getItem(slot).getCount();
        if (previousSlot != soupSlot) {
            player.getInventory().setSelectedSlot(soupSlot);
        }
        restorePending = true;
        nextAttemptTick = client.level.getGameTime() + (long) cooldownTicks.value();
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
    }

    private void finishUse(Minecraft client) {
        Player player = usingPlayer;
        if (player == null) {
            clearUseState();
            return;
        }

        ItemStack usedStack = player.getInventory().getItem(soupSlot);
        boolean consumedSoup = usedStack.is(Items.BOWL)
                || usedStack.getCount() < soupCountBeforeUse;
        if (dropBowl.value() && consumedSoup && usedStack.is(Items.BOWL)
                && player.getInventory().getSelectedSlot() == soupSlot) {
            client.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, soupSlot + 36,
                    1, ClickType.THROW, player);
        }
        restoreSlotIfSafe();
        clearUseState();
    }

    private static int findSoupSlot(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.MUSHROOM_STEW)) {
                return slot;
            }
        }
        return -1;
    }

    private void restoreSlotIfSafe() {
        if (usingPlayer != null && previousSlot >= 0 && soupSlot >= 0
                && usingPlayer.getInventory().getSelectedSlot() == soupSlot) {
            usingPlayer.getInventory().setSelectedSlot(previousSlot);
        }
    }

    private void clearUseState() {
        usingPlayer = null;
        previousSlot = -1;
        soupSlot = -1;
        soupCountBeforeUse = 0;
        restorePending = false;
    }

    public boolean isManagingHotbar() {
        return restorePending;
    }

    public boolean willUseNow(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.screen != null
                || player.isUsingItem() || client.options.keyUse.isDown()
                || client.level.getGameTime() < nextAttemptTick) {
            return false;
        }
        int slot = findSoupSlot(player);
        return AutoSoupPolicy.shouldUse(player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount(),
                healthPercent.value(), considerAbsorption.value(), slot >= 0, true, true);
    }

    public boolean acceptsRefillItem(ItemStack stack) {
        return enabled() && stack.is(Items.MUSHROOM_STEW);
    }

    public NumberSetting healthPercent() {
        return healthPercent;
    }

    public BooleanSetting considerAbsorption() {
        return considerAbsorption;
    }

    public BooleanSetting dropBowl() {
        return dropBowl;
    }

    public NumberSetting cooldownTicks() {
        return cooldownTicks;
    }
}
