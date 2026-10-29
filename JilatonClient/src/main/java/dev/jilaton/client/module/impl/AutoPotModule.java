package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
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

public final class AutoPotModule extends ClientModule {
    private static final float THROW_PITCH = 85.0F;

    private final NumberSetting healthThreshold;
    private Player preparedPlayer;
    private int originalSlot = -1;
    private int potionSlot = -1;
    private float originalPitch;
    private long useAtTick;
    private long cooldownUntilTick;
    private long reservedThroughTick = Long.MIN_VALUE;

    public AutoPotModule() {
        super("auto_pot", "AutoPot", "Throws a splash healing potion at your feet below the health threshold.",
                ModuleCategory.COMBAT);
        healthThreshold = addSetting(new NumberSetting("health_threshold", "Health threshold", 1.0, 20.0, 0.5, 8.0));
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
            finishPreparedUse(client);
            return;
        }

        Player player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.screen != null
                || player.isUsingItem() || client.options.keyUse.isDown()
                || player.getHealth() > healthThreshold.value()
                || client.level.getGameTime() < cooldownUntilTick) {
            return;
        }

        ClientModule gappleModule = JilatonClient.moduleManager().get("auto_gapple");
        if (gappleModule instanceof AutoGappleModule autoGapple
                && autoGapple.enabled() && autoGapple.willUseNow(client)) {
            return;
        }

        int slot = findHealingPotion(player);
        if (slot < 0) {
            return;
        }

        preparedPlayer = player;
        originalSlot = player.getInventory().getSelectedSlot();
        potionSlot = slot;
        originalPitch = player.getXRot();
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

    private static int findHealingPotion(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(Items.SPLASH_POTION)) {
                continue;
            }
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) {
                continue;
            }
            for (MobEffectInstance effect : contents.getAllEffects()) {
                if (effect.is(MobEffects.INSTANT_HEALTH)) {
                    return slot;
                }
            }
        }
        return -1;
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
