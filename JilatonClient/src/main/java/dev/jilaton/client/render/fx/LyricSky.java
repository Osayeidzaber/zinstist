package dev.jilaton.client.render.fx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * "Sky Lyrics" placement: every lyric line becomes its own fixed point on the celestial sphere, in the compass
 * direction the player was looking when the line started. Turning the camera never moves a line — it stays at
 * that bearing and height in the sky, like the sun or moon — and the next line takes a fresh direction. A line
 * born where a live one already sits is raised a row so the two never overlap. Lines share the explicit
 * {@link LyricStates} lifecycle and are bounded by a maximum number of live lines.
 *
 * <p>Pure direction and timing math: no Minecraft or texture types, so the placement and lifecycle are unit tested.
 */
public final class LyricSky {
    /** Distance of the text from the eye, in blocks: far enough to read as part of the sky. */
    public static final double RADIUS = 72;
    /** A line is never placed at or below the horizon; it lives in the sky. */
    public static final double MIN_ELEVATION_DEGREES = 12;
    /** Steepest elevation used, leaving room for rows without flipping over the zenith. */
    public static final double MAX_ELEVATION_DEGREES = 74;
    /** How far a crowded line is raised, in degrees. */
    public static final double ROW_STEP_DEGREES = 9;
    /** Azimuth distance under which, together with {@link #CROWD_ELEVATION_DEGREES}, a line counts as crowded. */
    public static final double CROWD_ANGLE_DEGREES = 14;
    /** Elevation distance under which a new line sits on top of an existing one. */
    public static final double CROWD_ELEVATION_DEGREES = 4;

    /** One placed line. Its direction is fixed at birth; only visibility animates. */
    public static final class Entry {
        public final String text;
        public final int lineIndex;
        /** Compass bearing in radians, fixed at birth. */
        public final double azimuth;
        /** Elevation in radians, fixed at birth. */
        public final double elevation;
        public final long bornNanos;
        long evictedNanos = -1;

        Entry(String text, int lineIndex, double azimuth, double elevation, long bornNanos) {
            this.text = text;
            this.lineIndex = lineIndex;
            this.azimuth = azimuth;
            this.elevation = elevation;
            this.bornNanos = bornNanos;
        }

        public boolean evicted() { return evictedNanos >= 0; }

        public LyricStates.State state(long now, double lingerSeconds, boolean active) {
            return LyricStates.state(ageSeconds(this, now), active, lingerSeconds, evictedAge(this, now));
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public List<Entry> entries() { return Collections.unmodifiableList(entries); }

    public void clear() { entries.clear(); }

    /**
     * Places a line in the sky along {@code dir} (the gaze at birth). The elevation is lifted above the horizon
     * and raised a row per older line it would overlap; the oldest lines beyond {@code maxLines} start fading.
     */
    public Entry place(String text, int lineIndex, double dirX, double dirY, double dirZ, long now, int maxLines) {
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (!(length > 1e-9)) { dirX = 0; dirY = 0; dirZ = 1; length = 1; }
        dirX /= length; dirY /= length; dirZ /= length;
        double azimuth = Math.atan2(-dirX, dirZ);
        double elevation = clampElevation(Math.asin(Math.max(-1, Math.min(1, dirY))));
        // Row escalation: lift the new anchor until it clears every live line it would overlap. Older lines stay
        // exactly where they were born, so a line change never teleports an earlier line.
        for (int guard = 0; guard < 32; guard++) {
            boolean crowded = false;
            for (Entry older : entries) {
                if (older.evicted()) continue;
                if (overlaps(azimuth, elevation, older.azimuth, older.elevation)) {
                    crowded = true;
                    break;
                }
            }
            if (!crowded) break;
            double next = Math.toDegrees(elevation) + ROW_STEP_DEGREES;
            elevation = Math.toRadians(next > MAX_ELEVATION_DEGREES ? MIN_ELEVATION_DEGREES : next);
        }
        Entry entry = new Entry(text, lineIndex, azimuth, elevation, now);
        entries.add(entry);
        int live = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry e = entries.get(i);
            if (e.evicted()) continue;
            if (++live > Math.max(1, maxLines)) e.evictedNanos = now;
        }
        return entry;
    }

