package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.design.Backdrops;
import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.gui.design.Kit;
import dev.jilaton.client.gui.design.Ui;
import dev.jilaton.client.music.MusicCenter;
import dev.jilaton.client.music.NowPlaying;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Home: a calm landing built on the {@link Kit} design system. Top bar with brand, clock and account; a hero with
 * a greeting, the wordmark and the two main actions (continue your last world, multiplayer); four quick tiles
 * (worlds, servers, client, settings); the player on the right; music and quit along the bottom. Everything
 * enters with a short stagger and every control has eased hover and press states. Keyboard: Tab/arrows move
 * focus through the actions, Enter activates.
 */
public final class JilatonMainMenuScreen extends Screen {
    private static final float HERO_X = 96;
    private static final float TILE_Y = 452, TILE_W = 190, TILE_H = 132, TILE_GAP = 16;
    private static final float AVATAR_CX = 1000, AVATAR_FLOOR = 556, AVATAR_H = 340;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    /** A clickable region: its key (for eased state), bounds and what it does. */
    private record Action(String key, float x, float y, float w, float h, Runnable run) {
        boolean hit(double mx, double my) {
            return Ui.inside(mx, my, x, y, w, h);
        }
    }

    private final Canvas canvas = new Canvas();
    private final List<Action> actions = new ArrayList<>();
    private MenuAvatar avatar;
    private long openedAt;
    private int focused = -1;
    private String pressedKey;
    private LevelSummary lastWorld;
    private int worldCount = -1;
    private CompletableFuture<List<LevelSummary>> worlds;
    private final String version = FabricLoader.getInstance().getModContainer("jilaton")
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("dev");

    public JilatonMainMenuScreen() {
        super(Component.literal("Jilaton Client"));
    }

    @Override
    protected void init() {
        canvas.fit(width, height);
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
    public boolean shouldCloseOnEsc() {
        return false;
    }

    // ---------------------------------------------------------------- actions

    private void play() {
        if (lastWorld != null) {
            minecraft.createWorldOpenFlows().openWorld(lastWorld.getLevelId(), () -> minecraft.setScreen(this));
        } else {
            minecraft.setScreen(new JilatonWorldsScreen(this));
        }
    }

    private void multiplayer() {
        minecraft.setScreen(minecraft.options.skipMultiplayerWarning
                ? new JilatonServersScreen(this)
                : new net.minecraft.client.gui.screens.multiplayer.SafetyScreen(this));
    }

    private void layoutActions() {
        actions.clear();
        actions.add(new Action("play", HERO_X, 360, 248, 52, this::play));
        actions.add(new Action("multi", HERO_X + 260, 360, 172, 52, this::multiplayer));
        String[] tiles = {"worlds", "servers", "client", "settings"};
        Runnable[] runs = {
                () -> minecraft.setScreen(new JilatonWorldsScreen(this)),
                this::multiplayer,
                () -> JilatonClient.openClickGui(this),
                () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))};
        for (int i = 0; i < 4; i++) {
            actions.add(new Action(tiles[i], HERO_X + i * (TILE_W + TILE_GAP), TILE_Y, TILE_W, TILE_H, runs[i]));
        }
        String name = minecraft.getUser().getName();
        float accW = 52 + Ui.width(name, Kit.CALLOUT, JilatonType.Face.MEDIUM);
        actions.add(new Action("account", 1184 - accW, 28, accW, 38, () -> minecraft.setScreen(new AccountsScreen(this))));
        actions.add(new Action("music", HERO_X, 640, musicWidth(), 44, () -> minecraft.setScreen(new MusicCenterScreen(this))));
        actions.add(new Action("quit", 1104, 642, 80, 40, minecraft::stop));
        actions.add(new Action("skin", AVATAR_CX - 132, AVATAR_FLOOR + 34, 124, 36, () -> minecraft.setScreen(new AccountsScreen(this))));
        actions.add(new Action("character", AVATAR_CX + 8, AVATAR_FLOOR + 34, 124, 36, () -> JilatonClient.openClickGui(this)));
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        if (openedAt == 0L || minecraft.getOverlay() != null) openedAt = now;
        resolveWorlds();
        layoutActions();
        double mx = canvas.x(mouseX), my = canvas.y(mouseY);
        String hovered = null;
        for (Action a : actions) if (a.hit(mx, my)) hovered = a.key();
        String focusKey = focused >= 0 && focused < actions.size() ? actions.get(focused).key() : null;

