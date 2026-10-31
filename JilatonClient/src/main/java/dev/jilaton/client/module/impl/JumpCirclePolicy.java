package dev.jilaton.client.module.impl;

/** Pure event rules for JumpCircles: one effect per jump/land cycle, with a shared cooldown gate. */
public final class JumpCirclePolicy {
    public static final long EVENT_GAP_NANOS = 120_000_000L;
    public static final long LANDING_GRACE_NANOS = 1_500_000_000L;

    private JumpCirclePolicy() { }

    public static boolean emits(boolean jumped, boolean landed, String trigger) {
        if (jumped) return !trigger.equals("Land");
        if (landed) return !trigger.equals("Jump");
        return false;
    }

    public static boolean withinCooldown(long now, long lastEvent) {
        return lastEvent > 0 && now - lastEvent < EVENT_GAP_NANOS;
    }

    /** A landing that belongs to the jump we just drew is the same effect, not a second one. */
    public static boolean landingCovered(long now, long graceUntil) {
        return graceUntil > now;
    }
}
