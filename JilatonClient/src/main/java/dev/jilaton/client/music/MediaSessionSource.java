package dev.jilaton.client.music;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Reads the desktop player's Windows media session (the data shown in the Windows volume flyout) through a
 * small bundled PowerShell bridge. Works with the Spotify desktop app with no developer app, login or API
 * key, and controls play/pause/skip on that app directly, so audio always comes from Spotify itself. The
 * bridge process only runs while this source is selected and exits on its own if Minecraft exits.
 */
public final class MediaSessionSource {
    private static final Logger LOGGER = LoggerFactory.getLogger("Jilaton Media");
    private final Path scriptDir;
    private volatile Process process;
    private volatile Path commandFile;
    private volatile NowPlaying state = NowPlaying.idle("Spotify", "Starting media bridge…");
    private volatile String artKey;
    private volatile long lastLineNanos;
    private long restartAfter;
    /** Consecutive bridge starts that never produced output; drives the restart back-off. */
    private int failedStarts;
    private String filter = "Spotify";
    private volatile boolean onlineLookups = true;

    public MediaSessionSource(Path configDir) {
        scriptDir = configDir.resolve("jilaton");
    }

    public static boolean supported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public NowPlaying state() { return state; }

    /** Whether a missing cover may be looked up online (iTunes Search), which sends the title and artist. */
    public void setOnlineLookups(boolean allowed) { onlineLookups = allowed; }

    public void setFilter(String filter) {
        if (!filter.equals(this.filter)) { this.filter = filter; stop(); }
    }

    /** Called each client tick while selected: keeps exactly one bridge alive and restarts it if it dies. */
    public void tick() {
        if (!supported()) { state = NowPlaying.idle("Spotify", "Windows media sessions are only available on Windows"); return; }
        Process p = process;
        if (p != null && p.isAlive()) {
            if (lastLineNanos != 0) failedStarts = 0;
            if (lastLineNanos != 0 && System.nanoTime() - lastLineNanos > 10_000_000_000L) stop();
            refreshArtwork();
            return;
        }
        if (System.nanoTime() < restartAfter) return;
        // 5 s, doubling per start that never spoke, capped at 5 min: a blocked PowerShell is not respawned forever.
        restartAfter = System.nanoTime() + restartDelayNanos(failedStarts);
        failedStarts++;
        start();
    }

    static long restartDelayNanos(int failedStarts) {
        return Math.min(300_000_000_000L, 5_000_000_000L << Math.min(Math.max(failedStarts, 0), 6));
    }

    private void start() {
        try {
            Path script = scriptDir.resolve("media-session.ps1");
            Files.createDirectories(scriptDir);
            try (InputStream in = MediaSessionSource.class.getResourceAsStream("/assets/jilaton/scripts/media-session.ps1")) {
                if (in == null) throw new IOException("bridge script missing from the mod");
                Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
            }
            Path artDir = scriptDir.resolve("art-cache");
            Files.createDirectories(artDir);
            Path commands = scriptDir.resolve("media-command.txt");
            Files.deleteIfExists(commands);
            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-File", script.toString(), "-Filter", filter, "-ArtDir", artDir.toString(), "-IntervalMs", "500",
                    "-CommandFile", commands.toString(), "-ParentPid", Long.toString(ProcessHandle.current().pid()));
            builder.redirectInput(ProcessBuilder.Redirect.from(new java.io.File("NUL")));
            builder.redirectErrorStream(false);
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = builder.start();
            process = p;
            commandFile = commands;
            lastLineNanos = 0;
            Thread reader = new Thread(() -> read(p), "Jilaton media bridge");
            reader.setDaemon(true);
            reader.start();
        } catch (IOException exception) {
            state = NowPlaying.idle("Spotify", "Media bridge failed: " + exception.getMessage());
            LOGGER.warn("Could not start the media-session bridge", exception);
        }
    }

    private void read(Process p) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastLineNanos = System.nanoTime();
                if (!line.startsWith("{")) continue;
                try { accept(JsonParser.parseString(line).getAsJsonObject()); }
                catch (RuntimeException exception) { LOGGER.debug("Ignoring bridge line {}", line); }
            }
        } catch (IOException ignored) {
        }
    }

    private void accept(JsonObject json) {
        if (!json.has("active") || !json.get("active").getAsBoolean()) {
            String error = json.has("error") ? json.get("error").getAsString() : "";
            state = NowPlaying.idle("Spotify", error.isEmpty()
                    ? (filter.isEmpty() ? "No media is playing" : "Spotify is not open — press Play to launch it") : error);
            return;
        }
        String title = str(json, "title"), artist = str(json, "artist"), album = str(json, "album");
        String art = str(json, "art");
        String key = title + "|" + artist;
        if (!key.equals(artKey) && !title.isEmpty()) {
            artKey = key;
            boolean loaded = false;
            if (!art.isEmpty()) {
                try { Artwork.load("media:" + key, Files.readAllBytes(Path.of(art))); loaded = true; } catch (IOException | RuntimeException ignored) { }
            }
            // Windows PowerShell often cannot read the session thumbnail stream; fall back to a keyless
            // iTunes Search lookup for the cover (this thread is already a background reader).
            if (!loaded && onlineLookups) {
                try { Artwork.load("media:" + key, CoverLookup.find(title, artist)); } catch (IOException | RuntimeException ignored) { }
            }
        }
        boolean playing = "Playing".equals(str(json, "status"));
        String app = str(json, "app");
        String device = app.toLowerCase(Locale.ROOT).contains("spotify") ? "Spotify · This PC" : app.replace(".exe", "");
        state = new NowPlaying("Spotify", title, artist, album, json.get("positionMs").getAsLong(), json.get("durationMs").getAsLong(),
                playing, Artwork.get("media:" + key), device, playing ? "Playing" : str(json, "status"), true, true, false, System.nanoTime());
    }

    private void refreshArtwork() {
        NowPlaying s = state;
        if (artKey != null && s.artwork() == null && Artwork.get("media:" + artKey) != null)
            state = new NowPlaying(s.source(), s.title(), s.artist(), s.album(), s.positionMs(), s.durationMs(), s.playing(),
                    Artwork.get("media:" + artKey), s.device(), s.status(), s.connected(), s.lyricsAllowed(), false, s.capturedNanos());
    }

    /** Sends a transport command; if Spotify is not running, opens the desktop app instead. */
    public void control(String command) {
        if (!state.hasTrack() && filter.equals("Spotify") && !command.equals("previous")) {
            Util.getPlatform().openUri(URI.create("spotify:"));
            state = NowPlaying.idle("Spotify", "Opening Spotify…");
            return;
        }
        Path file = commandFile;
        if (file == null) return;
        try {
            Files.writeString(file, command);
        } catch (IOException exception) {
            stop();
        }
    }

    public void stop() {
        Process p = process;
        process = null;
        Path file = commandFile;
        commandFile = null;
        if (file != null) try { Files.deleteIfExists(file); } catch (IOException ignored) { }
        if (p != null) p.destroy();
        restartAfter = 0;
    }

    private static String str(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }
}
