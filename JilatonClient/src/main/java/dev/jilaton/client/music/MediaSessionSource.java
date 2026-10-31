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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Reads the desktop player's media session (the data shown in the Windows volume flyout) through a
 * small bundled PowerShell bridge, or AppleScript on macOS. The bridge is a privacy-sensitive feature: it is
 * only ever created after an explicit opt-in ({@link #setEnabled(boolean)}, persisted in
 * {@code ClientConfig}), it is never copied or spawned while that opt-in is off, and it is destroyed on
 * disable, source change and shutdown.
 *
 * <p>All file and process work happens on the {@code Jilaton media bridge control} worker; the client thread
 * only reads the immutable {@link NowPlaying} snapshot. Starts are bounded: after
 * {@link MediaBridgePolicy#MAX_CONSECUTIVE_STARTS} consecutive starts without a line the bridge gives up and
 * stays off until the user disables and re-enables it.
 */
public final class MediaSessionSource {
    private static final Logger LOGGER = LoggerFactory.getLogger("Jilaton Media");
    private final Path scriptDir;
    private final ExecutorService lifecycle = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Jilaton media bridge control");
        thread.setDaemon(true);
        return thread;
    });
    private final Object lock = new Object();
    private volatile Process process;
    private volatile Path commandFile;
    private volatile Path scriptFile;
    private volatile NowPlaying state = NowPlaying.idle("Spotify", MediaBridgePolicy.OFF_STATUS);
    private volatile String artKey;
    private volatile long lastLineNanos;
    private volatile long startedNanos;
    private volatile boolean starting;
    private volatile boolean enabled;
    private long restartAfter;
    /** Starts since the bridge last produced a line; drives the bounded restart policy. */
    private int consecutiveStarts;
    /** Incremented on every stop so a start that finishes late can never adopt its process. */
    private long generation;
    private String filter = "Spotify";
    private volatile boolean onlineLookups;

    public MediaSessionSource(Path configDir) {
        this(configDir, false);
    }

    public MediaSessionSource(Path configDir, boolean enabled) {
        this.scriptDir = configDir.resolve("jilaton");
        this.enabled = enabled;
        state = NowPlaying.idle("Spotify", MediaBridgePolicy.idleStatus(enabled, supported()));
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    public static boolean supported() {
        return isWindows() || isMac();
    }

    public NowPlaying state() { return state; }

    public boolean enabled() { return enabled; }

    /** The user's opt-in gate. Disabling destroys the process and leaves the status line "bridge is off". */
    public void setEnabled(boolean enable) {
        if (this.enabled == enable) return;
        this.enabled = enable;
        if (!enable) {
            stop();
            artKey = null;
            state = NowPlaying.idle("Spotify", MediaBridgePolicy.OFF_STATUS);
        } else {
            consecutiveStarts = 0;
            restartAfter = 0;
            state = NowPlaying.idle("Spotify", "Starting media bridge…");
        }
    }

    /** Whether a missing cover may be looked up online (iTunes Search), which sends the title and artist. */
    public void setOnlineLookups(boolean allowed) { onlineLookups = allowed; }

    public void setFilter(String filter) {
        if (!filter.equals(this.filter)) { this.filter = filter; stop(); }
    }

    /** Called each client tick while selected: keeps exactly one bridge alive and restarts it if it dies. */
    public void tick() {
        long now = System.nanoTime();
        if (!enabled) {
            state = NowPlaying.idle("Spotify", MediaBridgePolicy.OFF_STATUS);
            return;
        }
        if (!supported()) {
            state = NowPlaying.idle("Spotify", MediaBridgePolicy.UNSUPPORTED_STATUS);
            return;
        }
        Process p = process;
        boolean alive = p != null && p.isAlive();
        MediaBridgePolicy.Decision decision =
                MediaBridgePolicy.decide(true, true, alive, now, restartAfter, consecutiveStarts);
        if (decision == MediaBridgePolicy.Decision.RUNNING) {
            if (lastLineNanos != 0) consecutiveStarts = 0;
            if (MediaBridgePolicy.stale(lastLineNanos, startedNanos, now)) {
                // A bridge that went silent counts as one failed start; the policy bounds the total.
                consecutiveStarts++;
                restartAfter = now + MediaBridgePolicy.restartDelayNanos(consecutiveStarts);
                destroyProcess();
                return;
            }
            refreshArtwork();
            return;
        }
        if (starting) return;
        switch (decision) {
            case GAVE_UP -> state = NowPlaying.idle("Spotify", MediaBridgePolicy.GAVE_UP_STATUS);
            case SPAWN -> {
                consecutiveStarts++;
                restartAfter = now + MediaBridgePolicy.restartDelayNanos(consecutiveStarts);
                starting = true;
                long generationSnapshot;
                synchronized (lock) { generationSnapshot = generation; }
                try {
                    lifecycle.execute(() -> start(generationSnapshot));
                } catch (RejectedExecutionException shuttingDown) {
                    starting = false;
                }
            }
            default -> { }
        }
    }

    static long restartDelayNanos(int failedStarts) {
        return MediaBridgePolicy.restartDelayNanos(failedStarts);
    }

    private void start(long generationSnapshot) {
        Process p = null;
        try {
            Files.createDirectories(scriptDir);
            Path commands = scriptDir.resolve("media-command.txt");
            Files.deleteIfExists(commands);
            ProcessBuilder builder;
            Path script;
            if (isMac()) {
                script = scriptDir.resolve("media-session-mac.sh");
                Files.writeString(script, macBridgeScript(commands.toString()));
                builder = new ProcessBuilder("/bin/sh", script.toString());
                builder.redirectInput(ProcessBuilder.Redirect.from(new java.io.File("/dev/null")));
            } else {
                script = scriptDir.resolve("media-session.ps1");
                try (InputStream in = MediaSessionSource.class.getResourceAsStream("/assets/jilaton/scripts/media-session.ps1")) {
                    if (in == null) throw new IOException("bridge script missing from the mod");
                    Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
                }
                Path artDir = scriptDir.resolve("art-cache");
                Files.createDirectories(artDir);
                builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                        "-File", script.toString(), "-Filter", filter, "-ArtDir", artDir.toString(), "-IntervalMs", "500",
                        "-CommandFile", commands.toString(), "-ParentPid", Long.toString(ProcessHandle.current().pid()));
                builder.redirectInput(ProcessBuilder.Redirect.from(new java.io.File("NUL")));
            }
            builder.redirectErrorStream(false);
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            p = builder.start();
            synchronized (lock) {
                // The opt-in may have been revoked (or the source changed) while the process was starting.
                if (generationSnapshot != generation || !enabled) {
                    p.destroy();
                    return;
                }
                process = p;
                commandFile = commands;
                scriptFile = script;
                lastLineNanos = 0;
                startedNanos = System.nanoTime();
                starting = false;
            }
            Process startedProcess = p;
            Thread reader = new Thread(() -> read(startedProcess), "Jilaton media bridge");
            reader.setDaemon(true);
            reader.start();
        } catch (IOException exception) {
            synchronized (lock) {
                if (generationSnapshot == generation) {
                    starting = false;
                    state = NowPlaying.idle("Spotify", "Media bridge failed: " + exception.getMessage());
                }
            }
            LOGGER.warn("Could not start the media-session bridge", exception);
        } finally {
            synchronized (lock) {
                if (generationSnapshot == generation) starting = false;
            }
        }
    }

    private static String macBridgeScript(String cmdFile) {
        return """
                #!/bin/sh
                CMD_FILE="%s"
                while true; do
                  if [ -f "$CMD_FILE" ]; then
                    CMD=$(cat "$CMD_FILE" 2>/dev/null)
                    rm -f "$CMD_FILE"
                    case "$CMD" in
                      playpause|toggle) osascript -e 'if application "Spotify" is running then tell application "Spotify" to playpause' 2>/dev/null ;;
                      next) osascript -e 'if application "Spotify" is running then tell application "Spotify" to next track' 2>/dev/null ;;
                      previous) osascript -e 'if application "Spotify" is running then tell application "Spotify" to previous track' 2>/dev/null ;;
                    esac
                  fi
                  OUT=$(osascript -e '
                    if application "Spotify" is running then
                      tell application "Spotify"
                        set st to player state as string
                        if st is "playing" or st is "paused" then
                          set t to name of current track
                          set a to artist of current track
                          set al to album of current track
                          set d to duration of current track
                          set p to (player position * 1000) as integer
                          set sStr to "Paused"
                          if st is "playing" then set sStr to "Playing"
                          return "{\\"active\\":true,\\"app\\":\\"Spotify\\",\\"status\\":\\"" & sStr & "\\",\\"title\\":\\"" & t & "\\",\\"artist\\":\\"" & a & "\\",\\"album\\":\\"" & al & "\\",\\"positionMs\\":" & p & ",\\"durationMs\\":" & d & ",\\"art\\":\\"\\"}"
                        end if
                      end tell
                    end if
                    return "{\\"active\\":false}"
                  ' 2>/dev/null)
                  if [ -n "$OUT" ]; then
                    echo "$OUT"
                  else
                    echo '{"active":false}'
                  fi
                  sleep 1
                done
                """.formatted(cmdFile.replace("\"", "\\\""));
    }

    private void read(Process p) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!enabled || p != process) return;
                lastLineNanos = System.nanoTime();
                if (!line.startsWith("{")) continue;
                try { accept(JsonParser.parseString(line).getAsJsonObject()); }
                catch (RuntimeException exception) { LOGGER.debug("Ignoring bridge line {}", line); }
            }
        } catch (IOException ignored) {
        }
    }

    private void accept(JsonObject json) {
        if (!enabled) return;
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
        if (!enabled) return;
        if (!state.hasTrack() && filter.equals("Spotify") && !command.equals("previous")) {
            state = NowPlaying.idle("Spotify", "Opening Spotify…");
            // Launching a process is IO: keep it on the control worker like every other bridge action.
            submit(() -> {
                if (enabled) Util.getPlatform().openUri(URI.create("spotify:"));
            });
            return;
        }
        submit(() -> writeCommand(command));
    }

    /** Command-file writes happen on the lifecycle worker, never on the client thread. */
    private void writeCommand(String command) {
        if (!enabled) return;
        Path file = commandFile;
        if (file == null) return;
        try {
            Files.writeString(file, command);
        } catch (IOException exception) {
            stop();
        }
    }

    /** Destroys the process and resets the retry budget; the next opt-in starts from a clean slate. */
    public void stop() {
        Process p;
        Path file;
        Path script;
        synchronized (lock) {
            generation++;
            p = process;
            process = null;
            file = commandFile;
            commandFile = null;
            script = scriptFile;
            scriptFile = null;
            starting = false;
            restartAfter = 0;
            consecutiveStarts = 0;
            startedNanos = 0;
            lastLineNanos = 0;
        }
        if (p != null) p.destroy();
        deleteLater(file, script);
    }

    /** Kills a hung process but keeps the failure count so the bounded policy still applies. */
    private void destroyProcess() {
        Process p;
        Path file;
        Path script;
        synchronized (lock) {
            generation++;
            p = process;
            process = null;
            file = commandFile;
            commandFile = null;
            script = scriptFile;
            scriptFile = null;
            starting = false;
            startedNanos = 0;
            lastLineNanos = 0;
        }
        if (p != null) p.destroy();
        deleteLater(file, script);
    }

    private void deleteLater(Path... files) {
        if (files.length == 0) return;
        Runnable cleanup = () -> {
            for (Path file : files) {
                if (file == null) continue;
                try { Files.deleteIfExists(file); } catch (IOException ignored) { }
            }
        };
        submit(cleanup);
    }

    private void submit(Runnable task) {
        try {
            lifecycle.execute(task);
        } catch (RejectedExecutionException shuttingDown) {
            task.run();
        }
    }

    /** Stops the bridge and its worker; used when the client shuts down. */
    public void shutdown() {
        setEnabled(false);
        stop();
        lifecycle.shutdownNow();
    }

    private static String str(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }
}
