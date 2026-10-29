package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec2;
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
}
