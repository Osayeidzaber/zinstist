package dev.jilaton.client.gui.hud;

import dev.jilaton.client.gui.AnimatedValue;

/** Horizontal blocks per second measured from successive client positions. */
public final class Speedometer {
    private final AnimatedValue smoothed = new AnimatedValue();
    private double lastX;
    private double lastZ;
    private long lastNanos;
    private double reading;
    private boolean started;

    public double update(double x, double z, long nowNanos, boolean immediate) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Speed sample must be finite");
        }
        if (!started) {
            started = true;
            lastX = x;
            lastZ = z;
            lastNanos = nowNanos;
            reading = smoothed.update(0, nowNanos, true);
            return reading;
        }
        double seconds = (nowNanos - lastNanos) / 1_000_000_000.0;
        if (!(seconds > 0)) {
            return reading;
        }
        double target = Math.hypot(x - lastX, z - lastZ) / seconds;
        lastX = x;
        lastZ = z;
        lastNanos = nowNanos;
        reading = smoothed.update(target, nowNanos, immediate);
        return reading;
    }

    public void reset() {
        started = false;
        reading = 0;
        smoothed.reset();
    }
}
