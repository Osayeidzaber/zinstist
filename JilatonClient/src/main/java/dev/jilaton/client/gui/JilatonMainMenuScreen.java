package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.gui.design.Ui;
import dev.jilaton.client.music.MusicCenter;
import dev.jilaton.client.music.NowPlaying;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Title screen, built to the "Main menu" design board (development/GUI_REDESIGN.md §1): a left column with the mark,
 * wordmark and five text-link rows whose hover brightens them and draws an underline from the left; the player
 * walks in on the right ({@link MenuAvatar}); a music pill bottom-left and versions bottom-right. Laid out in board
 * coordinates on a {@link Canvas}.
 */
public final class JilatonMainMenuScreen extends Screen {
    /** Inter instead of the vanilla pixel font for everything this screen draws ({@link JilatonType}). */
    private final Font font = JilatonType.font();
    private static final float NAV_X = 112, NAV_Y = 224, NAV_W = 360, ROW_H = 44;
    private static final float AVATAR_CX = 990, AVATAR_FLOOR = 528, AVATAR_H = 360;

    private record Row(String label, String hint, boolean muted, Runnable action) { }

    private final Canvas canvas = new Canvas();
    private final double[] hover = new double[5];
    private List<Row> rows = List.of();
    private MenuAvatar avatar;
    private long openedAt;
    private long lastFrame;
    private int focused = -1;
    private String lastWorld;
    private CompletableFuture<List<LevelSummary>> worlds;
    private final String version = FabricLoader.getInstance().getModContainer("jilaton")
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    private final String loader = FabricLoader.getInstance().getModContainer("fabricloader")
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");

    public JilatonMainMenuScreen() {
        super(Component.literal("Jilaton Client"));
    }

