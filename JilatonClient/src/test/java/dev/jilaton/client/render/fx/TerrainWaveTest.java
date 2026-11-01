package dev.jilaton.client.render.fx;

import dev.jilaton.client.render.EffectBatch;
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
        assertTrue(TerrainWave.selectsCell(0.4, 0, 0.4, 1, 3, 0, -1), "the impact core ignores the view direction");
        assertFalse(TerrainWave.selectsCell(0.4, 0, 0.4, 20, 3, 0, 1), "the core is still outside a wide wave's band");
        assertFalse(TerrainWave.selectsCell(0, 22, 22, 0.3, 3, 0, 1), "an early wave has no band yet");
    }

    @Test
    void alphaStaysHighThroughTheMiddleOfTheSweep() {
        // Scene 28 captures the wave around half life, when the front is already 17 of 21 blocks out. The old
        // (1-u)^1.4 curve had lost 62% of its opacity by then and the sweep read as a faint flat band; a
        // shockwave has to stay legible until it reaches the radius the user configured.
        assertEquals(1, TerrainWave.alpha(0), 1e-9);
        assertEquals(0, TerrainWave.alpha(1), 1e-9);
        assertTrue(TerrainWave.alpha(0.5) >= 0.6, "half life keeps most of the front: " + TerrainWave.alpha(0.5));
        double previous = 1;
        for (int i = 1; i <= 20; i++) {
            double next = TerrainWave.alpha(i / 20.0);
            assertTrue(next <= previous + 1e-9, "the front must fade monotonically");
            previous = next;
        }
    }

    @Test
    void oneImpactPaintsTheGroundBandOfItsFront() {
        // Camera 3.3 blocks above the plaza looking along +z, exactly the harness geometry; impact at the origin.
        EffectBatch batch = new EffectBatch();
        double radius = TerrainWave.radius(0.5, 21);
        double alpha = 0.9 * TerrainWave.alpha(0.5);
        int emitted = TerrainWave.emitBand(batch, (x, z) -> 140.0, 0, 0, 0, 143.3, 0,
                radius, 3, alpha, 0xF2F2F2, 0, 1, TerrainWave.MAX_BLOCKS_PER_FRAME, new double[12]);
        assertTrue(emitted > 60, "one impact must paint the whole visible front, painted " + emitted);
        assertEquals(emitted, batch.quads().size(), "one block top per sampled cell");
        int peak = 0;
        for (EffectBatch.Quad quad : batch.quads()) {
            assertFalse(quad.walls(), "the wave lies on the terrain and must stay depth-tested");
            assertEquals(140.03 - 143.3, quad.ay(), 1e-9);
            assertEquals(140.03 - 143.3, quad.cy(), 1e-9);
            double distance = Math.hypot(quad.ax(), quad.az());
            assertTrue(distance >= radius - 3 - 0.71 && distance <= radius + TerrainWave.LEADING_EDGE + 0.71,
                    "block top outside the front band: " + distance);
            peak = Math.max(peak, Math.max(quad.colorA() >>> 24, quad.colorC() >>> 24));
        }
        // At half life the old curve painted 55/255 here, which is why the sweep was not readable.
        assertTrue(peak >= 0.35 * 255, "half-life block tops stay clearly visible, peak " + peak);
        assertTrue(batch.segments().size() >= emitted, "the leading edge is outlined");
    }

    @Test
    void bandFollowsUnevenGroundAndStopsAtItsBudget() {
        EffectBatch batch = new EffectBatch();
        double radius = TerrainWave.radius(0.5, 21);
        TerrainWave.emitBand(batch, (x, z) -> x >= 0 ? 141.0 : 140.0, 0, 0, 0, 143.3, 0,
                radius, 3, 0.9 * TerrainWave.alpha(0.5), 0xF2F2F2, 0, 1,
                TerrainWave.MAX_BLOCKS_PER_FRAME, new double[12]);
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (EffectBatch.Quad quad : batch.quads()) {
            minY = Math.min(minY, Math.min(quad.ay(), quad.cy()));
            maxY = Math.max(maxY, Math.max(quad.ay(), quad.cy()));
        }
        assertEquals(1.0, maxY - minY, 1e-9, "a one-block step must lift the band by exactly one block");

        EffectBatch limited = new EffectBatch();
        int emitted = TerrainWave.emitBand(limited, (x, z) -> 140.0, 0, 0, 0, 143.3, 0,
                radius, 3, 0.9 * TerrainWave.alpha(0.5), 0xF2F2F2, 0, 1, 7, new double[12]);
        assertEquals(7, emitted);
        assertEquals(7, limited.quads().size());

        EffectBatch unknown = new EffectBatch();
        assertEquals(0, TerrainWave.emitBand(unknown, (x, z) -> Double.NaN, 0, 0, 0, 143.3, 0,
                radius, 3, 0.9 * TerrainWave.alpha(0.5), 0xF2F2F2, 0, 1, 1400, new double[12]),
                "a column without ground emits nothing");
        assertTrue(unknown.isEmpty());
    }

    @Test
    void anOverBudgetWaveIsThinnedAcrossItsWholeArcNotCut() {
        // Three live waves at the maximum settings (radius 64, thickness 8) share 1400 cells; each needs far more.
        double alpha = 0.9 * TerrainWave.alpha(0.9);
        EffectBatch full = new EffectBatch();
        int needed = TerrainWave.emitBand(full, (x, z) -> 140.0, 0, 0, 0, 143.3, 0,
                64, 8, alpha, 0xF2F2F2, 0, 1, Integer.MAX_VALUE, new double[12]);
        int budget = TerrainWave.MAX_BLOCKS_PER_FRAME / 3;
        assertTrue(needed > budget, "the case must actually be over budget, needed " + needed);

        EffectBatch thinned = new EffectBatch();
        int emitted = TerrainWave.emitBand(thinned, (x, z) -> 140.0, 0, 0, 0, 143.3, 0,
                64, 8, alpha, 0xF2F2F2, 0, 1, budget, new double[12]);
        assertTrue(emitted <= budget && emitted >= budget / 2, "emitted " + emitted + " of " + budget);
        // The visible arc runs from -x through +z to +x; a truncated scan starves the +x end. Every sector must
        // keep at least half of its fair (budget / needed) share of the full-density cells.
        int[] all = sectors(full), kept = sectors(thinned);
        double share = (double) budget / needed;
        for (int s = 0; s < all.length; s++) {
            assertTrue(kept[s] >= 0.5 * share * all[s], "sector " + s + " starved: kept "
                    + java.util.Arrays.toString(kept) + " of " + java.util.Arrays.toString(all));
        }
    }

    /** Block tops per 30-degree sector of the half-plane in front of a camera looking along +z. */
    private static int[] sectors(EffectBatch batch) {
        int[] counts = new int[6];
        for (EffectBatch.Quad quad : batch.quads()) {
            double angle = Math.atan2(quad.az(), quad.ax());
            if (angle < 0) continue;
            counts[Math.min(5, (int) (angle / (Math.PI / 6)))]++;
        }
        return counts;
    }
}
