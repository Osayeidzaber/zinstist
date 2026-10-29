package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BowAimMathTest {
    private static Optional<BowAimMath.Solution> solve(double distance, double targetVelocityX,
                                                        boolean highArc) {
        return BowAimMath.solve(0.0, 1.6, 0.0, 0.0, 1.6, distance,
                targetVelocityX, 0.0, 0.0, 3.0, 0.05, 0.99, true, highArc);
    }

    @Test
    void solvesAStaticTargetWithDragAndGravity() {
        BowAimMath.Solution solution = solve(20.0, 0.0, false).orElseThrow();

        assertEquals(0.0, solution.yaw(), 0.001);
        assertTrue(solution.pitch() < 0.0F);
        assertTrue(solution.flightTicks() > 0.0);
        assertTrue(solution.speedResidual() <= 0.03);
    }

    @Test
    void leadsMovingTargets() {
        BowAimMath.Solution still = solve(20.0, 0.0, false).orElseThrow();
        BowAimMath.Solution moving = solve(20.0, 0.2, false).orElseThrow();

        assertTrue(moving.yaw() < still.yaw());
        assertTrue(moving.speedResidual() <= 0.03);
    }

    @Test
    void rejectsImpossibleAndDegenerateTrajectories() {
        assertFalse(solve(1000.0, 0.0, false).isPresent());
        assertFalse(BowAimMath.solve(0, 0, 0, 0, 1, 0, 0, 0, 0,
                3, 0.05, 0.99, true, false).isPresent());
        assertFalse(BowAimMath.solve(0, 0, 0, 0, 1, 20, 0, 0, 0,
                Double.NaN, 0.05, 0.99, true, false).isPresent());
    }

    @Test
    void highArcPrefersTheLaterInterceptWhenBothExist() {
        BowAimMath.Solution low = solve(80.0, 0.0, false).orElseThrow();
        BowAimMath.Solution high = solve(80.0, 0.0, true).orElseThrow();

        assertTrue(high.flightTicks() > low.flightTicks());
        assertTrue(high.pitch() < low.pitch());
        assertTrue(high.speedResidual() <= 0.03);
    }
}
