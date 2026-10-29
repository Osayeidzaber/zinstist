package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;

public final class AutoTotemModule extends ClientModule {
    private static final int SWAP_WITH_OFFHAND_BUTTON = 40;
    private final BooleanSetting replaceOffhand;
    private final BooleanSetting useHotbarTotems;
    private final NumberSetting refillDelay;
    private long nextSwapTick;

    public AutoTotemModule() {
        super("auto_totem", "AutoTotem", "Moves a carried totem into the offhand when it is safe to do so.", ModuleCategory.COMBAT);
        replaceOffhand = addSetting(new BooleanSetting("replace_offhand", "Replace offhand item", false));
        useHotbarTotems = addSetting(new BooleanSetting("use_hotbar_totems", "Use hotbar totems", false));
        refillDelay = addSetting(new NumberSetting("refill_delay", "Refill delay (ticks)", 1.0, 10.0, 1.0, 3.0));
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

        boolean[] hasTotem = new boolean[36];
        for (int inventorySlot = 0; inventorySlot < hasTotem.length; inventorySlot++) {
            hasTotem[inventorySlot] = client.player.getInventory().getItem(inventorySlot).is(Items.TOTEM_OF_UNDYING);
        }
        int inventorySlot = OffhandTotemPolicy.findSource(hasTotem, useHotbarTotems.value());
        if (inventorySlot >= 0) {
            int menuSlot = inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
            InventoryMenu menu = client.player.inventoryMenu;
            client.gameMode.handleInventoryMouseClick(menu.containerId, menuSlot, SWAP_WITH_OFFHAND_BUTTON,
                    ClickType.SWAP, client.player);
            nextSwapTick = gameTick + (long) refillDelay.value();
        }
    }
}
