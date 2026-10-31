package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.design.Canvas;
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
        search.setTextColor(Ui.TEXT);
        search.setHint(Component.literal("Search").withColor(Ui.TEXT_3));
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

        graphics.fill(0, 0, width, height, Ui.BACKDROP);
        canvas.begin(graphics);
        Ui.glow(graphics, 1120, 80, 420, accent & 0xFFFFFF, 0.05);

        // Header.
        double p = entrance(0, now, reduced);
        boolean backHover = Ui.inside(mx, my, LEFT, TOP, 36, 36);
        Ui.card(graphics, LEFT, TOP, 36, 36, 10, Ui.alpha(Ui.RAISED, Ui.ease("list:back", backHover)), Ui.STROKE);
        Ui.text(graphics, "‹", LEFT + 13, TOP + 8, 17, Ui.SOFT);
        Ui.text(graphics, getTitle().getString(), LEFT + 52, TOP + 4, 26, JilatonType.Face.SEMIBOLD, Ui.alpha(Ui.TEXT, p));
        List<String> segs = segments();
        if (!segs.isEmpty()) {
            float sx = LEFT + 68 + Ui.width(getTitle().getString(), 26, JilatonType.Face.SEMIBOLD);
            Ui.card(graphics, sx, TOP + 2, segmentsWidth(segs), 32, 9, Ui.CARD, Ui.STROKE);
            float cx = sx + 3;
            for (int i = 0; i < segs.size(); i++) {
                float w = Ui.width(segs.get(i), 12, JilatonType.Face.MEDIUM) + 28;
                if (i == segment) Ui.round(graphics, cx, TOP + 5, w, 26, 7, Ui.RAISED);
                Ui.text(graphics, segs.get(i), cx + 14, TOP + 12, 12, JilatonType.Face.MEDIUM, i == segment ? Ui.TEXT : Ui.TEXT_2);
                cx += w;
            }
        }
        boolean searchFocus = search.isFocused();
        Ui.card(graphics, RIGHT - SEARCH_W, TOP, SEARCH_W, 34, 9, Ui.CARD, searchFocus ? Ui.alpha(accent, 0.55) : Ui.STROKE);
        Ui.outline(graphics, RIGHT - SEARCH_W + 13, TOP + 11, 10, 10, 5, Ui.TEXT_3);
        Ui.rect(graphics, RIGHT - SEARCH_W + 22, TOP + 20, 3, 2, Ui.TEXT_3);
        Ui.outline(graphics, RIGHT - 58, TOP + 8, 46, 18, 5, Ui.STROKE);
        Ui.text(graphics, "Ctrl F", RIGHT - 52, TOP + 12, 10, JilatonType.Face.MONO, Ui.TEXT_3);

        // Rows.
        targetScroll = Math.max(0, Math.min(maxScroll(), targetScroll));
        scroll = Ui.approach(scroll, targetScroll, dt, 16, reduced);
        float view = LIST_BOTTOM - LIST_Y;
        graphics.enableScissor(canvas.screenX(LEFT - 8), canvas.screenY(LIST_Y - 6), canvas.screenX(RIGHT + 8),
                canvas.screenY(LIST_BOTTOM + 6));
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
            float rowY = y + rise;
            int fill = isSelected ? Ui.mix(Ui.CARD, accent, 0.05) : Ui.mix(Ui.CARD, Ui.RAISED, h[0]);
            Ui.round(graphics, LEFT, rowY, RIGHT - LEFT, ROW_H, 14, Ui.alpha(fill, e));
            if (isSelected) Ui.glowRing(graphics, LEFT, rowY, RIGHT - LEFT, ROW_H, 14, accent, e);
            else Ui.outline(graphics, LEFT, rowY, RIGHT - LEFT, ROW_H, 14, Ui.alpha(Ui.STROKE, e));
            float contentW = RIGHT - LEFT - (isSelected ? 110 : 0);
            renderCard(graphics, item, LEFT, rowY, contentW, ROW_H, h[0], isSelected, e, now);
            if (isSelected) {
                boolean active = rowActionActive(item);
                boolean btnHover = active && Ui.inside(mx, my, RIGHT - 96, rowY + 20, 80, 36);
                if (active) Ui.button(graphics, rowAction(), RIGHT - 96, rowY + 20, 80, 36, btnHover, true, accent);
                else Ui.button(graphics, rowAction(), RIGHT - 96, rowY + 20, 80, 36, false, false, accent);
            }
        }
        graphics.disableScissor();
        if (items.isEmpty()) {
            Ui.textCentre(graphics, emptyMessage().getString(), 640, LIST_Y + 60, 13, JilatonType.Face.REGULAR, Ui.TEXT_2);
        }
        int max = maxScroll();
        if (max > 0) {
            float thumb = Math.max(24, view * view / (view + max));
            float thumbY = (float) (LIST_Y + (view - thumb) * scroll / max);
            Ui.round(graphics, RIGHT + 14, thumbY, 4, thumb, 2, Ui.FAINT);
        }

        // Footer.
        Ui.hline(graphics, LEFT, FOOTER_Y - 12, RIGHT - LEFT, Ui.STROKE);
        Ui.text(graphics, subtitle(items.size()), LEFT, FOOTER_Y + 12, 12, Ui.TEXT_3);
        float bx = RIGHT;
        for (int i = actions.size() - 1; i >= 0; i--) {
            Action action = actions.get(i);
            float w = actionWidth(action);
            bx -= w;
            boolean enabled = action.enabled().getAsBoolean();
            boolean over = enabled && Ui.inside(mx, my, bx, FOOTER_Y, w, 36);
            if (enabled) Ui.button(graphics, action.label(), bx, FOOTER_Y, w, 36, over, action.primary(), accent);
            else {
                Ui.outline(graphics, bx, FOOTER_Y, w, 36, 10, Ui.alpha(Ui.STROKE, 0.6));
                Ui.textCentre(graphics, action.label(), bx + w / 2, FOOTER_Y + 12, 13, JilatonType.Face.MEDIUM, Ui.FAINT);
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
        float w = 6;
        for (String s : segs) w += Ui.width(s, 12, JilatonType.Face.MEDIUM) + 28;
        return w;
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
            float cx = LEFT + 71 + Ui.width(getTitle().getString(), 26, JilatonType.Face.SEMIBOLD);
            for (int i = 0; i < segs.size(); i++) {
                float w = Ui.width(segs.get(i), 12, JilatonType.Face.MEDIUM) + 28;
                if (Ui.inside(mx, my, cx, TOP + 2, w, 32)) {
                    segment = i;
                    targetScroll = 0;
                    refreshFilter();
                    return true;
                }
                cx += w;
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
