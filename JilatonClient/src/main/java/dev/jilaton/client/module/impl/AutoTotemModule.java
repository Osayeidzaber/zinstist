package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;

public final class AutoTotemModule extends ClientModule {
    private static final int SWAP_WITH_OFFHAND_BUTTON = 40;
    private final BooleanSetting replaceOffhand;
    private long nextSwapTick;

    public AutoTotemModule() {
        super("auto_totem", "AutoTotem", "Moves a carried totem into the offhand when it is safe to do so.", ModuleCategory.COMBAT);
        replaceOffhand = addSetting(new BooleanSetting("replace_offhand", "Replace offhand item", false));
    }

    @Override
    protected void onEnable(Minecraft client) {
        nextSwapTick = 0L;
    }

    @Override
    protected void onDisable(Minecraft client) {
        nextSwapTick = 0L;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.level == null || client.screen != null
                || client.player.containerMenu != client.player.inventoryMenu) {
            return;
        }

        if (client.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                || (!replaceOffhand.value() && !client.player.getOffhandItem().isEmpty())) {
            return;
        }

        long gameTick = client.level.getGameTime();
        if (gameTick < nextSwapTick) {
            return;
        }

        for (int inventorySlot = 0; inventorySlot < 36; inventorySlot++) {
            if (!client.player.getInventory().getItem(inventorySlot).is(Items.TOTEM_OF_UNDYING)) {
                continue;
            }

            int menuSlot = inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
            InventoryMenu menu = client.player.inventoryMenu;
            client.gameMode.handleInventoryMouseClick(menu.containerId, menuSlot, SWAP_WITH_OFFHAND_BUTTON,
                    ClickType.SWAP, client.player);
            nextSwapTick = gameTick + 5L;
            return;
        }
    }
}
