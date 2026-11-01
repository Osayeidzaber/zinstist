package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.config.HudStyle;
import dev.jilaton.client.gui.hud.HudPreset;
import dev.jilaton.client.gui.hud.TargetPreset;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.module.impl.HudModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public final class HudEditorScreen extends Screen {
    /** Inter instead of the vanilla pixel font for everything this screen draws ({@link JilatonType}). */
    private final Font font = JilatonType.font();
    private final ModuleManager modules;
    private final ClientConfig config;
    private final Screen parent;
    private final HudRenderer renderer;
    private String selectedWidget;
    private String draggingWidget;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean changed;
    private double guideX = Double.NaN, guideY = Double.NaN;
    private static final Integer[] ACCENTS = {null, 0xF2F2F2, 0x9C9C9C, 0x8878FF, 0x58AEFF, 0x51D6CB, 0xF080A5, 0xE9B65B};
    private static final String[] ACTIONS = {"Scale -", "Scale +", "Panel -", "Panel +", "Color", "Align", "Visible", "Reset"};
    private static final String HINT = "Drag (snaps; Alt = free) · TAB select · P preset · T target · M motion · ESC back";
    private static final int PRESET_Y = 12;
    private static final int PRESET_HEIGHT = 14;
    private static final int TARGET_PRESET_Y = PRESET_Y + PRESET_HEIGHT + 2;
    private static final int TARGET_PRESET_HEIGHT = 12;

    public HudEditorScreen(ModuleManager modules, ClientConfig config, Screen parent) {
        super(Component.literal("Jilaton HUD Editor"));
        this.modules = modules;
        this.config = config;
        this.parent = parent;
        this.renderer = new HudRenderer(modules, config);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x44070707);
        renderer.renderForEditor(graphics);
        if (minecraft.player == null) {
            String hint = "Enter a world to preview and edit HUD widgets";
            graphics.drawCenteredString(font, hint, width / 2, height / 2, 0xFFBEBEBE);
        }
        List<HudRenderer.WidgetBounds> widgets = renderer.lastBounds();
        for (HudRenderer.WidgetBounds widget : widgets) {
            int color = !config.hudStyle(widget.id()).visible() ? 0xFF6A6A6A
                    : widget.id().equals(selectedWidget) ? 0xFFDDDDDD : 0xFF919191;
            brackets(graphics, widget.x() - 3, widget.y() - 3, widget.width() + 6, widget.height() + 6, color);
            if (widget.id().equals(selectedWidget)) {
                graphics.fill(widget.x() - 2, widget.y() - 14, widget.x() + font.width(widget.id().toUpperCase()) + 6,
                        widget.y() - 3, 0xEE1B1B1B);
                graphics.drawString(font, widget.id().toUpperCase(), widget.x() + 1, widget.y() - 12, color);
            }
        }

        int accentGuide = 0xCC000000 | (ClientTheme.resolveAccent(config.themeId(), config.customAccent()) & 0xFFFFFF);
        if (!Double.isNaN(guideX)) graphics.fill((int) Math.round(guideX), 0, (int) Math.round(guideX) + 1, height, accentGuide);
        if (!Double.isNaN(guideY)) graphics.fill(0, (int) Math.round(guideY), width, (int) Math.round(guideY) + 1, accentGuide);

        int toolbarWidth = toolbarWidth();
        int toolbarX = toolbarX();
        int toolbarAccent = ClientTheme.resolveAccent(config.themeId(), config.customAccent());
        graphics.fill(toolbarX, 8, toolbarX + toolbarWidth, 60, 0xE6080808);
        graphics.fill(toolbarX, 8, toolbarX + toolbarWidth, 9, 0x33FFFFFF);
        String title = font.plainSubstrByWidth("HUD EDITOR" + (config.reducedMotion() ? " · Reduced motion" : ""),
                Math.max(0, Math.min(presetButtonX(0), targetPresetButtonX(0)) - toolbarX - 16));
        graphics.drawString(font, title, toolbarX + 9, 13, toolbarAccent);
        graphics.drawString(font, font.plainSubstrByWidth(HINT, toolbarWidth - 18), toolbarX + 9, 44, 0xFFBEBEBE);
        HudModule hud = (HudModule) modules.get("hud");
        for (int index = 0; index < HudPreset.CHOICES.size(); index++) {
            boolean active = hud.preset().equals(HudPreset.CHOICES.get(index));
            int left = presetButtonX(index);
            int buttonWidth = presetButtonWidth();
            graphics.fill(left, PRESET_Y, left + buttonWidth, PRESET_Y + PRESET_HEIGHT,
                    active ? 0xFF2A2A2A : 0xFF121212);
            graphics.fill(left, PRESET_Y + PRESET_HEIGHT - 2, left + buttonWidth, PRESET_Y + PRESET_HEIGHT,
                    active ? toolbarAccent : 0x00000000);
            graphics.drawString(font, HudPreset.CHOICES.get(index).toUpperCase(Locale.ROOT), left + 6,
                    PRESET_Y + 3, active ? toolbarAccent : 0xFFB0B0B0);
        }
        for (int index = 0; index < TargetPreset.CHOICES.size(); index++) {
            boolean active = hud.targetPreset().equals(TargetPreset.CHOICES.get(index));
            int left = targetPresetButtonX(index);
            int buttonWidth = targetPresetButtonWidth();
            graphics.fill(left, TARGET_PRESET_Y, left + buttonWidth, TARGET_PRESET_Y + TARGET_PRESET_HEIGHT,
                    active ? 0xFF2A2A2A : 0xFF121212);
            graphics.fill(left, TARGET_PRESET_Y + TARGET_PRESET_HEIGHT - 1, left + buttonWidth,
                    TARGET_PRESET_Y + TARGET_PRESET_HEIGHT, active ? toolbarAccent : 0x00000000);
            graphics.drawString(font, TargetPreset.CHOICES.get(index).toUpperCase(Locale.ROOT), left + 5,
                    TARGET_PRESET_Y + 2, active ? toolbarAccent : 0xFFB0B0B0);
        }
        if (selectedWidget != null) {
            renderInspector(graphics, mouseX, mouseY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Corner brackets around a widget instead of a full box outline. */
    private static void brackets(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        int l = Math.min(8, Math.min(w, h) / 3);
        graphics.fill(x, y, x + l, y + 1, color);
        graphics.fill(x, y, x + 1, y + l, color);
        graphics.fill(x + w - l, y, x + w, y + 1, color);
        graphics.fill(x + w - 1, y, x + w, y + l, color);
        graphics.fill(x, y + h - 1, x + l, y + h, color);
        graphics.fill(x, y + h - l, x + 1, y + h, color);
        graphics.fill(x + w - l, y + h - 1, x + w, y + h, color);
        graphics.fill(x + w - 1, y + h - l, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }
        int targetPreset = targetPresetAt(event.x(), event.y());
        if (targetPreset >= 0) {
            applyTargetPreset(targetPreset);
            return true;
        }
        int preset = presetAt(event.x(), event.y());
        if (preset >= 0) {
            applyPreset(preset);
            return true;
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
        // Snap to the screen centre and edges and to other widgets; hold Alt to place freely.
        guideX = Double.NaN;
        guideY = Double.NaN;
        if (!event.hasAltDown()) {
            List<HudSnap.Box> others = new java.util.ArrayList<>();
            for (HudRenderer.WidgetBounds other : renderer.lastBounds()) {
                if (!other.id().equals(draggingWidget)) others.add(new HudSnap.Box(other.x(), other.y(), other.width(), other.height()));
            }
            HudSnap.Result snapped = HudSnap.snap(new HudSnap.Box(x, y, bounds.width(), bounds.height()), others, width, height);
            x = Math.max(0.0, Math.min(width - bounds.width(), snapped.x()));
            y = Math.max(0.0, Math.min(height - bounds.height(), snapped.y()));
            guideX = snapped.guideX();
            guideY = snapped.guideY();
        }
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
            guideX = Double.NaN;
            guideY = Double.NaN;
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
        if (event.key() == GLFW.GLFW_KEY_P) {
            HudModule hud = (HudModule) modules.get("hud");
            int current = HudPreset.CHOICES.indexOf(hud.preset());
            applyPreset((current + 1) % HudPreset.CHOICES.size());
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_T) {
            HudModule hud = (HudModule) modules.get("hud");
            int current = TargetPreset.CHOICES.indexOf(hud.targetPreset());
            applyTargetPreset((current + 1) % TargetPreset.CHOICES.size());
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

    private int toolbarWidth() {
        return Math.min(Math.max(300, font.width(HINT) + 18), Math.max(0, width - 16));
    }

    private int toolbarX() {
        return (width - toolbarWidth()) / 2;
    }

    private int presetButtonWidth() {
        return Math.max(46, Math.max(font.width("Minimal"), font.width("Full")) + 12);
    }

    private int presetButtonX(int index) {
        return toolbarX() + toolbarWidth() - 7 - presetButtonWidth() * 2 - 4
                + index * (presetButtonWidth() + 4);
    }

    private int presetAt(double mouseX, double mouseY) {
        if (mouseY < PRESET_Y || mouseY >= PRESET_Y + PRESET_HEIGHT) {
            return -1;
        }
        for (int index = 0; index < HudPreset.CHOICES.size(); index++) {
            int left = presetButtonX(index);
            if (mouseX >= left && mouseX < left + presetButtonWidth()) {
                return index;
            }
        }
        return -1;
    }

    private void applyPreset(int index) {
        if (modules.get("hud") instanceof HudModule hud) {
            hud.selectPreset(HudPreset.CHOICES.get(index));
            config.save(modules);
        }
    }

    private int targetPresetButtonWidth() {
        int widest = 0;
        for (String label : TargetPreset.CHOICES) {
            widest = Math.max(widest, font.width(label));
        }
        return Math.max(50, widest + 10);
    }

    private int targetPresetButtonX(int index) {
        return toolbarX() + toolbarWidth() - 7
                - targetPresetButtonWidth() * TargetPreset.CHOICES.size()
                + index * targetPresetButtonWidth();
    }

    private int targetPresetAt(double mouseX, double mouseY) {
        if (mouseY < TARGET_PRESET_Y || mouseY >= TARGET_PRESET_Y + TARGET_PRESET_HEIGHT) {
            return -1;
        }
        for (int index = 0; index < TargetPreset.CHOICES.size(); index++) {
            int left = targetPresetButtonX(index);
            if (mouseX >= left && mouseX < left + targetPresetButtonWidth()) {
                return index;
            }
        }
        return -1;
    }

    private void applyTargetPreset(int index) {
        if (modules.get("hud") instanceof HudModule hud) {
            hud.selectTargetPreset(TargetPreset.CHOICES.get(index));
            config.save(modules);
        }
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
        graphics.fill(x, y, x + panelWidth, height - 7, 0xF2131313);
        String label = selectedWidget.toUpperCase(Locale.ROOT) + String.format(Locale.ROOT,
                "  %.2fx  Panel %d%%  %s", style.scale(), Math.round(style.panelOpacity() * 100), style.alignment());
        graphics.drawString(font, font.plainSubstrByWidth(label, panelWidth - 12), x + 6, y + 5, accent);
        String color = style.accent() == null ? "Theme" : String.format(Locale.ROOT, "#%06X", style.accent());
        graphics.drawString(font, "Color " + color + "  ·  " + (style.visible() ? "Visible" : "Hidden") + "  ·  TAB select",
                x + 6, y + 17, 0xFFA5A5A5);
        int cellWidth = panelWidth / 4;
        for (int index = 0; index < ACTIONS.length; index++) {
            int left = x + index % 4 * cellWidth;
            int top = height - 43 + index / 4 * 18;
            boolean hover = mouseX >= left && mouseX < left + cellWidth && mouseY >= top && mouseY < top + 17;
            graphics.fill(left + 2, top, left + cellWidth - 2, top + 16, hover ? 0xFF2C2C2C : 0xFF1E1E1E);
            graphics.drawString(font, font.plainSubstrByWidth(ACTIONS[index], cellWidth - 10), left + 6, top + 4,
                    hover ? accent : 0xFFE0E0E0);
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
