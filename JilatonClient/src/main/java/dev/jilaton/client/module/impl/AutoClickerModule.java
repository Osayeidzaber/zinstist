package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.mixin.KeyMappingAccessor;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
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
    private final ChoiceSetting weaponFilter;
    private final BooleanSetting delayOnBlockBreak;
    private final NumberSetting delayPostUseTicks;
    private long nextAttackAt;
    private long nextUseAt;
    private volatile long lastBlockBreakAtNanos = Long.MIN_VALUE;
    private long attackResumeTick = Long.MIN_VALUE;
    private boolean wasUsingItem;

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
        weaponFilter = addSetting(new ChoiceSetting("weapon_filter", "Weapon filter",
                List.of("Any", "Sword", "Axe", "Mace", "Spear / Trident", "Knockback", "Fire Aspect", "Any weapon"),
                "Any"));
        delayOnBlockBreak = addSetting(new BooleanSetting("delay_on_broken", "Delay after breaking blocks", true));
        delayPostUseTicks = addSetting(new NumberSetting("delay_post_use", "Delay after item use (ticks)",
                0.0, 20.0, 1.0, 0.0));
    }

    @Override
    protected void onEnable(Minecraft client) {
        nextAttackAt = 0L;
        nextUseAt = 0L;
        lastBlockBreakAtNanos = Long.MIN_VALUE;
        attackResumeTick = Long.MIN_VALUE;
        wasUsingItem = false;
    }

    @Override
    protected void onDisable(Minecraft client) {
        nextAttackAt = 0L;
        nextUseAt = 0L;
        lastBlockBreakAtNanos = Long.MIN_VALUE;
        attackResumeTick = Long.MIN_VALUE;
        wasUsingItem = false;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.screen != null) {
            return;
        }

        updatePostUseDelay(client);

        tickRightClick(client);
        tickAttack(client);
    }

    @Override
    protected void onOutgoingPacket(Packet<?> packet) {
        if (packet instanceof ServerboundPlayerActionPacket action
                && action.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
            lastBlockBreakAtNanos = System.nanoTime();
        }
    }

    private void tickAttack(Minecraft client) {
        ClientModule criticalsModule = JilatonClient.moduleManager().get("criticals");
        if (criticalsModule instanceof CriticalsModule criticals && criticals.hasPendingAttack()) {
            return;
        }

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

        if (!weaponAllowed(client, client.player.getMainHandItem())) {
            return;
        }

        if (respectAttackCooldown.value() && client.player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }

        long now = System.nanoTime();
        if (AutoClickerPolicy.delayAfterBlockBreak(now, lastBlockBreakAtNanos, delayOnBlockBreak.value())
                || (attackResumeTick != Long.MIN_VALUE && client.level != null
                    && client.level.getGameTime() < attackResumeTick)) {
            return;
        }
        if (now < nextAttackAt) {
            return;
        }

        client.gameMode.attack(client.player, target);
        if (criticalsModule instanceof CriticalsModule criticals && criticals.hasPendingAttack()) {
            return;
        }
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

    private void updatePostUseDelay(Minecraft client) {
        if (client.level == null) {
            wasUsingItem = false;
            attackResumeTick = Long.MIN_VALUE;
            return;
        }
        if (client.player.isUsingItem()) {
            wasUsingItem = true;
        } else if (wasUsingItem) {
            wasUsingItem = false;
            attackResumeTick = AutoClickerPolicy.postUseResumeTick(client.level.getGameTime(),
                    (int) delayPostUseTicks.value());
        }
    }

    private boolean weaponAllowed(Minecraft client, ItemStack stack) {
        String filter = weaponFilter.value();
        boolean sword = stack.is(ItemTags.SWORDS);
        boolean axe = stack.is(ItemTags.AXES);
        boolean mace = stack.is(Items.MACE);
        boolean spear = stack.is(Items.TRIDENT);
        boolean anyWeapon = filter.equals("Any weapon");
        boolean knockback = (filter.equals("Knockback") || anyWeapon)
                && hasEnchantment(client, stack, Enchantments.KNOCKBACK);
        boolean fireAspect = (filter.equals("Fire Aspect") || anyWeapon)
                && hasEnchantment(client, stack, Enchantments.FIRE_ASPECT);
        return AutoClickerWeaponPolicy.matches(filter, sword, axe, mace, spear, knockback, fireAspect);
    }

    private static boolean hasEnchantment(Minecraft client, ItemStack stack,
                                         net.minecraft.resources.ResourceKey<Enchantment> key) {
        Holder<Enchantment> enchantment = client.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) > 0;
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

    public ChoiceSetting weaponFilter() { return weaponFilter; }

    public BooleanSetting delayOnBlockBreak() { return delayOnBlockBreak; }

    public NumberSetting delayPostUseTicks() { return delayPostUseTicks; }
}
