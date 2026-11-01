package dev.jilaton.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.config.ClientConfig.HudPosition;
import dev.jilaton.client.config.HudStyle;
import dev.jilaton.client.gui.hud.HudDesign;
import dev.jilaton.client.gui.hud.HudNumberPainter;
import dev.jilaton.client.gui.hud.HudNumbers;
import dev.jilaton.client.gui.hud.NumericCrossfade;
import dev.jilaton.client.gui.hud.RadarMath;
import dev.jilaton.client.gui.hud.Speedometer;
import dev.jilaton.client.gui.hud.StaffClassifier;
import dev.jilaton.client.gui.hud.HudText;
import dev.jilaton.client.gui.hud.VanishRetention;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.module.impl.HudModule;
import dev.jilaton.client.module.impl.KillAuraModule;
import dev.jilaton.client.render.CombatTargetTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HudRenderer {
    private static final int TEXT_PRIMARY = 0xFFF2F0F5;
    private static final int TEXT_SECONDARY = 0xFF8E8A9A;
    private static final int HAIRLINE = 0xFF262231;
    private static final int DANGER = 0xFFE5484D;
    private HudStyle activeStyle = HudStyle.DEFAULT;
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] ARMOR_LABELS = {"HEAD", "BODY", "LEGS", "FEET"};
    private static final String[] CHIP_LABELS = {"COMBO", "HITS", "LEAD"};
    private final List<EffectRow> effectRows = new ArrayList<>();
    private long lastEffectTick = Long.MIN_VALUE;
    private Object lastLevel;
    private HudLayout activeLayout;
    private final AnimatedValue targetHealth = new AnimatedValue();
    private final AnimatedValue aboveHeadPresence = new AnimatedValue();
    private final AnimatedValue heldItemPresence = new AnimatedValue();
    private final TargetCardPlacement targetPlacement = new TargetCardPlacement();
    private final VanishRetention retention = new VanishRetention(5_000);
    private final Speedometer speedometer = new Speedometer();
    private final Map<UUID, StaffRow> staffRows = new LinkedHashMap<>();
    private final Map<UUID, RadarBlip> radarBlips = new LinkedHashMap<>();
    private long retentionWindowMillis = 5_000;
    private static final double TARGET_MEMORY_SECONDS = 6.0;
    private static final double TARGET_MAX_DISTANCE_SQR = 12.0 * 12.0;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private dev.jilaton.client.render.WorldOverlayProjection projection;
    private float partialTick;
    private java.util.UUID lastTargetId;
    private final AnimatedValue sideBarHealth = new AnimatedValue();
    private final dev.jilaton.client.render.FightStats fightStats = new dev.jilaton.client.render.FightStats();
    private final ModuleManager modules;
    private final ClientConfig config;
    private final List<ClientModule> sortedModules;
    private final List<ClientModule> activeModuleScratch = new ArrayList<>();
    private final List<WidgetBounds> lastBounds = new ArrayList<>();
    private HudNumberPainter numberPainter;
    private Font numberFont;
    private final IntText fpsText = new IntText();
    private final IntText pingText = new IntText();
    private final IntText armorPercentText = new IntText();
    private final IntText radarRangeText = new IntText();
    private final IntText staffCountText = new IntText();
    private final IntText cpsLeftText = new IntText();
    private final IntText cpsRightText = new IntText();
    private final IntText serverPingText = new IntText();
    private final IntText serverPlayersText = new IntText();
    private final TenthsText coordinateX = new TenthsText();
    private final TenthsText coordinateY = new TenthsText();
    private final TenthsText coordinateZ = new TenthsText();
    private final TenthsText speedText = new TenthsText();
    private final ClockText clockText = new ClockText();
    private final NumericCrossfade fpsFade = new NumericCrossfade();
    private final NumericCrossfade pingFade = new NumericCrossfade();
    private final NumericCrossfade armorFade = new NumericCrossfade();
    private final NumericCrossfade rangeFade = new NumericCrossfade();
    private final NumericCrossfade staffFade = new NumericCrossfade();
    private final NumericCrossfade cpsLeftFade = new NumericCrossfade();
    private final NumericCrossfade cpsRightFade = new NumericCrossfade();
    private final NumericCrossfade serverPingFade = new NumericCrossfade();
    private final NumericCrossfade serverPlayersFade = new NumericCrossfade();
    private final NumericCrossfade clockFade = new NumericCrossfade();
    private final NumericCrossfade coordinateXFade = new NumericCrossfade();
    private final NumericCrossfade coordinateYFade = new NumericCrossfade();
    private final NumericCrossfade coordinateZFade = new NumericCrossfade();
    private final NumericCrossfade speedFade = new NumericCrossfade();
    private final Map<String, NumericCrossfade> effectFades = new LinkedHashMap<>();
    private static final String HEART_MARK = "\u2665 ";
    private final TenthsText targetDistanceValue = new TenthsText();
    private final TenthsText targetHealthValue = new TenthsText();
    private final TenthsText compactDistanceValue = new TenthsText();
    private final TenthsText aboveHeadHealthValue = new TenthsText();
    private final TenthsText aboveHeadDistanceValue = new TenthsText();
    private final IntText chipComboValue = new IntText();
    private final IntText aboveHeadArmorValue = new IntText();
    private final IntText heldCountValue = new IntText();
    private final NumericCrossfade targetDistanceFade = new NumericCrossfade();
    private final NumericCrossfade targetHealthFade = new NumericCrossfade();
    private final NumericCrossfade compactDistanceFade = new NumericCrossfade();
    private final NumericCrossfade aboveHeadHealthFade = new NumericCrossfade();
    private final String[] chipLabelCache = new String[3];
    private final int[] chipLabelSpace = {-1, -1, -1};
    private String heldLabelSource = "";
    private int heldLabelSpace = -1;
    private String heldLabelText = "";

    public HudRenderer(ModuleManager modules, ClientConfig config) {
        this.modules = modules;
        this.config = config;
        sortedModules = new ArrayList<>(modules.all());
        sortedModules.sort(Comparator.comparing(ClientModule::name, String.CASE_INSENSITIVE_ORDER));
    }

    public HudRenderer(ModuleManager modules, ClientConfig config, dev.jilaton.client.render.WorldOverlayProjection projection) {
        this(modules, config);
        this.projection = projection;
    }

    public void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
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
            aboveHeadPresence.reset();
            heldItemPresence.reset();
            targetPlacement.reset();
            retention.clear();
            retentionWindowMillis = 5_000;
            speedometer.reset();
            staffRows.clear();
            radarBlips.clear();
            effectFades.clear();
            resetNumeric();
            lastTargetId = null;
            return;
        }

        Font font = JilatonType.font();
        int accent = ClientTheme.resolveAccent(config.themeId(), config.customAccent());
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();
        long now = System.nanoTime();
        ClientModule auraModule = modules.get("kill_aura");
        LivingEntity target = resolveCombatTarget(client, editorPreview);
        boolean targetRecentlyAttacked = target != null
                && auraModule instanceof KillAuraModule aura && aura.enabled() && aura.recentlyAttacked(target);
        LivingEntity displayTarget = target == null && editorPreview ? client.player : target;

        if (module instanceof HudModule hud && (hud.enabled() || editorPreview) && hud.targetHud()
                && visible("target", editorPreview)
                && (editorPreview || !(client.screen instanceof HudEditorScreen))) {
            if (editorPreview) {
                renderTargetCard(graphics, font, client, displayTarget,
                        accent, guiWidth, guiHeight, true, target != null && targetRecentlyAttacked);
            } else if (target != null && client.screen == null) {
                if (hud.targetSideBar()) renderTargetSideBar(graphics, font, client, target, guiWidth, guiHeight);
                renderTargetCard(graphics, font, client, target, accent, guiWidth, guiHeight,
                        false, targetRecentlyAttacked);
            }
        }
        if (target == null || !(module instanceof HudModule hud) || !hud.enabled() || !hud.followTarget()) targetPlacement.reset();

        if (module instanceof HudModule hud && (hud.enabled() || editorPreview) && (hud.aboveHead() || editorPreview)
                && visible("above_head", editorPreview)
                && (editorPreview || !(client.screen instanceof HudEditorScreen))) {
            renderAboveHead(graphics, font, client, editorPreview ? displayTarget : target,
                    accent, guiWidth, guiHeight, editorPreview, targetRecentlyAttacked);
        }

        int rightColumnTop = 36;
        if (modules.get("now_playing") instanceof dev.jilaton.client.module.impl.NowPlayingModule nowPlaying
                && nowPlaying.enabled() && visible("music", editorPreview)
                && (editorPreview || !(client.screen instanceof HudEditorScreen))) {
            dev.jilaton.client.music.NowPlaying state = dev.jilaton.client.music.MusicCenter.get().nowPlaying();
            if (editorPreview || state.hasTrack() || !nowPlaying.hideIdle()) {
                NowPlayingWidget.Style style = nowPlaying.style();
                int width = NowPlayingWidget.width(style), height = NowPlayingWidget.height(style);
                int x = widgetX("music", guiWidth - width - 8, width, guiWidth);
                int y = widgetY("music", 40, height, guiHeight);
                beginWidget(graphics, "music", x, y, width, height, editorPreview);
                NowPlayingWidget.render(graphics, font, state, style, x, y, activeStyle.accentColor(accent) & 0xFFFFFF,
                        nowPlaying.artwork(), nowPlaying.device(), System.nanoTime());
                graphics.pose().popMatrix();
                // A dragged music card owns its slot and must not move the list below it.
                if (config.hudPosition("music") == null) {
                    rightColumnTop = activeLayout.y() + activeLayout.height() + HudDesign.STACK_GAP;
                }
            }
        }
        int rightColumnNext = rightColumnTop;

        if (!(module instanceof HudModule hud) || (!editorPreview && !hud.enabled())
                || (!editorPreview && client.screen instanceof HudEditorScreen)) {
            return;
        }
        if (hud.staff() || hud.radar() || editorPreview) {
            refreshRetention(client, hud);
        }

        // Bottom-right slots, top to bottom: radar, held item, server, armor, keystrokes. The chain is
        // anchored to the screen edge so no panel can be pushed off-screen from below.
        int[] bottomColumn = bottomColumnTops(guiHeight, hud);

        int moduleListTop = 8;
        if (hud.watermark() && visible("watermark", editorPreview)) {
            String version = "JILATON";
            int width = Math.round(dev.jilaton.client.gui.design.Ui.spacedWidth(version, 9, 1.6F, JilatonType.Face.BOLD)) + 34;
            int x = widgetX("watermark", 8, width, guiWidth);
            int y = widgetY("watermark", 8, 22, guiHeight);
            beginWidget(graphics, "watermark", x, y, width, 22, editorPreview);
            accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
            drawPanel(graphics, x, y, width, 22);
            dev.jilaton.client.gui.design.Ui.mark(graphics, x + 8, y + 5, 12, 0xFFF5F5F5);
            dev.jilaton.client.gui.design.Ui.spaced(graphics, version, x + 26, y + 7, 9, 1.6F, JilatonType.Face.BOLD, 0xFFF5F5F5);
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
                listWidth = Math.max(listWidth, font.width(active.name()) + 4);
            }
            if (!activeModuleScratch.isEmpty()) {
                int listHeight = HudDesign.moduleListHeight(activeModuleScratch.size());
                int x = widgetX("modules", guiWidth - listWidth - 8, listWidth, guiWidth);
                int y = widgetY("modules", rightColumnTop, listHeight, guiHeight);
                beginWidget(graphics, "modules", x, y, listWidth, listHeight, editorPreview);
                accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
                for (ClientModule active : activeModuleScratch) {
                    String label = active.name();
                    graphics.drawString(font, label, x + listWidth - font.width(label), y + 2, 0xFFF5F5F5, true);
                    y += HudDesign.MODULE_ROW;
                }
                moduleListTop = activeLayout.y() + activeLayout.height() + HudDesign.STACK_GAP;
                // The list reserves the next slot only while it is auto-placed; a dragged list is free.
                if (config.hudPosition("modules") == null) rightColumnNext = moduleListTop;
                graphics.pose().popMatrix();
            }
        }

        if ((hud.keybinds() || editorPreview) && visible("keybinds", editorPreview)) {
            renderKeybinds(graphics, font, client, accent, moduleListTop, guiWidth, guiHeight, editorPreview);
        }

        if (hud.performance() && visible("performance", editorPreview)) {
            HudNumberPainter painter = numbers(font);
            HudNumbers metrics = painter.numbers();
            int fpsColumn = metrics.columnWidth(4);
            int pingColumn = metrics.columnWidth(5);
            int fpsLabelWidth = font.width("FPS");
            int pingLabelWidth = font.width("PING");
            int dotWidth = font.width("·");
            int millisWidth = font.width("ms");
            int width = 9 + fpsLabelWidth + 4 + fpsColumn + 6 + dotWidth + 6
                    + pingLabelWidth + 4 + pingColumn + HudNumberPainter.SUFFIX_GAP + millisWidth + 9;
            int x = widgetX("performance", guiWidth - width - 8, width, guiWidth);
            int y = widgetY("performance", 8, 22, guiHeight);
            beginWidget(graphics, "performance", x, y, width, 22, editorPreview);
            accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
            drawPanel(graphics, x, y, width, 22);
            int textY = y + 7;
            int cursor = x + 9;
            graphics.drawString(font, "FPS", cursor, textY, TEXT_SECONDARY);
            cursor += fpsLabelWidth + 4;
            fpsFade.update(fpsText.update(client.getFps()), now, config.reducedMotion());
            painter.drawFadeRight(graphics, fpsFade, now, cursor + fpsColumn, textY, TEXT_PRIMARY);
            cursor += fpsColumn + 6;
            graphics.drawString(font, "·", cursor, textY, TEXT_SECONDARY);
            cursor += dotWidth + 6;
            graphics.drawString(font, "PING", cursor, textY, TEXT_SECONDARY);
            cursor += pingLabelWidth + 4;
            int pingMillis = latency(client);
            int pingRight = cursor + pingColumn + HudNumberPainter.SUFFIX_GAP + millisWidth;
            if (pingMillis < 0) {
                pingFade.reset();
                painter.drawRight(graphics, "--", pingRight, textY, TEXT_SECONDARY);
            } else {
                pingFade.update(pingText.update(pingMillis), now, config.reducedMotion());
                painter.drawSuffixedFade(graphics, pingFade, now, "ms", pingRight, textY,
                        TEXT_PRIMARY, TEXT_SECONDARY);
            }
            graphics.pose().popMatrix();
        }

        Player player = client.player;
        if (hud.coordinates() && visible("coordinates", editorPreview)) {
            double speed = speedometer.update(player.getX(), player.getZ(), now, config.reducedMotion());
            HudNumberPainter painter = numbers(font);
            HudNumbers metrics = painter.numbers();
            int column = metrics.columnWidth(7);
            int speedColumn = metrics.columnWidth(5);
            int labelWidth = font.width("X");
            int suffixWidth = font.width("b/s");
            int width = 9 + 3 * (labelWidth + 2 + column) + 2 * 8 + 10 + speedColumn
                    + HudNumberPainter.SUFFIX_GAP + suffixWidth + 9;
            int x = widgetX("coordinates", 8, width, guiWidth);
            // Bottom-left beside the hotbar when it fits; on narrow windows it moves under the brand chip instead
            // of running under the hotbar.
            boolean fitsBeside = 8 + width <= guiWidth / 2 - 91 - 6;
            int y = widgetY("coordinates", fitsBeside ? guiHeight - 29 : 36, 21, guiHeight);
            beginWidget(graphics, "coordinates", x, y, width, 21, editorPreview);
            accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
            drawPanel(graphics, x, y, width, 21);
            int textY = y + 7;
            int cursor = x + 9;
            refreshCoordinates(player);
            coordinateXFade.update(coordinateX.text(), now, config.reducedMotion());
            graphics.drawString(font, "X", cursor, textY, TEXT_SECONDARY);
            cursor += labelWidth + 2;
            painter.drawFadeRight(graphics, coordinateXFade, now, cursor + column, textY, TEXT_PRIMARY);
            cursor += column + 8;
            coordinateYFade.update(coordinateY.text(), now, config.reducedMotion());
            graphics.drawString(font, "Y", cursor, textY, TEXT_SECONDARY);
            cursor += labelWidth + 2;
            painter.drawFadeRight(graphics, coordinateYFade, now, cursor + column, textY, TEXT_PRIMARY);
            cursor += column + 8;
            coordinateZFade.update(coordinateZ.text(), now, config.reducedMotion());
            graphics.drawString(font, "Z", cursor, textY, TEXT_SECONDARY);
            cursor += labelWidth + 2;
            painter.drawFadeRight(graphics, coordinateZFade, now, cursor + column, textY, TEXT_PRIMARY);
            cursor += column + 10;
            speedFade.update(speedText.update((int) Math.round(speed * 10)), now, config.reducedMotion());
            painter.drawSuffixedFade(graphics, speedFade, now, "b/s",
                    cursor + speedColumn + HudNumberPainter.SUFFIX_GAP + suffixWidth, textY,
                    TEXT_PRIMARY, TEXT_SECONDARY);
            graphics.pose().popMatrix();
        }

        if (hud.keystrokes() && visible("keystrokes", editorPreview)) {
            int width = HudDesign.KEYSTROKES_WIDTH;
            int height = HudDesign.KEYSTROKES_HEIGHT;
            int x = widgetX("keystrokes", guiWidth - width - 8, width, guiWidth);
            int y = widgetY("keystrokes", bottomColumn[4], height, guiHeight);
            beginWidget(graphics, "keystrokes", x, y, width, height, editorPreview);
            accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
            renderKeystrokes(graphics, client, accent, x, y);
            graphics.pose().popMatrix();
        }

        if (hud.armor() && visible("armor", editorPreview)) {
            renderArmor(graphics, client, editorPreview, bottomColumn[3]);
        }
        if (hud.effects() && visible("effects", editorPreview)) {
            // The potion panel may grow until the next panel in the column, CPS or the bottom-right chain.
            int effectsLimit = bottomColumn[0] - HudDesign.STACK_GAP;
            if (hud.cps() && visible("cps", editorPreview)) {
                effectsLimit -= HudDesign.CPS_HEIGHT + HudDesign.STACK_GAP;
            }
            rightColumnNext = renderEffects(graphics, client, editorPreview, rightColumnNext, effectsLimit);
        }
        if ((hud.serverPanel() || editorPreview) && visible("server", editorPreview)) {
            renderServerPanel(graphics, font, client, editorPreview, bottomColumn[2]);
        }
        if ((hud.heldItem() || editorPreview) && visible("held_item", editorPreview)) {
            renderHeldItem(graphics, font, client, editorPreview, bottomColumn[1]);
        }
        if ((hud.staff() || editorPreview) && visible("staff", editorPreview)) {
            renderStaff(graphics, font, client, editorPreview);
        }
        if ((hud.radar() || editorPreview) && visible("radar", editorPreview)) {
            renderRadar(graphics, client, editorPreview, bottomColumn[0]);
        }
        if (hud.cps() && visible("cps", editorPreview)) {
            HudNumberPainter painter = numbers(font);
            int column = painter.numbers().columnWidth(3);
            int labelWidth = font.width("CPS");
            int betweenWidth = font.width(" L   /   ");
            int suffixWidth = font.width(" R");
            int width = 9 + labelWidth + 4 + column + betweenWidth + column + suffixWidth + 9;
            int x = widgetX("cps", guiWidth - width - 8, width, guiWidth);
            int y = widgetY("cps", rightColumnNext, HudDesign.CPS_HEIGHT, guiHeight);
            beginWidget(graphics, "cps", x, y, width, HudDesign.CPS_HEIGHT, editorPreview);
            drawPanel(graphics, x, y, width, HudDesign.CPS_HEIGHT);
            int textY = y + 7;
            int cursor = x + 9;
            int color = activeStyle.accentColor(accent);
            graphics.drawString(font, "CPS", cursor, textY, TEXT_SECONDARY);
            cursor += labelWidth + 4;
            cpsLeftFade.update(cpsLeftText.update(HudInputMetrics.left(now)), now, config.reducedMotion());
            painter.drawFadeRight(graphics, cpsLeftFade, now, cursor + column, textY, color);
            cursor += column;
            graphics.drawString(font, " L   /   ", cursor, textY, TEXT_SECONDARY);
            cursor += betweenWidth;
            cpsRightFade.update(cpsRightText.update(HudInputMetrics.right(now)), now, config.reducedMotion());
            painter.drawFadeRight(graphics, cpsRightFade, now, cursor + column, textY, color);
            cursor += column;
            graphics.drawString(font, " R", cursor, textY, TEXT_SECONDARY);
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
            drawPanel(graphics, x, top, width, 23);
            JilatonUi.fillRounded(graphics, x + 8, top + 9, 5, 5, 3, notice == null || notice.enabled() ? 0xFF30A46C : 0xFF5F5F5F);
            graphics.drawString(JilatonType.font(), JilatonType.font().plainSubstrByWidth(label, width - 26), x + 18, top + 7, 0xFFF0F0F0, false);
            double remaining = notice == null ? 1.0 : Math.max(0, 1.0
                    - (now - notice.createdAt()) / (double) dev.jilaton.client.notification.NotificationCenter.LIFETIME_NANOS);
            int track = width - 16;
            JilatonUi.fillRounded(graphics, x + 8, top + 19, Math.max(2, (int) Math.round(track * remaining)), 1, 0,
                    HudDesign.withAlpha(0xFFF5F5F5, 0.35));
        }
        graphics.pose().popMatrix();
    }

    private void renderArmor(GuiGraphics graphics, Minecraft client, boolean preview, int defaultY) {
        int width = HudDesign.ARMOR_WIDTH;
        int height = HudDesign.ARMOR_HEIGHT;
        int x = widgetX("armor", graphics.guiWidth() - width - 8, width, graphics.guiWidth());
        int y = widgetY("armor", defaultY, height, graphics.guiHeight());
        beginWidget(graphics, "armor", x, y, width, height, preview);
        drawPanel(graphics, x, y, width, height);
        int accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
        graphics.drawString(JilatonType.font(), "ARMOR", x + 6, y + 5, accent);
        HudNumberPainter painter = numbers(JilatonType.font());
        int worst = worstDurabilityPercent(client);
        if (worst >= 0) {
            long now = System.nanoTime();
            int percentRight = x + width - 6;
            armorFade.update(armorPercentText.update(worst), now, config.reducedMotion());
            painter.drawSuffixedFade(graphics, armorFade, now, "%", percentRight, y + 5,
                    worst < 20 ? DANGER : TEXT_PRIMARY, TEXT_SECONDARY);
        } else {
            graphics.drawString(JilatonType.font(), "--", x + width - 6 - JilatonType.font().width("--"), y + 5,
                    TEXT_SECONDARY);
        }
        for (int index = 0; index < ARMOR_SLOTS.length; index++) {
            ItemStack stack = client.player.getItemBySlot(ARMOR_SLOTS[index]);
            int tileX = x + 6 + index * 31;
            if (stack.isEmpty()) {
                graphics.drawString(JilatonType.font(), "--", tileX + 7, y + 20, 0xFF707070);
            } else {
                graphics.renderItem(stack, tileX + 5, y + 16);
                if (stack.isDamageableItem()) {
                    double remaining = Math.max(0, 1.0 - stack.getDamageValue() / (double) Math.max(1, stack.getMaxDamage()));
                    graphics.fill(tileX, y + 34, tileX + 27, y + 37, 0xFF343434);
                    graphics.fill(tileX, y + 34, tileX + (int) Math.round(27 * remaining), y + 37,
                            remaining < 0.2 ? 0xFFFF647C : accent);
                }
            }
            graphics.drawString(JilatonType.font(), ARMOR_LABELS[index], tileX, y + 39, 0xFFA5A5A5);
        }
        graphics.pose().popMatrix();
    }

    /**
     * Draws the potion panel in the next right-column slot and returns the slot below it. A dragged
     * panel keeps its saved spot and does not move the panels that follow.
     */
    private int renderEffects(GuiGraphics graphics, Minecraft client, boolean preview, int defaultY,
                              int bottomLimit) {
        long now = System.nanoTime();
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
                effectRows.add(new EffectRow(name, name, duration));
            }
            effectRows.sort(Comparator.comparing(EffectRow::name));
            Iterator<Map.Entry<String, NumericCrossfade>> stale = effectFades.entrySet().iterator();
            while (stale.hasNext()) {
                Map.Entry<String, NumericCrossfade> entry = stale.next();
                boolean present = false;
                for (EffectRow row : effectRows) {
                    if (row.key().equals(entry.getKey())) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    stale.remove();
                }
            }
        }
        if (effectRows.isEmpty() && !preview) return defaultY;
        int width = HudDesign.EFFECTS_WIDTH;
        int maxRows = Math.max(1, (graphics.guiHeight() - 24) / HudDesign.EFFECTS_ROW);
        // An auto-placed panel also has to stop above the bottom-right column instead of growing into it.
        if (config.hudPosition("effects") == null) {
            int room = (bottomLimit - defaultY - HudDesign.EFFECTS_TITLE) / HudDesign.EFFECTS_ROW;
            maxRows = Math.max(1, Math.min(maxRows, room));
        }
        int rowCount = Math.max(1, Math.min(maxRows, effectRows.size()));
        int height = HudDesign.effectsHeight(rowCount);
        int x = widgetX("effects", graphics.guiWidth() - width - 8, width, graphics.guiWidth());
        int y = widgetY("effects", defaultY, height, graphics.guiHeight());
        beginWidget(graphics, "effects", x, y, width, height, preview);
        drawPanel(graphics, x, y, width, height);
        graphics.drawString(JilatonType.font(), "POTION EFFECTS", x + 6, y + 6,
                activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent())));
        HudNumberPainter painter = numbers(JilatonType.font());
        for (int index = 0; index < rowCount; index++) {
            EffectRow row = effectRows.isEmpty() ? new EffectRow("", "No active effects", "") : effectRows.get(index);
            int durationWidth = painter.width(row.duration());
            String name = JilatonType.font().plainSubstrByWidth(row.name(), width - durationWidth - 20);
            graphics.drawString(JilatonType.font(), name, x + 6, y + 21 + index * 14, 0xFFE0E0E0);
            NumericCrossfade fade = effectFades.computeIfAbsent(row.key(), key -> new NumericCrossfade());
            fade.update(row.duration(), now, config.reducedMotion());
            painter.drawFadeRight(graphics, fade, now, x + width - 6, y + 21 + index * 14, TEXT_SECONDARY);
        }
        int next = config.hudPosition("effects") == null
                ? activeLayout.y() + activeLayout.height() + HudDesign.STACK_GAP : defaultY;
        graphics.pose().popMatrix();
        return next;
    }

    private record EffectRow(String key, String name, String duration) { }

    private boolean visible(String id, boolean preview) {
        return preview || config.hudStyle(id).visible();
    }

    private void beginWidget(GuiGraphics graphics, String id, int x, int y, int width, int height,
                             boolean preview) {
        beginWidgetAtScale(graphics, id, x, y, width, height, preview, config.hudStyle(id).scale());
    }

    /**
     * One scale multiplies the whole widget: geometry is laid out at its base size and the pose is
     * scaled here, so text, rows and the panel shrink together and nothing drifts out of bounds.
     */
    private void beginWidgetAtScale(GuiGraphics graphics, String id, int x, int y, int width, int height,
                                    boolean preview, double widgetScale) {
        HudStyle stored = config.hudStyle(id);
        float scale = (float) Math.max(0.5, Math.min(2.0, widgetScale));
        activeStyle = new HudStyle(scale, stored.panelOpacity(), stored.accent(),
                stored.alignment(), stored.visible());
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
        HudModule hud = (HudModule) modules.get("hud");
        long now = System.nanoTime();
        boolean compact = hud.compactTarget();
        boolean chips = hud.targetChips();
        boolean heldVisible = hud.targetHeld();
        boolean distanceVisible = hud.targetDistance();
        HudDesign.TargetCard card = HudDesign.baseTargetCard(chips, heldVisible);
        HudDesign.CompactCard compactCard = HudDesign.compactTarget();
        int width = compact ? compactCard.width() : card.width();
        int height = compact ? compactCard.height() : card.height();
        int requestedWidth = hud.targetWidth();
        int screenWidth = requestedWidth > 0 ? requestedWidth : HudDesign.targetCardWidth(guiWidth);
        double widgetScale = Math.max(0.5, Math.min(2.0,
                config.hudStyle("target").scale() * hud.targetScale() * (screenWidth / (double) width)));
        int scaledWidth = (int) Math.ceil(width * widgetScale);
        int scaledHeight = (int) Math.ceil(height * widgetScale);
        int x = widgetX("target", (guiWidth - scaledWidth) / 2, scaledWidth, guiWidth);
        // Bottom-anchored: the card's own height (chip row, held-item pill) moves its top up, never its
        // bottom edge onto the hotbar. A saved drag still overrides this default in widgetY.
        int y = widgetY("target", HudDesign.aboveHotbarTop(guiHeight, scaledHeight), scaledHeight, guiHeight);
        if (!editorPreview && hud.followTarget()) {
            Vec3 anchor = target.getPosition(partialTick).add(0, target.getBbHeight() * 0.75, 0);
            if (projection == null || !projection.isCurrent(client) || !projection.project(anchor, guiWidth, guiHeight)) {
                targetPlacement.reset();
                return;
            }
            double anchorX = projection.x(guiWidth), anchorY = projection.y(guiHeight);
            // Project a point at the body's side (camera-right, plus room for arms and a held item) so the card
            // clears the opponent at any distance instead of sitting a fixed number of pixels from their centre.
            double yaw = Math.toRadians(client.gameRenderer.getMainCamera().yRot());
            double side = target.getBbWidth() * 0.5 + 0.15;
            double bodyHalfWidth = projection.project(anchor.add(-Math.cos(yaw) * side, 0, -Math.sin(yaw) * side), guiWidth, guiHeight)
                    ? Math.abs(projection.x(guiWidth) - anchorX) : 0;
            HudStyle stored = config.hudStyle("target");
            activeStyle = new HudStyle((float) widgetScale, stored.panelOpacity(), stored.accent(),
                    HudStyle.Alignment.FREE, stored.visible());
            // The pinned scale sits below HudStyle's 0.5 floor, so it shrinks the laid-out content instead.
            float pinned = (float) hud.followScale();
            activeLayout = targetPlacement.place(target.getUUID(), anchorX, anchorY, bodyHalfWidth,
                    Math.max(1, Math.round(width * pinned)), Math.max(1, Math.round(height * pinned)),
                    guiWidth, guiHeight, activeStyle, System.nanoTime(), config.reducedMotion());
            graphics.pose().pushMatrix();
            graphics.pose().translate(activeLayout.x(), activeLayout.y());
            graphics.pose().scale(activeLayout.scale() * pinned, activeLayout.scale() * pinned);
            x = y = 0;
        } else beginWidgetAtScale(graphics, "target", x, y, width, height, editorPreview, widgetScale);
        if (compact) {
            renderCompactTarget(graphics, font, client, target, x, y, compactCard, accent, recentlyAttacked);
            graphics.pose().popMatrix();
            return;
        }
        // Near-black panel, white head outline, and one white-to-grey health gradient.
        int surface = ((int) Math.round(activeStyle.panelOpacity() * 255) << 24) | 0x09090D;
        glass(graphics, x, y, width, height, surface);

        int headX = x + card.headX(), headY = y + card.headY(), headSize = card.headSize();
        JilatonUi.fillRounded(graphics, headX, headY, headSize, headSize, 6, 0xFF101010);
        JilatonUi.strokeRounded(graphics, headX, headY, headSize, headSize, 6, recentlyAttacked ? 0xFFFFFFFF : 0x30FFFFFF);
        // The entity preview is a picture-in-picture drawn in absolute screen pixels, so the tile is mapped
        // through the current pose; the pinned card draws at local (0, 0) under a translate + scale.
        org.joml.Vector2f tileMin = graphics.pose().transformPosition(headX, headY, new org.joml.Vector2f());
        org.joml.Vector2f tileMax = graphics.pose().transformPosition(headX + headSize, headY + headSize, new org.joml.Vector2f());
        float tileScale = (tileMax.x - tileMin.x) / headSize;
        graphics.pose().pushMatrix();
        graphics.pose().identity();
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                Math.round(tileMin.x), Math.round(tileMin.y), Math.round(tileMax.x), Math.round(tileMax.y),
                Math.max(1, Math.round(20 * tileScale)), 0.0625F,
                (tileMin.x + tileMax.x) * 0.5F, tileMin.y + (tileMax.y - tileMin.y) * 0.55F, target);
        graphics.pose().popMatrix();

        int contentX = x + card.contentX();
        HudNumberPainter painter = numbers(font);
        HudNumbers metrics = painter.numbers();
        int distanceColumn = distanceVisible
                ? metrics.columnWidth(3) + metrics.advanceOf('.')
                    + HudNumberPainter.SUFFIX_GAP + font.width(" m")
                : 0;
        double distance = Math.sqrt(client.player.distanceToSqr(target));
        int nameWidth = Math.max(0, card.contentWidth() - distanceColumn - 6);
        String name = HudText.ellipsize(target.getName().getString(),
                character -> font.width(String.valueOf(character)), nameWidth);
        graphics.drawString(font, name, contentX, y + card.nameY(), 0xFFF2F2F2);
        if (distanceVisible) {
            targetDistanceValue.update((int) Math.round(distance * 10));
            targetDistanceFade.update(targetDistanceValue.text(), now, config.reducedMotion());
            painter.drawSuffixedFade(graphics, targetDistanceFade, now, " m",
                    x + card.contentRight(), y + card.nameY(), 0xFF9A94A6, 0xFF9A94A6);
        }

        if (heldVisible) {
            ItemStack held = target.getMainHandItem();
            String heldName = held.isEmpty() ? "EMPTY" : held.getHoverName().getString().toUpperCase(Locale.ROOT);
            int pillY = y + card.pillY();
            int pillWidth = HudDesign.pillWidth(font.width(heldName), card.contentWidth());
            graphics.fill(contentX, pillY, contentX + pillWidth, pillY + card.pillHeight(), 0xFF1C1C1C);
            graphics.fill(contentX, pillY + 1, contentX + 2, pillY + card.pillHeight() - 1, 0xFFF2F2F2);
            if (!held.isEmpty()) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(contentX + 3, pillY - 1);
                graphics.pose().scale(0.75F, 0.75F);
                graphics.renderItem(held, 0, 0);
                graphics.pose().popMatrix();
            }
            String pillLabel = HudDesign.pillTextFits(font.width(heldName), card.contentWidth())
                    ? heldName
                    : HudText.ellipsize(heldName, character -> font.width(String.valueOf(character)),
                        Math.max(0, pillWidth - HudDesign.PILL_TEXT_INSET));
            graphics.drawString(font, pillLabel, contentX + 15, pillY + 2, 0xFFF2F2F2);
        }

        float health = Math.max(0.0F, target.getHealth());
        float maxHealth = Math.max(1.0F, target.getMaxHealth());
        float healthRatio = Mth.clamp(health / maxHealth, 0.0F, 1.0F);
        if (!target.getUUID().equals(lastTargetId)) {
            targetHealth.reset();
            lastTargetId = target.getUUID();
        }
        float displayedHealth = (float) targetHealth.update(healthRatio, now, config.reducedMotion());
        targetHealthValue.update(Math.round(health * 10.0F));
        targetHealthFade.update(targetHealthValue.text(), now, config.reducedMotion());
        int healthX = painter.drawLeft(graphics, HEART_MARK, contentX, y + card.healthY(), 0xFFF2F2F2);
        painter.drawFadeLeft(graphics, targetHealthFade, now, healthX, y + card.healthY(), 0xFFF2F2F2);

        HudDesign.Bar bar = HudDesign.healthBar(card);
        int barY = y + bar.y();
        for (int index = 0; index < bar.segments(); index++) {
            int left = x + bar.segmentLeft(index);
            int right = x + bar.segmentRight(index);
            graphics.fill(left, barY, right, barY + bar.height(), 0xFF232323);
            int filled = left + (int) Math.round((right - left) * HudDesign.segmentFill(index, displayedHealth));
            if (filled > left) {
                graphics.fill(left, barY, filled, barY + bar.height(),
                        HudDesign.segmentColor(index, bar.segments()));
            }
        }
        graphics.fill(contentX, y + card.ruleY(), x + card.contentRight(),
                y + card.ruleY() + 1, 0x26FFFFFF);

        // Combo, hit score and health lead, counted only from hurt flashes this client saw (FightStats).
        if (chips) fightStats.observe(target.getId(), target.hurtTime, client.player.hurtTime,
                CombatTargetTracker.sinceAttack(target, now));
        double lead = client.player.getHealth() + client.player.getAbsorptionAmount()
                - target.getHealth() - target.getAbsorptionAmount();
        for (int index = 0; chips && index < CHIP_LABELS.length; index++) {
            int left = x + card.chipX(index);
            int top = y + card.chipsY();
            int chipWidth = card.chipWidth();
            graphics.fill(left, top, left + chipWidth, top + card.chipHeight(), 0xFF161616);
            graphics.fill(left, top, left + 2, top + card.chipHeight(), 0x66FFFFFF);
            String value = switch (index) {
                case 0 -> chipComboValue.update(fightStats.combo());
                case 1 -> fightStats.landed() + "-" + fightStats.taken();
                default -> (lead >= 0 ? "+" : "") + String.format(Locale.ROOT, "%.1f", lead);
            };
            int valueRight = left + chipWidth - 6;
            int labelSpace = Math.max(0, valueRight - painter.width(value) - 3 - (left + 6));
            if (chipLabelSpace[index] != labelSpace) {
                chipLabelSpace[index] = labelSpace;
                chipLabelCache[index] = font.plainSubstrByWidth(CHIP_LABELS[index], labelSpace);
            }
            graphics.drawString(font, chipLabelCache[index], left + 6, top + 2, 0xFF9A9A9A);
            painter.drawRight(graphics, value, valueRight, top + 2, 0xFFF2F2F2);
        }
        graphics.pose().popMatrix();
    }

    private void renderCompactTarget(GuiGraphics graphics, Font font, Minecraft client, LivingEntity target,
                                     int x, int y, HudDesign.CompactCard card, int accent,
                                     boolean recentlyAttacked) {
        int color = activeStyle.accentColor(accent);
        glass(graphics, x, y, card.width(), card.height(), activeStyle.panelColor());
        JilatonUi.fillRounded(graphics, x + card.faceX(), y + card.faceY(), card.faceSize(), card.faceSize(),
                5, 0xFF1B1B1B);
        if (target instanceof net.minecraft.client.player.AbstractClientPlayer player)
            net.minecraft.client.gui.components.PlayerFaceRenderer.draw(graphics, player.getSkin(),
                    x + card.faceX(), y + card.faceY(), card.faceSize());
        else graphics.drawString(font, HudText.ellipsize(target.getName().getString(),
                        character -> font.width(String.valueOf(character)), card.faceSize() - 4),
                x + card.faceX() + 4, y + card.heartsY() - 3, color);
        int weaponRoom = Math.max(0, card.width() - card.textX() - card.rightPadding() - 40);
        String weapon = target.getMainHandItem().isEmpty() ? ""
                : HudText.ellipsize(target.getMainHandItem().getHoverName().getString(),
                    character -> font.width(String.valueOf(character)), Math.min(58, weaponRoom));
        int nameRoom = Math.max(0, card.width() - card.textX() - card.rightPadding()
                - (weapon.isEmpty() ? 0 : font.width(weapon) + 7));
        String name = HudText.ellipsize(target.getName().getString(),
                character -> font.width(String.valueOf(character)), nameRoom);
        graphics.drawString(font, name, x + card.textX(), y + card.nameY(), 0xFFEFEFEF);
        float health = Math.max(0, target.getHealth() + target.getAbsorptionAmount());
        HudNumberPainter painter = numbers(font);
        long now = System.nanoTime();
        String hearts = dev.jilaton.client.render.NametagLayout.healthText(health, true);
        int heartsX = painter.drawLeft(graphics, HEART_MARK, x + card.textX(), y + card.heartsY(), 0xFFF17EBB);
        painter.drawLeft(graphics, hearts, heartsX, y + card.heartsY(), 0xFFF17EBB);
        if (!weapon.isEmpty()) {
            graphics.drawString(font, weapon, x + card.width() - font.width(weapon) - card.rightPadding(),
                    y + card.nameY(), 0xFFA9A9A9);
        }
        compactDistanceValue.update((int) Math.round(Math.sqrt(client.player.distanceToSqr(target)) * 10));
        compactDistanceFade.update(compactDistanceValue.text(), now, config.reducedMotion());
        painter.drawSuffixedFade(graphics, compactDistanceFade, now, "m",
                x + card.width() - card.rightPadding(), y + card.heartsY(), 0xFFA9A9A9, 0xFFA9A9A9);
        if (!target.getUUID().equals(lastTargetId)) { targetHealth.reset(); lastTargetId = target.getUUID(); }
        double ratio = Math.max(0, Math.min(1, target.getHealth() / Math.max(1F, target.getMaxHealth())));
        float displayed = (float) targetHealth.update(ratio, System.nanoTime(), config.reducedMotion());
        int barWidth = card.width() - card.textX() - card.rightPadding();
        graphics.fill(x + card.textX(), y + card.barY(), x + card.textX() + barWidth,
                y + card.barY() + card.barHeight(), 0xFF2B2B2B);
        graphics.fill(x + card.textX(), y + card.barY(),
                x + card.textX() + Math.round(barWidth * displayed), y + card.barY() + card.barHeight(), color);
        // No fabricated combo or hit count: this is local attack-attempt feedback.
        graphics.drawString(font, recentlyAttacked ? "ATTACKING" : "TRACKING",
                x + card.textX(), y + card.statusY(), 0xFF858585);
    }

    /**
     * Vertical health bar standing beside the target, as tall as their body on screen, with the health in HP under
     * it. It sits on the camera-right side; the pinned card prefers the left, so the two never overlap.
     */
    private void renderTargetSideBar(GuiGraphics graphics, Font font, Minecraft client, LivingEntity target,
                                     int guiWidth, int guiHeight) {
        if (projection == null || !projection.isCurrent(client)) return;
        Vec3 feet = target.getPosition(partialTick);
        double yaw = Math.toRadians(client.gameRenderer.getMainCamera().yRot());
        double side = target.getBbWidth() * 0.5 + 0.3;
        Vec3 bottom = feet.add(-Math.cos(yaw) * side, 0, -Math.sin(yaw) * side);
        if (!projection.project(bottom, guiWidth, guiHeight)) return;
        double bx = projection.x(guiWidth), by = projection.y(guiHeight);
        if (!projection.project(bottom.add(0, target.getBbHeight(), 0), guiWidth, guiHeight)) return;
        double ty = projection.y(guiHeight);
        int height = (int) Math.round(by - ty);
        if (height < 12) return;
        int x = (int) Math.round(bx), top = (int) Math.round(ty), width = Math.max(2, Math.min(4, height / 30));
        double ratio = Mth.clamp(target.getHealth() / Math.max(1F, target.getMaxHealth()), 0F, 1F);
        int filled = (int) Math.round(height * sideBarHealth.update(ratio, System.nanoTime(), config.reducedMotion()));
        graphics.fill(x - 1, top - 1, x + width + 1, top + height + 1, 0xA0000000);
        graphics.fill(x, top, x + width, top + height, 0xFF2A2A2A);
        graphics.fill(x, top + height - filled, x + width, top + height, ratio > 0.3 ? 0xFFF2F2F2 : 0xFF9A9A9A);
        String label = Math.round(target.getHealth() + target.getAbsorptionAmount()) + " HP";
        int labelX = x + width / 2 - font.width(label) / 2;
        graphics.fill(labelX - 2, top + height + 3, labelX + font.width(label) + 2, top + height + 14, 0xA0000000);
        graphics.drawString(font, label, labelX, top + height + 5, 0xFFF2F2F2);
    }

    private LivingEntity resolveCombatTarget(Minecraft client, boolean editorPreview) {
        if (editorPreview || client.player == null || client.level == null) {
            return null;
        }
        if (modules.get("kill_aura") instanceof KillAuraModule aura && aura.enabled()) {
            LivingEntity auraTarget = aura.currentTarget();
            if (isValidTarget(client, auraTarget)) {
                return auraTarget;
            }
        }
        if (client.crosshairPickEntity instanceof LivingEntity picked && isValidTarget(client, picked)) {
            return picked;
        }
        LivingEntity remembered = CombatTargetTracker.lastHit(TARGET_MEMORY_SECONDS, System.nanoTime());
        return isValidTarget(client, remembered) ? remembered : null;
    }

    private static boolean isValidTarget(Minecraft client, LivingEntity candidate) {
        return candidate != null && client.player != null && candidate != client.player
                && candidate.isAlive() && !candidate.isRemoved() && !candidate.isSpectator()
                && !(candidate instanceof ArmorStand) && candidate.level() == client.level
                && candidate.distanceToSqr(client.player) <= TARGET_MAX_DISTANCE_SQR;
    }

    private void renderAboveHead(GuiGraphics graphics, Font font, Minecraft client, LivingEntity target,
                                 int accent, int guiWidth, int guiHeight, boolean editorPreview,
                                 boolean recentlyAttacked) {
        HudStyle style = config.hudStyle("above_head");
        activeStyle = style;
        double presence = aboveHeadPresence.update(target != null ? 1 : 0, System.nanoTime(),
                config.reducedMotion());
        if (target == null || presence <= 0.01) {
            return;
        }
        int width = HudDesign.ABOVE_HEAD_WIDTH;
        int height = HudDesign.ABOVE_HEAD_HEIGHT;
        int drawX;
        int drawY;
        if (editorPreview) {
            int x = widgetX("above_head", (guiWidth - width) / 2, width, guiWidth);
            int y = widgetY("above_head", guiHeight - 108, height, guiHeight);
            beginWidget(graphics, "above_head", x, y, width, height, true);
            drawX = x;
            drawY = y;
        } else {
            float scale = (float) style.scale();
            HudStyle free = new HudStyle(style.scale(), style.panelOpacity(), style.accent(),
                    HudStyle.Alignment.FREE, true);
            int anchorX = (guiWidth - width) / 2;
            int anchorY = guiHeight - 108;
            if (projection != null && projection.isCurrent(client)) {
                Vec3 anchor = target.getPosition(partialTick).add(0, target.getBbHeight() + 0.35, 0);
                if (projection.project(anchor, guiWidth, guiHeight)) {
                    anchorX = Math.round(projection.x(guiWidth) - width * scale * 0.5F);
                    anchorY = Math.round(projection.y(guiHeight) - height * scale - 4);
                }
            }
            HudLayout layout = HudLayout.resolve(width, height, guiWidth, guiHeight, anchorX, anchorY, null, free);
            graphics.pose().pushMatrix();
            graphics.pose().translate(layout.x(), layout.y());
            graphics.pose().scale(layout.scale(), layout.scale());
            drawX = 0;
            drawY = 0;
        }
        graphics.fill(drawX, drawY, drawX + width, drawY + height,
                HudDesign.withAlpha(style.panelColor(), presence));
        graphics.fill(drawX, drawY, drawX + 2, drawY + height,
                HudDesign.withAlpha(recentlyAttacked ? 0xFFFFFFFF : style.accentColor(accent), presence));
        String name = font.plainSubstrByWidth(target.getName().getString(), width - 46);
        graphics.drawString(font, name, drawX + 7, drawY + 5, HudDesign.withAlpha(0xFFF2F2F2, presence));
        HudNumberPainter painter = numbers(font);
        long now = System.nanoTime();
        aboveHeadHealthValue.update(Math.round(target.getHealth() * 10.0F));
        aboveHeadHealthFade.update(aboveHeadHealthValue.text(), now, config.reducedMotion());
        painter.drawFadeRight(graphics, aboveHeadHealthFade, now, drawX + width - 7, drawY + 5,
                HudDesign.withAlpha(0xFFCDBFFF, presence));
        double distance = Math.sqrt(client.player.distanceToSqr(target));
        int detailColor = HudDesign.withAlpha(0xFF9A94A6, presence);
        aboveHeadDistanceValue.update((int) Math.round(distance * 10));
        int detailCursor = painter.drawLeft(graphics, aboveHeadDistanceValue.text(), drawX + 7, drawY + 15,
                detailColor);
        painter.drawLeft(graphics, " m  \u00B7  ", detailCursor, drawY + 15, detailColor);
        int armorRight = drawX + width - 7;
        int armorLabelX = armorRight - painter.numbers().columnWidth(2) - 3 - font.width("ARMOR");
        graphics.drawString(font, "ARMOR", armorLabelX, drawY + 15, detailColor);
        painter.drawRight(graphics, aboveHeadArmorValue.update(target.getArmorValue()), armorRight,
                drawY + 15, detailColor);
        graphics.pose().popMatrix();
    }

    private void renderKeybinds(GuiGraphics graphics, Font font, Minecraft client, int accent, int defaultY,
                                int guiWidth, int guiHeight, boolean preview) {
        List<ClientModule> bound = new ArrayList<>();
        for (ClientModule active : sortedModules) {
            if (active != modules.get("hud") && active.enabled() && active.keyCode() >= 0) {
                bound.add(active);
            }
        }
        int rows = Math.max(1, Math.min(HudDesign.KEYBIND_MAX_ROWS, bound.size()));
        int width = HudDesign.KEYBIND_WIDTH;
        int height = HudDesign.keybindHeight(rows);
        int x = widgetX("keybinds", 8, width, guiWidth);
        int y = widgetY("keybinds", defaultY, height, guiHeight);
        beginWidget(graphics, "keybinds", x, y, width, height, preview);
        accent = activeStyle.accentColor(accent);
        drawPanel(graphics, x, y, width, height);
        graphics.drawString(font, "KEY BINDS", x + 7, y + 6, accent);
        long window = client.getWindow().handle();
        for (int index = 0; index < rows; index++) {
            String label;
            String key;
            boolean down = false;
            if (bound.isEmpty()) {
                label = "No bound modules";
                key = "";
            } else {
                ClientModule module = bound.get(index);
                label = module.name();
                key = InputConstants.Type.KEYSYM.getOrCreate(module.keyCode()).getDisplayName().getString()
                        .toUpperCase(Locale.ROOT);
                down = GLFW.glfwGetKey(window, module.keyCode()) == GLFW.GLFW_PRESS;
            }
            int rowY = y + HudDesign.KEYBIND_TITLE + index * HudDesign.KEYBIND_ROW;
            int chipWidth = key.isEmpty() ? 0 : Math.max(22, font.width(key) + 8);
            graphics.drawString(font, font.plainSubstrByWidth(label, width - chipWidth - 20), x + 7, rowY,
                    down ? 0xFFF2F2F2 : 0xFFC9C9C9);
            if (!key.isEmpty()) {
                int chipX = x + width - chipWidth - 7;
                graphics.fill(chipX, rowY - 1, chipX + chipWidth, rowY + 10,
                        down ? (accent & 0x00FFFFFF) | 0x66000000 : 0xFF161616);
                graphics.drawString(font, key, chipX + (chipWidth - font.width(key)) / 2, rowY + 1,
                        down ? 0xFFFFFFFF : 0xFFF5F5F5);
            }
        }
        graphics.pose().popMatrix();
    }

    private void renderServerPanel(GuiGraphics graphics, Font font, Minecraft client, boolean preview,
                                   int defaultY) {
        int width = HudDesign.SERVER_WIDTH;
        int height = HudDesign.SERVER_HEIGHT;
        int x = widgetX("server", graphics.guiWidth() - width - 8, width, graphics.guiWidth());
        int y = widgetY("server", defaultY, height, graphics.guiHeight());
        beginWidget(graphics, "server", x, y, width, height, preview);
        drawPanel(graphics, x, y, width, height);
        graphics.drawString(font, "SERVER", x + 7, y + 6,
                activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent())));
        String address = "SINGLEPLAYER";
        if (client.getCurrentServer() != null) {
            String ip = client.getCurrentServer().ip;
            address = ip == null || ip.isBlank() ? "MULTIPLAYER" : ip;
        }
        int pingMillis = latency(client);
        long now = System.nanoTime();
        int right = x + width - 7;
        HudNumberPainter painter = numbers(font);
        int rowY = y + HudDesign.SERVER_ROW + 8;
        graphics.drawString(font, font.plainSubstrByWidth(address, width - 14), x + 7, rowY, TEXT_PRIMARY);
        rowY += 11;
        graphics.drawString(font, "PING", x + 7, rowY, TEXT_SECONDARY);
        if (pingMillis < 0) {
            serverPingFade.reset();
            painter.drawRight(graphics, "--", right, rowY, TEXT_SECONDARY);
        } else {
            serverPingFade.update(serverPingText.update(pingMillis), now, config.reducedMotion());
            painter.drawSuffixedFade(graphics, serverPingFade, now, "ms", right, rowY,
                    TEXT_PRIMARY, TEXT_SECONDARY);
        }
        rowY += 11;
        graphics.drawString(font, "PLAYERS", x + 7, rowY, TEXT_SECONDARY);
        serverPlayersFade.update(serverPlayersText.update(client.level.players().size()), now,
                config.reducedMotion());
        painter.drawFadeRight(graphics, serverPlayersFade, now, right, rowY, TEXT_PRIMARY);
        rowY += 11;
        graphics.drawString(font, "TIME", x + 7, rowY, TEXT_SECONDARY);
        clockFade.update(clockText.update(System.currentTimeMillis()), now, config.reducedMotion());
        painter.drawFadeRight(graphics, clockFade, now, right, rowY, TEXT_PRIMARY);
        graphics.pose().popMatrix();
    }

    private void renderHeldItem(GuiGraphics graphics, Font font, Minecraft client, boolean preview,
                                int defaultY) {
        ItemStack stack = client.player.getMainHandItem();
        double presence = heldItemPresence.update(!stack.isEmpty() || preview ? 1 : 0, System.nanoTime(),
                config.reducedMotion());
        if (!preview && presence <= 0.01) {
            return;
        }
        int width = HudDesign.HELD_ITEM_WIDTH;
        int height = HudDesign.HELD_ITEM_HEIGHT;
        int x = widgetX("held_item", graphics.guiWidth() - width - 8, width, graphics.guiWidth());
        int y = widgetY("held_item", defaultY, height, graphics.guiHeight());
        beginWidget(graphics, "held_item", x, y, width, height, preview);
        int accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
        glass(graphics, x, y, width, height, HudDesign.withAlpha(activeStyle.panelColor(), presence));
        String label = fitHeldLabel(font, stack.isEmpty() ? "EMPTY"
                : stack.getHoverName().getString().toUpperCase(Locale.ROOT));
        if (!stack.isEmpty()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + 8, y + 8);
            graphics.pose().scale(HudDesign.HELD_ITEM_SCALE, HudDesign.HELD_ITEM_SCALE);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popMatrix();
        } else {
            graphics.fill(x + 10, y + 10, x + 42, y + 42, 0xFF161616);
        }
        graphics.drawString(font, label, x + HudDesign.HELD_ITEM_LABEL_X, y + 10,
                HudDesign.withAlpha(0xFFF2F2F2, presence));
        int count = stack.getCount();
        if (count > 1) {
            HudNumberPainter painter = numbers(font);
            int countX = painter.drawLeft(graphics, "x", x + HudDesign.HELD_ITEM_LABEL_X, y + 22,
                    HudDesign.withAlpha(0xFFA5A5A5, presence));
            painter.drawLeft(graphics, heldCountValue.update(count), countX, y + 22,
                    HudDesign.withAlpha(0xFFA5A5A5, presence));
        }
        graphics.drawString(font, "MAIN HAND", x + 8, y + height - 12,
                HudDesign.withAlpha(accent, presence));
        graphics.pose().popMatrix();
    }

    /** Records the last sighting of every other player so sudden disappearances can be shown briefly. */
    private void refreshRetention(Minecraft client, HudModule hud) {
        long window = hud.vanishWindowMillis();
        if (window != retentionWindowMillis) {
            retentionWindowMillis = window;
            retention.setWindowMillis(window);
        }
        if (client.level == null || client.getConnection() == null) {
            retention.clear();
            return;
        }
        long now = System.currentTimeMillis();
        Set<UUID> inWorld = new HashSet<>();
        for (Player player : client.level.players()) {
            if (player == client.player) {
                continue;
            }
            inWorld.add(player.getUUID());
            retention.seen(player.getUUID(), player.getName().getString(),
                    player.getX(), player.getY(), player.getZ(), now);
        }
        for (UUID id : retention.ids()) {
            if (inWorld.contains(id)) {
                continue;
            }
            if (client.getConnection().getPlayerInfo(id) != null) {
                retention.retain(id, now);
            } else {
                retention.remove(id);
            }
        }
        retention.sweep(now);
    }

    private void renderStaff(GuiGraphics graphics, Font font, Minecraft client, boolean preview) {
        HudModule hud = (HudModule) modules.get("hud");
        long nowNanos = System.nanoTime();
        Set<UUID> inWorld = new HashSet<>();
        if (client.level != null) {
            for (Player player : client.level.players()) {
                inWorld.add(player.getUUID());
            }
        }
        List<StaffEntry> detected = new ArrayList<>();
        List<StaffClassifier.Rule> rules = StaffClassifier.parseRules(hud.staffPatterns());
        if (client.getConnection() != null) {
            for (PlayerInfo info : client.getConnection().getOnlinePlayers()) {
                var profile = info.getProfile();
                var team = info.getTeam();
                var teamColor = team == null ? null : team.getColor();
                StaffClassifier.Candidate candidate = new StaffClassifier.Candidate(profile.name(),
                        team == null ? "" : team.getPlayerPrefix().getString(),
                        team == null ? "" : team.getPlayerSuffix().getString(),
                        teamColor == null ? null : teamColor.getName(),
                        teamColor == null ? null : teamColor.getColor());
                if (StaffClassifier.classify(candidate, rules).isPresent()) {
                    detected.add(new StaffEntry(profile.id(), profile.name(), inWorld.contains(profile.id())));
                }
            }
        }
        detected.sort(Comparator.comparing(StaffEntry::name, String.CASE_INSENSITIVE_ORDER));

        int limit = Math.max(1, Math.min(HudDesign.STAFF_MAX_ROWS, hud.staffRows()));
        Set<UUID> active = new HashSet<>();
        for (int index = 0; index < Math.min(limit, detected.size()); index++) {
            StaffEntry entry = detected.get(index);
            active.add(entry.id());
            StaffRow row = staffRows.computeIfAbsent(entry.id(), id -> new StaffRow(id, entry.name()));
            row.name = entry.name;
            row.present = entry.present();
        }
        for (Map.Entry<UUID, StaffRow> entry : staffRows.entrySet()) {
            if (!active.contains(entry.getKey())) {
                entry.getValue().present = false;
            }
        }
        Iterator<Map.Entry<UUID, StaffRow>> fading = staffRows.entrySet().iterator();
        while (fading.hasNext()) {
            Map.Entry<UUID, StaffRow> entry = fading.next();
            boolean shown = active.contains(entry.getKey());
            double opacity = entry.getValue().fade.update(shown ? 1 : 0, nowNanos, config.reducedMotion());
            entry.getValue().opacity = opacity;
            if (!shown && opacity < 0.02) {
                fading.remove();
            }
        }

        List<StaffRow> rows = new ArrayList<>(staffRows.values());
        rows.removeIf(row -> row.opacity < 0.02);
        rows.sort(Comparator.comparing((StaffRow row) -> !active.contains(row.id))
                .thenComparing(row -> row.name, String.CASE_INSENSITIVE_ORDER));
        int visibleRows = Math.max(1, Math.min(limit, rows.size()));
        int width = HudDesign.STAFF_WIDTH;
        int height = HudDesign.staffHeight(visibleRows);
        int x = widgetX("staff", 8, width, graphics.guiWidth());
        int y = widgetY("staff", graphics.guiHeight() - 210, height, graphics.guiHeight());
        beginWidget(graphics, "staff", x, y, width, height, preview);
        int accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
        glass(graphics, x, y, width, height, hudSurface());
        graphics.drawString(font, "STAFF", x + 7, y + 6, 0xFF8F8F8F);
        staffFade.update(staffCountText.update(detected.size()), nowNanos, config.reducedMotion());
        numbers(font).drawFadeRight(graphics, staffFade, nowNanos, x + width - 7, y + 6, 0xFFB9A8FF);
        if (rows.isEmpty()) {
            graphics.drawString(font, "No staff detected", x + 16, y + HudDesign.staffRowY(0), 0xFF8E879C);
        } else {
            for (int index = 0; index < Math.min(visibleRows, rows.size()); index++) {
                StaffRow row = rows.get(index);
                int rowY = y + HudDesign.staffRowY(index);
                graphics.fill(x + 8, rowY + 2, x + 11, rowY + 5, HudDesign.withAlpha(accent, row.opacity));
                graphics.drawString(font, font.plainSubstrByWidth(row.name, width - 26), x + 16, rowY,
                        HudDesign.withAlpha(row.present ? 0xFFE6E0F5 : 0xFF8E879C, row.opacity));
            }
        }
        graphics.pose().popMatrix();
    }

    private void renderRadar(GuiGraphics graphics, Minecraft client, boolean preview, int defaultY) {
        HudModule hud = (HudModule) modules.get("hud");
        int size = hud.radarSize();
        int height = HudDesign.radarPanelHeight(size);
        double range = hud.radarRange();
        boolean northUp = hud.radarNorthUp();
        long nowNanos = System.nanoTime();
        long nowMillis = System.currentTimeMillis();
        int x = widgetX("radar", graphics.guiWidth() - size - 8, size, graphics.guiWidth());
        int y = widgetY("radar", defaultY, height, graphics.guiHeight());
        beginWidget(graphics, "radar", x, y, size, height, preview);
        int accent = activeStyle.accentColor(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
        glass(graphics, x, y, size, height, hudSurface());
        graphics.fill(x + 2, y + size, x + size, y + size + 1, HAIRLINE);
        rangeFade.update(radarRangeText.update((int) Math.round(range)), nowNanos, config.reducedMotion());
        numbers(JilatonType.font()).drawSuffixedFade(graphics, rangeFade, nowNanos, "m", x + size - 6,
                y + size + 2, TEXT_PRIMARY, TEXT_SECONDARY);
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int radius = HudDesign.radarRadius(size);
        drawRadarGrid(graphics, centerX, centerY, radius, accent);

        Player self = client.player;
        if (self == null) {
            graphics.pose().popMatrix();
            return;
        }
        int blipRadius = Math.max(1, radius - 2);
        float yaw = self.getYRot();
        double selfX = self.getX();
        double selfZ = self.getZ();
        Set<UUID> active = new HashSet<>();
        if (client.level != null) {
            for (Player player : client.level.players()) {
                if (player == self) {
                    continue;
                }
                double dx = player.getX() - selfX;
                double dz = player.getZ() - selfZ;
                if (Math.hypot(dx, dz) > range) {
                    continue;
                }
                active.add(player.getUUID());
                RadarBlip blip = radarBlips.computeIfAbsent(player.getUUID(), id -> new RadarBlip());
                RadarMath.Point point = RadarMath.project(dx, dz, yaw, northUp, range, blipRadius);
                blip.opacity = blip.presence.update(1, nowNanos, config.reducedMotion());
                blip.drawX = centerX + blip.x.update(point.x(), nowNanos, config.reducedMotion());
                blip.drawY = centerY + blip.y.update(point.y(), nowNanos, config.reducedMotion());
                drawBlip(graphics, blip.drawX, blip.drawY, 0xFF8F8F8F, blip.opacity);
            }
        }
        for (VanishRetention.Snapshot snapshot : retention.snapshot(nowMillis)) {
            if (!snapshot.retained() || active.contains(snapshot.id()) || snapshot.id().equals(self.getUUID())) {
                continue;
            }
            double dx = snapshot.x() - selfX;
            double dz = snapshot.z() - selfZ;
            if (Math.hypot(dx, dz) > range) {
                continue;
            }
            active.add(snapshot.id());
            RadarBlip blip = radarBlips.computeIfAbsent(snapshot.id(), id -> new RadarBlip());
            RadarMath.Point point = RadarMath.project(dx, dz, yaw, northUp, range, blipRadius);
            blip.opacity = blip.presence.update(1, nowNanos, config.reducedMotion());
            blip.drawX = centerX + blip.x.update(point.x(), nowNanos, config.reducedMotion());
            blip.drawY = centerY + blip.y.update(point.y(), nowNanos, config.reducedMotion());
            double remaining = 1.0 - snapshot.ageMillis() / (double) Math.max(1, retention.windowMillis());
            drawBlip(graphics, blip.drawX, blip.drawY, 0xFF6E5E92,
                    blip.opacity * Math.max(0.25, Math.min(1.0, remaining)));
        }
        Iterator<Map.Entry<UUID, RadarBlip>> leaving = radarBlips.entrySet().iterator();
        while (leaving.hasNext()) {
            Map.Entry<UUID, RadarBlip> entry = leaving.next();
            if (active.contains(entry.getKey())) {
                continue;
            }
            double opacity = entry.getValue().presence.update(0, nowNanos, config.reducedMotion());
            entry.getValue().opacity = opacity;
            drawBlip(graphics, entry.getValue().drawX, entry.getValue().drawY, 0xFF8F8F8F, opacity);
            if (opacity < 0.02) {
                leaving.remove();
            }
        }
        graphics.pose().popMatrix();
    }

    private void drawRadarGrid(GuiGraphics graphics, int centerX, int centerY, int radius, int accent) {
        for (int index = 0; index < 64; index++) {
            double angle = Math.PI * 2 * index / 64.0;
            int px = centerX + (int) Math.round(Math.cos(angle) * radius);
            int py = centerY + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(px, py, px + 1, py + 1, 0x55FFFFFF);
        }
        int inner = Math.max(1, radius / 2);
        for (int index = 0; index < 48; index++) {
            double angle = Math.PI * 2 * index / 48.0;
            int px = centerX + (int) Math.round(Math.cos(angle) * inner);
            int py = centerY + (int) Math.round(Math.sin(angle) * inner);
            graphics.fill(px, py, px + 1, py + 1, 0x22FFFFFF);
        }
        graphics.fill(centerX - radius, centerY, centerX + radius + 1, centerY + 1, 0x1AFFFFFF);
        graphics.fill(centerX, centerY - radius, centerX + 1, centerY + radius + 1, 0x1AFFFFFF);
        graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, accent);
    }

    private void drawBlip(GuiGraphics graphics, double x, double y, int color, double opacity) {
        int px = (int) Math.round(x) - 1;
        int py = (int) Math.round(y) - 1;
        graphics.fill(px, py, px + 3, py + 3, HudDesign.withAlpha(color, opacity));
    }

    private int hudSurface() {
        return ((int) Math.round(activeStyle.panelOpacity() * 255) << 24) | 0x09090D;
    }

    private int widgetX(String widgetId, int defaultX, int widgetWidth, int guiWidth) {
        HudPosition position = config.hudPosition(widgetId);
        return position == null ? defaultX : (int) Math.round(Math.max(0, guiWidth - widgetWidth) * position.x());
    }

    private int widgetY(String widgetId, int defaultY, int widgetHeight, int guiHeight) {
        HudPosition position = config.hudPosition(widgetId);
        return position == null ? defaultY : (int) Math.round(Math.max(0, guiHeight - widgetHeight) * position.y());
    }

    /**
     * Default tops of the bottom-right column, ordered radar, held item, server, armor, keystrokes.
     * Every slot is reserved from the settings, so toggling one widget never shifts its neighbours;
     * a taller radar grows upward.
     */
    private static int[] bottomColumnTops(int guiHeight, HudModule hud) {
        return HudDesign.stackUp(guiHeight - HudDesign.HUD_MARGIN, HudDesign.STACK_GAP,
                HudDesign.radarPanelHeight(hud.radarSize()), HudDesign.HELD_ITEM_HEIGHT,
                HudDesign.SERVER_HEIGHT, HudDesign.ARMOR_HEIGHT, HudDesign.KEYSTROKES_HEIGHT);
    }

    private static int latency(Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            return -1;
        }
        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    private HudNumberPainter numbers(Font font) {
        if (numberPainter == null || numberFont != font) {
            numberFont = font;
            numberPainter = new HudNumberPainter(font);
        }
        return numberPainter;
    }

    private void resetNumeric() {
        fpsFade.reset();
        pingFade.reset();
        armorFade.reset();
        rangeFade.reset();
        staffFade.reset();
        cpsLeftFade.reset();
        cpsRightFade.reset();
        serverPingFade.reset();
        serverPlayersFade.reset();
        clockFade.reset();
        coordinateXFade.reset();
        coordinateYFade.reset();
        coordinateZFade.reset();
        speedFade.reset();
        targetDistanceFade.reset();
        targetHealthFade.reset();
        compactDistanceFade.reset();
        aboveHeadHealthFade.reset();
    }

    private void refreshCoordinates(Player player) {
        coordinateX.update((int) Math.floor(player.getX() * 10.0));
        coordinateY.update((int) Math.floor(player.getY() * 10.0));
        coordinateZ.update((int) Math.floor(player.getZ() * 10.0));
    }

    /** Fits the held item's real name into the label column without cutting a word in half. */
    private String fitHeldLabel(Font font, String source) {
        int space = HudDesign.HELD_ITEM_WIDTH - HudDesign.HELD_ITEM_LABEL_X - HudDesign.HELD_ITEM_PADDING;
        if (!source.equals(heldLabelSource) || space != heldLabelSpace) {
            heldLabelSource = source;
            heldLabelSpace = space;
            heldLabelText = numbers(font).ellipsize(source, space);
        }
        return heldLabelText;
    }

    /** Lowest remaining durability among worn damageable pieces, or -1 when nothing is worn. */
    private static int worstDurabilityPercent(Minecraft client) {
        int worst = -1;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = client.player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                continue;
            }
            int maximum = Math.max(1, stack.getMaxDamage());
            int percent = (int) Math.round(Math.max(0, 1.0 - stack.getDamageValue() / (double) maximum) * 100);
            worst = worst < 0 ? percent : Math.min(worst, percent);
        }
        return worst;
    }

    /** Re-uses the formatted string until the integer changes, so steady frames allocate nothing. */
    private static final class IntText {
        private int last = Integer.MIN_VALUE;
        private String text = "";

        private String update(int value) {
            if (value != last) {
                last = value;
                text = Integer.toString(value);
            }
            return text;
        }
    }

    private static final class TenthsText {
        private int last = Integer.MIN_VALUE;
        private String text = "";

        private String update(int tenths) {
            if (tenths != last) {
                last = tenths;
                text = String.format(Locale.ROOT, "%.1f", tenths / 10.0);
            }
            return text;
        }

        private String text() {
            return text;
        }
    }

    private static final class ClockText {
        private long lastMinute = Long.MIN_VALUE;
        private String text = "--:--";

        private String update(long epochMillis) {
            long minute = Math.floorDiv(epochMillis, 60_000L);
            if (minute != lastMinute) {
                lastMinute = minute;
                text = LocalTime.now().format(CLOCK);
            }
            return text;
        }
    }

    private void renderKeystrokes(GuiGraphics graphics, Minecraft client, int accent, int x, int y) {
        Font font = JilatonType.font();
        drawKey(graphics, font, client.options.keyUp, x + 24, y, 22, 19, accent);
        drawKey(graphics, font, client.options.keyLeft, x, y + 21, 22, 19, accent);
        drawKey(graphics, font, client.options.keyDown, x + 24, y + 21, 22, 19, accent);
        drawKey(graphics, font, client.options.keyRight, x + 48, y + 21, 22, 19, accent);
        drawKey(graphics, font, client.options.keyJump, x, y + 42, 70, 16, accent);
    }

    private void drawKey(GuiGraphics graphics, Font font, net.minecraft.client.KeyMapping mapping,
                                int x, int y, int width, int height, int accent) {
        boolean down = mapping.isDown();
        if (down) JilatonUi.fillRounded(graphics, x, y, width, height, 5, 0xF2F5F5F5);
        else glass(graphics, x, y, width, height, activeStyle.panelColor());
        String label = mapping.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
        if (width <= 22 && label.length() > 3) {
            label = label.substring(0, 3);
        }
        int labelX = x + (width - font.width(label)) / 2;
        graphics.drawString(font, label, labelX, y + (height - 8) / 2, down ? 0xFF0A0A0A : 0xFFB5B5B5, false);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        glass(graphics, x, y, width, height, activeStyle.panelColor());
    }

    /** Board HUD surface: rounded 7px ink chip with a faint white hairline. */
    private static void glass(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        JilatonUi.fillRounded(graphics, x, y, width, height, 7, fill);
        JilatonUi.strokeRounded(graphics, x, y, width, height, 7, HudDesign.withAlpha(0x14FFFFFF, (fill >>> 24) / 255.0));
    }

    private record StaffEntry(UUID id, String name, boolean present) { }

    private static final class StaffRow {
        private final UUID id;
        private final AnimatedValue fade = new AnimatedValue();
        private String name;
        private boolean present;
        private double opacity;

        private StaffRow(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final class RadarBlip {
        private final AnimatedValue x = new AnimatedValue();
        private final AnimatedValue y = new AnimatedValue();
        private final AnimatedValue presence = new AnimatedValue();
        private double opacity;
        private double drawX;
        private double drawY;
    }

    public record WidgetBounds(String id, int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
