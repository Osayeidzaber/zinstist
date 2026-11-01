package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Throws junk out of an open chest, barrel or shulker box, one stack per delay, using the InventoryCleaner junk
 * list so both modules agree on what junk is. Only container slots are touched, never your own inventory.
 */
public final class ChestCleanerModule extends ClientModule {
    private final NumberSetting delay = addSetting(new NumberSetting("delay", "Delay (ticks)", 0, 10, 1, 2));
    private final NumberSetting startDelay = addSetting(new NumberSetting("start_delay", "Wait after opening (ticks)", 0, 20, 1, 4));
    private final BooleanSetting shulkers = addSetting(new BooleanSetting("shulkers", "Shulker boxes too", false));
    private Object screen;
    private int wait;

    public ChestCleanerModule() {
        super("chest_cleaner", "ChestCleaner", "Throws junk (InventoryCleaner list) out of open chests.", ModuleCategory.PLAYER);
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null) return;
        boolean chest = client.screen instanceof ContainerScreen || shulkers.value() && client.screen instanceof ShulkerBoxScreen;
        if (!chest) {
            screen = null;
            return;
        }
        if (screen != client.screen) {
            screen = client.screen;
            wait = (int) Math.round(startDelay.value());
        }
        if (wait-- > 0) return;
        AbstractContainerMenu menu = ((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) client.screen).getMenu();
        if (!menu.getCarried().isEmpty()) return;
        Set<String> junk = junkList();
        if (junk.isEmpty()) return;
        int containerSlots = menu.slots.size() - 36;
        for (int slot = 0; slot < containerSlots; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty() || !junk.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())) continue;
            client.gameMode.handleInventoryMouseClick(menu.containerId, slot, 1, ClickType.THROW, client.player);
            wait = (int) Math.round(delay.value());
            return;
        }
    }

    private static Set<String> junkList() {
        ClientModule cleaner = JilatonClient.moduleManager().get("inventory_cleaner");
        if (cleaner == null) return Set.of();
        for (var setting : cleaner.settings()) {
            if (setting.id().equals("junk") && setting instanceof TextSetting text) return InventoryCleanerModule.parse(text.value());
        }
        return Set.of();
    }
}
