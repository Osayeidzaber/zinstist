package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoToolPolicyTest {
    private static final int U = Integer.MAX_VALUE;

    @Test
    void picksTheFastestTool() {
        float[] speed = {1, 1, 8, 4, 1, 1, 1, 1, 1};
        assertEquals(2, AutoToolPolicy.best(speed, new boolean[9], fill(U), 0, 5, false));
    }

    @Test
    void keepsTheCurrentSlotWhenItIsAlreadyBest() {
        float[] speed = {1, 1, 8, 4, 1, 1, 1, 1, 1};
        assertEquals(-1, AutoToolPolicy.best(speed, new boolean[9], fill(U), 2, 5, false));
    }

    @Test
    void skipsNearlyBrokenTools() {
        float[] speed = {1, 1, 8, 4, 1, 1, 1, 1, 1};
        int[] left = fill(U);
        left[2] = 3;
        assertEquals(3, AutoToolPolicy.best(speed, new boolean[9], left, 0, 5, false));
    }

    @Test
    void prefersTheToolThatGetsDropsForBlocksThatNeedOne() {
        // A gold pickaxe is fast but cannot mine diamond ore; the iron one can.
        float[] speed = {1, 12, 6, 1, 1, 1, 1, 1, 1};
        boolean[] correct = new boolean[9];
        correct[2] = true;
        assertEquals(2, AutoToolPolicy.best(speed, correct, fill(U), 0, 5, true));
    }

    private static int[] fill(int v) {
        int[] a = new int[9];
        java.util.Arrays.fill(a, v);
        return a;
    }

    @Test void anInstantWrongToolNeverOutranksAToolThatPreservesDrops() {
        org.junit.jupiter.api.Assertions.assertTrue(AutoToolPolicy.prefer(.1f, true, Float.POSITIVE_INFINITY, false));
        org.junit.jupiter.api.Assertions.assertFalse(AutoToolPolicy.prefer(Float.POSITIVE_INFINITY, false, .1f, true));
        org.junit.jupiter.api.Assertions.assertFalse(AutoToolPolicy.prefer(Float.NaN, true, .1f, true));
    }

    @Test void customDrillNamesUseTheReferenceNormalization() {
        assertEquals("shard pickaxe", ToolNameText.normalize("§bＳｈａｒｄ §rPíckaxe"));
        assertEquals("breaks 9 blocks at once", ToolNameText.normalize("Breaks 9 blocks at once!"));
        assertEquals("", ToolNameText.normalize(null));
    }
}
