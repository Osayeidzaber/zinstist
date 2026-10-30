package dev.jilaton.client.rotation;

/** Ordering for modules that can request ownership of the visible player rotation. */
public final class RotationPriorities {
    public static final int AIM_ASSIST = 20;
    public static final int AUTO_ROD = 40;
    public static final int AUTO_SHOOT = 45;
    public static final int AUTO_SHOOT_WITH_KILL_AURA = 75;
    public static final int BOW_AIM = 50;
    public static final int ELYTRA_TARGET = 60;
    public static final int KILL_AURA = 70;
    public static final int ANCHOR_AURA = 80;

    private RotationPriorities() {
    }
}
