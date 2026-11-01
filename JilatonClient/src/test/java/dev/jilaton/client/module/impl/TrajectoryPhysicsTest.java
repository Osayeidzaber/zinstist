package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrajectoryPhysicsTest {
    @Test
    void bowPowerMatchesVanilla() {
        assertEquals(0, TrajectoryPhysics.bowPower(0), 1e-6);
        assertEquals(1, TrajectoryPhysics.bowPower(20), 1e-6);
        assertEquals((0.25 + 1.0) / 3.0, TrajectoryPhysics.bowPower(10), 1e-6);
    }

    @Test
    void levelShotFallsAndSlows() {
        double[] pos = {0, 100, 0};
        double[] vel = TrajectoryPhysics.launch(0, 0, 3.0);
        assertEquals(3.0, vel[2], 1e-9);
        for (int i = 0; i < 20; i++) TrajectoryPhysics.step(pos, vel, TrajectoryPhysics.ARROW, false);
        assertTrue(pos[1] < 100, "gravity pulls it down");
        assertTrue(vel[2] < 3.0, "drag slows it");
        assertTrue(pos[2] > 40 && pos[2] < 60, "about 50 blocks in a second: " + pos[2]);
    }

    @Test
    void lookingUpGoesUp() {
        double[] vel = TrajectoryPhysics.launch(0, -45, 1.5);
        assertTrue(vel[1] > 1.0);
    }
}
