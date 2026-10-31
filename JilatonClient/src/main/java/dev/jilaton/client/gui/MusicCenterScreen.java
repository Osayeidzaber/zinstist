package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.gui.design.Ui;
import dev.jilaton.client.module.impl.MusicCenterModule;
import dev.jilaton.client.module.impl.SpatialLyricsModule;
import dev.jilaton.client.music.LocalLibrary;
import dev.jilaton.client.music.Lyrics;
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
 * Music Center, built to the "Music Center" design board (development/GUI_REDESIGN.md §7): the source control, album
 * art, track and transport on the left; synced lyrics with word timing and three info tiles on the right. The
 * Library, Spotify and Placement tabs take the right column. Everything writes to the same module settings as the
 * ClickGUI, so changes apply immediately and persist with the profile.
 */
public final class MusicCenterScreen extends Screen {
    /** Inter instead of the vanilla pixel font for everything this screen draws ({@link JilatonType}). */
    private final Font font = JilatonType.font();
    private enum Tab { PLAYING("Now playing"), LIBRARY("Library"), SPOTIFY("Spotify"), LYRICS("Placement");
        final String label; Tab(String label) { this.label = label; } }

    private static final float LEFT = 112, TOP = 56, COL_W = 340, ART = 340, RIGHT_X = 600, RIGHT = 1168,
            BAR_Y = 578, VOL_X = 700, VOL_W = 220, VOL_Y = 694;
    /** The older tab bodies draw in their own units, scaled into the right column. */
    private static final float LEGACY_X = 584, LEGACY_Y = 112, LEGACY_SCALE = 1.3F;
    private static final String[] SOURCES = {"Off", "Spotify app (no setup)", "Any desktop player", "Local files", "Spotify Web API"};
    private static final String[] SOURCE_SHORT = {"Off", "Spotify app", "Any player", "Local files", "Web API"};

