package dev.jilaton.client.module.impl;

/**
 * Vanilla projectile motion, one tick at a time: move, then apply drag, then gravity (the order
 * {@code Projectile} subclasses use). Values per projectile kind match 1.21's entities.
 */
final class TrajectoryPhysics {
    record Kind(double speed, double gravity, double drag) { }

    static final Kind ARROW = new Kind(3.0, 0.05, 0.99);
    static final Kind CROSSBOW = new Kind(3.15, 0.05, 0.99);
    static final Kind TRIDENT = new Kind(2.5, 0.05, 0.99);
    static final Kind THROWN = new Kind(1.5, 0.03, 0.99);
    static final Kind POTION = new Kind(0.5, 0.05, 0.99);

    private TrajectoryPhysics() {
    }

    /** Bow power for a draw time in ticks, as {@code BowItem.getPowerForTime}. */
    static float bowPower(int ticks) {
        float f = ticks / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }

    /** Initial velocity along yaw/pitch (degrees) at {@code speed} blocks per tick. */
    static double[] launch(float yaw, float pitch, double speed) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new double[]{-Math.sin(y) * Math.cos(p) * speed, -Math.sin(p) * speed, Math.cos(y) * Math.cos(p) * speed};
    }

    /** Advances {@code pos}/{@code vel} by one tick in place (drag {@code water ? 0.6 : kind.drag}). */
    static void step(double[] pos, double[] vel, Kind kind, boolean water) {
        pos[0] += vel[0];
        pos[1] += vel[1];
        pos[2] += vel[2];
        double drag = water ? 0.6 : kind.drag();
        vel[0] *= drag;
        vel[1] = vel[1] * drag - kind.gravity();
        vel[2] *= drag;
    }
}
