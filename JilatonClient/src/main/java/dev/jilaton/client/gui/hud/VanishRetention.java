package dev.jilaton.client.gui.hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Last-known positions for players who left the world while still listed on the tab list. */
public final class VanishRetention {
    public record Snapshot(UUID id, String name, double x, double y, double z,
                           boolean retained, long lastSeenMillis, long ageMillis) {
    }

    private record Track(String name, double x, double y, double z, long lastSeenMillis, boolean present) {
    }

    private final Map<UUID, Track> tracks = new LinkedHashMap<>();
    private long windowMillis;

    public VanishRetention(long windowMillis) {
        setWindowMillis(windowMillis);
    }

    public long windowMillis() {
        return windowMillis;
    }

    public void setWindowMillis(long windowMillis) {
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("Retention window must be positive");
        }
        this.windowMillis = windowMillis;
    }

    public void seen(UUID id, String name, double x, double y, double z, long nowMillis) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Retained position must be finite");
        }
        tracks.put(id, new Track(name, x, y, z, nowMillis, true));
    }

    /** Marks a tracked player as no longer visible; true while the last sighting is inside the window. */
    public boolean retain(UUID id, long nowMillis) {
        Track track = tracks.get(id);
        if (track == null) {
            return false;
        }
        if (expired(track, nowMillis)) {
            tracks.remove(id);
            return false;
        }
        tracks.put(id, new Track(track.name, track.x, track.y, track.z, track.lastSeenMillis, false));
        return true;
    }

    public void remove(UUID id) {
        tracks.remove(id);
    }

    public boolean isRetained(UUID id) {
        Track track = tracks.get(id);
        return track != null && !track.present;
    }

    public Optional<Snapshot> get(UUID id, long nowMillis) {
        Track track = tracks.get(id);
        if (track == null || (!track.present && expired(track, nowMillis))) {
            return Optional.empty();
        }
        return Optional.of(snapshot(id, track, nowMillis));
    }

    public List<Snapshot> snapshot(long nowMillis) {
        List<Snapshot> entries = new ArrayList<>(tracks.size());
        for (Map.Entry<UUID, Track> entry : tracks.entrySet()) {
            Track track = entry.getValue();
            if (track.present || !expired(track, nowMillis)) {
                entries.add(snapshot(entry.getKey(), track, nowMillis));
            }
        }
        return List.copyOf(entries);
    }

    public List<UUID> ids() {
        return List.copyOf(tracks.keySet());
    }

    public void sweep(long nowMillis) {
        tracks.entrySet().removeIf(entry -> !entry.getValue().present && expired(entry.getValue(), nowMillis));
    }

    public void clear() {
        tracks.clear();
    }

    private boolean expired(Track track, long nowMillis) {
        return Math.max(0, nowMillis - track.lastSeenMillis) > windowMillis;
    }

    private static Snapshot snapshot(UUID id, Track track, long nowMillis) {
        long age = Math.max(0, nowMillis - track.lastSeenMillis);
        return new Snapshot(id, track.name, track.x, track.y, track.z, !track.present, track.lastSeenMillis, age);
    }
}