    @Override
    protected void init() {
        canvas.fit(width, height);
        rows = List.of(
                new Row("Singleplayer", lastWorld == null ? "Your worlds" : lastWorld, false,
                        () -> minecraft.setScreen(new JilatonWorldsScreen(this))),
                new Row("Multiplayer", "Servers · LAN", false,
                        () -> minecraft.setScreen(minecraft.options.skipMultiplayerWarning
                                ? new JilatonServersScreen(this)
                                : new net.minecraft.client.gui.screens.multiplayer.SafetyScreen(this))),
                new Row("Client", "Modules · Visuals · Music", false, () -> JilatonClient.openClickGui(this)),
                new Row("Minecraft settings", "", false,
                        () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))),
                new Row("Quit", "", true, minecraft::stop));
        if (avatar == null) {
            try {
                avatar = new MenuAvatar(minecraft);
            } catch (RuntimeException exception) {
                avatar = null;
            }
        }
        if (worlds == null) {
            try {
                var source = minecraft.getLevelSource();
                worlds = source.loadLevelSummaries(source.findLevelCandidates());
            } catch (Exception exception) {
                worlds = CompletableFuture.completedFuture(List.of());
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        if (openedAt == 0L || minecraft.getOverlay() != null) openedAt = now;
        double dt = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        boolean reduced = JilatonClient.clientConfig().reducedMotion();
        resolveLastWorld();
        int accent = Ui.accent();
        double mx = canvas.x(mouseX), my = canvas.y(mouseY);

        graphics.fill(0, 0, width, height, Ui.BACKDROP);
        canvas.begin(graphics);
        backdrop(graphics, accent);

        // Brand row.
        double brandIn = stagger(now, 0.05, reduced);
        float lift = (float) (10 * (1 - brandIn));
        Ui.markTile(graphics, NAV_X, 132 + lift, 52, 14, true, accent);
        Ui.spaced(graphics, "JILATON", NAV_X + 68, 140 + lift, 22, 7, JilatonType.Face.BOLD,
                Ui.alpha(Ui.TEXT, brandIn));
        Ui.text(graphics, "Client · Minecraft 1.21.11", NAV_X + 68, 168 + lift, 12, Ui.alpha(Ui.TEXT_2, brandIn));

        // Navigation rows: text links, hover = accent + underline growing from the left.
        int hoveredRow = -1;
        for (int i = 0; i < rows.size(); i++) {
            float y = NAV_Y + i * ROW_H;
            if (Ui.inside(mx, my, NAV_X, y, NAV_W, ROW_H - 4)) hoveredRow = i;
        }
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            boolean on = i == hoveredRow || (hoveredRow < 0 && i == focused);
            hover[i] = Ui.approach(hover[i], on ? 1 : 0, dt, 14, reduced);
            double in = stagger(now, 0.25 + i * 0.04, reduced);
            float y = NAV_Y + i * ROW_H + (float) (8 * (1 - in));
            int base = row.muted() ? Ui.TEXT_2 : Ui.TEXT;
            int color = Ui.alpha(Ui.mix(base, accent, hover[i]), in);
            Ui.text(graphics, row.label(), NAV_X, y + 10, 17,
                    hover[i] > 0.5 ? JilatonType.Face.SEMIBOLD : JilatonType.Face.MEDIUM, color);
            if (!row.hint().isEmpty()) {
                String hint = Ui.fit(row.hint(), 170, 12, JilatonType.Face.REGULAR);
                Ui.textRight(graphics, hint, NAV_X + NAV_W, y + 14, 12, JilatonType.Face.REGULAR,
                        Ui.alpha(Ui.mix(Ui.TEXT_3, Ui.TEXT_2, hover[i]), in));
            }
            if (hover[i] > 0.01) {
                float line = (float) (120 * hover[i]);
                Ui.rect(graphics, NAV_X, y + 34, line, 1, Ui.alpha(accent, in));
                Ui.rect(graphics, NAV_X, y + 33, line, 3, Ui.alpha(accent, 0.18 * hover[i] * in));
            }
        }

        // Account chip above the player.
        String name = minecraft.getUser().getName();
        float chipW = 48 + Ui.width(name, 13, JilatonType.Face.MEDIUM);
        float chipX = AVATAR_CX - chipW / 2, chipY = 96;
        boolean chipHover = Ui.inside(mx, my, chipX, chipY, chipW, 36);
        Ui.card(graphics, chipX, chipY, chipW, 36, 10, Ui.alpha(chipHover ? Ui.RAISED : Ui.CARD, 0.85), Ui.STROKE);
        Ui.circle(graphics, chipX + 18, chipY + 18, 4, Ui.ONLINE);
        Ui.text(graphics, name, chipX + 30, chipY + 12, 13, JilatonType.Face.MEDIUM, Ui.TEXT);
        Ui.text(graphics, "⌄", chipX + chipW - 16, chipY + 9, 13, Ui.TEXT_2);

        // Chips under the player: only ones that lead somewhere.
        float chipsY = AVATAR_FLOOR + 30;
        String[] chips = {"Skin", "3D character"};
        float total = 0;
        for (String c : chips) total += Ui.width(c, 12, JilatonType.Face.MEDIUM) + 24 + 8;
        float cx = AVATAR_CX - (total - 8) / 2;
        for (String c : chips) {
            float w = Ui.width(c, 12, JilatonType.Face.MEDIUM) + 24;
            boolean h = Ui.inside(mx, my, cx, chipsY, w, 28);
            if (h) Ui.round(graphics, cx, chipsY, w, 28, 8, Ui.RAISED);
            Ui.outline(graphics, cx, chipsY, w, 28, 8, Ui.STROKE);
            Ui.text(graphics, c, cx + 12, chipsY + 9, 12, JilatonType.Face.MEDIUM, h ? Ui.TEXT : Ui.TEXT_2);
            cx += w + 8;
        }

        musicPill(graphics, mx, my);
        Ui.textRight(graphics, "Jilaton " + version, 1240, 650, 11, JilatonType.Face.MONO, Ui.TEXT_3);
        Ui.textRight(graphics, "Fabric " + loader, 1240, 668, 11, JilatonType.Face.MONO, Ui.TEXT_3);
        canvas.end(graphics);

        if (avatar != null) {
            int floor = canvas.screenY(AVATAR_FLOOR);
            avatar.render(graphics, canvas.screenX(AVATAR_CX), floor, Math.round(AVATAR_H * canvas.scale()),
                    canvas.screenX(700), width, openedAt, now, mouseX, mouseY, reduced,
                    JilatonClient.clientConfig().animationIntensity());
        }
    }

    private void backdrop(GuiGraphics graphics, int accent) {
        Ui.glow(graphics, 1010, 230, 450, accent & 0xFFFFFF, 0.07);
        Ui.glow(graphics, 120, 640, 380, 0xFFFFFF, 0.03);
        Ui.vGradient(graphics, 0, 540, Canvas.W, 180, 0x000B0A0E, Ui.DEEP);
        int[][] stars = {{700, 90, 2}, {1120, 160, 2}, {980, 60, 1}, {1210, 330, 2}, {860, 40, 1}, {560, 120, 1}};
        for (int[] s : stars) Ui.rect(graphics, s[0], s[1], s[2], s[2], Ui.alpha(Ui.TEXT, s[2] == 2 ? 0.35 : 0.5));
    }

    private void musicPill(GuiGraphics graphics, double mx, double my) {
        MusicCenter music = MusicCenter.get();
        NowPlaying now = music.nowPlaying();
        boolean off = music.source() == MusicCenter.Source.OFF;
        String title = off ? "Music is off" : now.hasTrack() ? now.title() : "Nothing playing";
        String sub = off ? "Press M to pick a source" : now.hasTrack() ? now.source() : "Press M to open Music";
        float w = 60 + Math.max(Ui.width(title, 12, JilatonType.Face.MEDIUM), Ui.width(sub, 11, JilatonType.Face.REGULAR));
        w = Math.min(w, 320);
        float x = NAV_X, y = 640;
        boolean h = Ui.inside(mx, my, x, y, w, 40);
        Ui.card(graphics, x, y, w, 40, 12, Ui.alpha(h ? Ui.RAISED : Ui.CARD, 0.7), Ui.STROKE);
        Ui.round(graphics, x + 6, y + 6, 28, 28, 7, Ui.RAISED);
        Ui.text(graphics, "♪", x + 15, y + 13, 13, Ui.TEXT_2);
        Ui.text(graphics, Ui.fit(title, w - 56, 12, JilatonType.Face.MEDIUM), x + 46, y + 7, 12, JilatonType.Face.MEDIUM, Ui.TEXT);
        Ui.text(graphics, Ui.fit(sub, w - 56, 11, JilatonType.Face.REGULAR), x + 46, y + 22, 11, Ui.TEXT_2);
    }

    private void resolveLastWorld() {
        if (lastWorld != null || worlds == null || !worlds.isDone()) return;
        List<LevelSummary> list = worlds.getNow(List.of());
        lastWorld = list.stream().max(Comparator.comparingLong(LevelSummary::getLastPlayed))
                .map(LevelSummary::getLevelName).orElse("Create a world");
        rows = List.of(new Row(rows.get(0).label(), lastWorld, false, rows.get(0).action()),
                rows.get(1), rows.get(2), rows.get(3), rows.get(4));
    }

    private double stagger(long now, double delay, boolean reduced) {
        return reduced ? 1 : Motion.staggered(openedAt, now, delay, 0.5, Motion.SETTLE);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = canvas.x(event.x()), my = canvas.y(event.y());
            for (int i = 0; i < rows.size(); i++) {
                if (Ui.inside(mx, my, NAV_X, NAV_Y + i * ROW_H, NAV_W, ROW_H - 4)) {
                    rows.get(i).action().run();
                    return true;
                }
            }
            String name = minecraft.getUser().getName();
            float chipW = 48 + Ui.width(name, 13, JilatonType.Face.MEDIUM);
            if (Ui.inside(mx, my, AVATAR_CX - chipW / 2, 96, chipW, 36)) {
                minecraft.setScreen(new AccountsScreen(this));
                return true;
            }
            float chipsY = AVATAR_FLOOR + 30;
            float skinW = Ui.width("Skin", 12, JilatonType.Face.MEDIUM) + 24;
            float charW = Ui.width("3D character", 12, JilatonType.Face.MEDIUM) + 24;
            float cx = AVATAR_CX - (skinW + charW + 8) / 2;
            if (Ui.inside(mx, my, cx, chipsY, skinW, 28)) {
                minecraft.setScreen(new AccountsScreen(this));
                return true;
            }
            if (Ui.inside(mx, my, cx + skinW + 8, chipsY, charW, 28)) {
                JilatonClient.openClickGui(this);
                return true;
            }
            if (Ui.inside(mx, my, NAV_X, 640, 320, 40)) {
                minecraft.setScreen(new MusicCenterScreen(this));
                return true;
            }
            if (avatar != null && avatar.mouseClicked(event.x(), event.y())) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (avatar != null && avatar.mouseDragged(dragX)) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (avatar != null) avatar.mouseReleased();
        return super.mouseReleased(event);
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
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) && focused >= 0) {
            rows.get(focused).action().run();
            return true;
        }
        return super.keyPressed(event);
    }
}
