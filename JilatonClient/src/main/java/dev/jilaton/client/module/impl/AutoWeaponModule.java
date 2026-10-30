/*
 * Adapted from LiquidBounce's ModuleAutoWeapon behavior (preferred hotbar weapon, shield break,
 * mace smash, and optional switch-back). Copyright (c) 2015 - 2026 CCBlueX; GPL-3.0-or-later.
 * Source: https://github.com/CCBlueX/LiquidBounce/tree/e033f5aaf05be802dd2ca51782f255119d7d5654
 */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;

import java.util.List;

public final class AutoWeaponModule extends ClientModule {
    private static final List<String> PREFERENCES = List.of("Best damage", "Sword", "Axe", "Mace", "Trident");

    private final ChoiceSetting preference;
    private final BooleanSetting playersOnly;
    private final BooleanSetting shieldBreak;
    private final BooleanSetting maceSmash;
    private final BooleanSetting switchBack;
    private final NumberSetting switchBackDelay;
    private final NumberSetting minimumAdvantage;

    private int originalSlot = -1;
    private int selectedSlot = -1;
    private long restoreAtTick = Long.MAX_VALUE;

    public AutoWeaponModule() {
        super("auto_weapon", "AutoWeapon", "Selects a stronger hotbar weapon before a valid attack.",
                ModuleCategory.COMBAT);
        preference = addSetting(new ChoiceSetting("preference", "Preferred weapon", PREFERENCES, "Best damage"));
        playersOnly = addSetting(new BooleanSetting("players_only", "Players only", true));
        shieldBreak = addSetting(new BooleanSetting("shield_break", "Prefer axes against shields", true));
        maceSmash = addSetting(new BooleanSetting("mace_smash", "Use mace for a smash", true));
        switchBack = addSetting(new BooleanSetting("switch_back", "Switch back after attack", true));
        switchBackDelay = addSetting(new NumberSetting("switch_back_delay", "Switch back delay (ticks)",
                1.0, 6.0, 1.0, 2.0));
        minimumAdvantage = addSetting(new NumberSetting("minimum_advantage", "Minimum base damage advantage",
                0.0, 4.0, 0.1, 0.2));
    }

    /** Called at the head of vanilla's attack method, before it synchronizes the carried slot. */
    public void beforeAttack(Minecraft client, Player attacker, LivingEntity target) {
        if (!enabled() || client.player != attacker || client.level == null || client.gameMode == null
                || client.screen != null || !attacker.isAlive() || !target.isAlive()
                || playersOnly.value() && !(target instanceof Player)) {
            return;
        }
        if (target instanceof Player targetPlayer
                && !PlayerTargetFilters.allowsPlayer(client, attacker, targetPlayer)) {
            return;
        }
        if (attacker.isUsingItem() && attacker.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            return;
        }
        if (hasConflictingHotbarReservation(client)) {
            return;
        }

        int candidate = -1;
        boolean forced = false;
        if (maceSmash.value() && MaceItem.canSmashAttack(attacker)) {
            candidate = findWeapon(attacker, "Mace", client);
            forced = candidate >= 0;
        }
        if (candidate < 0 && shieldBreak.value() && target instanceof Player targetPlayer
                && targetPlayer.isBlocking()) {
            candidate = findWeapon(attacker, "Axe", client);
            forced = candidate >= 0;
        }
        if (candidate < 0) {
            candidate = findWeapon(attacker, preference.value(), client);
            forced = candidate >= 0 && !preference.value().equals("Best damage");
        }
        if (candidate < 0) {
            return;
        }

        int current = attacker.getInventory().getSelectedSlot();
        if (current == candidate) {
            if (originalSlot >= 0 && switchBack.value()) {
                restoreAtTick = client.level.getGameTime() + (long) switchBackDelay.value();
            }
            return;
        }

        double currentDamage = weaponDamage(attacker, attacker.getInventory().getItem(current), client);
        double candidateDamage = weaponDamage(attacker, attacker.getInventory().getItem(candidate), client);
        if (!AutoWeaponPolicy.shouldSwitch(currentDamage, candidateDamage, minimumAdvantage.value(), forced)) {
            return;
        }

        if (switchBack.value()) {
            if (originalSlot < 0 || !AutoWeaponPolicy.stillOwnsSelectedSlot(current, selectedSlot)) {
                originalSlot = current;
            }
            selectedSlot = candidate;
            restoreAtTick = client.level.getGameTime() + (long) switchBackDelay.value();
        } else {
            clearRestore();
        }
        attacker.getInventory().setSelectedSlot(candidate);
    }

