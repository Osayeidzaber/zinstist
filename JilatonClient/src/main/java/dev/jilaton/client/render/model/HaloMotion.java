package dev.jilaton.client.render.model;

/**
 * Halo placement and motion. Jilaton's name tags are drawn from an anchor at
 * {@code getBbHeight() + 0.15} upwards, so the ring hovers just over the head and its highest world
 * point is clamped below that anchor: the halo can never enter the name-tag layer at any tilt.
 */
public final class HaloMotion {
    /** Name-tag anchor height above the entity's feet, in blocks (NametagRenderer.java:42). */
    public static final double TAG_ANCHOR = 0.15;
    public static final double TAG_GAP = 0.012;
    /** The ring centre sits this far above the top of the head. */
    public static final double HOVER = 0.02;
    /** Radians per second of the emission pulse; slow enough to read as breathing, not flicker. */
    public static final double PULSE_RATE = 1.05;

    private HaloMotion() { }

    /** The emission pulse has its own clock so it keeps breathing when spin is set to zero. */
    public static double advancePulse(double previous, double dt, boolean reducedMotion) {
        if (reducedMotion) return previous;
        return previous + Math.max(0, dt) * PULSE_RATE;
    }

    /** Subtle 0..1 emission swing; reduced motion holds it at the midpoint so the ring still glows. */
    public static double pulse(double phase, boolean reducedMotion) {
        return reducedMotion ? 0.5 : 0.5 + 0.5 * Math.sin(phase);
    }

    /** Slow spin; decorative, so reduced motion freezes it. */
    public static double advancePhase(double previous, double dt, double speed, boolean reducedMotion) {
        if (reducedMotion) return previous;
        return previous + Math.max(0, dt) * Math.max(0, speed) * Math.PI * 2 * 0.35;
    }

    public static double bob(double phase, double amplitude, boolean reducedMotion) {
        return reducedMotion ? 0 : amplitude * Math.sin(phase * 2);
    }

    public static double centreHeight(double bbHeight) {
        return bbHeight + HOVER;
    }

    /** Highest point of a tilted ring above the entity's feet. */
    public static double topHeight(double bbHeight, double radius, double tiltDegrees,
                                   double thickness, double bobAmplitude) {
        double extent = Math.abs(radius) * Math.abs(Math.sin(Math.toRadians(tiltDegrees))) + Math.abs(thickness);
        return centreHeight(bbHeight) + Math.abs(bobAmplitude) + extent;
    }

    /**
     * Largest tilt that still keeps the whole ring below the name-tag anchor. With the default radius
     * this is about 22 degrees; a larger ring can tilt less, which is the only way to honour the tag
     * clearance instead of overlapping it.
     */
    public static double maxTilt(double radius, double thickness, double bobAmplitude) {
        double r = Math.abs(radius);
        if (r < 1e-6) return 90;
        double room = TAG_ANCHOR - TAG_GAP - HOVER - Math.abs(bobAmplitude) - Math.abs(thickness);
        return Math.toDegrees(Math.asin(Math.max(0, Math.min(1, room / r))));
    }

    public static double clampTilt(double requested, double radius, double thickness, double bobAmplitude) {
        return Math.max(-maxTilt(radius, thickness, bobAmplitude),
                Math.min(maxTilt(radius, thickness, bobAmplitude), requested));
    }

    /** Point on the untransformed ring (XZ plane, axis +Y). */
    public static double[] ringPoint(double radius, double angle) {
        return new double[]{Math.cos(angle) * radius, 0, Math.sin(angle) * radius};
    }

    public static int majorSegments(int quality) {
        return 24 + Math.max(0, Math.min(3, quality)) * 8;
    }

    public static int minorSegments(int quality) {
        return 4 + Math.max(0, Math.min(3, quality));
    }
}
