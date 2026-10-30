package dev.jilaton.client.module.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Pure 20-tick click-array strategies adapted from LiquidBounce's click-pattern family. */
public final class ClickPatternDistribution {
    public static final List<String> OPTIONS = List.of(
            "Steady", "Normal distribution", "Stabilized", "Efficient", "Spamming",
            "Double click", "Drag", "Butterfly");

    private ClickPatternDistribution() {
    }

    public static void fill(int[] clicks, int requestedClicks, String pattern, Random random) {
        if (clicks == null || clicks.length == 0 || random == null) {
            throw new IllegalArgumentException("click array and random source are required");
        }
        Arrays.fill(clicks, 0);
        int boundedClicks = Math.max(0, Math.min(requestedClicks, clicks.length));
        switch (pattern == null ? "Stabilized" : pattern) {
            case "Efficient" -> fillEfficient(clicks, boundedClicks);
            case "Spamming" -> fillSpamming(clicks, boundedClicks, random);
            case "Double click" -> fillDoubleClick(clicks, boundedClicks, random);
            case "Drag" -> fillDrag(clicks, boundedClicks, random);
            case "Butterfly" -> fillButterfly(clicks, boundedClicks, random);
            default -> fillStabilized(clicks, boundedClicks);
        }
    }

    private static void fillStabilized(int[] clicks, int count) {
        if (count == 0) {
            return;
        }
        int stride = clicks.length / count;
        int remainder = clicks.length % count;
        int position = 0;
        for (int i = 0; i < count; i++) {
            clicks[position % clicks.length]++;
            position += Math.max(stride, 1);
            if (remainder > 0) {
                position++;
                remainder--;
            }
        }
    }

    private static void fillEfficient(int[] clicks, int count) {
        if (count < 10) {
            fillStabilized(clicks, count);
            return;
        }
        for (int i = 0; i < count; i++) {
            clicks[(i * 2) % clicks.length]++;
        }
    }

    private static void fillSpamming(int[] clicks, int count, Random random) {
        for (int i = 0; i < count; i++) {
            clicks[random.nextInt(clicks.length)]++;
        }
    }

    private static void fillDoubleClick(int[] clicks, int count, Random random) {
        for (int i = 0; i < count; i++) {
            clicks[random.nextInt(clicks.length)] += 2;
        }
    }

    private static void fillDrag(int[] clicks, int count, Random random) {
        int activeTicks = random.nextInt(17, 19);
        for (int scheduled = 0; scheduled < count; scheduled++) {
            int slowestTick = 0;
            for (int tick = 1; tick < activeTicks; tick++) {
                if (clicks[tick] < clicks[slowestTick]) {
                    slowestTick = tick;
                }
            }
            clicks[slowestTick]++;
        }
    }

    private static void fillButterfly(int[] clicks, int count, Random random) {
        int scheduled = 0;
        int[] emptyTicks = new int[clicks.length];
        while (scheduled < count) {
            int emptyCount = 0;
            for (int tick = 0; tick < clicks.length; tick++) {
                if (clicks[tick] == 0) {
                    emptyTicks[emptyCount++] = tick;
                }
            }
            if (emptyCount > 0) {
                clicks[emptyTicks[random.nextInt(emptyCount)]] = random.nextInt(1, 3);
            } else {
                clicks[random.nextInt(clicks.length)]++;
            }
            scheduled = sum(clicks);
        }
    }

    private static int sum(int[] clicks) {
        int total = 0;
        for (int click : clicks) {
            total += click;
        }
        return total;
    }
}
