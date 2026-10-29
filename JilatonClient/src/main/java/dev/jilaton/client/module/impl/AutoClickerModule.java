package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.mixin.KeyMappingAccessor;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class AutoClickerModule extends ClientModule {
    private final NumberSetting clicksPerSecond;
    private final NumberSetting minimumClicksPerSecond;
    private final BooleanSetting playersOnly;
    private final BooleanSetting requireAttackKey;
    private final BooleanSetting respectAttackCooldown;
    private final BooleanSetting pauseWhileUsingItem;
    private final BooleanSetting rightClickEnabled;
    private final NumberSetting rightClickCps;
    private final BooleanSetting rightClickRequiresInput;
    private final BooleanSetting rightClickBlockOnly;
    private final BooleanSetting ignoreSpecialUseItems;
    private final ChoiceSetting pattern;
    private long nextAttackAt;
    private long nextUseAt;

    public AutoClickerModule() {
        super("auto_clicker", "AutoClicker", "Configurable attack and use-button clickers with vanilla interaction handling.", ModuleCategory.COMBAT);
        minimumClicksPerSecond = addSetting(new NumberSetting("minimum_cps", "Minimum CPS", 1.0, 20.0, 1.0, 8.0));
        // Keep the original "cps" setting id so existing user configurations remain valid.
        clicksPerSecond = addSetting(new NumberSetting("cps", "Maximum CPS", 1.0, 20.0, 1.0, 8.0));
        playersOnly = addSetting(new BooleanSetting("players_only", "Players only", true));
        requireAttackKey = addSetting(new BooleanSetting("require_attack_key", "Require attack key", true));
        respectAttackCooldown = addSetting(new BooleanSetting("respect_attack_cooldown", "Respect attack cooldown", true));
        pauseWhileUsingItem = addSetting(new BooleanSetting("pause_while_using_item", "Pause while using item", true));
        rightClickEnabled = addSetting(new BooleanSetting("right_click_enabled", "Right click", false));
        rightClickCps = addSetting(new NumberSetting("right_click_cps", "Right click CPS", 1.0, 20.0, 1.0, 8.0));
        rightClickRequiresInput = addSetting(new BooleanSetting("right_click_requires_input", "Require use key", true));
        rightClickBlockOnly = addSetting(new BooleanSetting("right_click_block_only", "Blocks only", false));
        ignoreSpecialUseItems = addSetting(new BooleanSetting("ignore_special_use_items", "Ignore pearls and buckets", true));
        pattern = addSetting(new ChoiceSetting("pattern", "Click pattern", List.of("Steady", "Normal distribution"),
                "Steady"));
    }

    @Override
    protected void onEnable(Minecraft client) {
        nextAttackAt = 0L;
        nextUseAt = 0L;
    }

    @Override
    protected void onDisable(Minecraft client) {
        nextAttackAt = 0L;
        nextUseAt = 0L;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.screen != null) {
            return;
        }

        tickRightClick(client);
        tickAttack(client);
    }

    private void tickAttack(Minecraft client) {
        if ((requireAttackKey.value() && !client.options.keyAttack.isDown())
                || (pauseWhileUsingItem.value() && client.player.isUsingItem())
                || !(client.hitResult instanceof EntityHitResult hit)) {
            return;
        }

        if (!(hit.getEntity() instanceof LivingEntity target) || target == client.player
                || (playersOnly.value() && !(target instanceof Player))
                || (target instanceof Player candidate
                    ? !PlayerTargetFilters.allowsPlayer(client, client.player, candidate)
                    : client.player.isAlliedTo(target))) return;

        if (respectAttackCooldown.value() && client.player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }

        long now = System.nanoTime();
        if (now < nextAttackAt) {
            return;
        }

        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
        double cps = ClickTiming.sampleCps(minimumClicksPerSecond.value(), clicksPerSecond.value(),
                pattern.value(), ThreadLocalRandom.current().nextGaussian());
        nextAttackAt = now + ClickTiming.intervalNanos(cps);
    }

    /**
     * Queue a vanilla use-key click, rather than calling an item directly. Minecraft then
     * applies its normal block/entity/item interaction rules and per-action cooldowns.
     */
    private void tickRightClick(Minecraft client) {
        if (!rightClickEnabled.value()
                || (rightClickRequiresInput.value() && !client.options.keyUse.isDown())
                || (pauseWhileUsingItem.value() && client.player.isUsingItem())
                || (rightClickBlockOnly.value() && !(client.hitResult instanceof BlockHitResult))
                || (ignoreSpecialUseItems.value() && isSpecialUseItem(client.player.getMainHandItem().getItem()))) {
            return;
        }

        long now = System.nanoTime();
        if (now < nextUseAt) return;

        KeyMapping.click(((KeyMappingAccessor) client.options.keyUse).jilaton$getBoundKey());
        nextUseAt = now + ClickTiming.intervalNanos(rightClickCps.value());
    }

    private static boolean isSpecialUseItem(net.minecraft.world.item.Item item) {
        return item == Items.ENDER_PEARL || item == Items.ENDER_EYE
                || item == Items.WATER_BUCKET || item == Items.LAVA_BUCKET;
    }

    public NumberSetting minimumClicksPerSecond() {
        return minimumClicksPerSecond;
    }

    public NumberSetting clicksPerSecond() {
        return clicksPerSecond;
    }

    public BooleanSetting playersOnly() {
        return playersOnly;
    }

    public BooleanSetting requireAttackKey() {
        return requireAttackKey;
    }

    public BooleanSetting respectAttackCooldown() {
        return respectAttackCooldown;
    }

    public BooleanSetting pauseWhileUsingItem() {
        return pauseWhileUsingItem;
    }

    public ChoiceSetting pattern() {
        return pattern;
    }

    public BooleanSetting rightClickEnabled() { return rightClickEnabled; }

    public NumberSetting rightClickCps() { return rightClickCps; }

    public BooleanSetting rightClickRequiresInput() { return rightClickRequiresInput; }

    public BooleanSetting rightClickBlockOnly() { return rightClickBlockOnly; }

    public BooleanSetting ignoreSpecialUseItems() { return ignoreSpecialUseItems; }
}
