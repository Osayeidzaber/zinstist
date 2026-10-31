package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmbiencePrecedenceTest {
    @Test
    void ownerFollowsTheDocumentedOrder() {
        assertEquals(AmbiencePrecedence.Scene.CUSTOM_SKY, AmbiencePrecedence.owner(true, true, true, true));
        assertEquals(AmbiencePrecedence.Scene.CYBERSPACE, AmbiencePrecedence.owner(false, true, true, true));
        assertEquals(AmbiencePrecedence.Scene.GLASS, AmbiencePrecedence.owner(false, false, true, true));
        assertEquals(AmbiencePrecedence.Scene.FULLBRIGHT, AmbiencePrecedence.owner(false, false, false, true));
        assertEquals(AmbiencePrecedence.Scene.CLIENT, AmbiencePrecedence.owner(false, false, false, false));
    }

    @Test
    void accentComesFromTheOwnerOnly() {
        int sky = 0x112233, cyber = 0x445566, glass = 0x778899, client = 0xAABBCC;
        assertEquals(sky, AmbiencePrecedence.accent(AmbiencePrecedence.Scene.CUSTOM_SKY, sky, cyber, glass, client));
        assertEquals(cyber, AmbiencePrecedence.accent(AmbiencePrecedence.Scene.CYBERSPACE, sky, cyber, glass, client));
        assertEquals(glass, AmbiencePrecedence.accent(AmbiencePrecedence.Scene.GLASS, sky, cyber, glass, client));
        assertEquals(client, AmbiencePrecedence.accent(AmbiencePrecedence.Scene.FULLBRIGHT, sky, cyber, glass, client));
        assertEquals(client, AmbiencePrecedence.accent(AmbiencePrecedence.Scene.CLIENT, sky, cyber, glass, client));
    }

    @Test
    void fullbrightBypassesTheNightGateButSkyDoesNot() {
        assertTrue(AmbiencePrecedence.nightGateApplies(true, false));
        assertFalse(AmbiencePrecedence.nightGateApplies(true, true));
        assertFalse(AmbiencePrecedence.nightGateApplies(false, false));
    }

    @Test
    void skyOwnsLightStreaks() {
        assertFalse(AmbiencePrecedence.drawsSkyStreaks(AmbiencePrecedence.Scene.CUSTOM_SKY));
        assertTrue(AmbiencePrecedence.drawsSkyStreaks(AmbiencePrecedence.Scene.CYBERSPACE));
        assertTrue(AmbiencePrecedence.drawsSkyStreaks(AmbiencePrecedence.Scene.CLIENT));
    }
}
