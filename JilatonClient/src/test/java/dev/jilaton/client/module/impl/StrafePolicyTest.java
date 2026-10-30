package dev.jilaton.client.module.impl;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrafePolicyTest {
    @Test
    void samplesKeysWithVanillaLeftPositiveAxes() {
        assertEquals(new StrafePolicy.Sampling(1.0, 0.0), StrafePolicy.sample(keys(true, false, false, false), null));
        assertEquals(new StrafePolicy.Sampling(0.0, 1.0), StrafePolicy.sample(keys(false, false, true, false), null));
        assertEquals(new StrafePolicy.Sampling(0.0, -1.0), StrafePolicy.sample(keys(false, false, false, true), null));
        assertEquals(new StrafePolicy.Sampling(1.0, 1.0), StrafePolicy.sample(keys(true, false, true, false), null));
        assertEquals(new StrafePolicy.Sampling(1.0, -1.0), StrafePolicy.sample(keys(true, false, false, true), null));
        assertEquals(new StrafePolicy.Sampling(0.0, 0.0), StrafePolicy.sample(keys(true, true, true, true), null));
    }

    @Test
    void overrideVectorWinsOverKeysAndKeepsVanillaAxisOrder() {
        StrafePolicy.Sampling sampling = StrafePolicy.sample(keys(true, false, false, false), new Vec2(0.5F, -1.0F));

        assertEquals(-1.0, sampling.forward());
        assertEquals(0.5, sampling.sideways());
    }

    @Test
    void eligibilityRejectsEveryStateWhereVanillaAlreadyOwnsMovement() {
        assertTrue(StrafePolicy.shouldApply(true, false, false, false, false, false, false));
        assertFalse(StrafePolicy.shouldApply(false, false, false, false, false, false, false));
        assertFalse(StrafePolicy.shouldApply(true, true, false, false, false, false, false));
        assertFalse(StrafePolicy.shouldApply(true, false, true, false, false, false, false));
        assertFalse(StrafePolicy.shouldApply(true, false, false, true, false, false, false));
        assertFalse(StrafePolicy.shouldApply(true, false, false, false, true, false, false));
        assertFalse(StrafePolicy.shouldApply(true, false, false, false, false, true, false));
        assertFalse(StrafePolicy.shouldApply(true, false, false, false, false, false, true));
    }

    @Test
    void strengthFollowsTheGroundState() {
        assertEquals(0.25, StrafePolicy.strength(true, 0.25, 0.75));
        assertEquals(0.75, StrafePolicy.strength(false, 0.25, 0.75));
    }

    private static Input keys(boolean forward, boolean backward, boolean left, boolean right) {
        return new Input(forward, backward, left, right, false, false, false);
    }
}
