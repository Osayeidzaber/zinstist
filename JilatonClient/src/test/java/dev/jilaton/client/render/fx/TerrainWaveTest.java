package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainWaveTest {
    @Test
    void radiusGrowsMonotonicallyAndStopsAtTheSetting() {
        double previous = -1;
        for (int i = 0; i <= 20; i++) {
            double r = TerrainWave.radius(i / 20.0, 44);
            assertTrue(r >= previous);
            previous = r;
        }
        assertEquals(44, TerrainWave.radius(1, 44), 1e-9);
        assertEquals(0, TerrainWave.radius(0, 44), 1e-9);
    }

    @Test
    void samplesAreCappedForBigWaves() {
        assertEquals(24, TerrainWave.samples(0.4));
        assertEquals(TerrainWave.MAX_SAMPLES, TerrainWave.samples(64));
        assertTrue(TerrainWave.samples(44) <= TerrainWave.MAX_SAMPLES);
    }

    @Test
    void behindCameraIsCulled() {
        assertTrue(TerrainWave.inFront(0, 1, 0, 1, 0.15));
        assertFalse(TerrainWave.inFront(0, -1, 0, 1, 0.15));
        assertTrue(TerrainWave.inFront(0, -0.1, 0, 1, 0.15), "a shallow behind sample stays inside the margin");
    }

    @Test
    void blockTopIsOneBlockWideAndLifted() {
        double[] top = TerrainWave.blockTop(10.7, -3.2, 64.0);
        assertEquals(10, top[0], 1e-9);
        assertEquals(11, top[3], 1e-9);
        assertEquals(-4, top[2], 1e-9);
        assertEquals(-3, top[8], 1e-9);
        assertEquals(64.03, top[1], 1e-9);
        assertEquals(64.03, top[10], 1e-9);
    }

    @Test
    void progressClamps() {
        assertEquals(0, TerrainWave.progress(-1, 1.6), 1e-9);
        assertEquals(1, TerrainWave.progress(5, 1.6), 1e-9);
        assertEquals(0.5, TerrainWave.progress(0.8, 1.6), 1e-9);
        assertEquals(1, TerrainWave.progress(0.5, 0), 1e-9);
    }

    @Test
    void frontGlowPeaksOnTheFrontAndFadesBehindIt() {
        assertEquals(1, TerrainWave.frontGlow(20, 20, 3), 1e-9);
        assertTrue(TerrainWave.frontGlow(19, 20, 3) > TerrainWave.frontGlow(18, 20, 3), "afterglow decays inward");
        assertEquals(0, TerrainWave.frontGlow(17, 20, 3), 1e-9);
        assertEquals(0, TerrainWave.frontGlow(21.5, 20, 3), 1e-9);
        assertTrue(TerrainWave.frontGlow(20.6, 20, 3) < TerrainWave.frontGlow(19.4, 20, 3), "sharp leading edge");
    }

    @Test
    void impactLifeKeepsTheTerrainWaveAliveForItsWholeSweep() {
        // The wave always expands for MAX_LIFE_SECONDS, so a hit must never be culled before that.
        assertEquals(TerrainWave.MAX_LIFE_SECONDS, TerrainWave.impactLife(1.0), 1e-9);
        assertEquals(TerrainWave.MAX_LIFE_SECONDS, TerrainWave.impactLife(0.3), 1e-9);
        assertEquals(TerrainWave.MAX_LIFE_SECONDS, TerrainWave.impactLife(0), 1e-9);
        assertEquals(TerrainWave.MAX_LIFE_SECONDS, TerrainWave.impactLife(-2), 1e-9);
        assertEquals(2.4, TerrainWave.impactLife(2.0), 1e-9, "a long configured lifetime still wins");
        assertTrue(TerrainWave.impactLife(3.0) > TerrainWave.impactLife(2.0));
    }

    @Test
    void frontBandSelectionKeepsTheLeadingEdgeAndTheAfterglowOnly() {
        // 20-block front, 3-block afterglow, camera looking toward +z.
        assertTrue(TerrainWave.selectsCell(0, 20, 20, 20, 3, 0, 1));
        assertTrue(TerrainWave.selectsCell(0, 21.1, 21.1, 20, 3, 0, 1), "sharp leading edge is part of the band");
        assertFalse(TerrainWave.selectsCell(0, 21.3, 21.3, 20, 3, 0, 1), "nothing past the leading edge");
        assertTrue(TerrainWave.selectsCell(0, 17.1, 17.1, 20, 3, 0, 1), "afterglow trails behind the front");
        assertFalse(TerrainWave.selectsCell(0, 16.9, 16.9, 20, 3, 0, 1), "the afterglow has a hard inner edge");
        assertFalse(TerrainWave.selectsCell(10, -18, 20.59, 20, 3, 0, 1), "deep behind the camera is culled");
        assertTrue(TerrainWave.selectsCell(19, -1.5, 19.06, 20, 3, 0, 1), "a shallow-behind cell stays in the margin");
        assertTrue(TerrainWave.selectsCell(0.4, 0, 0.4, 20, 3, 0, 1), "the impact core is never culled");
        assertTrue(TerrainWave.selectsCell(0.4, 0, 0.4, 20, 3, 0, -1), "the impact core ignores the view direction");
        assertFalse(TerrainWave.selectsCell(0, 22, 22, 0.3, 3, 0, 1), "an early wave has no band yet");
    }
}