    /** The line currently being sung: the newest live entry for {@code lineIndex}, or null. */
    public Entry active(int lineIndex) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry e = entries.get(i);
            if (!e.evicted()) return e.lineIndex == lineIndex ? e : null;
        }
        return null;
    }

    /** Drops lines that have fully expired; the sung line never times out. */
    public void step(long now, double lingerSeconds, int activeLineIndex) {
        Entry active = active(activeLineIndex);
        entries.removeIf(e -> e.state(now, lingerSeconds, e == active) == LyricStates.State.EXPIRED);
    }

    public static double alpha(Entry e, long now, double lingerSeconds, boolean active) {
        return LyricStates.alpha(ageSeconds(e, now), active, lingerSeconds, evictedAge(e, now));
    }

    public static double appear(Entry e, long now) {
        return LyricStates.appear(ageSeconds(e, now));
    }

    /** Unit direction of an anchor, pointing from the eye toward the text. */
    public static double[] direction(double azimuth, double elevation) {
        double horizontal = Math.cos(elevation);
        return new double[]{-Math.sin(azimuth) * horizontal, Math.sin(elevation), Math.cos(azimuth) * horizontal};
    }

    /**
     * Corners of a camera-facing sky quad centred along the anchor direction, in camera-relative space, ordered
     * bottom-left, bottom-right, top-right, top-left (matching the {@code (0,1) (1,1) (1,0) (0,0)} UVs).
     */
    public static float[] corners(double azimuth, double elevation, double radius, double width, double height) {
        double[] dir = direction(azimuth, elevation);
        double dx = dir[0], dy = dir[1], dz = dir[2];
        double horizontal = Math.hypot(dx, dz);
        // Screen right for a viewer looking along dir with world up: dir x up, normalized.
        double rx = horizontal > 1e-9 ? -dz / horizontal : 1;
        double rz = horizontal > 1e-9 ? dx / horizontal : 0;
        // Quad up: right x dir, so the text stays upright in the sky.
        double ux = -rz * dy;
        double uy = rz * dx - rx * dz;
        double uz = rx * dy;
        double cx = dx * radius, cy = dy * radius, cz = dz * radius;
        double hw = width * 0.5, hh = height * 0.5;
        float[] out = new float[12];
        corner(out, 0, cx, cy, cz, rx, ux, uy, rz, uz, -hw, -hh);
        corner(out, 1, cx, cy, cz, rx, ux, uy, rz, uz, hw, -hh);
        corner(out, 2, cx, cy, cz, rx, ux, uy, rz, uz, hw, hh);
        corner(out, 3, cx, cy, cz, rx, ux, uy, rz, uz, -hw, hh);
        return out;
    }

    /** Right and up axes of the camera-facing quad (right.y is always zero, so it is omitted). */
    private static void corner(float[] out, int index, double cx, double cy, double cz,
                               double rx, double ux, double uy, double rz, double uz, double right, double up) {
        out[index * 3] = (float) (cx + rx * right + ux * up);
        out[index * 3 + 1] = (float) (cy + uy * up);
        out[index * 3 + 2] = (float) (cz + rz * right + uz * up);
    }

    static double clampElevation(double radians) {
        double min = Math.toRadians(MIN_ELEVATION_DEGREES), max = Math.toRadians(MAX_ELEVATION_DEGREES);
        return Math.max(min, Math.min(max, radians));
    }

    /** Two anchors crowd each other when they point in nearly the same compass direction at nearly the same row. */
    static boolean overlaps(double azimuthA, double elevationA, double azimuthB, double elevationB) {
        double dAz = Math.abs(Math.toDegrees(LyricLayout.wrap(azimuthA - azimuthB)));
        double dEl = Math.abs(Math.toDegrees(elevationA - elevationB));
        return dAz < CROWD_ANGLE_DEGREES && dEl < CROWD_ELEVATION_DEGREES;
    }

    static double ageSeconds(Entry e, long now) {
        return Math.max(0, (now - e.bornNanos) / 1e9);
    }

    static double evictedAge(Entry e, long now) {
        return e.evicted() ? Math.max(0, (now - e.evictedNanos) / 1e9) : -1;
    }
}
