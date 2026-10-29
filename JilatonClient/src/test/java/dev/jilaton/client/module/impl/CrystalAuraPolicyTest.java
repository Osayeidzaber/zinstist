package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalAuraPolicyTest {
    @Test
    void facePlaceActivatesForLowHealthArmorOrHoleAndCanBeDisabled() {
        assertTrue(CrystalAuraPolicy.shouldFacePlace(true, 8.0F, 10.0F, false, false));
        assertTrue(CrystalAuraPolicy.shouldFacePlace(true, 20.0F, 10.0F, true, false));
        assertTrue(CrystalAuraPolicy.shouldFacePlace(true, 20.0F, 10.0F, false, true));
        assertFalse(CrystalAuraPolicy.shouldFacePlace(false, 5.0F, 10.0F, true, true));
        assertFalse(CrystalAuraPolicy.shouldFacePlace(true, 20.0F, 10.0F, false, false));
    }

    @Test
    void breakThresholdUsesAnubisSixtyPercentFactor() {
        assertEquals(2.4F, CrystalAuraPolicy.minimumDamage(4.0F, 2.0F, false, true), 0.0001F);
        assertEquals(1.2F, CrystalAuraPolicy.minimumDamage(4.0F, 2.0F, true, true), 0.0001F);
        assertEquals(4.0F, CrystalAuraPolicy.minimumDamage(4.0F, 2.0F, false, false));
    }
}
