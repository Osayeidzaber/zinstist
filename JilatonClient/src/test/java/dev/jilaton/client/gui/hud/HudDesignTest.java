package dev.jilaton.client.gui.hud;

import dev.jilaton.client.config.HudStyle;
import dev.jilaton.client.gui.HudLayout;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudDesignTest {
    private static final int[][] WINDOWS = {
            {100, 80}, {120, 90}, {320, 240}, {427, 240}, {480, 270}, {640, 360}, {800, 600},
            {854, 480}, {960, 540}, {1280, 720}, {1920, 1080}, {2560, 1440}, {3440, 1440},
            {5120, 1440}, {480, 1080}, {1080, 1920}
    };

    @Test void detailedCardKeepsEveryPartInsideThePanel() {
        for (int[] window : WINDOWS) {
            HudDesign.TargetCard card = HudDesign.targetCard(window[0]);
            String at = window[0] + "x" + window[1] + " -> " + card;
            assertTrue(card.width() >= HudDesign.TARGET_MIN_WIDTH, "card narrower than minimum at " + at);
            assertTrue(card.width() <= HudDesign.TARGET_MAX_WIDTH, "card wider than maximum at " + at);
            assertTrue(card.headX() >= 0 && card.headY() >= 0, "head starts outside the card at " + at);
            assertTrue(card.headX() + card.headSize() <= card.contentX(), "head overlaps the text column at " + at);
            assertTrue(card.contentX() + card.contentWidth() <= card.width() - card.padding(),
                    "content column runs past the panel at " + at);
            assertTrue(card.nameY() < card.pillY(), "name and held item pill overlap at " + at);
            assertTrue(card.pillY() + card.pillHeight() <= card.healthY(), "pill overlaps health at " + at);
            assertTrue(card.healthY() < card.barY(), "health and bar overlap at " + at);
            assertTrue(card.barY() + card.barHeight() <= card.ruleY(), "bar overlaps the rule at " + at);
            assertTrue(card.ruleY() < card.chipsY(), "rule overlaps the chips at " + at);
            assertTrue(card.chipsY() + card.chipHeight() <= card.height() - card.padding(),
                    "chips run past the panel at " + at);
            assertTrue(card.chipWidth() > 0, "empty chip at " + at);
            assertTrue(card.chipX(2) + card.chipWidth() <= card.contentX() + card.contentWidth(),
                    "last chip runs past the content column at " + at);
        }
    }

    @Test void cardStillFitsEveryWindowAfterPlacementClamping() {
        for (int[] window : WINDOWS) {
            HudDesign.TargetCard card = HudDesign.targetCard(window[0]);
            HudLayout layout = HudLayout.resolve(card.width(), card.height(), window[0], window[1],
                    0, 0, null, HudStyle.DEFAULT);
            String at = window[0] + "x" + window[1] + " -> " + layout;
            assertTrue(layout.x() >= 0, "card starts off the left edge at " + at);
            assertTrue(layout.y() >= 0, "card starts above the top edge at " + at);
            assertTrue(layout.x() + layout.width() <= window[0], "card runs past the right edge at " + at);
            assertTrue(layout.y() + layout.height() <= window[1], "card runs past the bottom edge at " + at);
        }
    }

    @Test void segmentsTileTheBarAndFillSumsToTheHealthRatio() {
        HudDesign.TargetCard card = HudDesign.targetCard(1280);
        HudDesign.Bar bar = HudDesign.healthBar(card);
        assertEquals(HudDesign.SEGMENTS, bar.segments());
        assertTrue(bar.x() >= card.contentX());
        assertTrue(bar.x() + bar.width() <= card.width() - card.padding());
        int previousRight = bar.x();
        for (int index = 0; index < bar.segments(); index++) {
            int left = bar.segmentLeft(index);
            int right = bar.segmentRight(index);
            assertTrue(left >= previousRight, "segments overlap at index " + index);
            assertTrue(right > left, "empty segment at index " + index);
            assertTrue(right <= bar.x() + bar.width(), "segment leaves the bar at index " + index);
            previousRight = right + bar.gap();
        }
        double sum = 0;
        for (int index = 0; index < bar.segments(); index++) {
            double fill = HudDesign.segmentFill(index, 0.37);
            sum += fill;
            if (index > 0) {
                assertTrue(HudDesign.segmentFill(index - 1, 0.37) >= fill,
                        "segments fill out of order at index " + index);
            }
        }
        assertEquals(0.37 * bar.segments(), sum, 1e-9);
        assertEquals(0, HudDesign.segmentFill(0, 0), 1e-9);
        assertEquals(1, HudDesign.segmentFill(bar.segments() - 1, 1), 1e-9);
    }

    @Test void healthGradientRunsFromVioletToMagenta() {
        assertEquals(HudDesign.VIOLET, HudDesign.segmentColor(0, HudDesign.SEGMENTS));
        assertEquals(HudDesign.MAGENTA, HudDesign.segmentColor(HudDesign.SEGMENTS - 1, HudDesign.SEGMENTS));
        int middle = HudDesign.segmentColor(HudDesign.SEGMENTS / 2, HudDesign.SEGMENTS);
        assertTrue(((middle >> 16) & 0xFF) >= ((HudDesign.VIOLET >> 16) & 0xFF));
        assertTrue((middle & 0xFF) <= (HudDesign.VIOLET & 0xFF));
    }

    @Test void presetsOnlyOwnTheFullWidgets() {
        assertEquals(Set.of("keystrokes", "performance", "coordinates", "armor", "effects",
                        "keybinds", "server", "above_head", "held_item", "staff", "radar"),
                HudPreset.FULL.extraWidgets());
        assertTrue(HudPreset.MINIMAL.extraWidgets().isEmpty());
        for (String widget : Set.of("keystrokes", "performance", "coordinates", "armor", "effects",
                "keybinds", "server", "above_head", "held_item", "staff", "radar")) {
            assertFalse(HudPreset.MINIMAL.enabled(widget), widget + " should be off in Minimal");
            assertTrue(HudPreset.FULL.enabled(widget), widget + " should be on in Full");
        }
        assertEquals(HudPreset.MINIMAL, HudPreset.from("Minimal"));
        assertEquals(HudPreset.FULL, HudPreset.from("Full"));
        assertEquals(HudPreset.MINIMAL, HudPreset.from("not a preset"));
    }

    @Test void fullWidgetPanelsKeepReadableSizes() {
        for (int rows = 0; rows <= HudDesign.KEYBIND_MAX_ROWS + 3; rows++) {
            int capped = Math.min(rows, HudDesign.KEYBIND_MAX_ROWS);
            assertEquals(HudDesign.KEYBIND_TITLE + capped * HudDesign.KEYBIND_ROW + 2,
                    HudDesign.keybindHeight(rows));
        }
        HudDesign.TargetCard card = HudDesign.targetCard(800);
        assertTrue(HudDesign.chipTextWidth(card) > 20);
        assertTrue(HudDesign.SERVER_HEIGHT > HudDesign.SERVER_ROW * 4);
        assertTrue(HudDesign.HELD_ITEM_WIDTH > HudDesign.HELD_ITEM_SCALE * 16);
        assertTrue(HudDesign.HELD_ITEM_WIDTH - HudDesign.HELD_ITEM_LABEL_X
                - HudDesign.HELD_ITEM_PADDING >= 72, "held item label column cannot fit a two-word name");
        assertTrue(HudDesign.ABOVE_HEAD_HEIGHT > 16);
        assertTrue(HudDesign.STAFF_WIDTH > HudDesign.STAFF_TITLE * 4);
        assertEquals(HudDesign.RADAR_DEFAULT_SIZE, HudDesign.radarSize(HudDesign.RADAR_DEFAULT_SIZE));
        assertTrue(HudDesign.radarRadius(HudDesign.RADAR_DEFAULT_SIZE) > 12);
    }

    @Test void staffAndRadarPanelsFitCommonWindows() {
        for (int rows = 0; rows <= HudDesign.STAFF_MAX_ROWS + 3; rows++) {
            int capped = Math.max(0, Math.min(HudDesign.STAFF_MAX_ROWS, rows));
            assertEquals(HudDesign.STAFF_TITLE + capped * HudDesign.STAFF_ROW + HudDesign.STAFF_PADDING,
                    HudDesign.staffHeight(rows));
        }
        assertEquals(HudDesign.RADAR_MIN_SIZE, HudDesign.radarSize(10));
        assertEquals(HudDesign.RADAR_MAX_SIZE, HudDesign.radarSize(500));
        assertEquals(HudDesign.RADAR_DEFAULT_SIZE, HudDesign.radarSize(Double.NaN));

        for (int[] window : new int[][]{{1280, 720}, {1920, 1080}}) {
            String at = window[0] + "x" + window[1];
            int teamRows = 5;
            int staffHeight = HudDesign.staffHeight(teamRows);
            assertTrue(HudDesign.staffRowY(teamRows - 1) + 8 <= staffHeight - HudDesign.STAFF_PADDING,
                    "last staff row runs past the panel at " + at);
            HudLayout staff = HudLayout.resolve(HudDesign.STAFF_WIDTH, staffHeight, window[0], window[1],
                    8, window[1] - 210, null, HudStyle.DEFAULT);
            assertTrue(staff.x() >= 0 && staff.y() >= 0, "staff panel starts off-screen at " + at);
            assertTrue(staff.x() + staff.width() <= window[0], "staff panel runs past the right edge at " + at);
            assertTrue(staff.y() + staff.height() <= window[1], "staff panel runs past the bottom edge at " + at);

            for (int requested = HudDesign.RADAR_MIN_SIZE;
                 requested <= HudDesign.RADAR_MAX_SIZE; requested += 4) {
                int size = HudDesign.radarSize(requested);
                int radius = HudDesign.radarRadius(size);
                int panelHeight = HudDesign.radarPanelHeight(size);
                assertEquals(size + HudDesign.RADAR_FOOTER, panelHeight, "range footer at " + at);
                assertEquals(size / 2 - HudDesign.RADAR_RING_INSET, radius, "ring inset at " + at);
                assertTrue(radius + 1 < size / 2, "blip footprint leaves the radar at " + at);
                assertTrue(size / 2 + radius < size, "ring stays above the footer hairline at " + at);
                HudLayout radar = HudLayout.resolve(size, panelHeight, window[0], window[1],
                        window[0] - size - 8, window[1] - 290, null, HudStyle.DEFAULT);
                assertTrue(radar.x() >= 0 && radar.y() >= 0, "radar starts off-screen at " + at);
                assertTrue(radar.x() + radar.width() <= window[0], "radar runs past the right edge at " + at);
                assertTrue(radar.y() + radar.height() <= window[1], "radar runs past the bottom edge at " + at);
            }
        }
    }
}
