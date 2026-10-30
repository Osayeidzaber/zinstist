package dev.jilaton.client.render.model;

/**
 * First-person knife animation in the style of CS/CSO viewmodels: a draw that twirls the blade up into the hand,
 * idle breathing, camera-turn inertia that lets the arm lag behind fast looks, alternating slashes, and an inspect
 * that turns the blade to show both faces and flips it. Every action eases in and back out to the rest pose, so
 * switching between them never pops.
 *
 * <p>Pure state and math (no Minecraft types). Offsets are in view space, blocks and degrees, added on top of the
 * vanilla arm placement; {@code spin} turns only the knife about its own blade axis.
 */
public final class KnifeMotion {
    public static final double SLASH_SECONDS = 0.42;
    public static final double INSPECT_SECONDS = 2.6;
    public static final double UNEQUIP_SECONDS = 0.28;
    static final double INERTIA_LIMIT = 7;

    public enum State {
        IDLE,
        WALKING,
        SPRINTING,
        JUMPING,
        FALLING,
        CROUCHING,
        ATTACKING,
        SWITCHING,
        INSPECTING,
        EQUIPPING,
        UNEQUIPPING
    }

    public record Pose(double x, double y, double z, double pitch, double yaw, double roll, double spin) {
        static final Pose REST = new Pose(0, 0, 0, 0, 0, 0, 0);

        Pose plus(Pose o) {
            return new Pose(x + o.x, y + o.y, z + o.z, pitch + o.pitch, yaw + o.yaw, roll + o.roll, spin + o.spin);
        }

        Pose times(double k) {
            return new Pose(x * k, y * k, z * k, pitch * k, yaw * k, roll * k, spin * k);
        }
    }

    private long drawStart = -1;
    private long unequipStart = -1;
    private boolean switchedItem;
    private long slashStart = -1;
    private int slashDir = 1;
    private long inspectStart = -1;
    private double inertiaYaw, inertiaPitch;
    private double sprintWeight, crouchWeight, airOffset;
    private float lastYaw, lastPitch;
    private boolean hasLook;
    private long lastPoseNanos;

    public void startDraw(long now) {
        drawStart = now;
        unequipStart = -1;
        switchedItem = false;
        inspectStart = -1;
    }

    public void startSwitch(long now) {
        drawStart = now;
        unequipStart = -1;
        switchedItem = true;
        inspectStart = -1;
    }

    public void startUnequip(long now) {
        unequipStart = now;
        inspectStart = -1;
    }

    /** Each new slash comes from the opposite side of the last one. */
    public void startSlash(long now) {
        slashDir = -slashDir;
        slashStart = now;
        inspectStart = -1;
    }

    /** Inspect only starts from rest, like the games: never mid-slash or mid-draw. */
    public boolean startInspect(long now, double drawSeconds) {
        if (active(slashStart, now, SLASH_SECONDS) || active(drawStart, now, drawSeconds)
                || active(unequipStart, now, UNEQUIP_SECONDS)) return false;
        inspectStart = now;
        return true;
    }

    public int slashDirection() { return slashDir; }

    public State classifyState(long now, double drawSeconds, boolean sprinting, boolean crouching,
                               boolean onGround, double verticalVelocity, double moving) {
        if (active(unequipStart, now, UNEQUIP_SECONDS)) return State.UNEQUIPPING;
        if (active(slashStart, now, SLASH_SECONDS)) return State.ATTACKING;
        if (active(drawStart, now, drawSeconds)) return switchedItem ? State.SWITCHING : State.EQUIPPING;
        if (active(inspectStart, now, INSPECT_SECONDS)) return State.INSPECTING;
        if (!onGround) return verticalVelocity >= 0 ? State.JUMPING : State.FALLING;
        if (crouching) return State.CROUCHING;
        if (sprinting && moving > 0.15) return State.SPRINTING;
        if (moving > 0.05) return State.WALKING;
        return State.IDLE;
    }

    /** Feeds the camera angles once per frame; turning drags the arm the other way, then it springs back. */
    public void look(float yaw, float pitch, double dtSeconds, double amount) {
        if (!hasLook) {
            lastYaw = yaw;
            lastPitch = pitch;
            hasLook = true;
        }
        double dYaw = wrap(yaw - lastYaw), dPitch = pitch - lastPitch;
        lastYaw = yaw;
        lastPitch = pitch;
        double k = 1 - Math.exp(-Math.max(0, dtSeconds) * 9);
        double targetYaw = clamp(-dYaw * 0.9 * amount, INERTIA_LIMIT);
        double targetPitch = clamp(-dPitch * 0.7 * amount, INERTIA_LIMIT);
        inertiaYaw += (targetYaw - inertiaYaw) * k;
        inertiaPitch += (targetPitch - inertiaPitch) * k;
    }

    /**
     * The combined offset for this frame. {@code idle} scales breathing and inertia, {@code walk} the step bob,
     * {@code walkPhase} is the player's interpolated walk distance and {@code moving} 0..1 how fast they move.
     */
    public Pose pose(long now, double drawSeconds, double idle, double walk, double walkPhase, double moving,
                     boolean reducedMotion) {
        return pose(now, drawSeconds, idle, walk, walkPhase, moving, false, false, true, 0.0, reducedMotion);
    }

