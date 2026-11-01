package dev.jilaton.client.music;

import java.util.List;

/**
 * Layout math for the first-person music display. Each mode puts the album artwork and the track/artist line
 * somewhere specific relative to the eye, measured in blocks along the camera's forward, right and up axes, so
 * the card stays on screen while the player looks around without being a GUI overlay.
 *
 * <p>"Custom" keeps the original world-anchored panel and its "only in third person" switch; the other four
 * modes are explicit first-person displays. Pure math, unit tested without a client.
 */
public final class MusicDisplay {
    public static final String COMPACT = "Compact HUD";
    public static final String MINIMAL = "Minimal";
    public static final String WORLD_SPACE = "World Space";
    public static final String CINEMATIC = "Cinematic";
    public static final String CUSTOM = "Custom";
    public static final List<String> MODES = List.of(COMPACT, MINIMAL, WORLD_SPACE, CINEMATIC, CUSTOM);

    /**
     * Where one mode draws its card. All distances are blocks from the eye along the camera basis; art size is the
     * square edge length; text sizes are world em units; {@code titleGap} is the gap between artwork and title.
     */
    public record Placement(boolean cameraAnchored, double forward, double right, double up, double artSize,
                            boolean showArt, float titleSize, float artistSize, double progressWidth,
                            double titleGap) { }

    private MusicDisplay() { }

    /** True when {@code mode} pins the card to the camera in first person; Custom uses the legacy world panel. */
    public static boolean cameraAnchored(String mode, boolean firstPerson) {
        return firstPerson && !CUSTOM.equals(mode);
    }

    /** Layout for an explicit card mode. Custom (and unknown names) fall back to the legacy world panel. */
    public static Placement placement(String mode, double panelSize) {
        // Every first-person card sits out in the lower-left, at least ~25 degrees off the crosshair, so it never
        // covers what you are looking at or fighting.
        return switch (mode) {
            case COMPACT -> new Placement(true, 1.35, -0.66, -0.36, 0.28, true, 0.0085F, 0.0062F, 0.26, 0.10);
            case MINIMAL -> new Placement(true, 1.50, -0.62, -0.42, 0.0, false, 0.011F, 0.008F, 0.0, 0.0);
            case CINEMATIC -> new Placement(true, 2.80, 0.0, -0.30, 1.0, true, 0.020F, 0.0125F, 0.9, 0.18);
            case WORLD_SPACE -> new Placement(true, 2.40, -1.15, -0.42, panelSize * 1.2, true, 0.016F,
                    0.011F, 0.6, 0.14);
            default -> new Placement(false, 1.9, 0.0, 0.0, panelSize, true, 0.012F, 0.009F, 0.5, 0.12);
        };
    }

    /** True when this mode draws the track title in the given view (Custom can hide itself in first person). */
    public static boolean showsTrackTitle(String mode, boolean firstPerson, boolean thirdPersonOnly) {
        return !(CUSTOM.equals(mode) && firstPerson && thirdPersonOnly);
    }

    /** Distance below the card centre at which the title baseline sits, so text never lands on the artwork. */
    public static double titleOffset(Placement placement) {
        return placement.showArt() ? -(placement.artSize() * 0.5 + placement.titleGap()) : 0;
    }

    /** Smallest half-angle (radians) the artwork subtends at the eye; used to keep cards out of the near plane. */
    public static double artHalfAngle(Placement placement) {
        if (!placement.showArt() || placement.forward() <= 0) return 0;
        return Math.atan(placement.artSize() * 0.5 / placement.forward());
    }
}
