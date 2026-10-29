package dev.jilaton.client.rotation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        assertEquals(-179.0F, result.yaw(), 0.0001F);
    }

    @Test
    void turnSpeedBoundsYawAndPitchIndependentlyAndPitchStaysValid() {
        RotationAngles result = RotationPolicy.stepToward(0.0F, 88.0F,
                new RotationAngles(90.0F, -90.0F), 3.0F);

        assertEquals(3.0F, result.yaw(), 0.0001F);
        assertEquals(85.0F, result.pitch(), 0.0001F);
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
