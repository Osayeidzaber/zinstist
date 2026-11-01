package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;

public final class AutoEatModule extends ClientModule {
    private final NumberSetting hungerThreshold;
    private final NumberSetting healthThreshold;
    private final BooleanSetting useOffhand;
    private int previousSlot = -1;
    private int eatingSlot = -1;
    private boolean restorePending;

    public AutoEatModule() {
        super("auto_eat", "AutoEat", "Uses the best safe food from your hotbar or offhand when hunger or health is low.",
                ModuleCategory.PLAYER);
        hungerThreshold = addSetting(new NumberSetting("hunger_threshold", "Hunger threshold", 1.0, 19.0,
                1.0, 16.0));
        healthThreshold = addSetting(new NumberSetting("health_threshold", "Health threshold (0 disables)",
                0.0, 20.0, 0.5, 10.0));
        useOffhand = addSetting(new BooleanSetting("use_offhand", "Use offhand food", true));
    }

    @Override
    protected void onEnable(Minecraft client) {
        clearState();
    }

    @Override
    protected void onDisable(Minecraft client) {
        restoreSlotIfOwned(client);
        clearState();
    }

    @Override
    protected void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            clearState();
            return;
        }

        if (restorePending) {
            if (player.isUsingItem()) {
                return;
            }
            restoreSlotIfOwned(client);
            clearState();
        }

        if (!canStart(client, player) || !AutoEatPolicy.shouldEat(player.getFoodData().getFoodLevel(),
                player.getHealth(), (int) hungerThreshold.value(), (float) healthThreshold.value())) {
            return;
        }

        FoodChoice choice = findBestFood(player);
        if (choice == null) {
            return;
        }

        if (choice.hand() == InteractionHand.OFF_HAND) {
            InteractionResult result = client.gameMode.useItem(player, InteractionHand.OFF_HAND);
            if (result.consumesAction()) {
                restorePending = true;
            }
            return;
        }

        previousSlot = player.getInventory().getSelectedSlot();
        eatingSlot = choice.slot();
        if (previousSlot != eatingSlot) {
            player.getInventory().setSelectedSlot(eatingSlot);
        }
        InteractionResult result = client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            restorePending = true;
        } else {
            restoreSlotIfOwned(client);
            clearState();
        }
    }

    private boolean canStart(Minecraft client, Player player) {
        if (client.screen != null || player.isUsingItem() || client.options.keyUse.isDown()
                || player.isPassenger() || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        var modules = JilatonClient.moduleManager();
        ClientModule potion = modules.get("auto_pot");
        if (potion instanceof AutoPotModule autoPot && autoPot.enabled() && autoPot.blocksAutomaticFoodUse(client)) {
            return false;
        }
        ClientModule gapple = modules.get("auto_gapple");
        if (gapple instanceof AutoGappleModule autoGapple && autoGapple.enabled()
                && (autoGapple.isManagingHotbar() || autoGapple.willUseNow(client))) {
            return false;
        }
        ClientModule soup = modules.get("auto_soup");
        if (soup instanceof AutoSoupModule autoSoup && autoSoup.enabled()
                && (autoSoup.isManagingHotbar() || autoSoup.willUseNow(client))) {
            return false;
        }
        ClientModule head = modules.get("auto_head");
        if (head instanceof AutoHeadModule autoHead && autoHead.enabled() && autoHead.isManagingHotbar()) {
            return false;
        }
        ClientModule rod = modules.get("auto_rod");
        if (rod instanceof AutoRodModule autoRod && autoRod.enabled() && autoRod.isManagingHotbar()) {
            return false;
        }
        ClientModule totem = modules.get("auto_totem");
        if (totem instanceof AutoTotemModule autoTotem && autoTotem.enabled() && autoTotem.busy()) {
            return false;
        }
        ClientModule aura = modules.get("kill_aura");
        return !(aura instanceof KillAuraModule killAura && killAura.enabled() && killAura.currentTarget() != null);
    }

    private FoodChoice findBestFood(Player player) {
        boolean lowHealth = healthThreshold.value() > 0.0 && player.getHealth() <= healthThreshold.value();
        boolean hungry = player.getFoodData().getFoodLevel() <= hungerThreshold.value();
        FoodChoice best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            FoodProperties food = usableFood(player, stack);
            if (food == null) {
                continue;
            }
            float score = AutoEatPolicy.score(food.nutrition(), food.saturation(), food.canAlwaysEat(), lowHealth);
            if (score > bestScore) {
                bestScore = score;
                best = new FoodChoice(InteractionHand.MAIN_HAND, slot);
            }
        }
        if (useOffhand.value()) {
            FoodProperties food = usableFood(player, player.getOffhandItem());
            if (food != null) {
                float score = AutoEatPolicy.score(food.nutrition(), food.saturation(), food.canAlwaysEat(), lowHealth);
                if (score > bestScore) {
                    best = new FoodChoice(InteractionHand.OFF_HAND, -1);
                }
            }
        }
        return best;
    }

    private static FoodProperties usableFood(Player player, ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.ROTTEN_FLESH) || stack.is(Items.SPIDER_EYE)
                || stack.is(Items.POISONOUS_POTATO) || stack.is(Items.PUFFERFISH)
                || stack.is(Items.SUSPICIOUS_STEW) || stack.is(Items.CHORUS_FRUIT)) {
            return null;
        }
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null || !player.canEat(food.canAlwaysEat())) {
            return null;
        }
        return food;
    }

    private void restoreSlotIfOwned(Minecraft client) {
        Player player = client.player;
        if (player == null) {
            return;
        }
        if (HotbarSlotRestore.shouldRestore(
                player.getInventory().getSelectedSlot(), eatingSlot, previousSlot)) {
            player.getInventory().setSelectedSlot(previousSlot);
        }
    }

    private void clearState() {
        previousSlot = -1;
        eatingSlot = -1;
        restorePending = false;
    }

    private record FoodChoice(InteractionHand hand, int slot) {
    }
}
