package dev.jilaton.client.render.fx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * "World Trail" lyric placement: every new line becomes its own object in the world, placed where the camera is
 * looking at the moment the line starts and then left there. Turning the camera never moves a placed line; the
 * next line simply appears wherever the view is pointing. Older lines dim, stay readable while the player walks
 * around them, and fade out after a linger time. A line born on top of a still-visible one lifts the older one
 * upward so the two never overlap.
 *
 * <p>Pure state: no Minecraft types, so placement, stacking and fading are unit tested.
 */
public final class LyricTrail {
    /** Vertical step a crowded line is lifted by: one text line plus breathing room, in blocks. */
    public static final double STACK_STEP = 0.34;
    /** Horizontal radius inside which a new line counts as being on top of an older one. */
    public static final double CROWD_RADIUS = 1.1;
    /** Closest a line may be placed to the eye, so walls right in front still leave the text readable. */
    public static final double MIN_DISTANCE = 0.9;
    /** Gap kept between a line and the block surface it was placed against. */
    public static final double WALL_GAP = 0.35;
    /** Hard bound on live trail lines, independent of any caller-supplied setting. */
    public static final int MAX_LINES = 8;
    static final double APPEAR_SECONDS = LyricStates.SETTLE_SECONDS;
    static final double FADE_SECONDS = LyricStates.FADE_SECONDS;
    static final double EVICT_SECONDS = LyricStates.EVICT_SECONDS;

    /** One placed line. Position and facing are fixed at birth; only the stacking lift animates. */
    public static final class Entry {
        public final String text;
        public final int lineIndex;
        public final double x, y, z;
        /** Unit gaze direction at birth: "behind" the text, used to extrude its depth layers. */
        public final double dirX, dirY, dirZ;
        /** Text yaw in degrees so the face points back at where the camera stood. */
        public final float yaw;
        public final long bornNanos;
        double lift;
        double liftTarget;
        long evictedNanos = -1;

        Entry(String text, int lineIndex, double x, double y, double z, double dirX, double dirY, double dirZ,
              float yaw, long bornNanos) {
            this.text = text;
            this.lineIndex = lineIndex;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dirX = dirX;
            this.dirY = dirY;
            this.dirZ = dirZ;
            this.yaw = yaw;
            this.bornNanos = bornNanos;
        }

        /** Current height including the eased stacking lift. */
        public double drawY() { return y + lift; }

        public boolean evicted() { return evictedNanos >= 0; }

        /** Explicit lifecycle of this line, shared with every other lyric presentation. */
        public LyricStates.State state(long now, double lingerSeconds, boolean active) {
            return LyricStates.state(ageSeconds(this, now), active, lingerSeconds, evictedAge(this, now));
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final List<Entry> entriesView = Collections.unmodifiableList(entries);

    public List<Entry> entries() { return entriesView; }

    public void clear() { entries.clear(); }

    /**
     * Distance along the gaze at which a new line is placed: the preferred reach, pulled in front of the first
     * block the gaze hits ({@code hitDistance}, or a negative value when nothing is hit).
     */
    public static double placementDistance(double reach, double hitDistance) {
        double distance = hitDistance >= 0 ? Math.min(reach, hitDistance - WALL_GAP) : reach;
        return Math.max(MIN_DISTANCE, distance);
    }

    /** Text yaw (degrees) that faces a viewer looking along {@code (dirX, dirZ)}. */
    public static float facingYaw(double dirX, double dirZ) {
        double viewYaw = Math.atan2(-dirX, dirZ);
        return (float) (Math.toDegrees(viewYaw) + 180);
    }

    /**
     * Places a new line at {@code eye + dir * distance}. Lines it would overlap are lifted, and the oldest lines
     * beyond {@code maxLines} start fading out instead of vanishing.
     */
    public Entry place(String text, int lineIndex, double eyeX, double eyeY, double eyeZ,
                       double dirX, double dirY, double dirZ, double distance, long now, int maxLines) {
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (!(length > 1e-9)) { dirX = 0; dirY = 0; dirZ = 1; length = 1; }
        dirX /= length; dirY /= length; dirZ /= length;
        double x = eyeX + dirX * distance, y = eyeY + dirY * distance, z = eyeZ + dirZ * distance;
        // Lift whatever already sits here. Each lifted line can crowd the one above it, so cascade upward.
        double level = y;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry older = entries.get(i);
            if (older.evicted() || Math.hypot(older.x - x, older.z - z) > CROWD_RADIUS) continue;
            double olderTop = older.y + older.liftTarget;
            if (Math.abs(olderTop - level) < STACK_STEP) {
                older.liftTarget = level + STACK_STEP - older.y;
                level += STACK_STEP;
            }
        }
        Entry entry = new Entry(text, lineIndex, x, y, z, dirX, dirY, dirZ, facingYaw(dirX, dirZ), now);
        entries.add(entry);
        int cap = Math.max(1, Math.min(MAX_LINES, maxLines));
        int live = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry e = entries.get(i);
            if (e.evicted()) continue;
            if (++live > cap) e.evictedNanos = now;
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

    /** Eases stacking lifts and drops lines that have fully faded; the line being sung never times out. */
    public void step(double dtSeconds, long now, double lingerSeconds, int activeLineIndex) {
        double k = 1 - Math.exp(-Math.max(0, dtSeconds) * 9);
        Entry active = active(activeLineIndex);
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry e = entries.get(i);
            if (ageSeconds(e, now) > APPEAR_SECONDS && alpha(e, now, lingerSeconds, e == active) <= 0) entries.remove(i);
        }
        for (int i = 0, n = entries.size(); i < n; i++) {
            Entry e = entries.get(i);
            e.lift += (e.liftTarget - e.lift) * k;
        }
    }

    /** 0..1 visibility from age alone: ease in, hold for the linger time, then fade; eviction fades faster. */
    public static double alpha(Entry e, long now, double lingerSeconds, boolean active) {
        return LyricStates.alpha(ageSeconds(e, now), active, lingerSeconds, evictedAge(e, now));
    }

    /** Appearance progress 0..1, for the rise-in and scale-in of a new line. */
    public static double appear(Entry e, long now) {
        return LyricStates.appear(ageSeconds(e, now));
    }

    static double ageSeconds(Entry e, long now) {
        return Math.max(0, (now - e.bornNanos) / 1e9);
    }

    /** Seconds since eviction, or a negative value while the line is still live. */
    static double evictedAge(Entry e, long now) {
        return e.evicted() ? Math.max(0, (now - e.evictedNanos) / 1e9) : -1;
    }
}
