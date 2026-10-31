package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