    /** True if this module can select a hotbar axe for the imminent shield-breaking attack. */
    public boolean canSelectShieldBreaker(Minecraft client, Player attacker) {
        return enabled() && shieldBreak.value() && client.player == attacker && client.level != null
                && !hasConflictingHotbarReservation(client) && findWeapon(attacker, "Axe", client) >= 0;
    }

    private boolean hasConflictingHotbarReservation(Minecraft client) {
        ClientModule pot = JilatonClient.moduleManager().get("auto_pot");
        if (pot instanceof AutoPotModule autoPot && autoPot.enabled() && autoPot.blocksAutomaticFoodUse(client)) {
            return true;
        }
        ClientModule gapple = JilatonClient.moduleManager().get("auto_gapple");
        return gapple instanceof AutoGappleModule autoGapple && autoGapple.enabled()
                && (autoGapple.isManagingHotbar() || autoGapple.willUseNow(client));
    }

    private int findWeapon(Player player, String requested, Minecraft client) {
        int bestSlot = -1;
        double bestDamage = Double.NEGATIVE_INFINITY;
        Holder<Enchantment> sharpness = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isWeapon(stack, requested)) {
                continue;
            }
            double damage = weaponDamage(player, stack, client, sharpness);
            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private static boolean isWeapon(ItemStack stack, String requested) {
        boolean sword = stack.is(ItemTags.SWORDS);
        boolean axe = stack.is(ItemTags.AXES);
        boolean mace = stack.is(Items.MACE);
        boolean trident = stack.is(Items.TRIDENT);
        return switch (requested) {
            case "Sword" -> sword;
            case "Axe" -> axe;
            case "Mace" -> mace;
            case "Trident" -> trident;
            default -> sword || axe || mace || trident;
        };
    }

    private static double weaponDamage(Player player, ItemStack stack, Minecraft client) {
        Holder<Enchantment> sharpness = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS);
        return weaponDamage(player, stack, client, sharpness);
    }

    private static double weaponDamage(Player player, ItemStack stack, Minecraft client,
                                       Holder<Enchantment> sharpness) {
        if (!isWeapon(stack, "Best damage")) {
            return Double.NEGATIVE_INFINITY;
        }
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY);
        double damage = modifiers.compute(Attributes.ATTACK_DAMAGE,
                player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), EquipmentSlot.MAINHAND);
        int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(sharpness, stack);
        if (sharpnessLevel > 0) {
            damage += 0.5 * sharpnessLevel + 0.5;
        }
        return damage;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (originalSlot < 0) {
            return;
        }
        if (client.player == null || client.level == null) {
            clearRestore();
            return;
        }
        int current = client.player.getInventory().getSelectedSlot();
        if (!AutoWeaponPolicy.stillOwnsSelectedSlot(current, selectedSlot)) {
            clearRestore();
            return;
        }
        if (!switchBack.value()) {
            clearRestore();
            return;
        }
        if (client.level.getGameTime() >= restoreAtTick) {
            client.player.getInventory().setSelectedSlot(originalSlot);
            clearRestore();
        }
    }

    @Override
    protected void onDisable(Minecraft client) {
        restoreSelectedSlot(client);
    }

    private void restoreSelectedSlot(Minecraft client) {
        if (client.player != null && originalSlot >= 0
                && AutoWeaponPolicy.stillOwnsSelectedSlot(client.player.getInventory().getSelectedSlot(), selectedSlot)) {
            client.player.getInventory().setSelectedSlot(originalSlot);
        }
        clearRestore();
    }

    private void clearRestore() {
        originalSlot = -1;
        selectedSlot = -1;
        restoreAtTick = Long.MAX_VALUE;
    }
}
