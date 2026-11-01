package dev.jilaton.client.music;

import java.util.Objects;

/**
 * Keeps a polled source's position steady. Each poll reports a slightly different position (process and network
 * latency), and taking every one at face value makes lyric lines step back and replay their entrance. The clock
 * keeps running locally, eases out small drift, and only jumps for a real seek, a pause/play or a new track.
 */
public final class PlaybackClock {
    /** Larger disagreement than this is treated as a seek and applied at once. */
    static final long SEEK_MS = 600;
    /** Share of the remaining drift corrected per poll. */
    static final double CORRECTION = 0.3;

    private NowPlaying anchor;
    private long lastCaptured = Long.MIN_VALUE;

    public synchronized NowPlaying smooth(NowPlaying reported, long nowNanos) {
        if (!reported.hasTrack()) {
            anchor = null;
            lastCaptured = Long.MIN_VALUE;
            return reported;
        }
        boolean sameTrack = anchor != null && anchor.playing() == reported.playing()
                && Objects.equals(anchor.title(), reported.title()) && Objects.equals(anchor.artist(), reported.artist())
                && Objects.equals(anchor.source(), reported.source());
        if (!sameTrack || !reported.playing()) {
            anchor = reported;
            lastCaptured = reported.capturedNanos();
            return reported;
        }
        if (reported.capturedNanos() != lastCaptured) {
            lastCaptured = reported.capturedNanos();
            long expected = anchor.positionNow(nowNanos);
            long drift = reported.positionNow(nowNanos) - expected;
            long position = Math.abs(drift) > SEEK_MS ? expected + drift : expected + Math.round(drift * CORRECTION);
            anchor = withPosition(reported, position, nowNanos);
        } else if (anchor.artwork() != reported.artwork() || !Objects.equals(anchor.status(), reported.status())) {
            anchor = withPosition(reported, anchor.positionNow(nowNanos), nowNanos);
        }
        return anchor;
    }

    private static NowPlaying withPosition(NowPlaying s, long positionMs, long nowNanos) {
        return new NowPlaying(s.source(), s.title(), s.artist(), s.album(), positionMs, s.durationMs(), s.playing(),
                s.artwork(), s.device(), s.status(), s.connected(), s.lyricsAllowed(), s.visualSyncAllowed(), nowNanos);
    }
}
