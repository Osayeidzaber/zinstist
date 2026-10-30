package dev.jilaton.client.render.fx;

import dev.jilaton.client.gui.hud.HudText;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Pure placement math for spatial lyrics, kept free of Minecraft types so it can be unit tested. Slots are
 * indexed relative to the active line (0 = active, -1 = previous, +1 = next). Offsets are in blocks on the
 * horizontal plane around the stack's {@link Anchor}, using the bearing that was frozen when the stack was
 * placed — never the live gaze, so the text behaves like a world object.
 */
public final class LyricLayout {
    public static final List<String> MODES = List.of("Curved Arc", "Floating Side", "Circular Orbit", "Behind Player",
            "Cinematic Floating", "Minimal");
    /** Never place the active line closer than this to the view direction, so it stays off the crosshair. */
    public static final double MIN_VIEW_ANGLE = Math.toRadians(22);
    public static final double MIN_RADIUS = 1.2;
    /** In first person the arc may not come closer than this to the eye. */
    public static final double MIN_FIRST_PERSON_RADIUS = 2.2;
    /** In first person the active line floats this far above eye height. */
    public static final double FIRST_PERSON_LIFT = 0.35;
    /** A stack this far off the gaze is considered lost. */
    public static final double LOST_BEARING = Math.toRadians(55);
    /** A stack whose anchor is this far from the player is considered unreachable. */
    public static final double LOST_DISTANCE = 6.0;
    /** A lost stack must stay lost this long before it is moved. */
    public static final double LOST_SECONDS = 1.0;
    /** At most one re-anchor per this period, so slow head pans do not cause churn. */
    public static final double REANCHOR_SECONDS = 2.5;
    /** Re-anchoring is one smooth glide, never a snap. */
    public static final double GLIDE_SECONDS = 0.6;

    public record Params(String mode, double radius, double height, double sideAngleDegrees, double spreadDegrees,
                         boolean firstPerson, double time, double bodyYaw) { }

    public record Slot(double dx, double dy, double dz, double facingYaw) { }

    private LyricLayout() { }

    /** Radius actually used for the arc; first person keeps the text out of the near plane. */
    public static double effectiveRadius(double radius, boolean firstPerson) {
        return Math.max(firstPerson ? MIN_FIRST_PERSON_RADIUS : MIN_RADIUS, radius);
    }

    /**
     * Position of slot {@code k} (may be fractional while lines slide) for a bearing in radians where the
     * horizontal direction is {@code (-sin bearing, cos bearing)}. The bearing is the anchor's, so the arc
     * does not follow the camera.
     */
    public static Slot slot(Params p, double k, double viewYaw) {
        double radius = effectiveRadius(p.radius(), p.firstPerson());
        double side = Math.toRadians(p.sideAngleDegrees());
        if (p.firstPerson() && Math.abs(side) < MIN_VIEW_ANGLE) side = Math.copySign(MIN_VIEW_ANGLE, side == 0 ? 1 : side);
        double spread = Math.toRadians(p.spreadDegrees());
        return switch (p.mode()) {
            case "Floating Side", "Minimal" -> {
                double angle = viewYaw + side;
                double dy = p.height() - k * 0.32;
                yield around(angle, radius, dy);
            }
            case "Circular Orbit" -> {
                double angle = viewYaw + side + p.time() * 0.25 + k * Math.max(spread, Math.toRadians(40));
                yield around(angle, radius, p.height() + Math.sin(p.time() * 0.8 + k) * 0.08);
            }
            case "Behind Player" -> {
                double angle = p.bodyYaw() + Math.PI + k * spread * 0.35;
                yield around(angle, radius * 0.55, p.height() + 0.9 - k * 0.28);
            }
            case "Cinematic Floating" -> {
                double angle = viewYaw + side + k * spread;
                double drift = Math.sin(p.time() * 0.35 + k * 1.7) * 0.18;
                Slot base = around(angle, radius + Math.sin(p.time() * 0.27 + k) * 0.2, p.height() + Math.abs(k) * 0.1 + drift);
                yield base;
            }
            default -> {
                // Curved Arc: lines follow a gentle arc that rises toward its ends.
                double angle = viewYaw + side + k * spread;
                yield around(angle, radius, p.height() + k * k * 0.06 - k * 0.12);
            }
        };
    }

    private static Slot around(double angle, double radius, double dy) {
        double dx = -Math.sin(angle) * radius, dz = Math.cos(angle) * radius;
        return new Slot(dx, dy, dz, Math.toDegrees(angle));
    }

    /**
     * Height added to every slot so the active line never sits below {@link #FIRST_PERSON_LIFT} above eye
     * height, clear of the crosshair. A positive height setting still raises the stack above that floor.
     */
    public static double verticalShift(Params p, double bearing) {
        return p.firstPerson() ? Math.max(0, FIRST_PERSON_LIFT - slot(p, 0, bearing).dy()) : 0;
    }

