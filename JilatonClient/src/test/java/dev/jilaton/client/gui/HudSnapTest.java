package dev.jilaton.client.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudSnapTest {
    @Test
    void snapsCentreToTheScreenCentre() {
        var r = HudSnap.snap(new HudSnap.Box(196, 50, 100, 20), List.of(), 500, 300);
        assertEquals(200, r.x(), 1e-9);
        assertEquals(250, r.guideX(), 1e-9);
    }

    @Test
    void snapsEdgeToAnotherWidget() {
        var other = new HudSnap.Box(10, 100, 80, 30);
        var r = HudSnap.snap(new HudSnap.Box(300, 133, 60, 20), List.of(other), 1000, 1000);
        assertEquals(130, r.y(), 1e-9);
        assertEquals(130, r.guideY(), 1e-9);
    }

    @Test
    void leavesFarPositionsAlone() {
        var r = HudSnap.snap(new HudSnap.Box(333, 222, 40, 10), List.of(), 1000, 1000);
        assertEquals(333, r.x(), 1e-9);
        assertTrue(Double.isNaN(r.guideX()));
    }
}
