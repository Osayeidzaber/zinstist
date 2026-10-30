package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickScheduleTest {
    @Test
    void steadyRateDistributesClicksAcrossTicksAndKeepsTheConfiguredAverage() {
        ClickSchedule schedule = new ClickSchedule(new Random(4));
        int clicks = 0;

        for (int tick = 0; tick < 40; tick++) {
            if (schedule.advance(6.0, 6.0, "Steady")) {
                clicks++;
            }
        }

        assertEquals(12, clicks);
    }

    @Test
    void fractionalRateCarriesAcrossOneSecondWindows() {
        ClickSchedule schedule = new ClickSchedule(new Random(9));
        int clicks = 0;

        for (int tick = 0; tick < 40; tick++) {
            if (schedule.advance(7.5, 7.5, "Steady")) {
                clicks++;
            }
        }

        assertEquals(15, clicks);
    }

    @Test
    void normalPatternKeepsEachWindowWithinConfiguredCpsBounds() {
        ClickSchedule schedule = new ClickSchedule(new Random(23));

        for (int second = 0; second < 20; second++) {
            int clicks = 0;
            for (int tick = 0; tick < 20; tick++) {
                if (schedule.advance(5.0, 11.0, "Normal distribution")) {
                    clicks++;
                }
            }
            assertTrue(clicks >= 5 && clicks <= 11, "scheduled " + clicks + " clicks in one second");
        }
    }

    @Test
    void changingRateStartsANewScheduleAndToggleResetClearsIt() {
        ClickSchedule schedule = new ClickSchedule(new Random(42));
        for (int tick = 0; tick < 7; tick++) {
            schedule.advance(4.0, 4.0, "Steady");
        }

        int clicks = 0;
        for (int tick = 0; tick < 20; tick++) {
            if (schedule.advance(10.0, 10.0, "Steady")) {
                clicks++;
            }
        }
        assertEquals(10, clicks);

        schedule.reset();
        clicks = 0;
        for (int tick = 0; tick < 20; tick++) {
            if (schedule.advance(3.0, 3.0, "Steady")) {
                clicks++;
            }
        }
        assertEquals(3, clicks);
    }
}
