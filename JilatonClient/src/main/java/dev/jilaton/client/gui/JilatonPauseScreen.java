package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.gui.design.Kit;
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

/**
 * Pause menu on the {@link Kit} design system: the world stays visible behind Minecraft's GPU blur, with one
 * centred sheet. A large Resume button, a two-column grid of destinations (client, music, options, progress,
 * statistics, LAN), a status strip (active modules, now playing with play/pause) and the quiet actions — more
 * options and disconnect — at the bottom. Esc resumes; arrows/Tab move focus; Enter activates.
 */
public final class JilatonPauseScreen extends Screen {
    /** Set while "More options" opens vanilla's pause menu, so the redirect lets that one through. */
    public static boolean vanillaRequested;

    private static final float SHEET_W = 520, X0 = (Canvas.W - SHEET_W) / 2;

    private record Item(String key, String label, String detail, String icon, Runnable run) { }

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
        items.add(new Item("client", "Client", JilatonClient.clickGuiKeyName(), "grid", () -> JilatonClient.openClickGui(this)));
        items.add(new Item("music", "Music", JilatonClient.musicKeyName(), "music", () -> minecraft.setScreen(new MusicCenterScreen(this))));
        items.add(new Item("options", "Options", "", "gear", () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
        if (minecraft.player != null) {
            items.add(new Item("advancements", "Advancements", "", "sparkle", () -> minecraft.setScreen(
                    new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this))));
            items.add(new Item("stats", "Statistics", "", "person", () -> minecraft.setScreen(
                    new StatsScreen(this, minecraft.player.getStats()))));
        }
        if (minecraft.hasSingleplayerServer() && !minecraft.getSingleplayerServer().isPublished()) {
            items.add(new Item("lan", "Open to LAN", "", "globe", () -> minecraft.setScreen(new ShareToLanScreen(this))));
        }
        if (openedAt == 0) openedAt = System.nanoTime();
    }

