package dev.jilaton.client.gui;

import dev.jilaton.client.gui.JilatonLoadingScene.Layout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadingBrandLayoutTest {
    private static final int[][] WINDOWS = {
            {100, 80}, {120, 90}, {320, 240}, {427, 240}, {480, 270}, {640, 360}, {800, 600},
            {854, 480}, {960, 540}, {1280, 720}, {1920, 1080}, {2560, 1440}, {3440, 1440},
            {5120, 1440}, {480, 1080}, {1080, 1920}
    };

    @Test void wordmarkAndBarStayInsideEveryWindow() {
        for (int[] window : WINDOWS) {
            int width = window[0];
            int height = window[1];
            Layout layout = JilatonLoadingScene.layout(width, height);
            String at = width + "x" + height + " -> " + layout;
            assertTrue(layout.logoX() >= 0, "logo starts off the left edge at " + at);
            assertTrue(layout.logoX() + layout.logoWidth() <= width, "logo runs past the right edge at " + at);
            assertTrue(layout.barX() >= 0, "bar starts off the left edge at " + at);
            assertTrue(layout.barX() + layout.barWidth() <= width, "bar runs past the right edge at " + at);
            assertTrue(layout.barY() >= 0, "bar starts above the top edge at " + at);
            assertTrue(layout.barY() + layout.barHeight() <= height, "bar runs past the bottom edge at " + at);
            assertTrue(layout.barY() >= layout.textBottom() - 1, "bar overlaps the wordmark at " + at);
        }
    }

    @Test void visibleTextStaysVerticallyCentredOnNormalWindows() {
        for (int[] window : WINDOWS) {
            int width = window[0];
            int height = window[1];
            Layout layout = JilatonLoadingScene.layout(width, height);
            if (height < 200) continue;
            String at = width + "x" + height + " -> " + layout;
            assertTrue(layout.textTop() >= 0, "wordmark starts above the top edge at " + at);
            assertTrue(layout.textBottom() <= height, "wordmark runs past the bottom edge at " + at);
            assertTrue(layout.textTop() < layout.textBottom(), "empty wordmark at " + at);
        }
    }

    @Test void wordmarkKeepsTheFourToOneShapeOfItsTexture() {
        for (int[] window : WINDOWS) {
            Layout layout = JilatonLoadingScene.layout(window[0], window[1]);
            assertEquals(layout.logoHeight() * 4, layout.logoWidth(), "stretched wordmark at " + window[0] + "x" + window[1]);
        }
        Layout wide = JilatonLoadingScene.layout(1920, 1080);
        assertTrue(wide.logoWidth() <= 1920 * 3 / 4, "logo should leave breathing room on both sides");
        assertTrue(wide.logoX() > 1920 / 8, "logo should not hug the left edge");
    }

    @Test void shineIsABandThatSweepsAcrossTheWholeWordmark() {
        double peak = JilatonLoadingScene.shine(0.0, 0.0);
        assertEquals(0.0, peak, 1e-9, "the band starts left of the text");

        boolean leftSeen = false;
        boolean rightSeen = false;
        for (int step = 0; step <= 260; step++) {
            double time = step * 0.01;
            double atLeft = JilatonLoadingScene.shine(time, 0.05);
            double atRight = JilatonLoadingScene.shine(time, 0.95);
            leftSeen |= atLeft > 0.5;
            rightSeen |= atRight > 0.5;
            for (int strip = 0; strip < 32; strip++) {
                double glow = JilatonLoadingScene.shine(time, (strip + 0.5) / 32.0);
                assertTrue(glow >= 0.0 && glow <= 1.0, "glow out of range: " + glow);
            }
        }
        assertTrue(leftSeen, "the band never reaches the left of the wordmark");
        assertTrue(rightSeen, "the band never reaches the right of the wordmark");

        double centre = JilatonLoadingScene.shine(0.5 * 2.6, 0.5);
        assertTrue(centre > 0.9, "the band is at full strength in the middle of its cycle");
        assertTrue(JilatonLoadingScene.shine(0.5 * 2.6, 1.0) < 0.01, "the band is narrow");
    }
}
