package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalDamageEstimatorTest {
    @Test
    void fullExposureAtCrystalExplosionDiameterMatchesTheVanillaFalloffFormula() {
        assertEquals(85.0F, CrystalDamageEstimator.rawDamage(1.0, 12.0), 0.0001F);
    }

    @Test
    void rawDamageIsMonotonicWithinTheImpactRange() {
        float near = CrystalDamageEstimator.rawDamage(0.75, 12.0);
        float far = CrystalDamageEstimator.rawDamage(0.25, 12.0);

        assertTrue(near > far);
    }

    @Test
    void rawDamageClampsInvalidImpactValues() {
        assertEquals(CrystalDamageEstimator.rawDamage(0.0, 12.0),
                CrystalDamageEstimator.rawDamage(-1.0, 12.0));
        assertEquals(CrystalDamageEstimator.rawDamage(1.0, 12.0),
                CrystalDamageEstimator.rawDamage(2.0, 12.0));
    }

    @Test
    void rawDamageUsesAnchorPowerFiveDiameter() {
        assertEquals(71.0F, CrystalDamageEstimator.rawDamage(1.0, 10.0), 0.0001F);
    }

    @Test
    void anchorExposureRayStartsBeyondTheIgnoredSourceBlock() {
        var center = new net.minecraft.world.phys.Vec3(0.5, 0.5, 0.5);
        var sample = new net.minecraft.world.phys.Vec3(2.5, 0.5, 0.5);
        var start = CrystalDamageEstimator.sourceBlockTraceStart(center, sample,
                new net.minecraft.core.BlockPos(0, 0, 0));

        assertEquals(1.001, start.x, 0.00001);
        assertEquals(center.y, start.y, 0.00001);
        assertEquals(center.z, start.z, 0.00001);
        assertEquals(center, CrystalDamageEstimator.sourceBlockTraceStart(center, sample, null));
    }
}
