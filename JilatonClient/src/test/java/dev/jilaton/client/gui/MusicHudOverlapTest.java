package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig.HudPosition;
import dev.jilaton.client.config.HudStyle;
import dev.jilaton.client.gui.hud.HudDesign;
import dev.jilaton.client.gui.hud.HudPreset;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Screen-space collision audit for the music surfaces against the HUD rewrite. Every slot below is the
 * default the renderer passes to {@link HudLayout#resolve} when the user has never dragged the widget;
 * dragging stores a fraction in {@code ClientConfig.hudPosition}, which overrides these defaults.
 */
class MusicHudOverlapTest {
    private static final int[] WIDTHS = {1280, 1920};

    private record Rect(String id, int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean intersects(Rect other) {
            return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
        }
        Rect intersection(Rect other) {
            int left = Math.max(x, other.x), top = Math.max(y, other.y);
            return new Rect("overlap", left, top, Math.min(right(), other.right()) - left,
                    Math.min(bottom(), other.bottom()) - top);
        }
    }

    private static int heightForWidth(int guiWidth) { return guiWidth == 1280 ? 720 : 1080; }

    /** HudRenderer.java:146-147 — default slot {@code (guiWidth - width - 8, 40)}, Compact style. */
    private static Rect music(int w) {
        int width = NowPlayingWidget.width(NowPlayingWidget.Style.COMPACT);
        int height = NowPlayingWidget.height(NowPlayingWidget.Style.COMPACT);
        // Resolve exactly as the renderer does when no position is saved.
        HudLayout layout = HudLayout.resolve(width, height, w, heightForWidth(w),
                w - width - 8, 40, null, HudStyle.DEFAULT);
        return new Rect("music", layout.x(), layout.y(), layout.width(), layout.height());
    }

    /** HudRenderer.java:602-614 — left column, 152 wide; y only depends on the watermark/module list. */
    private static Rect keybinds() {
        int height = HudDesign.keybindHeight(HudDesign.KEYBIND_MAX_ROWS);
        return new Rect("keybinds", 8, 8, HudDesign.KEYBIND_WIDTH, height);
    }

    /** HudRenderer.java:649-653 — right column bottom, above the vanilla hotbar area. */
    private static Rect server(int w, int h) {
        return new Rect("server", w - HudDesign.SERVER_WIDTH - 8, h - 174,
                HudDesign.SERVER_WIDTH, HudDesign.SERVER_HEIGHT);
    }

    /** HudRenderer.java:678-688 — right column, above the server panel. */
    private static Rect heldItem(int w, int h) {
        return new Rect("held_item", w - HudDesign.HELD_ITEM_WIDTH - 8, h - 240,
                HudDesign.HELD_ITEM_WIDTH, HudDesign.HELD_ITEM_HEIGHT);
    }

    /** HudRenderer.java:560-563 — editor preview slot; in play the card is projected onto the target. */
    private static Rect aboveHeadEditor(int w, int h) {
        return new Rect("above_head", (w - HudDesign.ABOVE_HEAD_WIDTH) / 2, h - 108,
                HudDesign.ABOVE_HEAD_WIDTH, HudDesign.ABOVE_HEAD_HEIGHT);
    }

    /** HudRenderer.java:209-213 — FPS/ping, top right. */
    private static Rect performance(int w) {
        return new Rect("performance", w - 148, 8, 140, 22);
    }

    /** HudRenderer.java:345-346 — potion effects, top right below CPS. */
    private static Rect effects(int w, int rows) {
        return new Rect("effects", w - 148 - 8, 64, 148, 21 + Math.max(1, rows) * 14);
    }

    /** HudRenderer.java:222-226 — bottom left. */
    private static Rect coordinates(int h) {
        return new Rect("coordinates", 8, h - 29, 150, 21);
    }

    /** HudRenderer.java:234-236 — bottom left above coordinates. */
    private static Rect keystrokes(int h) {
        return new Rect("keystrokes", 8, h - 104, 70, 58);
    }

    /** HudRenderer.java:297-301 — right column above the hotbar. */
    private static Rect armor(int w, int h) {
        return new Rect("armor", w - 130 - 8, h - 100, 130, 50);
    }

    /** HudRenderer.java:390-391 — centred above the hotbar. */
    private static Rect targetCard(int w, int h) {
        HudDesign.TargetCard card = HudDesign.targetCard(w);
        int width = card.width();
        return new Rect("target", (w - width) / 2, h - 76, width, card.height());
    }

    /** HudRenderer.java:278-279 — right edge, vertically centred. */
    private static Rect notifications(int w, int h) {
        return new Rect("notifications", w - 174 - 8, h / 2, 174, 23);
    }

    /** HudRenderer.java:161-166 — top left. */
    private static Rect watermark() {
        return new Rect("watermark", 8, 8, 74, 22);
    }

    /** MusicCenterScreen.java:57-59 — centred panel, at most 460x300. */
    private static Rect musicCenterPanel(int w, int h) {
        int pw = Math.min(460, w - 24), ph = Math.min(300, h - 24);
        return new Rect("music_center", (w - pw) / 2, (h - ph) / 2, pw, ph);
    }

    /** MusicCenterScreen.java:139-142 — transport buttons, cy = y + ph - 44, span cx +- 66. */
    private static Rect musicCenterTransport(int w, int h) {
        Rect panel = musicCenterPanel(w, h);
        int cx = panel.x() + panel.width() / 2;
        return new Rect("music_center_transport", cx - 66, panel.y() + panel.height() - 44, 132, 20);
    }

    /** MusicCenterScreen.java:154-157 — one-line status hint under the transport row. */
    private static Rect musicCenterHint(int w, int h) {
        Rect panel = musicCenterPanel(w, h);
        return new Rect("music_center_hint", panel.x() + 20, panel.y() + panel.height() - 20,
                panel.width() - 40, 10);
    }

    private static List<Rect> hudDefaults(int w, int h) {
        return List.of(music(w), keybinds(), server(w, h), heldItem(w, h), aboveHeadEditor(w, h),
                performance(w), effects(w, 3), coordinates(h), keystrokes(h), armor(w, h), targetCard(w, h),
                notifications(w, h), watermark());
    }

    @Test
    void musicWidgetSharesNoSlotWithFullPresetWidgets() {
        for (HudPreset preset : List.of(HudPreset.MINIMAL, HudPreset.FULL)) {
            for (int w : WIDTHS) {
                int h = heightForWidth(w);
                Rect music = music(w);
                assertTrue(music.x() >= 0 && music.right() <= w && music.y() >= 0 && music.bottom() <= h);
                assertFalse(music.intersects(keybinds()), "music/keybinds " + preset + " " + w);
                if (preset == HudPreset.FULL) {
                    assertTrue(preset.enabled("keybinds") && preset.enabled("server"));
                    assertTrue(preset.enabled("above_head") && preset.enabled("held_item"));
                }
                assertFalse(music.intersects(server(w, h)), "music/server " + preset + " " + w);
                assertFalse(music.intersects(heldItem(w, h)), "music/held_item " + preset + " " + w);
                assertFalse(music.intersects(aboveHeadEditor(w, h)), "music/above_head " + preset + " " + w);
            }
        }
    }

    @Test
    void musicWidgetClearOfTheRestOfTheDefaultHud() {
        for (int w : WIDTHS) {
            int h = heightForWidth(w);
            Rect music = music(w);
            for (Rect other : List.of(performance(w), coordinates(h), keystrokes(h), armor(w, h),
                    targetCard(w, h), notifications(w, h), watermark())) {
                assertFalse(music.intersects(other), "music/" + other.id() + " " + w);
            }
        }
    }

    @Test
    void musicCenterPanelAndRowsClearOfEveryHudDefault() {
        for (int w : WIDTHS) {
            int h = heightForWidth(w);
            for (Rect row : List.of(musicCenterPanel(w, h), musicCenterTransport(w, h), musicCenterHint(w, h))) {
                for (Rect hud : hudDefaults(w, h)) {
                    assertFalse(row.intersects(hud), row.id() + "/" + hud.id() + " " + w + "x" + h);
                }
            }
        }
    }

    /**
     * Known HUD-side collision, reported rather than worked around: the potion panel starts at y 64 while the
     * Now Playing slot starts at y 40, so both sit in the right-hand column. The fix belongs in
     * HudRenderer's effects default (HudRenderer.java:346); when that moves, delete this test.
     */
    @Test
    void effectsPanelSharesTheMusicColumn() {
        for (int w : WIDTHS) {
            Rect music = music(w);
            Rect effects = effects(w, 3);
            assertTrue(music.intersects(effects), "expected the reported overlap at " + w);
            Rect overlap = music.intersection(effects);
            assertEquals(w - 156, overlap.x());
            assertEquals(64, overlap.y());
            assertEquals(148, overlap.width());
            assertEquals(16, overlap.height());
        }
        int expandedHeight = NowPlayingWidget.height(NowPlayingWidget.Style.EXPANDED);
        assertEquals(70, expandedHeight);
        Rect expanded = new Rect("music_expanded", 1280 - 220 - 8, 40, 220, expandedHeight);
        assertEquals(46, expanded.intersection(effects(1280, 3)).height());
    }

    @Test
    void defaultMusicSlotMatchesTheRendererDefault() {
        HudLayout layout = HudLayout.resolve(176, 40, 1280, 720, 1280 - 176 - 8, 40, null, HudStyle.DEFAULT);
        assertEquals(1096, layout.x());
        assertEquals(40, layout.y());
        assertEquals(176, layout.width());
        assertEquals(40, layout.height());
        HudLayout saved = HudLayout.resolve(176, 40, 1280, 720, 1096, 40, new HudPosition(0.5, 0.5),
                HudStyle.DEFAULT);
        assertEquals((1280 - 176) / 2, saved.x());
    }
}
