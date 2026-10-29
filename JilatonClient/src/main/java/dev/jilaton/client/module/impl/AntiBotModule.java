/*
 * Adapted from LiquidBounce's ModuleAntiBot tab-list filter.
 * Copyright (c) 2015 - 2026 CCBlueX. LiquidBounce is licensed under GPL-3.0-or-later.
 * Source: https://github.com/CCBlueX/LiquidBounce/tree/e033f5aaf05be802dd2ca51782f255119d7d5654
 */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AntiBotModule extends ClientModule {
    private final BooleanSetting ignorePlayersMissingFromTab;
    private final Set<UUID> listedPlayers = new HashSet<>();
    private ClientLevel cachedLevel;
    private long cachedGameTick = Long.MIN_VALUE;

    public AntiBotModule() {
        super("anti_bot", "AntiBot", "Filters player targets using the server's listed-player information.", ModuleCategory.COMBAT);
        ignorePlayersMissingFromTab = addSetting(new BooleanSetting(
                "ignore_players_missing_from_tab", "Ignore players missing from tab", true));
    }

    public boolean allowsTarget(Minecraft client, Player candidate) {
        if (!ignorePlayersMissingFromTab.value() || client.getConnection() == null || client.level == null) {
            return true;
        }

        if (cachedLevel != client.level || cachedGameTick != client.level.getGameTime()) {
            listedPlayers.clear();
            for (PlayerInfo info : client.getConnection().getListedOnlinePlayers()) {
                listedPlayers.add(info.getProfile().id());
            }
            cachedLevel = client.level;
            cachedGameTick = client.level.getGameTime();
        }
        return listedPlayers.contains(candidate.getUUID());
    }

    @Override
    protected void onDisable(Minecraft client) {
        listedPlayers.clear();
        cachedLevel = null;
        cachedGameTick = Long.MIN_VALUE;
    }
}
