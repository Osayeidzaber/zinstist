package dev.jilaton.client.music;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * User-provided music folder. Each subfolder is a playlist; files in the root form "Loose tracks".
 * Artwork and lyrics are discovered next to the audio: {@code <name>.png|jpg}, {@code cover.*} or
 * {@code folder.*}, and {@code <name>.lrc}. Nothing outside the chosen folder is read.
 */
public final class LocalLibrary {
    public record Track(Path file, String title, String artist, String album, Path artwork, Path lyrics, boolean supported) {
        public String display() { return artist.isEmpty() ? title : artist + " — " + title; }
    }

    private static final String[] IMAGE_EXT = {".png", ".jpg", ".jpeg"};
    private final Map<String, List<Track>> playlists = new LinkedHashMap<>();
    private Path root;

    public synchronized Map<String, List<Track>> playlists() { return Map.copyOf(playlists); }
    public synchronized List<String> playlistNames() { return List.copyOf(playlists.keySet()); }
    public synchronized List<Track> playlist(String name) { return playlists.getOrDefault(name, List.of()); }
    public Path root() { return root; }

    public synchronized List<Track> all() {
        List<Track> all = new ArrayList<>();
        for (List<Track> list : playlists.values()) all.addAll(list);
        return all;
    }

    /** Scans {@code folder} (creating it when missing) up to one level of subfolders. */
    public synchronized void scan(Path folder) throws IOException {
        root = folder;
        playlists.clear();
        Files.createDirectories(folder);
        List<Track> loose = tracksIn(folder);
        if (!loose.isEmpty()) playlists.put("Loose tracks", loose);
        try (Stream<Path> children = Files.list(folder)) {
            for (Path dir : children.filter(Files::isDirectory).sorted().toList()) {
                List<Track> tracks = tracksIn(dir);
                if (!tracks.isEmpty()) playlists.put(dir.getFileName().toString(), tracks);
            }
        }
    }

    private static List<Track> tracksIn(Path dir) throws IOException {
        List<Track> tracks = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(Files::isRegularFile).sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT))).toList()) {
                String name = file.getFileName().toString();
                String lower = name.toLowerCase(Locale.ROOT);
                boolean audio = lower.endsWith(".ogg") || lower.endsWith(".oga") || lower.endsWith(".wav")
                        || lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".m4a");
                if (!audio) continue;
                tracks.add(describe(file));
            }
        }
        return tracks;
    }

    static Track describe(Path file) {
        String name = file.getFileName().toString();
        String base = name.substring(0, name.lastIndexOf('.'));
        String title = base, artist = "";
        int dash = base.indexOf(" - ");
        if (dash > 0) { artist = base.substring(0, dash).strip(); title = base.substring(dash + 3).strip(); }
        Path dir = file.getParent();
        String album = dir == null ? "" : dir.getFileName().toString();
        Path art = null;
        for (String candidate : new String[]{base, "cover", "folder", "album"}) {
            for (String ext : IMAGE_EXT) {
                Path p = dir.resolve(candidate + ext);
                if (Files.isRegularFile(p)) { art = p; break; }
            }
            if (art != null) break;
        }
        Path lrc = dir.resolve(base + ".lrc");
        if (!Files.isRegularFile(lrc)) lrc = dir.resolve("lyrics").resolve(base + ".lrc");
        return new Track(file, title, artist, album, art, Files.isRegularFile(lrc) ? lrc : null, AudioStream.supported(file));
    }

    /** Applies tags read from the decoder (title/artist/album) over filename-derived values. */
    static Track withTags(Track track, Map<String, String> tags) {
        String title = tags.getOrDefault("TITLE", track.title());
        String artist = tags.getOrDefault("ARTIST", track.artist());
        String album = tags.getOrDefault("ALBUM", track.album());
        return new Track(track.file(), title, artist, album, track.artwork(), track.lyrics(), track.supported());
    }
}
