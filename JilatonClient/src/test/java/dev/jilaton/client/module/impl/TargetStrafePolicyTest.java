package dev.jilaton.client.module.impl;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetStrafePolicyTest {
    @Test
    void jumpEdgeFlipsTheOrbitOnceAndAHeldJumpDoesNotRepeat() {
        int direction = TargetStrafePolicy.directionAfterInput(1, true, false, false, false);
        assertEquals(-1, direction);

        direction = TargetStrafePolicy.directionAfterInput(direction, true, true, false, false);
        assertEquals(-1, direction);

        direction = TargetStrafePolicy.directionAfterInput(direction, false, true, false, false);
        direction = TargetStrafePolicy.directionAfterInput(direction, true, false, false, false);
        assertEquals(1, direction);
    }

    @Test
    void heldSideKeysPinTheOrbitAfterTheJumpFlip() {
        assertEquals(-1, TargetStrafePolicy.directionAfterInput(1, true, false, true, false));
        assertEquals(1, TargetStrafePolicy.directionAfterInput(-1, true, false, false, true));
        assertEquals(-1, TargetStrafePolicy.directionAfterInput(1, false, false, true, false));
        assertEquals(1, TargetStrafePolicy.directionAfterInput(-1, false, false, false, true));
        assertEquals(-1, TargetStrafePolicy.directionAfterInput(-1, false, false, false, false));
    }

    @Test
    void eligibilityHonoursEachOptionalRequirement() {
        assertTrue(TargetStrafePolicy.eligible(true, false, false, false, false, false, false));
        assertTrue(TargetStrafePolicy.eligible(true, false, true, true, true, true, false));
        assertFalse(TargetStrafePolicy.eligible(false, false, false, false, false, false, false));
        assertFalse(TargetStrafePolicy.eligible(true, true, false, false, false, false, false));
        assertFalse(TargetStrafePolicy.eligible(true, false, true, false, false, false, false));
        assertFalse(TargetStrafePolicy.eligible(true, false, false, false, true, false, false));
        assertTrue(TargetStrafePolicy.eligible(true, false, true, true, false, false, false));
        assertFalse(TargetStrafePolicy.eligible(true, false, false, false, false, false, true));
    }

    @Test
    void normalPlayWithNoScreenIsEligibleAndAnOpenScreenIsNot() {
        // The module passes client.screen straight through; a null screen is ordinary gameplay.
        assertFalse(TargetStrafePolicy.screenOpen(null));
        assertTrue(TargetStrafePolicy.screenOpen(new Object()));
        assertTrue(TargetStrafePolicy.eligible(true, TargetStrafePolicy.screenOpen(null), false, false, false, false, false));
        assertFalse(TargetStrafePolicy.eligible(true, TargetStrafePolicy.screenOpen(new Object()), false, false, false, false, false));
    }

    @Test
    void orbitNeedsATargetInsideTheFollowRangeButNotOnTopOfThePlayer() {
        assertFalse(TargetStrafePolicy.canOrbit(0.0, 4.0));
        assertFalse(TargetStrafePolicy.canOrbit(1.0E-6, 4.0));
        assertTrue(TargetStrafePolicy.canOrbit(1.0E-5, 4.0));
        assertTrue(TargetStrafePolicy.canOrbit(4.0, 4.0));
        assertFalse(TargetStrafePolicy.canOrbit(4.0 + 1.0E-6, 4.0));
        assertTrue(TargetStrafePolicy.inFollowRange(0.0, 4.0));
        assertTrue(TargetStrafePolicy.inFollowRange(4.0, 4.0));
        assertFalse(TargetStrafePolicy.inFollowRange(4.1, 4.0));
    }

    @Test
    void degenerateLocalInputIsNotWrittenBack() {
        assertFalse(TargetStrafePolicy.meaningfulInput(Vec2.ZERO));
        assertFalse(TargetStrafePolicy.meaningfulInput(new Vec2(0.0005F, 0.0F)));
        assertTrue(TargetStrafePolicy.meaningfulInput(new Vec2(0.002F, 0.0F)));
        assertTrue(TargetStrafePolicy.meaningfulInput(new Vec2(0.001F, 0.001F)));
    }

    @Test
    void movementInputIgnoresJumpSneakAndSprintKeys() {
        assertTrue(TargetStrafePolicy.hasMovementInput(new Input(false, false, false, true, false, false, false)));
        assertFalse(TargetStrafePolicy.hasMovementInput(new Input(false, false, false, false, true, true, true)));
    }
}
