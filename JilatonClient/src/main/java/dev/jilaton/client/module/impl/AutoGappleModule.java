package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AutoGappleModule extends ClientModule {
    private final NumberSetting healthThreshold;
    private final BooleanSetting enchantedApples;
    private int slotBeforeUse = -1;
    private int appleSlot = -1;
    private boolean restorePending;
    private long nextAttemptTick;

    public AutoGappleModule() {
        super("auto_gapple", "AutoGapple", "Uses a hotbar golden apple below the selected health threshold.", ModuleCategory.COMBAT);
        healthThreshold = addSetting(new NumberSetting("health_threshold", "Health threshold", 1.0, 20.0, 0.5, 10.0));
        enchantedApples = addSetting(new BooleanSetting("enchanted_apples", "Use enchanted apples", true));
    }

    @Override
    protected void onEnable(Minecraft client) {
        clearUseState();
        nextAttemptTick = 0L;
    }

    @Override
    protected void onDisable(Minecraft client) {
        restoreSlotIfSafe(client);
        clearUseState();
        nextAttemptTick = 0L;
    }

    @Override
    protected void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            clearUseState();
            nextAttemptTick = 0L;
            return;
        }

        if (restorePending) {
            if (player.isUsingItem()) {
                return;
            }
            restoreSlotIfSafe(client);
            clearUseState();
        }

        if (client.screen != null || player.isUsingItem() || client.options.keyUse.isDown()
                || player.getHealth() > healthThreshold.value()
                || client.level.getGameTime() < nextAttemptTick) {
            return;
        }

        int slot = findAppleSlot(player, Items.GOLDEN_APPLE);
        if (slot < 0 && enchantedApples.value()) {
            slot = findAppleSlot(player, Items.ENCHANTED_GOLDEN_APPLE);
        }
        if (slot >= 0) {
            slotBeforeUse = player.getInventory().getSelectedSlot();
            appleSlot = slot;
            if (slotBeforeUse != appleSlot) {
                player.getInventory().setSelectedSlot(appleSlot);
            }
            restorePending = true;
            nextAttemptTick = client.level.getGameTime() + 100L;
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        }
    }

    private int findAppleSlot(Player player, Item item) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item) && !player.getCooldowns().isOnCooldown(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private void restoreSlotIfSafe(Minecraft client) {
        if (client.player == null || slotBeforeUse < 0 || appleSlot < 0 || client.player.isUsingItem()) {
            return;
        }
        if (client.player.getInventory().getSelectedSlot() == appleSlot) {
            client.player.getInventory().setSelectedSlot(slotBeforeUse);
        }
    }

    private void clearUseState() {
        slotBeforeUse = -1;
        appleSlot = -1;
        restorePending = false;
    }
}
