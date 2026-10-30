package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.config.ClientConfig.HudPosition;
import dev.jilaton.client.config.HudStyle;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class HudRenderer {
    private static final int BACKGROUND = 0xB0101118;
    private HudStyle activeStyle = HudStyle.DEFAULT;
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] ARMOR_LABELS = {"HEAD", "BODY", "LEGS", "FEET"};
    private final List<EffectRow> effectRows = new ArrayList<>();
    private long lastEffectTick = Long.MIN_VALUE;
    private Object lastLevel;
    private HudLayout activeLayout;
    private final AnimatedValue targetHealth = new AnimatedValue();
    private java.util.UUID lastTargetId;
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
            effectRows.clear();
            lastEffectTick = Long.MIN_VALUE;
            lastLevel = null;
            HudInputMetrics.clear();
            modules.notifications().clear();
            targetHealth.reset();
            lastTargetId = null;
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

            }
        }

        if (module instanceof HudModule hud && (hud.enabled() || editorPreview) && hud.targetHud()
                && visible("target", editorPreview)
                && (editorPreview || !(client.screen instanceof HudEditorScreen))) {
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
        if (hud.watermark() && visible("watermark", editorPreview)) {
            String version = "JILATON";
            int width = font.width(version) + 20;
            int x = widgetX("watermark", 8, width, guiWidth);
            int y = widgetY("watermark", 8, 22, guiHeight);
            beginWidget(graphics, "watermark", x, y, width, 22, editorPreview);
            accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
            drawPanel(graphics, x, y, width, 22);
            graphics.fill(x, y, x + 2, y + 22, accent);
            graphics.drawString(font, version, x + 8, y + 7, 0xFFF4F1FA);
            graphics.pose().popMatrix();
            moduleListTop = activeLayout.y() + activeLayout.height() + 6;
        }

        if (hud.activeModules() && visible("modules", editorPreview)) {
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
                beginWidget(graphics, "modules", x, y, listWidth, listHeight, editorPreview);
                accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
                for (ClientModule active : activeModuleScratch) {
                    String label = active.name();
                    int width = font.width(label) + 18;
                    drawPanel(graphics, x, y, width, 18);
                    graphics.fill(x, y, x + 2, y + 18, accent);
                    graphics.drawString(font, label, x + 7, y + 5, 0xFFE3E0EC);
                    y += 20;
                }
                graphics.pose().popMatrix();
            }
        }

        if (hud.performance() && visible("performance", editorPreview)) {
            String stats = "FPS " + client.getFps() + "  ·  " + ping(client);
            int width = font.width(stats) + 18;
            int x = widgetX("performance", guiWidth - width - 8, width, guiWidth);
            int y = widgetY("performance", 8, 22, guiHeight);
            beginWidget(graphics, "performance", x, y, width, 22, editorPreview);
            accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
            drawPanel(graphics, x, y, width, 22);
            graphics.drawString(font, stats, x + 9, y + 7, 0xFFE3E0EC);
            graphics.pose().popMatrix();
        }

        Player player = client.player;
        if (hud.coordinates() && visible("coordinates", editorPreview)) {
            String coordinates = coordinates(player);
            int width = font.width(coordinates) + 18;
            int x = widgetX("coordinates", 8, width, guiWidth);
            int y = widgetY("coordinates", guiHeight - 29, 21, guiHeight);
            beginWidget(graphics, "coordinates", x, y, width, 21, editorPreview);
            accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
            drawPanel(graphics, x, y, width, 21);
            graphics.drawString(font, coordinates, x + 9, y + 7, 0xFFE3E0EC);
            graphics.pose().popMatrix();
        }

        if (hud.keystrokes() && visible("keystrokes", editorPreview)) {
            int x = widgetX("keystrokes", 8, 70, guiWidth);
            int y = widgetY("keystrokes", guiHeight - 104, 58, guiHeight);
            beginWidget(graphics, "keystrokes", x, y, 70, 58, editorPreview);
            accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
            renderKeystrokes(graphics, client, accent, x, y);
            graphics.pose().popMatrix();
        }

        if (hud.armor() && visible("armor", editorPreview)) {
            renderArmor(graphics, client, editorPreview);
        }
        if (hud.effects() && visible("effects", editorPreview)) {
            renderEffects(graphics, client, editorPreview);
        }
        if (hud.cps() && visible("cps", editorPreview)) {
            long now = System.nanoTime();
            String label = "CPS  " + HudInputMetrics.left(now) + " L  /  " + HudInputMetrics.right(now) + " R";
            int width = font.width(label) + 18;
            int x = widgetX("cps", guiWidth - width - 8, width, guiWidth);
            int y = widgetY("cps", 36, 22, guiHeight);
            beginWidget(graphics, "cps", x, y, width, 22, editorPreview);
            drawPanel(graphics, x, y, width, 22);
            graphics.drawString(font, label, x + 9, y + 7, activeStyle.accentColor(accent));
            graphics.pose().popMatrix();
        }
        if (hud.notifications() && visible("notifications", editorPreview)) {
            renderNotifications(graphics, client, editorPreview);
        }
    }

    private void renderNotifications(GuiGraphics graphics, Minecraft client, boolean preview) {
        long now = System.nanoTime();
        var notices = modules.notifications().current(now);
        if (notices.isEmpty() && !preview) return;
        int width = 174;
        int rows = Math.max(1, notices.size());
        int height = rows * 26 - 3;
        int x = widgetX("notifications", graphics.guiWidth() - width - 8, width, graphics.guiWidth());
        int y = widgetY("notifications", graphics.guiHeight() / 2, height, graphics.guiHeight());
        beginWidget(graphics, "notifications", x, y, width, height, preview);
        int accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
        for (int index = 0; index < rows; index++) {
            var notice = notices.isEmpty() ? null : notices.get(index);
            String label = notice == null ? "Module notifications" : notice.text();
            int top = y + index * 26;
            int color = notice == null || notice.enabled() ? accent : 0xFF9CA6BC;
            drawPanel(graphics, x, top, width, 23);
            graphics.fill(x, top, x + 2, top + 23, color);
            graphics.drawString(client.font, client.font.plainSubstrByWidth(label, width - 16), x + 8, top + 6, 0xFFE1DEEC);
            double remaining = notice == null ? 1.0 : Math.max(0, 1.0
                    - (now - notice.createdAt()) / (double) dev.jilaton.client.notification.NotificationCenter.LIFETIME_NANOS);
            graphics.fill(x + 2, top + 21, x + 2 + (int) Math.round((width - 2) * remaining), top + 23, color);
        }
        graphics.pose().popMatrix();
    }

    private void renderArmor(GuiGraphics graphics, Minecraft client, boolean preview) {
        int width = 130;
        int height = 50;
        int x = widgetX("armor", graphics.guiWidth() - width - 8, width, graphics.guiWidth());
        int y = widgetY("armor", graphics.guiHeight() - 100, height, graphics.guiHeight());
        beginWidget(graphics, "armor", x, y, width, height, preview);
        drawPanel(graphics, x, y, width, height);
        int accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
        graphics.drawString(client.font, "ARMOR", x + 6, y + 5, accent);
        for (int index = 0; index < ARMOR_SLOTS.length; index++) {
            ItemStack stack = client.player.getItemBySlot(ARMOR_SLOTS[index]);
            int tileX = x + 6 + index * 31;
            if (stack.isEmpty()) {
                graphics.drawString(client.font, "--", tileX + 7, y + 20, 0xFF697084);
            } else {
                graphics.renderItem(stack, tileX + 5, y + 16);
                if (stack.isDamageableItem()) {
                    double remaining = Math.max(0, 1.0 - stack.getDamageValue() / (double) Math.max(1, stack.getMaxDamage()));
                    graphics.fill(tileX, y + 34, tileX + 27, y + 37, 0xFF303344);
                    graphics.fill(tileX, y + 34, tileX + (int) Math.round(27 * remaining), y + 37,
                            remaining < 0.2 ? 0xFFFF647C : accent);
                }
            }
            graphics.drawString(client.font, ARMOR_LABELS[index], tileX, y + 39, 0xFF9CA6BC);
        }
        graphics.pose().popMatrix();
    }

    private void renderEffects(GuiGraphics graphics, Minecraft client, boolean preview) {
        long tick = client.level.getGameTime();
        if (tick != lastEffectTick || lastLevel != client.level) {
            lastEffectTick = tick;
            lastLevel = client.level;
            effectRows.clear();
            for (MobEffectInstance effect : client.player.getActiveEffects()) {
                String name = Component.translatable(effect.getEffect().value().getDescriptionId()).getString()
                        + (effect.getAmplifier() == 0 ? "" : " " + (effect.getAmplifier() + 1));
                int seconds = Math.max(0, (int) Math.ceil(effect.getDuration() / 20.0));
                String duration = effect.isInfiniteDuration() ? "∞" : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
                effectRows.add(new EffectRow(name, duration));
            }
            effectRows.sort(Comparator.comparing(EffectRow::name));
        }
        if (effectRows.isEmpty() && !preview) return;
        int width = 148;
        int maxRows = Math.max(1, (graphics.guiHeight() - 24) / 14);
        int rowCount = Math.max(1, Math.min(maxRows, effectRows.size()));
        int height = 21 + rowCount * 14;
        int x = widgetX("effects", graphics.guiWidth() - width - 8, width, graphics.guiWidth());
        int y = widgetY("effects", 64, height, graphics.guiHeight());
        beginWidget(graphics, "effects", x, y, width, height, preview);
        drawPanel(graphics, x, y, width, height);
        graphics.drawString(client.font, "POTION EFFECTS", x + 6, y + 6,
                activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent())));
        for (int index = 0; index < rowCount; index++) {
            EffectRow row = effectRows.isEmpty() ? new EffectRow("No active effects", "") : effectRows.get(index);
            int durationWidth = client.font.width(row.duration());
            String name = client.font.plainSubstrByWidth(row.name(), width - durationWidth - 20);
            graphics.drawString(client.font, name, x + 6, y + 21 + index * 14, 0xFFE1DEEC);
            graphics.drawString(client.font, row.duration(), x + width - durationWidth - 6, y + 21 + index * 14, 0xFF9CA6BC);
        }
        graphics.pose().popMatrix();
    }

    private record EffectRow(String name, String duration) { }

    private boolean visible(String id, boolean preview) {
        return preview || config.hudStyle(id).visible();
    }

    private void beginWidget(GuiGraphics graphics, String id, int x, int y, int width, int height,
                             boolean preview) {
        activeStyle = config.hudStyle(id);
        HudLayout layout = HudLayout.resolve(width, height, graphics.guiWidth(), graphics.guiHeight(),
                x, y, config.hudPosition(id), activeStyle);
        activeLayout = layout;
        if (preview) {
            lastBounds.add(new WidgetBounds(id, layout.x(), layout.y(), layout.width(), layout.height()));
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) layout.x(), (float) layout.y());
        graphics.pose().scale(layout.scale(), layout.scale());
        graphics.pose().translate((float) -x, (float) -y);
    }

    private void renderTargetCard(GuiGraphics graphics, Font font, Minecraft client, LivingEntity target,
                                  int accent, int guiWidth, int guiHeight,
                                  boolean editorPreview, boolean recentlyAttacked) {
        int width = 218;
        int height = 56;
        int x = widgetX("target", (guiWidth - width) / 2, width, guiWidth);
        int y = widgetY("target", guiHeight - 76, height, guiHeight);
        beginWidget(graphics, "target", x, y, width, height, editorPreview);
        // Neutral black surfaces and restrained silver details match the primary Jilaton identity.
        int silver = activeStyle.accent() == null ? 0xFFE3E5EA : activeStyle.accentColor(accent);
        int surface = ((int)Math.round(activeStyle.panelOpacity() * 255) << 24) | 0x090A0C;
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, surface);
        graphics.fill(x + 7, y, x + width - 7, y + 1, 0x554A4D53);
        graphics.fill(x + 7, y + height - 1, x + width - 7, y + height, 0x33373A40);
        graphics.fill(x, y + 7, x + 1, y + height - 7, 0x33373A40);
        graphics.fill(x + width - 1, y + 7, x + width, y + height - 7, 0x33373A40);
        graphics.fill(x + 1, y + 1, x + 7, y + 2, 0x554A4D53);
        graphics.fill(x + width - 7, y + 1, x + width - 1, y + 2, 0x554A4D53);
        graphics.fill(x + 7, y + 9, x + 44, y + 47, 0xFF111215);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                x + 8, y + 9, x + 43, y + 47, 18, 0.0625F, x + 25.5F, y + 27.0F, target);
        graphics.drawString(font, "TARGET / 01", x + 53, y + 8, 0xFF767A83);
        String name = font.plainSubstrByWidth(target.getName().getString(), 146);
        graphics.drawString(font, name, x + 53, y + 20, 0xFFF1F2F4);
        float health = Math.max(0.0F, target.getHealth());
        float maxHealth = Math.max(1.0F, target.getMaxHealth());
        float healthRatio = Mth.clamp(health / maxHealth, 0.0F, 1.0F);
        if (!target.getUUID().equals(lastTargetId)) {
            targetHealth.reset();
            lastTargetId = target.getUUID();
        }
        float displayedHealth = (float) targetHealth.update(healthRatio, System.nanoTime(), config.reducedMotion());
        int barX = x + 53, barY = y + 35, barWidth = 154;
        graphics.fill(barX, barY, barX + barWidth, barY + 3, 0xFF25272D);
        graphics.fill(barX, barY, barX + Math.round(barWidth * displayedHealth), barY + 3, silver);
        double distance = Math.sqrt(client.player.distanceToSqr(target));
        String stats = String.format(Locale.ROOT, "%.1f HP", health);
        String meters = String.format(Locale.ROOT, "%.1f M", distance);
        graphics.drawString(font, stats, barX, y + 43, 0xFF9599A2);
        graphics.drawString(font, meters, x + width - font.width(meters) - 11, y + 43, 0xFF9599A2);
        // A tiny neutral status light gives attack feedback without a flashing label or color change.
        graphics.fill(x + width - 13, y + 9, x + width - 10, y + 12, recentlyAttacked ? silver : 0xFF42464E);
        graphics.pose().popMatrix();
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

    private void renderKeystrokes(GuiGraphics graphics, Minecraft client, int accent, int x, int y) {
        Font font = client.font;
        drawKey(graphics, font, client.options.keyUp, x + 24, y, 22, 19, accent);
        drawKey(graphics, font, client.options.keyLeft, x, y + 21, 22, 19, accent);
        drawKey(graphics, font, client.options.keyDown, x + 24, y + 21, 22, 19, accent);
        drawKey(graphics, font, client.options.keyRight, x + 48, y + 21, 22, 19, accent);
        drawKey(graphics, font, client.options.keyJump, x, y + 42, 70, 16, accent);
    }

    private void drawKey(GuiGraphics graphics, Font font, net.minecraft.client.KeyMapping mapping,
                                int x, int y, int width, int height, int accent) {
        boolean down = mapping.isDown();
        graphics.fill(x, y, x + width, y + height, down ? (activeStyle.panelColor() & 0xFF000000) | 0x24203D : activeStyle.panelColor());
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

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, activeStyle.panelColor());
        graphics.fill(x, y, x + 2, y + height,
                activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent())));
    }

    public record WidgetBounds(String id, int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
