package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffects;

public final class CriticalsModule extends ClientModule {
    private final ChoiceSetting mode;
    private final NumberSetting waitTicks;
    private final NumberSetting range;
    private final BooleanSetting playersOnly;
    private final BooleanSetting requireLineOfSight;
    private LivingEntity pendingTarget;
    private Player pendingAttacker;
    private Level pendingWorld;
    private long pendingDeadline = -1L;
    private boolean replaying;

    public CriticalsModule() {
        super("criticals", "Criticals", "Queues ready attacks and replays them in a valid vanilla critical fall window.",
                ModuleCategory.COMBAT);
        mode = addSetting(new ChoiceSetting("mode", "Critical mode", java.util.List.of("Jump", "Wait"), "Jump"));
        waitTicks = addSetting(new NumberSetting("wait_ticks", "Maximum wait (ticks)", 2.0, 12.0, 1.0, 8.0));
        range = addSetting(new NumberSetting("range", "Target range", 2.0, 4.5, 0.1, 3.0));
        playersOnly = addSetting(new BooleanSetting("players_only", "Players only", true));
        requireLineOfSight = addSetting(new BooleanSetting("line_of_sight", "Require line of sight", true));
    }

    /** Returns true when the original attack was queued and must be cancelled. */
    public boolean beforeAttack(Player attacker, Level world, Entity target) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled() || replaying || attacker != client.player || world != client.level || client.screen != null
                || client.gameMode == null) {
            return false;
        }

        if (pendingTarget != null) {
            if (pendingAttacker == attacker && pendingTarget == target) {
                return true;
            }
            clearPending();
        }

        if (!(target instanceof LivingEntity living) || living == attacker || !living.isAlive()
                || living.isSpectator() || (playersOnly.value() && !(living instanceof Player))
                || !allowsPlayer(client, attacker, living) || !withinConfiguredRange(attacker, living)
                || (requireLineOfSight.value() && !attacker.hasLineOfSight(living))
                || attacker.getAttackStrengthScale(0.0F) < 0.9F) {
            return false;
        }

        boolean jumpMode = mode.value().equals("Jump");
        boolean criticalWindow = isCriticalFall(attacker);
        if (!CriticalWindow.shouldQueueAttack(jumpMode, attacker.onGround(), criticalWindow)) {
            return false;
        }

        pendingTarget = living;
        pendingAttacker = attacker;
        pendingWorld = world;
        pendingDeadline = world.getGameTime() + (long) waitTicks.value();
        if (jumpMode && attacker.onGround()) {
            attacker.jumpFromGround();
        }
        return true;
    }

    @Override
    protected void onEnable(Minecraft client) {
        clearPending();
    }

    @Override
    protected void onDisable(Minecraft client) {
        clearPending();
    }

    @Override
    protected void onTick(Minecraft client) {
        if (pendingTarget == null) {
            return;
        }
        if (client.player != pendingAttacker || client.level != pendingWorld || client.gameMode == null
                || client.screen != null || !pendingTarget.isAlive() || pendingTarget.isSpectator()
                || pendingWorld.getGameTime() > pendingDeadline || !withinConfiguredRange(pendingAttacker, pendingTarget)
                || (requireLineOfSight.value() && !pendingAttacker.hasLineOfSight(pendingTarget))) {
            clearPending();
            return;
        }

        if (!isCriticalFall(pendingAttacker)) {
            return;
        }

        LivingEntity target = pendingTarget;
        Player attacker = pendingAttacker;
        clearPending();
        replaying = true;
        try {
            client.gameMode.attack(attacker, target);
            attacker.swing(InteractionHand.MAIN_HAND);
        } finally {
            replaying = false;
        }
    }

    private boolean isCriticalFall(Player player) {
        return CriticalWindow.isDescending(player.onGround(), player.fallDistance, player.getDeltaMovement().y)
                && !player.isInWater() && !player.isInLava() && !player.onClimbable() && !player.isPassenger()
                && !player.isFallFlying() && !player.hasEffect(MobEffects.BLINDNESS);
    }

    private boolean withinConfiguredRange(Player attacker, LivingEntity target) {
        return attacker.distanceTo(target) <= range.value();
    }

    private static boolean allowsPlayer(Minecraft client, Player attacker, LivingEntity target) {
        if (!(target instanceof Player targetPlayer)) {
            return !attacker.isAlliedTo(target);
        }
        return PlayerTargetFilters.allowsPlayer(client, attacker, targetPlayer);
    }

    private void clearPending() {
        pendingTarget = null;
        pendingAttacker = null;
        pendingWorld = null;
        pendingDeadline = -1L;
    }

    public boolean hasPendingAttack() {
        return pendingTarget != null;
    }

    public NumberSetting waitTicks() {
        return waitTicks;
    }

    public ChoiceSetting mode() {
        return mode;
    }

    public NumberSetting range() {
        return range;
    }

    public BooleanSetting playersOnly() {
        return playersOnly;
    }

    public BooleanSetting requireLineOfSight() {
        return requireLineOfSight;
    }
}
