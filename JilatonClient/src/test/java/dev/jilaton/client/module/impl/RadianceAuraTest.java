package dev.jilaton.client.module.impl;

import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.VisualQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure geometry of the two feathered camera-facing layers that replaced the old six-face shell. */
class RadianceAuraTest {
    @Test
    void featherGrowsWithTheGlowThicknessAndNeverGoesNegative() {
        assertEquals(0.30, RadianceModule.auraFeather(0.3), 1e-9);
        assertEquals(0.15, RadianceModule.auraFeather(0), 1e-9);
        assertEquals(0.15, RadianceModule.auraFeather(-4), 1e-9, "a bad setting must not invert the aura");
        assertTrue(RadianceModule.auraFeather(0.6) > RadianceModule.auraFeather(0.1));
        assertEquals(0.45, RadianceModule.auraFeather(0.6), 1e-9);
    }

    @Test
    void auraLayersHugTheBodyAndStayOrdered() {
        double[] out = new double[4];
        RadianceModule.auraExtents(0.6, 0.6, 1.8, 0.3, out);
        // Player: 0.6 wide and 1.8 tall. Inner layer = tight bloom, outer layer = wide halo.
        assertEquals(0.55, out[0], 1e-9);
        assertEquals(1.15, out[1], 1e-9);
        assertEquals(0.70, out[2], 1e-9);
        assertEquals(1.30, out[3], 1e-9);
        assertTrue(out[0] < out[2], "the halo is wider than the bloom");
        assertTrue(out[1] < out[3], "the halo is taller than the bloom");
    }

    @Test
    void wideBodiesUseTheLargerFootprintAndTinyItemsStayPositive() {
        double[] out = new double[4];
        RadianceModule.auraExtents(2.0, 1.0, 1.4, 0.3, out);
        assertEquals(1.25, out[0], 1e-9, "the larger horizontal extent drives the glow width");
        RadianceModule.auraExtents(0.25, 0.25, 0.25, 0.1, out);
        for (double value : out) assertTrue(value > 0 && Double.isFinite(value));
        RadianceModule.auraExtents(0, 0, 0, 0.6, out);
        assertTrue(out[0] > 0 && out[1] > 0, "even a point-sized box keeps a feather margin");
    }

    @Test
    void auraExtentsRequireAResultBuffer() {
        assertThrows(IllegalArgumentException.class, () -> RadianceModule.auraExtents(1, 1, 1, 0.3, null));
        assertThrows(IllegalArgumentException.class, () -> RadianceModule.auraExtents(1, 1, 1, 0.3, new double[3]));
    }

    @Test
    void auraPutsItsBrightnessWhereTheBodyCannotHideIt() {
        // The aura is drawn after the entities into the depth-tested pass, so everything inside the silhouette
        // is depth-rejected. Scene 23 showed exactly that: a fringe clipped to the arms and no glow outside.
        EffectBatch batch = new EffectBatch();
        FxFrame frame = new FxFrame(null, batch, 0, 0, 0,
                new float[]{1, 0, 0}, new float[]{0, 1, 0}, new float[]{0, 0, 1},
                0, 0.016, 0, 0xFFFFFF, VisualQuality.CINEMATIC, false, 1, false);
        double[] aura = new double[4];
        RadianceModule.emitAura(frame, 0, 1, 0, 0.6, 0.6, 1.8, 0.3, 0xA98CFF, 0.28, aura);
        assertFalse(batch.quads().isEmpty(), "the aura must draw something");

        int peak = 0, outside = 0;
        double reach = 0;
        for (EffectBatch.Quad q : batch.quads()) {
            double[][] corners = {{q.ax(), q.ay()}, {q.bx(), q.by()}, {q.cx(), q.cy()}, {q.dx(), q.dy()}};
            int[] colors = {q.colorA(), q.colorB(), q.colorC(), q.colorD()};
            assertFalse(q.walls(), "the aura must stay depth-tested");
            for (int i = 0; i < 4; i++) {
                int a = colors[i] >>> 24;
                peak = Math.max(peak, a);
                // Billboard plane: right = +x, up = +y, body centre 1 block above the camera.
                double dx = Math.abs(corners[i][0]), dy = Math.abs(corners[i][1] - 1);
                if (dx > 0.31 || dy > 0.91) {
                    outside = Math.max(outside, a);
                    reach = Math.max(reach, Math.hypot(dx / 0.3, dy / 0.9));
                }
            }
        }
        assertTrue(peak >= 0.10 * 255, "the aura is drawn at the requested strength, peak " + peak);
        assertTrue(outside >= 0.45 * peak,
                "most of the aura's alpha must survive outside the silhouette, outside " + outside + " peak " + peak);
        assertTrue(reach >= 1.5, "the glow extends past the body, reach " + reach);
    }
}
