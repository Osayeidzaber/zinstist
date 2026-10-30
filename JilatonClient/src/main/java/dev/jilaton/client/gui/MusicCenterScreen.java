package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.MusicCenterModule;
import dev.jilaton.client.module.impl.SpatialLyricsModule;
import dev.jilaton.client.music.LocalLibrary;
import dev.jilaton.client.music.MusicCenter;
import dev.jilaton.client.music.NowPlaying;
import dev.jilaton.client.music.SpotifyConnect;
import dev.jilaton.client.render.fx.LyricLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Music Center: now playing with transport controls, the local library and playlists, Spotify connection
 * and an interactive top-down placement preview for spatial lyrics. Everything writes to the same module
 * settings as the ClickGUI, so changes apply immediately and persist with the profile.
 */
public final class MusicCenterScreen extends Screen {
    /** Inter instead of the vanilla pixel font for everything this screen draws ({@link JilatonType}). */
    private final Font font = JilatonType.font();
    private enum Tab { PLAYING("Now Playing"), LIBRARY("Library"), SPOTIFY("Spotify"), LYRICS("Lyrics");
        final String label; Tab(String label) { this.label = label; } }

    private final Screen parent;
    private final long openedAt = System.nanoTime();
    private Tab tab = Tab.PLAYING;
    private String playlist;
    private int trackScroll;
    private EditBox clientIdBox;
    private final List<Hit> hits = new ArrayList<>();
    private boolean draggingLyric, draggingVolume, draggingSeek;
    private int px, py, pw, ph;

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    public MusicCenterScreen(Screen parent) {
        super(Component.literal("Music Center"));
        this.parent = parent;
    }

    private MusicCenter center() { return MusicCenter.get(); }
    private MusicCenterModule settings() { return (MusicCenterModule) JilatonClient.moduleManager().get("music_center"); }
    private SpatialLyricsModule lyrics() { return (SpatialLyricsModule) JilatonClient.moduleManager().get("spatial_lyrics"); }

