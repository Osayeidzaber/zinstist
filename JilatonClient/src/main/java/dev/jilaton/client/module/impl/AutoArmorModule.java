package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

public final class AutoArmorModule extends ClientModule {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private final NumberSetting minimumUpgrade;
    private long nextEquipTick;

    public AutoArmorModule() {
        super("auto_armor", "AutoArmor", "Equips carried armor when its protection is better than the worn piece.", ModuleCategory.COMBAT);
        minimumUpgrade = addSetting(new NumberSetting("minimum_upgrade", "Minimum upgrade", 0.0, 8.0, 0.25, 0.25));
    }

    @Override
    protected void onEnable(Minecraft client) {
        nextEquipTick = 0L;
    }

    @Override
    protected void onDisable(Minecraft client) {
        nextEquipTick = 0L;
    }

    @Override
    protected void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.screen != null
                || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()
                || client.level.getGameTime() < nextEquipTick) {
            return;
        }

        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            ItemStack current = player.getItemBySlot(equipmentSlot);
            double currentScore = armorScore(current, equipmentSlot);
            int bestInventorySlot = -1;
            double bestScore = currentScore;

            for (int inventorySlot = 0; inventorySlot < 36; inventorySlot++) {
                ItemStack candidate = player.getInventory().getItem(inventorySlot);
                Equippable equippable = candidate.get(DataComponents.EQUIPPABLE);
                if (candidate.isEmpty() || equippable == null || equippable.slot() != equipmentSlot) {
                    continue;
                }

                double score = armorScore(candidate, equipmentSlot);
                if (score > bestScore + minimumUpgrade.value()) {
                    bestScore = score;
                    bestInventorySlot = inventorySlot;
                }
            }

            if (bestInventorySlot >= 0) {
                equipFromInventory(client, bestInventorySlot, equipmentSlot);
                nextEquipTick = client.level.getGameTime() + 4L;
                return;
            }
        }
    }

    private static double armorScore(ItemStack stack, EquipmentSlot slot) {
        ItemAttributeModifiers attributes = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (attributes == null) {
            return 0.0;
        }
        double armor = attributes.compute(Attributes.ARMOR, 0.0, slot);
        double toughness = attributes.compute(Attributes.ARMOR_TOUGHNESS, 0.0, slot);
        return armor + toughness * 0.5;
    }

    private static void equipFromInventory(Minecraft client, int inventorySlot, EquipmentSlot equipmentSlot) {
        int sourceSlot = inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
        int armorSlot = InventoryMenu.ARMOR_SLOT_START + EquipmentSlot.HEAD.getIndex() - equipmentSlot.getIndex();
        int menuId = client.player.inventoryMenu.containerId;
        client.gameMode.handleInventoryMouseClick(menuId, sourceSlot, 0, ClickType.PICKUP, client.player);
        client.gameMode.handleInventoryMouseClick(menuId, armorSlot, 0, ClickType.PICKUP, client.player);
        client.gameMode.handleInventoryMouseClick(menuId, sourceSlot, 0, ClickType.PICKUP, client.player);
    }
}
