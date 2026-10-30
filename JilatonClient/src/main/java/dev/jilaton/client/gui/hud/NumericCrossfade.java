package dev.jilaton.client.gui.hud;

import dev.jilaton.client.gui.Motion;

/**
 * 120 ms crossfade between two successive renderings of one HUD number. The caller keeps the strings
 * cached and only calls {@link #update} with an existing string, so the draw path never allocates.
 */
public final class NumericCrossfade {
    public static final long DURATION_NANOS = 120_000_000L;

    private String current = "";
    private String previous = "";
    private long startNanos;
    private boolean fading;

    /** Returns true when the visible value changed. Reduced motion swaps instantly with no fade. */
    public boolean update(String next, long now, boolean reducedMotion) {
        if (next == null) {
            throw new IllegalArgumentException("Numeric text is required");
        }
        if (next.equals(current)) {
            finishIfElapsed(now);
            return false;
        }
        if (!current.isEmpty() && !reducedMotion) {
            previous = current;
            startNanos = now;
            fading = true;
        } else {
            previous = "";
            fading = false;
        }
        current = next;
        return true;
    }

    public String current() {
        return current;
    }

    public String previous() {
        return previous;
    }

    public boolean fading() {
        return fading;
    }

    public double currentOpacity(long now) {
        return fading ? progress(now) : 1;
    }

    public double previousOpacity(long now) {
        return fading ? 1 - progress(now) : 0;
    }

    public void reset() {
        current = "";
        previous = "";
        startNanos = 0;
        fading = false;
    }

    private void finishIfElapsed(long now) {
        if (fading && progress(now) >= 1) {
            previous = "";
            fading = false;
        }
    }

    private double progress(long now) {
        if (!fading) {
            return 1;
        }
        double elapsed = (now - startNanos) / (double) DURATION_NANOS;
        return Motion.STANDARD.apply(Motion.clamp01(elapsed));
    }
}
