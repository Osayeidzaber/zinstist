package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Curved Arc neighbours must not share a text row: scene 35 showed the next line printed over the active one. */
class LyricLayoutArcTest {
    @Test
    void adjacentArcLinesAreAtLeastOneTextLineApartVertically() {
        LyricLayout.Params p = new LyricLayout.Params("Curved Arc", 2.3, 0.3, 26, 16, true, 0, 0);
        LyricLayout.Slot previous = LyricLayout.slot(p, -1, 0), active = LyricLayout.slot(p, 0, 0),
                next = LyricLayout.slot(p, 1, 0);
        assertTrue(active.dy() - next.dy() >= 0.25, "next line drop " + (active.dy() - next.dy()));
        assertTrue(previous.dy() - active.dy() >= 0.25, "previous line rise " + (previous.dy() - active.dy()));
    }
}
