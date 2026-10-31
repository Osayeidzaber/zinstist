package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LyricSkyTest {
    private static final long S = 1_000_000_000L;

    @Test
    void eachLineGetsItsOwnCompassPointAndAnEarlierLineNeverMoves() {
        LyricSky sky = new LyricSky();
        LyricSky.Entry south = sky.place("one", 0, 0, 0, 1, 0, 5);
        LyricSky.Entry east = sky.place("two", 1, 1, 0, 0, S, 5);
        assertEquals(0, south.azimuth, 1e-9);
        assertEquals(-Math.PI / 2, east.azimuth, 1e-6);
        assertEquals(0, south.azimuth, 1e-12);
        assertEquals(Math.toRadians(LyricSky.MIN_ELEVATION_DEGREES), south.elevation, 1e-12);
        assertEquals(2, sky.entries().size());
    }

    @Test
    void linesSitInTheSkyEvenWhenTheGazeIsLevelOrDown() {
        LyricSky sky = new LyricSky();
        LyricSky.Entry level = sky.place("a", 0, 0, 0, 1, 0, 5);
        LyricSky.Entry down = sky.place("b", 1, 0, -1, 0.2, S, 5);
        double minY = Math.sin(Math.toRadians(LyricSky.MIN_ELEVATION_DEGREES));
        assertTrue(LyricSky.direction(level.azimuth, level.elevation)[1] >= minY - 1e-9);
        assertTrue(LyricSky.direction(down.azimuth, down.elevation)[1] >= minY - 1e-9);
    }

    @Test
    void aCrowdedLineRisesARowWhileTheOlderLineStaysPut() {
        LyricSky sky = new LyricSky();
        LyricSky.Entry first = sky.place("one", 0, 0, 0, 1, 0, 5);
        double firstElevation = first.elevation;
        LyricSky.Entry second = sky.place("two", 1, 0, 0, 1, S, 5);
        assertEquals(firstElevation + Math.toRadians(LyricSky.ROW_STEP_DEGREES), second.elevation, 1e-9);
        assertEquals(firstElevation, first.elevation, 1e-12);
        assertTrue(second.elevation <= Math.toRadians(LyricSky.MAX_ELEVATION_DEGREES));
    }

    @Test
    void linesBeyondTheLimitFadeQuicklyThenExpire() {
        LyricSky sky = new LyricSky();
        LyricSky.Entry oldest = sky.place("a", 0, 0, 0, 1, 0, 2);
        sky.place("b", 1, 1, 0, 0, S, 2);
        LyricSky.Entry newest = sky.place("c", 2, -1, 0, 0, 2 * S, 2);
        assertTrue(oldest.evicted());
        assertTrue(LyricSky.alpha(oldest, 2 * S + S / 10, 14, false) > 0);
        assertEquals(0, LyricSky.alpha(oldest, 3 * S, 14, false), 1e-9);
        sky.step(3 * S, 14, 2);
        assertEquals(2, sky.entries().size());
        assertSame(newest, sky.active(2));
    }

    @Test
    void theSharedLifecycleDrivesSkyLinesToo() {
        LyricSky sky = new LyricSky();
        LyricSky.Entry line = sky.place("one", 0, 0, 0, 1, 0, 5);
        assertEquals(LyricStates.State.INCOMING, line.state(S / 20, 14, true));
        assertEquals(LyricStates.State.SETTLING, line.state(S / 5, 14, true));
        assertEquals(LyricStates.State.ACTIVE, line.state(S, 14, true));
        assertEquals(LyricStates.State.TRANSITIONING, line.state(5 * S, 14, false));
        assertEquals(LyricStates.State.FADING, line.state(15 * S, 14, false));
        assertEquals(LyricStates.State.EXPIRED, line.state(16 * S, 14, false));
        sky.step(16 * S, 14, 1);
        assertEquals(0, sky.entries().size());
    }

    @Test
    void theQuadCentresOnTheAnchorAndFacesTheViewer() {
        double radius = 10, width = 4, height = 2;
        float[] corners = LyricSky.corners(0, Math.toRadians(30), radius, width, height);
        assertEquals(width, distance(corners, 0, 1), 1e-4);
        assertEquals(height, distance(corners, 0, 3), 1e-4);
        double[] dir = LyricSky.direction(0, Math.toRadians(30));
        double cx = 0, cy = 0, cz = 0;
        for (int i = 0; i < 4; i++) {
            cx += corners[i * 3] / 4.0;
            cy += corners[i * 3 + 1] / 4.0;
            cz += corners[i * 3 + 2] / 4.0;
        }
        assertEquals(dir[0] * radius, cx, 1e-3);
        assertEquals(dir[1] * radius, cy, 1e-3);
        assertEquals(dir[2] * radius, cz, 1e-3);
        for (int i = 0; i < 4; i++) {
            double dot = (corners[i * 3] - cx) * dir[0] + (corners[i * 3 + 1] - cy) * dir[1]
                    + (corners[i * 3 + 2] - cz) * dir[2];
            assertEquals(0, dot, 1e-3);
        }
    }

    private static double distance(float[] corners, int a, int b) {
        double dx = corners[a * 3] - corners[b * 3];
        double dy = corners[a * 3 + 1] - corners[b * 3 + 1];
        double dz = corners[a * 3 + 2] - corners[b * 3 + 2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
