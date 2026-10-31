package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.gui.design.Ui;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.music.MusicCenter;
import dev.jilaton.client.music.NowPlaying;
import net.minecraft.client.gui.Font;
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
 * Pause menu, built to the "Pause menu" design board (development/GUI_REDESIGN.md §9). The world stays visible
 * behind a left-to-right scrim; the left column holds the actions in main-menu style, the right column shows the
 * active modules, music and account. Every vanilla action stays reachable: the rarely used ones (feedback, bug
 * reports, player reporting, server links) through "More options", which opens vanilla's own pause menu.
 */
public final class JilatonPauseScreen extends Screen {
    /** Inter instead of the vanilla pixel font for everything this screen draws ({@link JilatonType}). */
    private final Font font = JilatonType.font();
    /** Set while "More options" opens vanilla's pause menu, so the redirect lets that one through. */
    public static boolean vanillaRequested;
    private static final float LEFT = 112, NAV_Y = 196, ROW_H = 42, NAV_W = 380, RIGHT_X = 864, RIGHT_W = 320;

    private record Row(String label, String key, int color, Runnable action) { }

    private final Canvas canvas = new Canvas();
    private final List<Row> rows = new ArrayList<>();
    private double[] hover = new double[0];
    private int focused;
    private long lastFrame;

    public JilatonPauseScreen() {
        super(Component.translatable("menu.game"));
    }

    @Override
    protected void init() {
        canvas.fit(width, height);
        rows.clear();
        rows.add(new Row("Back to game", "", Ui.TEXT, this::onClose));
        rows.add(new Row("Client", JilatonClient.clickGuiKeyName(), Ui.TEXT, () -> JilatonClient.openClickGui(this)));
        rows.add(new Row("Music", JilatonClient.musicKeyName(), Ui.TEXT, () -> minecraft.setScreen(new MusicCenterScreen(this))));
        if (minecraft.player != null) {
            rows.add(new Row("Advancements", "", Ui.TEXT, () -> minecraft.setScreen(
                    new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this))));
            rows.add(new Row("Statistics", "", Ui.TEXT, () -> minecraft.setScreen(
                    new StatsScreen(this, minecraft.player.getStats()))));
        }
        rows.add(new Row("Options", "", Ui.TEXT, () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
        if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null
                && !minecraft.getSingleplayerServer().isPublished())
            rows.add(new Row("Open to LAN", "", Ui.TEXT, () -> minecraft.setScreen(new ShareToLanScreen(this))));
        rows.add(new Row("More options", "", Ui.TEXT_2, () -> {
            vanillaRequested = true;
            minecraft.setScreen(new PauseScreen(true));
        }));
        rows.add(new Row(CommonComponents.disconnectButtonLabel(minecraft.isLocalServer()).getString(), "", Ui.DANGER,
                () -> minecraft.getReportingContext().draftReportHandled(minecraft, this,
                        () -> minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true)));
        hover = new double[rows.size()];
    }

    private String place() {
        if (minecraft.getSingleplayerServer() != null)
            return minecraft.getSingleplayerServer().getWorldData().getLevelName();
        var server = minecraft.getCurrentServer();
        return server == null ? "Multiplayer" : server.name;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The world shows through; the scrim below is the only background.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        boolean reduced = JilatonClient.clientConfig().reducedMotion();
        int accent = Ui.accent();
        double mx = canvas.x(mouseX), my = canvas.y(mouseY);

        graphics.fillGradient(0, 0, width, height, 0xEB08070A, 0xEB08070A);
        canvas.begin(graphics);
        // Lighter toward the right so the world reads there (92% → 55%).
        for (int i = 0; i < 16; i++) {
            float x = 640 + i * 40;
            Ui.rect(graphics, x, -400, 40, 1520, Ui.alpha(0xFF08070A, -0.37 * i / 15.0 + 0.0));
        }

        Ui.spaced(graphics, place().toUpperCase(java.util.Locale.ROOT), LEFT, 120, 12, 1.4F, JilatonType.Face.MEDIUM, Ui.TEXT_2);
        Ui.text(graphics, "Paused", LEFT, 140, 34, JilatonType.Face.BOLD, Ui.TEXT);

        int hovered = -1;
        for (int i = 0; i < rows.size(); i++)
            if (Ui.inside(mx, my, LEFT, NAV_Y + i * ROW_H, NAV_W, ROW_H - 4)) hovered = i;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            boolean on = i == hovered || (hovered < 0 && i == focused);
            hover[i] = Ui.approach(hover[i], on ? 1 : 0, dt, 14, reduced);
            float y = NAV_Y + i * ROW_H;
            int color = row.color() == Ui.DANGER ? row.color() : Ui.mix(row.color(), accent, hover[i]);
            Ui.text(graphics, row.label(), LEFT, y + 10, 17,
                    hover[i] > 0.5 ? JilatonType.Face.SEMIBOLD : JilatonType.Face.MEDIUM, color);
            if (!row.key().isEmpty())
                Ui.textRight(graphics, row.key(), LEFT + NAV_W, y + 14, 11, JilatonType.Face.MONO, Ui.TEXT_3);
            if (hover[i] > 0.01) {
                int line = row.color() == Ui.DANGER ? Ui.DANGER : accent;
                Ui.rect(graphics, LEFT, y + 33, (float) (110 * hover[i]), 1, line);
            }
        }

