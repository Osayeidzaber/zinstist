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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Home, editorial direction (after zaber.tech): a live GPU shader background with parallax hairline rings, a
 * "volume" header with a pill navigation whose white capsule slides to the hovered item and a working FULL EFFECTS
 * switch, a kinetic wordmark revealed letter by letter, a hairline rule with a travelling light sweep, four
 * numbered columns (I.–IV.) with magnetic hover and growing indicator lines, and a slow kinetic marquee along the
 * bottom. Every motion goes through {@link Kit#spring} / {@link Kit#enter}, so transition speed and reduced motion
 * apply everywhere.
 */
public final class JilatonMainMenuScreen extends Screen {
    private static final float M = 64;
    private static final String[] NAV = {"Home", "Worlds", "Servers", "Client", "Settings"};
    private static final String[] COL_NUM = {"I.", "II.", "III.", "IV."};
    private static final String[] COL_TITLE = {"Singleplayer", "Multiplayer", "Client", "Settings"};

    private record Hit(String key, float x, float y, float w, float h, Runnable run) {
        boolean inside(double mx, double my) {
            return Ui.inside(mx, my, x, y, w, h);
        }
    }

    private final Canvas canvas = new Canvas();
    private final List<Hit> hits = new ArrayList<>();
    private long openedAt;
    private String pressed;
    private int focused = -1;
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

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Backdrops.drawScreen(graphics, width, height, partialTick);
    }

    // ---------------------------------------------------------------- actions

    private void play() {
        if (lastWorld != null) minecraft.createWorldOpenFlows().openWorld(lastWorld.getLevelId(), () -> minecraft.setScreen(this));
        else minecraft.setScreen(new JilatonWorldsScreen(this));
    }

    private void worldsScreen() {
        minecraft.setScreen(new JilatonWorldsScreen(this));
    }

    private void multiplayer() {
        minecraft.setScreen(minecraft.options.skipMultiplayerWarning
                ? new JilatonServersScreen(this)
                : new net.minecraft.client.gui.screens.multiplayer.SafetyScreen(this));
    }

    private void client() {
        JilatonClient.openClickGui(this);
    }

    private void settings() {
        minecraft.setScreen(new OptionsScreen(this, minecraft.options));
    }

    private Runnable navAction(int i) {
        return switch (i) {
            case 1 -> this::worldsScreen;
            case 2 -> this::multiplayer;
            case 3 -> this::client;
            case 4 -> this::settings;
            default -> () -> { };
        };
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (openedAt == 0L || minecraft.getOverlay() != null) openedAt = System.nanoTime();
        resolveWorlds();
        double mx = canvas.x(mouseX), my = canvas.y(mouseY);
        // Parallax offsets: the cursor's distance from centre, eased.
        double px = Kit.spring("home:px", (mx - 640) / 640.0), py = Kit.spring("home:py", (my - 360) / 360.0);
        String focusKey = focused >= 0 && focused < hits.size() ? hits.get(focused).key() : null;
        List<Hit> previous = new ArrayList<>(hits);
        hits.clear();

        canvas.begin(g);
        rings(g, px, py);
        header(g, mx, my, previous, focusKey);
        hero(g, px, py);
        columns(g, mx, my, previous, focusKey);
        footer(g, mx, my, previous, focusKey);
        marquee(g);
        canvas.end(g);
    }

    private boolean over(String key, double mx, double my, List<Hit> previous, String focusKey) {
        if (key.equals(focusKey)) return true;
        for (Hit h : previous) if (h.key().equals(key)) return h.inside(mx, my);
        return false;
    }

    /** Concentric hairline rings on the right, drifting against the cursor; a tick orbits the inner one. */
    private void rings(GuiGraphics g, double px, double py) {
        double in = Kit.enter(openedAt, 0.1);
        float cx = (float) (930 - px * 22), cy = (float) (330 - py * 14);
        float[] radii = {150, 250, 370, 520};
        for (int i = 0; i < radii.length; i++) {
            Ui.ring(g, cx, cy, radii[i] * (float) (0.9 + 0.1 * in), 1, Kit.a(0xFFFFFFFF, (0.09 - i * 0.015) * in));
        }
        double t = Kit.reduced() ? 0.6 : System.nanoTime() / 1e9 * 0.25;
        for (int i = 0; i < 3; i++) {
            double a = t + i * 2.1;
            float r = radii[i];
            Ui.circle(g, cx + (float) (Math.cos(a) * r), cy + (float) (Math.sin(a) * r), 2.5F - i * 0.5F, Kit.a(Kit.LABEL, 0.7 * in));
        }
    }

    private void header(GuiGraphics g, double mx, double my, List<Hit> previous, String focusKey) {
        double in = Kit.enter(openedAt, 0);
        mono(g, "JL — VOL. 01", M, 34, Kit.a(Kit.LABEL, in));

        // Pill navigation; the white capsule slides to the hovered item, resting on Home.
        float navW = 0;
        float[] widths = new float[NAV.length];
        for (int i = 0; i < NAV.length; i++) {
            widths[i] = Ui.spacedWidth(NAV[i].toUpperCase(Locale.ROOT), Kit.CAPTION, 1.6F, JilatonType.Face.MONO) + 32;
            navW += widths[i];
        }
        float nx = (Canvas.W - navW - 8) / 2, ny = 22;
        Ui.round(g, nx, ny, navW + 8, 38, 19, Kit.a(0xFF0C0C0C, 0.85 * in));
        dev.jilaton.client.gui.design.Paint.outline(g, nx, ny, navW + 8, 38, 19, 0, Kit.a(0xFFFFFFFF, 0.12 * in));
        int target = 0;
        float cx = nx + 4;
        float[] xs = new float[NAV.length];
        for (int i = 0; i < NAV.length; i++) {
            xs[i] = cx;
            hits.add(new Hit("nav" + i, cx, ny + 4, widths[i], 30, navAction(i)));
            if (over("nav" + i, mx, my, previous, focusKey)) target = i;
            cx += widths[i];
        }
        double sx = Kit.spring("home:navx", xs[target]), sw = Kit.spring("home:navw", widths[target]);
        Ui.round(g, (float) sx, ny + 4, (float) sw, 30, 15, Kit.a(Kit.LABEL, in));
        for (int i = 0; i < NAV.length; i++) {
            double on = 1 - Math.min(1, Math.abs(sx - xs[i]) / Math.max(1, widths[i]));
            int color = Kit.mix(Kit.LABEL_2, 0xFF0A0A0A, on);
            Ui.spaced(g, NAV[i].toUpperCase(Locale.ROOT), xs[i] + 16, ny + 14, Kit.CAPTION, 1.6F, JilatonType.Face.MONO, Kit.a(color, in));
        }

        // FULL EFFECTS switch: the dot is lit while high-quality effects run.
        boolean full = JilatonClient.clientConfig().fullEffects();
        String label = "FULL EFFECTS";
        float fw = Ui.spacedWidth(label, Kit.CAPTION, 1.6F, JilatonType.Face.MONO_MEDIUM) + 46;
        float fx = Canvas.W - M - fw;
        hits.add(new Hit("effects", fx, ny, fw, 38, () -> {
            JilatonClient.clientConfig().setFullEffects(!JilatonClient.clientConfig().fullEffects());
            JilatonClient.clientConfig().save(JilatonClient.moduleManager());
        }));
        double fh = Kit.spring("home:fx", over("effects", mx, my, previous, focusKey));
        Ui.round(g, fx, ny, fw, 38, 19, Kit.a(Kit.mix(0xFF0C0C0C, 0xFF1C1C1C, fh), 0.9 * in));
        dev.jilaton.client.gui.design.Paint.outline(g, fx, ny, fw, 38, 19, 0, Kit.a(0xFFFFFFFF, (0.18 + 0.2 * fh) * in));
        double lit = Kit.spring("home:fxlit", full);
        Ui.circle(g, fx + 20, ny + 19, 4, Kit.a(Kit.mix(Kit.LABEL_4, Kit.LABEL, lit), in));
        if (lit > 0.01) Ui.glow(g, fx + 20, ny + 19, 12, 0xFFFFFF, 0.25 * lit * in);
        Ui.spaced(g, label, fx + 32, ny + 14, Kit.CAPTION, 1.6F, JilatonType.Face.MONO_MEDIUM, Kit.a(Kit.LABEL, in));

        // Section meta row.
        double meta = Kit.enter(openedAt, 0.12);
        mono(g, "00 — HOME", M, 112, Kit.a(Kit.LABEL_3, meta));
        String right = "MINECRAFT 1.21.11 · JILATON " + version.toUpperCase(Locale.ROOT);
        mono(g, right, Canvas.W - M - Ui.spacedWidth(right, Kit.CAPTION, 1.6F, JilatonType.Face.MONO), 112, Kit.a(Kit.LABEL_3, meta));
    }

    /** Kinetic wordmark: each letter rises out of a mask in turn; the subtitle types in after. */
    private void hero(GuiGraphics g, double px, double py) {
        String word = "Jilaton";
        float size = 168, x = M - 6 + (float) (px * 6), base = 168 + (float) (py * 4);
        float cx = x;
        g.enableScissor(0, Math.round(base - 4), Math.round(Canvas.W), Math.round(base + size * 0.98F));
        for (int i = 0; i < word.length(); i++) {
            String ch = word.substring(i, i + 1);
            double in = Kit.enter(openedAt, 0.15 + i * 0.05);
            float rise = (float) ((1 - in) * size * 0.9);
            Kit.text(g, ch, cx, base + rise, size, JilatonType.Face.DISPLAY_HEAVY, Kit.LABEL);
            cx += Kit.width(ch, size, JilatonType.Face.DISPLAY_HEAVY) - 2;
        }
        g.disableScissor();
        double sub = Kit.enter(openedAt, 0.55);
        String line = "client for people who notice the details.";
        int shown = (int) Math.round(line.length() * sub);
        float lx = cx + 24;
        Kit.text(g, "Minecraft client", lx, base + 44, 46, JilatonType.Face.SERIF_ITALIC, Kit.a(Kit.accentSoft(), sub));
        Kit.text(g, line.substring(0, shown), lx, base + 102, Kit.HEADLINE, JilatonType.Face.REGULAR, Kit.a(Kit.LABEL_2, sub));
        if (shown < line.length() && !Kit.reduced()) {
            float caret = lx + Kit.width(line.substring(0, shown), Kit.HEADLINE, JilatonType.Face.REGULAR) + 2;
            Ui.rect(g, caret, base + 101, 2, 16, Kit.LABEL);
        }
    }

    /** Four editorial columns under a rule with a travelling light sweep. */
    private void columns(GuiGraphics g, double mx, double my, List<Hit> previous, String focusKey) {
        float ruleY = 410;
        double rule = Kit.enter(openedAt, 0.4);
        float rw = (Canvas.W - 2 * M) * (float) rule;
        Ui.rect(g, M, ruleY, rw, 1, Kit.a(0xFFFFFFFF, 0.14));
        if (!Kit.reduced() && JilatonClient.clientConfig().fullEffects()) {
            double sweep = (System.nanoTime() / 1e9 * 0.18) % 1.4 - 0.2;
            float sx = M + (float) (sweep * (Canvas.W - 2 * M));
            for (int i = 0; i < 24; i++) {
                float k = 1 - Math.abs(i - 12) / 12F;
                Ui.rect(g, sx + i * 6, ruleY, 6, 1, Kit.a(0xFFFFFFFF, 0.55 * k * k * rule));
            }
        }
        String worldsLine = worldCount < 0 ? "Reading your saves…" : lastWorld != null
                ? "Continue " + lastWorld.getLevelName() + ", or open one of " + worldCount + "." : "Create your first world.";
        int on = 0;
        for (var m : JilatonClient.moduleManager().all()) if (m.enabled()) on++;
        String[] desc = {worldsLine, "Saved servers, direct connect and LAN games.",
                on + " modules running. Visuals, combat, music, cosmetics.", "Video, audio, controls and language."};
        Runnable[] run = {this::worldsScreen, this::multiplayer, this::client, this::settings};
        float colW = (Canvas.W - 2 * M - 3 * 28) / 4;
        for (int i = 0; i < 4; i++) {
            float x = M + i * (colW + 28), y = ruleY + 26;
            String key = "col" + i;
            hits.add(new Hit(key, x, y - 10, colW, 150, run[i]));
            boolean hot = over(key, mx, my, previous, focusKey);
            double hv = Kit.spring("home:col" + i, hot);
            double pr = Kit.spring("home:colp" + i, key.equals(pressed));
            double in = Kit.enter(openedAt, 0.48 + i * 0.07);
            // Magnetic pull toward the cursor while hovered.
            float pull = hot ? (float) Math.max(-6, Math.min(6, (mx - (x + colW / 2)) * 0.03)) : 0;
            double mag = Kit.spring("home:colm" + i, pull);
            float ox = (float) (mag * hv), oy = (float) ((1 - in) * 24 + pr * 2);
            Ui.rect(g, x, y - 26, (float) (colW * hv), 2, Kit.a(Kit.accentStrong(), hv));
            mono(g, COL_NUM[i], x + ox, y + oy, Kit.a(Kit.mix(Kit.LABEL_3, Kit.LABEL, hv), in));
            Kit.text(g, COL_TITLE[i], x + ox + (float) (8 * hv), y + 28 + oy, 26, JilatonType.Face.DISPLAY_HEAVY, Kit.a(Kit.LABEL, in));
            float tw = Kit.width(COL_TITLE[i], 26, JilatonType.Face.DISPLAY_HEAVY);
            if (hv > 0.02) {
                Kit.text(g, "↗", x + ox + tw + 18 + (float) (4 * hv), y + 30 + oy - (float) (4 * hv), 20,
                        JilatonType.Face.MEDIUM, Kit.a(Kit.LABEL, hv * in));
            }
            wrap(g, desc[i], x + ox, y + 68 + oy, colW - 8, Kit.a(Kit.mix(Kit.LABEL_3, Kit.LABEL_2, hv), in));
        }
    }

    private void footer(GuiGraphics g, double mx, double my, List<Hit> previous, String focusKey) {
        double in = Kit.enter(openedAt, 0.7);
        float y = 612;
        String label = lastWorld != null ? "Continue — " + Ui.fit(lastWorld.getLevelName(), 160, Kit.HEADLINE, JilatonType.Face.SEMIBOLD)
                : worldCount == 0 ? "Create a world" : "Play";
        float pw = Kit.width(label, Kit.HEADLINE, JilatonType.Face.SEMIBOLD) + 66;
        hits.add(new Hit("play", M, y, pw, 48, this::play));
        double ph = Kit.spring("home:play", over("play", mx, my, previous, focusKey));
        double pp = Kit.spring("home:playp", "play".equals(pressed));
        float sink = (float) (pp * 1.5);
        Ui.round(g, M, y + sink, pw, 48, 24, Kit.a(Kit.mix(Kit.LABEL, 0xFFFFFFFF, ph), in));
        Kit.text(g, label, M + 26, y + 17 + sink, Kit.HEADLINE, JilatonType.Face.SEMIBOLD, Kit.a(0xFF0A0A0A, in));
        Kit.text(g, "↘", M + pw - 32 + (float) (3 * ph), y + 15 + sink + (float) (2 * ph), 17, JilatonType.Face.MEDIUM, Kit.a(0xFF0A0A0A, in));

        float qx = M + pw + 12, qw = 104;
        hits.add(new Hit("quit", qx, y, qw, 48, minecraft::stop));
        double qh = Kit.spring("home:quit", over("quit", mx, my, previous, focusKey));
        Ui.round(g, qx, y, qw, 48, 24, Kit.a(Kit.mix(0xFF000000, 0xFF1A1A1A, qh), 0.6 * in));
        dev.jilaton.client.gui.design.Paint.outline(g, qx, y, qw, 48, 24, 0, Kit.a(0xFFFFFFFF, (0.22 + 0.25 * qh) * in));
        Ui.textCentre(g, "Quit", qx + qw / 2, y + 17, Kit.HEADLINE, JilatonType.Face.SEMIBOLD, Kit.a(Kit.LABEL, in));

        // Right side: music line and account.
        MusicCenter center = MusicCenter.get();
        NowPlaying np = center.nowPlaying();
        String music = center.source() == MusicCenter.Source.OFF ? "MUSIC — OFF" : np.hasTrack()
                ? "NOW — " + np.title().toUpperCase(Locale.ROOT) : "MUSIC — IDLE";
        music = Ui.fit(music, 300, Kit.CAPTION, JilatonType.Face.MONO);
        float mw = Ui.spacedWidth(music, Kit.CAPTION, 1.6F, JilatonType.Face.MONO);
        String name = minecraft.getUser().getName();
        float aw = 44 + Kit.width(name, Kit.CALLOUT, JilatonType.Face.MEDIUM);
        float ax = Canvas.W - M - aw;
        hits.add(new Hit("account", ax, y + 5, aw, 38, () -> minecraft.setScreen(new AccountsScreen(this))));
        double ah = Kit.spring("home:acc", over("account", mx, my, previous, focusKey));
        Ui.round(g, ax, y + 5, aw, 38, 19, Kit.a(Kit.mix(0xFF0C0C0C, 0xFF1C1C1C, ah), 0.9 * in));
        dev.jilaton.client.gui.design.Paint.outline(g, ax, y + 5, aw, 38, 19, 0, Kit.a(0xFFFFFFFF, (0.14 + 0.2 * ah) * in));
        Ui.face(g, ax + 7, y + 12, 24);
        Kit.text(g, name, ax + 38, y + 18, Kit.CALLOUT, JilatonType.Face.MEDIUM, Kit.a(Kit.LABEL, in));
        float mxp = ax - 24 - mw;
        hits.add(new Hit("music", mxp - 8, y + 5, mw + 16, 38, () -> minecraft.setScreen(new MusicCenterScreen(this))));
        double mh = Kit.spring("home:music", over("music", mx, my, previous, focusKey));
        mono(g, music, mxp, y + 19, Kit.a(Kit.mix(Kit.LABEL_3, Kit.LABEL, mh), in));
        Ui.rect(g, mxp, y + 34, (float) (mw * mh), 1, Kit.a(Kit.LABEL, mh * in));
    }

    /** A slow mono marquee along the bottom edge. */
    private void marquee(GuiGraphics g) {
        double in = Kit.enter(openedAt, 0.8);
        String unit = "SINGLEPLAYER  /  MULTIPLAYER  /  MODULES  /  VISUALS  /  MUSIC  /  COSMETICS  /  ";
        float uw = Ui.spacedWidth(unit, Kit.CAPTION, 1.6F, JilatonType.Face.MONO);
        double t = Kit.reduced() ? 0 : System.nanoTime() / 1e9 * 26;
        float off = (float) (t % uw);
        float y = 690;
        Ui.rect(g, 0, y - 12, Canvas.W, 1, Kit.a(0xFFFFFFFF, 0.08 * in));
        for (float x = -off; x < Canvas.W; x += uw) mono(g, unit, x, y, Kit.a(Kit.LABEL_4, in));
    }

    // ---------------------------------------------------------------- helpers

    private static void mono(GuiGraphics g, String s, float x, float y, int color) {
        Ui.spaced(g, s, x, y, Kit.CAPTION, 1.6F, JilatonType.Face.MONO, color);
    }

    private static void wrap(GuiGraphics g, String text, float x, float y, float w, int color) {
        StringBuilder line = new StringBuilder();
        float ly = y;
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (Kit.width(next, Kit.CALLOUT, JilatonType.Face.REGULAR) > w && !line.isEmpty()) {
                Kit.text(g, line.toString(), x, ly, Kit.CALLOUT, JilatonType.Face.REGULAR, color);
                line = new StringBuilder(word);
                ly += 19;
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) Kit.text(g, line.toString(), x, ly, Kit.CALLOUT, JilatonType.Face.REGULAR, color);
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
            if (key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_DOWN) {
                focused = (focused + 1) % hits.size();
                return true;
            }
            if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_UP) {
                focused = focused <= 0 ? hits.size() - 1 : focused - 1;
                return true;
            }
            if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE)
                    && focused >= 0 && focused < hits.size()) {
                hits.get(focused).run().run();
                return true;
            }
        }
        return super.keyPressed(event);
    }
}
