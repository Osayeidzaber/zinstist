package dev.jilaton.client.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimatedValueTest {
    @Test
    void equalElapsedTimeProducesSameValueAtDifferentFrameRates() {
        AnimatedValue fast = new AnimatedValue();
        AnimatedValue slow = new AnimatedValue();
        fast.update(1, 0, false);
        slow.update(1, 0, false);
        for (int index = 1; index <= 100; index++) fast.update(0, index * 10_000_000L, false);
        for (int index = 1; index <= 20; index++) slow.update(0, index * 50_000_000L, false);
        assertEquals(fast.update(0, 1_000_000_000L, false), slow.update(0, 1_000_000_000L, false), 1e-12);
    }

    @Test
    void reducedMotionAndNewTargetSnapImmediately() {
        AnimatedValue value = new AnimatedValue();
        assertEquals(0.8, value.update(0.8, 0, false));
        assertEquals(0.2, value.update(0.2, 10_000, true));
        value.reset();
        assertEquals(1, value.update(1, 20_000, false));
        assertThrows(IllegalArgumentException.class, () -> value.update(Double.NaN, 30_000, false));
    }
}
