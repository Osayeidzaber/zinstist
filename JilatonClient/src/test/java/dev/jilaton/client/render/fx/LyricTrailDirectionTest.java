package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Trail lines spawn beside the gaze, in view but not on the crosshair. */
class LyricTrailDirectionTest {
    @Test
    void trailDirectionTurnsByTheSideAngleAndKeepsPitch() {
        double[] dir = LyricLayout.turned(0, -0.3, 1, 26);
        assertEquals(-0.3, dir[1], 1e-9);
        double cos = (dir[0] * 0 + dir[2] * 1) / Math.hypot(dir[0], dir[2]);
        assertEquals(26, Math.toDegrees(Math.acos(cos)), 1e-6);
        assertTrue(Math.abs(dir[0]) > 0.4);
    }
}
