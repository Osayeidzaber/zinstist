package dev.jilaton.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The title-screen watermark is drawn from pure position functions, so its drift, its freeze under
 * reduced motion and the "keep the centre column clear" rule can all be checked without a client.
 */
class JilatonBackdropTest {
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    @Test
    void clockStopsUnderReducedMotion() {
        assertEquals(0, JilatonBackdrop.clock(true), 1e-12);
        assertTrue(JilatonBackdrop.clock(false) >= 0);
    }

    @Test
    void bandsDriftOverTimeButAreStillWhenTheClockIs() {
        double frozen = JilatonBackdrop.bandX(1, WIDTH, 0, 0, 400);
        assertEquals(frozen, JilatonBackdrop.bandX(1, WIDTH, 0, 0, 400), 1e-12);
        assertNotEquals(frozen, JilatonBackdrop.bandX(1, WIDTH, 4.5, 0, 400), 1e-9);
    }

    @Test
    void bandsStayAtTheEdgesAndInsideTheirSwing() {
        for (int band = 0; band < 6; band++) {
            boolean left = (band & 1) == 0;
            for (int y = 0; y <= HEIGHT; y += 60) {
                for (double time = 0; time < 8; time += 0.7) {
                    double x = JilatonBackdrop.bandX(band, WIDTH, time, 0, y);
                    assertTrue(x >= 0 && x <= WIDTH, "band " + band + " left the screen: " + x);
                    assertTrue(left ? x < WIDTH * 0.30 : x > WIDTH * 0.70,
                            "band " + band + " drifted into the centre column: " + x);
                }
            }
        }
    }

    @Test
    void starsKeepTheCentreColumnClear() {
        for (int index = 0; index < 9; index++) {
            double x = JilatonBackdrop.starX(index, WIDTH);
            assertTrue(Math.abs(x - WIDTH / 2.0) > WIDTH * 0.25, "star " + index + " sits in the centre: " + x);
            int radius = JilatonBackdrop.starRadius(index);
            assertTrue(radius >= 3 && radius <= 6, "star radius " + radius);
        }
    }

    @Test
    void starsDriftUpwardWithoutLeavingTheScreen() {
        int moved = 0;
        for (int index = 0; index < 9; index++) {
            if (JilatonBackdrop.starY(index, HEIGHT, 0) != JilatonBackdrop.starY(index, HEIGHT, 3)) moved++;
            for (double time = 0; time < 20; time += 0.5) {
                double y = JilatonBackdrop.starY(index, HEIGHT, time);
                assertTrue(y >= 0 && y < HEIGHT, "star " + index + " left the screen: " + y);
            }
        }
        assertTrue(moved >= 7, "stars must drift with the clock, only " + moved + " of 9 moved");
        assertEquals(JilatonBackdrop.starY(3, HEIGHT, 2), JilatonBackdrop.starY(3, HEIGHT, 2), 1e-12);
    }
}
