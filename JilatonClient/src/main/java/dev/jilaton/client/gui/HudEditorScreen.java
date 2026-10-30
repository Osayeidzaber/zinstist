package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.config.HudStyle;
import dev.jilaton.client.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public final class HudEditorScreen extends Screen {
    private final ModuleManager modules;
    private final ClientConfig config;
    private final Screen parent;
    private final HudRenderer renderer;
    private String selectedWidget;
    private String draggingWidget;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean changed;
    private static final Integer[] ACCENTS = {null, 0x8878FF, 0x58AEFF, 0x51D6CB, 0xF080A5, 0xE9B65B};
    private static final String[] ACTIONS = {"Scale -", "Scale +", "Panel -", "Panel +", "Color", "Align", "Visible", "Reset"};

    public HudEditorScreen(ModuleManager modules, ClientConfig config, Screen parent) {
        super(Component.literal("Jilaton HUD Editor"));
        this.modules = modules;
        this.config = config;
        this.parent = parent;
        this.renderer = new HudRenderer(modules, config);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x4404070F);
        renderer.renderForEditor(graphics);
        if (minecraft.player == null) {
            String hint = "Enter a world to preview and edit HUD widgets";
            graphics.drawCenteredString(font, hint, width / 2, height / 2, 0xFFB6BED0);
        }
        List<HudRenderer.WidgetBounds> widgets = renderer.lastBounds();
        for (HudRenderer.WidgetBounds widget : widgets) {
            int color = !config.hudStyle(widget.id()).visible() ? 0xFF765362
                    : widget.id().equals(selectedWidget) ? 0xFFE0D8FF : 0xFF8792AB;
            drawOutline(graphics, widget.x() - 2, widget.y() - 2, widget.width() + 4, widget.height() + 4, color);
            if (widget.id().equals(selectedWidget)) {
                graphics.fill(widget.x() - 2, widget.y() - 14, widget.x() + font.width(widget.id().toUpperCase()) + 6,
                        widget.y() - 3, 0xEE161B2B);
                graphics.drawString(font, widget.id().toUpperCase(), widget.x() + 1, widget.y() - 12, color);
            }
        }

        int toolbarWidth = Math.min(Math.max(180, font.width("Drag · TAB select · M motion · ESC back") + 18), Math.max(0, width - 16));
        int toolbarX = (width - toolbarWidth) / 2;
        graphics.fill(toolbarX, 8, toolbarX + toolbarWidth, 42, 0xB0101118);
        graphics.drawString(font, "HUD EDITOR" + (config.reducedMotion() ? " · Reduced motion" : ""), toolbarX + 9, 15, ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
        graphics.drawString(font, "Drag · TAB select · M motion · ESC back", toolbarX + 9, 28, 0xFFB6BED0);
        if (selectedWidget != null) {
            renderInspector(graphics, mouseX, mouseY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }
        if (selectedWidget != null && inspectorContains(event.x(), event.y())) {
            int cellWidth = inspectorWidth() / 4;
            int row = (int) (event.y() - (height - 43)) / 18;
            if (event.y() >= height - 43 && event.y() < height - 7) {
                int column = Math.min(3, (int) (event.x() - inspectorX()) / cellWidth);
                edit(row * 4 + column);
            }
            return true;
        }
        List<HudRenderer.WidgetBounds> widgets = renderer.lastBounds();
        for (int index = widgets.size() - 1; index >= 0; index--) {
            HudRenderer.WidgetBounds widget = widgets.get(index);
            if (!widget.contains(event.x(), event.y())) {
                continue;
            }
            selectedWidget = widget.id();
            draggingWidget = widget.id();
            dragOffsetX = event.x() - widget.x();
            dragOffsetY = event.y() - widget.y();
            changed = false;
            return true;
        }
        selectedWidget = null;
        draggingWidget = null;
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (super.mouseDragged(event, dragX, dragY)) {
            return true;
        }
        if (draggingWidget == null || event.button() != 0) {
            return false;
        }
        HudRenderer.WidgetBounds bounds = renderer.lastBounds().stream()
                .filter(widget -> widget.id().equals(draggingWidget))
                .findFirst().orElse(null);
        if (bounds == null) {
            return true;
        }

        double x = Math.max(0.0, Math.min(width - bounds.width(), event.x() - dragOffsetX));
        double y = Math.max(0.0, Math.min(height - bounds.height(), event.y() - dragOffsetY));
        double xFraction = width <= bounds.width() ? 0.0 : x / (width - bounds.width());
        double yFraction = height <= bounds.height() ? 0.0 : y / (height - bounds.height());
        config.setHudPosition(draggingWidget, xFraction, yFraction);
        HudStyle style = config.hudStyle(draggingWidget);
        if (style.alignment() != HudStyle.Alignment.FREE) {
            config.setHudStyle(draggingWidget, new HudStyle(style.scale(), style.panelOpacity(), style.accent(),
                    HudStyle.Alignment.FREE, style.visible()));
        }
        changed = true;
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && draggingWidget != null) {
            draggingWidget = null;
            if (changed) {
                config.save(modules);
            }
            changed = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_TAB) {
            List<HudRenderer.WidgetBounds> widgets = renderer.lastBounds();
            if (!widgets.isEmpty()) {
                int current = -1;
                for (int index = 0; index < widgets.size(); index++) {
                    if (widgets.get(index).id().equals(selectedWidget)) current = index;
                }
                selectedWidget = widgets.get((current + 1) % widgets.size()).id();
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_M) {
            config.setReducedMotion(!config.reducedMotion());
            config.save(modules);
            return true;
        }
        if (selectedWidget != null) {
            int action = switch (event.key()) {
                case GLFW.GLFW_KEY_LEFT_BRACKET -> 0;
                case GLFW.GLFW_KEY_RIGHT_BRACKET -> 1;
                case GLFW.GLFW_KEY_MINUS -> 2;
                case GLFW.GLFW_KEY_EQUAL -> 3;
                case GLFW.GLFW_KEY_C -> 4;
                case GLFW.GLFW_KEY_A -> 5;
                case GLFW.GLFW_KEY_H -> 6;
                case GLFW.GLFW_KEY_R -> 7;
                default -> -1;
            };
            if (action >= 0) {
                edit(action);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        savePending();
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        savePending();
        super.removed();
    }

    private void savePending() {
        if (changed) config.save(modules);
        changed = false;
        draggingWidget = null;
    }

    private int inspectorWidth() { return Math.min(320, Math.max(4, width - 16)); }
    private int inspectorX() { return (width - inspectorWidth()) / 2; }

    private boolean inspectorContains(double x, double y) {
        return x >= inspectorX() && x < inspectorX() + inspectorWidth() && y >= height - 73 && y < height - 7;
    }

    private void renderInspector(GuiGraphics graphics, int mouseX, int mouseY) {
        HudStyle style = config.hudStyle(selectedWidget);
        int x = inspectorX();
        int y = height - 73;
        int panelWidth = inspectorWidth();
        int accent = style.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
        graphics.fill(x, y, x + panelWidth, height - 7, 0xF211131F);
        String label = selectedWidget.toUpperCase(Locale.ROOT) + String.format(Locale.ROOT,
                "  %.2fx  Panel %d%%  %s", style.scale(), Math.round(style.panelOpacity() * 100), style.alignment());
        graphics.drawString(font, font.plainSubstrByWidth(label, panelWidth - 12), x + 6, y + 5, accent);
        String color = style.accent() == null ? "Theme" : String.format(Locale.ROOT, "#%06X", style.accent());
        graphics.drawString(font, "Color " + color + "  ·  " + (style.visible() ? "Visible" : "Hidden") + "  ·  TAB select",
                x + 6, y + 17, 0xFF9CA6BC);
        int cellWidth = panelWidth / 4;
        for (int index = 0; index < ACTIONS.length; index++) {
            int left = x + index % 4 * cellWidth;
            int top = height - 43 + index / 4 * 18;
            boolean hover = mouseX >= left && mouseX < left + cellWidth && mouseY >= top && mouseY < top + 17;
            graphics.fill(left + 2, top, left + cellWidth - 2, top + 16, hover ? 0xFF302943 : 0xFF1C1D2A);
            graphics.drawString(font, font.plainSubstrByWidth(ACTIONS[index], cellWidth - 10), left + 6, top + 4,
                    hover ? accent : 0xFFE1DEEC);
        }
    }

    private void edit(int action) {
        HudStyle style = config.hudStyle(selectedWidget);
        double scale = style.scale();
        double opacity = style.panelOpacity();
        Integer accent = style.accent();
        HudStyle.Alignment alignment = style.alignment();
        boolean visible = style.visible();
        switch (action) {
            case 0 -> scale -= 0.1;
            case 1 -> scale += 0.1;
            case 2 -> opacity -= 0.1;
            case 3 -> opacity += 0.1;
            case 4 -> {
                int index = java.util.Arrays.asList(ACCENTS).indexOf(accent);
                accent = ACCENTS[(index + 1) % ACCENTS.length];
            }
            case 5 -> alignment = HudStyle.Alignment.values()[(alignment.ordinal() + 1) % HudStyle.Alignment.values().length];
            case 6 -> visible = !visible;
            case 7 -> {
                config.resetHudWidget(selectedWidget);
                config.save(modules);
                return;
            }
            default -> { return; }
        }
        config.setHudStyle(selectedWidget, new HudStyle(scale, opacity, accent, alignment, visible));
        config.save(modules);
    }

    private static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
