package dev.jilaton.client.module.impl;

/** Pure policy for the generic velocity modifier; kept separate for deterministic verification. */
public final class VelocityPolicy {
    private VelocityPolicy() {
    }

    public static boolean shouldModify(boolean onGround, boolean moving, int chanceRoll,
                                       int chancePercent, String groundFilter, boolean onlyMove) {
        if (chancePercent <= 0 || chanceRoll < 0 || chanceRoll >= 100 || chanceRoll >= chancePercent
                || (onlyMove && !moving)) {
            return false;
        }
        return switch (groundFilter) {
            case "On ground" -> onGround;
            case "In air" -> !onGround;
            case "Any" -> true;
            default -> false;
        };
    }

    public static double applyAxis(double incoming, double current, double scalePercent, double motionPercent) {
        double scale = scalePercent == 0.0 ? motionPercent : scalePercent;
        return scalePercent == 0.0 ? current * scale / 100.0 : incoming * scale / 100.0;
    }
}
