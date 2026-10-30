package dev.jilaton.client.music;

import net.minecraft.resources.Identifier;

/**
 * Immutable snapshot of what a music source reports, shared by the HUD, the Music Center screen and the
 * world-space visuals. {@code positionMs} is extrapolated from the capture time while playing.
 */
public record NowPlaying(String source, String title, String artist, String album, long positionMs, long durationMs,
                         boolean playing, Identifier artwork, String device, String status, boolean connected,
                         boolean lyricsAllowed, boolean visualSyncAllowed, long capturedNanos) {
    public static NowPlaying idle(String source, String status) {
        return new NowPlaying(source, "", "", "", 0, 0, false, null, "", status, false, false, false, System.nanoTime());
    }

    public boolean hasTrack() { return title != null && !title.isEmpty(); }

    /** Playback position now, advancing locally between source updates. */
    public long positionNow(long nowNanos) {
        if (!playing) return positionMs;
        long advanced = positionMs + (nowNanos - capturedNanos) / 1_000_000L;
        return durationMs > 0 ? Math.min(durationMs, advanced) : advanced;
    }

    public double progress(long nowNanos) {
        return durationMs <= 0 ? 0 : Math.max(0, Math.min(1, positionNow(nowNanos) / (double) durationMs));
    }

    public static String time(long ms) {
        long seconds = Math.max(0, ms / 1000);
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }
}
