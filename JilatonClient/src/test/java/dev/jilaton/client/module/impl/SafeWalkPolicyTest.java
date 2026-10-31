package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeWalkPolicyTest {
    @Test
    void sneaksOnlyForUnsupportedGroundedMovement() {
        assertTrue(SafeWalkPolicy.shouldSneak(true, false, true, false, true));
        assertFalse(SafeWalkPolicy.shouldSneak(true, false, true, false, false));
    }

    @Test
    void preservesManualSneakAndExemptOrStationaryStates() {
        assertFalse(SafeWalkPolicy.shouldSneak(false, false, true, false, true));
        assertFalse(SafeWalkPolicy.shouldSneak(true, true, true, false, true));
        assertFalse(SafeWalkPolicy.shouldSneak(true, false, false, false, true));
        assertFalse(SafeWalkPolicy.shouldSneak(true, false, true, true, true));
    }

    @Test
    void aheadDirectionUsesVanillaLeftPositiveAxes() {
        assertDirection(0.0F, 1.0F, SafeWalkPolicy.aheadDirection(0.0F, 1.0, 0.0));
        assertDirection(1.0F, 0.0F, SafeWalkPolicy.aheadDirection(0.0F, 0.0, 1.0));
        assertDirection(-1.0F, 0.0F, SafeWalkPolicy.aheadDirection(0.0F, 0.0, -1.0));
        assertDirection(0.0F, 1.0F, SafeWalkPolicy.aheadDirection(90.0F, 0.0, 1.0));
        assertDirection(0.0F, -1.0F, SafeWalkPolicy.aheadDirection(90.0F, 0.0, -1.0));
        assertDirection(-1.0F, 0.0F, SafeWalkPolicy.aheadDirection(90.0F, 1.0, 0.0));
        assertDirection(1.0F, 0.0F, SafeWalkPolicy.aheadDirection(270.0F, 1.0, 0.0));
        assertDirection(0.70710677F, 0.70710677F, SafeWalkPolicy.aheadDirection(0.0F, 1.0, 1.0));
    }

    @Test
    void aheadDirectionWrapsYawAndRejectsDegenerateInput() {
        Vec2 atPositive = SafeWalkPolicy.aheadDirection(180.0F, 1.0, 1.0);
        Vec2 atNegative = SafeWalkPolicy.aheadDirection(-180.0F, 1.0, 1.0);
        assertEquals(atPositive.x, atNegative.x, 1.0E-5F);
        assertEquals(atPositive.y, atNegative.y, 1.0E-5F);

        Vec2 justBelow = SafeWalkPolicy.aheadDirection(179.9F, 1.0, 1.0);
        Vec2 justAbove = SafeWalkPolicy.aheadDirection(-180.1F, 1.0, 1.0);
        assertEquals(justBelow.x, justAbove.x, 1.0E-3F);
        assertEquals(justBelow.y, justAbove.y, 1.0E-3F);

        assertEquals(Vec2.ZERO, SafeWalkPolicy.aheadDirection(0.0F, 0.0, 0.0));
        assertEquals(Vec2.ZERO, SafeWalkPolicy.aheadDirection(0.0F, 1.0E-7, 0.0));
        assertEquals(Vec2.ZERO, SafeWalkPolicy.aheadDirection(Float.NaN, 1.0, 0.0));
        assertEquals(Vec2.ZERO, SafeWalkPolicy.aheadDirection(0.0F, Double.NaN, 0.0));
    }

    private static void assertDirection(float expectedX, float expectedY, Vec2 actual) {
        assertEquals(expectedX, actual.x, 1.0E-5F);
        assertEquals(expectedY, actual.y, 1.0E-5F);
    }
}
