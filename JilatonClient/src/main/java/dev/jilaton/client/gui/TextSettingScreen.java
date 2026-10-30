package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Focused text editor; invalid input stays editable and is never saved. */
public final class TextSettingScreen extends Screen {
    /** Inter instead of the vanilla pixel font for everything this screen draws ({@link JilatonType}). */
    private final Font font = JilatonType.font();
    private final Screen parent;
    private final TextSetting setting;
    private final ClientConfig config;
    private final ModuleManager modules;
    private EditBox input;
    private String error = "";
    private int left;
    private int top;
    private int panelWidth;

    public TextSettingScreen(Screen parent, TextSetting setting, ModuleManager modules, ClientConfig config) {
        super(Component.literal(setting.label()));
        this.parent = parent;
        this.setting = setting;
        this.modules = modules;
        this.config = config;
    }

    @Override protected void init() {
        String draft = input == null ? setting.value() : input.getValue();
        panelWidth = Math.min(440, Math.max(120, width - 24));
        left = (width - panelWidth) / 2;
        top = Math.max(8, (height - 144) / 2);
        input = new EditBox(font, left + 12, top + 46, panelWidth - 24, 22, title);
        input.setMaxLength(setting.maximumLength());
        input.setValue(draft);
        input.setBordered(false);
        addRenderableWidget(input);
        setInitialFocus(input);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int accent = ClientTheme.resolveAccent(config.themeId(), config.customAccent());
        graphics.fill(left, top, left + panelWidth, top + 144, 0xF2101010);
        graphics.fill(left, top, left + panelWidth, top + 2, accent);
        graphics.drawString(font, title, left + 12, top + 14, accent);
        graphics.drawString(font, "ENTER saves · ESC cancels", left + 12, top + 29, 0xFFA5A5A5);
        graphics.fill(left + 8, top + 42, left + panelWidth - 8, top + 72, 0xFF202020);
        graphics.drawString(font, font.plainSubstrByWidth(error, panelWidth - 24), left + 12, top + 82, 0xFFFF858D);
        for (int index = 0; index < 2; index++) {
            int x = left + panelWidth - 156 + index * 74;
            boolean hover = mouseX >= x && mouseX < x + 68 && mouseY >= top + 110 && mouseY < top + 132;
            graphics.fill(x, top + 110, x + 68, top + 132, hover ? 0xFF343434 : 0xFF282828);
            graphics.drawString(font, index == 0 ? "SAVE" : "CANCEL", x + 10, top + 117, 0xFFE3E3E3);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void save() {
        try {
            setting.setValue(input.getValue());
            config.save(modules);
            onClose();
        } catch (IllegalArgumentException exception) {
            error = exception.getMessage();
        }
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) { save(); return true; }
        return super.keyPressed(event);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && event.y() >= top + 110 && event.y() < top + 132) {
            if (event.x() >= left + panelWidth - 156 && event.x() < left + panelWidth - 88) { save(); return true; }
            if (event.x() >= left + panelWidth - 82 && event.x() < left + panelWidth - 14) { onClose(); return true; }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
}
