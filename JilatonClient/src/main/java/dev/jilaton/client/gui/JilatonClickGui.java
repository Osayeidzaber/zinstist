package dev.jilaton.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.gui.design.Ui;
import dev.jilaton.client.gui.shell.CardMetric;
import dev.jilaton.client.gui.shell.PreviewSpecs;
import dev.jilaton.client.gui.shell.VisualPreview;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.gui.shell.ModuleOrder;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.Setting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The ClickGUI, built to the "ClickGUI · Combat", "ClickGUI · Visuals + Knife settings" and "ClickGUI · Theme" boards
 * (development/GUI_REDESIGN.md §2, §3, §10) on the 1280x720 board canvas: a 1152x640 panel with a 212px text sidebar,
 * a greeting header with search, a title row with the live active count, 2-column module cards (3-column preview
 * cards on Visuals), and a 300px settings drawer on the right that every module opens into, so the list never
 * shifts under the cursor. Settings, Configs and Theme are pages of the same panel. Where you were is remembered.
 */
public final class JilatonClickGui extends Screen {
    /** Inter instead of the vanilla pixel font for everything this screen draws ({@link JilatonType}). */
    private final Font font = JilatonType.font();

    private static final float PX = 64, PY = 40, PW = 1152, PH = 640, SIDE = 212, DRAWER = 300;
    private static final float MAIN_X = PX + SIDE + 24, HEADER_Y = PY + 18, TITLE_Y = PY + 84, LIST_Y = PY + 130;
    private static final float FOOTER_Y = PY + PH - 40, CARD_H = 62, VISUAL_H = 128, GAP = 10;
    private static final String[] CATEGORIES = {"All", "Combat", "Movement", "Player", "Visuals", "Client"};
    private static final String[] SUBTITLES = {"Every module, its key and its state", "Attack, targeting and defence",
            "How you move through the world", "Things that act on your own player", "Each card previews what the module draws",
            "The client itself: HUD, music, interface"};
    private static final String[] GENERAL = {"Settings", "Configs", "Theme"};
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private enum Page { MODULES, SETTINGS, CONFIGS, THEME }

    private final ModuleManager modules;
    private final ClientConfig config;
    private final Screen parent;
    private final Canvas canvas = new Canvas();
    private final VisualPreview preview = new VisualPreview();
    private final List<ClientModule> allModules;
    private Page page = Page.MODULES;
    private String category = "All";
    private String drawerId;
    private String bindingId;
    private boolean enabledOnly;
    private boolean resumed;
    private long openedAt;
    private double scroll, targetScroll, drawerScroll;
    private EditBox search;
    private EditBox profileName;
    private String selectedProfile;
    private List<String> profiles = List.of();
    /** One-line result of the last config action (export, import, reset), shown under the profile field. */
    private String configStatus = "";
    private String restoredQuery = "";
    /** Active slider drag: the setting and its track in board coordinates. */
    private NumberSetting dragging;
    private float dragX, dragW;
    private boolean dragMotion;
    private boolean dragBackdrop;
    private boolean dragScale;
    private boolean dragSpeed;
    /** Card reorder: the card held by its grip, the press point and the slot it would drop into. */
    private String dragCard;
    private double dragCardX, dragCardY;
    private boolean cardDragging;
    private int dropIndex = -1;

