package dev.jilaton.client.gui.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Headless checks for the 120 ms numeric crossfade and its reduced-motion contract. */
class NumericCrossfadeTest {
    private static final long START = 1_000_000_000L;

    @Test
    void firstValueAppearsWithoutAFade() {
        NumericCrossfade fade = new NumericCrossfade();
        assertTrue(fade.update("42", START, false));
        assertEquals("42", fade.current());
        assertEquals("", fade.previous());
        assertFalse(fade.fading());
        assertEquals(1.0, fade.currentOpacity(START));
        assertEquals(0.0, fade.previousOpacity(START));
    }

    @Test
    void valueChangeCrossfadesOver120Milliseconds() {
        NumericCrossfade fade = new NumericCrossfade();
        fade.update("42", START, false);
        assertTrue(fade.update("43", START, false));
        assertEquals("43", fade.current());
        assertEquals("42", fade.previous());
        assertTrue(fade.fading());

        assertEquals(0.0, fade.currentOpacity(START), 1.0E-9);
        assertEquals(1.0, fade.previousOpacity(START), 1.0E-9);

        double halfway = fade.currentOpacity(START + NumericCrossfade.DURATION_NANOS / 2);
        assertTrue(halfway > 0 && halfway < 1, "mid-fade opacity must be partial");
        assertEquals(1.0, halfway + fade.previousOpacity(START + NumericCrossfade.DURATION_NANOS / 2), 1.0E-9);

        assertFalse(fade.update("43", START + NumericCrossfade.DURATION_NANOS, false));
        assertEquals("", fade.previous(), "the old value is released once the fade ends");
        assertFalse(fade.fading());
        assertEquals(1.0, fade.currentOpacity(START + NumericCrossfade.DURATION_NANOS));
    }

    @Test
    void reducedMotionSwapsTheValueInstantly() {
        NumericCrossfade fade = new NumericCrossfade();
        fade.update("42", START, false);
        assertTrue(fade.update("43", START, true));
        assertEquals("43", fade.current());
        assertEquals("", fade.previous());
        assertFalse(fade.fading());
        assertEquals(1.0, fade.currentOpacity(START));
    }

    @Test
    void unchangedValueDoesNotRestartTheFade() {
        NumericCrossfade fade = new NumericCrossfade();
        fade.update("42", START, false);
        fade.update("43", START, false);
        assertFalse(fade.update("43", START + NumericCrossfade.DURATION_NANOS / 2, false));
        assertTrue(fade.fading(), "repeating the current value keeps the running fade");
        double opacity = fade.currentOpacity(START + NumericCrossfade.DURATION_NANOS / 2);
        assertTrue(opacity > 0 && opacity < 1);
    }

    @Test
    void resetClearsEveryValue() {
        NumericCrossfade fade = new NumericCrossfade();
        fade.update("42", START, false);
        fade.update("43", START, false);
        fade.reset();
        assertEquals("", fade.current());
        assertEquals("", fade.previous());
        assertFalse(fade.fading());
    }
}
