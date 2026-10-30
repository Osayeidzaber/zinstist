package dev.jilaton.client.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClickRateTest {
    @Test
    void dropsClicksExactlyOneSecondOldAndKeepsNewerPresses() {
        ClickRate rate = new ClickRate();
        rate.press(10);
        rate.press(500_000_010);
        assertEquals(2, rate.count(1_000_000_009));
        assertEquals(1, rate.count(1_000_000_010));
        assertEquals(0, rate.count(1_500_000_010));
    }

    @Test
    void boundedHistoryWrapsAndResetDropsPreviousWorldClicks() {
        ClickRate rate = new ClickRate();
        for (int index = 0; index < 1000; index++) rate.press(index);
        assertEquals(256, rate.count(1000));
        rate.clear();
        assertEquals(0, rate.count(1000));
        rate.press(1100);
        assertEquals(1, rate.count(1100));
        assertEquals(0, rate.count(1_000_001_100));
    }
}
