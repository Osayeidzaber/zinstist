package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Original, lightweight title screen for Jilaton. */
public final class JilatonMainMenuScreen extends Screen {
    private static final int INK = 0xFF080A12;
    private static final int PANEL = 0xF20D101B;
    private static final int MUTED = 0xFF838BA0;
    private final List<MenuAction> actions = new ArrayList<>(5);
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int accent;
    private boolean compact;

    public JilatonMainMenuScreen() {
        super(Component.literal("Jilaton Client"));
    }

    @Override
    protected void init() {
        actions.clear();
        accent = ClientTheme.resolveAccent(JilatonClient.clientConfig().themeId(),
                JilatonClient.clientConfig().customAccent());
        compact = width < 690 || height < 430;
        panelWidth = Math.max(240, Math.min(compact ? 560 : 1020, width - 36));
        panelHeight = Math.max(200, Math.min(compact ? 520 : 500, height - 32));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        if (compact) {
            int buttonWidth = Math.min(panelWidth - 36, 390);
            int buttonX = (width - buttonWidth) / 2;
            int buttonHeight = height < 360 ? 28 : 36;
            int gap = height < 360 ? 4 : 7;
            int buttonY = panelY + Math.min(116, Math.max(76, panelHeight / 3));
            addAction("Singleplayer", "Open a local world", buttonX, buttonY, buttonWidth, buttonHeight,
                    false, () -> minecraft.setScreen(new SelectWorldScreen(this)));
            addAction("Multiplayer", "Join a server", buttonX, buttonY += buttonHeight + gap,
                    buttonWidth, buttonHeight, true, () -> minecraft.setScreen(new JoinMultiplayerScreen(this)));
            addAction("Jilaton menu", "Modules, themes and profiles", buttonX, buttonY += buttonHeight + gap,
                    buttonWidth, buttonHeight, false, () -> JilatonClient.openClickGui(this));
            addAction("Options", "Controls, video and audio", buttonX, buttonY += buttonHeight + gap,
                    buttonWidth, buttonHeight, false,
                    () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
            addAction("Quit", "Exit Minecraft", buttonX, buttonY += buttonHeight + gap,
                    buttonWidth, buttonHeight, false, minecraft::stop);
        } else {
            int leftWidth = Math.round(panelWidth * 0.54F);
            int rightX = panelX + leftWidth + 18;
            int rightWidth = panelX + panelWidth - rightX - 18;
            int buttonX = rightX;
            int buttonWidth = rightWidth;
            int buttonHeight = 48;
            int gap = 9;
            int buttonY = panelY + 118;
            addAction("Singleplayer", "Open a local world", buttonX, buttonY, buttonWidth, buttonHeight,
                    false, () -> minecraft.setScreen(new SelectWorldScreen(this)));
            addAction("Multiplayer", "Join a server", buttonX, buttonY += buttonHeight + gap,
                    buttonWidth, buttonHeight, true, () -> minecraft.setScreen(new JoinMultiplayerScreen(this)));
            addAction("Jilaton menu", "Modules, themes and profiles", buttonX, buttonY += buttonHeight + gap,
                    buttonWidth, buttonHeight, false, () -> JilatonClient.openClickGui(this));
            addAction("Options", "Controls, video and audio", buttonX, buttonY += buttonHeight + gap,
                    buttonWidth, buttonHeight, false,
                    () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
            addAction("Quit", "Exit Minecraft", buttonX, buttonY += buttonHeight + gap,
                    buttonWidth, buttonHeight, false, minecraft::stop);
        }
    }

    private void addAction(String label, String subtitle, int x, int y, int buttonWidth, int buttonHeight,
                           boolean featured, Runnable action) {
        Button button = Button.builder(Component.literal(label), ignored -> action.run())
                .bounds(x, y, buttonWidth, buttonHeight)
                .build();
        button.setAlpha(0.0F);
        addRenderableWidget(button);
        actions.add(new MenuAction(label, subtitle, x, y, buttonWidth, buttonHeight, featured, button));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        double time = System.nanoTime() / 1_000_000_000.0;
        renderBackdrop(graphics, time);
        renderPanel(graphics);
        if (compact) {
            renderCompactBrand(graphics, time);
        } else {
            renderBrandColumn(graphics, time);
        }
        renderActions(graphics, mouseX, mouseY, time);
        renderFooter(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderBackdrop(GuiGraphics graphics, double time) {
        graphics.fill(0, 0, width, height, INK);
        int centerX = compact ? width / 2 : panelX + panelWidth / 3;
        int centerY = panelY + panelHeight / 2;
        int pulse = (int) (7 + (Math.sin(time * 0.52) + 1.0) * 4.0);
        int glow = (pulse << 24) | (accent & 0x00FFFFFF);
        graphics.fill(centerX - 190, centerY - 155, centerX + 190, centerY + 155, glow);
        graphics.fill(0, 0, width, 1, 0x302B3151);
        for (int x = 24; x < width; x += 64) {
            graphics.fill(x, 0, x + 1, height, 0x0A9CA6C8);
        }
        for (int y = 28; y < height; y += 64) {
            graphics.fill(0, y, width, y + 1, 0x089CA6C8);
        }
        int sweepX = (int) ((time * 24.0) % (width + 180)) - 90;
        graphics.fill(sweepX, 0, sweepX + 1, height, 0x149B83FF);
    }

    private void renderPanel(GuiGraphics graphics) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF262C42);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, accent);
        if (!compact) {
            int dividerX = panelX + Math.round(panelWidth * 0.54F) + 8;
            graphics.fill(dividerX, panelY + 20, dividerX + 1, panelY + panelHeight - 20, 0xFF22283A);
        }
    }

    private void renderBrandColumn(GuiGraphics graphics, double time) {
        int x = panelX + 34;
        int y = panelY + 32;
        renderLogo(graphics, x, y, 42, time);
        graphics.drawString(font, "JILATON", x, y + 68, 0xFFF0F1FA);
        graphics.drawString(font, "CLIENT  /  JAVA EDITION", x, y + 84, 0xFF9BA2B5);
        graphics.fill(x, y + 108, x + 34, y + 110, accent);
        graphics.drawString(font, "COMPETITIVE BY DESIGN", x, y + 128, 0xFFE1E3EC);
        graphics.drawString(font, "A focused PvP toolkit with", x, y + 149, MUTED);
        graphics.drawString(font, "your own play style at the center.", x, y + 164, MUTED);
        graphics.fill(x, panelY + panelHeight - 77, x + 5, panelY + panelHeight - 72, accent);
        graphics.drawString(font, "CLIENT BUILD  0.1.0", x + 14, panelY + panelHeight - 76, 0xFFC3C8D8);
        graphics.drawString(font, "FABRIC  /  MINECRAFT 1.21.11", x + 14, panelY + panelHeight - 58, MUTED);
        graphics.drawString(font, "Minimal interface. Full control.", x, panelY + panelHeight - 31, 0xFF737C92);
    }

    private void renderCompactBrand(GuiGraphics graphics, double time) {
        int logoSize = height < 360 ? 27 : 34;
        int y = panelY + (height < 360 ? 12 : 18);
        renderLogo(graphics, width / 2 - logoSize / 2, y, logoSize, time);
        graphics.drawCenteredString(font, "JILATON CLIENT", width / 2, y + logoSize + 5, 0xFFF0F1FA);
        graphics.drawCenteredString(font, "JAVA  /  PVP TOOLKIT", width / 2, y + logoSize + 19, MUTED);
    }

    private void renderLogo(GuiGraphics graphics, int x, int y, int size, double time) {
        int breathe = (int) (8 + (Math.sin(time * 0.8) + 1.0) * 5.0);
        int glow = (breathe << 24) | (accent & 0x00FFFFFF);
        graphics.fill(x - 5, y - 5, x + size + 5, y + size + 5, glow);
        graphics.fill(x, y, x + size, y + size, 0xFF211C38);
        graphics.fill(x + 5, y + 5, x + size - 5, y + 8, accent);
        graphics.fill(x + size - 8, y + 5, x + size - 5, y + size - 7, accent);
        graphics.fill(x + size / 2 - 2, y + size - 10, x + size - 6, y + size - 7, accent);
        graphics.fill(x + 5, y + size - 10, x + 8, y + size - 5, accent);
        graphics.fill(x + 7, y + size - 7, x + size / 2, y + size - 4, accent);
    }

    private void renderActions(GuiGraphics graphics, int mouseX, int mouseY, double time) {
        for (int index = 0; index < actions.size(); index++) {
            MenuAction action = actions.get(index);
            boolean hovered = mouseX >= action.x() && mouseX < action.x() + action.width()
                    && mouseY >= action.y() && mouseY < action.y() + action.height();
            boolean focused = action.button().isFocused();
            boolean active = hovered || focused;
            int background = active ? 0xFF191C2D : 0xFF111520;
            graphics.fill(action.x(), action.y(), action.x() + action.width(), action.y() + action.height(), background);
            graphics.fill(action.x(), action.y(), action.x() + (active || action.featured() ? 3 : 1),
                    action.y() + action.height(), active || action.featured() ? accent : 0xFF2A3040);
            graphics.fill(action.x() + 1, action.y(), action.x() + action.width() - 1, action.y() + 1,
                    active ? 0xFF3A3B57 : 0xFF202535);
            int textColor = action.label().equals("Quit") ? 0xFFB4A0AE : 0xFFF0F1F7;
            graphics.drawString(font, action.label(), action.x() + 15, action.y() + action.height() / 2 - 8, textColor);
            if (!compact) {
                graphics.drawString(font, action.subtitle(), action.x() + 15, action.y() + action.height() / 2 + 5,
                        active ? 0xFFB0B6C9 : MUTED);
            }
            int chevronColor = active ? accent : 0xFF626B80;
            graphics.fill(action.x() + action.width() - 19, action.y() + action.height() / 2,
                    action.x() + action.width() - 13, action.y() + action.height() / 2 + 1, chevronColor);
            if (active) {
                int marker = (int) ((Math.sin(time * 3.0 + index) + 1.0) * 1.5);
                graphics.fill(action.x() + action.width() - 25 - marker, action.y() + action.height() / 2 - 1,
                        action.x() + action.width() - 22 - marker, action.y() + action.height() / 2 + 2, accent);
            }
        }
    }

    private void renderFooter(GuiGraphics graphics) {
        String footer = "JILATON  ·  FABRIC CLIENT";
        graphics.drawString(font, footer, panelX + 16, panelY + panelHeight - 14, 0xFF717A90);
        String status = "READY";
        int statusWidth = font.width(status);
        graphics.fill(panelX + panelWidth - statusWidth - 31, panelY + panelHeight - 14,
                panelX + panelWidth - statusWidth - 26, panelY + panelHeight - 9, accent);
        graphics.drawString(font, status, panelX + panelWidth - statusWidth - 20,
                panelY + panelHeight - 16, 0xFFD4D8E3);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private record MenuAction(String label, String subtitle, int x, int y, int width, int height,
                              boolean featured, Button button) {
    }
}
