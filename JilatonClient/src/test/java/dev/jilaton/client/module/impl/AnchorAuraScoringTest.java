package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorAuraScoringTest {
    @Test
    void damageValueRanksTotemPopBelowLethalButAboveOrdinaryDamage() {
        float ordinary = AnchorAuraScoring.value(8.0F, 20.0F, 0.0F, false);
        float pop = AnchorAuraScoring.value(20.0F, 20.0F, 0.0F, true);
        float lethal = AnchorAuraScoring.value(20.0F, 20.0F, 0.0F, false);

        assertEquals(8.0F, ordinary);
        assertEquals(520.0F, pop);
        assertEquals(1020.0F, lethal);
    }

    @Test
    void absorptionIsIncludedWhenRankingLethalBlasts() {
        assertEquals(19.0F, AnchorAuraScoring.value(19.0F, 18.0F, 2.0F, false));
        assertEquals(1020.0F, AnchorAuraScoring.value(24.0F, 18.0F, 2.0F, false));
    }

    @Test
    void blastScorePenalizesSelfDamageAndRejectsInvalidInputs() {
        assertEquals(17.0F, AnchorAuraScoring.blastScore(20.0F, 6.0F));
        assertTrue(Float.isInfinite(AnchorAuraScoring.blastScore(20.0F, Float.NaN)));
        assertTrue(Float.isInfinite(AnchorAuraScoring.value(Float.NaN, 20.0F, 0.0F, false)));
    }
}