    /**
     * Full state-blended first-person pose including sprint cant, crouch tuck, jump/fall vertical inertia,
     * equip/switch draw, unequip lower, alternating slash, and inspect.
     */
    public Pose pose(long now, double drawSeconds, double idle, double walk, double walkPhase, double moving,
                     boolean sprinting, boolean crouching, boolean onGround, double verticalVelocity,
                     boolean reducedMotion) {
        if (reducedMotion) return Pose.REST;
        double dt = lastPoseNanos == 0L ? 0.016 : Math.max(0.0, Math.min(0.1, (now - lastPoseNanos) / 1e9));
        lastPoseNanos = now;
        double blend = 1.0 - Math.exp(-dt * 12.0);
        sprintWeight += ((sprinting && moving > 0.15 ? 1.0 : 0.0) - sprintWeight) * blend;
        crouchWeight += ((crouching ? 1.0 : 0.0) - crouchWeight) * blend;
        double targetAir = onGround ? 0.0 : clamp(-verticalVelocity * 0.065, 0.055);
        airOffset += (targetAir - airOffset) * blend;

        double seconds = now / 1e9;
        Pose pose = new Pose(0, Math.sin(seconds * 1.5) * 0.005 * idle, 0,
                Math.sin(seconds * 1.5 + 1) * 0.6 * idle, inertiaYaw * idle, Math.sin(seconds * 1.1) * 0.8 * idle, 0)
                .plus(new Pose(inertiaYaw * 0.004 * idle, 0, 0, inertiaPitch * idle, 0, 0, 0));
        double step = Math.PI * walkPhase;
        double sprintBoost = 1.0 + sprintWeight * 0.55;
        pose = pose.plus(new Pose(Math.sin(step) * 0.012 * sprintBoost, -Math.abs(Math.cos(step)) * 0.014 * sprintBoost, 0,
                sprintWeight * -8.0, sprintWeight * 6.0, Math.sin(step) * 1.4 * sprintBoost - sprintWeight * 5.0, 0)
                .times(walk * clamp01(moving)));
        if (crouchWeight > 1e-4) {
            pose = pose.plus(new Pose(-0.018, -0.028, -0.015, 6.0, -5.0, -8.0, 0).times(crouchWeight));
        }
        if (Math.abs(airOffset) > 1e-4) {
            pose = pose.plus(new Pose(0, airOffset, 0, airOffset * 180.0, 0, airOffset * 90.0, 0));
        }
        if (active(unequipStart, now, UNEQUIP_SECONDS)) {
            double u = progress(unequipStart, now, UNEQUIP_SECONDS);
            double ease = u * u * (3 - 2 * u);
            pose = pose.plus(new Pose(0.02 * ease, -0.48 * ease, 0.04 * ease, 45 * ease, 0, -18 * ease, 0));
        } else if (active(drawStart, now, drawSeconds)) {
            pose = pose.plus(draw(progress(drawStart, now, drawSeconds)));
        }
        if (active(slashStart, now, SLASH_SECONDS)) {
            pose = pose.plus(slash(progress(slashStart, now, SLASH_SECONDS), slashDir));
        } else if (active(inspectStart, now, INSPECT_SECONDS)) {
            pose = pose.plus(inspect(progress(inspectStart, now, INSPECT_SECONDS)));
        }
        return pose;
    }

    /** Raised from below the screen while the blade twirls once into the grip. */
    static Pose draw(double t) {
        double e = 1 - Math.pow(1 - t, 3);
        double settle = Math.sin(Math.PI * t) * (1 - t);
        return new Pose(0.02 * settle, -0.55 * (1 - e), 0.05 * (1 - e), 55 * (1 - e) - 6 * settle, 0,
                -20 * (1 - e), 360 * (1 - e));
    }

    /**
     * A diagonal cut across the screen. {@code dir} +1 sweeps right-to-left, -1 left-to-right. The envelope is
     * zero at both ends, so a slash starts and finishes exactly at rest.
     */
    static Pose slash(double t, int dir) {
        double s = 1 - Math.pow(1 - t, 3);
        double b = Math.sin(Math.PI * t);
        return new Pose(dir * (0.16 - 0.44 * s) * b, 0.07 * b, -0.13 * b, -16 * b,
                dir * (28 - 74 * s) * b, dir * (-32 + 64 * s) * b, 0);
    }

    private static final double[] INSPECT_T = {0, 0.22, 0.45, 0.62, 0.80, 1};
    private static final Pose[] INSPECT_KEYS = {
            Pose.REST,
            new Pose(-0.18, 0.05, 0.02, -6, -38, 72, 0),
            new Pose(-0.18, 0.05, 0.02, -6, -38, -64, 0),
            new Pose(-0.10, 0.03, 0, -10, -20, 0, 360),
            new Pose(-0.03, 0.01, 0, -3, -6, 6, 360),
            new Pose(0, 0, 0, 0, 0, 0, 360),
    };

    /** Blade turned flat to the camera, rolled to show the other face, then flipped once and returned. */
    static Pose inspect(double t) {
        for (int i = 1; i < INSPECT_T.length; i++) {
            if (t <= INSPECT_T[i]) {
                double local = (t - INSPECT_T[i - 1]) / (INSPECT_T[i] - INSPECT_T[i - 1]);
                double w = local * local * (3 - 2 * local);
                return INSPECT_KEYS[i - 1].times(1 - w).plus(INSPECT_KEYS[i].times(w));
            }
        }
        return INSPECT_KEYS[INSPECT_KEYS.length - 1];
    }

    private static boolean active(long start, long now, double seconds) {
        return start >= 0 && now - start < seconds * 1e9;
    }

    private static double progress(long start, long now, double seconds) {
        return clamp01((now - start) / 1e9 / Math.max(1e-3, seconds));
    }

    private static double clamp(double v, double limit) { return Math.max(-limit, Math.min(limit, v)); }

    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }

    private static double wrap(double degrees) {
        double d = degrees % 360;
        if (d >= 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }
}
