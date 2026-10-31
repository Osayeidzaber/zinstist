package dev.jilaton.client.music;

import dev.jilaton.client.config.ClientConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

/**
 * One place every music surface reads from. The HUD, Music Center screen, spatial lyrics and music visuals
 * all consume {@link #nowPlaying()}, so switching the source never requires rebuilding any of them.
 */
public final class MusicCenter {
    public enum Source { LOCAL, DESKTOP, SPOTIFY, OFF }

    private static MusicCenter instance;
    private final LocalLibrary library = new LocalLibrary();
    private final LocalMusicPlayer player = new LocalMusicPlayer();
    private final SpotifyConnect spotify;
    private final MediaSessionSource desktop;
    private final OnlineLyrics onlineLyrics;
    /** Third-party lookups stay off until the user's explicit opt-in reaches {@link #setFetchLyrics}. */
    private boolean fetchLyrics;
    /** Persisted desktop-bridge opt-in; replaced by {@link #attachConfig} at client start. */
    private volatile BooleanSupplier desktopBridgeEnabled = () -> false;
    private Source source = Source.LOCAL;
    private Path folder;
    private String scanError = "";

    private MusicCenter(Path configDir) {
        spotify = new SpotifyConnect(configDir);
        desktop = new MediaSessionSource(configDir);
        onlineLyrics = new OnlineLyrics(configDir);
        folder = configDir.resolve("jilaton").resolve("music");
    }

    public static synchronized MusicCenter get() {
        if (instance == null) instance = new MusicCenter(FabricLoader.getInstance().getConfigDir());
        return instance;
    }

    public LocalLibrary library() { return library; }
    public LocalMusicPlayer player() { return player; }
    public SpotifyConnect spotify() { return spotify; }
    public MediaSessionSource desktop() { return desktop; }

    /** Binds the persisted desktop-bridge opt-in; until then the gate stays off. */
    public void attachConfig(ClientConfig config) {
        desktopBridgeEnabled = config::desktopMediaBridgeEnabled;
        syncDesktopBridge();
    }

    /** Test/UI seam for the same gate; the client always uses the persisted value via {@link #attachConfig}. */
    public void refreshDesktopBridgeGate() { syncDesktopBridge(); }

    /** The desktop bridge may run only while it is selected AND explicitly enabled. */
    private void syncDesktopBridge() {
        desktop.setEnabled(source == Source.DESKTOP && desktopBridgeEnabled.getAsBoolean());
    }

    public void setFetchLyrics(boolean fetch) {
        fetchLyrics = fetch;
        // The same opt-out covers the desktop source's cover-art lookup, which also sends the track online.
        desktop.setOnlineLookups(fetch);
    }

    /** Where the current lyrics come from, for the UI. */
    public String lyricsStatus() {
        NowPlaying np = nowPlaying();
        if (!np.hasTrack()) return "";
        if (source == Source.LOCAL && !player.lyrics().isEmpty()) return "Synced lyrics · local .lrc";
        if (!fetchLyrics) return "Online lyrics off";
        return onlineLyrics.status(np);
    }
    public Source source() { return source; }
    public Path folder() { return folder; }
    public String scanError() { return scanError; }

    public void setSource(Source source) {
        if (this.source == source) return;
        if (source != Source.LOCAL) player.stop();
        if (source != Source.DESKTOP) desktop.stop();
        this.source = source;
        syncDesktopBridge();
    }

    public void setFolder(Path folder) {
        if (folder != null && !folder.equals(this.folder)) { this.folder = folder; rescan(); }
    }

    public void rescan() {
        try {
            library.scan(folder);
            scanError = "";
        } catch (IOException | RuntimeException exception) {
            scanError = "Cannot read " + folder + ": " + exception.getMessage();
        }
    }

    public void ensureScanned() {
        if (library.root() == null) rescan();
    }

    /** Volume multiplier from Minecraft's master and music sliders, applied on top of Jilaton's own volume. */
    static float masterGain() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) return 1;
        return client.options.getSoundSourceVolume(SoundSource.MASTER);
    }

    public void tick(double spotifyIdle, double spotifyActive) {
        // Re-read the persisted opt-in every tick: revoking it stops the bridge immediately.
        syncDesktopBridge();
        if (source == Source.DESKTOP) desktop.tick();
        if (source == Source.SPOTIFY) {
            spotify.tick(spotifyIdle, spotifyActive);
            spotify.refreshArtwork();
        }
        if (source == Source.LOCAL) {
            LocalLibrary.Track track = player.current();
            if (track != null && track.artwork() != null && Artwork.get(track.artwork().toString()) == null) {
                Path art = track.artwork();
                try { Artwork.load(art.toString(), Files.readAllBytes(art)); } catch (IOException ignored) { }
            }
        }
    }

    public NowPlaying nowPlaying() {
        return switch (source) {
            case SPOTIFY -> spotify.state();
            case DESKTOP -> desktop.state();
            case OFF -> NowPlaying.idle("Off", "Music source off");
            case LOCAL -> {
                LocalLibrary.Track track = player.current();
                if (track == null) yield NowPlaying.idle("Local", player.error().isEmpty() ? "Nothing queued" : player.error());
                yield new NowPlaying("Local", track.title(), track.artist(), track.album(), player.positionMs(), player.durationMs(),
                        player.playing(), track.artwork() == null ? null : Artwork.get(track.artwork().toString()),
                        "This PC", player.playing() ? "Playing" : "Paused", true, true, true, System.nanoTime());
            }
        };
    }

    /** Local .lrc first; otherwise synced lyrics matched on LRCLIB for whatever is playing. */
    public Lyrics lyrics() {
        if (source == Source.OFF) return Lyrics.EMPTY;
        if (source == Source.LOCAL && !player.lyrics().isEmpty()) return player.lyrics();
        if (!fetchLyrics) return Lyrics.EMPTY;
        NowPlaying np = nowPlaying();
        return np.hasTrack() ? onlineLyrics.get(np) : Lyrics.EMPTY;
    }

    /** Audio-reactive data, only from the local player (never captured from Spotify). */
    public float amplitude() { return source == Source.LOCAL ? player.amplitude() : 0; }

    public void togglePause() {
        if (source == Source.DESKTOP) desktop.control("toggle");
        else if (source == Source.SPOTIFY) spotify.control(spotify.state().playing() ? "pause" : "play");
        else if (source == Source.LOCAL) player.togglePause();
    }

    public void next() { if (source == Source.DESKTOP) desktop.control("next"); else if (source == Source.SPOTIFY) spotify.control("next"); else if (source == Source.LOCAL) player.next(); }
    public void previous() { if (source == Source.DESKTOP) desktop.control("previous"); else if (source == Source.SPOTIFY) spotify.control("previous"); else if (source == Source.LOCAL) player.previous(); }

    public void shutdown() {
        player.stop();
        desktop.shutdown();
        spotify.shutdown();
        onlineLyrics.shutdown();
    }
}
