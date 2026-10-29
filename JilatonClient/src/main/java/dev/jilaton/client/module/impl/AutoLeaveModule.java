package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.ThreadLocalRandom;

/** Gracefully disconnects from multiplayer when health stays below a configured threshold. */
public final class AutoLeaveModule extends ClientModule {
    private final NumberSetting healthThreshold;
    private final NumberSetting minimumDelay;
    private final NumberSetting maximumDelay;

    private int lowHealthTicks;
    private int sampledDelay = -1;

    public AutoLeaveModule() {
        super("auto_leave", "AutoLeave", "Gracefully leave a multiplayer server at low health.",
                ModuleCategory.COMBAT);
        healthThreshold = addSetting(new NumberSetting("health_threshold", "Health threshold (HP)",
                0.0, 20.0, 0.5, 8.0));
        minimumDelay = addSetting(new NumberSetting("minimum_delay", "Minimum delay (ticks)",
                0.0, 60.0, 1.0, 0.0));
        maximumDelay = addSetting(new NumberSetting("maximum_delay", "Maximum delay (ticks)",
                0.0, 60.0, 1.0, 0.0));
    }

    @Override
    protected void onEnable(Minecraft client) {
        resetDelay();
    }

    @Override
    protected void onDisable(Minecraft client) {
        resetDelay();
    }

    @Override
    protected void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null) {
            resetDelay();
            return;
        }

        boolean hasDeathProtection = player.getMainHandItem().has(DataComponents.DEATH_PROTECTION)
                || player.getOffhandItem().has(DataComponents.DEATH_PROTECTION);
        boolean shouldDisconnect = AutoLeavePolicy.shouldDisconnect(client.getConnection() != null,
                client.hasSingleplayerServer(), player.getAbilities().instabuild || player.isSpectator(),
                hasDeathProtection, player.getHealth(), healthThreshold.value());
        if (!shouldDisconnect) {
            resetDelay();
            return;
        }

        if (sampledDelay < 0) {
            int low = AutoLeavePolicy.clampDelay((int) Math.round(minimumDelay.value()));
            int high = AutoLeavePolicy.clampDelay((int) Math.round(maximumDelay.value()));
            if (low > high) {
                int swap = low;
                low = high;
                high = swap;
            }
            sampledDelay = ThreadLocalRandom.current().nextInt(low, high + 1);
        }

        lowHealthTicks = Math.min(lowHealthTicks + 1, 61);
        if (AutoLeavePolicy.delayElapsed(lowHealthTicks, sampledDelay)) {
            client.disconnectWithProgressScreen();
            setEnabled(false);
        }
    }

    private void resetDelay() {
        lowHealthTicks = 0;
        sampledDelay = -1;
    }
}
