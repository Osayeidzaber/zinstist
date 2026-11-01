package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendManagerTest {
    @BeforeEach
    @AfterEach
    void resetFriends() {
        FriendManager.replace(List.of());
    }

    @Test
    void togglingAddsThenRemovesNameWithoutCaseDuplicates() {
        assertTrue(FriendManager.toggle("Player_One"));
        assertTrue(FriendManager.contains("player_one"));
        assertEquals(List.of("player_one"), FriendManager.names());

        assertFalse(FriendManager.toggle("PLAYER_ONE"));
        assertFalse(FriendManager.contains("player_one"));
        assertTrue(FriendManager.names().isEmpty());
    }

    @Test
    void rejectsInvalidPlayerNames() {
        assertThrows(IllegalArgumentException.class, () -> FriendManager.add("name with spaces"));
        assertThrows(IllegalArgumentException.class, () -> FriendManager.toggle(""));
    }

    @Test void malformedRemoteNamesDoNotCrashFriendLookups() {
        assertFalse(FriendManager.contains((String) null));
        assertFalse(FriendManager.contains("NPC name with spaces"));
    }

    @Test void attackGuardHonorsItsModuleAndSetting() {
        FriendsModule module = new FriendsModule();
        FriendManager.add("Player_One");
        assertFalse(module.blocks("Player_One"));
        module.setEnabled(true);
        try {
            assertTrue(module.blocks("PLAYER_ONE"));
            assertFalse(module.blocks("OtherPlayer"));
            module.settings().stream().filter(s -> s.id().equals("protect_attacks"))
                    .map(dev.jilaton.client.setting.BooleanSetting.class::cast).findFirst().orElseThrow().setValue(false);
            assertFalse(module.blocks("Player_One"));
        } finally { module.setEnabled(false); }
    }
}
