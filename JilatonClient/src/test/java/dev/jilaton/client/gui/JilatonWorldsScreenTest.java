package dev.jilaton.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JilatonWorldsScreenTest {
    @Test
    void lastPlayedLabelsUseTheLargestWholeUnit() {
        long now = System.currentTimeMillis();
        assertEquals("just now", JilatonWorldsScreen.ago(now - 5_000));
        assertEquals("5m ago", JilatonWorldsScreen.ago(now - 5 * 60_000 - 1_000));
        assertEquals("3h ago", JilatonWorldsScreen.ago(now - 3 * 3_600_000L - 1_000));
        assertEquals("2d ago", JilatonWorldsScreen.ago(now - 2 * 86_400_000L - 1_000));
        assertEquals("2mo ago", JilatonWorldsScreen.ago(now - 61L * 86_400_000L));
        assertEquals("just now", JilatonWorldsScreen.ago(now + 60_000), "clock skew never shows negative time");
    }
}
