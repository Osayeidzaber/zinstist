package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

public final class AutoBuffRefillModule extends ClientModule {
    private final BooleanSetting potions;
    private final BooleanSetting goldenApples;
    private final BooleanSetting mushroomStew;
    private final BooleanSetting playerHeads;
    private final NumberSetting delayTicks;
    private long nextActionTick;
    private ClientLevel lastLevel;

    public AutoBuffRefillModule() {
        super("auto_buff_refill", "AutoBuff Refill",
                "Moves enabled buff items from inventory into empty hotbar slots while your inventory is open.",
                ModuleCategory.PLAYER);
        potions = addSetting(new BooleanSetting("potions", "Potions", true));
        goldenApples = addSetting(new BooleanSetting("golden_apples", "Golden apples", true));
        mushroomStew = addSetting(new BooleanSetting("mushroom_stew", "Mushroom stew", true));
        playerHeads = addSetting(new BooleanSetting("player_heads", "Player heads", true));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay between moves", 0.0, 20.0, 1.0, 1.0));
    }

    @Override
    protected void onEnable(Minecraft client) {
        nextActionTick = 0L;
        lastLevel = null;
    }

    @Override
    protected void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (client.level != lastLevel) {
            lastLevel = client.level;
            nextActionTick = client.level.getGameTime();
        }

        boolean ownInventoryOpen = client.screen instanceof InventoryScreen;
        boolean inventoryMenuActive = player.containerMenu instanceof InventoryMenu
                && player.containerMenu == player.inventoryMenu;
        boolean cursorEmpty = inventoryMenuActive && player.containerMenu.getCarried().isEmpty();
        long tick = client.level.getGameTime();
        if (!AutoBuffRefillPolicy.shouldRefill(enabled(), ownInventoryOpen, inventoryMenuActive,
                cursorEmpty, hasEmptyHotbarSlot(player), tick >= nextActionTick)) {
            return;
        }

        InventoryMenu menu = player.inventoryMenu;
        for (int inventorySlot = 9; inventorySlot < 36; inventorySlot++) {
            ItemStack stack = player.getInventory().getItem(inventorySlot);
            if (!isEnabledBuffItem(stack, player)) {
                continue;
            }
            client.gameMode.handleInventoryMouseClick(menu.containerId, inventorySlot, 0,
                    ClickType.QUICK_MOVE, player);
            nextActionTick = tick + (long) delayTicks.value();
            return;
        }
    }

    private boolean isEnabledBuffItem(ItemStack stack, Player player) {
        if (stack.isEmpty()) {
            return false;
        }

        if (potions.value()) {
            ClientModule module = JilatonClient.moduleManager().get("auto_pot");
            if (module instanceof AutoPotModule autoPot && autoPot.acceptsRefillItem(stack, player)) {
                return true;
            }
        }
        if (goldenApples.value()) {
            ClientModule module = JilatonClient.moduleManager().get("auto_gapple");
            if (module instanceof AutoGappleModule autoGapple && autoGapple.acceptsRefillItem(stack)) {
                return true;
            }
        }
        if (mushroomStew.value()) {
            ClientModule module = JilatonClient.moduleManager().get("auto_soup");
            if (module instanceof AutoSoupModule autoSoup && autoSoup.acceptsRefillItem(stack)) {
                return true;
            }
        }
        if (playerHeads.value()) {
            ClientModule module = JilatonClient.moduleManager().get("auto_head");
            return module instanceof AutoHeadModule autoHead && autoHead.acceptsRefillItem(stack);
        }
        return false;
    }

    private static boolean hasEmptyHotbarSlot(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
