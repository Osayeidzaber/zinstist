package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetStrafeMathTest {
    @Test
    void followsTheRequestedOrbitAndCanReverseDirection() {
        Vec2 clockwise = TargetStrafeMath.movementInput(2.95, 0.0, 0.0, 0.0, 0.0F, 2.95, 1, 1.0);
        Vec2 counterClockwise = TargetStrafeMath.movementInput(2.95, 0.0, 0.0, 0.0, 0.0F, 2.95, -1, 1.0);

        assertEquals(0.0F, clockwise.x, 1.0E-5F);
        assertEquals(-1.0F, clockwise.y, 1.0E-5F);
        assertEquals(0.0F, counterClockwise.x, 1.0E-5F);
        assertEquals(1.0F, counterClockwise.y, 1.0E-5F);
    }

    @Test
    void correctsTowardTheRadiusAndRotatesIntoPlayerLocalCoordinates() {
        Vec2 movement = TargetStrafeMath.movementInput(4.0, 0.0, 0.0, 0.0, 90.0F, 2.0, 1, 1.0);

        assertTrue(movement.x < 0.0F);
        assertTrue(movement.y > 0.0F);
        assertEquals(1.0F, movement.length(), 1.0E-5F);
    }

    @Test
    void stopsAtTheTargetAndClampsStrength() {
        Vec2 stopped = TargetStrafeMath.movementInput(0.0, 0.0, 0.0, 0.0, 0.0F, 3.0, 1, 1.0);
        Vec2 limited = TargetStrafeMath.movementInput(3.0, 0.0, 0.0, 0.0, 0.0F, 3.0, 1, 4.0);

        assertEquals(Vec2.ZERO, stopped);
        assertEquals(1.0F, limited.length(), 1.0E-5F);
    }

    @Test
    void motionModeUsesTangentAtOrbitRadiusAndCorrectsRadiallyOutsideIt() {
        Vec3 tangent = TargetStrafeMath.motionVector(2.95, 0.0, 0.0, 0.0, 2.95, 1, 0.2, 1.0);
        Vec3 correction = TargetStrafeMath.motionVector(4.0, 0.0, 0.0, 0.0, 2.0, 1, 0.2, 1.0);

        assertEquals(0.0, tangent.x, 1.0E-5);
        assertEquals(-0.2, tangent.z, 1.0E-5);
        assertTrue(correction.x < 0.0);
        assertTrue(correction.z < 0.0);
        assertEquals(0.2, correction.horizontalDistance(), 1.0E-5);
    }

    @Test
    void motionModePreservesZeroForCoincidentTargetAndClampsStrength() {
        Vec3 stopped = TargetStrafeMath.motionVector(1.0, 1.0, 1.0, 1.0, 2.0, 1, 0.2, 1.0);
        Vec3 limited = TargetStrafeMath.motionVector(2.95, 0.0, 0.0, 0.0, 2.95, 1, 0.2, 4.0);

        assertEquals(Vec3.ZERO, stopped);
        assertEquals(0.2, limited.horizontalDistance(), 1.0E-5);
    }
}
