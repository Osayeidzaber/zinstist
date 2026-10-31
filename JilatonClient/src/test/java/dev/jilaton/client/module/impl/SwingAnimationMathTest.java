package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwingAnimationMathTest {
    @Test
    void crestIsZeroAtBothEndsAndPeaksAtPeak() {
        for (float peak : new float[]{0.28f, 0.3f, 0.42f}) {
            assertEquals(0.0f, SwingAnimationsModule.crest(0.0f, peak), 1e-6);
            assertEquals(0.0f, SwingAnimationsModule.crest(1.0f, peak), 1e-6);
            assertEquals(1.0f, SwingAnimationsModule.crest(peak, peak), 1e-6);
        }
    }

    @Test
    void crestRisesThenFalls() {
        float previous = -1;
        for (int i = 0; i <= 30; i++) {
            float value = SwingAnimationsModule.crest(i / 100.0f, 0.3f);
            assertTrue(value >= previous, "crest must not dip on the rise");
            previous = value;
        }
        previous = 2;
        for (int i = 30; i <= 100; i++) {
            float value = SwingAnimationsModule.crest(i / 100.0f, 0.3f);
            assertTrue(value <= previous, "crest must not rise on the fall");
            previous = value;
        }
    }

    @Test
    void tempoScalesTheVanillaSwingDuration() {
        assertEquals(6, SwingAnimationsModule.scaledTicks(6, 1.0f, true));
        assertEquals(3, SwingAnimationsModule.scaledTicks(6, 2.0f, true));
        assertEquals(12, SwingAnimationsModule.scaledTicks(6, 0.5f, true));
        assertEquals(1, SwingAnimationsModule.scaledTicks(1, 2.5f, true));
        assertEquals(6, SwingAnimationsModule.scaledTicks(6, 2.0f, false), "unstyled swings keep vanilla timing");
    }

    @Test
    void legacyStyleNamesMapOntoAnubisStyles() {
        assertEquals(0, SwingAnimationsModule.styleIndex("Slash"));
        assertEquals(3, SwingAnimationsModule.styleIndex("Spin"));
        assertEquals(1, SwingAnimationsModule.styleIndex("Power"));
        assertEquals(1, SwingAnimationsModule.styleIndex("Inject"));
        assertEquals(4, SwingAnimationsModule.styleIndex("Tap"));
        assertEquals(5, SwingAnimationsModule.styleIndex("Dash"));
        assertEquals(0, SwingAnimationsModule.styleIndex("Smooth"));
        assertEquals(0, SwingAnimationsModule.styleIndex("unknown"));
    }
}
