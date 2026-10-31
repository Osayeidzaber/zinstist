package dev.jilaton.client.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reflects the user's own Spotify Connect session (normally the desktop app) through Spotify's official Web
 * API. Authorization uses Authorization Code + PKCE with a loopback redirect: no client secret, no cookies,
 * no reading of the desktop client's local data. Only the refresh token is persisted.
 *
 * <p>Policy limits honoured here: Spotify's Developer Policy forbids synchronizing recordings with visual
 * media, and there is no public lyrics API, so this source never feeds lyrics or audio-reactive visuals.
 * Playback control endpoints require Spotify Premium; a 403 is reported rather than retried.
 */
public final class SpotifyConnect {
    public static final int PORT = 43821;
    public static final String REDIRECT = "http://127.0.0.1:" + PORT + "/callback";
    private static final String SCOPES = "user-read-playback-state user-modify-playback-state";
    private static final Logger LOGGER = LoggerFactory.getLogger("Jilaton Spotify");
    private final ExecutorService http = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Jilaton Spotify");
        thread.setDaemon(true);
        return thread;
    });
    private final Path tokenFile;
    private final SecureRandom random = new SecureRandom();
    private volatile String clientId = "";
    private volatile String accessToken;
    private volatile long accessExpiry;
    private volatile String refreshToken;
    private volatile NowPlaying state = NowPlaying.idle("Spotify", "Not connected");
    private volatile long nextPollNanos;
    private volatile boolean polling;
    private volatile boolean authorizing;
    private volatile String lastArtworkUrl;

    public SpotifyConnect(Path configDir) {
        tokenFile = configDir.resolve("jilaton").resolve("spotify-token.json");
        try {
            if (Files.isRegularFile(tokenFile)) {
                // Tighten tokens written by older builds before reading them.
                SpotifyTokenPolicy.secure(tokenFile);
                JsonObject saved = JsonParser.parseString(Files.readString(tokenFile)).getAsJsonObject();
                if (saved.has("refresh_token")) refreshToken = saved.get("refresh_token").getAsString();
                if (saved.has("client_id")) clientId = saved.get("client_id").getAsString();
                if (refreshToken != null) state = NowPlaying.idle("Spotify", "Reconnecting…");
            }
        } catch (Exception exception) {
            LOGGER.warn("Ignoring unreadable Spotify token file");
        }
    }

    public NowPlaying state() { return state; }
    public boolean linked() { return refreshToken != null; }
    public boolean authorizing() { return authorizing; }

    public void setClientId(String id) {
        String trimmed = id == null ? "" : id.strip();
        if (!trimmed.equals(clientId) && refreshToken != null && !clientId.isEmpty()) disconnect();
        clientId = trimmed;
    }

    /** Opens the browser for consent and waits (in the background) for the loopback redirect. */
    public void connect() {
        if (clientId.isEmpty()) { state = NowPlaying.idle("Spotify", "Add your Spotify app Client ID first"); return; }
        if (authorizing) return;
        authorizing = true;
        String verifier = token(64);
        String stateParam = token(24);
        String challenge;
        try {
            challenge = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception exception) {
            authorizing = false;
            state = NowPlaying.idle("Spotify", "SHA-256 unavailable");
            return;
        }
        String url = "https://accounts.spotify.com/authorize?response_type=code&client_id=" + enc(clientId)
                + "&scope=" + enc(SCOPES) + "&redirect_uri=" + enc(REDIRECT)
                + "&code_challenge_method=S256&code_challenge=" + challenge + "&state=" + stateParam;
        state = NowPlaying.idle("Spotify", "Waiting for approval in your browser…");
        Thread listener = new Thread(() -> awaitCallback(verifier, stateParam), "Jilaton Spotify login");
        listener.setDaemon(true);
        listener.start();
        Util.getPlatform().openUri(URI.create(url));
    }

    private void awaitCallback(String verifier, String expectedState) {
        try (ServerSocket server = new ServerSocket(PORT, 1, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(180_000);
            while (true) {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    String requestLine = readLine(socket.getInputStream());
                    if (requestLine == null || !requestLine.startsWith("GET /callback")) { respond(socket, 404, "Not found"); continue; }
                    String query = requestLine.split(" ")[1];
                    Map<String, String> params = query(query.contains("?") ? query.substring(query.indexOf('?') + 1) : "");
                    if (!expectedState.equals(params.get("state"))) { respond(socket, 400, "State mismatch. Close this tab and retry from Jilaton."); continue; }
                    if (params.containsKey("error")) {
                        respond(socket, 200, "Spotify authorization was declined. You can close this tab.");
                        state = NowPlaying.idle("Spotify", "Authorization declined");
                        return;
                    }
                    String code = params.get("code");
                    if (code == null || code.isBlank()) { respond(socket, 400, "Spotify sent no authorization code. Close this tab and retry from Jilaton."); continue; }
                    respond(socket, 200, "Jilaton is connected to Spotify. You can close this tab.");
                    exchange(code, verifier);
                    return;
                }
            }
        } catch (SocketTimeoutException timeout) {
            state = NowPlaying.idle("Spotify", "Login timed out");
        } catch (IOException exception) {
            state = NowPlaying.idle("Spotify", "Login failed: port " + PORT + " unavailable");
            LOGGER.warn("Spotify loopback listener failed", exception);
        } finally {
            authorizing = false;
        }
    }

    private void exchange(String code, String verifier) {
        try {
            JsonObject json = postForm("https://accounts.spotify.com/api/token", Map.of("grant_type", "authorization_code",
                    "code", code, "redirect_uri", REDIRECT, "client_id", clientId, "code_verifier", verifier));
            acceptToken(json);
            state = NowPlaying.idle("Spotify", "Connected");
            nextPollNanos = 0;
        } catch (IOException exception) {
            state = NowPlaying.idle("Spotify", "Token exchange failed");
            LOGGER.warn("Spotify token exchange failed: {}", exception.getMessage());
        }
    }

    private void acceptToken(JsonObject json) throws IOException {
        accessToken = json.get("access_token").getAsString();
        accessExpiry = System.nanoTime() + (json.get("expires_in").getAsLong() - 30) * 1_000_000_000L;
        if (json.has("refresh_token")) refreshToken = json.get("refresh_token").getAsString();
        JsonObject saved = new JsonObject();
        saved.addProperty("client_id", clientId);
        saved.addProperty("refresh_token", refreshToken);
        Files.createDirectories(tokenFile.getParent());
        // Write then move, so a crash mid-write never leaves a truncated token file behind. On POSIX
        // filesystems the temporary file and the token are both owner-only (600).
        Path temporary = SpotifyTokenPolicy.temporarySibling(tokenFile);
        SpotifyTokenPolicy.writeOwnerOnly(temporary, saved.toString());
        try {
            Files.move(temporary, tokenFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, tokenFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        SpotifyTokenPolicy.secure(tokenFile);
    }

    private boolean ensureToken() throws IOException {
        if (accessToken != null && System.nanoTime() < accessExpiry) return true;
        if (refreshToken == null || clientId.isEmpty()) return false;
        try {
            acceptToken(postForm("https://accounts.spotify.com/api/token",
                    Map.of("grant_type", "refresh_token", "refresh_token", refreshToken, "client_id", clientId)));
            return true;
        } catch (HttpStatus status) {
            if (status.code == 400 || status.code == 401) { disconnect(); state = NowPlaying.idle("Spotify", "Authorization expired — reconnect"); }
            throw status;
        }
    }

    public void disconnect() {
        accessToken = null;
        refreshToken = null;
        lastArtworkUrl = null;
        try {
            SpotifyTokenPolicy.deleteTokenFiles(tokenFile);
        } catch (IOException ignored) { }
        state = NowPlaying.idle("Spotify", "Not connected");
    }

    /** Called every client tick while Spotify is the selected source; requests are rate-limited. */
    public void tick(double idleSeconds, double activeSeconds) {
        if (refreshToken == null || polling || System.nanoTime() < nextPollNanos) return;
        polling = true;
        http.execute(() -> {
            try {
                poll(idleSeconds, activeSeconds);
            } finally {
                polling = false;
            }
        });
    }

    private void poll(double idleSeconds, double activeSeconds) {
        try {
            if (!ensureToken()) return;
            HttpURLConnection connection = open("https://api.spotify.com/v1/me/player?additional_types=episode", "GET");
            int code = connection.getResponseCode();
            if (code == 204) {
                state = new NowPlaying("Spotify", "", "", "", 0, 0, false, null, "", "Spotify is closed or idle", true, false, false, System.nanoTime());
                schedule(idleSeconds);
                return;
            }
            if (code == 429) { schedule(retryAfter(connection, 10)); state = withStatus("Rate limited — slowing down"); return; }
            if (code == 401) { accessToken = null; schedule(1); return; }
            if (code >= 300) { schedule(idleSeconds * 2); state = withStatus("Spotify error " + code); return; }
            JsonObject json = JsonParser.parseString(read(connection.getInputStream())).getAsJsonObject();
            boolean playing = json.has("is_playing") && json.get("is_playing").getAsBoolean();
            long progress = json.has("progress_ms") && !json.get("progress_ms").isJsonNull() ? json.get("progress_ms").getAsLong() : 0;
            String device = "";
            if (json.has("device") && json.get("device").isJsonObject()) {
                JsonObject d = json.getAsJsonObject("device");
                device = str(d, "name") + (str(d, "type").isEmpty() ? "" : " · " + str(d, "type"));
            }
            JsonObject item = json.has("item") && json.get("item").isJsonObject() ? json.getAsJsonObject("item") : null;
            String title = "", artist = "", album = "", art = null;
            long duration = 0;
            if (item != null) {
                title = str(item, "name");
                duration = item.has("duration_ms") ? item.get("duration_ms").getAsLong() : 0;
                if (item.has("artists")) {
                    StringBuilder names = new StringBuilder();
                    for (JsonElement a : item.getAsJsonArray("artists")) {
                        if (!names.isEmpty()) names.append(", ");
                        names.append(str(a.getAsJsonObject(), "name"));
                    }
                    artist = names.toString();
                } else if (item.has("show")) artist = str(item.getAsJsonObject("show"), "name");
                JsonObject albumJson = item.has("album") ? item.getAsJsonObject("album") : item.has("show") ? item.getAsJsonObject("show") : null;
                if (albumJson != null) {
                    album = str(albumJson, "name");
                    art = smallestImage(albumJson.getAsJsonArray("images"));
                }
            }
            if (art != null && !art.equals(lastArtworkUrl)) {
                lastArtworkUrl = art;
                String url = art;
                try { Artwork.load(url, download(url)); } catch (IOException ignored) { }
            }
            state = new NowPlaying("Spotify", title, artist, album, progress, duration, playing,
                    art == null ? null : Artwork.get(art), device, playing ? "Playing" : "Paused", true, false, false, System.nanoTime());
            if (art != null && state.artwork() == null) schedule(0.6);
            else schedule(playing ? activeSeconds : idleSeconds);
        } catch (IOException exception) {
            state = withStatus("Offline — retrying");
            schedule(idleSeconds * 2);
        } catch (RuntimeException exception) {
            LOGGER.warn("Unexpected Spotify response", exception);
            schedule(idleSeconds * 2);
        }
    }

    public void control(String action) {
        if (refreshToken == null) return;
        http.execute(() -> {
            try {
                if (!ensureToken()) return;
                String method = action.equals("next") || action.equals("previous") ? "POST" : "PUT";
                HttpURLConnection connection = open("https://api.spotify.com/v1/me/player/" + action, method);
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(0);
                connection.getOutputStream().close();
                int code = connection.getResponseCode();
                if (code == 404 && !action.equals("pause")) code = transferToComputer() ? 204 : 404;
                if (code == 403) state = withStatus("Playback control needs Spotify Premium");
                else if (code == 404) {
                    state = withStatus("Spotify isn't open on any device — opening the app");
                    Util.getPlatform().openUri(URI.create("spotify:"));
                }
                else if (code == 429) schedule(retryAfter(connection, 5));
                nextPollNanos = System.nanoTime() + 350_000_000L;
            } catch (IOException exception) {
                state = withStatus("Control failed — offline?");
            }
        });
    }

    /**
     * With no active device Spotify ignores play/skip. Pick this PC's Spotify device (or the first available)
     * and transfer playback to it with play=true, so sound starts in the desktop app.
     */
    private boolean transferToComputer() throws IOException {
        HttpURLConnection list = open("https://api.spotify.com/v1/me/player/devices", "GET");
        if (list.getResponseCode() >= 300) return false;
        JsonArray devices = JsonParser.parseString(read(list.getInputStream())).getAsJsonObject().getAsJsonArray("devices");
        String chosen = null;
        for (JsonElement element : devices) {
            JsonObject device = element.getAsJsonObject();
            if (device.has("is_restricted") && device.get("is_restricted").getAsBoolean()) continue;
            if ("Computer".equalsIgnoreCase(str(device, "type"))) { chosen = str(device, "id"); break; }
            if (chosen == null) chosen = str(device, "id");
        }
        if (chosen == null || chosen.isEmpty()) return false;
        HttpURLConnection transfer = open("https://api.spotify.com/v1/me/player", "PUT");
        transfer.setDoOutput(true);
        transfer.setRequestProperty("Content-Type", "application/json");
        byte[] body = ("{\"device_ids\":[\"" + chosen + "\"],\"play\":true}").getBytes(StandardCharsets.UTF_8);
        transfer.setFixedLengthStreamingMode(body.length);
        try (OutputStream out = transfer.getOutputStream()) { out.write(body); }
        boolean ok = transfer.getResponseCode() < 300;
        if (ok) state = withStatus("Playing on this PC");
        return ok;
    }

    private NowPlaying withStatus(String status) {
        NowPlaying s = state;
        return new NowPlaying(s.source(), s.title(), s.artist(), s.album(), s.positionNow(System.nanoTime()), s.durationMs(),
                s.playing(), s.artwork(), s.device(), status, s.connected(), false, false, System.nanoTime());
    }

    /** Refreshes the artwork identifier once the texture finishes uploading. */
    public void refreshArtwork() {
        NowPlaying s = state;
        String url = lastArtworkUrl;
        if (url != null && s.artwork() == null && Artwork.get(url) != null)
            state = new NowPlaying(s.source(), s.title(), s.artist(), s.album(), s.positionMs(), s.durationMs(), s.playing(),
                    Artwork.get(url), s.device(), s.status(), s.connected(), false, false, s.capturedNanos());
    }

    private void schedule(double seconds) { nextPollNanos = System.nanoTime() + (long) (seconds * 1e9); }

    private static double retryAfter(HttpURLConnection connection, double fallback) {
        try { return Math.max(1, Double.parseDouble(connection.getHeaderField("Retry-After"))); }
        catch (RuntimeException exception) { return fallback; }
    }

    private static String smallestImage(JsonArray images) {
        if (images == null || images.isEmpty()) return null;
        String best = null;
        int bestSize = Integer.MAX_VALUE;
        for (JsonElement e : images) {
            JsonObject image = e.getAsJsonObject();
            int size = image.has("width") && !image.get("width").isJsonNull() ? image.get("width").getAsInt() : 640;
            if (size >= 64 && size < bestSize) { bestSize = size; best = str(image, "url"); }
        }
        return best != null ? best : str(images.get(0).getAsJsonObject(), "url");
    }

    private HttpURLConnection open(String url, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        return connection;
    }

    private static JsonObject postForm(String url, Map<String, String> form) throws IOException {
        StringBuilder body = new StringBuilder();
        for (var entry : form.entrySet()) {
            if (!body.isEmpty()) body.append('&');
            body.append(enc(entry.getKey())).append('=').append(enc(entry.getValue()));
        }
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(8000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        try (OutputStream out = connection.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
        int code = connection.getResponseCode();
        if (code >= 300) throw new HttpStatus(code);
        return JsonParser.parseString(read(connection.getInputStream())).getAsJsonObject();
    }

    private static byte[] download(String url) throws IOException {
        if (!url.startsWith("https://")) throw new IOException("Refusing non-HTTPS artwork");
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(8000);
        try (InputStream in = connection.getInputStream()) { return in.readNBytes(4 * 1024 * 1024); }
    }

    private static String read(InputStream in) throws IOException {
        try (in) { return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1 && c != '\n') if (c != '\r') line.write(c);
        return c == -1 && line.size() == 0 ? null : line.toString(StandardCharsets.US_ASCII);
    }

    private static void respond(Socket socket, int code, String message) throws IOException {
        String html = "<!doctype html><meta charset=utf-8><title>Jilaton</title><body style=\"background:#000;color:#f2f2f2;"
                + "font:15px system-ui;display:grid;place-items:center;height:90vh\"><p>" + message + "</p>";
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        OutputStream out = socket.getOutputStream();
        out.write(("HTTP/1.1 " + code + " OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: " + body.length
                + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    private static Map<String, String> query(String query) {
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) params.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return params;
    }

    private static String str(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private String token(int length) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return builder.toString();
    }

    public void shutdown() { http.shutdownNow(); }

    private static final class HttpStatus extends IOException {
        final int code;
        HttpStatus(int code) { super("HTTP " + code); this.code = code; }
    }
}
