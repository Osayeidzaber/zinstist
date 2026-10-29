package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
    private final ChoiceSetting pattern;
    private long nextAttackAt;

    public AutoClickerModule() {
        super("auto_clicker", "AutoClicker", "Repeats normal attacks on the crosshair target with configurable CPS timing.", ModuleCategory.COMBAT);
        minimumClicksPerSecond = addSetting(new NumberSetting("minimum_cps", "Minimum CPS", 1.0, 20.0, 1.0, 8.0));
        // Keep the original "cps" setting id so existing user configurations remain valid.
        clicksPerSecond = addSetting(new NumberSetting("cps", "Maximum CPS", 1.0, 20.0, 1.0, 8.0));
        playersOnly = addSetting(new BooleanSetting("players_only", "Players only", true));
        requireAttackKey = addSetting(new BooleanSetting("require_attack_key", "Require attack key", true));
        respectAttackCooldown = addSetting(new BooleanSetting("respect_attack_cooldown", "Respect attack cooldown", true));
        pauseWhileUsingItem = addSetting(new BooleanSetting("pause_while_using_item", "Pause while using item", true));
        pattern = addSetting(new ChoiceSetting("pattern", "Click pattern", List.of("Steady", "Normal distribution"),
                "Steady"));
    }

    @Override
    protected void onEnable(Minecraft client) {
        nextAttackAt = 0L;
    }

    @Override
    protected void onDisable(Minecraft client) {
        nextAttackAt = 0L;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.screen != null
                || (requireAttackKey.value() && !client.options.keyAttack.isDown())
                || (pauseWhileUsingItem.value() && client.player.isUsingItem())
                || !(client.hitResult instanceof EntityHitResult hit)) {
            return;
        }

        if (!(hit.getEntity() instanceof LivingEntity target) || target == client.player
                || (playersOnly.value() && !(target instanceof Player))
                || (target instanceof Player candidate
                        ? !PlayerTargetFilters.allowsPlayer(client, client.player, candidate)
                        : client.player.isAlliedTo(target))) {
            return;
        }

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
}
