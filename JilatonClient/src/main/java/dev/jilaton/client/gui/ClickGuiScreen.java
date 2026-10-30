package dev.jilaton.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.gui.shell.CardMetric;
import dev.jilaton.client.gui.shell.ModuleFlow;
import dev.jilaton.client.gui.shell.ModuleGlyph;
import dev.jilaton.client.gui.shell.ModuleOrder;
import dev.jilaton.client.gui.shell.PreviewSpecs;
import dev.jilaton.client.gui.shell.ShellChrome;
import dev.jilaton.client.gui.shell.ShellLayout;
import dev.jilaton.client.gui.shell.ShellMetrics;
import dev.jilaton.client.gui.shell.ShellPalette;
import dev.jilaton.client.gui.shell.TextWrap;
import dev.jilaton.client.gui.shell.VisualPreview;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.ModuleManager;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** ClickGUI shell: measured sidebar, searchable module list and a live Visuals preview grid. */
public final class ClickGuiScreen extends Screen {
    /** Inter instead of the vanilla pixel font for everything this screen draws ({@link JilatonType}). */
    private final Font font = JilatonType.font();
    private static final int ROW_HEIGHT = ShellMetrics.MODULE_CARD_HEIGHT;
    private static final int VISUAL_HEIGHT = ShellMetrics.VISUAL_CARD_HEIGHT;
    private static final int COLUMN_GAP = ShellMetrics.COLUMN_GAP;
    private static final int ROW_GAP = ShellMetrics.ROW_GAP;
    private static final int SETTING_ROW = 30;
    private static final int EXPANSION_HEADER = 24;
    private static final int NUMBER_TRACK = 84;
    private static final int NUMBER_VALUE = 40;
    private static final float TITLE_SCALE = 2.2F;
    private static final float TITLE_SCALE_COMPACT = 1.7F;
    private static final float CARD_TITLE = 1.35F;
    private static final String[] CATEGORIES = {"All", "Combat", "Movement", "Player", "Visuals", "Client"};
    private static final String[] CATEGORY_ICONS = {"all", "combat", "movement", "player", "visuals", "client"};
    private static final String[] GENERAL = {"Settings", "Configs", "Theme"};
    private static final String[] GENERAL_ICONS = {"settings", "configs", "theme"};

    private enum Page {
        MODULES,
        SETTINGS,
        CONFIGS,
        THEME
    }

    private enum DragKind {
        NONE,
        MODULE_NUMBER,
        THEME_HSB,
        MOTION,
        REORDER
    }

    private record View(List<ClientModule> modules, ModuleFlow flow, int cardHeight, int maxScroll) { }

    /** Where the GUI was when it last closed; reopening resumes there for the rest of the game session. */
    private record Resume(Page page, String category, String selectedId, String expandedId, boolean enabledOnly,
                          double scroll, String query) { }

    private static Resume resume;

    private record Counts(int[] enabled, int[] total) { }

    private record SettingsPage(int hudY, int hudWidth, int hudHeight, int studioY, int studioWidth, int studioHeight,
                                int appearY, int reducedY, int motionY, int motionX, int motionWidth,
                                int controlsY, int height) { }

    private record ThemePage(int top, int columns, int cardWidth, int cardHeight, int customTop,
                             int sliderX, int sliderWidth, int rowStep, int panelHeight, int height) { }

    private final ModuleManager modules;
    private final ClientConfig config;
    private final Screen parent;
    private final List<ClientModule> allModules;
    private final Motion.Transition openTransition = new Motion.Transition(0.22, Motion.STANDARD);
    private final Motion.Transition closeTransition = new Motion.Transition(0.16, Motion.STANDARD);
    private final Motion.Transition revealTransition = new Motion.Transition(0.14, Motion.STANDARD);
    private final List<String> visualOrder = new ArrayList<>();
    private final List<String> moduleOrder = new ArrayList<>();
    private final Map<String, AnimatedValue> motion = new HashMap<>();
    private ShellLayout layout = ShellLayout.compute(427, 240);
    private EditBox search;
    private EditBox profileName;
    private List<String> profiles = List.of();
    private String selectedProfile;
    private String profileStatus = "Profiles are stored on this computer.";
    private boolean profileStatusError;
    private Page page = Page.MODULES;
    private String selectedCategory = "All";
    private String selectedId;
    private String expandedId;
    private String bindingModuleId;
    private boolean enabledOnly;
    private int accent;
    private double scroll;
    private double targetScroll;
    private long frameNanos;
    private long lastFrameNanos;
    private final VisualPreview preview = new VisualPreview();
    private boolean closing;
    private String restoredQuery = "";
    private boolean initialising;
    private String clockText = "";
    private int clockMinute = -1;
    private DragKind drag = DragKind.NONE;
    private ClientModule dragModule;
    private Setting dragSetting;
    private int dragChannel = -1;
    private int dragTrackX;
    private int dragTrackWidth;
    private int profileInputWidth;
    private int profileInputY;
    private int profileSaveX;
    private int profileLoadX;
    private int profileDeleteX;

    public ClickGuiScreen(ModuleManager modules, ClientConfig config) {
        this(modules, config, null);
    }

    public ClickGuiScreen(ModuleManager modules, ClientConfig config, Screen parent) {
        super(Component.literal("Jilaton Client"));
        this.modules = modules;
        this.config = config;
        this.parent = parent;
        this.allModules = List.copyOf(modules.all());
        this.accent = selectedAccent();
        this.visualOrder.addAll(config.visualOrder());
        this.moduleOrder.addAll(config.moduleOrder());
        try {
            this.page = Page.valueOf(config.guiPage());
        } catch (IllegalArgumentException ignored) {
            this.page = Page.MODULES;
        }
        this.selectedCategory = config.guiCategory();
        String cfgSelected = config.guiSelectedModule();
        this.selectedId = !cfgSelected.isEmpty() && modules.get(cfgSelected) != null ? cfgSelected : null;
        String cfgExpanded = config.guiExpandedModule();
        this.expandedId = !cfgExpanded.isEmpty() && modules.get(cfgExpanded) != null ? cfgExpanded : null;
        this.enabledOnly = config.guiEnabledOnly();
        double catScroll = config.categoryScroll(this.selectedCategory);
        this.scroll = this.targetScroll = catScroll > 0.0 ? catScroll : config.guiScroll();
        this.restoredQuery = config.guiSearchQuery();
        String cfgProfile = config.guiSelectedProfile();
        this.selectedProfile = cfgProfile.isEmpty() ? null : cfgProfile;
    }

    private void syncStateToConfig() {
        config.setGuiPage(page.name());
        config.setGuiCategory(selectedCategory);
        config.setGuiSelectedModule(selectedId == null ? "" : selectedId);
        config.setGuiExpandedModule(expandedId == null ? "" : expandedId);
        config.setGuiEnabledOnly(enabledOnly);
        config.setGuiSearchQuery(search == null ? restoredQuery : search.getValue());
        config.setGuiSelectedProfile(selectedProfile == null ? "" : selectedProfile);
        config.setGuiScroll(targetScroll);
        if (page == Page.MODULES) {
            config.setCategoryScroll(selectedCategory, targetScroll);
        }
    }

    @Override
    public void removed() {
        syncStateToConfig();
        config.save(modules);
        resume = new Resume(page, selectedCategory, selectedId, expandedId, enabledOnly, targetScroll,
                search == null ? "" : search.getValue());
        super.removed();
    }

