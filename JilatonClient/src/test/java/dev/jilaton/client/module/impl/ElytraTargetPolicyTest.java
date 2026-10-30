package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElytraTargetPolicyTest {
    @Test
    void candidateMustBeAliveVisibleInRangeAndInsideFov() {
        assertTrue(ElytraTargetPolicy.isEligible(true, true, 100.0, 10.0, 45.0, 90.0));
        assertFalse(ElytraTargetPolicy.isEligible(false, true, 25.0, 10.0, 0.0, 90.0));
        assertFalse(ElytraTargetPolicy.isEligible(true, false, 25.0, 10.0, 0.0, 90.0));
        assertFalse(ElytraTargetPolicy.isEligible(true, true, 101.0, 10.0, 0.0, 90.0));
        assertFalse(ElytraTargetPolicy.isEligible(true, true, 25.0, 10.0, 91.0, 90.0));
    }

    @Test
    void movementPredictionMatchesConfiguredLeadAndOptionalGravity() {
        Vec3 position = new Vec3(1.0, 4.0, 2.0);
        Vec3 velocity = new Vec3(0.5, 0.0, -1.0);

        assertEquals(new Vec3(2.0, 4.0, 0.0),
                ElytraTargetPolicy.predictPosition(position, velocity, true, 2.0, false));
        assertEquals(new Vec3(2.0, 3.9, 0.0),
                ElytraTargetPolicy.predictPosition(position, velocity, true, 2.0, true));
        assertEquals(position, ElytraTargetPolicy.predictPosition(position, velocity, false, 2.0, true));
    }
}
