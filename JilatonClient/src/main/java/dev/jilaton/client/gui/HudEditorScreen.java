package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

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
        List<HudRenderer.WidgetBounds> widgets = renderer.lastBounds();
        for (HudRenderer.WidgetBounds widget : widgets) {
            int color = widget.id().equals(selectedWidget) ? 0xFFE0D8FF : 0xFF8792AB;
            drawOutline(graphics, widget.x() - 2, widget.y() - 2, widget.width() + 4, widget.height() + 4, color);
            if (widget.id().equals(selectedWidget)) {
                graphics.fill(widget.x() - 2, widget.y() - 14, widget.x() + font.width(widget.id().toUpperCase()) + 6,
                        widget.y() - 3, 0xEE161B2B);
                graphics.drawString(font, widget.id().toUpperCase(), widget.x() + 1, widget.y() - 12, color);
            }
        }

        int toolbarWidth = Math.min(Math.max(180, font.width("Drag widgets · R reset · ESC back") + 18), Math.max(0, width - 16));
        int toolbarX = (width - toolbarWidth) / 2;
        graphics.fill(toolbarX, 8, toolbarX + toolbarWidth, 42, 0xB0101118);
        graphics.drawString(font, "HUD EDITOR", toolbarX + 9, 15, ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
        graphics.drawString(font, "Drag widgets · R reset · ESC back", toolbarX + 9, 28, 0xFFB6BED0);
        if (selectedWidget != null) {
            graphics.drawString(font, "Selected: " + selectedWidget, toolbarX + 9, 48, 0xFF9CA6BC);
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
            minecraft.setScreen(parent);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_R && selectedWidget != null) {
            config.clearHudPosition(selectedWidget);
            config.save(modules);
            selectedWidget = null;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
