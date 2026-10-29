package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HitboxMarginTest {
    @Test
    void disabledOrIneligibleEntitiesKeepTheVanillaMargin() {
        assertEquals(0.0F, HitboxMargin.apply(0.0F, false, true, 0.5));
        assertEquals(0.0F, HitboxMargin.apply(0.0F, true, false, 0.5));
    }

    @Test
    void eligibleEntitiesUseTheConfiguredMargin() {
        assertEquals(0.1F, HitboxMargin.apply(0.0F, true, true, 0.1));
    }

    @Test
    void configuredMarginIsClampedAndNonFiniteValuesPreserveVanilla() {
        assertEquals(1.0F, HitboxMargin.apply(0.0F, true, true, 2.0));
        assertEquals(0.0F, HitboxMargin.apply(0.0F, true, true, -1.0));
        assertEquals(0.15F, HitboxMargin.apply(0.15F, true, true, Double.NaN));
    }
}
