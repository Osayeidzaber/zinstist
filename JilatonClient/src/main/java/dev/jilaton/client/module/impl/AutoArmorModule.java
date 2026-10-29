package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.EnumMap;
import java.util.Map;

public final class AutoArmorModule extends ClientModule {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final double EXPECTED_DAMAGE = 6.0;

    private final NumberSetting minimumUpgrade;
    private final NumberSetting minimumDurability;
    private final NumberSetting equipDelay;
    private final BooleanSetting avoidBindingCurse;
    private final BooleanSetting saveArmor;
    private final NumberSetting durabilityThreshold;
    private long nextEquipTick;

    public AutoArmorModule() {
        super("auto_armor", "AutoArmor",
                "Equips the best available armor using full-kit damage reduction, enchantments, and durability.",
                ModuleCategory.COMBAT);
        minimumUpgrade = addSetting(new NumberSetting("minimum_upgrade", "Minimum upgrade", 0.0, 8.0, 0.25, 0.25));
        minimumDurability = addSetting(new NumberSetting("minimum_durability", "Minimum durability %", 0.0, 50.0, 1.0, 5.0));
        equipDelay = addSetting(new NumberSetting("equip_delay", "Equip delay (ticks)", 1.0, 10.0, 1.0, 4.0));
        avoidBindingCurse = addSetting(new BooleanSetting("avoid_binding_curse", "Avoid Binding Curse", true));
        saveArmor = addSetting(new BooleanSetting("save_armor", "Prioritize durable replacements", true));
        durabilityThreshold = addSetting(new NumberSetting("durability_threshold", "Save threshold (durability points)",
                0.0, 100.0, 1.0, 24.0));
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

        HolderLookup<Enchantment> enchantments = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Map<EquipmentSlot, ArmorStats> worn = new EnumMap<>(EquipmentSlot.class);
        ArmorStats totals = ArmorStats.ZERO;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ArmorStats stats = armorStats(player.getItemBySlot(slot), slot, enchantments);
            worn.put(slot, stats);
            totals = totals.plus(stats);
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ArmorStats currentlyWorn = worn.get(slot);
            double currentScore = score(totals);
            boolean currentArmorIsDurable = !saveArmor.value() || isDurableForSave(player.getItemBySlot(slot));
            int bestInventorySlot = -1;
            double bestScore = currentScore;
            boolean bestCandidateIsDurable = currentArmorIsDurable;

            for (int inventorySlot = 0; inventorySlot < 36; inventorySlot++) {
                ItemStack candidate = player.getInventory().getItem(inventorySlot);
                Equippable equippable = candidate.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
                if (candidate.isEmpty() || equippable == null || equippable.slot() != slot
                        || !hasEnoughDurability(candidate)
                        || (avoidBindingCurse.value() && enchantmentLevel(enchantments, candidate, Enchantments.BINDING_CURSE) > 0)) {
                    continue;
                }

                ArmorStats candidateStats = armorStats(candidate, slot, enchantments);
                double candidateScore = score(totals.minus(currentlyWorn).plus(candidateStats));
                boolean candidateIsDurable = !saveArmor.value() || isDurableForSave(candidate);
                if (ArmorSelectionPolicy.shouldSelect(currentArmorIsDurable, candidateIsDurable,
                        bestCandidateIsDurable, candidateScore, bestScore, minimumUpgrade.value())) {
                    bestScore = candidateScore;
                    bestCandidateIsDurable = candidateIsDurable;
                    bestInventorySlot = inventorySlot;
                }
            }

            if (bestInventorySlot >= 0) {
                equipFromInventory(client, bestInventorySlot, slot);
                nextEquipTick = client.level.getGameTime() + (long) equipDelay.value();
                return;
            }
        }
    }

    private boolean hasEnoughDurability(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return true;
        }
        double remainingPercent = 100.0 * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
        return remainingPercent >= minimumDurability.value();
    }

    private boolean isDurableForSave(ItemStack stack) {
        return !stack.isEmpty() && (!stack.isDamageableItem()
                || stack.getMaxDamage() - stack.getDamageValue() > durabilityThreshold.value());
    }

    private static ArmorStats armorStats(ItemStack stack, EquipmentSlot slot, HolderLookup<Enchantment> enchantments) {
        if (stack.isEmpty()) {
            return ArmorStats.ZERO;
        }

        ItemAttributeModifiers attributes = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
        double armor = attributes == null ? 0.0 : attributes.compute(Attributes.ARMOR, 0.0, slot);
        double toughness = attributes == null ? 0.0 : attributes.compute(Attributes.ARMOR_TOUGHNESS, 0.0, slot);
        double knockbackResistance = attributes == null ? 0.0
                : attributes.compute(Attributes.KNOCKBACK_RESISTANCE, 0.0, slot);
        double durability = stack.isDamageableItem()
                ? (double) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage() : 1.0;

        return new ArmorStats(armor, toughness, knockbackResistance,
                enchantmentLevel(enchantments, stack, Enchantments.PROTECTION),
                enchantmentLevel(enchantments, stack, Enchantments.PROJECTILE_PROTECTION),
                enchantmentLevel(enchantments, stack, Enchantments.FIRE_PROTECTION),
                enchantmentLevel(enchantments, stack, Enchantments.BLAST_PROTECTION),
                enchantmentLevel(enchantments, stack, Enchantments.FEATHER_FALLING),
                enchantmentLevel(enchantments, stack, Enchantments.THORNS),
                enchantmentLevel(enchantments, stack, Enchantments.RESPIRATION),
                enchantmentLevel(enchantments, stack, Enchantments.AQUA_AFFINITY),
                enchantmentLevel(enchantments, stack, Enchantments.UNBREAKING), durability);
    }

    private static int enchantmentLevel(HolderLookup<Enchantment> enchantments, ItemStack stack,
                                        ResourceKey<Enchantment> enchantment) {
        return EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(enchantment), stack);
    }

    private static double score(ArmorStats stats) {
        return ArmorScoring.score(EXPECTED_DAMAGE, stats.armor(), stats.toughness(), stats.knockbackResistance(),
                stats.protection(), stats.projectileProtection(), stats.fireProtection(), stats.blastProtection(),
                stats.featherFalling(), stats.thorns(), stats.respiration(), stats.aquaAffinity(), stats.unbreaking(),
                stats.durability());
    }

    private static void equipFromInventory(Minecraft client, int inventorySlot, EquipmentSlot equipmentSlot) {
        int sourceSlot = inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
        int armorSlot = InventoryMenu.ARMOR_SLOT_START + EquipmentSlot.HEAD.getIndex() - equipmentSlot.getIndex();
        int menuId = client.player.inventoryMenu.containerId;
        client.gameMode.handleInventoryMouseClick(menuId, sourceSlot, 0, ClickType.PICKUP, client.player);
        client.gameMode.handleInventoryMouseClick(menuId, armorSlot, 0, ClickType.PICKUP, client.player);
        client.gameMode.handleInventoryMouseClick(menuId, sourceSlot, 0, ClickType.PICKUP, client.player);
    }

    public NumberSetting minimumUpgrade() {
        return minimumUpgrade;
    }

    public NumberSetting minimumDurability() {
        return minimumDurability;
    }

    public NumberSetting equipDelay() {
        return equipDelay;
    }

    public BooleanSetting avoidBindingCurse() {
        return avoidBindingCurse;
    }

    public BooleanSetting saveArmor() {
        return saveArmor;
    }

    public NumberSetting durabilityThreshold() {
        return durabilityThreshold;
    }

    private record ArmorStats(double armor, double toughness, double knockbackResistance,
                              int protection, int projectileProtection, int fireProtection, int blastProtection,
                              int featherFalling, int thorns, int respiration, int aquaAffinity, int unbreaking,
                              double durability) {
        private static final ArmorStats ZERO = new ArmorStats(0.0, 0.0, 0.0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0);

        private ArmorStats plus(ArmorStats other) {
            return new ArmorStats(armor + other.armor, toughness + other.toughness,
                    knockbackResistance + other.knockbackResistance,
                    protection + other.protection, projectileProtection + other.projectileProtection,
                    fireProtection + other.fireProtection, blastProtection + other.blastProtection,
                    featherFalling + other.featherFalling, thorns + other.thorns,
                    respiration + other.respiration, aquaAffinity + other.aquaAffinity,
                    unbreaking + other.unbreaking, durability + other.durability);
        }

        private ArmorStats minus(ArmorStats other) {
            return new ArmorStats(armor - other.armor, toughness - other.toughness,
                    knockbackResistance - other.knockbackResistance,
                    protection - other.protection, projectileProtection - other.projectileProtection,
                    fireProtection - other.fireProtection, blastProtection - other.blastProtection,
                    featherFalling - other.featherFalling, thorns - other.thorns,
                    respiration - other.respiration, aquaAffinity - other.aquaAffinity,
                    unbreaking - other.unbreaking, durability - other.durability);
        }
    }
}
