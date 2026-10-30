package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoClickerWeaponPolicyTest {
    @Test
    void anyWeaponCategoryMatchesOnlyItsConfiguredItemOrEnchantment() {
        assertTrue(AutoClickerWeaponPolicy.matches("Sword", true, false, false, false, false, false));
        assertFalse(AutoClickerWeaponPolicy.matches("Sword", false, true, false, false, false, false));
        assertTrue(AutoClickerWeaponPolicy.matches("Mace", false, false, true, false, false, false));
        assertTrue(AutoClickerWeaponPolicy.matches("Spear / Trident", false, false, false, true, false, false));
        assertTrue(AutoClickerWeaponPolicy.matches("Knockback", false, false, false, false, true, false));
        assertTrue(AutoClickerWeaponPolicy.matches("Fire Aspect", false, false, false, false, false, true));
        assertFalse(AutoClickerWeaponPolicy.matches("unknown", true, true, true, true, true, true));
    }

    @Test
    void anyWeaponAndUnfilteredModesPreserveTheirExpectedBehavior() {
        assertTrue(AutoClickerWeaponPolicy.matches("Any", false, false, false, false, false, false));
        assertTrue(AutoClickerWeaponPolicy.matches("Any weapon", false, false, false, false, true, false));
        assertFalse(AutoClickerWeaponPolicy.matches("Any weapon", false, false, false, false, false, false));
    }
}
