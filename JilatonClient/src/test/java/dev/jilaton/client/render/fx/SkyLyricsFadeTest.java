package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sky Lyrics rendering-side contract: soft fades, bounded live lines and anchors that never move. */
class SkyLyricsFadeTest {
    private static final long S = 1_000_000_000L;

    @Test
    void theAlphaPathIsSmoothMonotonicAndContinuousFromBirthToExpiry() {
        double linger = 14;
        double previous = -1;
        double maxRise = 0, maxFall = 0;
        int frames = (int) Math.round((linger + LyricStates.FADE_SECONDS) * 60);
        for (int i = 0; i <= frames + 2; i++) {
            double age = i / 60.0;
            double alpha = LyricStates.alpha(age, false, linger, -1);
            assertTrue(alpha >= 0 && alpha <= 1);
            if (previous >= 0) {
                double delta = alpha - previous;
                if (age <= linger) maxRise = Math.max(maxRise, delta);
                else maxFall = Math.max(maxFall, -delta);
            }
            previous = alpha;
        }
        // Entrance eases in over 0.38 s; every frame moves less than 14% and never backwards.
        assertTrue(maxRise <= 0.14, "max entrance step " + maxRise);
        // Exit fades over 1.4 s and never moves upwards.
        assertTrue(maxFall <= 0.02, "max exit step " + maxFall);
        // No discontinuity at the linger boundary: full brightness, then the fade starts from the top.
        assertEquals(1, LyricStates.alpha(linger - 1e-9, false, linger, -1), 1e-12);
        assertTrue(LyricStates.alpha(linger + 1e-9, false, linger, -1) > 0.999);
        assertEquals(1, LyricStates.alpha(linger, false, linger, -1), 1e-12);
        assertEquals(0, LyricStates.alpha(linger + LyricStates.FADE_SECONDS, false, linger, -1), 0);
    }

    @Test
    void evictionFadesStrictlyDownAndExpiresInsideTheFastWindow() {
        double previous = 1.0001;
        for (int i = 0; i < 90; i++) {
            double age = i * (LyricStates.EVICT_SECONDS / 90.0);
            double alpha = LyricStates.alpha(5, false, 14, age);
            assertTrue(alpha < previous, "eviction fade must keep decreasing at " + age);
            previous = alpha;
        }
        assertEquals(LyricStates.State.FADING,
                LyricStates.state(5, false, 14, LyricStates.EVICT_SECONDS - 1e-9));
        assertEquals(LyricStates.State.EXPIRED,
                LyricStates.state(5, false, 14, LyricStates.EVICT_SECONDS));
        assertEquals(0, LyricStates.alpha(5, false, 14, LyricStates.EVICT_SECONDS), 0);
    }

    @Test
    void theLiveLineCountIsHardBoundedForBothPlacements() {
        LyricSky sky = new LyricSky();
        for (int i = 0; i < 30; i++) {
            sky.place("sky " + i, i, Math.sin(i * 0.7), 0.5, Math.cos(i * 0.7), i * S, 999);
        }
        assertEquals(8, LyricSky.MAX_LINES);
        assertEquals(LyricSky.MAX_LINES, sky.entries().stream().filter(e -> !e.evicted()).count());
        // Half a second after the last placement every evicted line has finished its 0.45 s fade.
        sky.step(29 * S + S / 2, 14, 29);
        assertEquals(LyricSky.MAX_LINES, sky.entries().size());

        LyricTrail trail = new LyricTrail();
        for (int i = 0; i < 30; i++) {
            trail.place("trail " + i, i, 0, 64, 0, Math.sin(i * 0.7), 0.2, Math.cos(i * 0.7), 3, i * S, 999);
        }
        assertEquals(8, LyricTrail.MAX_LINES);
        assertEquals(LyricTrail.MAX_LINES, trail.entries().stream().filter(e -> !e.evicted()).count());
        trail.step(0.016, 29 * S + S / 2, 14, 29);
        assertEquals(LyricTrail.MAX_LINES, trail.entries().size());
    }

    @Test
    void aPlacedSkyLineKeepsItsAnchorOnTheCelestialSphere() {
        LyricSky sky = new LyricSky();
        LyricSky.Entry first = sky.place("first", 0, 0.1, 0.35, 1, 0, 5);
        double azimuth = first.azimuth, elevation = first.elevation;
        float[] before = LyricSky.corners(azimuth, elevation, LyricSky.RADIUS, 4, 1);
        for (int i = 1; i < 12; i++) {
            sky.place("line " + i, i, Math.sin(i * 1.3), 0.4, Math.cos(i * 1.3), i * S, 8);
        }
        sky.step(12 * S, 14, 11);
        assertEquals(azimuth, first.azimuth, 0);
        assertEquals(elevation, first.elevation, 0);
        assertArrayEquals(before,
                LyricSky.corners(first.azimuth, first.elevation, LyricSky.RADIUS, 4, 1), 0);
        for (LyricSky.Entry entry : sky.entries()) {
            double[] direction = LyricSky.direction(entry.azimuth, entry.elevation);
            double length = Math.sqrt(direction[0] * direction[0] + direction[1] * direction[1]
                    + direction[2] * direction[2]);
            assertEquals(1, length, 1e-9);
            double degrees = Math.toDegrees(Math.asin(direction[1]));
            assertTrue(degrees >= LyricSky.MIN_ELEVATION_DEGREES - 1e-9, "below horizon: " + degrees);
            assertTrue(degrees <= LyricSky.MAX_ELEVATION_DEGREES + 1e-9, "above zenith guard: " + degrees);
            assertFalse(Double.isNaN(entry.azimuth) || Double.isNaN(entry.elevation));
        }
    }

    @Test
    void theSungSkyLineNeverTimesOutWhileItIsActive() {
        LyricSky sky = new LyricSky();
        LyricSky.Entry line = sky.place("singing", 0, 0, 0.3, 1, 0, 8);
        long late = 60 * S;
        assertEquals(LyricStates.State.ACTIVE, line.state(late, 14, true));
        assertEquals(1, LyricSky.alpha(line, late, 14, true), 1e-12);
        sky.step(late, 14, 0);
        assertEquals(1, sky.entries().size());
        // The very same line would have expired had it stopped being sung.
        assertEquals(LyricStates.State.EXPIRED, line.state(late, 14, false));
    }
}
