/* Adapted from Anubis HandsModule.HandState, Horcus / 4ldenz, GPL-3.0-only.
 * See ATTRIBUTION.md. */
package dev.jilaton.client.render.model;

/** Per-hand, time-based easing; no frame allocations and no dependence on render frequency. */
public final class HandPoseState {
    private final float[] value = {0, 0, 0, 0, 0, 0, 1};
    private long stamp;
    private boolean stamped;
    private boolean rested = true;

    public float value(int axis) { return value[axis]; }
    public boolean rested() { return rested; }

    public void reset() {
        for (int i = 0; i < value.length; i++) value[i] = i == 6 ? 1 : 0;
        stamp = 0;
        stamped = false;
        rested = true;
    }

    public void step(float[] target, boolean on, double glide, long now) {
        float dt = !stamped ? 0 : (float) Math.min(0.1, Math.max(0, (now - stamp) / 1e9));
        stamp = now;
        stamped = true;
        float rate = (float) (30 - 27 * glide);
        float k = glide <= 0 ? 1 : 1 - (float) Math.exp(-rate * dt);
        rested = true;
        for (int i = 0; i < value.length; i++) {
            float rest = i == 6 ? 1 : 0;
            float goal = on ? target[i] : rest;
            float next = value[i] + (goal - value[i]) * k;
            if (Math.abs(goal - next) < 0.001f) next = goal;
            value[i] = next;
            if (next != rest) rested = false;
        }
    }
}
