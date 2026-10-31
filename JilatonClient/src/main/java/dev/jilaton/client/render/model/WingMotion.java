package dev.jilaton.client.render.model;

import java.util.List;

/**
 * Wing layout and flap timing, kept free of rendering state so the headless tests can pin the phase lag
 * and the quality scaling. A wing is a fan of feathers from the leading edge (near vertical) to the tip
 * (slightly below horizontal); each feather samples the flap wave a little later than the one above it,
 * which makes the crest travel out along the wing instead of moving as one rigid piece.
 */
public final class WingMotion {
    public static final List<String> STYLES = List.of("Feather", "Blade", "Energy");
    public static final double LAG_STEP = 0.085;
    private static final double TOP_FAN = 78;
    private static final double FAN_SPREAD = 86;
    /** Forward component of every feather, so the fan sits slightly behind the shoulder plane. */
    private static final double FEATHER_SWEEP_BACK = -0.16;

    private WingMotion() { }

    public static double advancedPhase(double previous, double dt, double speed, boolean reducedMotion) {
        if (!Double.isFinite(previous)) previous = 0;
        if (reducedMotion || !(dt > 0) || !(speed > 0)) return previous;
        return previous + dt * speed * Math.PI * 2;
    }

    /** Monotonic in the feather index: the leading feather moves first, the tip trails it. */
    public static double lagFor(int featherIndex) {
        return Math.max(0, featherIndex) * LAG_STEP;
    }

    public static double flapAngle(double phase, int featherIndex, double base, double amplitude) {
        return base + amplitude * Math.sin(phase - lagFor(featherIndex));
    }

    public static int layers(String style) {
        return style.equals("Feather") ? 3 : 2;
    }

    public static int feathers(String style, int layer, int quality) {
        int q = Math.max(0, Math.min(3, quality));
        return switch (style) {
            case "Blade" -> (4 + q * 2) - (layer == 0 ? 1 : 0);
            case "Energy" -> 3 + q;
            default -> (5 + q * 2) - (2 - layer);
        };
    }

    public static int segments(String style, int quality) {
        int q = Math.max(0, Math.min(3, quality));
        return 3 + q + (style.equals("Feather") ? 1 : 0);
    }

    public static double baseFlap(String style) {
        return switch (style) {
            case "Blade" -> -0.12;
            case "Energy" -> -0.22;
            default -> -0.18;
        };
    }

    public static double amplitude(String style) {
        return switch (style) {
            case "Blade" -> 0.24;
            case "Energy" -> 0.42;
            default -> 0.34;
        };
    }

    public static double spread(String style) {
        return switch (style) {
            case "Blade" -> 0.62;
            case "Energy" -> 1.05;
            default -> 0.85;
        };
    }

    /** 0 at the leading feather, 1 at the tip feather. */
    public static double fanPosition(int index, int count) {
        return count <= 1 ? 0 : (double) index / (count - 1);
    }

    public static double fanDegrees(int index, int count) {
        return TOP_FAN - FAN_SPREAD * fanPosition(index, count);
    }

    public static double featherLength(int layer, int index, int count) {
        double base = layer == 0 ? 0.42 : layer == 1 ? 0.64 : 1.0;
        return base * (0.72 + 0.28 * fanPosition(index, count));
    }

    public static double layerDepth(int layer) {
        return -0.045 * layer;
    }

    public static double rootHeight(int layer) {
        return 0.055 * layer;
    }

    public static double halfWidth(String style) {
        return style.equals("Blade") ? 0.03 : style.equals("Energy") ? 0.05 : 0.055;
    }

    public static double tipWidth(String style) {
        return style.equals("Blade") ? 0.004 : style.equals("Energy") ? 0.016 : 0.01;
    }

    public static double fold(String style) {
        return style.equals("Blade") ? 0.055 : style.equals("Energy") ? 0.02 : 0.045;
    }

    public static double bend(String style) {
        return style.equals("Blade") ? -0.02 : style.equals("Energy") ? -0.14 : -0.06;
    }

    /**
     * Pose frame of one feather on one side. The fan angle is measured in the wing's left-handed body frame
     * (local +X is the entity's right), and the flap sweeps that fan up and down, so the leading feather
     * rises before the tip does. The fan direction is authored directly, then the trailing X mirror restores
     * the submit path's winding handedness without touching the direction or the up axis (the strip is
     * symmetric in X). Public so the headless tests can pin the fan direction.
     */
    public static Transform featherFrame(Transform pivot, int side, double fanDegrees, double flapDegrees) {
        double angle = Math.toRadians(fanDegrees + flapDegrees);
        double dirX = side * Math.cos(angle);
        double dirY = Math.sin(angle);
        return pivot.compose(Transform.lookAlong(dirX, dirY, FEATHER_SWEEP_BACK, 0, 1, 0))
                .compose(Transform.mirrorX());
    }
}
