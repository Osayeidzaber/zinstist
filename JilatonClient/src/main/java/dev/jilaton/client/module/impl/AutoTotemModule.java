package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.concurrent.ThreadLocalRandom;

public final class AutoTotemModule extends ClientModule {
    private static final int SWAP_WITH_OFFHAND_BUTTON = 40;

    private final BooleanSetting replaceOffhand;
    private final BooleanSetting useHotbarTotems;
    private final NumberSetting refillDelay;
    private final OffhandRefillController refill;

    public AutoTotemModule() {
        super("auto_totem", "AutoTotem", "Refills an empty offhand from inventory with server-confirmed retries.",
                ModuleCategory.COMBAT);
        replaceOffhand = addSetting(new BooleanSetting("replace_offhand", "Replace offhand item", false));
        useHotbarTotems = addSetting(new BooleanSetting("use_hotbar_totems", "Use hotbar totems", false));
        refillDelay = addSetting(new NumberSetting("refill_delay", "Refill delay", 1.0, 10.0, 1.0, 3.0));
        refill = new OffhandRefillController(new OffhandRefillController.Driver() {
            @Override
            public boolean needsTotem() {
                Player player = Minecraft.getInstance().player;
                if (player == null) {
                    return false;
                }
                ItemStack offhand = player.getOffhandItem();
                return offhand.isEmpty() || replaceOffhand.value() && !offhand.is(Items.TOTEM_OF_UNDYING);
            }

            @Override
            public boolean canInteract() {
                Minecraft client = Minecraft.getInstance();
                return client.player != null && client.level != null && client.gameMode != null
                        && client.gameMode.getPlayerMode() == GameType.SURVIVAL
                        && client.player.isAlive() && !client.player.isSpectator()
                        && client.screen == null && client.player.containerMenu == client.player.inventoryMenu;
            }

            @Override
            public int findSource() {
                Player player = Minecraft.getInstance().player;
                if (player == null) {
                    return -1;
                }
                boolean[] hasTotem = new boolean[36];
                for (int slot = 0; slot < hasTotem.length; slot++) {
                    hasTotem[slot] = player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING);
                }
                return OffhandTotemPolicy.findSource(hasTotem, useHotbarTotems.value());
            }

            @Override
            public boolean validSource(int slot) {
                Minecraft client = Minecraft.getInstance();
                return client.player != null && slot >= 0 && slot < 36
                        && client.player.containerMenu == client.player.inventoryMenu
                        && client.player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING);
            }

            @Override
            public void swap(int slot) {
                Minecraft client = Minecraft.getInstance();
                Player player = client.player;
                if (player == null || !validSource(slot)) {
                    return;
                }
                int menuSlot = slot < 9 ? 36 + slot : slot;
                InventoryMenu menu = player.inventoryMenu;
                client.gameMode.handleInventoryMouseClick(menu.containerId, menuSlot,
                        SWAP_WITH_OFFHAND_BUTTON, ClickType.SWAP, player);
            }

            @Override
            public int containerId() {
                Player player = Minecraft.getInstance().player;
                return player == null ? -1 : player.inventoryMenu.containerId;
            }

            @Override
            public int stateId() {
                Player player = Minecraft.getInstance().player;
                return player == null ? -1 : player.inventoryMenu.getStateId();
            }

            @Override
            public int latencyMillis() {
                Minecraft client = Minecraft.getInstance();
                Player player = client.player;
                if (player == null || client.getConnection() == null) {
                    return 0;
                }
                PlayerInfo info = client.getConnection().getPlayerInfo(player.getUUID());
                return info == null ? 0 : info.getLatency();
            }
        }, ThreadLocalRandom.current());
    }

    @Override
    protected void onEnable(Minecraft client) {
        refill.reset();
    }

    @Override
    protected void onDisable(Minecraft client) {
        refill.reset();
    }

    @Override
    protected void onTick(Minecraft client) {
        refill.step(System.nanoTime(), (int) refillDelay.value());
    }

    public boolean busy() {
        return enabled() && refill.busy();
    }

    public void observeServerOffhand(int containerId, int stateId, ItemStack stack) {
        OffhandRefillController.Offhand observed = stack.isEmpty()
                ? OffhandRefillController.Offhand.EMPTY
                : stack.is(Items.TOTEM_OF_UNDYING)
                ? OffhandRefillController.Offhand.TOTEM : OffhandRefillController.Offhand.OTHER;
        refill.observe(containerId, stateId, observed, System.nanoTime());
    }

    public void onTotemPop() {
        refill.reset();
    }
}
