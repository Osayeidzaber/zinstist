package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec3;

/** Frame-rate independent spring toward a target point, with a snap for teleports and respawns. */
final class SmoothFollow {
    /** Further than this behind and the camera snaps instead of gliding across the world. */
    static final double SNAP_DISTANCE = 6;

    private SmoothFollow() { }

    /**
     * @param smoothing 0 = rigid (vanilla), 1 = loosest trail
     */
    static Vec3 step(Vec3 current, Vec3 target, double dt, double smoothing) {
        if (current == null || smoothing <= 0 || current.distanceToSqr(target) > SNAP_DISTANCE * SNAP_DISTANCE)
            return target;
        // Rate 30/s when barely smoothed down to 4/s at full smoothing.
        double rate = 30 - 26 * Math.max(0, Math.min(1, smoothing));
        double k = 1 - Math.exp(-rate * dt);
        return current.add(target.subtract(current).scale(k));
    }
}