    @Override
    protected void init() {
        pw = Math.min(460, width - 24);
        ph = Math.min(300, height - 24);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        center().ensureScanned();
        clientIdBox = new EditBox(font, px + 20, py + 92, pw - 40, 18, Component.literal("Client ID"));
        clientIdBox.setMaxLength(64);
        clientIdBox.setHint(Component.literal("Spotify app Client ID"));
        var setting = JilatonClient.moduleManager().get("music_center").settings().stream()
                .filter(s -> s.id().equals("spotify_client_id")).findFirst().orElse(null);
        if (setting instanceof dev.jilaton.client.setting.TextSetting text) clientIdBox.setValue(text.value());
        clientIdBox.setResponder(value -> {
            if (setting instanceof dev.jilaton.client.setting.TextSetting text) {
                try { text.setValue(value); } catch (IllegalArgumentException ignored) { }
            }
        });
        addRenderableWidget(clientIdBox);
        clientIdBox.visible = tab == Tab.SPOTIFY;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        double t = JilatonClient.clientConfig().reducedMotion() ? 1 : Motion.SETTLE.apply(Math.min(1, (System.nanoTime() - openedAt) / 3.2e8));
        g.fill(0, 0, width, height, JilatonUi.alpha(0xC8000000, t));
        int rise = (int) Math.round((1 - t) * 8);
        int x = px, y = py + rise;
        JilatonUi.shadow(g, x, y, pw, ph, 6, 0.6 * t);
        JilatonUi.fillRounded(g, x, y, pw, ph, 6, JilatonUi.alpha(0xF5050505, t));
        JilatonUi.strokeRounded(g, x, y, pw, ph, 6, JilatonUi.HAIRLINE);
        g.drawString(font, "MUSIC CENTER", x + 20, y + 16, JilatonUi.TEXT_3, false);
        String source = settings() == null ? "" : settings().sourceLabel();
        button(g, x + pw - 20 - font.width(source) - 16, y + 11, font.width(source) + 16, 16, source, mouseX, mouseY, true, this::cycleSource);
        int tabX = x + 20;
        for (Tab each : Tab.values()) {
            int w = font.width(each.label) + 18;
            boolean selected = tab == each;
            Hit hit = new Hit(tabX, y + 34, w, 18, () -> { tab = each; clientIdBox.visible = each == Tab.SPOTIFY; });
            hits.add(hit);
            if (selected) JilatonUi.fillRounded(g, tabX, y + 34, w, 18, 9, JilatonUi.TEXT);
            else if (hit.contains(mouseX, mouseY)) JilatonUi.fillRounded(g, tabX, y + 34, w, 18, 9, JilatonUi.RAISED);
            g.drawString(font, each.label, tabX + 9, y + 39, selected ? JilatonUi.INK : JilatonUi.TEXT_2, false);
            tabX += w + 6;
        }
        g.fill(x + 20, y + 60, x + pw - 20, y + 61, JilatonUi.HAIRLINE);
        clientIdBox.setY(y + 92);
        switch (tab) {
            case PLAYING -> renderPlaying(g, x, y, mouseX, mouseY);
            case LIBRARY -> renderLibrary(g, x, y, mouseX, mouseY);
            case SPOTIFY -> renderSpotify(g, x, y, mouseX, mouseY);
            case LYRICS -> renderLyrics(g, x, y, mouseX, mouseY);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) { }

    private void renderPlaying(GuiGraphics g, int x, int y, int mx, int my) {
        NowPlaying np = center().nowPlaying();
        long now = System.nanoTime();
        int art = Math.min(96, ph - 130), ax = x + 20, ay = y + 74;
        if (np.artwork() != null) g.blit(np.artwork(), ax, ay, ax + art, ay + art, 0, 1, 0, 1);
        else {
            JilatonUi.fillRounded(g, ax, ay, art, art, 4, 0xFF111111);
            g.drawCenteredString(font, "♪", ax + art / 2, ay + art / 2 - 4, JilatonUi.TEXT_3);
        }
        int tx = ax + art + 16, tw = x + pw - 20 - tx;
        g.drawString(font, NowPlayingWidget.clip(font, np.hasTrack() ? np.title() : "Nothing playing", tw), tx, ay + 4, JilatonUi.TEXT, false);
        g.drawString(font, NowPlayingWidget.clip(font, np.artist(), tw), tx, ay + 18, JilatonUi.TEXT_2, false);
        g.drawString(font, NowPlayingWidget.clip(font, np.album(), tw), tx, ay + 30, JilatonUi.TEXT_3, false);
        g.drawString(font, NowPlayingWidget.clip(font, (np.device().isEmpty() ? "" : np.device() + "  ·  ") + np.status(), tw), tx, ay + 44, JilatonUi.TEXT_3, false);
        // Seek bar (local only; Spotify seeking is not exposed to keep requests minimal).
        int barY = ay + 66;
        double progress = np.progress(now);
        g.fill(tx, barY, tx + tw, barY + 2, 0xFF262626);
        g.fill(tx, barY, tx + (int) (tw * progress), barY + 2, JilatonUi.TEXT);
        g.fill(tx + (int) (tw * progress) - 1, barY - 2, tx + (int) (tw * progress) + 2, barY + 4, JilatonUi.TEXT);
        hits.add(new Hit(tx, barY - 4, tw, 10, () -> draggingSeek = true));
        g.drawString(font, NowPlaying.time(np.positionNow(now)), tx, barY + 7, JilatonUi.TEXT_3, false);
        String total = NowPlaying.time(np.durationMs());
        g.drawString(font, total, tx + tw - font.width(total), barY + 7, JilatonUi.TEXT_3, false);
        int cy = y + ph - 44, cx = x + pw / 2;
        button(g, cx - 66, cy, 36, 20, "◀◀", mx, my, true, center()::previous);
        button(g, cx - 22, cy, 44, 20, np.playing() ? "Pause" : "Play", mx, my, true, center()::togglePause);
        button(g, cx + 30, cy, 36, 20, "▶▶", mx, my, true, center()::next);
        boolean local = center().source() == MusicCenter.Source.LOCAL;
        MusicCenterModule s = settings();
        if (s != null && local) {
            button(g, x + 20, cy, 64, 20, s.shuffle() ? "Shuffle ●" : "Shuffle", mx, my, true, () -> s.setShuffle(!s.shuffle()));
            button(g, x + 88, cy, 74, 20, "Repeat " + s.repeatLabel(), mx, my, true, s::cycleRepeat);
            int vx = x + pw - 120, vy = cy + 9;
            g.drawString(font, "VOL", vx - 24, vy - 4, JilatonUi.TEXT_3, false);
            g.fill(vx, vy, vx + 100, vy + 2, 0xFF262626);
            g.fill(vx, vy, vx + (int) (100 * s.volume()), vy + 2, JilatonUi.TEXT);
            hits.add(new Hit(vx - 2, vy - 6, 104, 14, () -> draggingVolume = true));
        }
        String lyricsStatus = center().lyricsStatus();
        String hint = (lyricsStatus.isEmpty() ? "" : lyricsStatus + "  ·  ") + (local ? "Playing from your files"
                : "Audio plays in the Spotify app; Jilaton shows and controls it");
        g.drawString(font, NowPlayingWidget.clip(font, hint, pw - 40), x + 20, y + ph - 20, JilatonUi.TEXT_4, false);
    }

    private void renderLibrary(GuiGraphics g, int x, int y, int mx, int my) {
        LocalLibrary library = center().library();
        List<String> names = library.playlistNames();
        if (playlist == null || !names.contains(playlist)) playlist = names.isEmpty() ? null : names.get(0);
        int listX = x + 20, listY = y + 72, listW = 130;
        button(g, listX, y + ph - 30, 62, 18, "Rescan", mx, my, true, () -> center().rescan());
        button(g, listX + 66, y + ph - 30, 64, 18, "Open folder", mx, my, true, () -> Util.getPlatform().openPath(center().folder()));
        if (names.isEmpty()) {
            g.drawString(font, "Your library is empty.", listX, listY, JilatonUi.TEXT_2, false);
            g.drawString(font, "Add .ogg or .wav files (and optional .lrc lyrics, cover.png)", listX, listY + 14, JilatonUi.TEXT_3, false);
            g.drawString(font, NowPlayingWidget.clip(font, "to " + center().folder(), pw - 40), listX, listY + 26, JilatonUi.TEXT_3, false);
            if (!center().scanError().isEmpty()) g.drawString(font, center().scanError(), listX, listY + 44, JilatonUi.DANGER, false);
            return;
        }
        int rowY = listY;
        for (String name : names) {
            boolean selected = name.equals(playlist);
            Hit hit = new Hit(listX, rowY - 3, listW, 15, () -> { playlist = name; trackScroll = 0; });
            hits.add(hit);
            if (selected) JilatonUi.fillRounded(g, listX, rowY - 3, listW, 15, 3, JilatonUi.RAISED);
            else if (hit.contains(mx, my)) JilatonUi.fillRounded(g, listX, rowY - 3, listW, 15, 3, 0xFF101010);
            g.drawString(font, NowPlayingWidget.clip(font, name, listW - 10), listX + 6, rowY, selected ? JilatonUi.TEXT : JilatonUi.TEXT_2, false);
            rowY += 17;
            if (rowY > y + ph - 44) break;
        }
        int tx = listX + listW + 12, tw = x + pw - 20 - tx;
        g.fill(tx - 6, listY - 4, tx - 5, y + ph - 40, JilatonUi.HAIRLINE);
        List<LocalLibrary.Track> tracks = library.playlist(playlist);
        int visible = Math.max(1, (y + ph - 40 - listY) / 16);
        trackScroll = Math.max(0, Math.min(trackScroll, Math.max(0, tracks.size() - visible)));
        LocalLibrary.Track current = center().player().current();
        for (int i = trackScroll; i < Math.min(tracks.size(), trackScroll + visible); i++) {
            LocalLibrary.Track track = tracks.get(i);
            int ry = listY + (i - trackScroll) * 16;
            int index = i;
            Hit hit = new Hit(tx, ry - 3, tw, 15, () -> {
                if (track.supported()) {
                    if (settings() != null) settings().setSource("Local files");
                    center().setSource(MusicCenter.Source.LOCAL);
                    center().player().setQueue(tracks, index);
                }
            });
            hits.add(hit);
            boolean playingThis = current != null && current.file().equals(track.file());
            if (hit.contains(mx, my)) JilatonUi.fillRounded(g, tx, ry - 3, tw, 15, 3, 0xFF101010);
            int color = !track.supported() ? JilatonUi.TEXT_4 : playingThis ? JilatonUi.TEXT : JilatonUi.TEXT_2;
            String label = (playingThis ? "▶ " : "") + track.display() + (track.supported() ? "" : "  (unsupported format)");
            g.drawString(font, NowPlayingWidget.clip(font, label, tw - 24), tx + 4, ry, color, false);
            if (track.lyrics() != null) g.drawString(font, "LRC", tx + tw - font.width("LRC") - 4, ry, JilatonUi.TEXT_3, false);
        }
    }

    private void renderSpotify(GuiGraphics g, int x, int y, int mx, int my) {
        SpotifyConnect spotify = center().spotify();
        int lx = x + 20;
        g.drawString(font, "Easiest: choose \"Spotify app (no setup)\" in the source button above.", lx, y + 62, JilatonUi.TEXT, false);
        g.drawString(font, "Optional Web API route below. Audio always plays in the Spotify app itself.", lx, y + 70, JilatonUi.TEXT_2, false);
        g.drawString(font, "1. Create an app at developer.spotify.com and paste its Client ID:", lx, y + 80, JilatonUi.TEXT_3, false);
        g.drawString(font, "2. Add this exact Redirect URI to the app:", lx, y + 118, JilatonUi.TEXT_3, false);
        g.drawString(font, SpotifyConnect.REDIRECT, lx + 8, y + 130, JilatonUi.TEXT, false);
        String connect = spotify.authorizing() ? "Waiting for browser…" : spotify.linked() ? "Disconnect" : "Connect Spotify";
        button(g, lx, y + 148, font.width(connect) + 20, 20, connect, mx, my, !spotify.authorizing(), () -> {
            if (spotify.linked()) spotify.disconnect(); else spotify.connect();
        });
        g.drawString(font, "Status: " + spotify.state().status(), lx, y + 176, JilatonUi.TEXT_2, false);
        int ny = y + 196;
        for (String note : List.of("Only playback-state scopes are requested; the refresh token stays in config/jilaton.",
                "If Play does nothing, Jilaton moves playback to this PC's Spotify device first.",
                "Lyrics come from LRCLIB (free) for both routes; audio-reactive visuals stay local-only.",
                "Web API play/pause/skip require Premium; the no-setup route does not.")) {
            g.drawString(font, note, lx, ny, JilatonUi.TEXT_4, false);
            ny += 11;
        }
    }

    private void renderLyrics(GuiGraphics g, int x, int y, int mx, int my) {
        SpatialLyricsModule lyrics = lyrics();
        if (lyrics == null) return;
        int size = Math.min(ph - 92, 190);
        int cx = x + 20 + size / 2, cy = y + 72 + size / 2;
        double scale = size / 2.0 / 4.2;
        JilatonUi.fillRounded(g, cx - size / 2, cy - size / 2, size, size, 4, 0xFF080808);
        for (double r = 1; r <= 4; r++) ring(g, cx, cy, r * scale, 0xFF161616);
        g.fill(cx, cy - size / 2 + 6, cx + 1, cy, 0xFF2A2A2A);
        g.drawString(font, "VIEW", cx - font.width("VIEW") / 2, cy - size / 2 + 8, JilatonUi.TEXT_4, false);
        JilatonUi.fillRounded(g, cx - 3, cy - 3, 6, 6, 3, JilatonUi.TEXT);
        // Preview with the camera looking "up" the diagram; viewYaw = pi so forward = (0, -1) in screen space.
        LyricLayout.Params params = lyrics.params(true, System.nanoTime() / 1e9, 0);
        for (int k = -1; k <= 1; k++) {
            LyricLayout.Slot slot = LyricLayout.slot(params, k, Math.PI);
            int sx = cx + (int) Math.round(slot.dx() * scale), sy = cy + (int) Math.round(slot.dz() * scale);
            int w = k == 0 ? 34 : 24;
            JilatonUi.fillRounded(g, sx - w / 2, sy - 3, w, 6, 3, k == 0 ? JilatonUi.TEXT : JilatonUi.TEXT_3);
        }
        hits.add(new Hit(cx - size / 2, cy - size / 2, size, size, () -> draggingLyric = true));
        int rx = cx + size / 2 + 18;
        g.drawString(font, "Drag in the preview to place the active line.", rx, y + 72, JilatonUi.TEXT_2, false);
        g.drawString(font, String.format("Radius %.2f   Angle %.0f°", lyrics.radiusSetting().value(), lyrics.sideSetting().value()), rx, y + 86, JilatonUi.TEXT_3, false);
        String modeLabel = "Mode: " + lyrics.modeSetting().value();
        button(g, rx, y + 102, font.width(modeLabel) + 16, 18, modeLabel, mx, my, true, () -> lyrics.modeSetting().cycle(1));
        g.drawString(font, String.format("Height %.2f", lyrics.heightSetting().value()), rx, y + 128, JilatonUi.TEXT_3, false);
        button(g, rx + 70, y + 124, 18, 16, "−", mx, my, true, () -> lyrics.heightSetting().setValue(lyrics.heightSetting().value() - 0.1));
        button(g, rx + 92, y + 124, 18, 16, "+", mx, my, true, () -> lyrics.heightSetting().setValue(lyrics.heightSetting().value() + 0.1));
        g.drawString(font, String.format("Timing %+.0f ms", lyrics.offsetSetting().value()), rx, y + 150, JilatonUi.TEXT_3, false);
        button(g, rx + 92, y + 146, 18, 16, "−", mx, my, true, () -> lyrics.offsetSetting().setValue(lyrics.offsetSetting().value() - 100));
        button(g, rx + 114, y + 146, 18, 16, "+", mx, my, true, () -> lyrics.offsetSetting().setValue(lyrics.offsetSetting().value() + 100));
        String state = lyrics.enabled() ? "Spatial Lyrics is on" : "Enable Spatial Lyrics";
        button(g, rx, y + 172, font.width(state) + 16, 18, state, mx, my, true, lyrics::toggle);
        g.drawString(font, "Lyrics come from .lrc files next to your", rx, y + 200, JilatonUi.TEXT_4, false);
        g.drawString(font, "local tracks (word timing supported).", rx, y + 211, JilatonUi.TEXT_4, false);
    }

    private void ring(GuiGraphics g, int cx, int cy, double r, int color) {
        int steps = 64;
        for (int i = 0; i < steps; i++) {
            double a = i * Math.PI * 2 / steps;
            int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void updateLyricDrag(double mx, double my) {
        SpatialLyricsModule lyrics = lyrics();
        if (lyrics == null) return;
        int size = Math.min(ph - 92, 190);
        int cx = px + 20 + size / 2, cy = py + 72 + size / 2;
        double scale = size / 2.0 / 4.2;
        double dx = (mx - cx) / scale, dy = (my - cy) / scale;
        lyrics.radiusSetting().setValue(Math.hypot(dx, dy));
        // Up the diagram is the view direction; positive angles are to the right.
        lyrics.sideSetting().setValue(Math.toDegrees(Math.atan2(dx, -dy)));
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, boolean active, Runnable action) {
        boolean hover = active && mx >= x && mx < x + w && my >= y && my < y + h;
        JilatonUi.fillRounded(g, x, y, w, h, 3, hover ? JilatonUi.RAISED_HOVER : JilatonUi.RAISED);
        JilatonUi.strokeRounded(g, x, y, w, h, 3, hover ? JilatonUi.HAIRLINE_STRONG : JilatonUi.HAIRLINE);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, active ? JilatonUi.TEXT : JilatonUi.TEXT_4);
        if (active) hits.add(new Hit(x, y, w, h, action));
    }

    private void cycleSource() {
        MusicCenterModule s = settings();
        if (s == null) return;
        s.setSource(switch (s.sourceLabel()) {
            case "Spotify app (no setup)" -> "Any desktop player";
            case "Any desktop player" -> "Local files";
            case "Local files" -> "Spotify Web API";
            case "Spotify Web API" -> "Off";
            default -> "Spotify app (no setup)";
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (hit.contains(event.x(), event.y())) {
                hit.action().run();
                if (draggingLyric) updateLyricDrag(event.x(), event.y());
                if (draggingVolume || draggingSeek) mouseDragged(event, 0, 0);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingLyric) { updateLyricDrag(event.x(), event.y()); return true; }
        MusicCenterModule s = settings();
        if (draggingVolume && s != null) {
            int vx = px + pw - 120;
            s.setVolume((event.x() - vx) / 100.0);
            return true;
        }
        if (draggingSeek) {
            int tx = px + 20 + 96 + 16, tw = px + pw - 20 - tx;
            double fraction = Math.max(0, Math.min(1, (event.x() - tx) / tw));
            if (center().source() == MusicCenter.Source.LOCAL)
                center().player().seek((long) (center().player().durationMs() * fraction));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean was = draggingLyric || draggingVolume || draggingSeek;
        draggingLyric = draggingVolume = draggingSeek = false;
        if (was) JilatonClient.clientConfig().save(JilatonClient.moduleManager());
        return super.mouseReleased(event) || was;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (tab == Tab.LIBRARY) { trackScroll -= (int) Math.signum(vertical); return true; }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_SPACE && !clientIdBox.isFocused()) { center().togglePause(); return true; }
        if (event.key() == GLFW.GLFW_KEY_TAB && !clientIdBox.isFocused()) {
            tab = Tab.values()[(tab.ordinal() + 1) % Tab.values().length];
            clientIdBox.visible = tab == Tab.SPOTIFY;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        JilatonClient.clientConfig().save(JilatonClient.moduleManager());
        minecraft.setScreen(parent);
    }
}
