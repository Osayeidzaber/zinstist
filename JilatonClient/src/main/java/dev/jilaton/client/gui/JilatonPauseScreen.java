package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.gui.design.Kit;
import dev.jilaton.client.gui.design.Paint;
import dev.jilaton.client.gui.design.Ui;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.music.MusicCenter;
import dev.jilaton.client.music.NowPlaying;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pause, editorial layout over the live world (Minecraft's GPU blur plus a left-weighted dark gradient). Left: a
 * monospace section line, a large "Paused" with the world name in italic serif, and numbered destination rows;
 * hovering one slides it right, draws an indicator line and reveals an arrow, the same language as the home
 * columns. Right: a live session sheet (modules running, now playing with play/pause, position). Bottom: more
 * options and disconnect on a hairline rule. Esc resumes; arrows/Tab move focus; Enter activates.
 */
public final class JilatonPauseScreen extends Screen {
    /** Set while "More options" opens vanilla's pause menu, so the redirect lets that one through. */
    public static boolean vanillaRequested;

    private static final float M = 72;
    private static final float ROW_H = 52;

    private record Item(String key, String label, String hint, Runnable run) { }

    private record Hit(String key, float x, float y, float w, float h, Runnable run) {
        boolean inside(double mx, double my) {
            return Ui.inside(mx, my, x, y, w, h);
        }
    }

    private final Canvas canvas = new Canvas();
    private final List<Item> items = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private long openedAt;
    private int focused = -1;
    private String pressed;

    public JilatonPauseScreen() {
        super(Component.translatable("menu.game"));
    }

    @Override
    protected void init() {
        canvas.fit(width, height);
        items.clear();
        items.add(new Item("resume", "Back to game", "ESC", this::onClose));
        items.add(new Item("client", "Client", JilatonClient.clickGuiKeyName(), () -> JilatonClient.openClickGui(this)));
        items.add(new Item("music", "Music", JilatonClient.musicKeyName(), () -> minecraft.setScreen(new MusicCenterScreen(this))));
        items.add(new Item("options", "Options", "", () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
        if (minecraft.player != null) {
            items.add(new Item("advancements", "Advancements", "", () -> minecraft.setScreen(
                    new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this))));
            items.add(new Item("stats", "Statistics", "", () -> minecraft.setScreen(new StatsScreen(this, minecraft.player.getStats()))));
        }
        if (minecraft.hasSingleplayerServer() && !minecraft.getSingleplayerServer().isPublished()) {
            items.add(new Item("lan", "Open to LAN", "", () -> minecraft.setScreen(new ShareToLanScreen(this))));
        }
        if (openedAt == 0) openedAt = System.nanoTime();
    }

    private void disconnect() {
        minecraft.getReportingContext().draftReportHandled(minecraft, this,
                () -> minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(graphics);
        // Heavier on the left where the type sits, lighter on the right so the world stays readable.
        for (int i = 0; i < 24; i++) {
            float k = i / 23F;
            int x0 = Math.round(width * i / 24F), x1 = Math.round(width * (i + 1) / 24F);
            int a = Math.round(235 - 120 * k);
            graphics.fill(x0, 0, x1, height, (a << 24) | 0x040404);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        double mx = canvas.x(mouseX), my = canvas.y(mouseY);
        String focusKey = focused >= 0 && focused < hits.size() ? hits.get(focused).key() : null;
        List<Hit> previous = new ArrayList<>(hits);
        hits.clear();
        canvas.begin(g);

        double head = Kit.enter(openedAt, 0);
        mono(g, "II — PAUSED", M, 52, Kit.a(Kit.LABEL_3, head));
        String clock = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        mono(g, clock, Canvas.W - M - Ui.spacedWidth(clock, Kit.CAPTION, 1.6F, JilatonType.Face.MONO), 52, Kit.a(Kit.LABEL_3, head));
        Ui.rect(g, M, 76, (Canvas.W - 2 * M) * (float) head, 1, Kit.a(0xFFFFFFFF, 0.12));

        // Title block: the word rises out of a mask, the world name follows in italic serif.
        double t1 = Kit.enter(openedAt, 0.05), t2 = Kit.enter(openedAt, 0.12);
        g.enableScissor(0, 96, Math.round(Canvas.W), 196);
        Kit.text(g, "Paused", M - 4, 100 + (float) ((1 - t1) * 90), 88, JilatonType.Face.DISPLAY_HEAVY, Kit.LABEL);
        g.disableScissor();
        Kit.text(g, worldName(), M, 200 + (float) ((1 - t2) * 12), 30, JilatonType.Face.SERIF_ITALIC, Kit.a(Kit.accentSoft(), t2));

        // Numbered destinations.
        float y = 262;
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            float rowY = y + i * ROW_H;
            hits.add(new Hit(item.key(), M - 8, rowY, 560, ROW_H - 4, item.run()));
            boolean hot = over(item.key(), mx, my, previous, focusKey);
            double hv = Kit.spring("pause:" + item.key(), hot);
            double pr = Kit.spring("pause:p:" + item.key(), item.key().equals(pressed));
            double in = Kit.enter(openedAt, 0.16 + i * 0.04);
            float shift = (float) (14 * hv + (1 - in) * 30);
            JilatonType.Face face = i == 0 ? JilatonType.Face.DISPLAY_HEAVY : JilatonType.Face.DISPLAY;
            int base = i == 0 ? Kit.LABEL : Kit.mix(Kit.LABEL_2, Kit.LABEL, hv);
            Ui.rect(g, M, rowY + ROW_H - 5, (float) (520 * hv), 1, Kit.a(Kit.accentStrong(), hv * in));
            mono(g, String.format(Locale.ROOT, "%02d", i + 1), M + (float) (2 * pr), rowY + 18,
                    Kit.a(Kit.mix(Kit.LABEL_4, Kit.LABEL_2, hv), in));
            Kit.text(g, item.label(), M + 46 + shift, rowY + 10 + (float) (1.5 * pr), 28, face, Kit.a(base, in));
            float lw = Kit.width(item.label(), 28, face);
            if (hv > 0.02) {
                Kit.text(g, "↗", M + 46 + shift + lw + 16, rowY + 12 - (float) (3 * hv), 22, JilatonType.Face.MEDIUM,
                        Kit.a(Kit.LABEL, hv * in));
            }
            if (!item.hint().isEmpty()) {
                String hint = item.hint().toUpperCase(Locale.ROOT);
                mono(g, hint, M + 520 - Ui.spacedWidth(hint, Kit.CAPTION, 1.6F, JilatonType.Face.MONO), rowY + 18,
                        Kit.a(Kit.mix(Kit.LABEL_4, Kit.LABEL_3, hv), in));
            }
        }

        session(g, mx, my, previous, focusKey);

        // Bottom rule: quiet actions.
        double foot = Kit.enter(openedAt, 0.45);
        float fy = 650;
        Ui.rect(g, M, fy - 14, (Canvas.W - 2 * M) * (float) foot, 1, Kit.a(0xFFFFFFFF, 0.1));
        hits.add(new Hit("more", M - 6, fy - 6, 150, 32, () -> {
            vanillaRequested = true;
            minecraft.setScreen(new PauseScreen(true));
        }));
        double mh = Kit.spring("pause:more", over("more", mx, my, previous, focusKey));
        mono(g, "MORE OPTIONS", M, fy + 6, Kit.a(Kit.mix(Kit.LABEL_3, Kit.LABEL, mh), foot));
        Ui.rect(g, M, fy + 20, (float) (Ui.spacedWidth("MORE OPTIONS", Kit.CAPTION, 1.6F, JilatonType.Face.MONO) * mh), 1, Kit.a(Kit.LABEL, mh));
        String leave = CommonComponents.disconnectButtonLabel(minecraft.isLocalServer()).getString().toUpperCase(Locale.ROOT);
        float lw = Ui.spacedWidth(leave, Kit.CAPTION, 1.6F, JilatonType.Face.MONO);
        float lx = Canvas.W - M - lw;
        hits.add(new Hit("leave", lx - 8, fy - 6, lw + 16, 32, this::disconnect));
        double lh = Kit.spring("pause:leave", over("leave", mx, my, previous, focusKey));
        mono(g, leave, lx, fy + 6, Kit.a(Kit.mix(Kit.a(Kit.RED, 0.75), Kit.RED, lh), foot));
        Ui.rect(g, lx, fy + 20, (float) (lw * lh), 1, Kit.a(Kit.RED, lh));
        canvas.end(g);
    }

    /** Right column: what is running right now. */
    private void session(GuiGraphics g, double mx, double my, List<Hit> previous, String focusKey) {
        double in = Kit.enter(openedAt, 0.3);
        float x = 830, y = 262 + (float) ((1 - in) * 16), w = Canvas.W - M - x, h = 320;
        Ui.round(g, x, y, w, h, 6, Kit.a(0xFF080808, 0.82 * in));
        Paint.outline(g, x, y, w, h, 6, 0, Kit.a(0xFFFFFFFF, 0.1 * in));
        Ui.rect(g, x + 6, y, w - 12, 1, Kit.a(0xFFFFFFFF, 0.28 * in));
        mono(g, "SESSION", x + 20, y + 20, Kit.a(Kit.LABEL_3, in));

        int on = 0;
        StringBuilder names = new StringBuilder();
        for (ClientModule m : JilatonClient.moduleManager().all()) {
            if (!m.enabled()) continue;
            on++;
            if (names.length() < 60) names.append(names.length() == 0 ? "" : ", ").append(m.name());
        }
        Kit.text(g, String.valueOf(on), x + 20, y + 40, 44, JilatonType.Face.DISPLAY_HEAVY, Kit.a(Kit.LABEL, in));
        float nw = Kit.width(String.valueOf(on), 44, JilatonType.Face.DISPLAY_HEAVY);
        Kit.text(g, on == 1 ? "module running" : "modules running", x + 28 + nw, y + 62, 18, JilatonType.Face.SERIF_ITALIC,
                Kit.a(Kit.LABEL_2, in));
        Kit.text(g, Ui.fit(names.toString(), w - 40, Kit.FOOTNOTE, JilatonType.Face.REGULAR), x + 20, y + 98, Kit.FOOTNOTE,
                JilatonType.Face.REGULAR, Kit.a(Kit.LABEL_3, in));
        Ui.rect(g, x + 20, y + 124, w - 40, 1, Kit.a(0xFFFFFFFF, 0.08 * in));

        // Now playing with play/pause.
        MusicCenter music = MusicCenter.get();
        NowPlaying np = music.nowPlaying();
        mono(g, "NOW PLAYING", x + 20, y + 140, Kit.a(Kit.LABEL_3, in));
        boolean live = music.source() != MusicCenter.Source.OFF && np.hasTrack();
        Kit.text(g, Ui.fit(live ? np.title() : "Nothing playing", w - 100, 17, JilatonType.Face.DISPLAY), x + 20, y + 160, 17,
                JilatonType.Face.DISPLAY, Kit.a(live ? Kit.LABEL : Kit.LABEL_3, in));
        if (live) {
            Kit.text(g, Ui.fit(np.artist(), w - 100, Kit.FOOTNOTE, JilatonType.Face.REGULAR), x + 20, y + 184, Kit.FOOTNOTE,
                    JilatonType.Face.REGULAR, Kit.a(Kit.LABEL_2, in));
            float px = x + w - 58, py = y + 156;
            hits.add(new Hit("playpause", px, py, 38, 38, music::togglePause));
            double hv = Kit.spring("pause:pp", over("playpause", mx, my, previous, focusKey));
            Ui.circle(g, px + 19, py + 19, (float) (19 + 1.5 * hv), Kit.a(Kit.LABEL, in));
            if (np.playing()) {
                Ui.rect(g, px + 13, py + 12, 4, 14, Kit.a(0xFF0A0A0A, in));
                Ui.rect(g, px + 21, py + 12, 4, 14, Kit.a(0xFF0A0A0A, in));
            } else {
                Kit.icon(g, "play", px + 20, py + 19, 15, Kit.a(0xFF0A0A0A, in));
            }
        }
        Ui.rect(g, x + 20, y + 228, w - 40, 1, Kit.a(0xFFFFFFFF, 0.08 * in));

        // Position and dimension.
        if (minecraft.player != null) {
            var p = minecraft.player.blockPosition();
            mono(g, "POSITION", x + 20, y + 244, Kit.a(Kit.LABEL_3, in));
            Kit.text(g, p.getX() + "  " + p.getY() + "  " + p.getZ(), x + 20, y + 264, 17, JilatonType.Face.MONO_MEDIUM, Kit.a(Kit.LABEL, in));
            String dim = minecraft.level == null ? "" : minecraft.level.dimension().identifier().getPath().replace('_', ' ').toUpperCase(Locale.ROOT);
            mono(g, dim, x + w - 20 - Ui.spacedWidth(dim, Kit.CAPTION, 1.6F, JilatonType.Face.MONO), y + 270, Kit.a(Kit.LABEL_3, in));
        }
    }

    private boolean over(String key, double mx, double my, List<Hit> previous, String focusKey) {
        if (key.equals(focusKey)) return true;
        for (Hit h : previous) if (h.key().equals(key)) return h.inside(mx, my);
        return false;
    }

    private static void mono(GuiGraphics g, String s, float x, float y, int color) {
        Ui.spaced(g, s, x, y, Kit.CAPTION, 1.6F, JilatonType.Face.MONO, color);
    }

    private String worldName() {
        if (minecraft.getSingleplayerServer() != null) return minecraft.getSingleplayerServer().getWorldData().getLevelName();
        var server = minecraft.getCurrentServer();
        return server != null ? server.name : "";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = canvas.x(event.x()), my = canvas.y(event.y());
            for (Hit h : hits) {
                if (h.inside(mx, my)) {
                    pressed = h.key();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (pressed != null) {
            String key = pressed;
            pressed = null;
            double mx = canvas.x(event.x()), my = canvas.y(event.y());
            for (Hit h : hits) {
                if (h.key().equals(key) && h.inside(mx, my)) {
                    h.run().run();
                    break;
                }
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (!hits.isEmpty()) {
            if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_TAB) {
                focused = (focused + 1) % hits.size();
                return true;
            }
            if (key == GLFW.GLFW_KEY_UP) {
                focused = focused <= 0 ? hits.size() - 1 : focused - 1;
                return true;
            }
            if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && focused >= 0 && focused < hits.size()) {
                hits.get(focused).run().run();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
