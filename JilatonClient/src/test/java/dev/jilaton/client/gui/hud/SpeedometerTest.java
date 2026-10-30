package dev.jilaton.client.gui.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeedometerTest {
    @Test void firstSampleAndResetReadZero() {
        Speedometer speedometer = new Speedometer();
        assertEquals(0, speedometer.update(0, 0, 0, true), 1e-9);
        speedometer.reset();
        assertEquals(0, speedometer.update(5, 5, 2_000_000_000L, true), 1e-9);
        speedometer.reset();
        assertEquals(0, speedometer.update(9, 9, 3_000_000_000L, true), 1e-9);
    }

    @Test void oneBlockInHalfASecondIsTwoBlocksPerSecond() {
        Speedometer speedometer = new Speedometer();
        speedometer.update(0, 0, 0, true);
        assertEquals(2, speedometer.update(1, 0, 500_000_000L, true), 1e-9);
    }

    @Test void diagonalMovementUsesHorizontalDistance() {
        Speedometer speedometer = new Speedometer();
        speedometer.update(0, 0, 0, true);
        assertEquals(5, speedometer.update(3, 4, 1_000_000_000L, true), 1e-9);
    }

    @Test void smoothedReadingApproachesTheInstantaneousSpeed() {
        Speedometer speedometer = new Speedometer();
        speedometer.update(0, 0, 0, true);
        double smoothed = speedometer.update(1, 0, 500_000_000L, false);
        assertTrue(smoothed > 1.9 && smoothed < 2.0 + 1e-9, "smoothed reading was " + smoothed);
    }

    @Test void duplicateTimestampDoesNotConsumeTheSample() {
        Speedometer speedometer = new Speedometer();
        speedometer.update(0, 0, 0, true);
        assertEquals(1, speedometer.update(1, 0, 1_000_000_000L, true), 1e-9);
        assertEquals(1, speedometer.update(9, 0, 1_000_000_000L, true), 1e-9);
        assertEquals(1, speedometer.update(2, 0, 2_000_000_000L, true), 1e-9);
    }

    @Test void nonFiniteSamplesAreRejected() {
        Speedometer speedometer = new Speedometer();
        assertThrows(IllegalArgumentException.class,
                () -> speedometer.update(Double.NaN, 0, 0, true));
        assertThrows(IllegalArgumentException.class,
                () -> speedometer.update(0, Double.POSITIVE_INFINITY, 0, true));
    }
}
