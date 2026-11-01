package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Uses server-provided player-head consumables at low health, with hotbar restoration. */
public final class AutoHeadModule extends ClientModule {
    private static final long MIN_RETRY_TICKS = 5L;

    private final NumberSetting healthPercent;
    private final NumberSetting maxAbsorption;
    private final NumberSetting cooldownSeconds;
    private final BooleanSetting considerAbsorption;
    private Player usingPlayer;
    private int previousSlot = -1;
    private int headSlot = -1;
    private boolean offhandUse;
    private boolean restorePending;
    private long nextAttemptTick;

    public AutoHeadModule() {
        super("auto_head", "AutoHead", "Uses a PvP server's player-head heal at low health.",
                ModuleCategory.COMBAT);
        healthPercent = addSetting(new NumberSetting("health_percent", "Health threshold", 1.0, 100.0,
                1.0, 40.0));
        maxAbsorption = addSetting(new NumberSetting("max_absorption", "Maximum absorption", 0.0, 8.0,
                0.1, 1.0));
        cooldownSeconds = addSetting(new NumberSetting("cooldown_seconds", "Use cooldown", 0.0, 120.0,
                0.1, 0.0));
        considerAbsorption = addSetting(new BooleanSetting("consider_absorption", "Count absorption", true));
    }

    @Override
    protected void onEnable(Minecraft client) {
        clearUseState();
        nextAttemptTick = 0L;
    }

    @Override
    protected void onDisable(Minecraft client) {
        restoreSlotIfSafe();
        clearUseState();
        nextAttemptTick = 0L;
    }

    @Override
    protected void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            restoreSlotIfSafe();
            clearUseState();
            nextAttemptTick = 0L;
            return;
        }

        if (restorePending) {
            if (player == usingPlayer && player.isUsingItem()) {
                return;
            }
            restoreSlotIfSafe();
            clearUseState();
            return;
        }

        ClientModule autoPot = JilatonClient.moduleManager().get("auto_pot");
        if (autoPot instanceof AutoPotModule potion && potion.enabled()
                && potion.blocksAutomaticFoodUse(client)) {
            return;
        }
        ClientModule autoGapple = JilatonClient.moduleManager().get("auto_gapple");
        if (autoGapple instanceof AutoGappleModule gapple && gapple.enabled()
                && (gapple.isManagingHotbar() || gapple.willUseNow(client))) {
            return;
        }
        ClientModule autoSoup = JilatonClient.moduleManager().get("auto_soup");
        if (autoSoup instanceof AutoSoupModule soup && soup.enabled()
                && (soup.isManagingHotbar() || soup.willUseNow(client))) {
            return;
        }

        if (client.screen != null || player.isUsingItem() || client.options.keyUse.isDown()
                || client.level.getGameTime() < nextAttemptTick) {
            return;
        }

        boolean useOffhand = player.getOffhandItem().is(Items.PLAYER_HEAD);
        int slot = useOffhand ? -1 : findHeadSlot(player);
        boolean hasHead = useOffhand || slot >= 0;
        boolean safeContext = client.screen == null && !player.isUsingItem() && !client.options.keyUse.isDown();
        if (!AutoHeadPolicy.shouldUse(player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount(),
                healthPercent.value(), maxAbsorption.value(), considerAbsorption.value(), hasHead,
                safeContext, client.level.getGameTime() >= nextAttemptTick)) {
            return;
        }

        usingPlayer = player;
        offhandUse = useOffhand;
        if (offhandUse) {
            headSlot = -1;
            previousSlot = -1;
        } else {
            headSlot = slot;
            previousSlot = player.getInventory().getSelectedSlot();
            if (previousSlot != headSlot) {
                player.getInventory().setSelectedSlot(headSlot);
            }
        }
        restorePending = true;
        long gameTick = client.level.getGameTime();
        long configuredCooldown = Math.round(cooldownSeconds.value() * 20.0);
        nextAttemptTick = gameTick + Math.max(MIN_RETRY_TICKS, configuredCooldown);
        client.gameMode.useItem(player, offhandUse ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    private static int findHeadSlot(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.PLAYER_HEAD)) {
                return slot;
            }
        }
        return -1;
    }

    private void restoreSlotIfSafe() {
        if (usingPlayer != null && !offhandUse
                && HotbarSlotRestore.shouldRestore(
                        usingPlayer.getInventory().getSelectedSlot(), headSlot, previousSlot)) {
            usingPlayer.getInventory().setSelectedSlot(previousSlot);
        }
    }

    private void clearUseState() {
        usingPlayer = null;
        previousSlot = -1;
        headSlot = -1;
        offhandUse = false;
        restorePending = false;
    }

    public boolean isManagingHotbar() {
        return restorePending;
    }

    public NumberSetting healthPercent() {
        return healthPercent;
    }

    public NumberSetting maxAbsorption() {
        return maxAbsorption;
    }

    public NumberSetting cooldownSeconds() {
        return cooldownSeconds;
    }

    public BooleanSetting considerAbsorption() {
        return considerAbsorption;
    }

    public boolean acceptsRefillItem(ItemStack stack) {
        return enabled() && stack.is(Items.PLAYER_HEAD);
    }
}
