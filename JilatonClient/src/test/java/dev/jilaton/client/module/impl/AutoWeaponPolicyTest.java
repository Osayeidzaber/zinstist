package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoWeaponPolicyTest {
    @Test
    void switchesOnlyForAdvantageOrAnExplicitCombatOverride() {
        assertFalse(AutoWeaponPolicy.shouldSwitch(8.0, 8.1, 0.2, false));
        assertTrue(AutoWeaponPolicy.shouldSwitch(8.0, 8.3, 0.2, false));
        assertTrue(AutoWeaponPolicy.shouldSwitch(10.0, 6.0, 0.2, true));
    }

    @Test
    void restoresOnlyWhileTheModuleStillOwnsTheSelectedSlot() {
        assertTrue(AutoWeaponPolicy.stillOwnsSelectedSlot(4, 4));
        assertFalse(AutoWeaponPolicy.stillOwnsSelectedSlot(2, 4));
    }
}
