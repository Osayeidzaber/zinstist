package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LongJumpBoostPolicyTest {
    @Test
    void scalesHorizontalVelocityAndPreservesVerticalMotion() {
        Vec3 boosted = LongJumpBoostPolicy.apply(new Vec3(0.4, 0.42, 0.8), 4.25);

        assertEquals(1.7, boosted.x, 1.0E-12);
        assertEquals(0.42, boosted.y, 1.0E-12);
        assertEquals(3.4, boosted.z, 1.0E-12);
    }

    @Test
    void ignoresOutOfRangeOrNonFiniteBoostValues() {
        Vec3 original = new Vec3(0.3, 0.5, -0.6);

        assertEquals(original, LongJumpBoostPolicy.apply(original, 0.5));
        assertEquals(original, LongJumpBoostPolicy.apply(original, Double.NaN));
    }
}