    @Override
    protected void init() {
        initialising = true;
        String previousQuery = search == null ? restoredQuery : search.getValue();
        layout = ShellLayout.compute(width, height);
        accent = selectedAccent();

        search = new EditBox(font, layout.searchX() + 26, layout.searchY() + 5,
                Math.max(24, layout.searchWidth() - 88), 14, Component.literal("Search modules"));
        search.setMaxLength(64);
        search.setHint(Component.literal("Search modules"));
        search.setBordered(false);
        search.setTextColor(0xFFE7E5F2);
        search.setResponder(value -> {
            if (initialising) return;
            targetScroll = 0;
            if (!value.isEmpty() && page != Page.MODULES) openPage(Page.MODULES);
        });
        addRenderableWidget(search);

        profileInputY = layout.listTop() + 4;
        profileDeleteX = layout.contentRight() - 62;
        profileLoadX = profileDeleteX - 56;
        profileSaveX = profileLoadX - 56;
        profileInputWidth = Math.max(70, profileSaveX - layout.contentX() - 12);
        profileName = new EditBox(font, layout.contentX() + 8, profileInputY + 4,
                Math.max(40, profileInputWidth - 16), 12, Component.literal("Profile name"));
        profileName.setHint(Component.literal("Profile name"));
        profileName.setMaxLength(32);
        profileName.setBordered(false);
        profileName.setTextColor(0xFFE7E5F2);
        profileName.setVisible(page == Page.CONFIGS);
        addRenderableWidget(profileName);

        search.setValue(previousQuery);
        if (page == Page.MODULES) setInitialFocus(search);
        refreshProfiles();
        initialising = false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        double seconds = lastFrameNanos == 0L ? 0.0 : Math.min(0.05, (now - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = now;
        frameNanos = now;

        boolean reduced = config.reducedMotion();
        double opened = openTransition.update(true, now, reduced);
        double visibility = closing ? opened * (1 - closeTransition.update(true, now, reduced)) : opened;
        if (closing && closeTransition.settled(true)) {
            finishClose();
            return;
        }

        double limit = scrollLimit();
        targetScroll = Math.max(0.0, Math.min(limit, targetScroll));
        if (reduced) {
            scroll = targetScroll;
        } else {
            double easing = 1 - Math.exp(-seconds / 0.07);
            scroll += (targetScroll - scroll) * easing;
            if (Math.abs(targetScroll - scroll) < 0.3) scroll = targetScroll;
        }

        accent = selectedAccent();
        double intensity = config.animationIntensity();
        float scale = (float) (1 - Motion.amount(0.04, intensity, reduced) * (1 - visibility));
        float rise = (float) Motion.amount(closing ? 3 : 8, intensity, reduced) * (float) (1 - visibility);
        float centerX = layout.panelX() + layout.panelWidth() / 2F;
        float centerY = layout.panelY() + layout.panelHeight() / 2F;

        // Minecraft 1.21.11 already blurred the world before this pass; blurring again throws.
        ShellChrome.backdrop(graphics, width, height, visibility);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY + rise);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-centerX, -centerY);

        ShellChrome.panel(graphics, layout, visibility);
        updateClock();
        renderHeader(graphics);
        ShellChrome.headerRule(graphics, layout);
        renderSidebar(graphics, mouseX, mouseY);
        renderTitle(graphics);
        switch (page) {
            case MODULES -> renderModules(graphics, mouseX, mouseY);
            case SETTINGS -> renderSettings(graphics, mouseX, mouseY);
            case CONFIGS -> renderConfigs(graphics, mouseX, mouseY);
            case THEME -> renderTheme(graphics, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        double reveal = revealTransition.update(true, now, reduced);
        if (reveal < 1) {
            graphics.fill(layout.contentX(), layout.listTop() - 6, layout.contentRight(), layout.listBottom(),
                    JilatonUi.alpha(ShellPalette.PANEL, 1 - reveal));
        }
        drawHint(graphics);
        if (visibility < 1) {
            JilatonUi.fillRounded(graphics, layout.panelX(), layout.panelY(), layout.panelWidth(), layout.panelHeight(),
                    10, JilatonUi.alpha(ShellPalette.SIDEBAR, 1 - visibility));
        }
        graphics.pose().popMatrix();
    }

    private void renderHeader(GuiGraphics graphics) {
        int mid = layout.panelY() + layout.headerHeight() / 2;
        int clockWidth = font.width(clockText);
        int clockX = layout.searchX() - 18 - clockWidth;
        boolean showClock = clockX > layout.contentX() + 130;
        int greetX = layout.contentX();
        int greetRight = (showClock ? clockX : layout.searchX()) - 16;
        if (greetRight - greetX > 60) {
            String prefix = "Hello, ";
            String name = fit(playerName(), Math.max(0, greetRight - greetX - Math.round(font.width(prefix) * 1.25F)));
            float scale = 1.25F;
            graphics.pose().pushMatrix();
            graphics.pose().translate(greetX, mid - 5);
            graphics.pose().scale(scale, scale);
            graphics.drawString(font, prefix, 0, 0, ShellPalette.TEXT_2, false);
            graphics.drawString(font, name, font.width(prefix), 0, accent, false);
            graphics.pose().popMatrix();
        }
        if (showClock) graphics.drawString(font, clockText, clockX, mid - 4, ShellPalette.TEXT_2, false);

        ShellChrome.searchFrame(graphics, layout.searchX(), layout.searchY(), layout.searchWidth(),
                layout.searchHeight(), accent, search.isFocused());
        String cap = "CTRL F";
        int capWidth = ShellChrome.keycapWidth(font, cap);
        ShellChrome.keycap(graphics, font, cap, layout.searchX() + layout.searchWidth() - capWidth - 6,
                layout.searchY() + (layout.searchHeight() - 13) / 2, accent, false);
    }

    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = layout.panelX() + 16;
        int width = layout.sidebarWidth() - 32;
        int rowHeight = layout.rowHeight();
        renderBrand(graphics, x);

        sectionLabel(graphics, "MODULES", x + 4, layout.navTop() - 12);
        Counts counts = counts();
        for (int index = 0; index < CATEGORIES.length; index++) {
            int y = layout.navY(index);
            boolean selected = page == Page.MODULES && selectedCategory.equals(CATEGORIES[index]);
            boolean hovered = inside(mouseX, mouseY, x, y, width, rowHeight);
            navRow(graphics, CATEGORY_ICONS[index], CATEGORIES[index], x, y, width, rowHeight, selected, hovered,
                    counts.enabled()[index] + "/" + counts.total()[index]);
        }

        sectionLabel(graphics, "GENERAL", x + 4, layout.generalTop() - 12);
        for (int index = 0; index < GENERAL.length; index++) {
            int y = layout.generalY(index);
            boolean selected = page == switch (index) {
                case 0 -> Page.SETTINGS;
                case 1 -> Page.CONFIGS;
                default -> Page.THEME;
            };
            boolean hovered = inside(mouseX, mouseY, x, y, width, rowHeight);
            navRow(graphics, GENERAL_ICONS[index], GENERAL[index], x, y, width, rowHeight, selected, hovered, null);
        }

        if (layout.footerHeight() > 0) renderUserCard(graphics, x, width);
    }

    /** 72 px logo block: mark, wordmark and a tertiary version chip. */
    private void renderBrand(GuiGraphics graphics, int x) {
        int top = layout.panelY() + 18;
        JilatonLogo.draw(graphics, x + 2, top + 2, 24, ShellPalette.TEXT);
        graphics.drawString(font, "JILATON", x + 36, top + 4, ShellPalette.TEXT, false);
        String version = "v0.1";
        int chipWidth = font.width(version) + 10;
        JilatonUi.fillRounded(graphics, x + 36, top + 16, chipWidth, 13, 4, ShellPalette.CONTROL);
        graphics.drawString(font, version, x + 41, top + 19, ShellPalette.TEXT_3, false);
    }

    private void renderUserCard(GuiGraphics graphics, int x, int width) {
        int y = layout.footerY() + 4;
        int height = layout.footerHeight() - 8;
        JilatonUi.fillRounded(graphics, x, y, width, height, 8, ShellPalette.CARD);
        JilatonUi.strokeRounded(graphics, x, y, width, height, 8, ShellPalette.STROKE);
        ShellChrome.avatar(graphics, x + 10, y + 12, 28, accent);
        String name = fit(playerName(), width - 62);
        graphics.drawString(font, name, x + 46, y + 12, ShellPalette.TEXT, false);
        boolean online = minecraft != null && minecraft.player != null;
        String status = online ? "IN GAME" : "LOCAL PROFILE";
        graphics.drawString(font, status, x + 46, y + 25, ShellPalette.TEXT_3, false);
        ShellChrome.dot(graphics, x + 46 + font.width(status) + 6, y + 27,
                online ? ShellPalette.ONLINE : ShellPalette.STROKE_HOVER);
    }

    /** Uppercase tertiary group label with a little letter spacing. */
    private void sectionLabel(GuiGraphics graphics, String text, int x, int y) {
        int cursor = x;
        for (int index = 0; index < text.length(); index++) {
            String glyph = String.valueOf(text.charAt(index));
            graphics.drawString(font, glyph, cursor, y, ShellPalette.TEXT_3, false);
            cursor += font.width(glyph) + 1;
        }
    }

    /** Active rows are accent icon + accent label + chevron + accent count pill; no filled bar. */
    private void navRow(GuiGraphics graphics, String icon, String label, int x, int y, int width, int height,
                        boolean selected, boolean hovered, String count) {
        if (hovered && !selected) {
            JilatonUi.fillRounded(graphics, x, y + 2, width, height - 4, 6, ShellPalette.CARD);
        }
        int iconSize = 16;
        int color = selected ? accent : hovered ? ShellPalette.TEXT : ShellPalette.TEXT_2;
        ShellChrome.icon(graphics, icon, x + 10, y + (height - iconSize) / 2, iconSize, color);
        String shown = count == null ? label : fit(label, width - 36 - font.width(count) - 30);
        int labelY = y + (height - 8) / 2;
        graphics.drawString(font, shown, x + 36, labelY, selected ? accent : color, false);
        if (selected) {
            ShellChrome.navChevron(graphics, x + 36 + font.width(shown) + 7, y + (height - 7) / 2, accent);
        }
        if (count != null) {
            ShellChrome.countPill(graphics, font, count, x + width - 8, y + (height - 14) / 2, selected, accent);
        }
    }

    private void renderTitle(GuiGraphics graphics) {
        int y = layout.titleY();
        boolean modulesPage = page == Page.MODULES;
        String title;
        String subtitle;
        String meta;
        switch (page) {
            case MODULES -> {
                title = selectedCategory.equals("All") ? "All modules" : selectedCategory;
                subtitle = selectedCategory.equals("Visuals")
                        ? "Live previews drawn from each module's own settings"
                        : "Every module, its key and its live state";
                List<ClientModule> shownModules = view().modules();
                int on = 0;
                for (ClientModule module : shownModules) if (module.enabled()) on++;
                meta = on + " of " + shownModules.size() + " active";
            }
            case SETTINGS -> {
                title = "Settings";
                subtitle = "Client preferences";
                meta = "";
            }
            case CONFIGS -> {
                title = "Configs";
                subtitle = "Profiles stored on this computer";
                meta = profiles.size() == 1 ? "1 profile" : profiles.size() + " profiles";
            }
            default -> {
                title = "Theme";
                subtitle = "Palette and motion";
                meta = "";
            }
        }
        int chipWidth = modulesPage ? chipWidth() : 0;
        int metaWidth = meta.isEmpty() ? 0 : font.width(meta) + (modulesPage ? 14 : 0);
        if (layout.contentRight() - layout.contentX() - metaWidth < 150) {
            meta = "";
            metaWidth = 0;
        }
        float scale = Math.min(layout.compact() ? TITLE_SCALE_COMPACT : TITLE_SCALE,
                Math.max(1.0F, (layout.contentWidth() - metaWidth - 24) / (float) Math.max(1, font.width(title))));
        String shown = fit(title, Math.round((layout.contentWidth() - metaWidth - 24) / Math.max(1.0F, scale)));
        drawScaled(graphics, shown, layout.contentX(), y, scale, ShellPalette.TEXT);
        if (!meta.isEmpty()) {
            int textX = layout.contentRight() - font.width(meta);
            if (modulesPage) ShellChrome.dot(graphics, textX - 10, y + 8, accent);
            graphics.drawString(font, meta, textX, y + 4, ShellPalette.TEXT_2, false);
        }
        graphics.drawString(font, fit(subtitle, layout.contentWidth() - (modulesPage ? chipWidth + 12 : 0)),
                layout.contentX(), layout.subtitleY(), ShellPalette.TEXT_2, false);
        if (modulesPage) drawChip(graphics);
    }

    private void drawChip(GuiGraphics graphics) {
        int width = chipWidth();
        int x = layout.contentRight() - width;
        int y = layout.subtitleY() - 4;
        JilatonUi.fillRounded(graphics, x, y, width, 16, 4,
                enabledOnly ? ShellPalette.accentSurface(accent) : ShellPalette.CONTROL);
        JilatonUi.strokeRounded(graphics, x, y, width, 16, 4,
                enabledOnly ? ShellPalette.accentBorder(accent) : ShellPalette.STROKE);
        JilatonUi.fillRounded(graphics, x + 6, y + 4, 8, 8, 2,
                enabledOnly ? accent : ShellPalette.STROKE_HOVER);
        graphics.drawString(font, "ENABLED ONLY", x + 18, y + 4,
                enabledOnly ? ShellPalette.TEXT : ShellPalette.TEXT_2, false);
    }

    private void renderModules(GuiGraphics graphics, int mouseX, int mouseY) {
        View view = view();
        int top = layout.listTop();
        int offset = (int) Math.round(scroll);
        int bottom = layout.listBottom();
        graphics.enableScissor(layout.contentX(), top, layout.contentRight(), bottom);
        // The viewport ends at listBottom, one hint strip above the panel floor: scrolled rows are
        // clipped there and never paint over the footer.
        if (view.modules().isEmpty()) {
            graphics.drawString(font, "No modules match. Clear the search or pick another category.",
                    layout.contentX() + 2, top + 16, JilatonUi.TEXT_3);
        }
        for (ModuleFlow.Card card : view.flow().cards()) {
            int y = top + card.y() - offset;
            if (y + card.height() < top || y > bottom) continue;
            renderCard(graphics, card, layout.contentX() + card.x(), y, mouseX, mouseY);
        }
        for (ModuleFlow.Panel panel : view.flow().panels()) {
            int y = top + panel.y() - offset;
            if (y + panel.height() < top || y > bottom) continue;
            renderSettingsPanel(graphics, panel, top, mouseX, mouseY);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, view.maxScroll(), top, bottom);
    }

    private void renderCard(GuiGraphics graphics, ModuleFlow.Card card, int x, int y, int mouseX, int mouseY) {
        if (card.module().category() == ModuleCategory.RENDER) {
            renderVisualCard(graphics, card, x, y, mouseX, mouseY);
        } else {
            renderModuleCard(graphics, card, x, y, mouseX, mouseY);
        }
    }

    /** 64 px module card: 13 px title, KeyBind line, gear and a 36x20 switch. Settings expand inline. */
    private void renderModuleCard(GuiGraphics graphics, ModuleFlow.Card card, int x, int y, int mouseX, int mouseY) {
        ClientModule module = card.module();
        int width = card.width();
        int height = card.height();
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        boolean active = module.id().equals(selectedId);
        boolean expanded = module.id().equals(expandedId);
        boolean binding = module.id().equals(bindingModuleId);
        double hover = animate("hover:" + module.id(), hovered ? 1 : 0);
        boolean gearPointed = inside(mouseX, mouseY, x + width - 14 - 36 - 20, y + 8, 20, 20);
        double gearHover = animate("gear:" + module.id(), gearPointed ? 1 : 0);

        int fill = active ? ShellPalette.accentSurface(accent)
                : JilatonUi.mix(ShellPalette.CARD, ShellPalette.CARD_HOVER, hover * 0.9);
        JilatonUi.fillRounded(graphics, x, y, width, height, 10, fill);
        JilatonUi.strokeRounded(graphics, x, y, width, height, 10,
                expanded || active ? ShellPalette.accentBorder(accent)
                        : hover > 0.02 ? ShellPalette.STROKE_HOVER : ShellPalette.STROKE);

        int toggleX = x + width - 14 - 36;
        // Grip and gear appear on hover (or while expanded) so the description gets the room at rest.
        double tools = Math.max(hover, expanded ? 1 : 0);
        int textWidth = width - 28 - 36 - 40;
        drawScaled(graphics, fit(module.name(), Math.round(textWidth / CARD_TITLE)), x + 14, y + 18,
                CARD_TITLE, ShellPalette.TEXT);
        // "Unbound · what it does": the live keybind first, then the module's own one-line description.
        String key = binding ? "Press a key" : module.keyCode() < 0 ? "Unbound" : keyLabel(module);
        graphics.drawString(font, key, x + 14, y + 38, binding ? accent : ShellPalette.TEXT_2, false);
        int restX = x + 14 + font.width(key);
        String rest = fit(" · " + module.description(), width - 28 - (tools > 0.05 ? 90 : 52) - font.width(key));
        if (!rest.isEmpty()) graphics.drawString(font, rest, restX, y + 38, ShellPalette.TEXT_3, false);
        int gearX = toggleX - 20;
        if (tools > 0.05) {
            ShellChrome.grip(graphics, gearX - 16, y + 12, JilatonUi.alpha(ShellPalette.TEXT_3, tools));
            ShellChrome.gear(graphics, gearX, y + 13, expanded ? accent
                    : JilatonUi.alpha(gearHover > 0.05 ? ShellPalette.TEXT_2 : ShellPalette.TEXT_3, tools));
        }
        drawToggle(graphics, toggleX, y + 22, module.enabled(), module.id());
    }

    /**
     * 90 px preview card. The card surface is the live miniature rendered from the module's own
     * settings; the icon tile, two-line copy, grip and counter sit on a scrim above it.
     */
    private void renderVisualCard(GuiGraphics graphics, ModuleFlow.Card card, int x, int y, int mouseX, int mouseY) {
        ClientModule module = card.module();
        int width = card.width();
        int height = card.height();
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        boolean active = module.id().equals(selectedId);
        boolean enabled = module.enabled();
        double hover = animate("hover:" + module.id(), hovered ? 1 : 0);
        preview.render(graphics, font, x, y, width, height, 8, module.id(), PreviewSpecs.of(module, accent),
                enabled, hover, config.reducedMotion(), frameNanos);
        graphics.fillGradient(x + 1, y + height - 46, x + width - 1, y + height - 1,
                0x00000000, JilatonUi.alpha(ShellPalette.CARD, 0.94));

        ShellChrome.iconTile(graphics, x + 12, y + 12, 32, accent);
        ModuleGlyph.draw(graphics, module.id(), x + 20, y + 20, 16,
                enabled ? accent : ShellPalette.TEXT_2);
        drawToggle(graphics, x + width - 14 - 36, y + 12, enabled, module.id());
        ShellChrome.grip(graphics, x + width - 26, y + 46,
                hover > 0.05 ? ShellPalette.TEXT_2 : ShellPalette.TEXT_3);

        int textRight = width - 24 - 60;
        drawScaled(graphics, fit(module.name(), Math.round(textRight / CARD_TITLE)), x + 12, y + height - 40,
                CARD_TITLE, ShellPalette.TEXT);
        String[] copy = twoLines(module.description(), textRight);
        graphics.drawString(font, copy[0], x + 12, y + height - 26, ShellPalette.TEXT_2, false);
        if (!copy[1].isEmpty()) {
            graphics.drawString(font, copy[1], x + 12, y + height - 16, ShellPalette.TEXT_2, false);
        }
        String metric = CardMetric.label(module);
        graphics.drawString(font, metric, x + width - 12 - font.width(metric), y + height - 26,
                CardMetric.on(module) > 0 ? accent : ShellPalette.TEXT_3, false);

        if (active) {
            JilatonUi.fillRounded(graphics, x, y, width, height, 8, JilatonUi.alpha(accent, 0.06));
            JilatonUi.strokeRounded(graphics, x, y, width, height, 8, accent);
        }
        if (module.id().equals(bindingModuleId)) {
            JilatonUi.strokeRounded(graphics, x, y, width, height, 8, JilatonUi.alpha(accent, 0.55));
        }
    }

    /** Splits a description into two word-broken lines that fit {@code width}. */
    private String[] twoLines(String text, int width) {
        return TextWrap.twoLines(text, width, font::width);
    }

    private void renderSettingsPanel(GuiGraphics graphics, ModuleFlow.Panel panel, int top, int mouseX, int mouseY) {
        int x = layout.contentX() + panel.x();
        int y = top + panel.y() - (int) Math.round(scroll);
        int width = panel.width();
        ClientModule module = panel.module();
        JilatonUi.fillRounded(graphics, x, y, width, panel.height(), 8, ShellPalette.EXPANSION);
        JilatonUi.strokeRounded(graphics, x, y, width, panel.height(), 8, ShellPalette.accentBorder(accent));
        graphics.fill(x + 10, y + EXPANSION_HEADER - 1, x + width - 10, y + EXPANSION_HEADER, ShellPalette.BORDER);
        String heading = fit(module.name().toUpperCase(Locale.ROOT), width - 130);
        graphics.drawString(font, heading, x + 14, y + 8, JilatonUi.TEXT_2);
        String count = module.settings().size() + " SETTINGS";
        graphics.drawString(font, count, x + 22 + font.width(heading), y + 8, JilatonUi.TEXT_4);
        ShellChrome.chevron(graphics, x + width - 22, y + 9, false, JilatonUi.TEXT_2);

        List<Setting> settings = module.settings();
        int offset = (int) Math.round(scroll);
        for (int index = 0; index < settings.size(); index++) {
            Setting setting = settings.get(index);
            int rowY = top + panel.settingsTop() - offset + index * SETTING_ROW;
            if (rowY + SETTING_ROW < top || rowY > layout.listBottom()) continue;
            if (index > 0) graphics.fill(x + 12, rowY, x + width - 12, rowY + 1, ShellPalette.BORDER);
            boolean rowHover = inside(mouseX, mouseY, x, rowY, width, SETTING_ROW);
            graphics.drawString(font, setting.label(), x + 14, rowY + 10,
                    rowHover ? JilatonUi.TEXT : JilatonUi.TEXT_2);
            renderSettingControl(graphics, setting, module, x + width - 14, rowY, mouseX, mouseY);
        }
    }

    private void renderSettingControl(GuiGraphics graphics, Setting setting, ClientModule module, int right, int y,
                                      int mouseX, int mouseY) {
        if (setting instanceof BooleanSetting booleanSetting) {
            ShellChrome.toggle(graphics, right - 36, y + 7,
                    animate("toggle:" + module.id() + "." + setting.id(), booleanSetting.value() ? 1 : 0), accent);
        } else if (setting instanceof ChoiceSetting choiceSetting) {
            int x = right - 100;
            int buttonY = y + 5;
            boolean hovered = inside(mouseX, mouseY, x, buttonY, 100, 20);
            JilatonUi.fillRounded(graphics, x, buttonY, 100, 20, 5,
                    hovered ? ShellPalette.accentSurface(accent) : ShellPalette.CONTROL);
            JilatonUi.strokeRounded(graphics, x, buttonY, 100, 20, 5,
                    hovered ? ShellPalette.accentBorder(accent) : ShellPalette.BORDER);
            graphics.drawString(font, "<", x + 6, buttonY + 6, JilatonUi.TEXT_3);
            String value = fit(choiceSetting.value(), 66);
            graphics.drawString(font, value, x + (100 - font.width(value)) / 2, buttonY + 6, JilatonUi.TEXT);
            graphics.drawString(font, ">", x + 89, buttonY + 6, JilatonUi.TEXT_3);
        } else if (setting instanceof TextSetting textSetting) {
            int x = right - 100;
            int buttonY = y + 5;
            boolean hovered = inside(mouseX, mouseY, x, buttonY, 100, 20);
            boolean empty = textSetting.value().isEmpty();
            JilatonUi.fillRounded(graphics, x, buttonY, 100, 20, 5,
                    hovered ? ShellPalette.accentSurface(accent) : ShellPalette.CONTROL);
            JilatonUi.strokeRounded(graphics, x, buttonY, 100, 20, 5,
                    hovered ? ShellPalette.accentBorder(accent) : ShellPalette.BORDER);
            graphics.drawString(font, empty ? "Edit..." : fit(textSetting.value(), 84),
                    x + 8, buttonY + 6, empty ? JilatonUi.TEXT_3 : JilatonUi.TEXT);
        } else if (setting instanceof NumberSetting numberSetting) {
            String value = formatNumber(numberSetting);
            graphics.drawString(font, value, right - font.width(value), y + 10, JilatonUi.TEXT_2);
            int trackRight = right - NUMBER_VALUE;
            int trackX = trackRight - NUMBER_TRACK;
            double range = numberSetting.maximum() - numberSetting.minimum();
            double fraction = range <= 0 ? 0 : (numberSetting.value() - numberSetting.minimum()) / range;
            int filled = (int) Math.round(NUMBER_TRACK
                    * animate("num:" + module.id() + "." + setting.id(), fraction));
            JilatonUi.fillRounded(graphics, trackX, y + 14, NUMBER_TRACK, 3, 1, ShellPalette.CONTROL);
            JilatonUi.fillRounded(graphics, trackX, y + 14, Math.max(2, filled), 3, 1, accent);
            graphics.fill(trackX + filled - 1, y + 11, trackX + filled + 2, y + 20, JilatonUi.TEXT);
        }
    }

    private void renderSettings(GuiGraphics graphics, int mouseX, int mouseY) {
        SettingsPage settings = settingsPage();
        drawFeatureCard(graphics, layout.contentX(), settings.hudY(), settings.hudWidth(), settings.hudHeight(),
                "settings", "HUD Editor", "Move and resize overlays", mouseX, mouseY);
        int studioX = settings.studioY() == settings.hudY()
                ? layout.contentX() + settings.hudWidth() + 10 : layout.contentX();
        drawFeatureCard(graphics, studioX, settings.studioY(), settings.studioWidth(), settings.studioHeight(),
                "visuals", "Visual Studio", "Preview skies, halos and wings", mouseX, mouseY);

        graphics.drawString(font, "APPEARANCE", layout.contentX(), settings.appearY(), JilatonUi.TEXT_4);
        graphics.drawString(font, "Reduced motion", layout.contentX(), settings.reducedY() + 4, JilatonUi.TEXT_2);
        drawToggle(graphics, layout.contentRight() - 36, settings.reducedY(), config.reducedMotion(), "config.reduced");
        graphics.drawString(font, "Animation intensity", layout.contentX(), settings.motionY() + 4,
                JilatonUi.TEXT_2);
        drawTrack(graphics, settings.motionX(), settings.motionY() + 4, settings.motionWidth(),
                animate("config.motion", config.animationIntensity() / 1.5), accent);
        String percent = Math.round(config.animationIntensity() * 100) + "%";
        graphics.drawString(font, percent, settings.motionX() + settings.motionWidth() + 8,
                settings.motionY() + 4, JilatonUi.TEXT_3);

        graphics.drawString(font, "KEYBOARD", layout.contentX(), settings.controlsY(), JilatonUi.TEXT_4);
        int keyY = settings.controlsY() + 13;
        int cursor = keycapRow(graphics, layout.contentX(), keyY, "RIGHT SHIFT", "open / close");
        cursor = keycapRow(graphics, cursor, keyY, "/jilaton gui", "open");
        keycapRow(graphics, cursor, keyY, "ESC", "close");
        graphics.drawString(font, "RIGHT-CLICK A CARD TO BIND ITS KEY", layout.contentX(), keyY + 22,
                JilatonUi.TEXT_4);
    }

    private void renderConfigs(GuiGraphics graphics, int mouseX, int mouseY) {
        int inputY = profileInputY;
        JilatonUi.fillRounded(graphics, layout.contentX(), inputY, profileInputWidth, 20, 5, ShellPalette.CONTROL);
        JilatonUi.strokeRounded(graphics, layout.contentX(), inputY, profileInputWidth, 20, 5,
                profileName.isFocused() ? accent : ShellPalette.BORDER);
        drawActionButton(graphics, "SAVE", profileSaveX, inputY, 50, accent, mouseX, mouseY);
        drawActionButton(graphics, "LOAD", profileLoadX, inputY, 50,
                selectedProfile == null ? ShellPalette.CONTROL : ShellPalette.SURFACE_HOVER, mouseX, mouseY);
        drawActionButton(graphics, "DELETE", profileDeleteX, inputY, 62,
                selectedProfile == null ? ShellPalette.CONTROL
                        : JilatonUi.mix(ShellPalette.CONTROL, JilatonUi.DANGER, 0.22), mouseX, mouseY);
        graphics.drawString(font, fit(profileStatus, layout.contentWidth()), layout.contentX(), inputY + 28,
                profileStatusError ? JilatonUi.DANGER : JilatonUi.TEXT_3);

        int rowsTop = inputY + 48;
        graphics.enableScissor(layout.contentX(), rowsTop, layout.contentRight(), layout.listBottom());
        if (profiles.isEmpty()) {
            graphics.drawString(font, "No profiles yet. Type a name and press SAVE.",
                    layout.contentX() + 2, rowsTop + 12, JilatonUi.TEXT_3);
        }
        int y = rowsTop - (int) Math.round(scroll);
        for (String profile : profiles) {
            if (y + 34 >= rowsTop && y <= layout.listBottom()) {
                boolean selected = profile.equals(selectedProfile);
                boolean hovered = inside(mouseX, mouseY, layout.contentX(), y, layout.contentWidth(), 34);
                JilatonUi.fillRounded(graphics, layout.contentX(), y, layout.contentWidth(), 34, 6,
                        selected ? ShellPalette.accentSurface(accent)
                                : hovered ? ShellPalette.SURFACE_HOVER : ShellPalette.SURFACE);
                JilatonUi.strokeRounded(graphics, layout.contentX(), y, layout.contentWidth(), 34, 6,
                        selected ? ShellPalette.accentBorder(accent) : ShellPalette.BORDER);
                graphics.drawString(font, fit(profile, layout.contentWidth() - 110), layout.contentX() + 12, y + 13,
                        selected ? JilatonUi.TEXT : JilatonUi.TEXT_2);
                String tag = selected ? "SELECTED" : "PROFILE";
                graphics.drawString(font, tag, layout.contentRight() - 12 - font.width(tag), y + 13,
                        selected ? accent : JilatonUi.TEXT_4);
            }
            y += 42;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, Math.max(0, profiles.size() * 42 - (layout.listHeight() - 48)),
                rowsTop, layout.listBottom());
    }

    private void renderTheme(GuiGraphics graphics, int mouseX, int mouseY) {
        ThemePage theme = themePage();
        ClientTheme[] themes = ClientTheme.values();
        for (int index = 0; index < themes.length; index++) {
            int x = layout.contentX() + (index % theme.columns()) * (theme.cardWidth() + 8);
            int y = theme.top() + (index / theme.columns()) * (theme.cardHeight() + 8);
            boolean selected = config.themeId().equals(themes[index].id());
            boolean hovered = inside(mouseX, mouseY, x, y, theme.cardWidth(), theme.cardHeight());
            JilatonUi.fillRounded(graphics, x, y, theme.cardWidth(), theme.cardHeight(), 7,
                    selected ? ShellPalette.accentSurface(accent)
                            : hovered ? ShellPalette.SURFACE_HOVER : ShellPalette.SURFACE);
            JilatonUi.strokeRounded(graphics, x, y, theme.cardWidth(), theme.cardHeight(), 7,
                    selected ? ShellPalette.accentBorder(accent) : ShellPalette.BORDER);
            graphics.drawString(font, fit(themes[index].label(), theme.cardWidth() - 46), x + 12, y + 10,
                    JilatonUi.TEXT);
            graphics.drawString(font, themes[index].id().toUpperCase(Locale.ROOT), x + 12, y + 24,
                    JilatonUi.TEXT_4);
            JilatonUi.fillRounded(graphics, x + theme.cardWidth() - 32, y + 12, 18, 22, 5, themes[index].accent());
            if (selected) {
                graphics.fill(x + 6, y + 10, x + 8, y + theme.cardHeight() - 10, themes[index].accent());
            }
        }

        int panelTop = theme.customTop();
        int panelHeight = theme.panelHeight();
        JilatonUi.fillRounded(graphics, layout.contentX(), panelTop, layout.contentWidth(), panelHeight, 8,
                ShellPalette.SURFACE);
        JilatonUi.strokeRounded(graphics, layout.contentX(), panelTop, layout.contentWidth(), panelHeight, 8,
                ShellPalette.BORDER);
        graphics.drawString(font, "CUSTOM ACCENT", layout.contentX() + 14, panelTop + 11, JilatonUi.TEXT);
        String hex = "#" + config.customAccent();
        JilatonUi.fillRounded(graphics, layout.contentRight() - 30, panelTop + 9, 16, 14, 4,
                parseAccent(config.customAccent()));
        graphics.drawString(font, hex, layout.contentRight() - 38 - font.width(hex), panelTop + 12,
                JilatonUi.TEXT_3);
        graphics.fill(layout.contentX() + 12, panelTop + 28, layout.contentRight() - 12, panelTop + 29,
                ShellPalette.BORDER);

        String[] labels = {"HUE", "SATURATION", "BRIGHTNESS", "MOTION"};
        float[] hsb = customHsb();
        for (int channel = 0; channel < labels.length; channel++) {
            int y = panelTop + 40 + channel * theme.rowStep();
            graphics.drawString(font, labels[channel], layout.contentX() + 14, y - 3, JilatonUi.TEXT_2);
            if (channel == 3) {
                drawTrack(graphics, theme.sliderX(), y, theme.sliderWidth(),
                        animate("theme.motion", config.animationIntensity() / 1.5), accent);
            } else {
                drawGradient(graphics, theme.sliderX(), y, theme.sliderWidth(), channel, hsb);
            }
            String value = channel == 3 ? Math.round(config.animationIntensity() * 100) + "%"
                    : Math.round(hsb[channel] * 100) + "%";
            graphics.drawString(font, value, theme.sliderX() + theme.sliderWidth() + 8, y - 3, JilatonUi.TEXT_3);
        }
        int reducedY = panelTop + 40 + 4 * theme.rowStep() + 2;
        graphics.drawString(font, "REDUCED MOTION", layout.contentX() + 14, reducedY + 4, JilatonUi.TEXT_2);
        drawToggle(graphics, layout.contentRight() - 36, reducedY, config.reducedMotion(), "theme.reduced");
    }

    private void drawFeatureCard(GuiGraphics graphics, int x, int y, int width, int height, String icon,
                                 String title, String detail, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        double amount = animate("feature:" + title, hovered ? 1 : 0);
        JilatonUi.fillRounded(graphics, x, y, width, height, 8,
                JilatonUi.mix(ShellPalette.SURFACE, ShellPalette.SURFACE_HOVER, amount));
        JilatonUi.strokeRounded(graphics, x, y, width, height, 8,
                hovered ? ShellPalette.accentBorder(accent) : ShellPalette.BORDER);
        int iconY = y + height / 2 - 13;
        JilatonUi.fillRounded(graphics, x + 12, iconY, 26, 26, 6, ShellPalette.CONTROL);
        ShellChrome.icon(graphics, icon, x + 20, iconY + 8, hovered ? accent : JilatonUi.TEXT_2);
        graphics.drawString(font, title, x + 46, y + height / 2 - 11, JilatonUi.TEXT);
        graphics.drawString(font, fit(detail, width - 46 - 46), x + 46, y + height / 2 + 1, JilatonUi.TEXT_3);
        String open = "OPEN";
        graphics.drawString(font, open, x + width - 12 - font.width(open), y + height / 2 - 4,
                hovered ? accent : JilatonUi.TEXT_4);
    }

    private void drawScrollbar(GuiGraphics graphics, int limit, int top, int bottom) {
        if (limit <= 0) return;
        int view = bottom - top;
        if (view <= 0) return;
        int content = view + limit;
        int thumb = Math.max(24, (int) ((long) view * view / content));
        int travel = view - thumb;
        int y = top + (travel <= 0 ? 0 : (int) Math.round(travel * (scroll / limit)));
        int x = layout.contentRight() - 3;
        JilatonUi.fillRounded(graphics, x, y, 3, thumb, 1, JilatonUi.alpha(accent, 0.6));
    }

    private void drawHint(GuiGraphics graphics) {
        if (layout.listHeight() < 90) return;
        String hint;
        if (bindingModuleId != null) {
            hint = "Press a key  ·  Delete unbinds  ·  Esc cancels";
        } else if (page == Page.MODULES) {
            hint = "Click to toggle  ·  Right-click for settings  ·  Drag to reorder  ·  Esc to close";
        } else {
            hint = "Esc to close  ·  / to search modules";
        }
        String shown = fit(hint, layout.contentWidth());
        int hintX = layout.contentX() + Math.max(0, (layout.contentWidth() - font.width(shown)) / 2);
        graphics.drawString(font, shown, hintX, layout.hintY(),
                bindingModuleId != null ? accent : ShellPalette.TEXT_3);
    }

    private void drawToggle(GuiGraphics graphics, int x, int y, boolean enabled, String key) {
        ShellChrome.toggle(graphics, x, y, animate("toggle:" + key, enabled ? 1 : 0), accent);
    }

    private void drawTrack(GuiGraphics graphics, int x, int y, int width, double fraction, int color) {
        JilatonUi.fillRounded(graphics, x, y, width, 4, 2, ShellPalette.CONTROL);
        int filled = (int) Math.round(width * Motion.clamp01(fraction));
        if (filled > 0) JilatonUi.fillRounded(graphics, x, y, Math.max(2, filled), 4, 2, color);
        graphics.fill(x + filled - 1, y - 3, x + filled + 2, y + 7, JilatonUi.TEXT);
    }

    private void drawGradient(GuiGraphics graphics, int x, int y, int width, int channel, float[] hsb) {
        int segments = Math.max(12, width / 6);
        float[] sample = hsb.clone();
        for (int index = 0; index < segments; index++) {
            float value = segments == 1 ? 0 : index / (float) (segments - 1);
            sample[channel] = value;
            int left = x + width * index / segments;
            int right = x + width * (index + 1) / segments;
            graphics.fill(left, y, Math.max(left + 1, right), y + 4, hsbColor(sample));
        }
        int knob = x + Math.round(width * hsb[channel]);
        graphics.fill(knob - 1, y - 3, knob + 2, y + 7, JilatonUi.TEXT);
    }

    private void drawActionButton(GuiGraphics graphics, String label, int x, int y, int width, int color,
                                  int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, 20);
        JilatonUi.fillRounded(graphics, x, y, width, 20, 5,
                hovered ? JilatonUi.mix(color, JilatonUi.TEXT, 0.08) : color);
        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + 6, JilatonUi.onColor(color));
    }

    private int keycapRow(GuiGraphics graphics, int x, int y, String key, String action) {
        int width = ShellChrome.keycapWidth(font, key);
        int actionWidth = font.width(action);
        if (x + width + 6 + actionWidth > layout.contentRight()) return x;
        ShellChrome.keycap(graphics, font, key, x, y, accent, false);
        graphics.drawString(font, action, x + width + 6, y + 3, JilatonUi.TEXT_4);
        return x + width + 12 + actionWidth + 14;
    }

    private void drawScaled(GuiGraphics graphics, String text, int x, int y, float scale, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    private double animate(String key, double target) {
        return motion.computeIfAbsent(key, ignored -> new AnimatedValue())
                .update(target, frameNanos, config.reducedMotion());
    }

    private View view() {
        String query = query();
        boolean visual = selectedCategory.equals("Visuals");
        List<ClientModule> visible = new ArrayList<>();
        for (ClientModule module : allModules) {
            if (matches(module, query) && categoryMatches(module) && (!enabledOnly || module.enabled())) {
                visible.add(module);
            }
        }
        List<String> activeOrder = visual && !visualOrder.isEmpty() ? visualOrder : moduleOrder;
        if (!activeOrder.isEmpty()) {
            List<String> ids = new ArrayList<>();
            for (ClientModule module : visible) ids.add(module.id());
            List<String> ordered = ModuleOrder.apply(activeOrder, ids);
            List<ClientModule> sorted = new ArrayList<>();
            for (String id : ordered) {
                for (ClientModule module : visible) {
                    if (module.id().equals(id)) {
                        sorted.add(module);
                        break;
                    }
                }
            }
            visible = sorted;
        }
        int cardHeight = visual ? VISUAL_HEIGHT : ROW_HEIGHT;
        int columns = visual ? ShellMetrics.visualColumns(layout.contentWidth())
                : ShellMetrics.moduleColumns(layout.contentWidth());
        int cardWidth = ShellMetrics.cardWidth(layout.contentWidth(), columns);
        ModuleFlow flow = ModuleFlow.layout(visible, expandedId, columns, cardWidth, cardHeight,
                layout.contentWidth(), COLUMN_GAP, ROW_GAP, EXPANSION_HEADER, SETTING_ROW);
        int maxScroll = Math.max(0, flow.height() - layout.listHeight());
        return new View(visible, flow, cardHeight, maxScroll);
    }

    private Counts counts() {
        int[] enabled = new int[CATEGORIES.length];
        int[] total = new int[CATEGORIES.length];
        for (ClientModule module : allModules) {
            total[0]++;
            if (module.enabled()) enabled[0]++;
            int slot = slot(module.category());
            total[slot]++;
            if (module.enabled()) enabled[slot]++;
        }
        return new Counts(enabled, total);
    }

    private SettingsPage settingsPage() {
        int top = layout.listTop();
        boolean wide = layout.contentWidth() >= 420;
        int gap = 10;
        int cardHeight = wide ? 80 : 56;
        int cardWidth = wide ? (layout.contentWidth() - gap) / 2 : layout.contentWidth();
        int studioY = wide ? top : top + cardHeight + gap;
        int appearY = studioY + cardHeight + 22;
        int reducedY = appearY + 20;
        int motionY = reducedY + 30;
        int controlsY = motionY + 30;
        int height = controlsY + 22 - top;
        int motionX = layout.contentX() + Math.min(160, Math.max(96, layout.contentWidth() / 3));
        int motionWidth = Math.max(70, layout.contentRight() - 54 - motionX);
        return new SettingsPage(top, cardWidth, cardHeight, studioY, cardWidth, cardHeight,
                appearY, reducedY, motionY, motionX, motionWidth, controlsY, height);
    }

    private ThemePage themePage() {
        int top = layout.listTop();
        int columns = layout.contentWidth() >= 600 ? 4 : layout.contentWidth() >= 400 ? 3 : 2;
        int cardWidth = (layout.contentWidth() - 8 * (columns - 1)) / columns;
        int rows = (ClientTheme.values().length + columns - 1) / columns;
        int cardHeight = 46;
        int customTop = top + rows * (cardHeight + 8) + 14;
        int sliderX = layout.contentX() + Math.min(104, Math.max(72, layout.contentWidth() / 4));
        int sliderWidth = Math.max(60, layout.contentRight() - 46 - sliderX);
        int rowStep = layout.compact() ? 21 : 26;
        int panelHeight = 70 + 4 * rowStep;
        return new ThemePage(top, columns, cardWidth, cardHeight, customTop, sliderX, sliderWidth,
                rowStep, panelHeight, customTop + panelHeight - top);
    }

    private int scrollLimit() {
        int height = layout.listHeight();
        return switch (page) {
            case MODULES -> Math.max(0, view().flow().height() - height);
            case CONFIGS -> Math.max(0, profiles.size() * 42 - (height - 48));
            case SETTINGS, THEME -> 0;
        };
    }

    private String query() {
        return search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private boolean categoryMatches(ClientModule module) {
        return selectedCategory.equals("All") || selectedCategory.equals(module.category().displayName());
    }

    private static boolean matches(ClientModule module, String query) {
        if (query.isEmpty()) return true;
        return module.name().toLowerCase(Locale.ROOT).contains(query)
                || module.description().toLowerCase(Locale.ROOT).contains(query)
                || module.category().displayName().toLowerCase(Locale.ROOT).contains(query);
    }

    private static int slot(ModuleCategory category) {
        return switch (category) {
            case COMBAT -> 1;
            case MOVEMENT -> 2;
            case PLAYER -> 3;
            case RENDER -> 4;
            case CLIENT -> 5;
        };
    }

    private int selectedAccent() {
        return accentFor(config.themeId(), config.customAccent());
    }

    /** Seam for tests: the shell must paint exactly the accent the theme page reports, for every theme. */
    static int accentFor(String themeId, String customAccent) {
        return ClientTheme.resolveAccent(themeId, customAccent);
    }

    private ClientModule selectedModule() {
        if (selectedId == null) return null;
        for (ClientModule module : view().modules()) {
            if (module.id().equals(selectedId)) return module;
        }
        return null;
    }

    private void moveSelection(int delta) {
        List<ClientModule> visible = view().modules();
        if (visible.isEmpty()) {
            selectedId = null;
            return;
        }
        int index = -1;
        for (int position = 0; position < visible.size(); position++) {
            if (visible.get(position).id().equals(selectedId)) {
                index = position;
                break;
            }
        }
        if (index < 0) {
            index = delta > 0 ? 0 : visible.size() - 1;
        } else {
            index = Math.floorMod(index + delta, visible.size());
        }
        selectedId = visible.get(index).id();
        revealSelection(visible.get(index));
    }

    private void revealSelection(ClientModule module) {
        ModuleFlow.Card card = view().flow().cardOf(module);
        if (card == null) return;
        int height = layout.listHeight();
        if (card.y() < targetScroll) {
            targetScroll = card.y();
        } else if (card.y() + card.height() > targetScroll + height) {
            targetScroll = card.y() + card.height() - height;
        }
        targetScroll = Math.max(0, Math.min(scrollLimit(), targetScroll));
    }

    private int chipWidth() {
        return font.width("ENABLED ONLY") + 28;
    }

    private boolean chipHit(double mouseX, double mouseY) {
        int width = chipWidth();
        return inside(mouseX, mouseY, layout.contentRight() - width, layout.subtitleY() - 4, width, 16);
    }

    private void openPage(Page next) {
        page = next;
        if (profileName != null) profileName.setVisible(next == Page.CONFIGS);
        bindingModuleId = null;
        drag = DragKind.NONE;
        scroll = 0;
        targetScroll = 0;
        revealTransition.set(0);
        if (next == Page.MODULES) {
            setFocused(search);
        } else if (next == Page.CONFIGS && profileName != null) {
            setFocused(profileName);
        } else {
            setFocused(null);
        }
        if (next == Page.CONFIGS) refreshProfiles();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing) return true;
        if (super.mouseClicked(event, doubleClick)) return true;
        double mouseX = event.x();
        double mouseY = event.y();
        if (bindingModuleId != null) {
            bindingModuleId = null;
            return true;
        }
        if (inside(mouseX, mouseY, layout.searchX(), layout.searchY(), layout.searchWidth(), layout.searchHeight())) {
            setFocused(search);
            return true;
        }
        if (page == Page.CONFIGS
                && inside(mouseX, mouseY, layout.contentX(), profileInputY, profileInputWidth, 20)) {
            setFocused(profileName);
            return true;
        }
        if (clickSidebar(mouseX, mouseY)) return true;
        if (page == Page.MODULES && chipHit(mouseX, mouseY)) {
            enabledOnly = !enabledOnly;
            scroll = 0;
            targetScroll = 0;
            selectedId = null;
            return true;
        }
        return switch (page) {
            case MODULES -> clickModules(mouseX, mouseY, event.button());
            case SETTINGS -> clickSettings(mouseX, mouseY);
            case CONFIGS -> clickConfigs(mouseX, mouseY);
            case THEME -> clickTheme(mouseX, mouseY);
        };
    }

    private boolean clickSidebar(double mouseX, double mouseY) {
        int x = layout.panelX() + 10;
        int width = layout.sidebarWidth() - 20;
        int rowHeight = layout.rowHeight();
        for (int index = 0; index < CATEGORIES.length; index++) {
            if (inside(mouseX, mouseY, x, layout.navY(index), width, rowHeight)) {
                boolean same = page == Page.MODULES && selectedCategory.equals(CATEGORIES[index]);
                if (page == Page.MODULES) {
                    config.setCategoryScroll(selectedCategory, targetScroll);
                }
                openPage(Page.MODULES);
                selectedCategory = CATEGORIES[index];
                double savedScroll = config.categoryScroll(selectedCategory);
                scroll = targetScroll = savedScroll;
                if (!same) {
                    ClientModule sel = selectedId != null ? modules.get(selectedId) : null;
                    if (sel != null && !categoryMatches(sel)) {
                        selectedId = null;
                    }
                    ClientModule exp = expandedId != null ? modules.get(expandedId) : null;
                    if (exp != null && !categoryMatches(exp)) {
                        expandedId = null;
                    }
                }
                syncStateToConfig();
                return true;
            }
        }
        for (int index = 0; index < GENERAL.length; index++) {
            if (inside(mouseX, mouseY, x, layout.generalY(index), width, rowHeight)) {
                if (page == Page.MODULES) {
                    config.setCategoryScroll(selectedCategory, targetScroll);
                }
                openPage(index == 0 ? Page.SETTINGS : index == 1 ? Page.CONFIGS : Page.THEME);
                syncStateToConfig();
                return true;
            }
        }
        return false;
    }

    private boolean clickModules(double mouseX, double mouseY, int button) {
        int top = layout.listTop();
        if (mouseY < top || mouseY >= layout.listBottom()
                || mouseX < layout.contentX() || mouseX >= layout.contentRight()) {
            return false;
        }
        View view = view();
        double contentX = mouseX - layout.contentX();
        double contentY = mouseY - top + scroll;
        ModuleFlow.Panel panel = view.flow().panelAt(contentX, contentY);
        if (panel != null) return clickPanel(panel, mouseX, mouseY, top, button);
        ModuleFlow.Card card = view.flow().cardAt(contentX, contentY);
        if (card == null) {
            selectedId = null;
            return true;
        }
        return clickCard(card, mouseX, mouseY, top, button);
    }

    private boolean clickPanel(ModuleFlow.Panel panel, double mouseX, double mouseY, int top, int button) {
        int x = layout.contentX() + panel.x();
        int width = panel.width();
        int offset = (int) Math.round(scroll);
        int panelTop = top + panel.y() - offset;
        if (mouseY < panelTop + EXPANSION_HEADER) {
            expandedId = null;
            return true;
        }
        if (button != 0) return true;
        ClientModule module = panel.module();
        List<Setting> settings = module.settings();
        int index = (int) Math.floor((mouseY - (top + panel.settingsTop() - offset)) / (double) SETTING_ROW);
        if (index < 0 || index >= settings.size()) return true;
        Setting setting = settings.get(index);
        int right = x + width - 14;
        if (setting instanceof BooleanSetting booleanSetting) {
            if (mouseX >= right - 40 && mouseX <= right) {
                booleanSetting.toggle();
                config.save(modules);
            }
        } else if (setting instanceof ChoiceSetting choiceSetting) {
            int pillX = right - 100;
            if (mouseX >= pillX && mouseX <= right) {
                choiceSetting.cycle(mouseX < pillX + 50 ? -1 : 1);
                config.save(modules);
            }
        } else if (setting instanceof TextSetting textSetting) {
            if (mouseX >= right - 100) {
                minecraft.setScreen(new TextSettingScreen(this, textSetting, modules, config));
                return true;
            }
        } else if (setting instanceof NumberSetting numberSetting) {
            int trackRight = right - NUMBER_VALUE;
            int trackX = trackRight - NUMBER_TRACK;
            if (mouseX >= trackX - 4 && mouseX <= trackRight + 4) {
                drag = DragKind.MODULE_NUMBER;
                dragModule = module;
                dragSetting = numberSetting;
                dragTrackX = trackX;
                dragTrackWidth = NUMBER_TRACK;
                applyModuleSlider(numberSetting, mouseX);
            }
        }
        return true;
    }

    private boolean clickCard(ModuleFlow.Card card, double mouseX, double mouseY, int top, int button) {
        ClientModule module = card.module();
        int x = layout.contentX() + card.x();
        int y = top + card.y() - (int) Math.round(scroll);
        int width = card.width();
        boolean visual = module.category() == ModuleCategory.RENDER;
        int toggleX = x + width - 14 - 36;
        if (button == 1) {
            bindingModuleId = module.id();
            setFocused(null);
            return true;
        }
        if (button != 0) return true;
        if (inside(mouseX, mouseY, toggleX, y + 22, 36, 20)) {
            module.toggle();
            config.save(modules);
            return true;
        }
        if (visual) {
            if (inside(mouseX, mouseY, x + width - 32, y + 40, 22, 20)) {
                drag = DragKind.REORDER;
                dragModule = module;
                return true;
            }
            selectedId = module.id();
            expandedId = module.id().equals(expandedId) ? null : module.id();
            syncStateToConfig();
            return true;
        }
        int gearX = toggleX - 20;
        if (inside(mouseX, mouseY, gearX - 20, y + 22, 18, 20)) {
            drag = DragKind.REORDER;
            dragModule = module;
            return true;
        }
        if (inside(mouseX, mouseY, gearX, y + 8, 20, 20)) {
            expandedId = module.id().equals(expandedId) ? null : module.id();
            syncStateToConfig();
            return true;
        }
        selectedId = module.id();
        expandedId = module.id().equals(expandedId) ? null : module.id();
        syncStateToConfig();
        return true;
    }

    /** Visuals card index under the pointer, for grip reordering. */
    private int reorderTarget(double mouseX, double mouseY) {
        View view = view();
        double contentX = mouseX - layout.contentX();
        double contentY = mouseY - layout.listTop() + scroll;
        ModuleFlow.Card over = view.flow().cardAt(contentX, contentY);
        List<ClientModule> cards = view.modules();
        if (over == null || cards.isEmpty()) return Math.max(0, cards.size() - 1);
        return cards.indexOf(over.module());
    }

    private boolean clickSettings(double mouseX, double mouseY) {
        SettingsPage settings = settingsPage();
        int studioX = settings.studioY() == settings.hudY()
                ? layout.contentX() + settings.hudWidth() + 10 : layout.contentX();
        if (inside(mouseX, mouseY, layout.contentX(), settings.hudY(),
                settings.hudWidth(), settings.hudHeight())) {
            minecraft.setScreen(new HudEditorScreen(modules, config, this));
            return true;
        }
        if (inside(mouseX, mouseY, studioX, settings.studioY(),
                settings.studioWidth(), settings.studioHeight())) {
            config.save(modules);
            minecraft.setScreen(new VisualStudioScreen(this));
            return true;
        }
        if (inside(mouseX, mouseY, layout.contentRight() - 40, settings.reducedY() - 2, 44, 22)) {
            config.setReducedMotion(!config.reducedMotion());
            config.save(modules);
            return true;
        }
        if (inside(mouseX, mouseY, settings.motionX() - 4, settings.motionY() - 4,
                settings.motionWidth() + 8, 16)) {
            drag = DragKind.MOTION;
            dragTrackX = settings.motionX();
            dragTrackWidth = settings.motionWidth();
            applyMotion(mouseX);
            return true;
        }
        return false;
    }

    private boolean clickConfigs(double mouseX, double mouseY) {
        int inputY = profileInputY;
        if (inside(mouseX, mouseY, profileSaveX, inputY, 50, 20)) {
            saveProfile();
            return true;
        }
        if (inside(mouseX, mouseY, profileLoadX, inputY, 50, 20)) {
            loadProfile();
            return true;
        }
        if (inside(mouseX, mouseY, profileDeleteX, inputY, 62, 20)) {
            deleteProfile();
            return true;
        }
        int rowsTop = inputY + 48;
        if (mouseY < rowsTop || mouseY > layout.listBottom()) return false;
        int y = rowsTop - (int) Math.round(scroll);
        for (String profile : profiles) {
            if (mouseY >= y && mouseY < y + 34
                    && mouseX >= layout.contentX() && mouseX <= layout.contentRight()) {
                selectedProfile = profile;
                profileName.setValue(profile);
                return true;
            }
            y += 42;
        }
        return false;
    }

    private boolean clickTheme(double mouseX, double mouseY) {
        ThemePage theme = themePage();
        ClientTheme[] themes = ClientTheme.values();
        for (int index = 0; index < themes.length; index++) {
            int x = layout.contentX() + (index % theme.columns()) * (theme.cardWidth() + 8);
            int y = theme.top() + (index / theme.columns()) * (theme.cardHeight() + 8);
            if (inside(mouseX, mouseY, x, y, theme.cardWidth(), theme.cardHeight())) {
                config.setThemeId(themes[index].id());
                accent = selectedAccent();
                config.save(modules);
                return true;
            }
        }
        int panelTop = theme.customTop();
        if (mouseX < layout.contentX() || mouseX > layout.contentRight()
                || mouseY < panelTop || mouseY > panelTop + theme.panelHeight()) {
            return false;
        }
        if (mouseY <= panelTop + 30) return true;
        for (int channel = 0; channel < 4; channel++) {
            if (inside(mouseX, mouseY, theme.sliderX() - 4,
                    panelTop + 40 + channel * theme.rowStep() - 6, theme.sliderWidth() + 8, 14)) {
                dragTrackX = theme.sliderX();
                dragTrackWidth = theme.sliderWidth();
                if (channel == 3) {
                    drag = DragKind.MOTION;
                    applyMotion(mouseX);
                } else {
                    drag = DragKind.THEME_HSB;
                    dragChannel = channel;
                    applyHsb(channel, mouseX);
                }
                return true;
            }
        }
        int reducedY = panelTop + 40 + 4 * theme.rowStep() + 2;
        if (inside(mouseX, mouseY, layout.contentRight() - 40, reducedY - 2, 44, 22)) {
            config.setReducedMotion(!config.reducedMotion());
            config.save(modules);
            return true;
        }
        return true;
    }

    private void applyModuleSlider(NumberSetting setting, double mouseX) {
        double fraction = Math.max(0, Math.min(1,
                (mouseX - dragTrackX) / (double) Math.max(1, dragTrackWidth)));
        setting.setValue(setting.minimum() + fraction * (setting.maximum() - setting.minimum()));
    }

    private void applyMotion(double mouseX) {
        double fraction = Math.max(0, Math.min(1,
                (mouseX - dragTrackX) / (double) Math.max(1, dragTrackWidth)));
        config.setAnimationIntensity(fraction * 1.5);
    }

    private void applyHsb(int channel, double mouseX) {
        double fraction = Math.max(0, Math.min(1,
                (mouseX - dragTrackX) / (double) Math.max(1, dragTrackWidth)));
        float[] hsb = customHsb();
        hsb[channel] = (float) fraction;
        config.setCustomAccent(String.format(Locale.ROOT, "%06X", hsbColor(hsb) & 0xFFFFFF));
        config.setThemeId("custom");
        accent = selectedAccent();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0) {
            if (drag == DragKind.MODULE_NUMBER && dragSetting instanceof NumberSetting numberSetting) {
                applyModuleSlider(numberSetting, event.x());
                return true;
            }
            if (drag == DragKind.THEME_HSB) {
                applyHsb(dragChannel, event.x());
                return true;
            }
            if (drag == DragKind.MOTION) {
                applyMotion(event.x());
                return true;
            }
            if (drag == DragKind.REORDER) {
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (drag == DragKind.REORDER && dragModule != null) {
            List<String> current = new ArrayList<>();
            for (ClientModule module : view().modules()) current.add(module.id());
            boolean visual = selectedCategory.equals("Visuals");
            List<String> activeOrder = visual && !visualOrder.isEmpty() ? visualOrder : moduleOrder;
            List<String> base = activeOrder.isEmpty() ? current : ModuleOrder.apply(activeOrder, current);
            int target = reorderTarget(event.x(), event.y());
            String moved = dragModule.id();
            List<String> next = ModuleOrder.move(base, moved, target);
            List<String> allIds = new ArrayList<>();
            for (ClientModule module : allModules) allIds.add(module.id());
            List<String> mergedFull = ModuleOrder.apply(next, ModuleOrder.apply(moduleOrder, allIds));
            moduleOrder.clear();
            moduleOrder.addAll(mergedFull);
            config.setModuleOrder(moduleOrder);
            if (visual || dragModule.category() == ModuleCategory.RENDER) {
                List<String> renderIds = new ArrayList<>();
                for (ClientModule module : allModules) {
                    if (module.category() == ModuleCategory.RENDER) renderIds.add(module.id());
                }
                List<String> nextVisual = ModuleOrder.apply(next, ModuleOrder.apply(visualOrder, renderIds));
                visualOrder.clear();
                visualOrder.addAll(nextVisual);
                config.setVisualOrder(visualOrder);
            }
            syncStateToConfig();
            config.save(modules);
            drag = DragKind.NONE;
            dragModule = null;
            return true;
        }
        if (drag != DragKind.NONE) {
            drag = DragKind.NONE;
            dragChannel = -1;
            dragModule = null;
            dragSetting = null;
            syncStateToConfig();
            config.save(modules);
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= layout.contentX() && mouseX <= layout.contentRight()
                && mouseY >= layout.listTop() && mouseY <= layout.listBottom()) {
            targetScroll = Math.max(0, Math.min(scrollLimit(), targetScroll - verticalAmount * 34));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (closing) return true;
        int key = event.key();
        if (bindingModuleId != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                bindingModuleId = null;
                return true;
            }
            ClientModule module = modules.get(bindingModuleId);
            if (module != null) {
                module.setKeyCode(key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE ? -1 : key);
                config.save(modules);
            }
            bindingModuleId = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (page != Page.MODULES) {
                onClose();
                return true;
            }
            if (!search.getValue().isEmpty()) {
                search.setValue("");
                return true;
            }
            if (search.isFocused()) {
                setFocused(null);
                return true;
            }
            onClose();
            return true;
        }
        if (event.hasControlDown() && key == GLFW.GLFW_KEY_F) {
            setFocused(search);
            return true;
        }
        if (key == GLFW.GLFW_KEY_SLASH && !search.isFocused()) {
            setFocused(search);
            return true;
        }
        if (page == Page.MODULES) {
            if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
                moveSelection(key == GLFW.GLFW_KEY_UP ? -1 : 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                ClientModule module = selectedModule();
                if (module == null) {
                    moveSelection(1);
                } else {
                    module.toggle();
                    config.save(modules);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_RIGHT && !search.isFocused()) {
                if (selectedModule() != null) expandedId = selectedId;
                return true;
            }
            if (key == GLFW.GLFW_KEY_LEFT && !search.isFocused()) {
                expandedId = null;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (closing) return;
        if (config.reducedMotion()) {
            finishClose();
            return;
        }
        closing = true;
        bindingModuleId = null;
        drag = DragKind.NONE;
        setFocused(null);
    }

    private void finishClose() {
        syncStateToConfig();
        config.save(modules);
        if (parent != null) {
            minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    private void refreshProfiles() {
        try {
            profiles = config.listProfiles();
            if (selectedProfile != null && !profiles.contains(selectedProfile)) {
                selectedProfile = null;
            }
        } catch (IOException exception) {
            profiles = List.of();
            profileStatus = "Could not read profiles: " + exception.getMessage();
            profileStatusError = true;
        }
    }

    private void saveProfile() {
        String name = profileName.getValue().trim();
        try {
            config.saveProfile(name, modules);
            selectedProfile = name;
            profileStatus = "Saved profile \"" + name + "\".";
            profileStatusError = false;
            refreshProfiles();
        } catch (IllegalArgumentException | IOException exception) {
            profileStatus = exception instanceof IllegalArgumentException
                    ? "Use 1-32 letters, numbers, underscores, or hyphens."
                    : "Could not save profile: " + exception.getMessage();
            profileStatusError = true;
        }
    }

    private void loadProfile() {
        if (selectedProfile == null) return;
        try {
            if (config.loadProfile(selectedProfile, modules)) {
                config.save(modules);
                profileStatus = "Loaded profile \"" + selectedProfile + "\".";
                profileStatusError = false;
            } else {
                profileStatus = "That profile no longer exists.";
                profileStatusError = true;
                refreshProfiles();
            }
        } catch (IOException exception) {
            profileStatus = "Could not load profile: " + exception.getMessage();
            profileStatusError = true;
        }
    }

    private void deleteProfile() {
        if (selectedProfile == null) return;
        try {
            if (config.deleteProfile(selectedProfile)) {
                profileStatus = "Deleted profile \"" + selectedProfile + "\".";
                profileStatusError = false;
                selectedProfile = null;
                refreshProfiles();
            } else {
                profileStatus = "That profile no longer exists.";
                profileStatusError = true;
                refreshProfiles();
            }
        } catch (IOException exception) {
            profileStatus = "Could not delete profile: " + exception.getMessage();
            profileStatusError = true;
        }
    }

    private void updateClock() {
        LocalTime now = LocalTime.now();
        int minute = now.getHour() * 60 + now.getMinute();
        if (minute == clockMinute) return;
        clockMinute = minute;
        clockText = String.format(Locale.ROOT, "%02d:%02d", now.getHour(), now.getMinute());
    }

    private String playerName() {
        if (minecraft != null && minecraft.getUser() != null) {
            String name = minecraft.getUser().getName();
            if (name != null && !name.isBlank()) return name;
        }
        return "there";
    }

    private String keyLabel(ClientModule module) {
        if (module.id().equals(bindingModuleId)) return "PRESS";
        String label = keyName(module.keyCode());
        return label.length() > 8 ? label.substring(0, 7) + "." : label;
    }

    private static String keyName(int keyCode) {
        if (keyCode < 0) return "UNBOUND";
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString().toUpperCase(Locale.ROOT);
    }

    private static String formatNumber(NumberSetting setting) {
        double span = setting.maximum() - setting.minimum();
        return span >= 20 ? String.format(Locale.ROOT, "%.0f", setting.value())
                : String.format(Locale.ROOT, "%.1f", setting.value());
    }

    private String fit(String text, int width) {
        if (text == null || width <= 0) return "";
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int parseAccent(String value) {
        try {
            return 0xFF000000 | Integer.parseInt(value, 16);
        } catch (RuntimeException exception) {
            return ShellPalette.ACCENT;
        }
    }

    private float[] customHsb() {
        int color = parseAccent(config.customAccent());
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float delta = max - min;
        float hue;
        if (delta == 0.0F) {
            hue = 0.0F;
        } else if (max == red) {
            hue = ((green - blue) / delta) % 6.0F / 6.0F;
        } else if (max == green) {
            hue = ((blue - red) / delta + 2.0F) / 6.0F;
        } else {
            hue = ((red - green) / delta + 4.0F) / 6.0F;
        }
        if (hue < 0.0F) hue += 1.0F;
        float saturation = max == 0.0F ? 0.0F : delta / max;
        return new float[]{hue, saturation, max};
    }

    private static int hsbColor(float[] hsb) {
        float hue = (hsb[0] - (float) Math.floor(hsb[0])) * 6.0F;
        int sector = (int) hue;
        float fraction = hue - sector;
        float p = hsb[2] * (1.0F - hsb[1]);
        float q = hsb[2] * (1.0F - fraction * hsb[1]);
        float t = hsb[2] * (1.0F - (1.0F - fraction) * hsb[1]);
        float red;
        float green;
        float blue;
        switch (sector % 6) {
            case 0 -> {
                red = hsb[2];
                green = t;
                blue = p;
            }
            case 1 -> {
                red = q;
                green = hsb[2];
                blue = p;
            }
            case 2 -> {
                red = p;
                green = hsb[2];
                blue = t;
            }
            case 3 -> {
                red = p;
                green = q;
                blue = hsb[2];
            }
            case 4 -> {
                red = t;
                green = p;
                blue = hsb[2];
            }
            default -> {
                red = hsb[2];
                green = p;
                blue = q;
            }
        }
        return 0xFF000000 | (Math.round(red * 255.0F) << 16)
                | (Math.round(green * 255.0F) << 8) | Math.round(blue * 255.0F);
    }
}
