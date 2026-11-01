package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.gui.design.Kit;
import dev.jilaton.client.gui.design.Ui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * A Jilaton list screen, built to the "Multiplayer · server list" design board (development/GUI_REDESIGN.md §6):
 * back button, title, optional segmented control and search in the header, 76px rows with the selected one ringed in
 * accent, and outline actions in the footer. Everything is drawn in board coordinates on a {@link Canvas}; subclasses
 * supply the items, how a row looks and what activating one does.
 */
public abstract class JilatonCardScreen<T> extends Screen {
    /** Inter instead of the vanilla pixel font for everything this screen draws ({@link JilatonType}). */
    protected final Font font = JilatonType.font();
    protected static final float LEFT = 112, RIGHT = 1168, TOP = 64, LIST_Y = 128, LIST_BOTTOM = 604, ROW_H = 76,
            GAP = 10, FOOTER_Y = 628, SEARCH_W = 260;
    private static final long DOUBLE_CLICK_NANOS = 350_000_000L;

    /** A footer action: outline button, or the accent one when {@code primary}. */
    protected record Action(String label, boolean primary, BooleanSupplier enabled, Runnable run) { }

    protected final Screen parent;
    protected final Canvas canvas = new Canvas();
    private final List<T> items = new ArrayList<>();
    private final Map<T, double[]> hover = new IdentityHashMap<>();
    private final List<Action> actions = new ArrayList<>();
    private double scroll, targetScroll;
    private long openedAt, lastFrame;
    private long lastClickNanos;
    private T lastClicked;
    protected T selected;
    protected EditBox search;
    private int segment;

    protected JilatonCardScreen(Component title, String section, Screen parent) {
        super(title);
        this.parent = parent;
    }

    /** All items, unfiltered, in display order. */
    protected abstract List<T> source();

    protected abstract String searchText(T item);

    /** Draws one row in board coordinates. */
    protected abstract void renderCard(GuiGraphics graphics, T item, float x, float y, float w, float h, double hover,
                                       boolean selected, double alpha, long now);

    protected abstract void activate(T item);

    /** Footer hint, left of the actions. */
    protected abstract String subtitle(int shown);

    protected abstract Component emptyMessage();

    /** The footer actions, left to right. */
    protected abstract List<Action> actions();

    /** Label of the accent button inside the selected row ("Join", "Play"). */
    protected abstract String rowAction();

    /** Whether the selected row's accent button is usable. */
    protected boolean rowActionActive(T item) { return true; }

    /** Segmented control labels in the header; none by default. */
    protected List<String> segments() { return List.of(); }

    protected boolean inSegment(T item, int segment) { return true; }

    protected final int segment() { return segment; }

    protected void selectionChanged() { }

    @Override
    protected void init() {
        canvas.fit(width, height);
        String previous = search == null ? "" : search.getValue();
        int sx = canvas.screenX(RIGHT - SEARCH_W + 34), sy = canvas.screenY(TOP + 17) - 4;
        search = new EditBox(font, sx, sy, Math.max(40, canvas.screenX(RIGHT - 70) - sx), 10, Component.literal("Search"));
        search.setBordered(false);
        search.setTextColor(Kit.LABEL);
        search.setHint(Component.literal("Search").withColor(Kit.LABEL_3));
        search.setValue(previous);
        search.setResponder(value -> {
            targetScroll = 0;
            refreshFilter();
        });
        addRenderableWidget(search);
        actions.clear();
        actions.addAll(actions());
        refreshFilter();
    }

