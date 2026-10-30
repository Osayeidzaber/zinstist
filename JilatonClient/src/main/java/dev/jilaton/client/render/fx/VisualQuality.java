package dev.jilaton.client.render.fx;

import java.util.List;

/** Shared quality tiers. Every world effect scales its particle and tessellation budget from one of these. */
public enum VisualQuality {
    LOW("Low", 0.35, 256, 24),
    BALANCED("Balanced", 0.7, 768, 40),
    HIGH("High", 1.0, 1536, 56),
    CINEMATIC("Cinematic", 1.4, 3072, 72);

    public static final List<String> NAMES = List.of("Low", "Balanced", "High", "Cinematic");
    private final String label;
    private final double density;
    private final int particleBudget;
    private final int circleSegments;

    VisualQuality(String label, double density, int particleBudget, int circleSegments) {
        this.label = label;
        this.density = density;
        this.particleBudget = particleBudget;
        this.circleSegments = circleSegments;
    }

    public String label() { return label; }
    /** Multiplier for spawn counts. */
    public double density() { return density; }
    /** Hard cap on live particles across every module. */
    public int particleBudget() { return particleBudget; }
    /** Segment count for full circles; arcs scale proportionally. */
    public int circleSegments() { return circleSegments; }

    public static VisualQuality byLabel(String label) {
        for (VisualQuality quality : values()) if (quality.label.equals(label)) return quality;
        return BALANCED;
    }
}
