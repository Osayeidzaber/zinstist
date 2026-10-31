package dev.jilaton.client.render.fx;

/**
 * Fixed visual precedence for the camera-local ambience layer.
 *
 * <p>Order: {@code CustomSky > Cyberspace > GlassMaterial > Fullbright > client accent}. The first enabled
 * module owns the scene accent; Fullbright owns no colour, so it falls back to the client accent but also
 * disables the night-only gate because the world is lit. Keeping this in one pure class means the order is
 * enforceable and testable rather than documented in prose only.</p>
 */
public final class AmbiencePrecedence {
    public enum Scene { CUSTOM_SKY, CYBERSPACE, GLASS, FULLBRIGHT, CLIENT }

    private AmbiencePrecedence() { }

    public static Scene owner(boolean customSky, boolean cyberspace, boolean glass, boolean fullbright) {
        if (customSky) return Scene.CUSTOM_SKY;
        if (cyberspace) return Scene.CYBERSPACE;
        if (glass) return Scene.GLASS;
        if (fullbright) return Scene.FULLBRIGHT;
        return Scene.CLIENT;
    }

    public static int accent(Scene owner, int skyAccent, int cyberAccent, int glassAccent, int clientAccent) {
        return switch (owner) {
            case CUSTOM_SKY -> skyAccent;
            case CYBERSPACE -> cyberAccent;
            case GLASS -> glassAccent;
            default -> clientAccent;
        };
    }

    /** Fullbright alone lifts the world light level, so night-only ambience stays visible. */
    public static boolean nightGateApplies(boolean nightOnly, boolean fullbright) {
        return nightOnly && !fullbright;
    }

    /** The sky owner draws its own light streaks; ambience defers instead of double-drawing the sky. */
    public static boolean drawsSkyStreaks(Scene owner) {
        return owner != Scene.CUSTOM_SKY;
    }
}
