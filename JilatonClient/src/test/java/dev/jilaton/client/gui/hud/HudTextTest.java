package dev.jilaton.client.gui.hud;

import org.junit.jupiter.api.Test;

import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudTextTest {
    private static final IntUnaryOperator MONO = character -> 5;
    private static final IntUnaryOperator CAPS = character -> 6;

    @Test void textThatFitsIsReturnedUntouched() {
        String text = "IRON INGOT";
        assertSame(text, HudText.ellipsize(text, MONO, 60));
    }

    @Test void overlongTextKeepsOnlyWholeWords() {
        assertEquals("IRON\u2026", HudText.ellipsize("IRON INGOT", MONO, 40));
    }

    @Test void aSingleLongWordIsCutAndEllipsised() {
        assertEquals("IRONING\u2026", HudText.ellipsize("IRONINGOT", MONO, 40));
    }

    @Test void roomTooSmallForTheEllipsisShowsOnlyTheEllipsis() {
        assertEquals(HudText.ELLIPSIS, HudText.ellipsize("IRON INGOT", MONO, 4));
        assertEquals("", HudText.ellipsize("IRON INGOT", MONO, 0));
    }

    @Test void measuredWidthIsTheSumOfAdvances() {
        assertEquals(30, HudText.width("IRONIN", MONO));
        assertEquals(0, HudText.width("", MONO));
        assertEquals(0, HudText.width(null, MONO));
    }

    /** D2: the held-item label column must fit a two-word item name without cutting a word. */
    @Test void heldItemLabelColumnFitsIronIngot() {
        int room = HudDesign.HELD_ITEM_WIDTH - HudDesign.HELD_ITEM_LABEL_X - HudDesign.HELD_ITEM_PADDING;
        assertTrue(room > 0, "held item label column is empty");
        assertEquals("IRON INGOT", HudText.ellipsize("IRON INGOT", CAPS, room));
    }
}
