package dev.jilaton.client.render.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WingMotionTest {
    @Test
    void phaseLagIsStrictlyMonotonicFromLeadingFeatherToTip() {
        double previous = -1;
        for (int index = 0; index < 40; index++) {
            double lag = WingMotion.lagFor(index);
            assertTrue(lag > previous, "feather " + index + " does not trail the one above it");
            previous = lag;
        }
    }

    @Test
    void eachFeatherReachesItsCrestLaterThanTheOneBeforeIt() {
        double base = WingMotion.baseFlap("Feather");
        double amplitude = WingMotion.amplitude("Feather");
        int crestFeather = 6;
        // The leading feather peaks at phase pi/2; feather n peaks at pi/2 + lag(n).
        double lead = WingMotion.flapAngle(Math.PI / 2, 0, base, amplitude);
        assertEquals(base + amplitude, lead, 1e-9);
        double crest = Math.PI / 2 + WingMotion.lagFor(crestFeather);
        double trailing = WingMotion.flapAngle(crest, crestFeather, base, amplitude);
        assertEquals(base + amplitude, trailing, 1e-9);
        assertTrue(WingMotion.lagFor(crestFeather) > WingMotion.lagFor(crestFeather - 1));
    }

    @Test
    void flapStaysInsideItsAmplitudeBand() {
        for (String style : WingMotion.STYLES) {
            double base = WingMotion.baseFlap(style);
            double amplitude = WingMotion.amplitude(style);
            for (int step = 0; step < 400; step++) {
                double phase = step * 0.05;
                for (int feather = 0; feather < 20; feather++) {
                    double angle = WingMotion.flapAngle(phase, feather, base, amplitude);
                    assertTrue(angle >= base - amplitude - 1e-9 && angle <= base + amplitude + 1e-9);
                }
            }
        }
    }

    @Test
    void reducedMotionFreezesThePhase() {
        double frozen = WingMotion.advancedPhase(1.25, 0.5, 2.0, true);
        assertEquals(1.25, frozen, 0);
        double advanced = WingMotion.advancedPhase(1.25, 0.5, 2.0, false);
        assertEquals(1.25 + Math.PI * 2, advanced, 1e-9);
    }

    @Test
    void animateOffHoldsEveryFeatherStill() {
        double base = WingMotion.baseFlap("Feather");
        double amplitude = WingMotion.amplitude("Feather");
        double frozen = WingMotion.advancedPhase(0.7, 1.0, 1.3, true);
        for (int feather = 0; feather < 12; feather++) {
            double angle = WingMotion.flapAngle(frozen, feather, base, amplitude);
            double later = WingMotion.flapAngle(WingMotion.advancedPhase(frozen, 0.5, 1.3, true),
                    feather, base, amplitude);
            assertEquals(angle, later, 0, "feather " + feather + " still moves with animate off");
        }
    }

    @Test
    void phaseAdvanceIsFrameRateIndependent() {
        double oneStep = WingMotion.advancedPhase(0, 0.25, 0.9, false);
        double manySteps = 0;
        for (int i = 0; i < 100; i++) manySteps = WingMotion.advancedPhase(manySteps, 0.0025, 0.9, false);
        assertEquals(oneStep, manySteps, 1e-9);
    }

    @Test
    void qualityReducesFeatherAndSegmentCounts() {
        for (String style : WingMotion.STYLES) {
            for (int layer = 0; layer < WingMotion.layers(style); layer++) {
                int low = WingMotion.feathers(style, layer, 0);
                int balanced = WingMotion.feathers(style, layer, 1);
                int high = WingMotion.feathers(style, layer, 2);
                int cinematic = WingMotion.feathers(style, layer, 3);
                assertTrue(low <= balanced && balanced <= high && high <= cinematic);
                assertTrue(low < cinematic, style + " does not drop feathers at low quality");
            }
            assertTrue(WingMotion.segments(style, 0) < WingMotion.segments(style, 3));
        }
    }

    @Test
    void featherFanRunsFromUpwardToTheWingTip() {
        for (int count : List.of(2, 5, 12)) {
            double first = WingMotion.fanDegrees(0, count);
            double last = WingMotion.fanDegrees(count - 1, count);
            assertTrue(first > last, "fan should open downward");
            for (int i = 1; i < count; i++) {
                assertTrue(WingMotion.fanDegrees(i, count) < WingMotion.fanDegrees(i - 1, count));
            }
        }
    }

    @Test
    void generatedFeathersForEveryStyleAndQualityAreWellFormed() {
        for (String style : WingMotion.STYLES) {
            for (int quality = 0; quality < 4; quality++) {
                Mesh mesh = Meshes.strip(WingMotion.segments(style, quality), WingMotion.halfWidth(style),
                        WingMotion.tipWidth(style), WingMotion.fold(style), WingMotion.bend(style));
                assertTrue(mesh.quadCount() > 0);
                assertEquals(mesh.quadCount() * 4, mesh.indices().length);
                for (int index : mesh.indices()) {
                    assertTrue(index >= 0 && index < mesh.vertexCount());
                }
                for (int i = 0; i < mesh.vertexCount(); i++) {
                    float nx = mesh.normals()[i * 3], ny = mesh.normals()[i * 3 + 1], nz = mesh.normals()[i * 3 + 2];
                    assertEquals(1F, (float) Math.sqrt(nx * nx + ny * ny + nz * nz), 2e-3F);
                }
            }
        }
    }

    @Test
    void materialGradientRunsFromBaseToTipAndEmissiveAddsTheAccent() {
        Material matte = new Material(0x101010, 0xF0F0F0, 0x8844FF, 0);
        Material emissive = new Material(0x101010, 0xF0F0F0, 0x8844FF, 1);
        int root = matte.color(0, 1, 1);
        int tip = matte.color(1, 1, 1);
        assertTrue((root & 0xFF) < (tip & 0xFF), "tip should be lighter than the root");
        assertTrue((emissive.color(1, 0, 1) & 0xFF0000) > (matte.color(1, 0, 1) & 0xFF0000));
        assertEquals(0x80, matte.color(0.5, 0.5, 0.5) >>> 24 & 0xFF);
        assertEquals(0, matte.color(0.5, 0.5, 0) >>> 24);
    }
}
