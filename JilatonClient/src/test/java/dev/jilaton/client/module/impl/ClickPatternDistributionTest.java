package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickPatternDistributionTest {
    @Test
    void stabilizedPlacesTheRequestedClicksEvenly() {
        int[] clicks = fill("Stabilized", 7, 4);

        assertEquals(7, sum(clicks));
        assertEquals(7, nonEmptyTicks(clicks));
        assertEquals(1, max(clicks));
    }

    @Test
    void efficientUsesAlternatingTicksForHigherRates() {
        int[] clicks = fill("Efficient", 12, 4);

        assertEquals(12, sum(clicks));
        assertEquals(10, nonEmptyTicks(clicks));
        assertEquals(2, clicks[0]);
        assertEquals(2, clicks[2]);
        assertEquals(0, clicks[1]);
    }

    @Test
    void spammingPreservesTheCountAndIsReproducible() {
        assertArrayEquals(array("Spamming", 8, 28), array("Spamming", 8, 28));
        assertEquals(8, sum(array("Spamming", 8, 28)));
    }

    @Test
    void doubleClickDoublesTheRequestedCount() {
        int[] clicks = fill("Double click", 8, 9);

        assertEquals(16, sum(clicks));
        assertTrue(java.util.Arrays.stream(clicks).allMatch(click -> click % 2 == 0));
    }

    @Test
    void dragFillsTheEarlyPartOfTheTickWindow() {
        int[] clicks = fill("Drag", 8, 4);

        assertEquals(8, sum(clicks));
        assertEquals(0, clicks[18]);
        assertEquals(0, clicks[19]);
    }

    @Test
    void butterflyUsesSinglesAndDoublesWithoutLosingItsCount() {
        int[] clicks = fill("Butterfly", 8, 12);

        assertTrue(sum(clicks) >= 8 && sum(clicks) <= 9);
        assertTrue(max(clicks) <= 2);
    }

    @Test
    void clickScheduleExposesPerTickCountsForBurstPatterns() {
        ClickSchedule schedule = new ClickSchedule(new Random(4));
        int total = 0;

        for (int tick = 0; tick < 20; tick++) {
            total += schedule.advanceClicks(8.0, 8.0, "Double click");
        }

        assertEquals(16, total);
    }

    private static int[] fill(String pattern, int count, long seed) {
        int[] clicks = new int[20];
        ClickPatternDistribution.fill(clicks, count, pattern, new Random(seed));
        return clicks;
    }

    private static int[] array(String pattern, int count, long seed) {
        return fill(pattern, count, seed);
    }

    private static int sum(int[] clicks) {
        return java.util.Arrays.stream(clicks).sum();
    }

    private static int nonEmptyTicks(int[] clicks) {
        return (int) java.util.Arrays.stream(clicks).filter(click -> click > 0).count();
    }

    private static int max(int[] clicks) {
        return java.util.Arrays.stream(clicks).max().orElse(0);
    }
}
