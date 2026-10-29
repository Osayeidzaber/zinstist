package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.config.ClientConfig.HudPosition;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.module.impl.HudModule;
import dev.jilaton.client.module.impl.KillAuraModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class HudRenderer {
    private static final int BACKGROUND = 0xB0101118;
    private final ModuleManager modules;
    private final ClientConfig config;
    private final List<ClientModule> sortedModules;
    private final List<ClientModule> activeModuleScratch = new ArrayList<>();
    private final List<WidgetBounds> lastBounds = new ArrayList<>();
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
        renderInternal(graphics, false);
    }

    public void renderForEditor(GuiGraphics graphics) {
        renderInternal(graphics, true);
    }

    public List<WidgetBounds> lastBounds() {
        return List.copyOf(lastBounds);
    }

    private void renderInternal(GuiGraphics graphics, boolean editorPreview) {
        Minecraft client = Minecraft.getInstance();
        ClientModule module = modules.get("hud");
        lastBounds.clear();
        if (client.player == null) {
            return;
        }

        Font font = client.font;
        int accent = ClientTheme.resolveAccent(config.themeId(), config.customAccent());
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();
        ClientModule auraModule = modules.get("kill_aura");
        LivingEntity target = null;
        boolean targetRecentlyAttacked = false;
        if (auraModule instanceof KillAuraModule aura && aura.enabled()) {
            LivingEntity candidate = aura.currentTarget();
            if (candidate != null && candidate.isAlive() && candidate.level() == client.level) {
                target = candidate;
                targetRecentlyAttacked = aura.recentlyAttacked(candidate);
                if (client.screen == null && aura.showTargetIndicator()) {
                    renderTargetRing(graphics, client, target, accent, guiWidth, guiHeight,
                            targetRecentlyAttacked);
                }
            }
        }

        if (module instanceof HudModule hud && hud.enabled() && hud.targetHud()) {
            if (editorPreview) {
                renderTargetCard(graphics, font, client, target == null ? client.player : target,
                        accent, guiWidth, guiHeight, true, target != null && targetRecentlyAttacked);
            } else if (target != null && client.screen == null) {
                renderTargetCard(graphics, font, client, target, accent, guiWidth, guiHeight,
                        false, targetRecentlyAttacked);
            }
        }

        if (!(module instanceof HudModule hud) || (!editorPreview && !hud.enabled())
                || (!editorPreview && client.screen instanceof HudEditorScreen)) {
            return;
        }

        int moduleListTop = 8;
        if (hud.watermark()) {
            String version = "JILATON";
            int width = font.width(version) + 20;
            int x = widgetX("watermark", 8, width, guiWidth);
            int y = widgetY("watermark", 8, 22, guiHeight);
            drawPanel(graphics, x, y, width, 22);
            graphics.fill(x, y, x + 2, y + 22, accent);
            graphics.drawString(font, version, x + 8, y + 7, 0xFFF4F1FA);
            addBounds(editorPreview, "watermark", x, y, width, 22);
            moduleListTop = y + 28;
        }

        if (hud.activeModules()) {
            activeModuleScratch.clear();
            int listWidth = 0;
            for (ClientModule active : sortedModules) {
                if (active == hud || !active.enabled()) {
                    continue;
                }
                activeModuleScratch.add(active);
                listWidth = Math.max(listWidth, font.width(active.name()) + 18);
            }
            if (!activeModuleScratch.isEmpty()) {
                int listHeight = activeModuleScratch.size() * 20 - 2;
                int x = widgetX("modules", 8, listWidth, guiWidth);
                int y = widgetY("modules", moduleListTop, listHeight, guiHeight);
                for (ClientModule active : activeModuleScratch) {
                    String label = active.name();
                    int width = font.width(label) + 18;
                    drawPanel(graphics, x, y, width, 18);
                    graphics.fill(x, y, x + 2, y + 18, accent);
                    graphics.drawString(font, label, x + 7, y + 5, 0xFFE3E0EC);
                    y += 20;
                }
                addBounds(editorPreview, "modules", x, y - listHeight - 2, listWidth, listHeight);
            }
        }

        if (hud.performance()) {
            String stats = "FPS " + client.getFps() + "  ·  " + ping(client);
            int width = font.width(stats) + 18;
            int x = widgetX("performance", guiWidth - width - 8, width, guiWidth);
            int y = widgetY("performance", 8, 22, guiHeight);
            drawPanel(graphics, x, y, width, 22);
            graphics.drawString(font, stats, x + 9, y + 7, 0xFFE3E0EC);
            addBounds(editorPreview, "performance", x, y, width, 22);
        }

        Player player = client.player;
        if (hud.coordinates()) {
            String coordinates = coordinates(player);
            int width = font.width(coordinates) + 18;
            int x = widgetX("coordinates", 8, width, guiWidth);
            int y = widgetY("coordinates", guiHeight - 29, 21, guiHeight);
            drawPanel(graphics, x, y, width, 21);
            graphics.drawString(font, coordinates, x + 9, y + 7, 0xFFE3E0EC);
            addBounds(editorPreview, "coordinates", x, y, width, 21);
        }

        if (hud.keystrokes()) {
            int x = widgetX("keystrokes", 8, 70, guiWidth);
            int y = widgetY("keystrokes", guiHeight - 104, 58, guiHeight);
            renderKeystrokes(graphics, client, accent, x, y);
            addBounds(editorPreview, "keystrokes", x, y, 70, 58);
        }
    }

    private void addBounds(boolean editorPreview, String id, int x, int y, int width, int height) {
        if (editorPreview) {
            lastBounds.add(new WidgetBounds(id, x, y, width, height));
        }
    }

    private static void renderTargetRing(GuiGraphics graphics, Minecraft client, LivingEntity target,
                                         int accent, int guiWidth, int guiHeight, boolean recentlyAttacked) {
        Vec3 difference = target.getBoundingBox().getCenter().subtract(client.player.getEyePosition());
        double distance = difference.length();
        if (distance < 0.01) {
            return;
        }

        double horizontal = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
        float targetYaw = (float) (Math.atan2(difference.z, difference.x) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-Math.atan2(difference.y, horizontal) * (180.0 / Math.PI));
        float yawDelta = Mth.wrapDegrees(targetYaw - client.player.getYRot());
        float pitchDelta = targetPitch - client.player.getXRot();
        double fovY = Math.toRadians(client.options.fov().get().doubleValue());
        double tangentY = Math.tan(fovY * 0.5);
        double tangentX = tangentY * guiWidth / (double) guiHeight;
        if (tangentY <= 0.0 || tangentX <= 0.0
                || Math.abs(Math.toRadians(pitchDelta)) >= fovY * 0.5
                || Math.abs(Math.toRadians(yawDelta)) >= Math.atan(tangentX)) {
            return;
        }

        int centerX = (int) Math.round(guiWidth * 0.5
                + Math.tan(Math.toRadians(yawDelta)) / tangentX * guiWidth * 0.5);
        int centerY = (int) Math.round(guiHeight * 0.5
                + Math.tan(Math.toRadians(pitchDelta)) / tangentY * guiHeight * 0.5);
        double pixelsPerBlock = guiHeight / (2.0 * tangentY * distance);
        int radius = Mth.clamp((int) Math.round(target.getBbHeight() * pixelsPerBlock * 0.28), 12, 52);
        double beat = Math.sin(System.nanoTime() / 260_000_000.0);
        double pulse = recentlyAttacked ? 1.07 + beat * 0.10 : 1.0 + beat * 0.055;
        int outerRadius = (int) Math.round(radius * pulse);
        int ringColor = recentlyAttacked ? 0xFFFFD36A : accent;

        for (int point = 0; point < 48; point++) {
            double angle = point * (Math.PI * 2.0 / 48.0);
            int x = centerX + (int) Math.round(Math.cos(angle) * outerRadius);
            int y = centerY + (int) Math.round(Math.sin(angle) * outerRadius * 1.12);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, 0xFF000000);
            graphics.fill(x, y, x + 1, y + 1, ringColor);
        }

        String marker = recentlyAttacked ? "SWING" : "LOCKED";
        Font font = client.font;
        int markerWidth = font.width(marker) + 12;
        int markerX = centerX - markerWidth / 2;
        int markerY = centerY - outerRadius - 18;
        graphics.fill(markerX, markerY, markerX + markerWidth, markerY + 14, BACKGROUND);
        graphics.fill(markerX, markerY, markerX + 2, markerY + 14, ringColor);
        graphics.drawString(font, marker, markerX + 7, markerY + 3,
                recentlyAttacked ? ringColor : 0xFFF4F1FA);
    }

    private void renderTargetCard(GuiGraphics graphics, Font font, Minecraft client, LivingEntity target,
                                  int accent, int guiWidth, int guiHeight,
                                  boolean editorPreview, boolean recentlyAttacked) {
        int width = 210;
        int height = 45;
        int x = widgetX("target", (guiWidth - width) / 2, width, guiWidth);
        int y = widgetY("target", guiHeight - 63, height, guiHeight);
        drawPanel(graphics, x, y, width, height);
        graphics.fill(x, y, x + 2, y + height, accent);
        graphics.drawString(font, "TARGET LOCK", x + 43, y + 5, accent);
        if (recentlyAttacked) {
            graphics.fill(x + 153, y + 3, x + 202, y + 15, 0xFF332A1A);
            graphics.drawString(font, "SWING", x + 162, y + 5, 0xFFFFD36A);
        }

        int portraitLeft = x + 5;
        int portraitTop = y + 5;
        graphics.fill(portraitLeft, portraitTop, portraitLeft + 34, portraitTop + 35, 0xFF191A24);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                portraitLeft, portraitTop, portraitLeft + 34, portraitTop + 35,
                17, portraitLeft + 17.0F, portraitTop + 18.0F, 1.0F, target);

        String name = target.getName().getString();
        int maxNameWidth = width - 100;
        while (!name.isEmpty() && font.width(name) > maxNameWidth) {
            name = name.substring(0, name.length() - 1);
        }
        if (!name.equals(target.getName().getString())) {
            while (!name.isEmpty() && font.width(name + "…") > maxNameWidth) {
                name = name.substring(0, name.length() - 1);
            }
            name += "…";
        }
        graphics.drawString(font, name, x + 43, y + 17, 0xFFF4F1FA);

        float health = Math.max(0.0F, target.getHealth());
        float maxHealth = Math.max(1.0F, target.getMaxHealth());
        float healthRatio = Mth.clamp(health / maxHealth, 0.0F, 1.0F);
        int barX = x + 145;
        int barY = y + 27;
        int barWidth = 55;
        graphics.fill(barX, barY, barX + barWidth, barY + 5, 0xFF303344);
        graphics.fill(barX, barY, barX + Math.round(barWidth * healthRatio), barY + 5,
                healthRatio > 0.5F ? accent : 0xFFFF647C);

        double distance = Math.sqrt(client.player.distanceToSqr(target));
        String stats = String.format(Locale.ROOT, "%.1f HP  ·  %.1fm", health, distance);
        graphics.drawString(font, stats, x + 43, y + 35, 0xFFB6BED1);
        addBounds(editorPreview, "target", x, y, width, height);
    }

    private int widgetX(String widgetId, int defaultX, int widgetWidth, int guiWidth) {
        HudPosition position = config.hudPosition(widgetId);
        return position == null ? defaultX : (int) Math.round(Math.max(0, guiWidth - widgetWidth) * position.x());
    }

    private int widgetY(String widgetId, int defaultY, int widgetHeight, int guiHeight) {
        HudPosition position = config.hudPosition(widgetId);
        return position == null ? defaultY : (int) Math.round(Math.max(0, guiHeight - widgetHeight) * position.y());
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

    private static void renderKeystrokes(GuiGraphics graphics, Minecraft client, int accent, int x, int y) {
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

    public record WidgetBounds(String id, int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
