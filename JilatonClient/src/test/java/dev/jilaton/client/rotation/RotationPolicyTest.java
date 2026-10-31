package dev.jilaton.client.rotation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RotationPolicyTest {
    @Test
    void higherPriorityRequestWinsAndEqualPriorityIsStable() {
        RotationRequest first = request("first", 4, 10.0F, 0.0F, 2.0F);
        RotationRequest tied = request("tied", 4, 20.0F, 0.0F, 2.0F);
        RotationRequest combat = request("combat", 10, 30.0F, 0.0F, 2.0F);

        assertEquals(combat, RotationPolicy.select(List.of(first, tied, combat)));
        assertEquals(first, RotationPolicy.select(List.of(first, tied)));
        assertNull(RotationPolicy.select(List.of()));
    }

    @Test
    void turnTakesShortestYawPathAcrossWrapBoundary() {
        RotationAngles result = RotationPolicy.stepToward(179.0F, 0.0F,
                new RotationAngles(-179.0F, 0.0F), 5.0F);

        // Moves forward across +-180 (the 2 degree way), not backward through 0 (the 358 degree way).
        assertEquals(2.0F, Math.abs(RotationPolicy.wrapDegrees(result.yaw() - 179.0F)) * 2, 0.0001F);
        assertEquals(1.0F, RotationPolicy.angularError(result.yaw(), 0.0F, new RotationAngles(-179.0F, 0.0F)), 0.0001F);
    }

    @Test
    void turnFollowsAStraightPathCappedByTurnSpeed() {
        RotationAngles result = RotationPolicy.stepToward(0.0F, 0.0F, new RotationAngles(30.0F, 40.0F), 5.0F);

        assertEquals(5.0F, Math.hypot(result.yaw(), result.pitch()), 0.001F);
        assertEquals(30.0F / 40.0F, result.yaw() / result.pitch(), 0.001F);
    }

    @Test
    void turnEasesOutNearTheTargetAndPitchStaysValid() {
        RotationAngles near = RotationPolicy.stepToward(0.0F, 0.0F, new RotationAngles(4.0F, 0.0F), 30.0F);
        assertEquals(2.0F, near.yaw(), 0.001F);
        RotationAngles done = RotationPolicy.stepToward(0.0F, 0.0F, new RotationAngles(0.3F, 0.0F), 30.0F);
        assertEquals(0.3F, done.yaw(), 0.0001F);
        RotationAngles pitch = RotationPolicy.stepToward(0.0F, 88.0F, new RotationAngles(0.0F, 120.0F), 30.0F);
        assertTrue(pitch.pitch() <= 90.0F);
    }

    @Test
    void invalidRequestsAndZeroLookDirectionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RotationRequest("", 1, new RotationAngles(0.0F, 0.0F), 1.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new RotationAngles(0.0F, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> RotationAngles.lookingAt(0.0, 0.0, 0.0));
    }

    private static RotationRequest request(String owner, int priority, float yaw, float pitch, float speed) {
        return new RotationRequest(owner, priority, new RotationAngles(yaw, pitch), speed);
    }
}
