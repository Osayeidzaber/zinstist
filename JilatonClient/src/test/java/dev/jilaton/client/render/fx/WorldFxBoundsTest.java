package dev.jilaton.client.render.fx;

import dev.jilaton.client.module.impl.JumpCirclePolicy;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure bounds and one-shot rules for the world effects: waves, jump circles, ambience and particle pools. */
class WorldFxBoundsTest {
    private static final long S = 1_000_000_000L;

    @Test
    void everySupportedWaveRadiusStaysInsideTheSampleAndLifeCaps() {
        for (double radius = 40; radius <= 50; radius += 0.5) {
            assertEquals(TerrainWave.MAX_SAMPLES, TerrainWave.samples(radius), "samples at " + radius);
            assertEquals(radius, TerrainWave.radius(1, radius), 1e-9);
            assertEquals(0, TerrainWave.radius(0, radius), 1e-9);
            assertEquals(1, TerrainWave.alpha(0), 1e-9);
            assertEquals(0, TerrainWave.alpha(1), 1e-9);
        }
        int previous = 0;
        for (double radius = 0; radius <= 64; radius += 0.5) {
            int samples = TerrainWave.samples(radius);
            assertTrue(samples >= 24 && samples <= TerrainWave.MAX_SAMPLES);
            assertTrue(samples >= previous, "samples must not shrink as the wave grows");
            previous = samples;
        }
        assertEquals(160, TerrainWave.MAX_SAMPLES);
        assertEquals(1400, TerrainWave.MAX_BLOCKS_PER_FRAME);
        assertEquals(1.6, TerrainWave.MAX_LIFE_SECONDS, 1e-12);
    }

    @Test
    void theFrameSampleBudgetIsSharedAcrossEveryLiveWave() {
        assertEquals(240, TerrainWave.MAX_SAMPLES_PER_FRAME);
        assertEquals(TerrainWave.MAX_SAMPLES, TerrainWave.samplesThisFrame(44, 0));
        assertEquals(80, TerrainWave.samplesThisFrame(44, TerrainWave.MAX_SAMPLES));
        assertEquals(40, TerrainWave.samplesThisFrame(44, 200));
        assertEquals(0, TerrainWave.samplesThisFrame(44, TerrainWave.MAX_SAMPLES_PER_FRAME));
        assertEquals(0, TerrainWave.samplesThisFrame(44, 999));
        assertEquals(TerrainWave.samples(44), TerrainWave.samplesThisFrame(44, -50),
                "a negative counter behaves like the start of the frame");
        assertEquals(24, TerrainWave.samplesThisFrame(2, 0), "small waves keep their own minimum");
    }

    @Test
    void blockBudgetIsSplitAcrossEveryLiveWave() {
        assertEquals(TerrainWave.MAX_BLOCKS_PER_FRAME, TerrainWave.blockBudget(0));
        assertEquals(TerrainWave.MAX_BLOCKS_PER_FRAME, TerrainWave.blockBudget(1));
        assertEquals(700, TerrainWave.blockBudget(2));
        assertEquals(466, TerrainWave.blockBudget(3));
        assertEquals(280, TerrainWave.blockBudget(5));
        assertEquals(1, TerrainWave.blockBudget(10_000), "every wave always gets at least one cell");
        for (int waves = 1; waves <= 64; waves++) {
            assertTrue(TerrainWave.blockBudget(waves) * waves <= TerrainWave.MAX_BLOCKS_PER_FRAME,
                    "shares must stay inside the frame cap at " + waves + " waves");
        }
    }

    @Test
    void blockTopReusesACallerBufferAndMatchesTheAllocatingVariant() {
        double[] allocated = TerrainWave.blockTop(10.7, -3.2, 64);
        double[] buffer = new double[14];
        Arrays.fill(buffer, 42);
        TerrainWave.blockTop(10.7, -3.2, 64, buffer);
        assertArrayEquals(allocated, Arrays.copyOf(buffer, 12), 0);
        assertEquals(42, buffer[12], 0, "trailing entries must stay untouched");
        assertEquals(42, buffer[13], 0, "trailing entries must stay untouched");
        for (double value : Arrays.copyOf(buffer, 12)) assertTrue(Double.isFinite(value));
        assertThrows(IllegalArgumentException.class, () -> TerrainWave.blockTop(0, 0, 0, new double[11]));
        assertThrows(IllegalArgumentException.class, () -> TerrainWave.blockTop(0, 0, 0, null));
    }

