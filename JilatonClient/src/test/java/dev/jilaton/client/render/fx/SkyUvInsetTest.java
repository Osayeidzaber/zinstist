package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SkyUvInsetTest {
    @Test
    void theInsetIsHalfATexelOfTheActualImage() {
        assertArrayEquals(new float[]{0.5F / 2048F, 0.5F / 1365F}, FxFrame.halfTexel(2048, 1365), 0);
        assertArrayEquals(new float[]{0.5F / 1024F, 0.5F / 683F}, FxFrame.halfTexel(1024, 683), 0);
        // Unknown sizes keep the old conservative fallback rather than dividing by zero.
        assertArrayEquals(new float[]{0.5F / 1024F, 0.5F / 683F}, FxFrame.halfTexel(0, 0), 0);
    }

    @Test
    void eachCellEdgeMovesInwardByExactlyTheTexelInset() {
        float[] cell = {1 / 3F, 0.5F, 0, 0.5F, 0, 1, 1 / 3F, 1};
        float u = 0.5F / 2048F, v = 0.5F / 1365F;
        float[] inset = FxFrame.insetUv(cell, u, v);
        assertEquals(1 / 3F - u, inset[0], 1e-7);
        assertEquals(0.5F + v, inset[1], 1e-7);
        assertEquals(u, inset[2], 1e-7);
        assertEquals(1 - v, inset[5], 1e-7);
    }
}
