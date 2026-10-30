package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatAngleWrapTest {
    private static final float EPSILON = 1.0E-4F;

    @Test
    void killAuraMeasuresTwoDegreesAcrossTheSeamInsteadOfThreeHundredFiftyEight() {
        assertEquals(-2.0F, KillAuraTargeting.yawDelta(-179.0F, 179.0F), EPSILON);
        assertEquals(2.0F, KillAuraTargeting.yawDelta(179.0F, -179.0F), EPSILON);
        assertEquals(-10.0F, KillAuraTargeting.yawDelta(0.0F, 350.0F), EPSILON);
        assertEquals(10.0F, KillAuraTargeting.yawDelta(350.0F, 0.0F), EPSILON);
    }

    @Test
    void anchorAuraSeamErrorStaysInsideTheAimReadyThreshold() {
        assertEquals(2.0F, AnchorAuraScoring.yawError(-179.0F, 179.0F), EPSILON);
        assertEquals(2.0F, AnchorAuraScoring.yawError(179.0F, -179.0F), EPSILON);
        assertEquals(10.0F, AnchorAuraScoring.yawError(0.0F, 350.0F), EPSILON);
        assertTrue(AnchorAuraScoring.yawError(179.9F, -179.9F) < 0.6F);
    }

    @Test
    void bowAimTakesTheShortestPathAtTheSignedBoundary() {
        assertEquals(-2.0F, BowAimMath.yawDelta(-179.0F, 179.0F), EPSILON);
        assertEquals(2.0F, BowAimMath.yawDelta(179.0F, -179.0F), EPSILON);
        assertEquals(0.0F, BowAimMath.yawDelta(-180.0F, 180.0F), EPSILON);
    }

    @Test
    void autoRodAimErrorIsTwoDegreesAcrossTheSeam() {
        assertEquals(2.0F, AutoRodPolicy.aimError(-179.0F, 0.0F, 179.0F, 0.0F), EPSILON);
        assertEquals(2.0F, AutoRodPolicy.aimError(179.0F, 0.0F, -179.0F, 0.0F), EPSILON);
        assertEquals(10.0F, AutoRodPolicy.aimError(0.0F, 0.0F, 350.0F, 0.0F), EPSILON);
    }

    @Test
    void aimAssistBlendsAcrossTheSeamAlongTheShortArc() {
        AimAssistPolicy.Angles result = AimAssistPolicy.apply(179.0F, 0.0F, -179.0F, 0.0F,
                0.25, true, false);

        assertEquals(179.5F, result.yaw(), EPSILON);
    }

    @Test
    void plainSubtractionWouldDisagreeAtEverySeamCase() {
        assertEquals(358.0F, 179.0F - (-179.0F), EPSILON);
        assertEquals(2.0F, Math.abs(KillAuraTargeting.yawDelta(-179.0F, 179.0F)), EPSILON);
        assertEquals(2.0F, Math.abs(AnchorAuraScoring.yawError(-179.0F, 179.0F)), EPSILON);
        assertEquals(2.0F, Math.abs(BowAimMath.yawDelta(-179.0F, 179.0F)), EPSILON);
        assertEquals(2.0F, AutoRodPolicy.aimError(-179.0F, 0.0F, 179.0F, 0.0F), EPSILON);
    }
}
