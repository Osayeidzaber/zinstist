package dev.jilaton.client.gui;

import dev.jilaton.client.gui.shell.ShellPalette;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ClickGuiAccentTest {
    @Test
    void shellAccentMatchesEveryResolvedTheme() {
        for (ClientTheme theme : ClientTheme.values()) {
            assertEquals(ClientTheme.resolveAccent(theme.id(), null), ClickGuiScreen.accentFor(theme.id(), null),
                    "shell accent disagrees with the theme page for " + theme.id());
            assertEquals(ClientTheme.resolveAccent(theme.id(), "12AB34"),
                    ClickGuiScreen.accentFor(theme.id(), "12AB34"),
                    "a stored custom accent leaked into " + theme.id());
        }
    }

    @Test
    void obsidianStaysWhiteInsteadOfFallingBackToTheShellViolet() {
        assertEquals(0xFFF2F2F2, ClickGuiScreen.accentFor("obsidian", null));
        assertEquals(0xFFF2F2F2, ClickGuiScreen.accentFor("obsidian", "8878FF"));
        assertNotEquals(ShellPalette.ACCENT, ClickGuiScreen.accentFor("obsidian", null));
    }

    @Test
    void customThemeUsesTheStoredHexAndFallsBackLikeTheConfig() {
        assertEquals(0xFF12AB34, ClickGuiScreen.accentFor("custom", "12AB34"));
        assertEquals(ClientTheme.OBSIDIAN.accent(), ClickGuiScreen.accentFor("custom", "not-a-colour"));
        assertEquals(ClientTheme.OBSIDIAN.accent(), ClickGuiScreen.accentFor("custom", null));
        assertEquals(ClientTheme.resolveAccent("custom", ""), ClickGuiScreen.accentFor("custom", ""));
    }

    @Test
    void unknownThemeIdsResolveLikeTheThemePage() {
        assertEquals(ClientTheme.OBSIDIAN.accent(), ClickGuiScreen.accentFor("no-such-theme", null));
    }
}
