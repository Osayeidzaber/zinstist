package dev.jilaton.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientThemeTest {
    @Test
    void resolvesBuiltInAndCustomAccentColors() {
        assertEquals(ClientTheme.BLUE.accent(), ClientTheme.resolveAccent("blue", "000000"));
        assertEquals(0xFF12ABEF, ClientTheme.resolveAccent("custom", "12ABEF"));
    }

    @Test
    void invalidThemeOrColorFallsBackToJilatonObsidian() {
        assertEquals(ClientTheme.OBSIDIAN.accent(), ClientTheme.resolveAccent("missing", "000000"));
        assertEquals(ClientTheme.OBSIDIAN.accent(), ClientTheme.resolveAccent("custom", "bad"));
    }
}
