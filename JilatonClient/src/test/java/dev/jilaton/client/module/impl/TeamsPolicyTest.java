package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamsPolicyTest {
    @Test
    void scoreboardAllianceAlwaysMatches() {
        assertTrue(TeamsPolicy.isTeammate(true, null, 0xFF0000, false));
    }

    @Test
    void sameExplicitNameColorMatchesWhenEnabled() {
        assertTrue(TeamsPolicy.isTeammate(false, 0x55FF55, 0x55FF55, true));
    }

    @Test
    void sameNameColorIsIgnoredWhenDisabled() {
        assertFalse(TeamsPolicy.isTeammate(false, 0x55FF55, 0x55FF55, false));
    }

    @Test
    void missingOrDifferentColorsDoNotMatch() {
        assertFalse(TeamsPolicy.isTeammate(false, null, null, true));
        assertFalse(TeamsPolicy.isTeammate(false, 0xFF5555, 0x5555FF, true));
    }

    @Test
    void dyedArmorColorRequiresBothItemsToHaveTheSameExplicitDye() {
        assertTrue(TeamsPolicy.sameExplicitColor(0xAA33CC, 0xAA33CC));
        assertFalse(TeamsPolicy.sameExplicitColor(null, 0xAA33CC));
        assertFalse(TeamsPolicy.sameExplicitColor(0xAA33CC, 0x33AACC));
    }

    @Test void prefixMatchingStripsFormattingAndRequiresAnActualPrefix() {
        assertTrue(TeamsPolicy.samePrefix("§c[RED] Alice", "§4[RED] Bob"));
        assertFalse(TeamsPolicy.samePrefix("[RED] Alice", "[BLUE] Bob"));
        assertFalse(TeamsPolicy.samePrefix("Alice", "Alice"));
        assertFalse(TeamsPolicy.samePrefix(" Alice", " Bob"));
        assertFalse(TeamsPolicy.samePrefix(null, "[RED] Bob"));
    }
}
