package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Makes you able to set a custom time. The server keeps sending its own day time; {@code TimeChangerMixin}
 * intercepts it so the forced time sticks, and turning the module off restores the server's clock (advanced by
 * the ticks that passed meanwhile). Ported from Anubis Client (TimeChangerModule + MixinTimeChangerClientLevel, GPL-3.0, Horcus and 4ldenz).
 */
public final class TimeChangerModule extends ClientModule {
    public static volatile TimeChangerModule active;
    private final NumberSetting time = addSetting(new NumberSetting("time", "Time", -20000, 20000, 1, 0));
    private ClientLevel trackedLevel;
    private long serverDayTime;
    private long appliedDayTime;

    public TimeChangerModule() {
        super("time_changer", "Time Changer", "Makes you able to set a custom time.", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable(Minecraft client) {
        active = this;
        if (client.level != null) applyForcedTime(client.level);
    }

    @Override
    protected void onDisable(Minecraft client) {
        active = null;
        ClientLevel level = client.level;
        if (level != null && level == trackedLevel) writeDayTime(level, currentServerDayTime(level));
        trackedLevel = null;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.level == null) trackedLevel = null;
        else applyForcedTime(client.level);
    }

    private void applyForcedTime(ClientLevel level) {
        serverDayTime = level == trackedLevel ? currentServerDayTime(level) : level.getDayTime();
        trackedLevel = level;
        appliedDayTime = forcedDayTime();
        writeDayTime(level, appliedDayTime);
    }

    private static void writeDayTime(ClientLevel level, long dayTime) {
        if (level.getDayTime() != dayTime) {
            level.getLevelData().setDayTime(dayTime);
            level.environmentAttributes().invalidateTickCache();
        }
    }

    private long currentServerDayTime(ClientLevel level) {
        return serverDayTime + (level.getDayTime() - appliedDayTime);
    }

    /** Called with each server time update; returns the time to store instead. */
    public long interceptServerDayTime(ClientLevel level, long dayTime) {
        trackedLevel = level;
        serverDayTime = dayTime;
        appliedDayTime = forcedDayTime();
        return appliedDayTime;
    }

    private long forcedDayTime() {
        return (long) time.value();
    }
}
