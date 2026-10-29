package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.module.impl.HudModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class HudRenderer {
    private static final int BACKGROUND = 0xB0101118;
    private final ModuleManager modules;
    private final ClientConfig config;
    private final List<ClientModule> sortedModules;
    private int lastCoordinateX = Integer.MIN_VALUE;
    private int lastCoordinateY = Integer.MIN_VALUE;
    private int lastCoordinateZ = Integer.MIN_VALUE;
    private String cachedCoordinates = "";

    public HudRenderer(ModuleManager modules, ClientConfig config) {
        this.modules = modules;
        this.config = config;
        sortedModules = new ArrayList<>(modules.all());
        sortedModules.sort(Comparator.comparing(ClientModule::name, String.CASE_INSENSITIVE_ORDER));
    }

    public void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        ClientModule module = modules.get("hud");
        if (!(module instanceof HudModule hud) || !hud.enabled() || client.player == null) {
            return;
        }

        Font font = client.font;
        int accent = ClientTheme.resolveAccent(config.themeId(), config.customAccent());
        int top = 8;
        if (hud.watermark()) {
            String version = "JILATON";
            int width = font.width(version) + 20;
            drawPanel(graphics, 8, top, width, 22);
            graphics.fill(8, top, 10, top + 22, accent);
            graphics.drawString(font, version, 16, top + 7, 0xFFF4F1FA);
            top += 28;
        }

        if (hud.activeModules()) {
            for (ClientModule active : sortedModules) {
                if (active == hud || !active.enabled()) {
                    continue;
                }
                String label = active.name();
                int width = font.width(label) + 18;
                drawPanel(graphics, 8, top, width, 18);
                graphics.fill(8, top, 10, top + 18, accent);
                graphics.drawString(font, label, 15, top + 5, 0xFFE3E0EC);
                top += 20;
            }
        }

        if (hud.performance()) {
            String stats = "FPS " + client.getFps() + "  ·  " + ping(client);
            int width = font.width(stats) + 18;
            int x = graphics.guiWidth() - width - 8;
            drawPanel(graphics, x, 8, width, 22);
            graphics.drawString(font, stats, x + 9, 15, 0xFFE3E0EC);
        }

        Player player = client.player;
        if (hud.coordinates()) {
            String coordinates = coordinates(player);
            int width = font.width(coordinates) + 18;
            int x = 8;
            int y = graphics.guiHeight() - 29;
            drawPanel(graphics, x, y, width, 21);
            graphics.drawString(font, coordinates, x + 9, y + 7, 0xFFE3E0EC);
        }

        if (hud.keystrokes()) {
            renderKeystrokes(graphics, client, accent);
        }
    }

    private static String ping(Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            return "PING --";
        }
        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        return info == null ? "PING --" : "PING " + info.getLatency() + "ms";
    }

    private String coordinates(Player player) {
        int x = (int) Math.floor(player.getX() * 10.0);
        int y = (int) Math.floor(player.getY() * 10.0);
        int z = (int) Math.floor(player.getZ() * 10.0);
        if (x != lastCoordinateX || y != lastCoordinateY || z != lastCoordinateZ) {
            lastCoordinateX = x;
            lastCoordinateY = y;
            lastCoordinateZ = z;
            cachedCoordinates = String.format(Locale.ROOT, "X %.1f   Y %.1f   Z %.1f", x / 10.0, y / 10.0, z / 10.0);
        }
        return cachedCoordinates;
    }

    private static void renderKeystrokes(GuiGraphics graphics, Minecraft client, int accent) {
        int x = 8;
        int y = graphics.guiHeight() - 104;
        Font font = client.font;
        drawKey(graphics, font, client.options.keyUp, x + 24, y, 22, 19, accent);
        drawKey(graphics, font, client.options.keyLeft, x, y + 21, 22, 19, accent);
        drawKey(graphics, font, client.options.keyDown, x + 24, y + 21, 22, 19, accent);
        drawKey(graphics, font, client.options.keyRight, x + 48, y + 21, 22, 19, accent);
        drawKey(graphics, font, client.options.keyJump, x, y + 42, 70, 16, accent);
    }

    private static void drawKey(GuiGraphics graphics, Font font, net.minecraft.client.KeyMapping mapping,
                                int x, int y, int width, int height, int accent) {
        boolean down = mapping.isDown();
        graphics.fill(x, y, x + width, y + height, down ? 0xCC24203D : BACKGROUND);
        if (down) {
            graphics.fill(x, y, x + width, y + 2, accent);
        }
        String label = mapping.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
        if (width <= 22 && label.length() > 3) {
            label = label.substring(0, 3);
        }
        int labelX = x + (width - font.width(label)) / 2;
        graphics.drawString(font, label, labelX, y + (height - 8) / 2, down ? 0xFFF4F1FA : 0xFF9AA3B9);
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, BACKGROUND);
    }
}
