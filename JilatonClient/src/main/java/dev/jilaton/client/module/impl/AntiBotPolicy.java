package dev.jilaton.client.module.impl;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Stateless checks adapted from LiquidBounce's Custom AntiBot conditions. */
public final class AntiBotPolicy {
    private static final int MAX_NORMAL_ENTITY_ID = 1_000_000_000;

    private AntiBotPolicy() {
    }

    public static boolean hasIllegalPitch(float pitch) {
        return !Float.isFinite(pitch) || Math.abs(pitch) > 90.0F;
    }

    public static boolean hasSuspiciousEntityId(int entityId) {
        return entityId < 0 || entityId > MAX_NORMAL_ENTITY_ID;
    }

    public static boolean hasDuplicateProfile(String name, UUID id, Map<String, Set<UUID>> profileIdsByName) {
        if (name == null || id == null || profileIdsByName == null) {
            return false;
        }
        Set<UUID> profiles = profileIdsByName.get(name.toLowerCase(Locale.ROOT));
        return profiles != null && (profiles.size() > 1 || !profiles.isEmpty() && !profiles.contains(id));
    }
}
