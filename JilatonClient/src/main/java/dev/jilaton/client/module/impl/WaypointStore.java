package dev.jilaton.client.module.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Waypoints saved per world and dimension in a plain tab-separated file, one per line:
 * {@code world	dimension	name	x	y	z	rgb}. Names are unique per world and dimension, case-insensitively.
 */
public final class WaypointStore {
    public record Waypoint(String world, String dimension, String name, int x, int y, int z, int rgb) { }

    private final Path file;
    private final List<Waypoint> all = new ArrayList<>();

    public WaypointStore(Path file) {
        this.file = file;
        load();
    }

    public List<Waypoint> in(String world, String dimension) {
        List<Waypoint> out = new ArrayList<>();
        for (Waypoint w : all) if (w.world.equals(world) && w.dimension.equals(dimension)) out.add(w);
        return out;
    }

    /** Adds or moves the waypoint with this name. */
    public void put(Waypoint waypoint) {
        remove(waypoint.world, waypoint.dimension, waypoint.name);
        all.add(waypoint);
        save();
    }

    public boolean remove(String world, String dimension, String name) {
        boolean removed = all.removeIf(w -> w.world.equals(world) && w.dimension.equals(dimension)
                && w.name.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)));
        if (removed) save();
        return removed;
    }

    public void clear(String world, String dimension) {
        if (all.removeIf(w -> w.world.equals(world) && w.dimension.equals(dimension))) save();
    }

    private void load() {
        all.clear();
        if (file == null || !Files.isRegularFile(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t");
                if (p.length != 7) continue;
                try {
                    all.add(new Waypoint(p[0], p[1], p[2], Integer.parseInt(p[3]), Integer.parseInt(p[4]),
                            Integer.parseInt(p[5]), Integer.parseInt(p[6], 16)));
                } catch (NumberFormatException ignored) {
                    // A hand-edited line we cannot read is skipped rather than dropping the whole file.
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void save() {
        if (file == null) return;
        StringBuilder out = new StringBuilder();
        for (Waypoint w : all) {
            out.append(clean(w.world)).append('\t').append(clean(w.dimension)).append('\t').append(clean(w.name)).append('\t')
                    .append(w.x).append('\t').append(w.y).append('\t').append(w.z).append('\t')
                    .append(String.format(Locale.ROOT, "%06X", w.rgb & 0xFFFFFF)).append('\n');
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, out, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static String clean(String s) {
        return s.replace('\t', ' ').replace('\n', ' ');
    }
}
