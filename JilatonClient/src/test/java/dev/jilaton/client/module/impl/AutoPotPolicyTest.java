package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static dev.jilaton.client.module.impl.AutoPotPolicy.EffectStatus;
import static dev.jilaton.client.module.impl.AutoPotPolicy.EffectType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoPotPolicyTest {
    private static final Set<EffectType> SPEED_ENABLED = EnumSet.of(EffectType.SPEED);

    @Test
    void refreshesEnabledBuffWhenMissingOrNearExpiration() {
        assertTrue(AutoPotPolicy.shouldRefresh(true, null, 100));
        assertTrue(AutoPotPolicy.shouldRefresh(true, 100, 100));
        assertFalse(AutoPotPolicy.shouldRefresh(true, 101, 100));
        assertFalse(AutoPotPolicy.shouldRefresh(false, null, 100));
    }

    @Test
    void selectsOnlyMissingOrExpiringConfiguredEffects() {
        assertTrue(eligible(List.of(new EffectStatus(EffectType.SPEED, null)), false, false));
        assertTrue(eligible(List.of(new EffectStatus(EffectType.SPEED, 100)), false, false));
        assertFalse(eligible(List.of(new EffectStatus(EffectType.SPEED, 101)), false, false));
    }

    @Test
    void rejectsDisabledAndUnsupportedEffects() {
        assertFalse(eligible(List.of(new EffectStatus(EffectType.SPEED, null)), false, false, Set.of()));
        assertFalse(eligible(List.of(new EffectStatus(EffectType.UNSUPPORTED, null)), false, false));
    }

    @Test
    void healingRequiresItsToggleAndLowHealthAndCanBePrioritized() {
        List<EffectStatus> healing = List.of(new EffectStatus(EffectType.HEALING, null));
        assertFalse(eligible(healing, true, false));
        assertTrue(eligible(healing, true, true));
        assertFalse(AutoPotPolicy.isEligible(healing, false, false, true, SPEED_ENABLED, 100));
        assertFalse(AutoPotPolicy.isEligible(healing, false, true, false, SPEED_ENABLED, 100));
        assertTrue(AutoPotPolicy.isEligible(healing, true, true, true, SPEED_ENABLED, 100));
    }

    @Test
    void acceptsMixedPotionWhenOneConfiguredEffectNeedsRefresh() {
        List<EffectStatus> mixed = List.of(
                new EffectStatus(EffectType.SPEED, 200),
                new EffectStatus(EffectType.STRENGTH, null));
        Set<EffectType> enabled = EnumSet.of(EffectType.SPEED, EffectType.STRENGTH);

        assertTrue(AutoPotPolicy.isEligible(mixed, false, true, false, enabled, 100));
    }

    private static boolean eligible(List<EffectStatus> effects, boolean requireHealing, boolean healingNeeded) {
        return eligible(effects, requireHealing, healingNeeded, SPEED_ENABLED);
    }

    private static boolean eligible(List<EffectStatus> effects, boolean requireHealing,
                                    boolean healingNeeded, Set<EffectType> enabledBuffs) {
        return AutoPotPolicy.isEligible(effects, requireHealing, true, healingNeeded, enabledBuffs, 100);
    }
}
