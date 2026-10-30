package dev.jilaton.client.gui;

/** Bounded one-second window of physical input presses; held buttons add no clicks. */
public final class ClickRate {
    private final long[] presses = new long[256];
    private int start;
    private int size;

    public void press(long now) {
        count(now);
        if (size == presses.length) {
            start = (start + 1) % presses.length;
            size--;
        }
        presses[(start + size) % presses.length] = now;
        size++;
    }

    public int count(long now) {
        while (size > 0 && now - presses[start] >= 1_000_000_000L) {
            start = (start + 1) % presses.length;
            size--;
        }
        return size;
    }

    public void clear() {
        start = 0;
        size = 0;
    }
}
