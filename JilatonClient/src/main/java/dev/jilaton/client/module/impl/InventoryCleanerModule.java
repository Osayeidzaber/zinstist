package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Throws away items on your junk list, one per delay, so the inventory stays tidy without manual sorting. */
public final class InventoryCleanerModule extends ClientModule {
    private static final String DEFAULT_JUNK = "rotten_flesh,poisonous_potato,spider_eye,dead_bush,wheat_seeds,"
            + "pufferfish,string,bone,gunpowder,egg,feather,flint,stick,kelp,cobweb";

    private final ChoiceSetting when = addSetting(new ChoiceSetting("when", "When", List.of("Inventory open", "Always"), "Inventory open"));
    private final TextSetting junk = addSetting(new TextSetting("junk", "Junk items (ids, comma separated)", 600, DEFAULT_JUNK,
            value -> value.toLowerCase(Locale.ROOT).replace(" ", "")));
    private final NumberSetting delay = addSetting(new NumberSetting("delay", "Delay (ticks)", 0, 20, 1, 3));
    private final BooleanSetting hotbar = addSetting(new BooleanSetting("hotbar", "Clean hotbar too", false));
    private int wait;

    public InventoryCleanerModule() {
        super("inventory_cleaner", "InventoryCleaner", "Drops junk items from your inventory automatically.", ModuleCategory.PLAYER);
    }

    @Override
    protected void onTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || player.isCreative()) return;
        boolean open = client.screen instanceof InventoryScreen;
        if (when.value().equals("Inventory open") ? !open : client.screen != null && !open) return;
        if (!player.containerMenu.getCarried().isEmpty()) return;
        if (wait-- > 0) return;
        int slot = InventoryCleanerPolicy.firstJunk(ids(player), parse(junk.value()), hotbar.value());
        if (slot < 0) return;
        client.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, slot, 1, ClickType.THROW, player);
        wait = (int) Math.round(delay.value());
    }

    /** Item ids for inventory-menu slots 9..44 (main inventory then hotbar), empty string for empty slots. */
    private static String[] ids(LocalPlayer player) {
        String[] ids = new String[45];
        for (int menuSlot = 9; menuSlot <= 44; menuSlot++) {
            ItemStack stack = player.inventoryMenu.getSlot(menuSlot).getItem();
            ids[menuSlot] = stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        }
        return ids;
    }

    static Set<String> parse(String list) {
        java.util.HashSet<String> out = new java.util.HashSet<>();
        for (String part : list.split(",")) {
            String id = part.trim().toLowerCase(Locale.ROOT);
            if (id.startsWith("minecraft:")) id = id.substring(10);
            if (!id.isEmpty()) out.add(id);
        }
        return out;
    }
}