    /**
     * Fits one lyric line into {@code maxWidth}: whole words are kept and an ellipsis is appended, so a long
     * line cannot overflow its slot and an unbroken word cannot collapse to an empty string. This delegates to
     * the HUD's word-boundary fitter rather than duplicating it. Only null or empty text fits to {@code ""}.
     */
    public static String fitText(String text, IntUnaryOperator advance, int maxWidth) {
        if (text == null || text.isEmpty()) return "";
        String fitted = HudText.ellipsize(text, advance, maxWidth);
        return fitted.isEmpty() ? HudText.ELLIPSIS : fitted;
    }

    /** Opacity for slot distance {@code |k|} given the dim factor of neighbouring lines. */
    public static double opacity(double k, double dim) {
        double d = Math.abs(k);
        return d < 1 ? 1 - d * (1 - dim) : dim * Math.pow(0.55, d - 1);
    }

    /** Text scale multiplier: active line full size, neighbours smaller. */
    public static double scale(double k) {
        double d = Math.min(2, Math.abs(k));
        return 1 - d * 0.18;
    }

    /** Angle in radians between the view direction and a slot, used to keep lines off the crosshair. */
    public static double angleFromView(Slot slot, double viewYaw) {
        double fx = -Math.sin(viewYaw), fz = Math.cos(viewYaw);
        double l = Math.hypot(slot.dx(), slot.dz());
        if (l < 1e-6) return Math.PI;
        return Math.acos(Math.max(-1, Math.min(1, (slot.dx() * fx + slot.dz() * fz) / l)));
    }

    /** Bearing of a slot around the anchor, in radians. */
    public static double bearingOf(Slot slot) {
        return Math.atan2(-slot.dx(), slot.dz());
    }

    /** Shortest signed difference between two angles, in (-pi, pi]. */
    public static double wrap(double radians) {
        double wrapped = (radians + Math.PI) % (Math.PI * 2);
        if (wrapped < 0) wrapped += Math.PI * 2;
        return wrapped - Math.PI;
    }

    /**
     * One lyric stack's home in the world. The position and bearing are copied when the stack is placed, so
     * camera motion changes where the text appears on screen, never where it lives. Only {@link #reanchor}
     * moves it, and that move is a single eased glide. {@link #wantsReanchor} carries the hysteresis state:
     * a lost condition has to persist for {@link #LOST_SECONDS}, and placements are rate-limited to one per
     * {@link #REANCHOR_SECONDS}.
     */
    public static final class Anchor {
        private final double x, y, z, bearing;
        private final double fromX, fromY, fromZ, fromBearing;
        private final double placedAt;
        private double lostSince = Double.NaN;

        private Anchor(double x, double y, double z, double bearing, double fromX, double fromY, double fromZ,
                       double fromBearing, double placedAt) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.bearing = bearing;
            this.fromX = fromX;
            this.fromY = fromY;
            this.fromZ = fromZ;
            this.fromBearing = fromBearing;
            this.placedAt = placedAt;
        }

        /** A fresh stack appears exactly where the player is looking, with no glide. */
        public static Anchor birth(double x, double y, double z, double bearing, double now) {
            return new Anchor(x, y, z, bearing, x, y, z, bearing, now);
        }

        /** Moves the stack, gliding from wherever it currently is over {@link #GLIDE_SECONDS}. */
        public Anchor reanchor(double x, double y, double z, double bearing, double now) {
            return new Anchor(x, y, z, bearing, x(now), y(now), z(now), bearing(now), now);
        }

        /** True once the stack has been out of the gaze or out of reach long enough to justify moving it. */
        public boolean wantsReanchor(double now, double gazeYaw, double playerX, double playerY, double playerZ) {
            double dx = playerX - x, dy = playerY - y, dz = playerZ - z;
            boolean lost = Math.abs(wrap(gazeYaw - bearing)) > LOST_BEARING
                    || dx * dx + dy * dy + dz * dz > LOST_DISTANCE * LOST_DISTANCE;
            if (!lost) {
                lostSince = Double.NaN;
                return false;
            }
            if (Double.isNaN(lostSince)) lostSince = now;
            return now - lostSince >= LOST_SECONDS && now - placedAt >= REANCHOR_SECONDS;
        }

        public boolean gliding(double now) {
            double t = (now - placedAt) / GLIDE_SECONDS;
            return t >= 0 && t < 1 && !(fromX == x && fromY == y && fromZ == z && fromBearing == bearing);
        }

        public double x(double now) { return fromX + (x - fromX) * progress(now); }
        public double y(double now) { return fromY + (y - fromY) * progress(now); }
        public double z(double now) { return fromZ + (z - fromZ) * progress(now); }
        public double bearing(double now) { return fromBearing + wrap(bearing - fromBearing) * progress(now); }

        public double targetX() { return x; }
        public double targetY() { return y; }
        public double targetZ() { return z; }
        public double targetBearing() { return bearing; }
        public double placedAt() { return placedAt; }

        private double progress(double now) {
            double t = (now - placedAt) / GLIDE_SECONDS;
            if (t >= 1) return 1;
            if (t <= 0) return 0;
            return 1 - Math.pow(1 - t, 3);
        }
    }
}
