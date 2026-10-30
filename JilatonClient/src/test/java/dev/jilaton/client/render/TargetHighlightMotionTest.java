package dev.jilaton.client.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TargetHighlightMotionTest {
    private final TargetHighlightMotion.Target target = new TargetHighlightMotion.Target(1, 2, 3, 4, 1.8, 0.5);

    @Test void acquisitionAndLossFadeWithoutRetainingAnEntity() {
        TargetHighlightMotion motion = new TargetHighlightMotion();
        assertEquals(0, motion.update(target, 0, false).opacity());
        var acquired = motion.update(target, 100_000_000, false);
        assertTrue(acquired.opacity() > 0.5 && acquired.opacity() < 1);
        var lost = motion.update(null, 200_000_000, false);
        assertTrue(lost.opacity() > 0 && lost.opacity() < acquired.opacity());
        assertEquals(target, lost.target());
        for (int index = 3; index < 10; index++) motion.update(null, index * 100_000_000L, false);
        assertNull(motion.update(null, 1_000_000_000, false));
    }

    @Test void smoothTrackingStaysBetweenPreviousAndCurrentAndSwitchesDoNotSweepAcrossEntities() {
        TargetHighlightMotion motion = new TargetHighlightMotion();
        motion.update(target, 0, false);
        var moved = motion.update(new TargetHighlightMotion.Target(1, 3, 3, 4, 1.8, 0.5), 50_000_000, false);
        assertTrue(moved.target().x() > 2 && moved.target().x() < 3);
        var switched = motion.update(new TargetHighlightMotion.Target(2, 8, 3, 4, 1.8, 0.5), 100_000_000, false);
        assertEquals(8, switched.target().x());
        assertTrue(switched.opacity() < moved.opacity());
    }

    @Test void reducedMotionIsStationaryAndDisappearsImmediatelyOnLoss() {
        TargetHighlightMotion motion = new TargetHighlightMotion();
        var first = motion.update(target, 0, true);
        var later = motion.update(target, 5_000_000_000L, true);
        assertEquals(1, first.opacity());
        assertEquals(first.scan(), later.scan());
        assertEquals(0, later.phase());
        assertEquals(0, later.direction());
        assertNull(motion.update(null, 6_000_000_000L, true));
    }

    @Test void opacityDependsOnElapsedTimeRatherThanFrameCount() {
        TargetHighlightMotion fast = new TargetHighlightMotion(), slow = new TargetHighlightMotion();
        fast.update(target, 0, false); slow.update(target, 0, false);
        for (int index = 1; index <= 50; index++) fast.update(target, index * 10_000_000L, false);
        for (int index = 1; index <= 10; index++) slow.update(target, index * 50_000_000L, false);
        assertEquals(fast.update(target, 500_000_000, false).opacity(), slow.update(target, 500_000_000, false).opacity(), 1e-9);
        fast.reset();
        assertNull(fast.update(null, 600_000_000, false));
    }
}
