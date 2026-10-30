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
        for (String style : new String[] {TargetEspGeometry.HALO, TargetEspGeometry.ORBIT,
                TargetEspGeometry.HELIX, TargetEspGeometry.BRACKETS}) {
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
}
