package dev.jilaton.client.module.impl;

/** Slot arbitration rules shared by AutoWeapon and its focused unit tests. */
public final class AutoWeaponPolicy {
    private AutoWeaponPolicy() {
    }

    public static boolean shouldSwitch(double currentDamage, double candidateDamage,
                                       double minimumAdvantage, boolean forcedChoice) {
        return forcedChoice || candidateDamage > currentDamage + Math.max(0.0, minimumAdvantage);
    }

    public static boolean stillOwnsSelectedSlot(int currentSlot, int moduleSelectedSlot) {
        return currentSlot == moduleSelectedSlot;
    }
}
