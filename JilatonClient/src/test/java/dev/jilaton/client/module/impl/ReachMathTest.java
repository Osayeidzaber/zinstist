package dev.jilaton.client.module.impl;

import net.minecraft.world.item.component.AttackRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReachMathTest {
    /** The largest bonus each Reach slider offers, derived from the protocol ceilings (vanilla 4.5 block reach). */
    private static final double ENTITY_SLIDER_MAX = ReachMath.ENTITY_BONUS_LIMIT;
    private static final double BLOCK_SLIDER_MAX = ReachMath.BLOCK_INTERACTION_LIMIT - 4.5;

    @Test
    void interactionRangeBonusRequiresTheEnabledLocalPlayer() {
        assertEquals(4.0, ReachMath.extendEntityInteractionRange(3.0, true, 1.0));
        assertEquals(3.0, ReachMath.extendEntityInteractionRange(3.0, false, 1.0));
        assertEquals(5.0, ReachMath.extendBlockInteractionRange(4.5, true, 0.5));
        assertEquals(4.5, ReachMath.extendBlockInteractionRange(4.5, false, 0.5));
    }

    @Test
    void reachSlidersStopExactlyAtTheBonusTheServerAccepts() {
        ReachModule module = new ReachModule();
        assertEquals(ENTITY_SLIDER_MAX, module.entityBonus().maximum());
        assertEquals(BLOCK_SLIDER_MAX, module.blockBonus().maximum());
    }

    @Test
    void survivalEntitySliderMaximumReachesExactlyTheProtocolCeiling() {
        assertEquals(5.9, ReachMath.extendEntityInteractionRange(3.0, true, 2.9), 1.0E-9);
        assertEquals(6.0, ReachMath.extendEntityInteractionRange(3.0, true, ENTITY_SLIDER_MAX), 1.0E-9);
        assertEquals(6.0, ReachMath.extendEntityInteractionRange(3.0, true, ENTITY_SLIDER_MAX + 0.1), 1.0E-9);
        assertEquals(6.0, ReachMath.extendEntityInteractionRange(3.0, true, 99.0), 1.0E-9);
    }

    @Test
    void creativeEntityRangeUsesTheCreativeBaseInsteadOfBeingMistakenForBlockReach() {
        // Creative adds the 2.0 creative modifier to the 3.0 base; the server accepts that plus 3.0 padding.
        assertEquals(6.0, ReachMath.extendEntityInteractionRange(5.0, true, 1.0), 1.0E-9);
        assertEquals(8.0, ReachMath.extendEntityInteractionRange(5.0, true, ENTITY_SLIDER_MAX), 1.0E-9);
        assertEquals(8.0, ReachMath.extendEntityInteractionRange(5.0, true, 99.0), 1.0E-9);
        // The old base-based heuristic classified 5.0 as a block range and stopped at 5.5.
        assertEquals(8.0, ReachMath.ENTITY_INTERACTION_LIMIT + 2.0, 1.0E-9);
    }

    @Test
    void blockSliderMaximumReachesExactlyTheProtocolCeiling() {
        assertEquals(5.0, ReachMath.extendBlockInteractionRange(4.5, true, 0.5), 1.0E-9);
        assertEquals(5.5, ReachMath.extendBlockInteractionRange(4.5, true, BLOCK_SLIDER_MAX), 1.0E-9);
        assertEquals(5.5, ReachMath.extendBlockInteractionRange(4.5, true, BLOCK_SLIDER_MAX + 0.5), 1.0E-9);
        assertEquals(5.5, ReachMath.extendBlockInteractionRange(4.5, true, 64.0), 1.0E-9);
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
        assertEquals(3.0, ReachMath.extendEntityInteractionRange(3.0, true, -2.0));
        assertEquals(3.0, ReachMath.extendEntityInteractionRange(3.0, true, Double.NaN));
        assertEquals(3.0, ReachMath.extendEntityInteractionRange(3.0, true, Double.POSITIVE_INFINITY));
        assertTrue(Double.isNaN(ReachMath.extendEntityInteractionRange(Double.NaN, true, 1.0)));
        assertEquals(4.5, ReachMath.extendBlockInteractionRange(4.5, true, -1.0));
        assertEquals(4.5, ReachMath.extendBlockInteractionRange(4.5, true, Double.NaN));
        assertEquals(4.5, ReachMath.extendBlockInteractionRange(4.5, true, Double.POSITIVE_INFINITY));

        AttackRange original = new AttackRange(0.2F, 3.0F, 0.4F, 4.0F, 0.15F, 0.75F);
        assertSame(original, ReachMath.extendAttackRange(original, true, Double.NaN));
    }

    @Test
    void survivalAttackRangeStopsAtTheServerEntityCeiling() {
        AttackRange original = new AttackRange(0.2F, 3.0F, 0.4F, 3.0F, 0.15F, 0.75F);
        AttackRange extended = ReachMath.extendAttackRange(original, true, 99.0);

        assertEquals(6.0F, extended.maxRange(), 0.001F);
        assertEquals(6.0F, extended.maxCreativeRange(), 0.001F);
    }

    @Test
    void creativeAttackRangeReachesTheCreativeCeiling() {
        // AttackRange.defaultFor(creative player) uses the 5.0 entity interaction attribute for both maxima.
        AttackRange original = new AttackRange(0.0F, 3.0F, 0.0F, 5.0F, 0.0F, 1.0F);
        AttackRange extended = ReachMath.extendAttackRange(original, true, ENTITY_SLIDER_MAX);

        assertEquals(6.0F, extended.maxRange(), 0.001F);
        assertEquals(8.0F, extended.maxCreativeRange(), 0.001F);
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
