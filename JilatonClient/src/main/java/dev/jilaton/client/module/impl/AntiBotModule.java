/*
 * Adapts selected, configurable target filters from LiquidBounce's ModuleAntiBot.
 * Copyright (c) 2015 - 2026 CCBlueX. LiquidBounce is licensed under GPL-3.0-or-later.
 * Source: https://github.com/CCBlueX/LiquidBounce/tree/e033f5aaf05be802dd2ca51782f255119d7d5654
 */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AntiBotModule extends ClientModule {
    private final BooleanSetting ignorePlayersMissingFromTab;
    private final BooleanSetting ignoreDuplicateProfiles;
    private final BooleanSetting ignoreIllegalPitch;
    private final BooleanSetting ignoreSuspiciousEntityIds;
    private final Set<UUID> listedPlayers = new HashSet<>();
    private final Map<String, Set<UUID>> profileIdsByName = new HashMap<>();
    private ClientLevel cachedLevel;
    private ClientPacketListener cachedConnection;
    private long cachedGameTick = Long.MIN_VALUE;

    public AntiBotModule() {
        super("anti_bot", "AntiBot", "Filters player targets using server player-list and entity consistency checks.",
                ModuleCategory.COMBAT);
        ignorePlayersMissingFromTab = addSetting(new BooleanSetting(
                "ignore_players_missing_from_tab", "Ignore players missing from tab", true));
        ignoreDuplicateProfiles = addSetting(new BooleanSetting(
                "ignore_duplicate_profiles", "Ignore duplicate name profiles", false));
        ignoreIllegalPitch = addSetting(new BooleanSetting(
                "ignore_illegal_pitch", "Ignore invalid player pitch", true));
        ignoreSuspiciousEntityIds = addSetting(new BooleanSetting(
                "ignore_suspicious_entity_ids", "Ignore suspicious entity IDs", true));
    }

    public boolean allowsTarget(Minecraft client, Player candidate) {
        if (candidate == null || candidate == client.player) {
            return true;
        }

        ClientPacketListener connection = client.getConnection();
        if (client.level != null) {
            refreshPlayerInfo(client, connection);
        }

        if (ignorePlayersMissingFromTab.value() && connection != null
                && !listedPlayers.contains(candidate.getUUID())) {
            return false;
        }
        if (ignoreDuplicateProfiles.value() && AntiBotPolicy.hasDuplicateProfile(
                candidate.getGameProfile().name(), candidate.getUUID(), profileIdsByName)) {
            return false;
        }
        if (ignoreIllegalPitch.value() && AntiBotPolicy.hasIllegalPitch(candidate.getXRot())) {
            return false;
        }
        if (ignoreSuspiciousEntityIds.value() && AntiBotPolicy.hasSuspiciousEntityId(candidate.getId())) {
            return false;
        }
        return true;
    }

    private void refreshPlayerInfo(Minecraft client, ClientPacketListener connection) {
        long gameTick = client.level.getGameTime();
        if (cachedLevel == client.level && cachedConnection == connection && cachedGameTick == gameTick) {
            return;
        }

        listedPlayers.clear();
        profileIdsByName.clear();
        if (connection != null) {
            for (PlayerInfo info : connection.getListedOnlinePlayers()) {
                UUID profileId = info.getProfile().id();
                listedPlayers.add(profileId);
                String profileName = info.getProfile().name().toLowerCase(Locale.ROOT);
                profileIdsByName.computeIfAbsent(profileName, ignored -> new HashSet<>()).add(profileId);
            }
        }

        cachedLevel = client.level;
        cachedConnection = connection;
        cachedGameTick = gameTick;
    }

    @Override
    protected void onDisable(Minecraft client) {
        listedPlayers.clear();
        profileIdsByName.clear();
        cachedLevel = null;
        cachedConnection = null;
        cachedGameTick = Long.MIN_VALUE;
    }
}