        // Right column: active modules, music, account.
        float y = 140;
        List<ClientModule> active = JilatonClient.moduleManager().all().stream().filter(ClientModule::enabled).toList();
        float chipsHeight = layoutChips(null, active, 0, 0);
        float cardH = 50 + chipsHeight;
        Ui.card(graphics, RIGHT_X, y, RIGHT_W, cardH, 14, Ui.alpha(Ui.PANEL, 0.82), Ui.STROKE);
        Ui.spaced(graphics, "ACTIVE MODULES", RIGHT_X + 16, y + 16, 11, 1.1F, JilatonType.Face.MEDIUM, Ui.TEXT_3);
        layoutChips(graphics, active, RIGHT_X + 16, y + 38);
        y += cardH + 12;

        MusicCenter music = MusicCenter.get();
        NowPlaying now2 = music.nowPlaying();
        Ui.card(graphics, RIGHT_X, y, RIGHT_W, 68, 14, Ui.alpha(Ui.PANEL, 0.82), Ui.STROKE);
        Ui.round(graphics, RIGHT_X + 12, y + 12, 44, 44, 8, Ui.RAISED);
        Ui.textCentre(graphics, "♪", RIGHT_X + 34, y + 24, 18, JilatonType.Face.REGULAR, Ui.TEXT_2);
        boolean off = music.source() == MusicCenter.Source.OFF;
        Ui.text(graphics, Ui.fit(off ? "Music is off" : now2.hasTrack() ? now2.title() : "Nothing playing", 180, 13,
                JilatonType.Face.SEMIBOLD), RIGHT_X + 68, y + 18, 13, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        Ui.text(graphics, Ui.fit(off ? "Pick a source in Music" : now2.source(), 180, 11, JilatonType.Face.REGULAR),
                RIGHT_X + 68, y + 36, 11, Ui.TEXT_2);
        if (!off) {
            boolean playHover = Ui.inside(mx, my, RIGHT_X + RIGHT_W - 46, y + 17, 34, 34);
            Ui.circle(graphics, RIGHT_X + RIGHT_W - 29, y + 34, 17, playHover ? Ui.mix(Ui.TEXT, accent, 0.2) : Ui.TEXT);
            if (now2.playing()) {
                Ui.rect(graphics, RIGHT_X + RIGHT_W - 34, y + 28, 3, 12, Ui.PANEL);
                Ui.rect(graphics, RIGHT_X + RIGHT_W - 27, y + 28, 3, 12, Ui.PANEL);
            } else {
                for (int k = 0; k < 6; k++) Ui.rect(graphics, RIGHT_X + RIGHT_W - 32 + k, y + 28 + k, 1, 12 - 2 * k, Ui.PANEL);
            }
        }
        y += 80;

        boolean accHover = Ui.inside(mx, my, RIGHT_X, y, RIGHT_W, 44);
        Ui.card(graphics, RIGHT_X, y, RIGHT_W, 44, 14, Ui.alpha(Ui.mix(Ui.PANEL, Ui.RAISED, Ui.ease("pause:account", accHover)), 0.82), Ui.STROKE);
        Ui.face(graphics, RIGHT_X + 10, y + 8, 28);
        Ui.circle(graphics, RIGHT_X + 34, y + 34, 3.5F, Ui.ONLINE);
        Ui.text(graphics, minecraft.getUser().getName(), RIGHT_X + 48, y + 16, 13, JilatonType.Face.MEDIUM, Ui.TEXT);
        Ui.textRight(graphics, "Accounts", RIGHT_X + RIGHT_W - 16, y + 17, 12, JilatonType.Face.REGULAR, Ui.TEXT_2);
        canvas.end(graphics);
    }

    /** Lays out (and draws, when {@code graphics} is non-null) the module chips; returns their total height. */
    private float layoutChips(GuiGraphics graphics, List<ClientModule> modules, float x0, float y0) {
        float x = 0, y = 0, max = RIGHT_W - 32;
        if (modules.isEmpty()) {
            if (graphics != null) Ui.text(graphics, "None enabled", x0, y0 + 4, 12, Ui.TEXT_2);
            return 20;
        }
        for (ClientModule module : modules) {
            float w = Ui.width(module.name(), 12, JilatonType.Face.REGULAR) + 20;
            if (x > 0 && x + w > max) {
                x = 0;
                y += 32;
            }
            if (graphics != null) {
                Ui.round(graphics, x0 + x, y0 + y, w, 26, 7, Ui.RAISED);
                Ui.text(graphics, module.name(), x0 + x + 10, y0 + y + 8, 12, Ui.TEXT);
            }
            x += w + 6;
        }
        return y + 26;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = canvas.x(event.x()), my = canvas.y(event.y());
            for (int i = 0; i < rows.size(); i++) {
                if (Ui.inside(mx, my, LEFT, NAV_Y + i * ROW_H, NAV_W, ROW_H - 4)) {
                    rows.get(i).action().run();
                    return true;
                }
            }
            List<ClientModule> active = JilatonClient.moduleManager().all().stream().filter(ClientModule::enabled).toList();
            float y = 140 + 50 + layoutChips(null, active, 0, 0) + 12;
            if (MusicCenter.get().source() != MusicCenter.Source.OFF
                    && Ui.inside(mx, my, RIGHT_X + RIGHT_W - 46, y + 17, 34, 34)) {
                MusicCenter.get().togglePause();
                return true;
            }
            if (Ui.inside(mx, my, RIGHT_X, y + 80, RIGHT_W, 44)) {
                minecraft.setScreen(new AccountsScreen(this));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_TAB) {
            focused = (focused + 1) % rows.size();
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            focused = focused <= 0 ? rows.size() - 1 : focused - 1;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            rows.get(focused).action().run();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return true; }
}