    private final Screen parent;
    private final Canvas canvas = new Canvas();
    private final List<Hit> boardHits = new ArrayList<>();
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
        canvas.fit(width, height);
        pw = Math.round((RIGHT - LEGACY_X) / LEGACY_SCALE);
        ph = Math.round((700 - LEGACY_Y) / LEGACY_SCALE);
        px = 0;
        py = 0;
        center().ensureScanned();
        clientIdBox = new EditBox(font, 0, 0, 200, 10, Component.literal("Client ID"));
        clientIdBox.setMaxLength(64);
        clientIdBox.setBordered(false);
        clientIdBox.setTextColor(Ui.TEXT);
        clientIdBox.setHint(Component.literal("Spotify app Client ID").withColor(Ui.TEXT_3));
        var setting = JilatonClient.moduleManager().get("music_center").settings().stream()
                .filter(s -> s.id().equals("spotify_client_id")).findFirst().orElse(null);
        if (setting instanceof dev.jilaton.client.setting.TextSetting text) clientIdBox.setValue(text.value());
        clientIdBox.setResponder(value -> {
            if (setting instanceof dev.jilaton.client.setting.TextSetting text) {
                try { text.setValue(value); } catch (IllegalArgumentException ignored) { }
            }
        });
        addRenderableWidget(clientIdBox);
        placeClientIdBox();
    }

    /** The Client ID field sits in the Spotify tab, in screen pixels mapped from its legacy-unit position. */
    private void placeClientIdBox() {
        clientIdBox.visible = tab == Tab.SPOTIFY;
        float bx = LEGACY_X + 28 * LEGACY_SCALE, by = LEGACY_Y + 94 * LEGACY_SCALE;
        clientIdBox.setX(canvas.screenX(bx));
        clientIdBox.setY(canvas.screenY(by));
        clientIdBox.setWidth(Math.max(40, canvas.screenX(bx + (pw - 56) * LEGACY_SCALE) - canvas.screenX(bx)));
    }

    private int sourceIndex() {
        MusicCenterModule s = settings();
        if (s == null) return 0;
        for (int i = 0; i < SOURCES.length; i++) if (SOURCES[i].equals(s.sourceLabel())) return i;
        return 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        boardHits.clear();
        long now = System.nanoTime();
        int accent = Ui.accent();
        double mx = canvas.x(mouseX), my = canvas.y(mouseY);
        NowPlaying np = center().nowPlaying();
        g.fill(0, 0, width, height, 0xF00A0A0A);
        canvas.begin(g);
        Ui.glow(g, LEFT + ART / 2, 334, 330, 0xFFFFFF, np.hasTrack() ? 0.07 : 0.035);

        // Header: back, title, tabs.
        boolean backHover = Ui.inside(mx, my, LEFT, TOP, 36, 36);
        Ui.card(g, LEFT, TOP, 36, 36, 10, Ui.alpha(Ui.RAISED, Ui.ease("music:back", backHover)), Ui.STROKE);
        Ui.text(g, "\u2039", LEFT + 13, TOP + 8, 17, Ui.SOFT);
        boardHits.add(new Hit(Math.round(LEFT), Math.round(TOP), 36, 36, this::onClose));
        Ui.text(g, "Music", LEFT + 52, TOP + 4, 26, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        String[] tabs = new String[Tab.values().length];
        for (Tab each : Tab.values()) tabs[each.ordinal()] = each.label;
        segmented(g, tabs, tab.ordinal(), RIGHT, TOP + 2, true, mx, my, i -> {
            tab = Tab.values()[i];
            placeClientIdBox();
        });
        segmented(g, SOURCE_SHORT, sourceIndex(), LEFT, 112, false, mx, my, i -> {
            if (settings() != null) settings().setSource(SOURCES[i]);
        });

        renderNowPlaying(g, np, now, mx, my, accent);
        switch (tab) {
            case PLAYING -> renderLyricsPanel(g, np, now, mx, my);
            default -> {
                Ui.card(g, RIGHT_X, 164, RIGHT - RIGHT_X, 536, 18, Ui.DRAWER, Ui.STROKE);
                g.pose().pushMatrix();
                g.pose().translate(LEGACY_X, LEGACY_Y);
                g.pose().scale(LEGACY_SCALE, LEGACY_SCALE);
                int lmx = (int) Math.round((mx - LEGACY_X) / LEGACY_SCALE), lmy = (int) Math.round((my - LEGACY_Y) / LEGACY_SCALE);
                JilatonType.withScale(LEGACY_SCALE, () -> {
                    switch (tab) {
                        case LIBRARY -> renderLibrary(g, 0, 0, lmx, lmy);
                        case SPOTIFY -> renderSpotify(g, 0, 0, lmx, lmy);
                        case LYRICS -> renderLyrics(g, 0, 0, lmx, lmy);
                        default -> { }
                    }
                });
                g.pose().popMatrix();
                if (tab == Tab.SPOTIFY) {
                    float bx = LEGACY_X + 20 * LEGACY_SCALE, by = LEGACY_Y + 90 * LEGACY_SCALE;
                    Ui.card(g, bx, by, (pw - 40) * LEGACY_SCALE, 24, 8, Ui.CARD,
                            clientIdBox.isFocused() ? Ui.alpha(accent, 0.55) : Ui.STROKE);
                }
            }
        }
        canvas.end(g);
        if (tab == Tab.SPOTIFY) placeClientIdBox();
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Segmented control; {@code alignRight} anchors its right edge at {@code x}. Hits go to {@code select}. */
    private void segmented(GuiGraphics g, String[] labels, int selected, float x, float y, boolean alignRight,
                           double mx, double my, java.util.function.IntConsumer select) {
        float total = 6;
        for (String label : labels) total += Ui.width(label, 12, JilatonType.Face.MEDIUM) + 28;
        float sx = alignRight ? x - total : x;
        Ui.card(g, sx, y, total, 32, 9, Ui.CARD, Ui.STROKE);
        float cx = sx + 3;
        for (int i = 0; i < labels.length; i++) {
            float w = Ui.width(labels[i], 12, JilatonType.Face.MEDIUM) + 28;
            boolean over = Ui.inside(mx, my, cx, y + 3, w, 26);
            double t = Ui.ease("music:seg:" + labels[i], over);
            if (i == selected) Ui.round(g, cx, y + 3, w, 26, 7, Ui.TEXT);
            else if (t > 0.01) Ui.round(g, cx, y + 3, w, 26, 7, Ui.alpha(Ui.RAISED, t));
            Ui.text(g, labels[i], cx + 14, y + 10, 12, JilatonType.Face.MEDIUM,
                    i == selected ? Ui.DEEP : Ui.mix(Ui.TEXT_2, Ui.TEXT, t));
            int index = i;
            boardHits.add(new Hit(Math.round(cx), Math.round(y + 3), Math.round(w), 26, () -> select.accept(index)));
            cx += w;
        }
    }

    private void renderNowPlaying(GuiGraphics g, NowPlaying np, long now, double mx, double my, int accent) {
        float artY = 164;
        // Soft drop shadow, then the art (or an ink tile with a music glyph).
        for (int i = 3; i >= 1; i--)
            Ui.round(g, LEFT - i * 3, artY - i * 3 + 6, ART + i * 6, ART + i * 6, 18 + i * 3, Ui.alpha(0xFF000000, 0.10));
        if (np.artwork() != null) {
            g.blit(np.artwork(), Math.round(LEFT), Math.round(artY), Math.round(LEFT + ART), Math.round(artY + ART), 0, 1, 0, 1);
            Ui.outline(g, LEFT, artY, ART, ART, 18, Ui.alpha(Ui.TEXT, 0.08));
        } else {
            Ui.card(g, LEFT, artY, ART, ART, 18, Ui.CARD, Ui.STROKE);
            Ui.textCentre(g, "\u266A", LEFT + ART / 2, artY + ART / 2 - 26, 48, JilatonType.Face.REGULAR, Ui.FAINT);
        }
        boolean off = center().source() == MusicCenter.Source.OFF;
        String title = off ? "Music is off" : np.hasTrack() ? np.title() : "Nothing playing";
        Ui.text(g, Ui.fit(title, COL_W, 22, JilatonType.Face.SEMIBOLD), LEFT, 520, 22, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        String sub = off ? "Pick a source above" : String.join("  \u00b7  ", java.util.stream.Stream.of(np.artist(), np.device(), np.status())
                .filter(v -> v != null && !v.isBlank()).toList());
        Ui.text(g, Ui.fit(sub, COL_W, 13, JilatonType.Face.REGULAR), LEFT, 550, 13, Ui.TEXT_2);

        double progress = np.progress(now);
        Ui.round(g, LEFT, BAR_Y, COL_W, 4, 2, Ui.STROKE);
        if (progress > 0) Ui.round(g, LEFT, BAR_Y, (float) (COL_W * progress), 4, 2, Ui.TEXT);
        boolean barHover = Ui.inside(mx, my, LEFT, BAR_Y - 8, COL_W, 20) || draggingSeek;
        double knob = Ui.ease("music:seek", barHover);
        Ui.circle(g, (float) (LEFT + COL_W * progress), BAR_Y + 2, (float) (4 + 2 * knob), Ui.TEXT);
        boardHits.add(new Hit(Math.round(LEFT), Math.round(BAR_Y - 8), Math.round(COL_W), 20, () -> draggingSeek = true));
        Ui.text(g, NowPlaying.time(np.positionNow(now)), LEFT, BAR_Y + 12, 11, JilatonType.Face.MONO, Ui.TEXT_3);
        Ui.textRight(g, NowPlaying.time(np.durationMs()), LEFT + COL_W, BAR_Y + 12, 11, JilatonType.Face.MONO, Ui.TEXT_3);

        float cx = LEFT + COL_W / 2, cy = 632;
        transport(g, cx - 82, cy, 22, false, mx, my, "prev", center()::previous);
        transport(g, cx, cy, 30, true, mx, my, np.playing() ? "pause" : "play", center()::togglePause);
        transport(g, cx + 82, cy, 22, false, mx, my, "next", center()::next);

        MusicCenterModule s = settings();
        if (s != null && center().source() == MusicCenter.Source.LOCAL) {
            chip(g, "Shuffle", s.shuffle(), LEFT, 674, mx, my, () -> s.setShuffle(!s.shuffle()));
            String repeat = "Repeat " + s.repeatLabel();
            chip(g, repeat, !"Off".equalsIgnoreCase(s.repeatLabel()), LEFT + COL_W - Ui.width(repeat, 12, JilatonType.Face.MEDIUM) - 24,
                    674, mx, my, s::cycleRepeat);
        }
    }

    private void chip(GuiGraphics g, String label, boolean on, float x, float y, double mx, double my, Runnable action) {
        float w = Ui.width(label, 12, JilatonType.Face.MEDIUM) + 24;
        Ui.chip(g, label, x, y, w, 26, on, Ui.inside(mx, my, x, y, w, 26), Ui.accent());
        boardHits.add(new Hit(Math.round(x), Math.round(y), Math.round(w), 26, action));
    }

    /** Round transport button: white disc for the main one, outline for prev/next; icon drawn from shapes. */
    private void transport(GuiGraphics g, float cx, float cy, float r, boolean primary, double mx, double my, String icon,
                           Runnable action) {
        boolean over = Ui.inside(mx, my, cx - r, cy - r, r * 2, r * 2);
        double t = Ui.ease("music:" + icon + ":" + Math.round(cx), over);
        float rr = (float) (r + t * 1.5);
        int ink;
        if (primary) {
            Ui.circle(g, cx, cy, rr, Ui.mix(Ui.TEXT, 0xFFFFFFFF, t));
            ink = Ui.DEEP;
        } else {
            if (t > 0.01) Ui.circle(g, cx, cy, rr, Ui.alpha(Ui.RAISED, t));
            Ui.ring(g, cx, cy, rr, 1, Ui.mix(Ui.STROKE, Ui.TEXT_2, t));
            ink = Ui.TEXT;
        }
        float s = r * 0.42F;
        switch (icon) {
            case "pause" -> {
                Ui.round(g, cx - s * 0.75F, cy - s, s * 0.5F, s * 2, 1.5F, ink);
                Ui.round(g, cx + s * 0.25F, cy - s, s * 0.5F, s * 2, 1.5F, ink);
            }
            case "play" -> triangle(g, cx - s * 0.6F, cy, s * 2, true, ink);
            case "next" -> {
                triangle(g, cx - s * 0.7F, cy, s * 1.5F, true, ink);
                Ui.rect(g, cx + s * 0.55F, cy - s * 0.75F, Math.max(1.5F, s * 0.22F), s * 1.5F, ink);
            }
            default -> {
                triangle(g, cx + s * 0.7F, cy, s * 1.5F, false, ink);
                Ui.rect(g, cx - s * 0.55F - Math.max(1.5F, s * 0.22F), cy - s * 0.75F, Math.max(1.5F, s * 0.22F), s * 1.5F, ink);
            }
        }
        boardHits.add(new Hit(Math.round(cx - r), Math.round(cy - r), Math.round(r * 2), Math.round(r * 2), action));
    }

    /** Isosceles triangle of height {@code h} with its flat side at {@code x}, pointing right (or left). */
    private static void triangle(GuiGraphics g, float x, float cy, float h, boolean right, int color) {
        int rows = Math.max(6, Math.round(h));
        float step = h / rows;
        for (int i = 0; i < rows; i++) {
            float half = h / 2 * (1 - (i + 0.5F) / rows);
            float px = right ? x + i * step : x - (i + 1) * step;
            Ui.rect(g, px, cy - half, step + 0.01F, half * 2, color);
        }
    }

    private void renderLyricsPanel(GuiGraphics g, NowPlaying np, long now, double mx, double my) {
        SpatialLyricsModule lyrics = lyrics();
        Ui.spaced(g, "SYNCED LYRICS \u00b7 LRCLIB", RIGHT_X, 166, 11, 1.1F, JilatonType.Face.MEDIUM, Ui.TEXT_3);
        if (lyrics != null) {
            String where = lyrics.enabled() ? "Shown in your world: " + lyrics.modeSetting().value() : "Not shown in your world";
            Ui.textRight(g, where, RIGHT, 165, 12, JilatonType.Face.REGULAR, Ui.TEXT_2);
        }
        float panelY = 188, panelH = 392, panelW = RIGHT - RIGHT_X;
        Ui.card(g, RIGHT_X, panelY, panelW, panelH, 18, Ui.DRAWER, Ui.STROKE);
        Lyrics text = center().lyrics();
        float midY = panelY + panelH / 2 - 15;
        if (!np.hasTrack() || text == null || text.isEmpty()) {
            String status = center().lyricsStatus();
            String line = !np.hasTrack() ? "Pick a source to see lyrics here" : status.isEmpty() ? "No synced lyrics for this track" : status;
            Ui.textCentre(g, line, RIGHT_X + panelW / 2, midY + 8, 14, JilatonType.Face.REGULAR, Ui.TEXT_2);
        } else {
            long position = np.positionNow(now) + (lyrics == null ? 0 : Math.round(lyrics.offsetSetting().value()));
            int index = text.indexAt(position);
            List<Lyrics.Line> lines = text.lines();
            float maxW = panelW - 64;
            for (int k = -2; k <= 2; k++) {
                int i = index + k;
                if (i < 0 || i >= lines.size()) continue;
                Lyrics.Line line = lines.get(i);
                if (k == 0) {
                    drawSungLine(g, line, position, RIGHT_X + 32, midY, maxW);
                } else {
                    float y = k < 0 ? midY - 44 + (k + 1) * 34 - 8 : midY + 52 + (k - 1) * 34;
                    Ui.text(g, Ui.fit(line.text(), maxW, 18, JilatonType.Face.MEDIUM), RIGHT_X + 32, y, 18,
                            JilatonType.Face.MEDIUM, Math.abs(k) == 1 ? Ui.TEXT_3 : Ui.FAINT);
                }
            }
        }

        float tileY = 596, gap = 12, tileW = (panelW - gap * 2) / 3;
        if (lyrics != null) {
            tile(g, RIGHT_X, tileY, tileW, "Placement", String.valueOf(lyrics.modeSetting().value()), mx, my,
                    () -> lyrics.modeSetting().cycle(1));
            tile(g, RIGHT_X + tileW + gap, tileY, tileW, "Lines kept",
                    Math.round(lyrics.trailLinesSetting().value()) + " \u00b7 " + Math.round(lyrics.lingerSetting().value()) + " s",
                    mx, my, () -> { tab = Tab.LYRICS; placeClientIdBox(); });
            tile(g, RIGHT_X + (tileW + gap) * 2, tileY, tileW, "Timing offset",
                    String.format("%+.0f ms", lyrics.offsetSetting().value()), mx, my, () -> { tab = Tab.LYRICS; placeClientIdBox(); });
        }
        MusicCenterModule s = settings();
        if (s != null && center().source() == MusicCenter.Source.LOCAL) {
            Ui.text(g, "Volume", RIGHT_X, VOL_Y - 6, 12, Ui.TEXT_2);
            Ui.round(g, VOL_X, VOL_Y - 2, VOL_W, 4, 2, Ui.STROKE);
            Ui.round(g, VOL_X, VOL_Y - 2, (float) (VOL_W * s.volume()), 4, 2, Ui.TEXT);
            Ui.circle(g, (float) (VOL_X + VOL_W * s.volume()), VOL_Y, 6, Ui.TEXT);
            boardHits.add(new Hit(Math.round(VOL_X - 6), Math.round(VOL_Y - 10), Math.round(VOL_W + 12), 20, () -> draggingVolume = true));
        }
    }

    /** The current line at 30px: words already sung in white, the rest secondary (word timing from Lyrics). */
    private void drawSungLine(GuiGraphics g, Lyrics.Line line, long position, float x, float y, float maxW) {
        float size = 30;
        if (!line.timedWords()) {
            Ui.text(g, Ui.fit(line.text(), maxW, size, JilatonType.Face.SEMIBOLD), x, y, size, JilatonType.Face.SEMIBOLD, Ui.TEXT);
            return;
        }
        int sung = Lyrics.wordsSung(line, position);
        float cx = x;
        float space = Ui.width(" ", size, JilatonType.Face.SEMIBOLD);
        for (int i = 0; i < line.words().size(); i++) {
            String word = line.words().get(i).text().strip();
            float w = Ui.width(word, size, JilatonType.Face.SEMIBOLD);
            if (cx + w > x + maxW) {
                Ui.text(g, "\u2026", cx, y, size, JilatonType.Face.SEMIBOLD, Ui.TEXT_2);
                break;
            }
            Ui.text(g, word, cx, y, size, JilatonType.Face.SEMIBOLD, i < sung ? Ui.TEXT : Ui.TEXT_3);
            cx += w + space;
        }
    }

    private void tile(GuiGraphics g, float x, float y, float w, String label, String value, double mx, double my, Runnable action) {
        boolean over = Ui.inside(mx, my, x, y, w, 76);
        double t = Ui.ease("music:tile:" + label, over);
        Ui.card(g, x, y, w, 76, 14, Ui.mix(Ui.CARD, Ui.RAISED, t), Ui.mix(Ui.STROKE, Ui.FAINT, t));
        Ui.spaced(g, label.toUpperCase(java.util.Locale.ROOT), x + 16, y + 16, 10, 1, JilatonType.Face.MEDIUM, Ui.TEXT_3);
        Ui.text(g, Ui.fit(value, w - 32, 16, JilatonType.Face.SEMIBOLD), x + 16, y + 40, 16, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        boardHits.add(new Hit(Math.round(x), Math.round(y), Math.round(w), 76, action));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) { }

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
        g.drawString(font, "Easiest: choose \"Spotify app\" in the source control on the left.", lx, y + 62, JilatonUi.TEXT, false);
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
        double bx = canvas.x(event.x()), by = canvas.y(event.y());
        for (int i = boardHits.size() - 1; i >= 0; i--) {
            Hit hit = boardHits.get(i);
            if (hit.contains(bx, by)) {
                clientIdBox.setFocused(false);
                hit.action().run();
                if (draggingVolume || draggingSeek) mouseDragged(event, 0, 0);
                return true;
            }
        }
        double lx = (bx - LEGACY_X) / LEGACY_SCALE, ly = (by - LEGACY_Y) / LEGACY_SCALE;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (hit.contains(lx, ly)) {
                hit.action().run();
                if (draggingLyric) updateLyricDrag(lx, ly);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double bx = canvas.x(event.x()), by = canvas.y(event.y());
        if (draggingLyric) { updateLyricDrag((bx - LEGACY_X) / LEGACY_SCALE, (by - LEGACY_Y) / LEGACY_SCALE); return true; }
        MusicCenterModule s = settings();
        if (draggingVolume && s != null) {
            s.setVolume(Math.max(0, Math.min(1, (bx - VOL_X) / VOL_W)));
            return true;
        }
        if (draggingSeek) {
            double fraction = Math.max(0, Math.min(1, (bx - LEFT) / COL_W));
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
            placeClientIdBox();
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
