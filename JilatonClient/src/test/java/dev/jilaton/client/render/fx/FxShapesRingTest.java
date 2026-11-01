package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Geometry of the feathered band {@link FxShapes#softRing} emits: transparent edge, bright middle. */
class FxShapesRingTest {
    @Test
    void bandRadiiPutThePeakBetweenTwoTransparentEdges() {
        double[] radii = FxShapes.bandRadii(0.35, 0.95, 0.30);
        // {inner width, inner height, peak width, peak height, outer width, outer height}
        assertEquals(0.05, radii[0], 1e-9);
        assertEquals(0.65, radii[1], 1e-9);
        assertEquals(0.35, radii[2], 1e-9);
        assertEquals(0.95, radii[3], 1e-9);
        assertEquals(0.65, radii[4], 1e-9);
        assertEquals(1.25, radii[5], 1e-9);
        assertTrue(radii[0] < radii[2] && radii[2] < radii[4], "the peak lies between the inner and outer rim");
        assertTrue(radii[1] < radii[3] && radii[3] < radii[5], "the peak lies between the inner and outer rim");
    }

    @Test
    void aBandWiderThanTheRingCollapsesToTheCentreWithoutGoingNegative() {
        double[] radii = FxShapes.bandRadii(0.10, 0.10, 0.50);
        assertEquals(0, radii[0], 1e-9);
        assertEquals(0, radii[1], 1e-9);
        assertEquals(0.60, radii[4], 1e-9);
        assertEquals(0.60, radii[5], 1e-9);
        for (double value : radii) assertTrue(value >= 0 && Double.isFinite(value));
        double[] degenerate = FxShapes.bandRadii(0.2, 0.2, -1);
        assertEquals(0.2, degenerate[0], 1e-9, "a negative band must not shrink the ring");
        assertEquals(0.2, degenerate[4], 1e-9);
    }

    @Test
    void ringSidesStaySmoothOnEveryQualityTier() {
        for (VisualQuality quality : VisualQuality.values()) {
            assertTrue(FxShapes.ringSides(quality) >= 12, quality + " must stay smooth");
        }
        assertTrue(FxShapes.ringSides(VisualQuality.CINEMATIC) > FxShapes.ringSides(VisualQuality.LOW),
                "higher quality spends more segments on the ring");
    }
}
