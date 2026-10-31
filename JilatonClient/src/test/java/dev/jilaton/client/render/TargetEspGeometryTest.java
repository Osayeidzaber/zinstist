package dev.jilaton.client.render;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetEspGeometryTest {
    private static final double EPS = 1.0E-9;
    private static final TargetEspGeometry.Marker MARKER =
            new TargetEspGeometry.Marker(0.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0xFFFFFF, 1.0, 0.0, true);

    private static EffectBatch emit(String style, int mask) {
        EffectBatch batch = new EffectBatch();
        TargetEspGeometry.emit(batch, style, MARKER, mask);
        return batch;
    }

    @Test
    void minimalRingIsAClosedCircleWithClosureAtTheStartAngle() {
        EffectBatch batch = emit(TargetEspGeometry.MINIMAL, 0);
        assertEquals(200, batch.segments().size());
        assertEquals(0, batch.quads().size());

        // The 48 plain ring segments follow the 72 glow-ring segments (2 lines each).
        double plainRadius = 0.92 * 0.78;
        for (int i = 144; i < 192; i++) {
            EffectBatch.Segment segment = batch.segments().get(i);
            assertEquals(0.03, segment.y1(), EPS);
            assertEquals(plainRadius, Math.hypot(segment.x1(), segment.z1()), 1.0E-6);
        }
        EffectBatch.Segment first = batch.segments().get(144);
        EffectBatch.Segment last = batch.segments().get(191);
        assertEquals(first.x1(), last.x2(), 1.0E-6);
        assertEquals(first.z1(), last.z2(), 1.0E-6);
    }

    @Test
    void groundAuraBuildsThreeAnnuliWithIncreasingOuterRadius() {
        EffectBatch batch = emit(TargetEspGeometry.GROUND_AURA, 0);
        assertEquals(448, batch.segments().size());
        assertEquals(168, batch.quads().size());

        double[] expected = {0.62, 0.84, 1.06};
        for (int band = 0; band < 3; band++) {
            EffectBatch.Quad quad = batch.quads().get(band * 56);
            double outer = Math.hypot(quad.cx(), quad.cz());
            double inner = Math.hypot(quad.ax(), quad.az());
            assertEquals(expected[band], outer, 1.0E-6);
            assertEquals(expected[band] * 0.78, inner, 1.0E-6);
        }
    }

    @Test
    void cyberTargetHasOneRotatedScanPlaneAtHalfHeight() {
        EffectBatch batch = emit(TargetEspGeometry.CYBER_TARGET, 0);
        assertEquals(62, batch.segments().size());
        assertEquals(1, batch.quads().size());

        EffectBatch.Quad scan = batch.quads().get(0);
        assertEquals(1.0, scan.ay(), EPS);
        assertEquals(1.0, scan.by(), EPS);
        assertEquals(1.0, scan.cy(), EPS);
        assertEquals(1.0, scan.dy(), EPS);
        double corner = Math.hypot(0.74, 0.74);
        assertEquals(corner, Math.hypot(scan.ax(), scan.az()), 1.0E-6);
        assertEquals(corner, Math.hypot(scan.bx(), scan.bz()), 1.0E-6);
        assertEquals(corner, Math.hypot(scan.cx(), scan.cz()), 1.0E-6);
        assertEquals(corner, Math.hypot(scan.dx(), scan.dz()), 1.0E-6);
    }

    @Test
    void orbitalOrbitsAndHelixStrandsHaveTheirDeclaredShape() {
        EffectBatch orbit = emit(TargetEspGeometry.ORBIT, 0);
        assertEquals(111, orbit.segments().size());
        assertEquals(0, orbit.quads().size());

        EffectBatch helix = emit(TargetEspGeometry.HELIX, 0);
        assertEquals(200, helix.segments().size());
        assertEquals(0, helix.quads().size());
        assertEquals(0.0, helix.segments().get(0).y1(), EPS);
        assertEquals(2.0, helix.segments().get(helix.segments().size() - 1).y2(), 1.0E-6);

        double[] a = TargetEspGeometry.orbitPoint(0, 0, 0, 1.0, Math.toRadians(24), 0.3, 0.0);
        double[] b = TargetEspGeometry.orbitPoint(0, 0, 0, 1.0, Math.toRadians(24), 0.3, Math.PI * 2);
        assertEquals(a[0], b[0], 1.0E-9);
        assertEquals(a[1], b[1], 1.0E-9);
        assertEquals(a[2], b[2], 1.0E-9);
        double[] mid = TargetEspGeometry.orbitPoint(0, 0, 0, 1.0, Math.toRadians(24), 0.3, 1.1);
        assertEquals(1.0, Math.hypot(Math.hypot(mid[0], mid[1]), mid[2]), 1.0E-9);
    }

    @Test
    void cinematicCombinesGroundOrbitHelixAndReticle() {
        EffectBatch batch = emit(TargetEspGeometry.CINEMATIC, 0);
        assertEquals(765, batch.segments().size());
        assertEquals(168, batch.quads().size());
    }

    @Test
    void customMaskSelectsExactlyTheRequestedLayers() {
        EffectBatch accentOnly = emit(TargetEspGeometry.CUSTOM, TargetEspGeometry.COMP_HELIX);
        assertEquals(200, accentOnly.segments().size());
        assertEquals(0, accentOnly.quads().size());

        EffectBatch emptyMask = emit(TargetEspGeometry.CUSTOM, 0);
        EffectBatch halo = emit("Halo", 0);
        assertEquals(halo.segments().size(), emptyMask.segments().size());
        assertEquals(halo.quads().size(), emptyMask.quads().size());
    }

    @Test
    void everyVariantProducesDistinctFiniteGeometry() {
        Set<String> signatures = new HashSet<>();
        for (String style : List.of(TargetEspGeometry.MINIMAL, TargetEspGeometry.ORBIT, TargetEspGeometry.HELIX,
                TargetEspGeometry.GROUND_AURA, TargetEspGeometry.CYBER_TARGET, TargetEspGeometry.CINEMATIC,
                TargetEspGeometry.CUSTOM)) {
            EffectBatch batch = emit(style, TargetEspGeometry.DEFAULT_CUSTOM_MASK);
            assertTrue(!batch.isEmpty(), style + " emitted nothing");
            double length = 0;
            for (EffectBatch.Segment segment : batch.segments()) {
                assertTrue(Double.isFinite(segment.x1()) && Double.isFinite(segment.y1())
                        && Double.isFinite(segment.z1()) && Double.isFinite(segment.x2())
                        && Double.isFinite(segment.y2()) && Double.isFinite(segment.z2()), style);
                length += Math.sqrt(Math.pow(segment.x2() - segment.x1(), 2)
                        + Math.pow(segment.y2() - segment.y1(), 2)
                        + Math.pow(segment.z2() - segment.z1(), 2));
            }
            for (EffectBatch.Quad quad : batch.quads()) {
                for (double value : new double[]{quad.ax(), quad.ay(), quad.az(), quad.bx(), quad.by(), quad.bz(),
                        quad.cx(), quad.cy(), quad.cz(), quad.dx(), quad.dy(), quad.dz()}) {
                    assertTrue(Double.isFinite(value), style);
                }
            }
            signatures.add(batch.segments().size() + "/" + batch.quads().size() + "/" + Math.round(length * 100.0));
        }
        assertEquals(7, signatures.size());
    }

    @Test
    void fadedMarkersEmitNothing() {
        EffectBatch batch = new EffectBatch();
        TargetEspGeometry.emit(batch, TargetEspGeometry.CINEMATIC,
                new TargetEspGeometry.Marker(0, 0, 0, 2, 1, 0.5, 0xFFFFFF, 0.009, 0, true), 0);
        assertTrue(batch.isEmpty());
    }
}
