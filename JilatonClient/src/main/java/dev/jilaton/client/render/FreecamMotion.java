package dev.jilaton.client.render;

import net.minecraft.world.phys.Vec3;

/** Detached position state. No player/entity coordinates are written by this class. */
public final class FreecamMotion {
    private Vec3 previous = Vec3.ZERO, position = Vec3.ZERO;
    private final FreeLookRotation rotation = new FreeLookRotation();

    public void reset(Vec3 position, float yaw, float pitch) {
        this.previous = this.position = position;
        rotation.reset(yaw, pitch);
    }

    public void turn(double x, double y) { rotation.turn(x, y, 1, true); }

    public void step(double forward, double left, double rise, double speed, double vertical, boolean smooth) {
        previous = position;
        double radians = Math.toRadians(rotation.yaw());
        double step = speed * (smooth ? 0.5 : 1);
        position = position.add((-Math.sin(radians) * forward + Math.cos(radians) * left) * step,
                rise * step * vertical, (Math.cos(radians) * forward + Math.sin(radians) * left) * step);
    }

    public void pause() { previous = position; }
    public Vec3 position(float partial) { return previous.lerp(position, Math.max(0, Math.min(1, partial))); }
    public Vec3 current() { return position; }
    public float yaw() { return rotation.yaw(); }
    public float pitch() { return rotation.pitch(); }
    public static double scrollSpeed(double value, double amount, double minimum, double maximum) {
        if (!Double.isFinite(value) || !Double.isFinite(amount) || amount == 0) return value;
        return Math.max(minimum, Math.min(maximum, value * (amount > 0 ? 1.2 : 1 / 1.2)));
    }

    public static ChunkWindow chunkWindow(double x, double z, int bodyX, int bodyZ, int preference) {
        int cameraX = (int) Math.floor(x) >> 4, cameraZ = (int) Math.floor(z) >> 4;
        int span = Math.max(Math.abs(cameraX - bodyX), Math.abs(cameraZ - bodyZ));
        int distance = Math.max(2, Math.min(32, preference));
        int centerX = span > distance ? bodyX + (cameraX - bodyX) / 2 : cameraX;
        int centerZ = span > distance ? bodyZ + (cameraZ - bodyZ) / 2 : cameraZ;
        return new ChunkWindow(centerX, centerZ, Math.max(2, Math.min(32, Math.max(distance, span / 2 + 3))));
    }

    public record ChunkWindow(int x, int z, int radius) { }
}
