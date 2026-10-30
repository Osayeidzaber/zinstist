package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.music.LocalMusicPlayer;
import dev.jilaton.client.music.MusicCenter;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

/**
 * Settings and lifecycle for the Music Center. Disabling it stops local playback and Spotify polling, so no
 * audio thread or network request remains active. Open the full interface with the Music Center key (M).
 */
public final class MusicCenterModule extends ClientModule {
    // Off by default: the desktop source starts a PowerShell bridge and the lookups send track names online,
    // so neither happens until the user picks a source.
    private final ChoiceSetting source = addSetting(new ChoiceSetting("source", "Source", List.of("Spotify app (no setup)", "Any desktop player", "Local files", "Spotify Web API", "Off"), "Off"));
    private final BooleanSetting onlineLyrics = addSetting(new BooleanSetting("online_lyrics", "Online lookups (LRCLIB lyrics, iTunes covers)", true));
    private final TextSetting folder = addSetting(new TextSetting("folder", "Local music folder (blank = config/jilaton/music)", 240, "", value -> {
        String trimmed = value.strip();
        if (!trimmed.isEmpty()) {
            try { Path.of(trimmed); } catch (InvalidPathException exception) { throw new IllegalArgumentException("Not a valid folder path"); }
        }
        return trimmed;
    }));
    private final NumberSetting volume = addSetting(new NumberSetting("volume", "Local volume", 0, 1, 0.01, 0.6));
    private final BooleanSetting shuffle = addSetting(new BooleanSetting("shuffle", "Shuffle", false));
    private final ChoiceSetting repeat = addSetting(new ChoiceSetting("repeat", "Repeat", List.of("Off", "All", "One"), "All"));
    private final BooleanSetting quietVanilla = addSetting(new BooleanSetting("quiet_vanilla", "Pause Minecraft music while your music plays", true));
    private final TextSetting clientId = addSetting(new TextSetting("spotify_client_id", "Spotify app Client ID", 64, "", value -> {
        String trimmed = value.strip();
        if (!trimmed.isEmpty() && !trimmed.matches("[A-Za-z0-9]{16,64}")) throw new IllegalArgumentException("Client IDs are letters and digits");
        return trimmed;
    }));
    private final NumberSetting pollActive = addSetting(new NumberSetting("spotify_poll", "Spotify refresh while playing (s)", 2, 15, 1, 4));

    public MusicCenterModule() {
        super("music_center", "Music Center", "Local music library and desktop Spotify now-playing. Press M to open.", ModuleCategory.CLIENT);
    }

    @Override public boolean enabledByDefault() { return true; }

    @Override
    protected void onDisable(Minecraft client) {
        MusicCenter.get().player().stop();
        MusicCenter.get().setSource(MusicCenter.Source.OFF);
    }

    @Override
    protected void onTick(Minecraft client) {
        MusicCenter center = MusicCenter.get();
        center.setSource(switch (source.value()) {
            case "Spotify Web API" -> MusicCenter.Source.SPOTIFY;
            case "Spotify app (no setup)", "Any desktop player" -> MusicCenter.Source.DESKTOP;
            case "Off" -> MusicCenter.Source.OFF;
            default -> MusicCenter.Source.LOCAL;
        });
        center.desktop().setFilter(source.value().equals("Any desktop player") ? "" : "Spotify");
        center.setFetchLyrics(onlineLyrics.value());
        if (!folder.value().isEmpty()) center.setFolder(Path.of(folder.value()));
        center.spotify().setClientId(clientId.value());
        LocalMusicPlayer player = center.player();
        player.setVolume((float) volume.value());
        player.setShuffle(shuffle.value());
        player.setRepeat(switch (repeat.value()) { case "Off" -> LocalMusicPlayer.Repeat.OFF; case "One" -> LocalMusicPlayer.Repeat.ONE; default -> LocalMusicPlayer.Repeat.ALL; });
        if (quietVanilla.value() && (player.playing() || center.source() == MusicCenter.Source.DESKTOP && center.nowPlaying().playing()))
            client.getMusicManager().stopPlaying();
        center.tick(Math.max(8, pollActive.value() * 2), pollActive.value());
    }

    public void setSource(String label) { source.setValue(label); }
    public String sourceLabel() { return source.value(); }
    public void setShuffle(boolean value) { shuffle.setValue(value); }
    public void cycleRepeat() {
        repeat.setValue(switch (repeat.value()) { case "Off" -> "All"; case "All" -> "One"; default -> "Off"; });
    }
    public String repeatLabel() { return repeat.value(); }
    public void setVolume(double value) { volume.setValue(value); }
    public double volume() { return volume.value(); }
    public boolean shuffle() { return shuffle.value(); }
}
