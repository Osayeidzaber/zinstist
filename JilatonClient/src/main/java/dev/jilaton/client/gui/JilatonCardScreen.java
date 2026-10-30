package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import net.minecraft.ChatFormatting;
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

/**
 * A Jilaton list screen: editorial header, live search, a scrollable grid of animated cards and a bottom
 * action bar. Subclasses supply the items, how a card looks and what activating one does.
 */
public abstract class JilatonCardScreen<T> extends Screen {
    protected static final int CARD_HEIGHT = 64;
    private static final int GAP = 8;
    private static final int TARGET_CARD_WIDTH = 236;
    private static final long DOUBLE_CLICK_NANOS = 350_000_000L;
    protected final Screen parent;
    private final String section;
    private final List<T> items = new ArrayList<>();
    private final Map<T, Motion.Transition> hover = new IdentityHashMap<>();
    private final AnimatedValue scroll = new AnimatedValue();
    private double targetScroll;
    private long openedAt;
    private long frameNanos;
    private long lastClickNanos;
    private T lastClicked;
    protected T selected;
    protected EditBox search;
    protected int margin, gridX, gridY, gridW, gridBottom, columns, cardW, barY;

    protected JilatonCardScreen(Component title, String section, Screen parent) {
        super(title);
        this.section = section;
        this.parent = parent;
    }

    /** All items, unfiltered, in display order. */
    protected abstract List<T> source();

    protected abstract String searchText(T item);

    protected abstract void renderCard(GuiGraphics graphics, T item, int x, int y, int w, int h, double hover,
                                       boolean selected, double alpha, long now);

    protected abstract void activate(T item);

    protected abstract String subtitle(int shown);

    protected abstract Component emptyMessage();

    /** Adds the bottom action bar's buttons; called from init after layout. */
    protected abstract void addActions(int y);

    /** Lets subclasses enable/disable buttons that depend on a selection. */
    protected void selectionChanged() { }

    @Override
    protected void init() {
        margin = Math.max(16, Math.min(40, width / 20));
        gridX = margin;
        gridW = width - margin * 2;
        gridY = margin + 74;
        barY = height - margin - 20;
        gridBottom = barY - 12;
        columns = Math.max(1, (gridW + GAP) / (TARGET_CARD_WIDTH + GAP));
        cardW = (gridW - GAP * (columns - 1)) / columns;
        String previous = search == null ? "" : search.getValue();
        int searchW = Math.min(220, Math.max(120, width / 4));
        search = new EditBox(font, width - margin - searchW, margin + 36, searchW, 18, Component.literal("Search"));
        search.setHint(Component.literal("Search").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(previous);
        search.setResponder(value -> {
            targetScroll = 0;
            refreshFilter();
        });
        addRenderableWidget(search);
        addActions(barY);
        refreshFilter();
    }

    /** Rebuilds the filtered view; call after the source list changes. */
    protected final void refreshFilter() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        items.clear();
        for (T item : source()) if (query.isEmpty() || searchText(item).toLowerCase(Locale.ROOT).contains(query)) items.add(item);
        if (selected != null && !items.contains(selected)) selected = null;
        selectionChanged();
    }

    protected final long frameNanos() { return frameNanos; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        frameNanos = System.nanoTime();
        if (openedAt == 0L || minecraft.getOverlay() != null) openedAt = frameNanos;
        boolean reduced = JilatonWidgets.reducedMotion();
        double intensity = JilatonClient.clientConfig().animationIntensity();
        JilatonBackdrop.render(graphics, width, height, JilatonBackdrop.clock(reduced), 0, 0, 1);
        renderHeader(graphics, reduced, intensity);
        renderGrid(graphics, mouseX, mouseY, reduced, intensity);
        graphics.fill(margin, barY - 8, width - margin, barY - 7, JilatonUi.HAIRLINE);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The card screen paints its own canvas in render.
    }

    private double entrance(double delay, boolean reduced) {
        return reduced ? 1 : Motion.staggered(openedAt, frameNanos, delay, 0.7, Motion.SETTLE);
    }

    private void renderHeader(GuiGraphics graphics, boolean reduced, double intensity) {
        double p = entrance(0, reduced);
        int top = margin / 2 + 4;
        JilatonLogo.draw(graphics, margin, top, 9, JilatonUi.alpha(JilatonUi.TEXT_2, p));
        graphics.drawString(font, "JILATON", margin + 14, top + 1, JilatonUi.alpha(JilatonUi.TEXT_2, p), false);
        graphics.drawString(font, "— " + section, margin + 18 + font.width("JILATON"), top + 1,
                JilatonUi.alpha(JilatonUi.TEXT_4, p), false);
        String back = "ESC — BACK";
        graphics.drawString(font, back, width - margin - font.width(back), top + 1, JilatonUi.alpha(JilatonUi.TEXT_4, p), false);

        double q = entrance(0.06, reduced);
        int rise = (int) Math.round(Motion.amount(10, intensity, reduced) * (1 - q));
        graphics.pose().pushMatrix();
        graphics.pose().translate(margin, margin + 22 + rise);
        graphics.pose().scale(3, 3);
        graphics.drawString(font, getTitle().copy().withStyle(ChatFormatting.BOLD), 0, 0, JilatonUi.alpha(JilatonUi.TEXT, q), false);
        graphics.pose().popMatrix();
        graphics.drawString(font, subtitle(items.size()), margin + 1, margin + 54 + rise, JilatonUi.alpha(JilatonUi.TEXT_3, q), false);
        int line = (int) Math.round(gridW * q);
        graphics.fill(margin, gridY - 8, margin + line, gridY - 7, JilatonUi.HAIRLINE_STRONG);
    }