    @Test
    void jumpCirclePolicyAllowsExactlyOneEffectPerJumpLandCycle() {
        long now = 10 * S;
        assertEquals(120_000_000L, JumpCirclePolicy.EVENT_GAP_NANOS);
        assertEquals(1_500_000_000L, JumpCirclePolicy.LANDING_GRACE_NANOS);
        assertTrue(JumpCirclePolicy.withinCooldown(now, now - 119_999_999L));
        assertFalse(JumpCirclePolicy.withinCooldown(now, now - 120_000_000L));
        assertFalse(JumpCirclePolicy.withinCooldown(now, 0));

        // A jump fires on the jump trigger; its own landing is then suppressed by the grace window.
        assertTrue(JumpCirclePolicy.emits(true, false, "Jump"));
        assertFalse(JumpCirclePolicy.emits(true, false, "Land"));
        long jumpAt = now;
        long graceUntil = jumpAt + JumpCirclePolicy.LANDING_GRACE_NANOS;
        assertTrue(JumpCirclePolicy.landingCovered(jumpAt + S, graceUntil));
        assertFalse(JumpCirclePolicy.landingCovered(graceUntil, graceUntil));
        assertFalse(JumpCirclePolicy.landingCovered(graceUntil + 1, graceUntil));

        // A landing without a jump still fires on the landing trigger, and only once.
        assertTrue(JumpCirclePolicy.emits(false, true, "Land"));
        assertFalse(JumpCirclePolicy.emits(false, true, "Jump"));
        assertFalse(JumpCirclePolicy.emits(false, false, "Both"));
        assertTrue(JumpCirclePolicy.emits(true, true, "Both"));
    }

    @Test
    void ambiencePrecedenceIsAStrictTotalOrder() {
        int sky = 0x112233, cyber = 0x445566, glass = 0x778899, client = 0xAABBCC;
        for (int mask = 0; mask < 16; mask++) {
            boolean customSky = (mask & 8) != 0, cyberspace = (mask & 4) != 0;
            boolean glassMaterial = (mask & 2) != 0, fullbright = (mask & 1) != 0;
            AmbiencePrecedence.Scene owner =
                    AmbiencePrecedence.owner(customSky, cyberspace, glassMaterial, fullbright);
            AmbiencePrecedence.Scene expected = customSky ? AmbiencePrecedence.Scene.CUSTOM_SKY
                    : cyberspace ? AmbiencePrecedence.Scene.CYBERSPACE
                    : glassMaterial ? AmbiencePrecedence.Scene.GLASS
                    : fullbright ? AmbiencePrecedence.Scene.FULLBRIGHT
                    : AmbiencePrecedence.Scene.CLIENT;
            assertEquals(expected, owner, "mask " + mask);
            int expectedAccent = switch (expected) {
                case CUSTOM_SKY -> sky;
                case CYBERSPACE -> cyber;
                case GLASS -> glass;
                default -> client;
            };
            assertEquals(expectedAccent, AmbiencePrecedence.accent(owner, sky, cyber, glass, client));
            assertEquals(expected != AmbiencePrecedence.Scene.CUSTOM_SKY,
                    AmbiencePrecedence.drawsSkyStreaks(owner));
            for (boolean nightOnly : new boolean[]{false, true}) {
                assertEquals(nightOnly && !fullbright, AmbiencePrecedence.nightGateApplies(nightOnly, fullbright));
            }
        }
    }

    @Test
    void particleBudgetsNeverExceedThePoolCapacity() {
        assertEquals(4096, FxParticles.CAPACITY);
        assertEquals(192, FxParticles.DECAL_CAPACITY);
        int previous = 0;
        for (VisualQuality quality : VisualQuality.values()) {
            assertTrue(quality.particleBudget() >= 256);
            assertTrue(quality.particleBudget() <= FxParticles.CAPACITY);
            assertTrue(quality.particleBudget() >= previous, "later tiers must not shrink the pool");
            assertTrue(quality.circleSegments() > 0 && quality.circleSegments() <= 128);
            previous = quality.particleBudget();
        }
        assertEquals(256, VisualQuality.LOW.particleBudget());
        assertEquals(768, VisualQuality.BALANCED.particleBudget());
        assertEquals(1536, VisualQuality.HIGH.particleBudget());
        assertEquals(3072, VisualQuality.CINEMATIC.particleBudget());
    }
}
