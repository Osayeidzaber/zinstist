package dev.jilaton.client.gui.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Geometry only: the card and everything inside it are multiplied by the same factor. */
class TargetCardScalingTest {
    private static final double[] SCALES = {0.5, 1.0, 1.5};

    @Test
    void everyElementStaysInsideThePanelAtEveryScale() {
        for (boolean chips : new boolean[]{true, false}) {
            for (boolean held : new boolean[]{true, false}) {
                HudDesign.TargetCard base = HudDesign.baseTargetCard(chips, held);
                for (double scale : SCALES) {
                    HudDesign.TargetCard card = base.scaled(scale);
                    String at = "chips=" + chips + " held=" + held + " scale=" + scale + " -> " + card;

                    assertEquals(HudDesign.TARGET_BASE_WIDTH * scale, card.width(), 1.0, "panel width at " + at);
                    assertTrue(card.padding() > 0, "no padding at " + at);
                    assertTrue(card.headX() >= 0 && card.headY() >= 0, "head starts outside at " + at);
                    assertTrue(card.headX() + card.headSize() <= card.contentX(),
                            "head overlaps the text column at " + at);
                    assertTrue(card.contentX() + card.contentWidth() <= card.width() - card.padding(),
                            "content column runs past the panel at " + at);
                    assertTrue(card.nameY() < card.pillY(), "name and pill overlap at " + at);
                    assertTrue(card.pillY() + card.pillHeight() <= card.healthY(), "pill overlaps health at " + at);
                    assertTrue(card.healthY() < card.barY(), "health and bar overlap at " + at);
                    assertTrue(card.barY() + card.barHeight() <= card.ruleY(), "bar overlaps the rule at " + at);
                    assertTrue(card.ruleY() < bottomRow(card, chips), "rule overlaps the last row at " + at);

                    int bottom = chips ? card.chipsY() + card.chipHeight() : card.ruleY() + 1;
                    assertTrue(bottom <= card.height() - card.padding(),
                            "last row runs past the panel at " + at);
                    if (chips) {
                        assertTrue(card.chipWidth() > 0, "empty chip at " + at);
                        assertTrue(card.chipX(2) + card.chipWidth() <= card.contentRight(),
                                "last chip runs past the content column at " + at);
                    } else {
                        assertEquals(-1, card.chipsY(), "hidden chips still reserve a row at " + at);
                    }
                }
            }
        }
    }

    @Test
    void scalingGrowsEveryRectTogether() {
        HudDesign.TargetCard base = HudDesign.baseTargetCard(true, true);
        HudDesign.TargetCard half = base.scaled(0.5);
        HudDesign.TargetCard same = base.scaled(1.0);
        HudDesign.TargetCard wide = base.scaled(1.5);
        assertTrue(half.width() < same.width() && same.width() < wide.width());
        assertTrue(half.headSize() < same.headSize() && same.headSize() < wide.headSize());
        assertTrue(half.contentX() < same.contentX() && same.contentX() < wide.contentX());
        assertEquals(base.width(), same.width());
        assertEquals(base.headSize(), same.headSize());
        assertEquals(base.chipWidth(), same.chipWidth());
    }

    @Test
    void invalidScalesAreRejected() {
        HudDesign.TargetCard card = HudDesign.baseTargetCard(true, true);
        assertThrows(IllegalArgumentException.class, () -> card.scaled(0));
        assertThrows(IllegalArgumentException.class, () -> card.scaled(-1));
        assertThrows(IllegalArgumentException.class, () -> card.scaled(Double.NaN));
    }

    @Test
    void compactCardScalesAsOneUnitToo() {
        HudDesign.CompactCard base = HudDesign.compactTarget();
        for (double scale : SCALES) {
            HudDesign.CompactCard card = base.scaled(scale);
            String at = "scale=" + scale + " -> " + card;
            assertTrue(card.faceX() + card.faceSize() < card.textX(), "face overlaps text at " + at);
            assertTrue(card.textX() < card.width() - card.rightPadding(), "text starts past the panel at " + at);
            assertTrue(card.barY() + card.barHeight() <= card.statusY(), "bar overlaps status at " + at);
            assertTrue(card.statusY() < card.height(), "status line runs past the panel at " + at);
            assertTrue(card.rightPadding() > 0, "no right padding at " + at);
        }
    }

    @Test
    void heldItemPillOnlyEllipsisesWhenTheLabelIsGenuinelyTooWide() {
        int content = 250;
        assertEquals(74, HudDesign.pillWidth(54, content));
        assertTrue(HudDesign.pillTextFits(54, content));
        assertEquals(content, HudDesign.pillWidth(300, content));
        assertFalse(HudDesign.pillTextFits(300, content));
        assertEquals(0, HudDesign.pillWidth(0, content));

        // "IRON INGOT" measured at one pixel per glyph: it fits, so it must survive untouched.
        int ironIngot = 10;
        assertTrue(HudDesign.pillTextFits(ironIngot, content));
        assertEquals("IRON INGOT", HudText.ellipsize("IRON INGOT", ignored -> 1, content));
        assertEquals("IRON INGOT", HudText.ellipsize("IRON INGOT", ignored -> 1,
                HudDesign.pillWidth(ironIngot, content) - HudDesign.PILL_TEXT_INSET));
    }

    private static int bottomRow(HudDesign.TargetCard card, boolean chips) {
        return chips ? card.chipsY() : card.ruleY() + 1;
    }
}