    private int contentHeight() {
        int rows = (items.size() + columns - 1) / columns;
        return rows == 0 ? 0 : rows * (CARD_HEIGHT + GAP) - GAP;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (gridBottom - gridY));
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY, boolean reduced, double intensity) {
        targetScroll = Math.max(0, Math.min(maxScroll(), targetScroll));
        double offset = scroll.update(targetScroll, frameNanos, reduced);
        graphics.enableScissor(gridX - 4, gridY - 4, gridX + gridW + 4, gridBottom + 4);
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            int x = gridX + (i % columns) * (cardW + GAP);
            int y = gridY + (i / columns) * (CARD_HEIGHT + GAP) - (int) Math.round(offset);
            if (y + CARD_HEIGHT < gridY - 4 || y > gridBottom + 4) continue;
            double p = entrance(0.12 + 0.035 * Math.min(i, 14), reduced);
            int rise = (int) Math.round(Motion.amount(12, intensity, reduced) * (1 - p));
            boolean over = mouseY >= gridY && mouseY <= gridBottom && inside(mouseX, mouseY, x, y, cardW, CARD_HEIGHT);
            double h = hover.computeIfAbsent(item, ignored -> new Motion.Transition(0.2, Motion.SETTLE))
                    .update(over, frameNanos, reduced);
            int lift = (int) Math.round(Motion.amount(2, intensity, reduced) * h);
            boolean isSelected = item == selected;
            renderCard(graphics, item, x, y + rise - lift, cardW, CARD_HEIGHT, h, isSelected, p, frameNanos);
        }
        graphics.disableScissor();
        if (items.isEmpty()) {
            Component message = emptyMessage();
            graphics.drawString(font, message, gridX + (gridW - font.width(message)) / 2, gridY + 30, JilatonUi.TEXT_3, false);
        }
        int max = maxScroll();
        if (max > 0) {
            int track = gridBottom - gridY;
            int thumb = Math.max(20, track * track / (track + max));
            int thumbY = gridY + (int) Math.round((track - thumb) * offset / max);
            graphics.fill(width - margin / 2 - 1, gridY, width - margin / 2, gridBottom, JilatonUi.HAIRLINE);
            graphics.fill(width - margin / 2 - 1, thumbY, width - margin / 2 + 1, thumbY + thumb, JilatonUi.TEXT_3);
        }
    }

    /** Card surface shared by subclasses: lifts and brightens on hover, gets a white edge when selected. */
    protected static void cardSurface(GuiGraphics graphics, int x, int y, int w, int h, double hover, boolean selected, double alpha) {
        JilatonUi.fillRounded(graphics, x, y, w, h, 3, JilatonUi.alpha(JilatonUi.mix(0xEE0B0B0B, 0xF5161616, hover), alpha));
        int border = selected ? 0xFFD9D9D9 : JilatonUi.mix(0xFF212121, 0xFF555555, hover);
        JilatonUi.strokeRounded(graphics, x, y, w, h, 3, JilatonUi.alpha(border, alpha));
        if (selected) graphics.fill(x + 1, y + 6, x + 3, y + h - 6, JilatonUi.alpha(JilatonUi.TEXT, alpha));
    }

    /** Square icon: a texture when one is available, otherwise a monogram tile. */
    protected void cardIcon(GuiGraphics graphics, Identifier texture, String name, int x, int y, int size, double alpha) {
        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, size, size, size, size, JilatonUi.alpha(0xFFFFFFFF, alpha));
        } else {
            graphics.fill(x, y, x + size, y + size, JilatonUi.alpha(0xFF141414, alpha));
            String letter = name.isBlank() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + size / 2F - font.width(letter), y + size / 2F - 8);
            graphics.pose().scale(2, 2);
            graphics.drawString(font, letter, 0, 0, JilatonUi.alpha(JilatonUi.TEXT_3, alpha), false);
            graphics.pose().popMatrix();
        }
        JilatonUi.stroke(graphics, x, y, size, size, JilatonUi.alpha(0xFF262626, alpha));
    }

    protected String fit(String text, int width) {
        return font.width(text) <= width ? text : font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private T itemAt(double mouseX, double mouseY) {
        if (mouseY < gridY || mouseY > gridBottom) return null;
        double offset = targetScroll;
        for (int i = 0; i < items.size(); i++) {
            int x = gridX + (i % columns) * (cardW + GAP);
            int y = gridY + (i / columns) * (CARD_HEIGHT + GAP) - (int) Math.round(offset);
            if (inside(mouseX, mouseY, x, y, cardW, CARD_HEIGHT)) return items.get(i);
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;
        T item = itemAt(event.x(), event.y());
        if (item == null) return false;
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
        int top = (index / columns) * (CARD_HEIGHT + GAP);
        int view = gridBottom - gridY;
        if (top < targetScroll) targetScroll = top;
        else if (top + CARD_HEIGHT > targetScroll + view) targetScroll = top + CARD_HEIGHT - view;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        targetScroll = Math.max(0, Math.min(maxScroll(), targetScroll - vertical * 36));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (search.isFocused() && event.key() != GLFW.GLFW_KEY_ESCAPE && event.key() != GLFW.GLFW_KEY_DOWN)
            return super.keyPressed(event);
        int index = selected == null ? -1 : items.indexOf(selected);
        int move = switch (event.key()) {
            case GLFW.GLFW_KEY_RIGHT -> 1;
            case GLFW.GLFW_KEY_LEFT -> -1;
            case GLFW.GLFW_KEY_DOWN -> columns;
            case GLFW.GLFW_KEY_UP -> -columns;
            default -> 0;
        };
        if (move != 0 && !items.isEmpty()) {
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
}
