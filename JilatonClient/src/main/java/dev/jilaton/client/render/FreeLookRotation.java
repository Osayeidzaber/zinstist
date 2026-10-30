package dev.jilaton.client.render;

/** Camera-only mouse accumulation, in the same units as Entity.turn. */
public final class FreeLookRotation {
    private float yaw;
    private float pitch;

    public void reset(float yaw, float pitch) { this.yaw = yaw; this.pitch = pitch; }

    public void turn(double x, double y, double sensitivity, boolean limitPitch) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(sensitivity)) return;
        yaw = wrap(yaw + x * 0.15 * sensitivity);
        double nextPitch = pitch + y * 0.15 * sensitivity;
        pitch = limitPitch ? (float) Math.max(-90, Math.min(90, nextPitch)) : wrap(nextPitch);
    }

    private static float wrap(double degrees) {
        double wrapped = degrees % 360;
        return (float) (wrapped >= 180 ? wrapped - 360 : wrapped < -180 ? wrapped + 360 : wrapped);
    }

    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
}
