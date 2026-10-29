package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.util.Util;

public final class AutoClickerModule extends ClientModule {
    private final NumberSetting clicksPerSecond;
    private final BooleanSetting playersOnly;
    private long nextAttackAt;

    public AutoClickerModule() {
        super("auto_clicker", "AutoClicker", "Repeats a normal attack while attack is held and a target is under the crosshair.", ModuleCategory.COMBAT);
        clicksPerSecond = addSetting(new NumberSetting("cps", "Clicks per second", 1.0, 20.0, 1.0, 8.0));
        playersOnly = addSetting(new BooleanSetting("players_only", "Players only", true));
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
                || !client.options.keyAttack.isDown() || !(client.hitResult instanceof EntityHitResult hit)) {
            return;
        }

        if (!(hit.getEntity() instanceof LivingEntity target) || target == client.player
                || (playersOnly.value() && !(target instanceof Player))) {
            return;
        }

        if (client.player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }

        long now = Util.getMillis();
        if (now < nextAttackAt) {
            return;
        }

        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
        nextAttackAt = now + Math.round(1000.0 / clicksPerSecond.value());
    }
}
