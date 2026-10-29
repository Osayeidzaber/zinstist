package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiBotPolicyTest {
    @Test
    void flagsOnlyNonFiniteOrOutOfRangePitch() {
        assertTrue(AntiBotPolicy.hasIllegalPitch(Float.NaN));
        assertTrue(AntiBotPolicy.hasIllegalPitch(90.01F));
        assertTrue(AntiBotPolicy.hasIllegalPitch(-90.01F));
        assertFalse(AntiBotPolicy.hasIllegalPitch(90.0F));
        assertFalse(AntiBotPolicy.hasIllegalPitch(-90.0F));
    }

    @Test
    void flagsEntityIdsOutsideTheObservedNormalRange() {
        assertTrue(AntiBotPolicy.hasSuspiciousEntityId(-1));
        assertTrue(AntiBotPolicy.hasSuspiciousEntityId(1_000_000_001));
        assertFalse(AntiBotPolicy.hasSuspiciousEntityId(0));
        assertFalse(AntiBotPolicy.hasSuspiciousEntityId(1));
        assertFalse(AntiBotPolicy.hasSuspiciousEntityId(1_000_000_000));
    }

    @Test
    void detectsConflictingProfileIdsForSameCaseInsensitiveName() {
        UUID real = UUID.randomUUID();
        UUID duplicate = UUID.randomUUID();
        Map<String, Set<UUID>> profiles = Map.of("player", Set.of(real));

        assertFalse(AntiBotPolicy.hasDuplicateProfile("PLAYER", real, profiles));
        assertTrue(AntiBotPolicy.hasDuplicateProfile("Player", duplicate, profiles));
        assertFalse(AntiBotPolicy.hasDuplicateProfile("other", duplicate, profiles));
    }

    @Test
    void flagsEveryIdentityWhenTheNameHasMultipleProfiles() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Map<String, Set<UUID>> profiles = Map.of("player", Set.of(first, second));

        assertTrue(AntiBotPolicy.hasDuplicateProfile("player", first, profiles));
        assertTrue(AntiBotPolicy.hasDuplicateProfile("player", second, profiles));
    }
}
