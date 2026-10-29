package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class WTapModule extends ClientModule {
    private final NumberSetting releaseTicks;
    private final BooleanSetting playersOnly;
    private long sprintRestoreTick = -1L;
    private boolean restoreSprint;

    public WTapModule() {
        super("wtap", "WTap", "Briefly releases sprint after an entity attack, then resumes forward sprinting.", ModuleCategory.COMBAT);
        releaseTicks = addSetting(new NumberSetting("release_ticks", "Sprint release (ticks)", 1.0, 3.0, 1.0, 1.0));
        playersOnly = addSetting(new BooleanSetting("players_only", "Players only", true));

    }

    public void onAttack(Player attacker, Level world, Entity target) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled() || !world.isClientSide() || attacker != client.player || client.level == null
                || client.screen != null || (playersOnly.value() && !(target instanceof Player))
                || !attacker.isSprinting()) {
            return;
        }

        restoreSprint = true;
        sprintRestoreTick = client.level.getGameTime() + (long) releaseTicks.value();
        attacker.setSprinting(false);
    }

    @Override
    protected void onEnable(Minecraft client) {
        clearRestoreState();
    }

    @Override
    protected void onDisable(Minecraft client) {
        if (restoreSprint) {
            resumeSprintIfRequested(client);
        }
        clearRestoreState();
    }

    @Override
    protected void onTick(Minecraft client) {
        if (!restoreSprint) {
            return;
        }
        if (client.player == null || client.level == null) {
            clearRestoreState();
            return;
        }
        if (client.level.getGameTime() < sprintRestoreTick) {
            return;
        }

        resumeSprintIfRequested(client);
        clearRestoreState();
    }

    public NumberSetting releaseTicks() {
        return releaseTicks;
    }

    public BooleanSetting playersOnly() {
        return playersOnly;
    }

    private static void resumeSprintIfRequested(Minecraft client) {
        Player player = client.player;
        if (player != null && client.options != null && player.getFoodData().getFoodLevel() > 6
                && !player.horizontalCollision && client.options.keyUp.isDown()) {
            player.setSprinting(true);
        }
    }

    private void clearRestoreState() {
        sprintRestoreTick = -1L;
        restoreSprint = false;
    }
}
