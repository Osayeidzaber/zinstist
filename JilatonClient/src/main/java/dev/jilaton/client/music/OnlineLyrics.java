package dev.jilaton.client.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Synced lyrics from LRCLIB (lrclib.net), a free, keyless, community lyrics database. Tracks are matched
 * by title, artist, album and duration, then by scored search ({@link LyricsMatcher}, adapted from Anubis). Results (including "not found") are cached on disk so each song is requested once.
 *
 * <p>Lookups never run on the calling (render) thread. The worker pool has two threads so one slow or failed
 * track cannot hold every other track's lookup behind it. Each key is tried at most
 * {@link #MAX_LOOKUP_FAILURES} times, with a {@link #RETRY_MILLIS} back-off; the in-memory negative cache
 * expires together with the on-disk {@code .none} marker.
 */
public final class OnlineLyrics {
    private static final Logger LOGGER = LoggerFactory.getLogger("Jilaton Lyrics");
    private static final String API = "https://lrclib.net/api/";
    private static final String USER_AGENT = "Jilaton Client 0.1 (Minecraft Fabric mod; https://lrclib.net etiquette)";
    private final Path cacheDir;
    private final Map<String, Lyrics> memory = new ConcurrentHashMap<>();
    private final Map<String, String> status = new ConcurrentHashMap<>();
    /** Failed lookups may be retried after this long; the worker itself never waits. */
    static final long RETRY_MILLIS = 30_000L;
    /** LRCLIB "not found" results are cached on disk for an hour, then looked up again. */
    static final long NEGATIVE_TTL_MILLIS = 3_600_000L;
    /** After this many failed lookups a track is left alone until it changes or the client restarts. */
    static final int MAX_LOOKUP_FAILURES = 3;
    private final Map<String, Long> retryAfter = new ConcurrentHashMap<>();
    private final Map<String, Integer> failures = new ConcurrentHashMap<>();
    /** When an in-memory empty result stops counting as a valid negative cache entry. */
    private final Map<String, Long> negativeUntil = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Executor worker;
    private final boolean ownsWorker;
    private final SyncedFetcher fetcher;

    /** Seam for tests: the network lookup, replaceable without touching LRCLIB. */
    @FunctionalInterface
    interface SyncedFetcher {
        String fetch(String title, String artist, String album, int durationSeconds) throws IOException;
    }

    public OnlineLyrics(Path configDir) {
        this(configDir, Clock.systemUTC(), null, null);
    }

    OnlineLyrics(Path configDir, Clock clock, Executor worker, SyncedFetcher fetcher) {
        cacheDir = configDir.resolve("jilaton").resolve("lyrics-cache");
        this.clock = clock == null ? Clock.systemUTC() : clock;
        if (worker == null) {
            ExecutorService pool = Executors.newFixedThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "Jilaton lyrics");
                thread.setDaemon(true);
                return thread;
            });
            this.worker = pool;
            this.ownsWorker = true;
        } else {
            this.worker = worker;
            this.ownsWorker = false;
        }
        this.fetcher = fetcher == null ? this::fetch : fetcher;
    }

    /** Lyrics for the track if known; otherwise schedules a lookup and returns {@link Lyrics#EMPTY}. */
    public Lyrics get(NowPlaying track) {
        if (!track.hasTrack()) return Lyrics.EMPTY;
        // A source may report a played title without an artist or album; match on empty text rather than
        // letting a null reach the URL builder on the worker thread.
        String title = track.title();
        String artist = track.artist() == null ? "" : track.artist();
        String album = track.album() == null ? "" : track.album();
        String key = key(title, artist);
        Lyrics cached = memory.get(key);
        if (cached != null) {
            Long negativeExpiry = negativeUntil.get(key);
            if (negativeExpiry == null || negativeCacheAlive(negativeExpiry, clock.millis())) return cached;
            // The negative cache expired: forget the empty result and allow one fresh lookup.
            memory.remove(key, cached);
            negativeUntil.remove(key, negativeExpiry);
            status.remove(key);
        }
        long now = clock.millis();
        Long retryAt = retryAfter.get(key);
        if (retryAt != null && retryExpired(retryAt, now) && retryAfter.remove(key, retryAt)) status.remove(key);
        if (status.putIfAbsent(key, "Searching LRCLIB…") == null) {
            long duration = track.durationMs();
            try {
                worker.execute(() -> resolve(key, title, artist, album, duration));
            } catch (RejectedExecutionException shuttingDown) {
                status.remove(key);
            }
        }
        return Lyrics.EMPTY;
    }

    public String status(NowPlaying track) {
        return track.hasTrack() ? status.getOrDefault(key(track.title(), track.artist()), "") : "";
    }

    private void resolve(String key, String title, String artist, String album, long durationMs) {
        Path file = cacheDir.resolve(hash(key) + ".lrc");
        Path missing = cacheDir.resolve(hash(key) + ".none");
        try {
            if (Files.isRegularFile(file)) { finish(key, Lyrics.parse(Files.readString(file)), "Synced lyrics · LRCLIB (cached)"); return; }
            long nowMillis = clock.millis();
            if (Files.isRegularFile(missing)) {
                long modified = Files.getLastModifiedTime(missing).toMillis();
                if (negativeCacheFresh(modified, nowMillis)) {
                    finishNegative(key, "No synced lyrics on LRCLIB", modified + NEGATIVE_TTL_MILLIS);
                    return;
                }
            }
            String synced = fetcher.fetch(title, artist, album, (int) Math.max(0, (durationMs + 500) / 1000));
            Files.createDirectories(cacheDir);
            if (synced == null || synced.isBlank()) {
                Files.writeString(missing, "");
                Files.deleteIfExists(file);
                finishNegative(key, "No synced lyrics on LRCLIB", nowMillis + NEGATIVE_TTL_MILLIS);
            } else {
                Files.writeString(file, synced);
                Files.deleteIfExists(missing);
                failures.remove(key);
                negativeUntil.remove(key);
                finish(key, Lyrics.parse(synced), "Synced lyrics · LRCLIB");
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.debug("LRCLIB lookup failed for {}", key, exception);
            status.put(key, "Lyrics lookup failed (offline?)");
            // Allow a retry once the back-off passes. Sleeping on the worker here would stall every other
            // track's lookup behind this one failure, so the retry is scheduled by the next get() call instead.
            int failed = failures.merge(key, 1, Integer::sum);
            if (retryAllowed(failed)) retryAfter.put(key, clock.millis() + RETRY_MILLIS);
            else retryAfter.remove(key);
        }
    }

    private void finish(String key, Lyrics lyrics, String message) {
        memory.put(key, lyrics);
        status.put(key, message);
    }

    private void finishNegative(String key, String message, long expiresAtMillis) {
        memory.put(key, Lyrics.EMPTY);
        negativeUntil.put(key, expiresAtMillis);
        status.put(key, message);
    }

    /** True while the on-disk negative marker is younger than the negative TTL. */
    static boolean negativeCacheFresh(long markerMillis, long nowMillis) {
        return markerMillis > nowMillis - NEGATIVE_TTL_MILLIS;
    }

    /** True while a negative entry already converted to an expiry time is still in effect. */
    static boolean negativeCacheAlive(long expiresAtMillis, long nowMillis) {
        return nowMillis < expiresAtMillis;
    }

    static boolean retryExpired(long retryAtMillis, long nowMillis) {
        return nowMillis - retryAtMillis >= 0;
    }

    static boolean retryAllowed(int failedLookups) {
        return failedLookups < MAX_LOOKUP_FAILURES;
    }

    /**
     * Lookup order (as in Anubis' LyricsService): exact match with album, exact without album, then searches
     * with the full and the simplified title/artist, keeping the best-scoring candidate with synced lyrics.
     * Every literal lookup is repeated with accents folded, since LRCLIB compares the raw text.
     */
    private String fetch(String title, String artist, String album, int durationSeconds) throws IOException {
        IOException failure = null;
        boolean answered = false;
        for (int round = 0; round < 2; round++) {
            String t = round == 0 ? title : LyricsMatcher.folded(title);
            String a = round == 0 ? artist : LyricsMatcher.folded(artist);
            String al = round == 0 ? album : LyricsMatcher.folded(album);
            if (round > 0 && t.equals(title) && a.equals(artist) && al.equals(album)) break;
            try {
                JsonObject exact = get(t, a, al, durationSeconds);
                answered = true;
                if (exact == null && !al.isBlank()) exact = get(t, a, "", durationSeconds);
                if (exact != null && synced(exact) != null) return synced(exact);
            } catch (IOException exception) {
                // LRCLIB's exact endpoint returns 503 under load; the search endpoint is a separate route and
                // often still answers, so a failed exact lookup must not sink the whole request.
                failure = exception;
                LOGGER.debug("LRCLIB exact lookup failed, falling back to search", exception);
            }
        }
        for (String[] terms : LyricsMatcher.searchTerms(title, artist)) {
            try {
                JsonElement json = request(API + "search?track_name=" + enc(terms[0]) + "&artist_name=" + enc(terms[1]));
                answered = true;
                if (json == null || !json.isJsonArray()) continue;
                JsonArray syncedOnly = new JsonArray();
                for (JsonElement element : json.getAsJsonArray())
                    if (element.isJsonObject() && synced(element.getAsJsonObject()) != null) syncedOnly.add(element);
                JsonObject best = LyricsMatcher.best(syncedOnly, title, artist, durationSeconds);
                if (best != null) return synced(best);
            } catch (IOException exception) {
                failure = exception;
                LOGGER.debug("LRCLIB search failed for {} / {}", terms[0], terms[1], exception);
            }
        }
        if (!answered && failure != null) throw failure;
        return null;
    }

    private JsonObject get(String title, String artist, String album, int durationSeconds) throws IOException {
        StringBuilder url = new StringBuilder(API + "get?track_name=").append(enc(title)).append("&artist_name=").append(enc(artist));
        if (!album.isBlank()) url.append("&album_name=").append(enc(album));
        if (durationSeconds > 0) url.append("&duration=").append(durationSeconds);
        // One shot: this endpoint 503s often, and retrying it only delays the search fallback that does work.
        JsonElement json = request(url.toString(), 1);
        return json != null && json.isJsonObject() ? json.getAsJsonObject() : null;
    }

    private static String synced(JsonObject item) {
        return item.has("syncedLyrics") && !item.get("syncedLyrics").isJsonNull() ? item.get("syncedLyrics").getAsString() : null;
    }

    /** One request, retried briefly on 429/5xx (LRCLIB asks clients to "retry in a moment" when busy). */
    private static JsonElement request(String url) throws IOException {
        return request(url, 3);
    }

    private static JsonElement request(String url, int attempts) throws IOException {
        IOException failure = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (attempt > 0) {
                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while retrying LRCLIB", interrupted);
                }
            }
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            try {
                connection.setConnectTimeout(6000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent", USER_AGENT);
                int code = connection.getResponseCode();
                if (code == 404) return null;
                if (code == 429 || code >= 500) {
                    failure = new IOException("LRCLIB HTTP " + code);
                    continue;
                }
                if (code >= 300) throw new IOException("LRCLIB HTTP " + code);
                try (InputStream in = connection.getInputStream()) {
                    return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            } finally {
                connection.disconnect();
            }
        }
        throw failure != null ? failure : new IOException("LRCLIB request failed");
    }

    static String key(String title, String artist) {
        String name = title == null ? "" : title.strip();
        String credit = artist == null ? "" : artist.strip();
        return (name + "\u0000" + credit).toLowerCase(Locale.ROOT);
    }

    private static String hash(String key) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(key.getBytes(StandardCharsets.UTF_8))).substring(0, 20);
        } catch (Exception exception) {
            return Integer.toHexString(key.hashCode());
        }
    }

    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    public void shutdown() {
        if (ownsWorker && worker instanceof ExecutorService service) service.shutdownNow();
    }
}
