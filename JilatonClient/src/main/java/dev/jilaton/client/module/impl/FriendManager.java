package dev.jilaton.client.module.impl;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class FriendManager {
    private static final Set<String> FRIENDS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private FriendManager() {
    }

    public static boolean add(String name) {
        return FRIENDS.add(normalize(name));
    }

    public static boolean remove(String name) {
        return FRIENDS.remove(normalize(name));
    }

    /** Returns true when the player is now a friend, false when they were removed. */
    public static boolean toggle(Player player) {
        return toggle(player.getGameProfile().name());
    }

    /** Returns true when the name is now a friend, false when it was removed. */
    public static boolean toggle(String name) {
        name = normalize(name);
        if (FRIENDS.remove(name)) {
            return false;
        }
        FRIENDS.add(name);
        return true;
    }

    public static boolean contains(Player player) {
        return contains(player.getGameProfile().name());
    }

    public static boolean contains(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}") && FRIENDS.contains(normalize(name));
    }

    public static List<String> names() {
        return List.copyOf(FRIENDS);
    }

    public static void replace(List<String> names) {
        FRIENDS.clear();
        for (String name : names) {
            try {
                FRIENDS.add(normalize(name));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed names from a hand-edited config file.
            }
        }
    }

    private static String normalize(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_]{1,16}")) {
            throw new IllegalArgumentException("Player names must be 1-16 letters, numbers, or underscores");
        }
        return name.toLowerCase(Locale.ROOT);
    }
}
