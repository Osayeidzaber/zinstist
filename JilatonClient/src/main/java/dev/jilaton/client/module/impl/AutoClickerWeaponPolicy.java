package dev.jilaton.client.module.impl;

/** Maps the selected AutoClicker weapon category to the held-item characteristics. */
public final class AutoClickerWeaponPolicy {
    private AutoClickerWeaponPolicy() {
    }

    public static boolean matches(String filter, boolean sword, boolean axe, boolean mace,
                                  boolean spear, boolean knockback, boolean fireAspect) {
        return switch (filter) {
            case "Any" -> true;
            case "Sword" -> sword;
            case "Axe" -> axe;
            case "Mace" -> mace;
            case "Spear / Trident" -> spear;
            case "Knockback" -> knockback;
            case "Fire Aspect" -> fireAspect;
            case "Any weapon" -> sword || axe || mace || spear || knockback || fireAspect;
            default -> false;
        };
    }
}
