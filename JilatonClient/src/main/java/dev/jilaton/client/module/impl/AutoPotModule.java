package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class AutoPotModule extends ClientModule {
    private static final float THROW_PITCH = 85.0F;

    private final BooleanSetting heal;
    private final BooleanSetting speed;
    private final BooleanSetting strength;
    private final BooleanSetting regeneration;
    private final BooleanSetting fireResistance;
    private final BooleanSetting jumpBoost;
    private final BooleanSetting waterBreathing;
    private final NumberSetting healthThreshold;
    private final NumberSetting refreshWindow;
    private Player preparedPlayer;
    private int originalSlot = -1;
    private int potionSlot = -1;
    private float originalPitch;
    private long useAtTick;
    private long cooldownUntilTick;
    private long reservedThroughTick = Long.MIN_VALUE;

    public AutoPotModule() {
        super("auto_pot", "AutoPot", "Throws splash healing and configured buff potions when needed.",
                ModuleCategory.COMBAT);
        heal = addSetting(new BooleanSetting("health", "Health", true));
        regeneration = addSetting(new BooleanSetting("regen", "Regen", true));
        strength = addSetting(new BooleanSetting("strength", "Strength", true));
        speed = addSetting(new BooleanSetting("speed", "Speed", true));
        fireResistance = addSetting(new BooleanSetting("fire_resistance", "Fire resistance", true));
        jumpBoost = addSetting(new BooleanSetting("jump_boost", "Jump boost", true));
        waterBreathing = addSetting(new BooleanSetting("water_breathing", "Water breathing", true));
        healthThreshold = addSetting(new NumberSetting("health_threshold", "Health threshold", 1.0, 20.0, 0.5, 8.0));
        refreshWindow = addSetting(new NumberSetting("refresh_window", "Buff refresh window (seconds)",
                0.0, 60.0, 1.0, 5.0));
    }

    @Override
    protected void onEnable(Minecraft client) {
        resetState();
    }

    @Override
    protected void onDisable(Minecraft client) {
        restorePreparedState(client);
        resetState();
    }

    @Override
    protected void onTick(Minecraft client) {
        if (preparedPlayer != null) {
            JilatonClient.moduleManager().rotationManager().suspendRequestsForTick();
            finishPreparedUse(client);
            return;
        }

        Player player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.screen != null
                || player.isUsingItem() || client.options.keyUse.isDown()
                || client.level.getGameTime() < cooldownUntilTick) {
            return;
        }

        ClientModule gappleModule = JilatonClient.moduleManager().get("auto_gapple");
        if (gappleModule instanceof AutoGappleModule autoGapple
                && autoGapple.enabled() && autoGapple.willUseNow(client)) {
            return;
        }

        int slot = -1;
        if (heal.value() && player.getHealth() <= healthThreshold.value()) {
            slot = findPotion(player, true);
        }
        if (slot < 0) {
            slot = findPotion(player, false);
        }
        if (slot < 0) {
            return;
        }

        preparedPlayer = player;
        originalSlot = player.getInventory().getSelectedSlot();
        potionSlot = slot;
        originalPitch = player.getXRot();
        JilatonClient.moduleManager().rotationManager().suspendRequestsForTick();
        player.getInventory().setSelectedSlot(potionSlot);
        player.setXRot(THROW_PITCH);
        useAtTick = client.level.getGameTime() + 1L;
        reservedThroughTick = useAtTick;
    }

    public boolean blocksAutomaticFoodUse(Minecraft client) {
        return preparedPlayer != null || client.level != null
                && client.level.getGameTime() <= reservedThroughTick;
    }

    public boolean isPreparingThrow() {
        return preparedPlayer != null;
    }

    private void finishPreparedUse(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null || client.gameMode == null || player != preparedPlayer
                || client.screen != null || player.isUsingItem() || client.options.keyUse.isDown()
                || player.getInventory().getSelectedSlot() != potionSlot
                || Math.abs(player.getXRot() - THROW_PITCH) > 3.0F
                || player.getHealth() > healthThreshold.value()) {
            restorePreparedState(client);
            clearPreparedState();
            return;
        }

        if (client.level.getGameTime() < useAtTick) {
            return;
        }

        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        player.swing(InteractionHand.MAIN_HAND);
        cooldownUntilTick = client.level.getGameTime() + 60L;
        reservedThroughTick = client.level.getGameTime() + 1L;
        restorePreparedState(client);
        clearPreparedState();
    }

    private int findPotion(Player player, boolean requireHealing) {
        Set<AutoPotPolicy.EffectType> configuredBuffs = enabledBuffs();
        int refreshWindowTicks = (int) Math.round(refreshWindow.value() * 20.0);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(Items.SPLASH_POTION)) {
                continue;
            }
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) {
                continue;
            }
            List<AutoPotPolicy.EffectStatus> effects = new ArrayList<>();
            for (MobEffectInstance effect : contents.getAllEffects()) {
                AutoPotPolicy.EffectType type = effectType(effect);
                MobEffectInstance active = type == AutoPotPolicy.EffectType.HEALING
                        || type == AutoPotPolicy.EffectType.UNSUPPORTED
                        ? null : player.getEffect(effect.getEffect());
                effects.add(new AutoPotPolicy.EffectStatus(type,
                        active == null ? null : active.getDuration()));
            }

            if (AutoPotPolicy.isEligible(effects, requireHealing, heal.value(),
                    player.getHealth() <= healthThreshold.value(), configuredBuffs, refreshWindowTicks)) {
                return slot;
            }
        }
        return -1;
    }

    private Set<AutoPotPolicy.EffectType> enabledBuffs() {
        Set<AutoPotPolicy.EffectType> enabled = EnumSet.noneOf(AutoPotPolicy.EffectType.class);
        if (speed.value()) enabled.add(AutoPotPolicy.EffectType.SPEED);
        if (strength.value()) enabled.add(AutoPotPolicy.EffectType.STRENGTH);
        if (regeneration.value()) enabled.add(AutoPotPolicy.EffectType.REGENERATION);
        if (fireResistance.value()) enabled.add(AutoPotPolicy.EffectType.FIRE_RESISTANCE);
        if (jumpBoost.value()) enabled.add(AutoPotPolicy.EffectType.JUMP_BOOST);
        if (waterBreathing.value()) enabled.add(AutoPotPolicy.EffectType.WATER_BREATHING);
        return enabled;
    }

    public boolean acceptsRefillItem(ItemStack stack, Player player) {
        if (!enabled() || !stack.is(Items.SPLASH_POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || !contents.getAllEffects().iterator().hasNext()) {
            return false;
        }

        List<AutoPotPolicy.EffectStatus> effects = new ArrayList<>();
        for (MobEffectInstance effect : contents.getAllEffects()) {
            AutoPotPolicy.EffectType type = effectType(effect);
            MobEffectInstance active = type == AutoPotPolicy.EffectType.HEALING
                    || type == AutoPotPolicy.EffectType.UNSUPPORTED
                    ? null : player.getEffect(effect.getEffect());
            effects.add(new AutoPotPolicy.EffectStatus(type,
                    active == null ? null : active.getDuration()));
        }
        int refreshWindowTicks = (int) Math.round(refreshWindow.value() * 20.0);
        return AutoPotPolicy.isEligible(effects, false, heal.value(), true,
                enabledBuffs(), refreshWindowTicks);
    }

    private static AutoPotPolicy.EffectType effectType(MobEffectInstance effect) {
        if (effect.is(MobEffects.INSTANT_HEALTH)) return AutoPotPolicy.EffectType.HEALING;
        if (effect.is(MobEffects.SPEED)) return AutoPotPolicy.EffectType.SPEED;
        if (effect.is(MobEffects.STRENGTH)) return AutoPotPolicy.EffectType.STRENGTH;
        if (effect.is(MobEffects.REGENERATION)) return AutoPotPolicy.EffectType.REGENERATION;
        if (effect.is(MobEffects.FIRE_RESISTANCE)) return AutoPotPolicy.EffectType.FIRE_RESISTANCE;
        if (effect.is(MobEffects.JUMP_BOOST)) return AutoPotPolicy.EffectType.JUMP_BOOST;
        if (effect.is(MobEffects.WATER_BREATHING)) return AutoPotPolicy.EffectType.WATER_BREATHING;
        return AutoPotPolicy.EffectType.UNSUPPORTED;
    }

    private void restorePreparedState(Minecraft client) {
        Player player = client.player;
        if (player == null || player != preparedPlayer) {
            return;
        }
        if (potionSlot >= 0 && originalSlot >= 0
                && player.getInventory().getSelectedSlot() == potionSlot) {
            player.getInventory().setSelectedSlot(originalSlot);
        }
        if (Math.abs(player.getXRot() - THROW_PITCH) <= 3.0F) {
            player.setXRot(originalPitch);
        }
    }

    private void clearPreparedState() {
        preparedPlayer = null;
        originalSlot = -1;
        potionSlot = -1;
        useAtTick = 0L;
    }

    private void resetState() {
        clearPreparedState();
        cooldownUntilTick = 0L;
        reservedThroughTick = Long.MIN_VALUE;
    }
}
