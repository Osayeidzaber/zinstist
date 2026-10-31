package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Boundary and guard behaviour for the movement vector math and policy clamps. */
class MovementMathBoundaryTest {
    private static final double EPS = 1.0E-9;

    @Test
    void strafeMathGuardsNonFiniteAndOutOfRangeInputs() {
        Vec3 movement = new Vec3(0.3, -0.2, 0.4);
        assertEquals(movement, StrafeMath.apply(movement, Float.NaN, 1.0, 0.0, 1.0, false));
        assertEquals(movement, StrafeMath.apply(movement, Float.POSITIVE_INFINITY, 1.0, 0.0, 1.0, false));
        Vec3 nanMovement = StrafeMath.apply(new Vec3(Double.NaN, 0.0, 0.0), 0.0F, 1.0, 0.0, 1.0, false);
        assertTrue(Double.isNaN(nanMovement.x));
        assertEquals(0.0, nanMovement.y, EPS);
        assertEquals(0.0, nanMovement.z, EPS);
        assertEquals(movement, StrafeMath.apply(movement, 0.0F, 1.0, 0.0, Double.NaN, false));

        assertEquals(movement, StrafeMath.apply(movement, 0.0F, 1.0, 0.0, -1.0, false));
        Vec3 fullStrength = StrafeMath.apply(movement, 0.0F, 1.0, 0.0, 2.0, false);
        assertEquals(0.0, fullStrength.x, 1.0E-6);
        assertEquals(Math.hypot(0.3, 0.4), fullStrength.z, EPS);
        assertEquals(-0.2, fullStrength.y, EPS);

        Vec3 verticalOnly = new Vec3(0.0, -0.5, 0.0);
        assertEquals(verticalOnly, StrafeMath.apply(verticalOnly, 0.0F, 1.0, 0.0, 1.0, false));

        Vec3 tinyInput = StrafeMath.apply(movement, 0.0F, 1.0E-7, 0.0, 1.0, true);
        assertEquals(0.0, tinyInput.x, EPS);
        assertEquals(0.0, tinyInput.z, EPS);
        assertEquals(-0.2, tinyInput.y, EPS);
        assertEquals(movement, StrafeMath.apply(movement, 0.0F, 1.0E-7, 0.0, 1.0, false));
    }

    @Test
    void velocityStrafeRejectsNonPositiveSpeedAndFallsBackWithoutDirection() {
        assertHorizontal(0.0, 0.0, VelocityStrafeMath.reorient(3.0, 4.0, 0.0F, true, false, false, false, 0.0));
        assertHorizontal(0.0, 0.0, VelocityStrafeMath.reorient(3.0, 4.0, 0.0F, true, false, false, false, -1.0));
        assertHorizontal(0.0, 0.0, VelocityStrafeMath.reorient(3.0, 4.0, 0.0F, true, false, false, false, Double.NaN));
        assertHorizontal(0.0, 0.0, VelocityStrafeMath.reorient(0.0, 0.0, 0.0F, true, true, false, false, 2.0));
        assertHorizontal(1.2, 1.6, VelocityStrafeMath.reorient(3.0, 4.0, 0.0F, true, true, false, false, 2.0));

        VelocityStrafeMath.Horizontal diagonal = VelocityStrafeMath.reorient(0.0, 0.0, 90.0F,
                true, false, true, false, 2.0);
        assertEquals(2.0, Math.hypot(diagonal.x(), diagonal.z()), EPS);
    }

    @Test
    void targetStrafeMathClampsRadiusStrengthAndRejectsBadSpeed() {
        Vec2 atZeroRadius = TargetStrafeMath.movementInput(3.0, 0.0, 0.0, 0.0, 0.0F, 0.0, 1, 1.0);
        Vec2 atNegativeRadius = TargetStrafeMath.movementInput(3.0, 0.0, 0.0, 0.0, 0.0F, -5.0, 1, 1.0);
        assertEquals(atZeroRadius.x, atNegativeRadius.x, 1.0E-6F);
        assertEquals(atZeroRadius.y, atNegativeRadius.y, 1.0E-6F);

        Vec2 zeroStrength = TargetStrafeMath.movementInput(3.0, 0.0, 0.0, 0.0, 0.0F, 1.0, 1, -1.0);
        assertEquals(0.0F, zeroStrength.x, 1.0E-6F);
        assertEquals(0.0F, zeroStrength.y, 1.0E-6F);
        assertEquals(1.0F, TargetStrafeMath.movementInput(3.0, 0.0, 0.0, 0.0, 0.0F, 1.0, 1, 5.0).length(),
                1.0E-5F);

        assertEquals(Vec3.ZERO, TargetStrafeMath.motionVector(0.0, 0.0, 1.0, 1.0, 2.0, 1, 0.0, 1.0));
        assertEquals(Vec3.ZERO, TargetStrafeMath.motionVector(0.0, 0.0, 1.0, 0.0, 2.0, 1, -0.2, 1.0));
        assertEquals(Vec3.ZERO, TargetStrafeMath.motionVector(0.0, 0.0, 1.0, 0.0, 2.0, 1, Double.NaN, 1.0));
        assertEquals(Vec3.ZERO,
                TargetStrafeMath.motionVector(0.0, 0.0, 1.0, 0.0, 2.0, 1, Double.POSITIVE_INFINITY, 1.0));
        assertEquals(Vec3.ZERO, TargetStrafeMath.motionVector(Double.NaN, 0.0, 1.0, 0.0, 2.0, 1, 0.2, 1.0));
        assertEquals(Vec3.ZERO, TargetStrafeMath.motionVector(2.0, 0.0, 0.0, 0.0, 2.0, 0, 0.3, 1.0));

        Vec3 converging = TargetStrafeMath.motionVector(3.0, 0.0, 0.0, 0.0, 2.0, 0, 0.3, 1.0);
        assertEquals(0.3, converging.horizontalDistance(), 1.0E-5);
        assertTrue(Double.isFinite(converging.x) && Double.isFinite(converging.z));
    }

    @Test
    void policyClampsBoundTheirInputs() {
        assertEquals(0, NoJumpDelayPolicy.rearm(-3, 4, 2));
        assertEquals(0, NoJumpDelayPolicy.rearm(5, -4, -2));
        assertEquals(4, NoJumpDelayPolicy.rearm(9, 4, 0));
        assertFalse(NoJumpDelayPolicy.shouldRearm(4, 4));
        assertTrue(NoJumpDelayPolicy.shouldRearm(5, 4));

        VelocityReversalPolicy.Motion motion = VelocityReversalPolicy.apply(2.0, 1.0, -2.0, 2.0, 1.5);
        assertEquals(-2.0, motion.x(), EPS);
        assertEquals(2.0, motion.z(), EPS);
        assertEquals(1.0, motion.y(), EPS);

        assertEquals(0.75, VelocityPolicy.applyAxis(100.0, 3.0, 0.0, 25.0), EPS);
        assertEquals(2.0, VelocityPolicy.applyAxis(4.0, 0.0, 50.0, 0.0), EPS);
    }

    private static void assertHorizontal(double expectedX, double expectedZ, VelocityStrafeMath.Horizontal actual) {
        assertEquals(expectedX, actual.x(), EPS);
        assertEquals(expectedZ, actual.z(), EPS);
    }
}
