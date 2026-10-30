package dev.jilaton.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldEffectsTest {
    @Test
    void batchDropsInvisiblePrimitivesAndEnforcesCaps() {
        EffectBatch batch = new EffectBatch();
        batch.line(0, 0, 0, 1, 1, 1, 0x00FFFFFF, 1, false);
        batch.quad(0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, false);
        assertTrue(batch.isEmpty());
        for (int i = 0; i < EffectBatch.MAX_SEGMENTS + 500; i++) batch.line(0, 0, 0, 1, 0, 0, 0xFFFFFFFF, 1, false);
        assertEquals(EffectBatch.MAX_SEGMENTS, batch.segments().size());
        for (int i = 0; i < EffectBatch.MAX_QUADS + 500; i++)
            batch.quad(0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0x10FFFFFF, 0, 0, 0, true);
        assertEquals(EffectBatch.MAX_QUADS, batch.quads().size());
    }

    @Test
    void ringAndBandLieOnTheirRadius() {
        EffectBatch batch = new EffectBatch();
        batch.ring(1, 2, 3, 0.75, 32, 0xFFFFFFFF, 2, true, 0.5, 0);
        batch.band(1, 2, 3, 0.5, 0.75, 16, 0x00FFFFFF, 0x80FFFFFF, true);
        assertEquals(32, batch.segments().size());
        assertEquals(16, batch.quads().size());
        for (EffectBatch.Segment s : batch.segments()) {
            assertEquals(0.75, Math.hypot(s.x1() - 1, s.z1() - 3), 1e-9);
            assertEquals(2, s.y1(), 1e-9);
        }
        EffectBatch.Quad q = batch.quads().get(0);
        assertEquals(0.5, Math.hypot(q.ax() - 1, q.az() - 3), 1e-9);
        assertEquals(0.75, Math.hypot(q.cx() - 1, q.cz() - 3), 1e-9);
    }

    @Test
    void paletteDefaultsToMonochromeAndValidatesHex() {
        assertEquals(EffectPalette.MONO_RGB, EffectPalette.resolve(EffectPalette.MONO, 0xFF8878FF, "FF0000"));
        assertEquals(0x8878FF, EffectPalette.resolve(EffectPalette.THEME, 0xFF8878FF, "FF0000"));
        assertEquals(0xFF0000, EffectPalette.resolve(EffectPalette.CUSTOM, 0, "#ff0000"));
        assertEquals(EffectPalette.MONO_RGB, EffectPalette.resolve(EffectPalette.CUSTOM, 0, "nope"));
        assertEquals("ABCDEF", EffectPalette.normalizeHex("#abcdef"));
        assertThrows(IllegalArgumentException.class, () -> EffectPalette.normalizeHex("12345"));
        assertEquals(EffectPalette.MONO, EffectPalette.modeSetting("c", "C").value());
    }

    @Test
    void everyTargetStyleStaysNearItsTargetAndRespectsOpacity() {
        for (String style : TargetEspGeometry.STYLES) {
            EffectBatch batch = new EffectBatch();
            TargetEspGeometry.emit(batch, style, new TargetEspGeometry.Marker(4, 1, -2, 1.8, 0.7, 3.2,
                    0xF2F2F2, 0.9, 0.5, true));
            assertTrue(batch.size() > 0, style + " must draw");
            for (EffectBatch.Segment s : batch.segments()) {
                assertTrue(s.walls());
                assertTrue((s.color() >>> 24) <= Math.round(0.9 * 255), style + " exceeds requested opacity");
                for (double[] p : new double[][] {{s.x1(), s.y1(), s.z1()}, {s.x2(), s.y2(), s.z2()}}) {
                    assertTrue(Math.hypot(p[0] - 4, p[2] + 2) < 0.7 * 1.6, style + " strays horizontally");
                    assertTrue(p[1] > 1 - 0.5 && p[1] < 1 + 1.8 + 0.5, style + " strays vertically");
                }
            }
            EffectBatch hidden = new EffectBatch();
            TargetEspGeometry.emit(hidden, style, new TargetEspGeometry.Marker(0, 0, 0, 1.8, 0.7, 0, 0xFFFFFF, 0.005, 0, false));
            assertTrue(hidden.isEmpty());
        }
    }

    @Test
    void unknownStyleFallsBackToHalo() {
        EffectBatch fallback = new EffectBatch();
        EffectBatch halo = new EffectBatch();
        TargetEspGeometry.Marker marker = new TargetEspGeometry.Marker(0, 0, 0, 1.8, 0.7, 1, 0xFFFFFF, 1, 0, false);
        TargetEspGeometry.emit(fallback, "Removed style", marker);
        TargetEspGeometry.emit(halo, TargetEspGeometry.HALO, marker);
        assertEquals(halo.size(), fallback.size());
    }

    @Test
    void terrainShockwaveFollowsUnevenGroundHeights() {
        EffectBatch batch = new EffectBatch();
        // Stepped terrain: +1.0 block higher on the +X side than on the -X side.
        dev.jilaton.client.module.impl.JumpCirclesModule.emitTerrainShockwave(
                batch,
                (wx, wz) -> wx >= 0 ? 65.0 : 64.0,
                0, 64.0, 0,
                0, 64.0, 0,
                2.0, 24, 0xF2F2F2, 0.85,
                2.0F, true, true, 0.35
        );
        assertTrue(batch.segments().size() > 24);
        assertEquals(24, batch.quads().size());
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (EffectBatch.Segment s : batch.segments()) {
            minY = Math.min(minY, Math.min(s.y1(), s.y2()));
            maxY = Math.max(maxY, Math.max(s.y1(), s.y2()));
        }
        assertTrue(maxY - minY >= 0.95, "Shockwave vertices must follow terrain step height difference");
    }

    @Test
    void cyberspaceSkeletonGlassBlockPlacementAndJilatonGateEmitValidGeometry() {
        var joints = dev.jilaton.client.render.model.BodyAnchor.skeleton(
                0, 64, 0, 1.8, 25.0F, -10.0F, 35.0F, false, 0.25F, 1.0F, 0.35F
        );
        EffectBatch cyberBatch = new EffectBatch();
        dev.jilaton.client.module.impl.CyberspaceModule.emitSkeleton(
                cyberBatch, joints, 0, 64, 0, 0x4AF2EA, 0x8878FF, 0.9, 2.0F, true, true
        );
        assertTrue(cyberBatch.segments().size() >= 20, "Cyberspace skeleton must emit bones and target lock ring");

        EffectBatch glassBatch = new EffectBatch();
        dev.jilaton.client.module.impl.GlassMaterialModule.emitGlassShell(
                glassBatch, 0, 64, 0, 0.6, 1.8, joints, 0, 64, 0,
                0xC6E2F5, 0xFFFFFF, 0.5, 0.85, 0.7, true, 1.5
        );
        assertTrue(glassBatch.quads().size() >= 16, "Glass material shell must emit translucent facets");
        assertTrue(glassBatch.segments().size() >= 24, "Glass material shell must emit Fresnel rim highlights");

        for (String style : dev.jilaton.client.module.impl.BlockPlacementModule.STYLES) {
            EffectBatch placeBatch = new EffectBatch();
            dev.jilaton.client.module.impl.BlockPlacementModule.emitPlacementEffect(
                    placeBatch, style, 2, 64, 2, 3, 65, 3, 0, 64, 0,
                    0.35, 0.16, 0xF2F2F2, 0.88, 2.2F, true
            );
            assertTrue(placeBatch.segments().size() >= 4, style + " block placement must emit wireframe edges");
        }

        for (String style : dev.jilaton.client.module.impl.JilatonGateModule.STYLES) {
            EffectBatch gateBatch = new EffectBatch();
            dev.jilaton.client.module.impl.JilatonGateModule.emitGate(
                    gateBatch, style, 0, 64, 4, 0.0, 0, 64, 0,
                    1.0, 0x8878FF, 0.92, true, true, 2.0
            );
            assertTrue(gateBatch.quads().size() >= 20, style + " gate must emit 3D pillar and portal quads");
            assertTrue(gateBatch.segments().size() >= 40, style + " gate must emit pillar edges, vortex, and 3D Jilaton emblem");
        }
    }
}
