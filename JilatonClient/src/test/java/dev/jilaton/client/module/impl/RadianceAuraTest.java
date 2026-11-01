package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure geometry of the feathered silhouette ring that replaced the body-centred filled ellipse.
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
    void ringsAnchorTheBrightBandOnTheSilhouetteOfAPlayer() {
        double[] out = new double[6];
        RadianceModule.auraRings(0.6, 0.6, 1.8, 0.3, out);
        // Player: 0.6 wide and 1.8 tall, feather 0.30. Core ring peaks just outside the body, the halo
        // repeats the same anchor further out and fainter.
        assertEquals(0.35, out[0], 1e-9, "core peak half-width");
        assertEquals(0.95, out[1], 1e-9, "core peak half-height");
        assertEquals(0.30, out[2], 1e-9, "core band half-width");
        assertEquals(0.53, out[3], 1e-9, "halo peak half-width");
        assertEquals(1.13, out[4], 1e-9, "halo peak half-height");
        assertEquals(0.33, out[5], 1e-9, "halo band half-width");
        assertTrue(out[3] > out[0] && out[4] > out[1], "the halo sits outside the core ring");
        assertTrue(out[5] > out[2], "the halo band is wider than the core band");
    }

    @Test
    void theBrightestBandSitsOutsideTheBodyButStillCoversTheSilhouette() {
        double[] out = new double[6];
        RadianceModule.auraRings(0.6, 0.6, 1.8, 0.24, out);
        // Regression for the invisibility defect: a gradient centred on the body put its bright part behind
        // the entity. The ring's peak must be outside the silhouette so depth/occlusion cannot eat it...
        assertTrue(out[0] > 0.6 / 2, "core peak must lie outside the body silhouette");
        assertTrue(out[1] > 1.8 / 2, "core peak must reach past the body top and bottom");
        // ...while its inner edge must stay at or inside the silhouette, so no transparent gap opens up.
        assertTrue(out[0] - out[2] <= 0.6 / 2, "core band must still cover the body edge");
        assertTrue(out[1] - out[2] <= 1.8 / 2, "core band must still cover the body edge vertically");
    }

    @Test
    void wideBodiesUseTheLargerFootprintAndTinyItemsStayPositive() {
        double[] out = new double[6];
        RadianceModule.auraRings(2.0, 1.0, 1.4, 0.3, out);
        assertEquals(1.05, out[0], 1e-9, "the larger horizontal extent drives the glow width");
        assertEquals(0.75, out[1], 1e-9);
        RadianceModule.auraRings(0.25, 0.25, 0.25, 0.1, out);
        for (double value : out) assertTrue(value > 0 && Double.isFinite(value));
        RadianceModule.auraRings(0, 0, 0, 0.6, out);
        assertTrue(out[0] > 0 && out[1] > 0, "even a point-sized box keeps a feather margin");
    }

    @Test
    void auraRingsRequireAResultBuffer() {
        assertThrows(IllegalArgumentException.class, () -> RadianceModule.auraRings(1, 1, 1, 0.3, null));
        assertThrows(IllegalArgumentException.class, () -> RadianceModule.auraRings(1, 1, 1, 0.3, new double[5]));
    }
}