        Backdrops.drawScreen(graphics, width, height, partialTick);
        canvas.begin(graphics);
        Backdrops.drawBoard(graphics);

        topBar(graphics, now, hovered, focusKey);
        hero(graphics, now, hovered, focusKey);
        tiles(graphics, now, hovered, focusKey);
        bottom(graphics, now, hovered, focusKey);
        avatarChips(graphics, now, hovered, focusKey);
        canvas.end(graphics);

        if (avatar != null) {
            int floor = canvas.screenY(AVATAR_FLOOR);
            avatar.render(graphics, canvas.screenX(AVATAR_CX), floor, Math.round(AVATAR_H * canvas.scale()),
                    canvas.screenX(720), width, openedAt, now, mouseX, mouseY, Kit.reduced(),
                    JilatonClient.clientConfig().animationIntensity());
        }
    }

    private boolean active(String key, String hovered, String focusKey) {
        return key.equals(hovered) || key.equals(focusKey);
    }

    private void topBar(GuiGraphics g, long now, String hovered, String focusKey) {
        double in = Kit.enter(openedAt, 0.0);
        float y = 28 - (float) (6 * (1 - in));
        Ui.mark(g, HERO_X, y + 9, 20, Kit.a(Kit.LABEL, in));
        Kit.text(g, "Jilaton", HERO_X + 30, y + 12, Kit.HEADLINE, JilatonType.Face.SEMIBOLD, Kit.a(Kit.LABEL, in));
        Action account = find("account");
        String clock = LocalTime.now().format(CLOCK);
        Ui.textRight(g, clock, account.x() - 18, y + 13, Kit.CALLOUT, JilatonType.Face.MEDIUM, Kit.a(Kit.LABEL_2, in));
        double hv = Kit.spring("home:account", active("account", hovered, focusKey));
        Kit.glass(g, account.x(), y, account.w(), 38, 19, hv);
        Ui.face(g, account.x() + 7, y + 7, 24);
        Kit.text(g, minecraft.getUser().getName(), account.x() + 40, y + 13, Kit.CALLOUT, JilatonType.Face.MEDIUM, Kit.a(Kit.LABEL, in));
    }

    private void hero(GuiGraphics g, long now, String hovered, String focusKey) {
        double a = Kit.enter(openedAt, 0.08), b = Kit.enter(openedAt, 0.16), c = Kit.enter(openedAt, 0.24);
        Kit.text(g, greeting(), HERO_X, 186 + rise(a), Kit.HEADLINE, JilatonType.Face.MEDIUM, Kit.a(Kit.LABEL_2, a));
        Kit.text(g, "Jilaton", HERO_X - 3, 212 + rise(b), 84, JilatonType.Face.BOLD, Kit.a(Kit.LABEL, b));
        Kit.text(g, "Everything you need, nothing you don't.", HERO_X, 312 + rise(c), 17, JilatonType.Face.REGULAR,
                Kit.a(Kit.LABEL_2, c));

        double d = Kit.enter(openedAt, 0.32);
        Action play = find("play"), multi = find("multi");
        String label = lastWorld == null ? (worldCount == 0 ? "Create a world" : "Play") : "Continue  ·  " + lastWorld.getLevelName();
        label = Ui.fit(label, play.w() - 40, Kit.HEADLINE, JilatonType.Face.SEMIBOLD);
        float lift = rise(d);
        Kit.button(g, "home:play", label, play.x(), play.y() + lift, play.w(), play.h(), 0,
                active("play", hovered, focusKey), "play".equals(pressedKey));
        Kit.button(g, "home:multi", "Multiplayer", multi.x(), multi.y() + lift, multi.w(), multi.h(), 1,
                active("multi", hovered, focusKey), "multi".equals(pressedKey));
    }

    private void tiles(GuiGraphics g, long now, String hovered, String focusKey) {
        String[] icons = {"folder", "globe", "grid", "gear"};
        String[] titles = {"Worlds", "Servers", "Client", "Settings"};
        String worldsLine = worldCount < 0 ? "Loading…" : worldCount == 0 ? "No worlds yet" : worldCount + (worldCount == 1 ? " world" : " worlds");
        int on = 0;
        for (var m : JilatonClient.moduleManager().all()) if (m.enabled()) on++;
        String[] lines = {worldsLine, "Multiplayer and LAN", on + " modules on", "Video, audio, controls"};
        for (int i = 0; i < 4; i++) {
            Action t = find(new String[]{"worlds", "servers", "client", "settings"}[i]);
            double in = Kit.enter(openedAt, 0.4 + i * 0.05);
            double hv = Kit.spring("home:tile:" + i, active(t.key(), hovered, focusKey));
            double pr = Kit.spring("home:tilep:" + i, t.key().equals(pressedKey));
            float y = t.y() + rise(in) - (float) (3 * hv) + (float) (1.5 * pr);
            if (in < 0.02) continue;
            Kit.glass(g, t.x(), y, t.w(), t.h(), Kit.R_TILE, hv * (1 - pr));
            Ui.round(g, t.x() + 18, y + 18, 36, 36, 10, Kit.a(Kit.mix(Kit.SURFACE_3, Kit.accent(), 0.12 * hv), in));
            Kit.icon(g, icons[i], t.x() + 36, y + 36, 20, Kit.a(Kit.mix(Kit.LABEL_2, Kit.LABEL, hv), in));
            Kit.text(g, titles[i], t.x() + 18, y + 76, Kit.HEADLINE, JilatonType.Face.SEMIBOLD, Kit.a(Kit.LABEL, in));
            Kit.text(g, Ui.fit(lines[i], t.w() - 36, Kit.FOOTNOTE, JilatonType.Face.REGULAR), t.x() + 18, y + 98,
                    Kit.FOOTNOTE, JilatonType.Face.REGULAR, Kit.a(Kit.LABEL_2, in));
            Kit.icon(g, "chevron", t.x() + t.w() - 22, y + 24, 14, Kit.a(Kit.LABEL_3, hv * in));
        }
    }

    private void bottom(GuiGraphics g, long now, String hovered, String focusKey) {
        double in = Kit.enter(openedAt, 0.6);
        Action music = find("music");
        MusicCenter center = MusicCenter.get();
        NowPlaying np = center.nowPlaying();
        boolean off = center.source() == MusicCenter.Source.OFF;
        String title = off ? "Music" : np.hasTrack() ? np.title() : "Nothing playing";
        String sub = off ? "Connect Spotify or play local files" : np.hasTrack() ? np.artist() : "Open the music center";
        double hv = Kit.spring("home:music", active("music", hovered, focusKey));
        Kit.glass(g, music.x(), music.y(), music.w(), music.h(), 22, hv);
        Ui.circle(g, music.x() + 22, music.y() + 22, 14, Kit.a(Kit.SURFACE_3, in));
        Kit.icon(g, "music", music.x() + 22, music.y() + 22, 14, Kit.a(np.hasTrack() && np.playing() ? Kit.LABEL : Kit.LABEL_2, in));
        Kit.text(g, Ui.fit(title, music.w() - 64, Kit.CALLOUT, JilatonType.Face.SEMIBOLD), music.x() + 46, music.y() + 8,
                Kit.CALLOUT, JilatonType.Face.SEMIBOLD, Kit.a(Kit.LABEL, in));
        Kit.text(g, Ui.fit(sub, music.w() - 64, Kit.CAPTION, JilatonType.Face.REGULAR), music.x() + 46, music.y() + 25,
                Kit.CAPTION, JilatonType.Face.REGULAR, Kit.a(Kit.LABEL_2, in));

        Action quit = find("quit");
        double qh = Kit.spring("home:quit", active("quit", hovered, focusKey));
        if (qh > 0.01) Ui.round(g, quit.x(), quit.y(), quit.w(), quit.h(), 20, Kit.a(Kit.SURFACE_3, 0.7 * qh));
        Kit.icon(g, "power", quit.x() + 22, quit.y() + 20, 14, Kit.mix(Kit.LABEL_2, Kit.RED, qh));
        Kit.text(g, "Quit", quit.x() + 38, quit.y() + 13, Kit.CALLOUT, JilatonType.Face.MEDIUM, Kit.mix(Kit.LABEL_2, Kit.LABEL, qh));
        Ui.textRight(g, "Jilaton " + version + "  ·  Minecraft 1.21.11", 1184, 694, Kit.CAPTION, JilatonType.Face.REGULAR,
                Kit.a(Kit.LABEL_3, in));
    }

    private void avatarChips(GuiGraphics g, long now, String hovered, String focusKey) {
        double in = Kit.enter(openedAt, 0.5);
        Kit.button(g, "home:skin", "Skin", find("skin").x(), find("skin").y() + rise(in), 124, 36, 1,
                active("skin", hovered, focusKey), "skin".equals(pressedKey));
        Kit.button(g, "home:character", "Character", find("character").x(), find("character").y() + rise(in), 124, 36, 1,
                active("character", hovered, focusKey), "character".equals(pressedKey));
    }

    private float musicWidth() {
        NowPlaying np = MusicCenter.get().nowPlaying();
        String title = np.hasTrack() ? np.title() : "Connect Spotify or play local files";
        return Math.min(340, Math.max(220, 72 + Ui.width(title, Kit.CALLOUT, JilatonType.Face.SEMIBOLD)));
    }

    private static float rise(double in) {
        return (float) (12 * (1 - in));
    }

    private static String greeting() {
        int h = LocalTime.now().getHour();
        return h < 5 ? "Late night" : h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
    }

    private Action find(String key) {
        for (Action a : actions) if (a.key().equals(key)) return a;
        throw new IllegalStateException(key);
    }

    private void resolveWorlds() {
        if (worldCount >= 0 || worlds == null || !worlds.isDone()) return;
        List<LevelSummary> list = worlds.getNow(List.of());
        worldCount = list.size();
        lastWorld = list.stream().filter(s -> !s.isLocked() && !s.isDisabled())
                .max(Comparator.comparingLong(LevelSummary::getLastPlayed)).orElse(null);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = canvas.x(event.x()), my = canvas.y(event.y());
            for (Action a : actions) {
                if (a.hit(mx, my)) {
                    pressedKey = a.key();
                    return true;
                }
            }
            if (avatar != null && avatar.mouseClicked(event.x(), event.y())) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (avatar != null) avatar.mouseReleased();
        if (pressedKey != null) {
            String key = pressedKey;
            pressedKey = null;
            double mx = canvas.x(event.x()), my = canvas.y(event.y());
            for (Action a : actions) {
                if (a.key().equals(key) && a.hit(mx, my)) {
                    a.run().run();
                    return true;
                }
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (avatar != null && avatar.mouseDragged(dragX)) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (actions.isEmpty()) return super.keyPressed(event);
        if (key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_DOWN) {
            focused = (focused + 1) % actions.size();
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_UP) {
            focused = focused <= 0 ? actions.size() - 1 : focused - 1;
            return true;
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) && focused >= 0) {
            actions.get(focused).run().run();
            return true;
        }
        return super.keyPressed(event);
    }
}
