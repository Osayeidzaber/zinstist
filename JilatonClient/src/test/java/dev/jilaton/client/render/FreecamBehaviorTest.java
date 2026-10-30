package dev.jilaton.client.render;

import dev.jilaton.client.module.impl.FreecamBodyInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FreecamBehaviorTest {
    private static final Input IDLE = new Input(false, false, false, false, false, false, false);

    @Test void detachedMovementUsesCameraYawAndIndependentVerticalSpeed() {
        FreecamMotion motion = new FreecamMotion();
        motion.reset(new Vec3(5, 8, 9), 0, 0);
        motion.step(1, 0, 1, 2, 3, false);
        assertEquals(new Vec3(5, 14, 11), motion.current());
        assertEquals(new Vec3(5, 11, 10), motion.position(0.5F));
        motion.reset(Vec3.ZERO, 90, 0);
        motion.step(1, 0, 0, 1, 1, false);
        assertEquals(-1, motion.current().x, 1e-9);
        assertEquals(0, motion.current().z, 1e-9);
    }

    @Test void smoothHalfStepPauseAndMouseBoundsMatchDetachedState() {
        FreecamMotion motion = new FreecamMotion();
        motion.reset(Vec3.ZERO, 0, 0);
        motion.step(0, 1, -1, 2, 1, true);
        assertEquals(new Vec3(1, -1, 0), motion.current());
        motion.pause();
        assertEquals(motion.current(), motion.position(0));
        motion.turn(20, 10000);
        assertEquals(3, motion.yaw());
        assertEquals(90, motion.pitch());
        motion.turn(Double.NaN, 1);
        assertEquals(3, motion.yaw());
    }

    @Test void scrollScalingIsBoundedAndRejectsNonfiniteWheelInput() {
        assertEquals(1.2, FreecamMotion.scrollSpeed(1, 1, 0.1, 50), 1e-9);
        assertEquals(1, FreecamMotion.scrollSpeed(1.2, -1, 0.1, 50), 1e-9);
        assertEquals(50, FreecamMotion.scrollSpeed(50, 1, 0.1, 50));
        assertEquals(0.1, FreecamMotion.scrollSpeed(0.1, -1, 0.1, 50));
        assertEquals(1, FreecamMotion.scrollSpeed(1, Double.NaN, 0.1, 50));
    }

    @Test void chunkFocusUsesFloorForNegativeCoordinatesAndCapsTheRadius() {
        assertEquals(new FreecamMotion.ChunkWindow(-1, -1, 12), FreecamMotion.chunkWindow(-0.1, -0.1, 0, 0, 12));
        assertEquals(new FreecamMotion.ChunkWindow(20, 0, 23), FreecamMotion.chunkWindow(640, 0, 0, 0, 12));
        assertEquals(32, FreecamMotion.chunkWindow(16000, 0, 0, 0, 12).radius());
    }

    @Test void cameraKeysDoNotMoveAnIdleBodyUnlessLiveBodyKeysAreEnabled() {
        FreecamBodyInput body = new FreecamBodyInput();
        Input forward = new Input(true, false, false, false, true, false, true);
        assertEquals(IDLE, body.apply(forward, false));
        assertEquals(forward, body.apply(forward, true));
        assertEquals(IDLE, body.apply(IDLE, true));
    }

    @Test void carriedBodyIntentPersistsAndOppositeLiveKeysReleaseIt() {
        FreecamBodyInput body = new FreecamBodyInput();
        Input forward = new Input(true, false, false, false, false, false, true);
        body.learn(forward);
        assertEquals(forward, body.apply(IDLE, false));
        Input backward = new Input(false, true, false, false, false, false, false);
        assertFalse(body.apply(backward, true).forward());
        assertTrue(body.apply(backward, true).backward());
        body.clear();
        assertEquals(IDLE, body.apply(IDLE, false));
    }
}
