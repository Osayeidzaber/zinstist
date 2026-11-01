package dev.jilaton.client.module.impl;

import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.VisualQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure geometry of the feathered silhouette halo that replaced the body-centred filled ellipse.
 *
 * <p>The old layers peaked at the body centre, which the entity's own depth silhouette always hides, so the
 * only visible part was the rim where the gradient had already faded to zero. These tests pin the fix: the
 * brightest band must sit outside the body silhouette, and the band must still reach inward far enough that
 * no gap appears between the ring and the body.
 */
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
