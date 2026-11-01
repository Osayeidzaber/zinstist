package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothFollowTest {
    @Test
    void trailsBehindAndConvergesIndependentOfFrameRate() {
        Vec3 target = new Vec3(2, 0, 0);
        Vec3 at60 = Vec3.ZERO, at240 = Vec3.ZERO;
        for (int i = 0; i < 30; i++) at60 = SmoothFollow.step(at60, target, 1 / 60.0, 0.55);
        for (int i = 0; i < 120; i++) at240 = SmoothFollow.step(at240, target, 1 / 240.0, 0.55);
        assertTrue(at60.x > 1.5 && at60.x < 2, "half a second later the camera is close but still trailing: " + at60.x);
        assertEquals(at60.x, at240.x, 1e-9);
    }

    @Test
    void zeroSmoothingAndTeleportsSnap() {
        assertEquals(new Vec3(1, 0, 0), SmoothFollow.step(Vec3.ZERO, new Vec3(1, 0, 0), 0.016, 0));
        assertEquals(new Vec3(20, 0, 0), SmoothFollow.step(Vec3.ZERO, new Vec3(20, 0, 0), 0.016, 0.9));
    }
}
