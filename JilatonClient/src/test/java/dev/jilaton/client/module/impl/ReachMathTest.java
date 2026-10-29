package dev.jilaton.client.module.impl;

import net.minecraft.world.item.component.AttackRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ReachMathTest {
    @Test
    void interactionRangeBonusRequiresTheEnabledLocalPlayer() {
        assertEquals(4.0, ReachMath.addBonus(3.0, true, 1.0, 5.0));
        assertEquals(3.0, ReachMath.addBonus(3.0, false, 1.0, 5.0));
    }

    @Test
    void interactionRangeBonusIsBoundedAndRejectsNonFiniteInput() {
        assertEquals(8.0, ReachMath.addBonus(3.0, true, 99.0, 5.0));
        assertEquals(3.0, ReachMath.addBonus(3.0, true, Double.NaN, 5.0));
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
}
