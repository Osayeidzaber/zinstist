package dev.jilaton.client.module.impl;

import net.minecraft.world.item.component.AttackRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ReachMathTest {
    /** The largest bonus each Reach slider offers, derived from the protocol ceilings (vanilla 4.5 block reach). */
    private static final double ENTITY_SLIDER_MAX = ReachMath.ENTITY_BONUS_LIMIT;
    private static final double BLOCK_SLIDER_MAX = ReachMath.BLOCK_INTERACTION_LIMIT - 4.5;

    @Test
    void interactionRangeBonusRequiresTheEnabledLocalPlayer() {
        assertEquals(4.0, ReachMath.addBonus(3.0, true, 1.0, ENTITY_SLIDER_MAX));
        assertEquals(3.0, ReachMath.addBonus(3.0, false, 1.0, ENTITY_SLIDER_MAX));
    }

    @Test
    void reachSlidersStopExactlyAtTheBonusTheServerAccepts() {
        ReachModule module = new ReachModule();
        assertEquals(ENTITY_SLIDER_MAX, module.entityBonus().maximum());
        assertEquals(BLOCK_SLIDER_MAX, module.blockBonus().maximum());
    }

    @Test
    void entitySliderMaximumReachesExactlyTheProtocolCeiling() {
        assertEquals(5.9, ReachMath.addBonus(3.0, true, 2.9, ENTITY_SLIDER_MAX), 1.0E-9);
        assertEquals(6.0, ReachMath.addBonus(3.0, true, ENTITY_SLIDER_MAX, ENTITY_SLIDER_MAX), 1.0E-9);
        assertEquals(6.0, ReachMath.addBonus(3.0, true, ENTITY_SLIDER_MAX + 0.1, ENTITY_SLIDER_MAX), 1.0E-9);
        assertEquals(6.0, ReachMath.addBonus(3.0, true, 99.0, ENTITY_SLIDER_MAX), 1.0E-9);
    }

    @Test
    void blockSliderMaximumReachesExactlyTheProtocolCeiling() {
        assertEquals(5.0, ReachMath.addBonus(4.5, true, 0.5, BLOCK_SLIDER_MAX), 1.0E-9);
        assertEquals(5.5, ReachMath.addBonus(4.5, true, BLOCK_SLIDER_MAX, BLOCK_SLIDER_MAX), 1.0E-9);
        assertEquals(5.5, ReachMath.addBonus(4.5, true, BLOCK_SLIDER_MAX + 0.5, BLOCK_SLIDER_MAX), 1.0E-9);
        assertEquals(5.5, ReachMath.addBonus(4.5, true, 64.0, BLOCK_SLIDER_MAX), 1.0E-9);
    }

    @Test
    void attackRangeExtensionChangesOnlyMaxima() {
        AttackRange original = new AttackRange(0.2F, 3.0F, 0.4F, 4.0F, 0.15F, 0.75F);
        AttackRange extended = ReachMath.extendAttackRange(original, true, 1.0);

        assertEquals(0.2F, extended.minRange());
        assertEquals(4.0F, extended.maxRange());
        assertEquals(0.4F, extended.minCreativeRange());
        assertEquals(5.0F, extended.maxCreativeRange());
        assertEquals(0.15F, extended.hitboxMargin());
        assertEquals(0.75F, extended.mobFactor());
        assertSame(original, ReachMath.extendAttackRange(original, false, 1.0));
    }

    @Test
    void negativeAndNonFiniteBonusesLeaveTheVanillaRangeUntouched() {
        assertEquals(3.0, ReachMath.addBonus(3.0, true, -2.0, ENTITY_SLIDER_MAX));
        assertEquals(3.0, ReachMath.addBonus(3.0, true, Double.NaN, ENTITY_SLIDER_MAX));
        assertEquals(3.0, ReachMath.addBonus(3.0, true, 1.0, Double.NaN));
        assertEquals(4.5, ReachMath.addBonus(4.5, true, -1.0, BLOCK_SLIDER_MAX));
        assertEquals(4.5, ReachMath.addBonus(4.5, true, Double.NaN, BLOCK_SLIDER_MAX));
        assertEquals(4.5, ReachMath.addBonus(4.5, true, Double.POSITIVE_INFINITY, BLOCK_SLIDER_MAX));

        AttackRange original = new AttackRange(0.2F, 3.0F, 0.4F, 4.0F, 0.15F, 0.75F);
        assertSame(original, ReachMath.extendAttackRange(original, true, Double.NaN));
    }

    @Test
    void attackRangeExtensionStopsAtTheServerEntityCeiling() {
        AttackRange original = new AttackRange(0.2F, 3.0F, 0.4F, 4.0F, 0.15F, 0.75F);
        AttackRange extended = ReachMath.extendAttackRange(original, true, 99.0);

        assertEquals(6.0F, extended.maxRange(), 0.001F);
        assertEquals(6.0F, extended.maxCreativeRange(), 0.001F);
    }

    @Test
    void attackRangeExtensionUsesTheFullBonusUpToTheCeiling() {
        AttackRange original = new AttackRange(0.2F, 3.0F, 0.4F, 4.0F, 0.15F, 0.75F);
        AttackRange extended = ReachMath.extendAttackRange(original, true, ENTITY_SLIDER_MAX);

        assertEquals(0.2F, extended.minRange());
        assertEquals(6.0F, extended.maxRange());
        assertEquals(0.15F, extended.hitboxMargin());
    }
}
