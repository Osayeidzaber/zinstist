package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorScoringTest {
    @Test
    void ranksProtectionAndToughnessAgainstTheWholeLoadout() {
        double plain = ArmorScoring.score(6.0, 16.0, 0.0, 0.0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0);
        double tougher = ArmorScoring.score(6.0, 16.0, 8.0, 0.0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0);
        double protectedScore = ArmorScoring.score(6.0, 16.0, 8.0, 0.0,
                4, 0, 0, 0, 0, 0, 0, 0, 0, 1.0);

        assertTrue(tougher > plain);
        assertTrue(protectedScore > tougher);
    }

    @Test
    void favorsSituationalProtectionAndBetterConditionAsTieBreakers() {
        double plain = ArmorScoring.score(6.0, 20.0, 8.0, 0.0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0.2);
        double blastProtected = ArmorScoring.score(6.0, 20.0, 8.0, 0.0,
                0, 0, 0, 4, 0, 0, 0, 0, 0, 0.2);
        double durable = ArmorScoring.score(6.0, 20.0, 8.0, 0.0,
                0, 0, 0, 4, 0, 0, 0, 0, 0, 1.0);

        assertTrue(blastProtected > plain);
        assertTrue(durable > blastProtected);
    }

    @Test
    void capsDamageReductionAndClampsDurabilityInputs() {
        double capped = ArmorScoring.score(6.0, 80.0, 20.0, 1.0,
                30, 30, 30, 30, 0, 0, 0, 0, 0, 2.0);
        double low = ArmorScoring.score(6.0, 0.0, 0.0, 0.0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, -1.0);

        assertTrue(capped < 27.0);
        assertTrue(low >= 0.0);
    }
}