    public JilatonClickGui(ModuleManager modules, ClientConfig config, Screen parent) {
        super(Component.literal("Jilaton Client"));
        this.modules = modules;
        this.config = config;
        this.parent = parent;
        this.allModules = List.copyOf(modules.all());
        try {
            page = Page.valueOf(config.guiPage());
        } catch (IllegalArgumentException ignored) {
            page = Page.MODULES;
        }
        for (String c : CATEGORIES) if (c.equals(config.guiCategory())) category = c;
        String expanded = config.guiExpandedModule();
        drawerId = !expanded.isEmpty() && modules.get(expanded) != null ? expanded : null;
        enabledOnly = config.guiEnabledOnly();
        restoredQuery = config.guiSearchQuery();
        scroll = targetScroll = config.categoryScroll(category);
        drawerScroll = drawerId == null ? 0 : config.guiDrawerScroll();
        selectedProfile = config.guiSelectedProfile().isEmpty() ? null : config.guiSelectedProfile();
        resumed = page != Page.MODULES || !category.equals("All") || drawerId != null || !restoredQuery.isEmpty();
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void init() {
        canvas.fit(width, height);
        String query = search == null ? restoredQuery : search.getValue();
        search = new EditBox(font, 0, 0, 100, 10, Component.literal("Search modules"));
        search.setBordered(false);
        search.setMaxLength(64);
        search.setTextColor(Ui.TEXT);
        search.setHint(Component.literal("Search modules").withColor(Ui.TEXT_3));
        search.setValue(query);
        search.setResponder(value -> {
            targetScroll = 0;
            if (!value.isEmpty() && page != Page.MODULES) page = Page.MODULES;
        });
        addRenderableWidget(search);
        String profile = profileName == null ? "" : profileName.getValue();
        profileName = new EditBox(font, 0, 0, 100, 10, Component.literal("Profile name"));
        profileName.setBordered(false);
        profileName.setMaxLength(32);
        profileName.setTextColor(Ui.TEXT);
        profileName.setHint(Component.literal("Profile name").withColor(Ui.TEXT_3));
        profileName.setValue(profile);
        addRenderableWidget(profileName);
        refreshProfiles();
        place();
    }

    /** Puts the two text fields where their boxes are drawn this frame (they render in screen pixels). */
    private void place() {
        float right = mainRight();
        float sx = right - 260 + 30, sy = HEADER_Y + 24;
        search.setX(canvas.screenX(sx));
        search.setY(canvas.screenY(sy) - 4);
        search.setWidth(Math.max(30, canvas.screenX(right - 56) - canvas.screenX(sx)));
        float px = MAIN_X + 14, py = LIST_Y + 20;
        profileName.setX(canvas.screenX(px));
        profileName.setY(canvas.screenY(py) - 4);
        profileName.setWidth(Math.max(30, canvas.screenX(MAIN_X + 380) - canvas.screenX(px)));
        profileName.visible = page == Page.CONFIGS;
    }

    private void remember() {
        config.setGuiPage(page.name());
        config.setGuiCategory(category);
        config.setGuiExpandedModule(drawerId == null ? "" : drawerId);
        config.setGuiDrawerScroll(drawerId == null ? 0 : drawerScroll);
        config.setGuiSelectedModule(drawerId == null ? "" : drawerId);
        config.setGuiEnabledOnly(enabledOnly);
        config.setGuiSearchQuery(search == null ? restoredQuery : search.getValue());
        config.setGuiSelectedProfile(selectedProfile == null ? "" : selectedProfile);
        config.setCategoryScroll(category, targetScroll);
    }

    @Override
    public void removed() {
        remember();
        config.save(modules);
        super.removed();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ---------------------------------------------------------------- data

    private ClientModule drawerModule() {
        return drawerId == null || page != Page.MODULES ? null : modules.get(drawerId);
    }

    private float mainRight() {
        return drawerModule() != null ? PX + PW - DRAWER - 20 : PX + PW - 24;
    }

    private boolean visualPage() {
        return category.equals("Visuals");
    }

    private boolean inCategory(ClientModule module, String name) {
        return name.equals("All") || name.equals(module.category().displayName());
    }

    private List<ClientModule> visible() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        List<ClientModule> out = new ArrayList<>();
        for (ClientModule module : allModules) {
            if (!inCategory(module, category) || (enabledOnly && !module.enabled())) continue;
            if (!query.isEmpty() && !module.name().toLowerCase(Locale.ROOT).contains(query)
                    && !module.description().toLowerCase(Locale.ROOT).contains(query)) continue;
            out.add(module);
        }
        List<String> order = visualPage() && !config.visualOrder().isEmpty() ? config.visualOrder() : config.moduleOrder();
        if (!order.isEmpty()) {
            List<String> ids = new ArrayList<>();
            for (ClientModule module : out) ids.add(module.id());
            List<ClientModule> sorted = new ArrayList<>();
            for (String id : ModuleOrder.apply(order, ids)) for (ClientModule m : out) if (m.id().equals(id)) sorted.add(m);
            out = sorted;
        }
        return out;
    }

    private int columns() {
        if (!visualPage()) return 2;
        return mainRight() - MAIN_X >= 3 * 170 + 2 * GAP ? 3 : 2;
    }

    private float cardHeight() {
        return visualPage() ? VISUAL_H : CARD_H;
    }

    private float listBottom() {
        return FOOTER_Y - 10;
    }

    private float contentHeight(int count) {
        int rows = (count + columns() - 1) / columns();
        return rows == 0 ? 0 : rows * (cardHeight() + GAP) - GAP;
    }

    private float maxScroll() {
        return Math.max(0, contentHeight(visible().size()) - (listBottom() - LIST_Y));
    }

    private String keyName(ClientModule module) {
        if (module.id().equals(bindingId)) return "Press a key";
        if (module.keyCode() < 0) return "Unbound";
        String name = InputConstants.Type.KEYSYM.getOrCreate(module.keyCode()).getDisplayName().getString();
        return name.length() > 10 ? name.substring(0, 9) + "…" : name;
    }

    private void refreshProfiles() {
        try {
            profiles = config.listProfiles();
        } catch (IOException exception) {
            profiles = List.of();
        }
        if (selectedProfile != null && !profiles.contains(selectedProfile)) selectedProfile = null;
    }

    // ---------------------------------------------------------------- render

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) graphics.fill(0, 0, width, height, Ui.BACKDROP);
        else {
            if (config.backdropBlur()) renderBlurredBackground(graphics);
            graphics.fill(0, 0, width, height, Ui.alpha(0xFF050505, config.backdropOpacity()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        if (openedAt == 0) openedAt = now;
        boolean reduced = config.reducedMotion();
        scroll = Ui.approach(scroll, targetScroll, 1 / 60.0, 18, reduced);
        targetScroll = Math.max(0, Math.min(maxScroll(), targetScroll));
        int accent = Ui.accent();
        double mx = canvas.x(mouseX), my = canvas.y(mouseY);
        place();

        canvas.begin(graphics);
        Ui.round(graphics, PX - 6, PY + 2, PW + 12, PH + 12, 18, Ui.alpha(0xFF000000, 0.35));
        Ui.card(graphics, PX, PY, PW, PH, 14, Ui.PANEL, Ui.STROKE);
        renderSidebar(graphics, mx, my, accent);
        renderHeader(graphics, mx, my, accent, now);
        switch (page) {
            case MODULES -> renderModules(graphics, mx, my, accent, now, reduced);
            case SETTINGS -> renderSettingsPage(graphics, mx, my, accent);
            case CONFIGS -> renderConfigs(graphics, mx, my, accent);
            case THEME -> renderTheme(graphics, mx, my, accent);
        }
        ClientModule drawer = drawerModule();
        if (drawer != null) renderDrawer(graphics, drawer, mx, my, accent);
        renderFooter(graphics);
        canvas.end(graphics);
        search.render(graphics, mouseX, mouseY, partialTick);
        if (profileName.visible) profileName.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSidebar(GuiGraphics g, double mx, double my, int accent) {
        Ui.round(g, PX + 1, PY + 1, SIDE - 1, PH - 2, 13, Ui.SIDEBAR);
        Ui.rect(g, PX + SIDE, PY + 1, 1, PH - 2, Ui.STROKE);
        float x = PX + 14;
        Ui.card(g, x + 6, PY + 20, 34, 34, 10, Ui.CARD, Ui.STROKE);
        Ui.mark(g, x + 14, PY + 28, 18, Ui.TEXT);
        Ui.spaced(g, "JILATON", x + 50, PY + 30, 14, 3.2F, JilatonType.Face.BOLD, Ui.TEXT);
        Ui.textRight(g, "0.1", PX + SIDE - 20, PY + 32, 10, JilatonType.Face.MONO, Ui.TEXT_3);

        Ui.spaced(g, "MODULES", x + 8, PY + 76, 10, 1, JilatonType.Face.MEDIUM, Ui.TEXT_3);
        float y = PY + 92;
        for (String c : CATEGORIES) {
            int on = 0, total = 0;
            for (ClientModule module : allModules) {
                if (!inCategory(module, c)) continue;
                total++;
                if (module.enabled()) on++;
            }
            boolean active = page == Page.MODULES && c.equals(category);
            sideRow(g, c, on + " of " + total, x, y, active, mx, my, accent);
            y += 44;
        }
        Ui.spaced(g, "GENERAL", x + 8, y + 12, 10, 1, JilatonType.Face.MEDIUM, Ui.TEXT_3);
        y += 30;
        for (int i = 0; i < GENERAL.length; i++) {
            boolean active = page.ordinal() == i + 1;
            sideRow(g, GENERAL[i], null, x, y, active, mx, my, accent);
            y += 44;
        }
        // Who is playing, where.
        float uy = PY + PH - 62;
        Ui.hline(g, x, uy, SIDE - 28, Ui.STROKE);
        Ui.face(g, x + 8, uy + 14, 28);
        Ui.text(g, Ui.fit(minecraft.getUser().getName(), 110, 12, JilatonType.Face.SEMIBOLD), x + 46, uy + 14, 12,
                JilatonType.Face.SEMIBOLD, Ui.TEXT);
        String where = minecraft.level == null ? "Main menu" : minecraft.hasSingleplayerServer() ? "Singleplayer" : "Multiplayer";
        Ui.text(g, where, x + 46, uy + 31, 11, Ui.TEXT_3);
        Ui.circle(g, PX + SIDE - 24, uy + 28, 3.5F, Ui.ONLINE);
    }

    private void sideRow(GuiGraphics g, String label, String count, float x, float y, boolean active, double mx, double my, int accent) {
        double t = Ui.ease("cg:side:" + label, Ui.inside(mx, my, x, y, SIDE - 28, 40));
        if (active) Ui.round(g, x, y, SIDE - 28, 40, 8, Ui.alpha(accent, 0.08));
        else if (t > 0.01) Ui.round(g, x, y, SIDE - 28, 40, 8, Ui.alpha(Ui.RAISED, t));
        Ui.text(g, label, x + 8, y + 13, 13, active ? JilatonType.Face.SEMIBOLD : JilatonType.Face.REGULAR,
                active ? accent : Ui.mix(Ui.TEXT_2, Ui.TEXT, t));
        if (count != null) Ui.textRight(g, count, x + SIDE - 36, y + 14, 11, JilatonType.Face.MONO, active ? accent : Ui.TEXT_3);
    }

    private void renderHeader(GuiGraphics g, double mx, double my, int accent, long now) {
        float right = mainRight();
        float y = HEADER_Y;
        float hx = MAIN_X;
        Ui.text(g, "Hello, ", hx, y + 17, 15, JilatonType.Face.MEDIUM, Ui.TEXT);
        float nx = hx + Ui.width("Hello, ", 15, JilatonType.Face.MEDIUM);
        String name = minecraft.getUser().getName();
        Ui.text(g, name, nx, y + 17, 15, JilatonType.Face.MEDIUM, accent);
        float after = nx + Ui.width(name, 15, JilatonType.Face.MEDIUM) + 16;
        double since = (now - openedAt) / 1e9;
        if (resumed && since < 4) {
            double fade = since < 3 ? 1 : 1 - (since - 3);
            String chip = "Resumed where you left off";
            float cw = Ui.width(chip, 11, JilatonType.Face.REGULAR) + 30;
            if (after + cw < right - 330) {
                Ui.card(g, after, y + 12, cw, 24, 12, Ui.alpha(Ui.CARD, fade), Ui.alpha(Ui.STROKE, fade));
                Ui.ring(g, after + 13, y + 24, 4, 1.2F, Ui.alpha(Ui.TEXT_2, fade));
                Ui.text(g, chip, after + 22, y + 18, 11, Ui.alpha(Ui.TEXT_2, fade));
            }
        }
        String clock = LocalTime.now().format(CLOCK);
        Ui.textRight(g, clock, right - 260 - 16, y + 18, 12, JilatonType.Face.MONO, Ui.TEXT_2);
        boolean focus = search.isFocused();
        Ui.card(g, right - 260, y + 7, 260, 34, 9, Ui.CARD, focus ? Ui.alpha(accent, 0.55) : Ui.STROKE);
        Ui.ring(g, right - 260 + 15, y + 23, 4.2F, 1.4F, Ui.TEXT_3);
        Ui.rect(g, right - 260 + 18, y + 26, 3, 1.4F, Ui.TEXT_3);
        Ui.textRight(g, "Ctrl F", right - 12, y + 19, 10, JilatonType.Face.MONO, Ui.TEXT_3);
    }

    private void renderTitle(GuiGraphics g, String title, String subtitle, String right, double mx, double my, int accent) {
        float r = mainRight();
        Ui.text(g, title, MAIN_X, TITLE_Y, 24, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        float tx = MAIN_X + Ui.width(title, 24, JilatonType.Face.SEMIBOLD) + 12;
        float rightW = right == null ? 0 : Ui.width(right, 12, JilatonType.Face.REGULAR) + 14;
        float filterW = page == Page.MODULES ? Ui.width("Enabled only", 12, JilatonType.Face.MEDIUM) + 34 : 0;
        float room = r - tx - rightW - filterW - 24;
        Ui.text(g, Ui.fit(subtitle, room, 12, JilatonType.Face.REGULAR), tx, TITLE_Y + 10, 12, Ui.TEXT_2);
        if (right != null) {
            Ui.textRight(g, right, r, TITLE_Y + 10, 12, JilatonType.Face.REGULAR, Ui.TEXT_2);
            Ui.circle(g, r - rightW + 5, TITLE_Y + 16, 3, accent);
        }
        if (filterW > 0) {
            float fx = r - rightW - 12 - filterW;
            boolean over = Ui.inside(mx, my, fx, TITLE_Y + 4, filterW, 24);
            Ui.chip(g, "", fx, TITLE_Y + 4, filterW, 24, enabledOnly, over, accent);
            Ui.round(g, fx + 9, TITLE_Y + 12, 8, 8, 2, enabledOnly ? accent : Ui.FAINT);
            Ui.text(g, "Enabled only", fx + 24, TITLE_Y + 10, 12, JilatonType.Face.MEDIUM, enabledOnly ? Ui.TEXT : Ui.TEXT_2);
        }
    }

    private void renderModules(GuiGraphics g, double mx, double my, int accent, long now, boolean reduced) {
        List<ClientModule> list = visible();
        int on = 0, total = 0;
        for (ClientModule module : allModules) {
            if (!inCategory(module, category)) continue;
            total++;
            if (module.enabled()) on++;
        }
        int index = 0;
        for (int i = 0; i < CATEGORIES.length; i++) if (CATEGORIES[i].equals(category)) index = i;
        String query = search.getValue().trim();
        renderTitle(g, query.isEmpty() ? category : "Search", query.isEmpty() ? SUBTITLES[index]
                : list.size() + (list.size() == 1 ? " match" : " matches") + " for “" + query + "”",
                on + " of " + total + " active", mx, my, accent);

        float right = mainRight();
        if (visualPage()) {
            // The cards print "<on>/<total> SW ON"; this line is the only place the unit is spelled out.
            Ui.textRight(g, "SW = switches on", right, TITLE_Y + 28, 10, JilatonType.Face.REGULAR, Ui.TEXT_3);
        }
        int cols = columns();
        float cw = (right - MAIN_X - GAP * (cols - 1)) / cols, ch = cardHeight();
        // Scissor rectangles go through the current pose, so they are given in board coordinates.
        g.enableScissor(Math.round(MAIN_X - 4), Math.round(LIST_Y - 4), Math.round(right + 4), Math.round(listBottom()));
        if (list.isEmpty()) {
            Ui.textCentre(g, enabledOnly ? "Nothing is on here yet." : "No modules match. Clear the search or pick another category.",
                    (MAIN_X + right) / 2, LIST_Y + 40, 13, JilatonType.Face.REGULAR, Ui.TEXT_2);
        }
        for (int i = 0; i < list.size(); i++) {
            float x = MAIN_X + (i % cols) * (cw + GAP);
            float y = (float) (LIST_Y + (i / cols) * (ch + GAP) - scroll);
            if (y + ch < LIST_Y - 4 || y > listBottom()) continue;
            ClientModule module = list.get(i);
            boolean over = my >= LIST_Y && my <= listBottom() && Ui.inside(mx, my, x, y, cw, ch);
            if (visualPage()) visualCard(g, module, x, y, cw, ch, over, accent, now, reduced);
            else moduleCard(g, module, x, y, cw, ch, over, accent);
        }
        if (cardDragging && dropIndex >= 0 && dropIndex < list.size()) {
            float ix = MAIN_X + (dropIndex % cols) * (cw + GAP);
            float iy = (float) (LIST_Y + (dropIndex / cols) * (ch + GAP) - scroll);
            Ui.round(g, ix - 3, iy + 2, 3, ch - 4, 1.5F, accent);
        }
        g.disableScissor();
        float max = maxScroll();
        if (max > 0) {
            float view = listBottom() - LIST_Y;
            float thumb = Math.max(24, view * view / (view + max));
            Ui.round(g, right + 8, (float) (LIST_Y + (view - thumb) * scroll / max), 3, thumb, 1.5F, Ui.FAINT);
        }
    }

    private void moduleCard(GuiGraphics g, ClientModule module, float x, float y, float w, float h, boolean over, int accent) {
        boolean open = module.id().equals(drawerId);
        double t = Ui.ease("cg:card:" + module.id(), over);
        Ui.round(g, x, y, w, h, 12, Ui.mix(Ui.CARD, Ui.RAISED, t * 0.7));
        if (open) Ui.glowRing(g, x, y, w, h, 12, accent, 1);
        else Ui.outline(g, x, y, w, h, 12, Ui.mix(Ui.STROKE, Ui.FAINT, t));
        if (module.id().equals(dragCard)) Ui.outline(g, x, y, w, h, 12, accent);
        drawGrip(g, module, x, y, h, accent);
        float textW = w - 32 - 40 - 12;
        Ui.text(g, Ui.fit(module.name(), textW, 13, JilatonType.Face.SEMIBOLD), x + 16, y + 15, 13, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        String key = keyName(module);
        String sub = key + " · " + module.description();
        Ui.text(g, Ui.fit(sub, textW, 11, JilatonType.Face.REGULAR), x + 16, y + 35, 11,
                module.id().equals(bindingId) ? accent : Ui.TEXT_2);
        Ui.toggle(g, x + w - 16 - Ui.TOGGLE_W, y + 20, Ui.ease("cg:tog:" + module.id(), module.enabled()), accent);
    }

    private void visualCard(GuiGraphics g, ClientModule module, float x, float y, float w, float h, boolean over, int accent,
                            long now, boolean reduced) {
        boolean open = module.id().equals(drawerId);
        double t = Ui.ease("cg:card:" + module.id(), over);
        Ui.round(g, x, y, w, h, 12, Ui.mix(Ui.CARD, Ui.RAISED, t * 0.7));
        if (open) Ui.glowRing(g, x, y, w, h, 12, accent, 1);
        else Ui.outline(g, x, y, w, h, 12, Ui.mix(Ui.STROKE, Ui.FAINT, t));
        if (module.id().equals(dragCard)) Ui.outline(g, x, y, w, h, 12, accent);
        drawGrip(g, module, x, y, h, accent);
        float textW = w - 28 - 32 - 10;
        Ui.text(g, Ui.fit(module.name(), textW, 13, JilatonType.Face.SEMIBOLD), x + 14, y + 13, 13, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        Ui.smallToggle(g, x + w - 14 - Ui.SMALL_TOGGLE_W, y + 12, Ui.ease("cg:tog:" + module.id(), module.enabled()), accent);
        String summary = module.settings().isEmpty() ? module.description()
                : CardMetric.label(module) + " · " + module.description();
        Ui.text(g, Ui.fit(summary, w - 28, 11, JilatonType.Face.REGULAR), x + 14, y + 32, 11, Ui.TEXT_2);
        int px = Math.round(x + 14), py = Math.round(y + 52), pw = Math.round(w - 28), ph = Math.round(h - 52 - 12);
        preview.render(g, font, px, py, pw, ph, 8, module.id(), PreviewSpecs.of(module, accent), module.enabled(), t, reduced, now);
    }

    /** Three dots in the card's left gutter: press here to drag the card, never to toggle or open it. */
    private void drawGrip(GuiGraphics g, ClientModule module, float x, float y, float h, int accent) {
        int color = module.id().equals(dragCard) ? accent : Ui.FAINT;
        float top = y + (h - 10) / 2;
        for (int row = 0; row < 3; row++) Ui.rect(g, x + 7, top + row * 4, 2, 2, color);
    }

    /** The grip's grab area: the left gutter, clear of the name and summary text. */
    static boolean gripHit(double mx, double my, float x, float y, float h) {
        return Ui.inside(mx, my, x + 2, y + 6, 11, h - 12);
    }

    // ---------------------------------------------------------------- drawer

    private record Row(Setting setting, float y, float h) { }

    private static boolean chipGrid(Setting setting) {
        return setting instanceof ChoiceSetting c && c.choices().size() >= 2 && c.choices().size() <= 8;
    }

    private List<Row> drawerRows(ClientModule module, float top) {
        List<Row> rows = new ArrayList<>();
        float y = top;
        for (Setting setting : module.settings()) {
            float h;
            if (chipGrid(setting)) h = 22 + ((((ChoiceSetting) setting).choices().size() + 1) / 2) * 40f + 6;
            else if (setting instanceof NumberSetting) h = 50;
            else h = 34;
            rows.add(new Row(setting, y, h));
            y += h;
        }
        return rows;
    }

    private float drawerTop() { return PY + 132; }

    private float drawerContentHeight(ClientModule module) {
        List<Row> rows = drawerRows(module, 0);
        float end = rows.isEmpty() ? 0 : rows.get(rows.size() - 1).y() + rows.get(rows.size() - 1).h();
        return end + 50;
    }

    private float drawerMax(ClientModule module) {
        return Math.max(0, drawerContentHeight(module) - (PY + PH - 16 - drawerTop()));
    }

    private void renderDrawer(GuiGraphics g, ClientModule module, double mx, double my, int accent) {
        float x0 = PX + PW - DRAWER, x = x0 + 18, r = PX + PW - 18;
        Ui.round(g, x0, PY + 1, DRAWER - 1, PH - 2, 13, Ui.DRAWER);
        Ui.rect(g, x0, PY + 1, 1, PH - 2, Ui.STROKE);
        Ui.text(g, Ui.fit(module.name(), DRAWER - 90, 16, JilatonType.Face.SEMIBOLD), x, PY + 24, 16, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        boolean closeOver = Ui.inside(mx, my, r - 28, PY + 18, 28, 28);
        Ui.card(g, r - 28, PY + 18, 28, 28, 7, Ui.alpha(Ui.RAISED, Ui.ease("cg:close", closeOver)), Ui.STROKE);
        Ui.textCentre(g, "×", r - 14, PY + 23, 14, JilatonType.Face.REGULAR, Ui.TEXT_2);
        boolean changed = module.settings().stream().anyMatch(s -> !s.isDefault());
        boolean resetOver = changed && Ui.inside(mx, my, r - 92, PY + 18, 58, 28);
        Ui.card(g, r - 92, PY + 18, 58, 28, 7, Ui.alpha(Ui.RAISED, Ui.ease("cg:reset", resetOver)), Ui.STROKE);
        Ui.textCentre(g, "Reset", r - 63, PY + 26, 11, JilatonType.Face.MEDIUM, changed ? Ui.TEXT_2 : Ui.FAINT);
        // Description, two lines at most.
        String[] lines = wrap(module.description(), DRAWER - 36, 11, 2);
        for (int i = 0; i < lines.length; i++) Ui.text(g, lines[i], x, PY + 52 + i * 15, 11, Ui.TEXT_2);
        // Keybind and on/off.
        float ky = PY + 90;
        Ui.text(g, "Keybind", x, ky + 9, 12, Ui.TEXT_2);
        String key = keyName(module);
        float kw = Math.max(36, Ui.width(key, 11, JilatonType.Face.MONO) + 16);
        boolean keyOver = Ui.inside(mx, my, r - kw - 46, ky + 2, kw, 26);
        Ui.card(g, r - kw - 46, ky + 2, kw, 26, 6, keyOver || module.id().equals(bindingId) ? Ui.RAISED : Ui.CARD,
                module.id().equals(bindingId) ? Ui.alpha(accent, 0.6) : Ui.STROKE);
        Ui.textCentre(g, key, r - kw / 2 - 46, ky + 9, 11, JilatonType.Face.MONO, module.id().equals(bindingId) ? accent : Ui.TEXT);
        Ui.smallToggle(g, r - 32, ky + 6, Ui.ease("cg:tog:" + module.id(), module.enabled()), accent);
        Ui.hline(g, x0 + 1, drawerTop() - 8, DRAWER - 2, Ui.STROKE);

        drawerScroll = Math.max(0, Math.min(drawerMax(module), drawerScroll));
        float top = drawerTop() - (float) drawerScroll;
        g.enableScissor(Math.round(x0), Math.round(drawerTop() - 6), Math.round(PX + PW), Math.round(PY + PH - 12));
        if (module.settings().isEmpty()) Ui.text(g, "This module has no settings.", x, drawerTop() + 6, 12, Ui.TEXT_3);
        for (Row row : drawerRows(module, top)) {
            if (row.y() + row.h() < drawerTop() - 6 || row.y() > PY + PH) continue;
            drawerRow(g, module, row, x, r, mx, my, accent);
        }
        if (module.id().equals("cosmetic_knife")) {
            float hy = top + drawerContentHeight(module) - 44;
            Ui.text(g, "Hold V to keep a CS knife spinning.", x, hy, 11, Ui.TEXT_3);
            Ui.text(g, "Blade colour follows the sword you hold.", x, hy + 15, 11, Ui.TEXT_3);
        }
        g.disableScissor();
    }

    private void drawerRow(GuiGraphics g, ClientModule module, Row row, float x, float r, double mx, double my, int accent) {
        Setting setting = row.setting();
        float y = row.y();
        String key = module.id() + "." + setting.id();
        if (chipGrid(setting)) {
            ChoiceSetting choice = (ChoiceSetting) setting;
            Ui.spaced(g, setting.label().toUpperCase(Locale.ROOT), x, y + 6, 10, 1, JilatonType.Face.MEDIUM, Ui.TEXT_3);
            float cw = (r - x - 6) / 2;
            List<String> options = choice.choices();
            for (int i = 0; i < options.size(); i++) {
                float cx = x + (i % 2) * (cw + 6), cy = y + 22 + (i / 2) * 40f;
                String option = options.get(i);
                boolean selected = option.equals(choice.value());
                boolean over = Ui.inside(mx, my, cx, cy, cw, 34);
                double t = Ui.ease("cg:chip:" + key + ":" + option, over);
                Ui.round(g, cx, cy, cw, 34, 8, selected ? Ui.alpha(accent, 0.14) : Ui.mix(Ui.CARD, Ui.RAISED, t));
                Ui.outline(g, cx, cy, cw, 34, 8, selected ? Ui.alpha(accent, 0.55) : Ui.mix(Ui.STROKE, Ui.FAINT, t));
                boolean cs = option.startsWith("CS ");
                String label = cs ? option.substring(3) : option;
                Ui.text(g, Ui.fit(label, cw - (cs ? 44 : 20), 12, JilatonType.Face.MEDIUM), cx + 10, cy + 11, 12,
                        JilatonType.Face.MEDIUM, selected ? Ui.TEXT : Ui.TEXT_2);
                if (cs) Ui.textRight(g, "CS", cx + cw - 10, cy + 12, 10, JilatonType.Face.MEDIUM, Ui.TEXT_3);
            }
            return;
        }
        boolean over = Ui.inside(mx, my, x - 6, y, r - x + 12, row.h());
        int labelColor = over ? Ui.TEXT : Ui.TEXT_2;
        if (setting instanceof NumberSetting number) {
            Ui.text(g, Ui.fit(setting.label(), r - x - 70, 12, JilatonType.Face.REGULAR), x, y + 8, 12, labelColor);
            Ui.textRight(g, format(number), r, y + 8, 12, JilatonType.Face.MONO, Ui.TEXT);
            double range = number.maximum() - number.minimum();
            double f = range <= 0 ? 0 : (number.value() - number.minimum()) / range;
            float w = r - x;
            double shown = Ui.ease("cg:numhint:" + key, true) * 0 + f;
            Ui.round(g, x, y + 31, w, 4, 2, Ui.STROKE);
            if (shown > 0) Ui.round(g, x, y + 31, (float) (w * shown), 4, 2, accent);
            double k = Ui.ease("cg:knob:" + key, over || dragging == number);
            Ui.circle(g, (float) (x + w * shown), y + 33, (float) (7 + k), Ui.TEXT);
        } else if (setting instanceof BooleanSetting bool) {
            Ui.text(g, Ui.fit(setting.label(), r - x - 50, 12, JilatonType.Face.REGULAR), x, y + 10, 12, labelColor);
            Ui.smallToggle(g, r - 32, y + 8, Ui.ease("cg:b:" + key, bool.value()), accent);
        } else if (setting instanceof ChoiceSetting choice) {
            Ui.text(g, Ui.fit(setting.label(), r - x - 130, 12, JilatonType.Face.REGULAR), x, y + 10, 12, labelColor);
            float pw = 120;
            Ui.card(g, r - pw, y + 4, pw, 26, 7, over ? Ui.RAISED : Ui.CARD, Ui.STROKE);
            Ui.text(g, "‹", r - pw + 8, y + 9, 13, Ui.TEXT_3);
            Ui.textCentre(g, Ui.fit(choice.value(), pw - 36, 12, JilatonType.Face.MEDIUM), r - pw / 2, y + 11, 12,
                    JilatonType.Face.MEDIUM, Ui.TEXT);
            Ui.textRight(g, "›", r - 8, y + 9, 13, JilatonType.Face.REGULAR, Ui.TEXT_3);
        } else if (setting instanceof TextSetting text) {
            Ui.text(g, Ui.fit(setting.label(), r - x - 130, 12, JilatonType.Face.REGULAR), x, y + 10, 12, labelColor);
            float pw = 120;
            Ui.card(g, r - pw, y + 4, pw, 26, 7, over ? Ui.RAISED : Ui.CARD, Ui.STROKE);
            String value = text.value().isEmpty() ? "Edit…" : text.value();
            Ui.text(g, Ui.fit(value, pw - 20, 12, JilatonType.Face.REGULAR), r - pw + 10, y + 11, 12,
                    text.value().isEmpty() ? Ui.TEXT_3 : Ui.TEXT);
        }
    }

    private static String format(NumberSetting setting) {
        double span = setting.maximum() - setting.minimum();
        return span >= 20 ? String.format(Locale.ROOT, "%.0f", setting.value())
                : String.format(Locale.ROOT, "%.2f", setting.value());
    }

    private static String[] wrap(String text, float width, float size, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (Ui.width(next, size, JilatonType.Face.REGULAR) > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
                if (lines.size() == maxLines) break;
            } else line = new StringBuilder(next);
        }
        if (lines.size() < maxLines && !line.isEmpty()) lines.add(line.toString());
        else if (lines.size() == maxLines && !line.isEmpty()) {
            String last = lines.get(maxLines - 1);
            lines.set(maxLines - 1, Ui.fit(last + " " + line, width, size, JilatonType.Face.REGULAR));
        }
        return lines.toArray(new String[0]);
    }

    // ---------------------------------------------------------------- other pages

    private void renderSettingsPage(GuiGraphics g, double mx, double my, int accent) {
        renderTitle(g, "Settings", "Layouts and studios", null, mx, my, accent);
        featureCard(g, "HUD editor", "Move, resize and style every HUD widget", MAIN_X, LIST_Y, mx, my, accent);
        featureCard(g, "Visual studio", "Tune world effects with a live preview", MAIN_X, LIST_Y + 84, mx, my, accent);
    }

    private void featureCard(GuiGraphics g, String title, String detail, float x, float y, double mx, double my, int accent) {
        float w = mainRight() - MAIN_X;
        boolean over = Ui.inside(mx, my, x, y, w, 72);
        double t = Ui.ease("cg:feature:" + title, over);
        Ui.round(g, x, y, w, 72, 12, Ui.mix(Ui.CARD, Ui.RAISED, t));
        Ui.outline(g, x, y, w, 72, 12, Ui.mix(Ui.STROKE, Ui.FAINT, t));
        Ui.text(g, title, x + 18, y + 18, 15, JilatonType.Face.SEMIBOLD, Ui.TEXT);
        Ui.text(g, detail, x + 18, y + 42, 12, Ui.TEXT_2);
        Ui.button(g, "Open", x + w - 18 - 76, y + 18, 76, 36, t, false, accent);
    }

    private void renderConfigs(GuiGraphics g, double mx, double my, int accent) {
        renderTitle(g, "Configs", profiles.size() == 1 ? "1 saved profile" : profiles.size() + " saved profiles", null, mx, my, accent);
        Ui.card(g, MAIN_X, LIST_Y, 390, 40, 10, Ui.CARD, profileName.isFocused() ? Ui.alpha(accent, 0.55) : Ui.STROKE);
        float bx = MAIN_X + 402;
        Ui.button(g, "Save", bx, LIST_Y + 2, 72, 36, Ui.inside(mx, my, bx, LIST_Y + 2, 72, 36), true, accent);
        boolean has = selectedProfile != null;
        actionButton(g, "Load", bx + 80, LIST_Y + 2, has, mx, my, accent);
        actionButton(g, "Delete", bx + 160, LIST_Y + 2, has, mx, my, accent);
        actionButton(g, "Export", bx + 240, LIST_Y + 2, true, mx, my, accent);
        actionButton(g, "Import", bx + 320, LIST_Y + 2, true, mx, my, accent);
        if (!configStatus.isEmpty()) Ui.text(g, configStatus, MAIN_X, LIST_Y + 46, 11, Ui.TEXT_3);
        float y = LIST_Y + 60;
        if (profiles.isEmpty()) Ui.text(g, "No profiles yet. Name one above and press Save.", MAIN_X, y + 8, 13, Ui.TEXT_2);
        for (String profile : profiles) {
            if (y + 48 > listBottom()) break;
            boolean selected = profile.equals(selectedProfile);
            boolean over = Ui.inside(mx, my, MAIN_X, y, mainRight() - MAIN_X, 48);
            double t = Ui.ease("cg:profile:" + profile, over);
            Ui.round(g, MAIN_X, y, mainRight() - MAIN_X, 48, 10, selected ? Ui.alpha(accent, 0.10) : Ui.mix(Ui.CARD, Ui.RAISED, t));
            Ui.outline(g, MAIN_X, y, mainRight() - MAIN_X, 48, 10, selected ? Ui.alpha(accent, 0.5) : Ui.STROKE);
            Ui.text(g, profile, MAIN_X + 16, y + 17, 13, JilatonType.Face.MEDIUM, Ui.TEXT);
            y += 56;
        }
    }

    private void actionButton(GuiGraphics g, String label, float x, float y, boolean enabled, double mx, double my, int accent) {
        if (enabled) Ui.button(g, label, x, y, 72, 36, Ui.inside(mx, my, x, y, 72, 36), false, accent);
        else {
            Ui.outline(g, x, y, 72, 36, 10, Ui.alpha(Ui.STROKE, 0.6));
            Ui.textCentre(g, label, x + 36, y + 12, 13, JilatonType.Face.MEDIUM, Ui.FAINT);
        }
    }

    private void renderTheme(GuiGraphics g, double mx, double my, int accent) {
        renderTitle(g, "Theme", "Accent, motion and type", null, mx, my, accent);
        float w = mainRight() - MAIN_X - 316, x = MAIN_X;
        Ui.spaced(g, "ACCENT", x, LIST_Y, 10, 1, JilatonType.Face.MEDIUM, Ui.TEXT_3);
        ClientTheme[] themes = ClientTheme.values();
        float sw = (w - 3 * 8) / 4;
        for (int i = 0; i <= themes.length; i++) {
            boolean custom = i == themes.length;
            String id = custom ? "custom" : themes[i].id();
            String label = custom ? "Custom" : themes[i].label();
            int color = custom ? customAccent() : themes[i].accent();
            float sx = x + (i % 4) * (sw + 8), sy = LIST_Y + 18 + (i / 4) * 72f;
            boolean selected = config.themeId().equals(id);
            boolean over = Ui.inside(mx, my, sx, sy, sw, 64);
            double t = Ui.ease("cg:swatch:" + id, over);
            Ui.round(g, sx, sy, sw, 64, 12, Ui.mix(Ui.CARD, Ui.RAISED, Math.max(t, selected ? 0.5 : 0)));
            Ui.outline(g, sx, sy, sw, 64, 12, selected ? Ui.alpha(color, 0.8) : Ui.mix(Ui.STROKE, Ui.FAINT, t));
            Ui.glow(g, sx + 24, sy + 32, 18, color & 0xFFFFFF, selected ? 0.35 : 0.18);
            Ui.circle(g, sx + 24, sy + 32, 8, color);
            Ui.text(g, Ui.fit(label, sw - 52, 13, JilatonType.Face.MEDIUM), sx + 42, sy + 17, 13, JilatonType.Face.MEDIUM,
                    selected ? Ui.TEXT : Ui.TEXT_2);
            Ui.text(g, custom ? "#" + config.customAccent() : id.toUpperCase(Locale.ROOT), sx + 42, sy + 35, 10,
                    JilatonType.Face.MONO, Ui.TEXT_3);
        }
        int rows = (themes.length + 1 + 3) / 4;
        float y = LIST_Y + 18 + rows * 72f + 18;
        Ui.spaced(g, "MOTION & TYPE", x, y, 10, 1, JilatonType.Face.MEDIUM, Ui.TEXT_3);
        y += 20;
        Ui.text(g, "Reduced motion", x, y + 8, 13, Ui.TEXT_2);
        Ui.toggle(g, x + w - 40, y + 3, Ui.ease("cg:reduced", config.reducedMotion()), accent);
        y += 40;
        Ui.text(g, "Animation intensity", x, y + 6, 13, Ui.TEXT_2);
        float tx = x + 170, tw = w - 170 - 50;
        double f = config.animationIntensity() / 1.5;
        Ui.round(g, tx, y + 10, tw, 4, 2, Ui.STROKE);
        Ui.round(g, tx, y + 10, (float) (tw * f), 4, 2, accent);
        Ui.circle(g, (float) (tx + tw * f), y + 12, 7, Ui.TEXT);
        Ui.textRight(g, Math.round(config.animationIntensity() * 100) + "%", x + w, y + 6, 12, JilatonType.Face.MONO, Ui.TEXT);
        y += 40;
        Ui.text(g, "Backdrop blur", x, y + 8, 13, Ui.TEXT_2);
        Ui.toggle(g, x + w - 40, y + 3, Ui.ease("cg:blur", config.backdropBlur()), accent);
        y += 40;
        Ui.text(g, "Backdrop opacity", x, y + 6, 13, Ui.TEXT_2);
        double o = (config.backdropOpacity() - 0.2) / 0.75;
        Ui.round(g, tx, y + 10, tw, 4, 2, Ui.STROKE);
        Ui.round(g, tx, y + 10, (float) (tw * o), 4, 2, accent);
        Ui.circle(g, (float) (tx + tw * o), y + 12, 7, Ui.TEXT);
        Ui.textRight(g, Math.round(config.backdropOpacity() * 100) + "%", x + w, y + 6, 12, JilatonType.Face.MONO, Ui.TEXT);
        y += 40;
        Ui.text(g, "Font", x, y + 8, 13, Ui.TEXT_2);
        String[] fonts = {"Inter", "Minecraft"};
        float segW = 6;
        for (String f2 : fonts) segW += Ui.width(f2, 12, JilatonType.Face.MEDIUM) + 28;
        float segX = x + w - segW;
        Ui.card(g, segX, y, segW, 30, 8, Ui.CARD, Ui.STROKE);
        float cx = segX + 3;
        for (int i = 0; i < 2; i++) {
            float fw = Ui.width(fonts[i], 12, JilatonType.Face.MEDIUM) + 28;
            boolean on = (i == 0) == JilatonType.enabled();
            if (on) Ui.round(g, cx, y + 3, fw, 24, 6, Ui.TEXT);
            Ui.text(g, fonts[i], cx + 14, y + 9, 12, JilatonType.Face.MEDIUM, on ? Ui.DEEP : Ui.TEXT_2);
            cx += fw;
        }
        y += 40;
        slider(g, "GUI size", x, y, w, (config.guiScale() - 0.7) / 0.3, Math.round(config.guiScale() * 100) + "%", accent);
        y += 40;
        slider(g, "Transition speed", x, y, w, (config.transitionSpeed() - 0.5) / 1.5,
                String.format(java.util.Locale.ROOT, "%.2fx", config.transitionSpeed()), accent);
        // Live preview.
        float pvx = mainRight() - 300, pvy = LIST_Y - 2;
        Ui.card(g, pvx, pvy, 300, 250, 12, Ui.DRAWER, Ui.STROKE);
        Ui.spaced(g, "LIVE PREVIEW", pvx + 16, pvy + 16, 10, 1, JilatonType.Face.MEDIUM, Ui.TEXT_3);
        Ui.round(g, pvx + 12, pvy + 36, 276, 40, 8, Ui.alpha(accent, 0.08));
        Ui.text(g, "Combat", pvx + 22, pvy + 49, 13, JilatonType.Face.SEMIBOLD, accent);
        Ui.textRight(g, "2 / 28", pvx + 278, pvy + 50, 11, JilatonType.Face.MONO, accent);
        String[][] cards = {{"KillAura", "R · Attacks targets in range"}, {"Velocity", "Unbound · Reduces knockback"}};
        for (int i = 0; i < 2; i++) {
            float cy = pvy + 88 + i * 70;
            Ui.round(g, pvx + 12, cy, 276, 62, 12, Ui.CARD);
            if (i == 0) Ui.glowRing(g, pvx + 12, cy, 276, 62, 12, accent, 1);
            else Ui.outline(g, pvx + 12, cy, 276, 62, 12, Ui.STROKE);
            Ui.text(g, cards[i][0], pvx + 28, cy + 15, 13, JilatonType.Face.SEMIBOLD, Ui.TEXT);
            Ui.text(g, cards[i][1], pvx + 28, cy + 35, 11, Ui.TEXT_2);
            Ui.toggle(g, pvx + 288 - 16 - 40, cy + 20, i == 0 ? 1 : 0, accent);
        }
    }

    /** A labelled slider row in the Theme page's style; {@code f} is 0..1. */
    private void slider(GuiGraphics g, String label, float x, float y, float w, double f, String value, int accent) {
        float tx = x + 170, tw = w - 170 - 50;
        f = Math.max(0, Math.min(1, f));
        Ui.text(g, label, x, y + 6, 13, Ui.TEXT_2);
        Ui.round(g, tx, y + 10, tw, 4, 2, Ui.STROKE);
        Ui.round(g, tx, y + 10, (float) (tw * f), 4, 2, accent);
        Ui.circle(g, (float) (tx + tw * f), y + 12, 7, Ui.TEXT);
        Ui.textRight(g, value, x + w, y + 6, 12, JilatonType.Face.MONO, Ui.TEXT);
    }

    private int customAccent() {
        try {
            return 0xFF000000 | Integer.parseInt(config.customAccent(), 16);
        } catch (NumberFormatException exception) {
            return Ui.TEXT;
        }
    }

    private void renderFooter(GuiGraphics g) {
        float right = drawerModule() != null ? PX + PW - DRAWER : PX + PW;
        Ui.hline(g, PX + SIDE + 1, FOOTER_Y, right - PX - SIDE - 2, Ui.STROKE);
        String hint = bindingId != null ? "Press a key  ·  Backspace unbinds  ·  Esc cancels"
                : page == Page.MODULES ? "Click the switch to toggle  ·  Click a card for settings  ·  Drag the grip to reorder  ·  Esc to close"
                : "Esc to close  ·  Ctrl F to search modules";
        Ui.textCentre(g, hint, (PX + SIDE + right) / 2, FOOTER_Y + 14, 11, JilatonType.Face.REGULAR,
                bindingId != null ? Ui.accent() : Ui.TEXT_3);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = canvas.x(event.x()), my = canvas.y(event.y());
        int button = event.button();
        if (bindingId != null) bindingId = null;
        // Search field.
        float right = mainRight();
        if (Ui.inside(mx, my, right - 260, HEADER_Y + 7, 260, 34)) {
            setFocused(search);
            search.setFocused(true);
            profileName.setFocused(false);
            super.mouseClicked(event, doubleClick);
            return true;
        }
        search.setFocused(false);
        if (page == Page.CONFIGS && Ui.inside(mx, my, MAIN_X, LIST_Y, 390, 40)) {
            setFocused(profileName);
            profileName.setFocused(true);
            super.mouseClicked(event, doubleClick);
            return true;
        }
        profileName.setFocused(false);
        if (button != 0 && button != 1) return super.mouseClicked(event, doubleClick);
        if (clickSidebar(mx, my)) return true;
        ClientModule drawer = drawerModule();
        if (drawer != null && mx >= PX + PW - DRAWER && mx <= PX + PW && my >= PY && my <= PY + PH)
            return clickDrawer(drawer, mx, my, button);
        switch (page) {
            case MODULES -> { if (clickModules(mx, my, button)) return true; }
            case SETTINGS -> { if (clickSettings(mx, my)) return true; }
            case CONFIGS -> { if (clickConfigs(mx, my)) return true; }
            case THEME -> { if (clickTheme(mx, my)) return true; }
        }
        if (!Ui.inside(mx, my, PX, PY, PW, PH)) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean clickSidebar(double mx, double my) {
        dragCard = null;
        cardDragging = false;
        dropIndex = -1;
        float x = PX + 14, y = PY + 92;
        for (String c : CATEGORIES) {
            if (Ui.inside(mx, my, x, y, SIDE - 28, 40)) {
                config.setCategoryScroll(category, targetScroll);
                category = c;
                page = Page.MODULES;
                scroll = targetScroll = config.categoryScroll(c);
                if (drawerId != null && modules.get(drawerId) != null && !inCategory(modules.get(drawerId), c)) drawerId = null;
                remember();
                return true;
            }
            y += 44;
        }
        y += 30;
        for (int i = 0; i < GENERAL.length; i++) {
            if (Ui.inside(mx, my, x, y, SIDE - 28, 40)) {
                page = Page.values()[i + 1];
                remember();
                return true;
            }
            y += 44;
        }
        return Ui.inside(mx, my, PX, PY, SIDE, PH);
    }

    private boolean clickModules(double mx, double my, int button) {
        float right = mainRight();
        float filterW = Ui.width("Enabled only", 12, JilatonType.Face.MEDIUM) + 34;
        int on = 0, total = 0;
        for (ClientModule m : allModules) if (inCategory(m, category)) { total++; if (m.enabled()) on++; }
        float rightW = Ui.width(on + " of " + total + " active", 12, JilatonType.Face.REGULAR) + 14;
        if (Ui.inside(mx, my, right - rightW - 12 - filterW, TITLE_Y + 4, filterW, 24)) {
            enabledOnly = !enabledOnly;
            targetScroll = 0;
            remember();
            return true;
        }
        if (my < LIST_Y || my > listBottom() || mx < MAIN_X || mx > right) return false;
        List<ClientModule> list = visible();
        int cols = columns();
        float cw = (right - MAIN_X - GAP * (cols - 1)) / cols, ch = cardHeight();
        for (int i = 0; i < list.size(); i++) {
            float x = MAIN_X + (i % cols) * (cw + GAP);
            float y = (float) (LIST_Y + (i / cols) * (ch + GAP) - scroll);
            if (!Ui.inside(mx, my, x, y, cw, ch)) continue;
            ClientModule module = list.get(i);
            if (button == 0 && gripHit(mx, my, x, y, ch)) {
                dragCard = module.id();
                dragCardX = mx;
                dragCardY = my;
                cardDragging = false;
                dropIndex = i;
                return true;
            }
            if (button == 0 && toggleHit(mx, my, x, y, cw, visualPage())) {
                module.toggle();
                config.save(modules);
            } else {
                drawerId = module.id().equals(drawerId) ? null : module.id();
                drawerScroll = 0;
            }
            remember();
            return true;
        }
        return true;
    }

    /** True only on the card's switch; the rest of the card opens the settings drawer. */
    static boolean toggleHit(double mx, double my, float x, float y, float w, boolean visual) {
        return visual
                ? Ui.inside(mx, my, x + w - 14 - Ui.SMALL_TOGGLE_W, y + 12, Ui.SMALL_TOGGLE_W, Ui.SMALL_TOGGLE_H)
                : Ui.inside(mx, my, x + w - 16 - Ui.TOGGLE_W, y + 20, Ui.TOGGLE_W, Ui.TOGGLE_H);
    }

    /**
     * Persists a card drop: {@code moved} lands at {@code target} in the list the player sees. Modules hidden by
     * the category, the "Enabled only" chip or a search keep their slots (the legacy board's merge), and a drop on
     * Visuals also rewrites the render order the preview grid follows.
     */
    static void applyReorder(ClientConfig config, ModuleManager modules, List<ClientModule> all,
                             List<ClientModule> visible, ClientModule moved, int target, boolean visual) {
        List<String> current = new ArrayList<>();
        for (ClientModule module : visible) current.add(module.id());
        List<String> active = visual && !config.visualOrder().isEmpty() ? config.visualOrder() : config.moduleOrder();
        List<String> base = active.isEmpty() ? current : ModuleOrder.apply(active, current);
        List<String> next = ModuleOrder.move(base, moved.id(), target);
        List<String> allIds = new ArrayList<>();
        for (ClientModule module : all) allIds.add(module.id());
        config.setModuleOrder(ModuleOrder.apply(next, ModuleOrder.apply(config.moduleOrder(), allIds)));
        if (visual || moved.category() == ModuleCategory.RENDER) {
            List<String> renderIds = new ArrayList<>();
            for (ClientModule module : all) if (module.category() == ModuleCategory.RENDER) renderIds.add(module.id());
            config.setVisualOrder(ModuleOrder.apply(next, ModuleOrder.apply(config.visualOrder(), renderIds)));
        }
        config.save(modules);
    }

    private boolean clickDrawer(ClientModule module, double mx, double my, int button) {
        float x0 = PX + PW - DRAWER, x = x0 + 18, r = PX + PW - 18;
        if (Ui.inside(mx, my, r - 28, PY + 18, 28, 28)) {
            drawerId = null;
            remember();
            return true;
        }
        if (Ui.inside(mx, my, r - 92, PY + 18, 58, 28)) {
            module.resetSettings();
            config.save(modules);
            return true;
        }
        if (button != 0) return true;
        float ky = PY + 90;
        String key = keyName(module);
        float kw = Math.max(36, Ui.width(key, 11, JilatonType.Face.MONO) + 16);
        if (Ui.inside(mx, my, r - kw - 46, ky + 2, kw, 26)) {
            bindingId = module.id();
            return true;
        }
        if (Ui.inside(mx, my, r - 36, ky + 2, 40, 26)) {
            module.toggle();
            config.save(modules);
            return true;
        }
        if (my < drawerTop() - 6) return true;
        for (Row row : drawerRows(module, drawerTop() - (float) drawerScroll)) {
            if (my < row.y() || my >= row.y() + row.h()) continue;
            Setting setting = row.setting();
            if (chipGrid(setting)) {
                ChoiceSetting choice = (ChoiceSetting) setting;
                float cw = (r - x - 6) / 2;
                for (int i = 0; i < choice.choices().size(); i++) {
                    float cx = x + (i % 2) * (cw + 6), cy = row.y() + 22 + (i / 2) * 40f;
                    if (Ui.inside(mx, my, cx, cy, cw, 34)) {
                        choice.setValue(choice.choices().get(i));
                        config.save(modules);
                    }
                }
            } else if (setting instanceof NumberSetting number) {
                dragging = number;
                dragX = x;
                dragW = r - x;
                applySlider(mx);
            } else if (setting instanceof BooleanSetting bool) {
                bool.toggle();
                config.save(modules);
            } else if (setting instanceof ChoiceSetting choice) {
                if (mx >= r - 120) {
                    choice.cycle(mx < r - 60 ? -1 : 1);
                    config.save(modules);
                }
            } else if (setting instanceof TextSetting text) {
                if (mx >= r - 120) minecraft.setScreen(new TextSettingScreen(this, text, modules, config));
            }
            return true;
        }
        return true;
    }

    private boolean clickSettings(double mx, double my) {
        float w = mainRight() - MAIN_X;
        if (Ui.inside(mx, my, MAIN_X, LIST_Y, w, 72)) {
            remember();
            config.save(modules);
            minecraft.setScreen(new HudEditorScreen(modules, config, this));
            return true;
        }
        if (Ui.inside(mx, my, MAIN_X, LIST_Y + 84, w, 72)) {
            remember();
            config.save(modules);
            minecraft.setScreen(new VisualStudioScreen(this));
            return true;
        }
        return false;
    }

    private boolean clickConfigs(double mx, double my) {
        float bx = MAIN_X + 402;
        try {
            if (Ui.inside(mx, my, bx, LIST_Y + 2, 72, 36)) {
                String name = profileName.getValue().strip();
                if (!name.isEmpty()) {
                    config.saveProfile(name, modules);
                    selectedProfile = name;
                    refreshProfiles();
                }
                return true;
            }
            if (selectedProfile != null && Ui.inside(mx, my, bx + 80, LIST_Y + 2, 72, 36)) {
                if (config.loadProfile(selectedProfile, modules)) config.save(modules);
                return true;
            }
            if (Ui.inside(mx, my, bx + 240, LIST_Y + 2, 72, 36)) {
                minecraft.keyboardHandler.setClipboard(config.exportText(modules));
                configStatus = "Current settings copied to the clipboard";
                return true;
            }
            if (Ui.inside(mx, my, bx + 320, LIST_Y + 2, 72, 36)) {
                String name = profileName.getValue().strip();
                if (name.isEmpty()) name = "imported";
                if (!ClientConfig.isValidProfileName(name)) {
                    configStatus = "Profile names use letters, digits, - and _";
                } else if (config.importText(name, minecraft.keyboardHandler.getClipboard())) {
                    selectedProfile = name;
                    refreshProfiles();
                    configStatus = "Imported as \"" + name + "\"; press Load to apply it";
                } else {
                    configStatus = "The clipboard does not hold a Jilaton export";
                }
                return true;
            }
            if (selectedProfile != null && Ui.inside(mx, my, bx + 160, LIST_Y + 2, 72, 36)) {
                if (config.deleteProfile(selectedProfile)) selectedProfile = null;
                refreshProfiles();
                return true;
            }
        } catch (IOException exception) {
            return true;
        }
        float y = LIST_Y + 60;
        for (String profile : profiles) {
            if (Ui.inside(mx, my, MAIN_X, y, mainRight() - MAIN_X, 48)) {
                selectedProfile = profile;
                profileName.setValue(profile);
                remember();
                return true;
            }
            y += 56;
        }
        return false;
    }

    private boolean clickTheme(double mx, double my) {
        float w = mainRight() - MAIN_X - 316, x = MAIN_X;
        ClientTheme[] themes = ClientTheme.values();
        float sw = (w - 3 * 8) / 4;
        for (int i = 0; i <= themes.length; i++) {
            float sx = x + (i % 4) * (sw + 8), sy = LIST_Y + 18 + (i / 4) * 72f;
            if (Ui.inside(mx, my, sx, sy, sw, 64)) {
                config.setThemeId(i == themes.length ? "custom" : themes[i].id());
                config.save(modules);
                return true;
            }
        }
        int rows = (themes.length + 1 + 3) / 4;
        float y = LIST_Y + 18 + rows * 72f + 18 + 20;
        if (Ui.inside(mx, my, x, y, w, 34)) {
            config.setReducedMotion(!config.reducedMotion());
            config.save(modules);
            return true;
        }
        y += 40;
        float tx = x + 170, tw = w - 170 - 50;
        if (Ui.inside(mx, my, tx - 6, y, tw + 12, 24)) {
            dragMotion = true;
            dragX = tx;
            dragW = tw;
            applySlider(mx);
            return true;
        }
        y += 40;
        if (Ui.inside(mx, my, x, y, w, 34)) {
            config.setBackdropBlur(!config.backdropBlur());
            config.save(modules);
            return true;
        }
        y += 40;
        if (Ui.inside(mx, my, tx - 6, y, tw + 12, 24)) {
            dragBackdrop = true;
            dragX = tx;
            dragW = tw;
            applySlider(mx);
            return true;
        }
        y += 40;
        String[] fonts = {"Inter", "Minecraft"};
        float segW = 6;
        for (String f : fonts) segW += Ui.width(f, 12, JilatonType.Face.MEDIUM) + 28;
        float segX = x + w - segW;
        if (Ui.inside(mx, my, segX, y, segW, 30)) {
            boolean inter = mx < segX + 3 + Ui.width("Inter", 12, JilatonType.Face.MEDIUM) + 28;
            config.setUiFont(inter ? "inter" : "minecraft");
            config.save(modules);
            return true;
        }
        y += 40;
        if (Ui.inside(mx, my, tx - 6, y, tw + 12, 24)) {
            dragScale = true;
            dragX = tx;
            dragW = tw;
            applySlider(mx);
            return true;
        }
        y += 40;
        if (Ui.inside(mx, my, tx - 6, y, tw + 12, 24)) {
            dragSpeed = true;
            dragX = tx;
            dragW = tw;
            applySlider(mx);
            return true;
        }
        return Ui.inside(mx, my, MAIN_X, LIST_Y, mainRight() - MAIN_X, listBottom() - LIST_Y);
    }

    private void applySlider(double mx) {
        double f = Math.max(0, Math.min(1, (mx - dragX) / Math.max(1, dragW)));
        if (dragMotion) config.setAnimationIntensity(f * 1.5);
        else if (dragBackdrop) config.setBackdropOpacity(0.2 + f * 0.75);
        else if (dragScale) config.setGuiScale(0.7 + f * 0.3);
        else if (dragSpeed) config.setTransitionSpeed(0.5 + f * 1.5);
        else if (dragging != null) dragging.setValue(dragging.minimum() + f * (dragging.maximum() - dragging.minimum()));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragXAmount, double dragYAmount) {
        if (dragCard != null) {
            double mx = canvas.x(event.x()), my = canvas.y(event.y());
            if (!cardDragging && Math.hypot(mx - dragCardX, my - dragCardY) >= 4) cardDragging = true;
            if (cardDragging) dropIndex = dropIndexAt(mx, my);
            return true;
        }
        if (dragging != null || dragMotion || dragBackdrop || dragScale || dragSpeed) {
            applySlider(canvas.x(event.x()));
            return true;
        }
        return super.mouseDragged(event, dragXAmount, dragYAmount);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragCard != null) {
            boolean dropped = cardDragging;
            int index = dropIndex;
            ClientModule module = modules.get(dragCard);
            dragCard = null;
            cardDragging = false;
            dropIndex = -1;
            double mx = canvas.x(event.x()), my = canvas.y(event.y());
            if (dropped && module != null && index >= 0
                    && Ui.inside(mx, my, MAIN_X, LIST_Y, mainRight() - MAIN_X, listBottom() - LIST_Y)) {
                List<ClientModule> list = visible();
                if (index < list.size()) {
                    remember();
                    applyReorder(config, modules, allModules, list, module, index, visualPage());
                }
            }
            return true;
        }
        if (dragging != null || dragMotion || dragBackdrop || dragScale || dragSpeed) {
            dragging = null;
            dragMotion = false;
            dragBackdrop = false;
            if (dragScale) canvas.fit(width, height);
            dragScale = false;
            dragSpeed = false;
            config.save(modules);
            return true;
        }
        return super.mouseReleased(event);
    }

    /** The card cell the pointer is over, clamped to the visible list; -1 when nothing is listed. */
    private int dropIndexAt(double mx, double my) {
        List<ClientModule> list = visible();
        if (list.isEmpty()) return -1;
        int cols = columns();
        float cw = (mainRight() - MAIN_X - GAP * (cols - 1)) / cols, ch = cardHeight();
        int row = Math.max(0, (int) ((my - LIST_Y + scroll) / (ch + GAP)));
        int col = Math.max(0, Math.min(cols - 1, (int) ((mx - MAIN_X) / (cw + GAP))));
        return Math.min(list.size() - 1, row * cols + col);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double mx = canvas.x(mouseX);
        ClientModule drawer = drawerModule();
        if (drawer != null && mx >= PX + PW - DRAWER) {
            drawerScroll = Math.max(0, Math.min(drawerMax(drawer), drawerScroll - vertical * 36));
            return true;
        }
        if (page == Page.MODULES) {
            targetScroll = Math.max(0, Math.min(maxScroll(), targetScroll - vertical * 48));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (bindingId != null) {
            ClientModule module = modules.get(bindingId);
            if (key != GLFW.GLFW_KEY_ESCAPE && module != null) {
                module.setKeyCode(key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE ? -1 : key);
                config.save(modules);
            }
            bindingId = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_F && event.hasControlDown()) {
            page = Page.MODULES;
            setFocused(search);
            search.setFocused(true);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (dragCard != null) {
                dragCard = null;
                cardDragging = false;
                dropIndex = -1;
                return true;
            }
            if (search.isFocused() && !search.getValue().isEmpty()) {
                search.setValue("");
                return true;
            }
            if (drawerId != null && page == Page.MODULES) {
                drawerId = null;
                remember();
                return true;
            }
            onClose();
            return true;
        }
        if (search.isFocused() || profileName.isFocused()) return super.keyPressed(event);
        if (key == JilatonClient.clickGuiKeyCode()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
