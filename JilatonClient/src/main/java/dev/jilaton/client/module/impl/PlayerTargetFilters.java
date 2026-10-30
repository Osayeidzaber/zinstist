package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class PlayerTargetFilters {
    private PlayerTargetFilters() {
    }

    public static boolean allowsPlayer(Minecraft client, Player self, Player candidate) {
        ClientModule antiBotModule = JilatonClient.moduleManager().get("anti_bot");
        if (antiBotModule instanceof AntiBotModule antiBot && antiBot.enabled()
                && !antiBot.allowsTarget(client, candidate)) {
            return false;
        }

        ClientModule friendsModule = JilatonClient.moduleManager().get("friends");
        if (friendsModule != null && friendsModule.enabled() && FriendManager.contains(candidate)) {
            return false;
        }

        ClientModule teamsModule = JilatonClient.moduleManager().get("teams");
        return !(teamsModule instanceof TeamsModule teams && teams.enabled() && teams.ignoreTeammates()
                && teams.matches(self, candidate));
    }
}
