package dev.jilaton.client.gui;

/** Exponential smoothing based on elapsed time rather than frame count. */
public final class AnimatedValue {
    private double value;
    private long lastTime;
    private boolean initialized;

    public double update(double target, long now, boolean immediate) {
        if (!Double.isFinite(target)) throw new IllegalArgumentException("Animation target must be finite");
        if (!initialized || immediate) {
            value = target;
            initialized = true;
        } else {
            double seconds = Math.max(0, Math.min(0.25, (now - lastTime) / 1_000_000_000.0));
            value += (target - value) * -Math.expm1(-12.0 * seconds);
        }
        lastTime = now;
        return value;
    }

    public void reset() { initialized = false; }
}
