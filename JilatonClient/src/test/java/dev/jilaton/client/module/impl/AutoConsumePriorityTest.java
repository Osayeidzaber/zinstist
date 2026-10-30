package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static dev.jilaton.client.module.impl.AutoPotPolicy.EffectStatus;
import static dev.jilaton.client.module.impl.AutoPotPolicy.EffectType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Priority between the automatic consumable modules is enforced by their module-level guards and
 * registration order, not by a shared arbitration object. These cases pin the per-item gates those
 * guards rely on: which items qualify at the same vitals, which potion contents may preempt, and
 * which per-item cooldowns must have elapsed.
 */
class AutoConsumePriorityTest {
    private static final float MAX_HEALTH = 20.0F;

    @Test
    void everyConsumerQualifiesAtTheSameVitals() {
        float health = 5.0F;

        assertTrue(AutoGapplePolicy.shouldUse(health, 0.0, 10.0, true));
        assertTrue(AutoSoupPolicy.shouldUse(health, MAX_HEALTH, 0.0F, 40.0, true, true, true, true));
        assertTrue(AutoHeadPolicy.shouldUse(health, MAX_HEALTH, 0.0F, 40.0, 1.0, true, true, true, true));
        assertTrue(AutoEatPolicy.shouldEat(20, health, 16, 10.0F));
    }

    @Test
    void noConsumerQualifiesAboveEveryThreshold() {
        float health = 12.0F;

        assertFalse(AutoGapplePolicy.shouldUse(health, 0.0, 10.0, true));
        assertFalse(AutoSoupPolicy.shouldUse(health, MAX_HEALTH, 0.0F, 40.0, true, true, true, true));
        assertFalse(AutoHeadPolicy.shouldUse(health, MAX_HEALTH, 0.0F, 40.0, 1.0, true, true, true, true));
        assertFalse(AutoEatPolicy.shouldEat(20, health, 16, 10.0F));
    }

    @Test
    void potionsOnlyPreemptForFullySupportedContents() {
        Set<EffectType> buffs = EnumSet.of(EffectType.SPEED);
        List<EffectStatus> healingOnly = List.of(new EffectStatus(EffectType.HEALING, null));
        List<EffectStatus> healingAndUnknown = List.of(
                new EffectStatus(EffectType.HEALING, null),
                new EffectStatus(EffectType.UNSUPPORTED, null));

        assertTrue(AutoPotPolicy.isEligible(healingOnly, false, true, true, buffs, 100));
        assertFalse(AutoPotPolicy.isEligible(healingOnly, false, false, true, buffs, 100));
        assertFalse(AutoPotPolicy.isEligible(healingAndUnknown, false, true, true, buffs, 100));
    }

    @Test
    void perItemCooldownsBlockUseUntilTheyElapse() {
        assertFalse(AutoSoupPolicy.shouldUse(4.0F, MAX_HEALTH, 0.0F, 40.0, true, true, true, false));
        assertTrue(AutoSoupPolicy.shouldUse(4.0F, MAX_HEALTH, 0.0F, 40.0, true, true, true, true));
        assertFalse(AutoHeadPolicy.shouldUse(4.0F, MAX_HEALTH, 0.0F, 40.0, 1.0, true, true, true, false));
        assertTrue(AutoHeadPolicy.shouldUse(4.0F, MAX_HEALTH, 0.0F, 40.0, 1.0, true, true, true, true));
    }
}
