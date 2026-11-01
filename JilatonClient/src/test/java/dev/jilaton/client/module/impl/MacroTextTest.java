package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MacroTextTest {
    @Test
    void splitsChainedLinesAndDropsEmptyOnes() {
        assertEquals(List.of("/home", "gg"), MacroText.lines(" /home &&  && gg "));
        assertEquals(List.of(), MacroText.lines("  /  "));
        assertEquals(MacroText.MAX_LINE, MacroText.lines("x".repeat(400)).get(0).length());
    }
}
