package dev.jilaton.client.gui.hud;

import dev.jilaton.client.config.ClientConfig.HudPosition;
import dev.jilaton.client.config.HudStyle;
import dev.jilaton.client.gui.HudLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-12: the target card sits bottom-centre, above the vanilla hotbar. Its default anchor used to be the
 * fixed {@code guiHeight - 76}, which only cleared the hotbar for the 46 px card it was tuned for; the
 * chip row grows the card downward from the same top, so {@code FALL / ARMOR / ABSORB} landed on the
 * hotbar. The anchor is now derived from the card's own height and the hotbar strip.
 */
class TargetCardAnchorTest {
    /** GUI heights a 16:9 window produces from GUI scale 4 on 720p up to GUI scale 1 on 1440p. */
    private static final int[] GUI_HEIGHTS = {240, 270, 360, 480, 540, 720, 1080, 1440};
    /** Widget scales the renderer can produce: preset scale x target scale x auto width factor. */
    private static final double[] WIDGET_SCALES = {0.5, 0.7, 1.0, 1.5, 2.0};

    @Test
    void chipRowAndHeldPillNeverReachTheHotbarAtAnyScaleOrGuiHeight() {
        for (boolean chips : new boolean[]{true, false}) {
            for (boolean held : new boolean[]{true, false}) {
                HudDesign.TargetCard base = HudDesign.baseTargetCard(chips, held);
                for (double scale : WIDGET_SCALES) {
                    int height = (int) Math.ceil(base.height() * scale);
                    for (int guiHeight : GUI_HEIGHTS) {
                        int top = HudDesign.aboveHotbarTop(guiHeight, height);
                        int bottom = top + height;
                        String at = "chips=" + chips + " held=" + held + " scale=" + scale
                                + " guiHeight=" + guiHeight + " -> top " + top + ", bottom " + bottom;
                        assertTrue(bottom <= HudDesign.hotbarTop(guiHeight) - HudDesign.HUD_MARGIN,
                                "card reaches the hotbar at " + at);
                    }
                }
            }
        }
    }

    @Test
    void theRendererDefaultAt1080GuiPixelsNowClearsTheHotbar() {
        int guiHeight = 1080;
        int guiWidth = 1920;
        HudDesign.TargetCard card = HudDesign.baseTargetCard(true, true);
        // Mirrors HudRenderer: the default preset has target scale 1 and an auto width.
        double widgetScale = Math.max(0.5, Math.min(2.0,
                HudDesign.targetCardWidth(guiWidth) / (double) card.width()));
        int height = (int) Math.ceil(card.height() * widgetScale);
        assertEquals(74, height);

        // Before P1-12: the fixed anchor put the 74 px card at y 1004, so its bottom edge (1078)
        // sat 20 px inside the hotbar (top 1058).
        int oldTop = guiHeight - 76;
        assertEquals(1004, oldTop);
        assertEquals(1078, oldTop + height);
        assertTrue(oldTop + height > HudDesign.hotbarTop(guiHeight));

        // After: bottom edge 1050, an 8 px gap above the hotbar top.
        int top = HudDesign.aboveHotbarTop(guiHeight, height);
        assertEquals(976, top);
        assertEquals(1050, top + height);
        assertEquals(1058, HudDesign.hotbarTop(guiHeight));
    }

    @Test
    void theHistoricFixedAnchorIsReproducedForTheCardItWasTunedFor() {
        HudDesign.TargetCard plain = HudDesign.baseTargetCard(false, false);
        assertEquals(46, plain.height());
        for (int guiHeight : GUI_HEIGHTS) {
            assertEquals(guiHeight - 76, HudDesign.aboveHotbarTop(guiHeight, plain.height()),
                    "chips-off, held-off card moved at guiHeight=" + guiHeight);
        }
        // The overlap audit in development/CHECKPOINT.md records the 720p target card top as 644.
        assertEquals(644, HudDesign.aboveHotbarTop(720, plain.height()));
    }

    @Test
    void growingTheChipRowExtendsTheCardUpwardInsteadOfDownward() {
        HudDesign.TargetCard withChips = HudDesign.baseTargetCard(true, true);
        HudDesign.TargetCard withoutChips = HudDesign.baseTargetCard(false, true);
        assertTrue(withChips.height() > withoutChips.height());
        for (int guiHeight : GUI_HEIGHTS) {
            int chipTop = HudDesign.aboveHotbarTop(guiHeight, withChips.height());
            int plainTop = HudDesign.aboveHotbarTop(guiHeight, withoutChips.height());
            assertEquals(chipTop + withChips.height(), plainTop + withoutChips.height(),
                    "bottom edges differ at guiHeight=" + guiHeight);
            assertEquals(guiHeight - HudDesign.ABOVE_HOTBAR_INSET, chipTop + withChips.height());
            assertTrue(chipTop < plainTop, "chip row did not travel upward at guiHeight=" + guiHeight);
        }
    }

    @Test
    void aSavedDragStillWinsOverTheBottomAnchor() {
        HudDesign.TargetCard card = HudDesign.baseTargetCard(true, true);
        for (int guiHeight : GUI_HEIGHTS) {
            int viewportWidth = guiHeight * 16 / 9;
            int defaultTop = HudDesign.aboveHotbarTop(guiHeight, card.height());
            for (double fraction : new double[]{0.0, 0.25, 0.5, 1.0}) {
                HudLayout dragged = HudLayout.resolve(card.width(), card.height(), viewportWidth, guiHeight,
                        0, defaultTop, new HudPosition(0.3, fraction), HudStyle.DEFAULT);
                assertEquals((int) Math.round(Math.max(0, guiHeight - card.height()) * fraction), dragged.y(),
                        "saved fraction was re-anchored at guiHeight=" + guiHeight + " fraction=" + fraction);
            }
            HudLayout automatic = HudLayout.resolve(card.width(), card.height(), viewportWidth, guiHeight,
                    0, defaultTop, null, HudStyle.DEFAULT);
            assertEquals(defaultTop, automatic.y(), "unsaved card ignored the hotbar anchor at guiHeight=" + guiHeight);
        }
    }

    @Test
    void theAnchorNeverGoesAboveTheTopEdge() {
        assertEquals(0, HudDesign.aboveHotbarTop(100, 400));
        // No spare room left at all: the top pins to the screen edge instead of going negative.
        assertEquals(0, HudDesign.aboveHotbarTop(HudDesign.ABOVE_HOTBAR_INSET, HudDesign.HUD_MARGIN));
        assertTrue(HudDesign.aboveHotbarTop(240, 74) >= 0);
    }
}
