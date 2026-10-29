package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendManagerTest {
    @AfterEach
    void clearFriends() {
        FriendManager.replace(List.of());
    }

    @Test
    void togglingTheSamePlayerAddsThenRemovesCaseInsensitively() {
        assertTrue(FriendManager.toggle("Example_Player"));
        assertTrue(FriendManager.contains("example_player"));

        assertFalse(FriendManager.toggle("EXAMPLE_PLAYER"));
        assertTrue(FriendManager.names().isEmpty());
    }
}
