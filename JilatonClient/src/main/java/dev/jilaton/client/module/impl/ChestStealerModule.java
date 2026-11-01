package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/**
 * Moves items out of an open chest, barrel or shulker box into your inventory, one per delay, the same as
 * shift-clicking them. Junk from the InventoryCleaner list can be left behind; the screen can close itself when done.
 */
public final class ChestStealerModule extends ClientModule {
    private final NumberSetting delay = addSetting(new NumberSetting("delay", "Delay (ticks)", 0, 10, 1, 2));
    private final NumberSetting startDelay = addSetting(new NumberSetting("start_delay", "Wait after opening (ticks)", 0, 20, 1, 3));
    private final BooleanSetting skipJunk = addSetting(new BooleanSetting("skip_junk", "Leave InventoryCleaner junk", true));
    private final BooleanSetting autoClose = addSetting(new BooleanSetting("auto_close", "Close when done", true));
    private final BooleanSetting shulkers = addSetting(new BooleanSetting("shulkers", "Shulker boxes too", true));
    private Object screen;
    private int wait;

    public ChestStealerModule() {
        super("chest_stealer", "ChestStealer", "Takes items from open chests and closes them when empty.", ModuleCategory.PLAYER);
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null) return;
        if (!(client.screen instanceof ContainerScreen) && !(shulkers.value() && client.screen instanceof ShulkerBoxScreen)) {
            screen = null;
            return;
        }
        AbstractContainerScreen<?> open = (AbstractContainerScreen<?>) client.screen;
        if (screen != open) {
            screen = open;
            wait = (int) Math.round(startDelay.value());
        }
        if (wait-- > 0) return;
        AbstractContainerMenu menu = open.getMenu();
        if (!menu.getCarried().isEmpty()) return;
        int containerSlots = menu.slots.size() - 36;
        java.util.Set<String> junk = skipJunk.value() ? junkList() : java.util.Set.of();
        for (int slot = 0; slot < containerSlots; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty() || junk.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())) continue;
            if (!roomFor(client, stack)) {
                if (autoClose.value()) client.player.closeContainer();
                return;
            }
            client.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, ClickType.QUICK_MOVE, client.player);
            wait = (int) Math.round(delay.value());
            return;
        }
        if (autoClose.value()) client.player.closeContainer();
    }

    private static boolean roomFor(Minecraft client, ItemStack stack) {
        var inventory = client.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack own = inventory.getItem(i);
            if (own.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(own, stack) && own.getCount() < own.getMaxStackSize()) return true;
        }
        return false;
    }

    private static java.util.Set<String> junkList() {
        ClientModule cleaner = JilatonClient.moduleManager().get("inventory_cleaner");
        if (cleaner == null) return java.util.Set.of();
        for (var setting : cleaner.settings()) {
            if (setting.id().equals("junk") && setting instanceof dev.jilaton.client.setting.TextSetting text) {
                return InventoryCleanerModule.parse(text.value());
            }
        }
        return java.util.Set.of();
    }
}