    private void disconnect() {
        minecraft.getReportingContext().draftReportHandled(minecraft, this,
                () -> minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Real frosted glass: Minecraft's own blur pass over the live world, then a soft dim.
        renderBlurredBackground(graphics);
        graphics.fill(0, 0, width, height, 0x8A000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        double mx = canvas.x(mouseX), my = canvas.y(mouseY);
        hits.clear();
        canvas.begin(graphics);

        int rows = (items.size() + 1) / 2;
        float sheetH = 294 + rows * 72;
        float y0 = (Canvas.H - sheetH) / 2;
        double in = Kit.enter(openedAt, 0);
        y0 += (float) (14 * (1 - in));
        Kit.shadow(graphics, X0, y0, SHEET_W, sheetH, Kit.R_PANEL, in);
        Ui.round(graphics, X0, y0, SHEET_W, sheetH, Kit.R_PANEL, Kit.a(Kit.SURFACE, 0.86 * in));
        dev.jilaton.client.gui.design.Paint.outline(graphics, X0, y0, SHEET_W, sheetH, Kit.R_PANEL, 0, Kit.a(0xFFFFFFFF, 0.08 * in));

        float x = X0 + 28, w = SHEET_W - 56, y = y0 + 28;
        Kit.text(graphics, "Paused", x, y, Kit.TITLE, JilatonType.Face.BOLD, Kit.a(Kit.LABEL, in));
        Ui.textRight(graphics, worldName(), x + w, y + 9, Kit.FOOTNOTE, JilatonType.Face.MEDIUM, Kit.a(Kit.LABEL_2, in));
        y += 52;

        // Resume.
        hits.add(new Hit("resume", x, y, w, 50, this::onClose));
        Kit.button(graphics, "pause:resume", "Back to game", x, y, w, 50, 0, active("resume", mx, my), "resume".equals(pressed));
        y += 68;

        // Destinations grid.
        float cellW = (w - 12) / 2;
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            float cx = x + (i % 2) * (cellW + 12), cy = y + (i / 2) * 72;
            hits.add(new Hit(item.key(), cx, cy, cellW, 60, item.run()));
            double hv = Kit.spring("pause:" + item.key(), active(item.key(), mx, my));
            double pr = Kit.spring("pause:p:" + item.key(), item.key().equals(pressed));
            float sink = (float) (1.2 * pr);
            Ui.round(graphics, cx, cy + sink, cellW, 60, Kit.R_TILE, Kit.mix(Kit.SURFACE_2, Kit.SURFACE_3, hv));
            Ui.round(graphics, cx + 14, cy + 14 + sink, 32, 32, 9, Kit.mix(Kit.SURFACE_3, Kit.accent(), 0.10 * hv));
            Kit.icon(graphics, item.icon(), cx + 30, cy + 30 + sink, 17, Kit.mix(Kit.LABEL_2, Kit.LABEL, hv));
            Kit.text(graphics, item.label(), cx + 58, cy + 22 + sink, Kit.HEADLINE, JilatonType.Face.SEMIBOLD, Kit.LABEL);
            if (!item.detail().isEmpty()) {
                float tw = Kit.tagWidth(item.detail());
                Kit.tag(graphics, item.detail(), cx + cellW - tw - 14, cy + 20 + sink, Kit.SURFACE_3, Kit.LABEL_2);
            }
        }
        y += rows * 72 + 4;

        // Status strip: modules on, now playing.
        Ui.round(graphics, x, y, w, 54, Kit.R_TILE, Kit.a(Kit.SURFACE_2, 0.7));
        int on = 0;
        for (ClientModule m : JilatonClient.moduleManager().all()) if (m.enabled()) on++;
        Ui.circle(graphics, x + 20, y + 27, 4, on > 0 ? Kit.GREEN : Kit.LABEL_3);
        Kit.text(graphics, on + (on == 1 ? " module on" : " modules on"), x + 32, y + 20, Kit.CALLOUT, JilatonType.Face.MEDIUM, Kit.LABEL);
        MusicCenter music = MusicCenter.get();
        NowPlaying np = music.nowPlaying();
        if (music.source() != MusicCenter.Source.OFF && np.hasTrack()) {
            float px = x + w - 40;
            hits.add(new Hit("playpause", px, y + 11, 32, 32, music::togglePause));
            double hv = Kit.spring("pause:pp", active("playpause", mx, my));
            Ui.circle(graphics, px + 16, y + 27, 16, Kit.mix(Kit.SURFACE_3, Kit.LABEL, 0.15 + 0.15 * hv));
            if (np.playing()) {
                Ui.rect(graphics, px + 11, y + 21, 3.5F, 12, Kit.LABEL);
                Ui.rect(graphics, px + 18, y + 21, 3.5F, 12, Kit.LABEL);
            } else {
                Kit.icon(graphics, "play", px + 17, y + 27, 14, Kit.LABEL);
            }
            String track = Ui.fit(np.title() + "  ·  " + np.artist(), w - 230, Kit.FOOTNOTE, JilatonType.Face.REGULAR);
            Ui.textRight(graphics, track, px - 12, y + 21, Kit.FOOTNOTE, JilatonType.Face.REGULAR, Kit.LABEL_2);
        } else {
            Ui.textRight(graphics, "Nothing playing", x + w - 16, y + 21, Kit.FOOTNOTE, JilatonType.Face.REGULAR, Kit.LABEL_3);
        }
        y += 74;

        // Quiet actions.
        hits.add(new Hit("more", x, y, 150, 40, () -> {
            vanillaRequested = true;
            minecraft.setScreen(new PauseScreen(true));
        }));
        Kit.button(graphics, "pause:more", "More options", x, y, 150, 40, 2, active("more", mx, my), false);
        String leave = CommonComponents.disconnectButtonLabel(minecraft.isLocalServer()).getString();
        float lw = Kit.width(leave, Kit.CALLOUT, JilatonType.Face.MEDIUM) + 40;
        hits.add(new Hit("leave", x + w - lw, y, lw, 40, this::disconnect));
        double lh = Kit.spring("pause:leave", active("leave", mx, my));
        if (lh > 0.01) Ui.round(graphics, x + w - lw, y, lw, 40, 20, Kit.a(Kit.RED, 0.14 * lh));
        Ui.textCentre(graphics, leave, x + w - lw / 2, y + 14, Kit.CALLOUT, JilatonType.Face.MEDIUM, Kit.mix(Kit.a(Kit.RED, 0.85), Kit.RED, lh));

        canvas.end(graphics);
    }

    private boolean active(String key, double mx, double my) {
        if (focused >= 0 && focused < hits.size() && hits.get(focused).key().equals(key)) return true;
        for (Hit h : hits) if (h.key().equals(key)) return h.inside(mx, my);
        // Hits are registered as each control draws; a control reads its own bounds before registering.
        return false;
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
            if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_RIGHT) {
                focused = (focused + 1) % hits.size();
                return true;
            }
            if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_LEFT) {
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
