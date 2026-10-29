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

public final class TriggerbotModule extends ClientModule {
    private final NumberSetting reactionDelay;
    private final BooleanSetting playersOnly;
    private int targetId = -1;
    private long targetSinceTick;

    public TriggerbotModule() {
        super("triggerbot", "Triggerbot", "Attacks a living target under the crosshair after a short delay.", ModuleCategory.COMBAT);
        reactionDelay = addSetting(new NumberSetting("reaction_delay", "Reaction delay (ticks)", 0.0, 10.0, 1.0, 2.0));
        playersOnly = addSetting(new BooleanSetting("players_only", "Players only", true));
    }

    @Override
    protected void onEnable(Minecraft client) {
        resetTarget();
    }

    @Override
    protected void onDisable(Minecraft client) {
        resetTarget();
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null || client.screen != null
                || !(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target) || target == client.player
                || (playersOnly.value() && !(target instanceof Player))
                || (target instanceof Player candidate
                        ? !PlayerTargetFilters.allowsPlayer(client, client.player, candidate)
                        : client.player.isAlliedTo(target))) {
            resetTarget();
            return;
        }

        long gameTick = client.level.getGameTime();
        if (target.getId() != targetId) {
            targetId = target.getId();
            targetSinceTick = gameTick;
        }
        if (gameTick - targetSinceTick < (long) reactionDelay.value()) {
            return;
        }
        if (client.player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }

        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
    }

    private void resetTarget() {
        targetId = -1;
        targetSinceTick = 0L;
    }
}