    /** Rebuilds the filtered view; call after the source list changes. */
    protected final void refreshFilter() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        items.clear();
        for (T item : source())
            if (inSegment(item, segment) && (query.isEmpty() || searchText(item).toLowerCase(Locale.ROOT).contains(query)))
                items.add(item);
        if (selected != null && !items.contains(selected)) selected = null;
        selectionChanged();
    }

    protected final int shownCount() { return items.size(); }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Painted in render.
    }

    private double entrance(double delay, long now, boolean reduced) {
        if (reduced) return 1;
        double t = Math.max(0, Math.min(1, ((now - openedAt) / 1e9 - delay) / 0.45));
        return 1 - Math.pow(1 - t, 3);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        if (openedAt == 0L || minecraft.getOverlay() != null) openedAt = now;
        double dt = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        boolean reduced = JilatonClient.clientConfig().reducedMotion();
        int accent = Ui.accent();
        double mx = canvas.x(mouseX), my = canvas.y(mouseY);

        dev.jilaton.client.gui.design.Backdrops.drawScreen(graphics, width, height, partialTick);
        canvas.begin(graphics);
        dev.jilaton.client.gui.design.Backdrops.drawBoard(graphics);

        // Header: round glass back button, large title, sliding segmented control, search field.
        double p = entrance(0, now, reduced);
        double backHover = Kit.spring("list:back", Ui.inside(mx, my, LEFT, TOP, 36, 36));
        Kit.glass(graphics, LEFT, TOP, 36, 36, 18, backHover);
        Ui.textCentre(graphics, "\u2039", LEFT + 17, TOP + 6, 20, JilatonType.Face.MEDIUM, Kit.mix(Kit.LABEL_2, Kit.LABEL, backHover));
        Kit.text(graphics, getTitle().getString(), LEFT + 52, TOP + 2, Kit.TITLE, JilatonType.Face.BOLD, Kit.a(Kit.LABEL, p));
        List<String> segs = segments();
        if (!segs.isEmpty()) {
            Kit.segmented(graphics, "list:" + getClass().getSimpleName(), segs.toArray(new String[0]), segment,
                    segmentX(), TOP + 2, segmentsWidth(segs), 32, mx, my);
        }
        double searchFocus = Kit.spring("list:search", search.isFocused());
        Kit.field(graphics, RIGHT - SEARCH_W, TOP, SEARCH_W, 34, searchFocus);
        Kit.icon(graphics, "search", RIGHT - SEARCH_W + 19, TOP + 17, 14, Kit.LABEL_3);
        Kit.tag(graphics, "Ctrl F", RIGHT - 12 - Kit.tagWidth("Ctrl F"), TOP + 7, Kit.SURFACE_3, Kit.LABEL_3);

        // Rows.
        targetScroll = Math.max(0, Math.min(maxScroll(), targetScroll));
        scroll = Ui.approach(scroll, targetScroll, dt, 16, reduced);
        float view = LIST_BOTTOM - LIST_Y;
        // Scissor rectangles go through the current pose, so they are given in board coordinates.
        graphics.enableScissor(Math.round(LEFT - 8), Math.round(LIST_Y - 6), Math.round(RIGHT + 8), Math.round(LIST_BOTTOM + 6));
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            float y = (float) (LIST_Y + i * (ROW_H + GAP) - scroll);
            if (y + ROW_H < LIST_Y - 6 || y > LIST_BOTTOM + 6) continue;
            double e = entrance(0.06 + 0.03 * Math.min(i, 12), now, reduced);
            float rise = (float) (10 * (1 - e));
            boolean over = my >= LIST_Y && my <= LIST_BOTTOM && Ui.inside(mx, my, LEFT, y, RIGHT - LEFT, ROW_H);
            double[] h = hover.computeIfAbsent(item, ignored -> new double[1]);
            h[0] = Ui.approach(h[0], over ? 1 : 0, dt, 14, reduced);
            boolean isSelected = item == selected;
            double sel = Kit.spring("list:sel:" + System.identityHashCode(item), isSelected);
            float rowY = y + rise;
            Ui.round(graphics, LEFT, rowY, RIGHT - LEFT, ROW_H, Kit.R_TILE,
                    Kit.a(Kit.mix(Kit.mix(Kit.SURFACE, Kit.SURFACE_2, h[0]), Kit.SURFACE_3, sel), 0.86 * e));
            dev.jilaton.client.gui.design.Paint.outline(graphics, LEFT, rowY, RIGHT - LEFT, ROW_H, Kit.R_TILE, 0,
                    Kit.a(Kit.mix(Kit.HAIRLINE, Kit.accent(), 0.0), (0.6 + 0.4 * sel) * e));
            if (sel > 0.01) Ui.round(graphics, LEFT + 4, rowY + 18, 3, (float) ((ROW_H - 36) * sel), 1.5F, Kit.a(Kit.accent(), sel * e));
            float contentW = RIGHT - LEFT - (isSelected ? 110 : 0);
            renderCard(graphics, item, LEFT, rowY, contentW, ROW_H, h[0], isSelected, e, now);
            if (isSelected) {
                boolean active = rowActionActive(item);
                boolean btnHover = active && Ui.inside(mx, my, RIGHT - 96, rowY + 20, 80, 36);
                Kit.button(graphics, "list:row", rowAction(), RIGHT - 96, rowY + 20, 80, 36, active ? 0 : 2, btnHover, false);
            }
        }
        graphics.disableScissor();
        if (items.isEmpty()) {
            Ui.round(graphics, 640 - 22, LIST_Y + 40, 44, 44, 22, Kit.SURFACE_2);
            Kit.icon(graphics, segments().isEmpty() ? "folder" : "globe", 640, LIST_Y + 62, 20, Kit.LABEL_3);
            Ui.textCentre(graphics, emptyMessage().getString(), 640, LIST_Y + 104, Kit.CALLOUT, JilatonType.Face.REGULAR, Kit.LABEL_2);
        }
        int max = maxScroll();
        if (max > 0) {
            float thumb = Math.max(24, view * view / (view + max));
            float thumbY = (float) (LIST_Y + (view - thumb) * scroll / max);
            Ui.round(graphics, RIGHT + 14, thumbY, 4, thumb, 2, Kit.a(Kit.LABEL, 0.22));
        }

        // Footer.
        Kit.text(graphics, subtitle(items.size()), LEFT, FOOTER_Y + 11, Kit.FOOTNOTE, JilatonType.Face.REGULAR, Kit.LABEL_3);
        float bx = RIGHT;
        for (int i = actions.size() - 1; i >= 0; i--) {
            Action action = actions.get(i);
            float w = actionWidth(action);
            bx -= w;
            boolean enabled = action.enabled().getAsBoolean();
            boolean over = enabled && Ui.inside(mx, my, bx, FOOTER_Y, w, 36);
            if (enabled) {
                Kit.button(graphics, "list:act:" + action.label(), action.label(), bx, FOOTER_Y, w, 36, action.primary() ? 0 : 1, over, false);
            } else {
                Ui.round(graphics, bx, FOOTER_Y, w, 36, 18, Kit.a(Kit.SURFACE_2, 0.5));
                Ui.textCentre(graphics, action.label(), bx + w / 2, FOOTER_Y + 12, Kit.CALLOUT, JilatonType.Face.MEDIUM, Kit.LABEL_4);
            }
            bx -= 8;
        }
        canvas.end(graphics);
        search.render(graphics, mouseX, mouseY, partialTick);
    }

    private static float actionWidth(Action action) {
        return Math.max(84, Ui.width(action.label(), 13, JilatonType.Face.MEDIUM) + 32);
    }

    private static float segmentsWidth(List<String> segs) {
        float widest = 0;
        for (String s : segs) widest = Math.max(widest, Ui.width(s, Kit.CALLOUT, JilatonType.Face.SEMIBOLD) + 36);
        return widest * segs.size();
    }

    private float segmentX() {
        return LEFT + 72 + Ui.width(getTitle().getString(), Kit.TITLE, JilatonType.Face.BOLD);
    }

    /** Row surface helpers for subclasses: a square icon, texture or monogram tile, in board coordinates. */
    protected void cardIcon(GuiGraphics graphics, Identifier texture, String name, float x, float y, int size, double alpha) {
        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, Math.round(x), Math.round(y), 0, 0, size, size, size, size,
                    Ui.alpha(0xFFFFFFFF, alpha));
        } else {
            Ui.round(graphics, x, y, size, size, 10, Ui.alpha(Ui.RAISED, alpha));
            String letter = name.isBlank() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
            Ui.textCentre(graphics, letter, x + size / 2F, y + size / 2F - 9, 18, JilatonType.Face.SEMIBOLD, Ui.alpha(Ui.TEXT_2, alpha));
        }
    }

    /** Ellipsises {@code text} for {@code max} board px at {@code size}. */
    protected static String fit(String text, float max, float size, JilatonType.Face face) {
        return Ui.fit(text, max, size, face);
    }

    private int contentHeight() {
        return items.isEmpty() ? 0 : Math.round(items.size() * (ROW_H + GAP) - GAP);
    }

    private int maxScroll() {
        return Math.max(0, Math.round(contentHeight() - (LIST_BOTTOM - LIST_Y)));
    }

    private T itemAt(double mx, double my) {
        if (my < LIST_Y || my > LIST_BOTTOM || mx < LEFT || mx > RIGHT) return null;
        int index = (int) Math.floor((my - LIST_Y + targetScroll) / (ROW_H + GAP));
        if (index < 0 || index >= items.size()) return null;
        float top = (float) (LIST_Y + index * (ROW_H + GAP) - targetScroll);
        return my <= top + ROW_H ? items.get(index) : null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = canvas.x(event.x()), my = canvas.y(event.y());
        if (Ui.inside(mx, my, LEFT, TOP, 36, 36)) {
            onClose();
            return true;
        }
        if (Ui.inside(mx, my, RIGHT - SEARCH_W, TOP, SEARCH_W, 34)) {
            setFocused(search);
            search.setFocused(true);
            return super.mouseClicked(event, doubleClick) || true;
        }
        search.setFocused(false);
        List<String> segs = segments();
        if (!segs.isEmpty()) {
            int hit = Kit.segmentAt(segs.size(), segmentX(), TOP + 2, segmentsWidth(segs), 32, mx, my);
            if (hit >= 0) {
                segment = hit;
                targetScroll = 0;
                refreshFilter();
                return true;
            }
        }
        float bx = RIGHT;
        for (int i = actions.size() - 1; i >= 0; i--) {
            Action action = actions.get(i);
            float w = actionWidth(action);
            bx -= w;
            if (Ui.inside(mx, my, bx, FOOTER_Y, w, 36)) {
                if (action.enabled().getAsBoolean()) action.run().run();
                return true;
            }
            bx -= 8;
        }
        T item = itemAt(mx, my);
        if (item == null) return super.mouseClicked(event, doubleClick);
        if (item == selected && rowActionActive(item)) {
            int index = items.indexOf(item);
            float top = (float) (LIST_Y + index * (ROW_H + GAP) - targetScroll);
            if (Ui.inside(mx, my, RIGHT - 96, top + 20, 80, 36)) {
                activate(item);
                return true;
            }
        }
        long now = System.nanoTime();
        boolean repeat = item == lastClicked && now - lastClickNanos < DOUBLE_CLICK_NANOS;
        lastClicked = item;
        lastClickNanos = now;
        select(item);
        if (repeat || doubleClick) activate(item);
        return true;
    }

    protected final void select(T item) {
        selected = item;
        setFocused(null);
        selectionChanged();
        int index = items.indexOf(item);
        if (index < 0) return;
        float top = index * (ROW_H + GAP);
        float view = LIST_BOTTOM - LIST_Y;
        if (top < targetScroll) targetScroll = top;
        else if (top + ROW_H > targetScroll + view) targetScroll = top + ROW_H - view;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        targetScroll = Math.max(0, Math.min(maxScroll(), targetScroll - vertical * 48));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_F && event.hasControlDown()) {
            setFocused(search);
            search.setFocused(true);
            return true;
        }
        if (search.isFocused() && event.key() != GLFW.GLFW_KEY_ESCAPE && event.key() != GLFW.GLFW_KEY_DOWN)
            return super.keyPressed(event);
        int index = selected == null ? -1 : items.indexOf(selected);
        int move = switch (event.key()) {
            case GLFW.GLFW_KEY_DOWN -> 1;
            case GLFW.GLFW_KEY_UP -> -1;
            default -> 0;
        };
        if (move != 0 && !items.isEmpty()) {
            search.setFocused(false);
            select(items.get(Math.max(0, Math.min(items.size() - 1, index < 0 ? 0 : index + move))));
            return true;
        }
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) && selected != null) {
            activate(selected);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    /** Small helper: does a supplier-free "always" enabled flag read nicely at call sites. */
    protected static BooleanSupplier always() { return () -> true; }
}
