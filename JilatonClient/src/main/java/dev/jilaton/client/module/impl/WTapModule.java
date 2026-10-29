package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.MovementInputEvent;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public final class WTapModule extends ClientModule {
    private final ChoiceSetting mode;
    private final NumberSetting releaseTicks;
    private final BooleanSetting playersOnly;
    private final BooleanSetting onlyForward;
    private long sprintRestoreTick = -1L;
    private boolean restoreSprint;
    private Player pendingAttacker;
    private boolean pendingSprintHit;

    public WTapModule() {
        super("wtap", "WTap", "Applies a configurable W-Tap or S-Tap sprint reset after an attack.", ModuleCategory.COMBAT);
        mode = addSetting(new ChoiceSetting("mode", "Tap mode", List.of("W Tap", "S Tap"), "W Tap"));
        releaseTicks = addSetting(new NumberSetting("release_ticks", "Tap duration (ticks)", 1.0, 3.0, 1.0, 1.0));
        playersOnly = addSetting(new BooleanSetting("players_only", "Players only", true));
        onlyForward = addSetting(new BooleanSetting("only_forward", "Forward movement only", true));
    }

    public void beforeAttack(Player attacker, Level world, Entity target) {
        Minecraft client = Minecraft.getInstance();
        pendingAttacker = attacker;
        pendingSprintHit = enabled() && world.isClientSide() && attacker == client.player && client.level != null
                && client.screen == null && (!playersOnly.value() || target instanceof Player)
                && attacker.isSprinting() && hasEligibleMovement(client)
                && (!mode.value().equals("S Tap") || client.options.keyUp.isDown());
    }

    public void afterAttack(Player attacker) {
        if (!pendingSprintHit || pendingAttacker != attacker || !enabled()) {
            clearPendingAttack();
            return;
        }
        clearPendingAttack();
        if (Minecraft.getInstance().level == null) {
            return;
        }
        restoreSprint = true;
        sprintRestoreTick = Minecraft.getInstance().level.getGameTime() + (long) releaseTicks.value();
        attacker.setSprinting(false);
    }

    @Override
    protected void onEnable(Minecraft client) {
        clearRestoreState();
        clearPendingAttack();
    }

    @Override
    protected void onDisable(Minecraft client) {
        if (restoreSprint) {
            resumeSprintIfRequested(client);
        }
        clearRestoreState();
        clearPendingAttack();
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

    @Override
    protected void onMovementInput(MovementInputEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (!restoreSprint || client.level == null || client.level.getGameTime() > sprintRestoreTick
                || !mode.value().equals("S Tap")) {
            return;
        }

        Input current = event.input();
        Input tapped = SprintTapInput.apply(current, mode.value().equals("S Tap"));
        if (tapped != current) {
            event.setInput(tapped);
        }
    }

    public boolean isTapActive(Minecraft client) {
        return restoreSprint && client.level != null && client.level.getGameTime() <= sprintRestoreTick;
    }

    public NumberSetting releaseTicks() {
        return releaseTicks;
    }

    public ChoiceSetting mode() {
        return mode;
    }

    public BooleanSetting playersOnly() {
        return playersOnly;
    }

    public BooleanSetting onlyForward() {
        return onlyForward;
    }

    private boolean hasEligibleMovement(Minecraft client) {
        if (client.options == null) {
            return false;
        }
        boolean forward = client.options.keyUp.isDown();
        boolean backward = client.options.keyDown.isDown();
        boolean left = client.options.keyLeft.isDown();
        boolean right = client.options.keyRight.isDown();
        if (onlyForward.value()) {
            return forward && !backward && !left && !right;
        }
        return forward || backward || left || right;
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

    private void clearPendingAttack() {
        pendingAttacker = null;
        pendingSprintHit = false;
    }
}
