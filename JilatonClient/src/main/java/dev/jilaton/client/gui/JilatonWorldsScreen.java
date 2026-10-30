package dev.jilaton.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.validation.ContentValidationException;
import com.mojang.datafixers.util.Pair;
import java.io.IOException;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Singleplayer worlds as cards, with vanilla's play, create, edit, delete and re-create flows. */
public final class JilatonWorldsScreen extends JilatonCardScreen<LevelSummary> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<LevelSummary> worlds = new ArrayList<>();
    private final Map<String, FaviconTexture> icons = new HashMap<>();
    private boolean loading;
    private boolean loaded;
    private Button play;
    private Button edit;
    private Button delete;
    private Button recreate;

    public JilatonWorldsScreen(Screen parent) {
        super(Component.literal("Worlds"), "01  WORLDS", parent);
    }

    @Override
    protected void init() {
        super.init();
        if (!loaded && !loading) loadWorlds();
        else if (loaded && icons.isEmpty()) loadIcons();
    }

    private void loadWorlds() {
        loading = true;
        LevelStorageSource source = minecraft.getLevelSource();
        LevelStorageSource.LevelCandidates candidates;
        try {
            candidates = source.findLevelCandidates();
        } catch (LevelStorageException exception) {
            LOGGER.error("Couldn't load level list", exception);
            loading = false;
            loaded = true;
            return;
        }
        source.loadLevelSummaries(candidates).whenComplete((summaries, failure) -> minecraft.execute(() -> {
            loading = false;
            loaded = true;
            worlds.clear();
            if (failure != null) {
                LOGGER.error("Couldn't load level list", failure);
            } else {
                worlds.addAll(summaries);
                worlds.sort(null);
            }
            loadIcons();
            refreshFilter();
        }));
    }

    private void loadIcons() {
        for (LevelSummary summary : worlds) {
            Path file = summary.getIcon();
            if (file == null || Files.isSymbolicLink(file) || !Files.isRegularFile(file)) continue;
            FaviconTexture texture = FaviconTexture.forWorld(minecraft.getTextureManager(), summary.getLevelId());
            try (InputStream input = Files.newInputStream(file)) {
                texture.upload(NativeImage.read(input));
                icons.put(summary.getLevelId(), texture);
            } catch (Throwable failure) {
                LOGGER.warn("Invalid icon for world {}", summary.getLevelId(), failure);
                texture.close();
            }
        }
    }

    @Override
    protected List<LevelSummary> source() { return worlds; }

    @Override
    protected String searchText(LevelSummary item) { return item.getLevelName() + " " + item.getLevelId(); }

    @Override
    protected String subtitle(int shown) {
        if (loading) return "Loading worlds…";
        return shown + (shown == 1 ? " world" : " worlds") + "  ·  double-click to play";
    }

    @Override
    protected Component emptyMessage() {
        return Component.literal(loading ? "" : "No worlds yet — create one to begin.");
    }

    @Override
    protected void addActions(int y) {
        int w = Math.min(100, (gridW - 30) / 6);
        int x = width - margin - (w * 6 + 30);
        play = addRenderableWidget(Button.builder(Component.literal("Play"), b -> { if (selected != null) activate(selected); })
                .bounds(x, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Create"),
                b -> CreateWorldScreen.openFresh(minecraft, this::returnHere)).bounds(x + (w + 6), y, w, 20).build());
        edit = addRenderableWidget(Button.builder(Component.literal("Edit"), b -> { if (selected != null) editWorld(selected); })
                .bounds(x + (w + 6) * 2, y, w, 20).build());
        delete = addRenderableWidget(Button.builder(Component.literal("Delete"), b -> { if (selected != null) confirmDelete(selected); })
                .bounds(x + (w + 6) * 3, y, w, 20).build());
        recreate = addRenderableWidget(Button.builder(Component.literal("Re-create"), b -> { if (selected != null) recreateWorld(selected); })
                .bounds(x + (w + 6) * 4, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(x + (w + 6) * 5, y, w, 20).build());
        selectionChanged();
    }

    @Override
    protected void selectionChanged() {
        if (play == null) return;
        play.active = selected != null && selected.primaryActionActive();
        edit.active = selected != null && selected.canEdit();
        delete.active = selected != null && selected.canDelete();
        recreate.active = selected != null && selected.canRecreate();
    }

    @Override
    protected void activate(LevelSummary item) {
        if (item instanceof LevelSummary.SymlinkLevelSummary) {
            minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> minecraft.setScreen(this)));
            return;
        }
        if (!item.primaryActionActive()) return;
        minecraft.createWorldOpenFlows().openWorld(item.getLevelId(), this::returnHere);
    }

    /** Comes back to this screen with a freshly loaded world list (names, icons and dates may have changed). */
    private void returnHere() {
        loaded = false;
        worlds.clear();
        selected = null;
        minecraft.setScreen(this);
    }

    private void confirmDelete(LevelSummary item) {
        if (!item.canDelete()) return;
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                minecraft.setScreen(new ProgressScreen(true));
                deleteWorld(item.getLevelId());
            }
            returnHere();
        }, Component.translatable("selectWorld.deleteQuestion"),
                Component.translatable("selectWorld.deleteWarning", item.getLevelName()),
                Component.translatable("selectWorld.deleteButton"), CommonComponents.GUI_CANCEL));
    }

    private void deleteWorld(String levelId) {
        try (LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(levelId)) {
            access.deleteLevel();
        } catch (IOException exception) {
            SystemToast.onWorldDeleteFailure(minecraft, levelId);
            LOGGER.error("Failed to delete world {}", levelId, exception);
        }
    }

    private void editWorld(LevelSummary item) {
        if (!item.canEdit()) return;
        String levelId = item.getLevelId();
        minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
        LevelStorageSource.LevelStorageAccess access;
        try {
            access = minecraft.getLevelSource().validateAndCreateAccess(levelId);
        } catch (IOException exception) {
            SystemToast.onWorldAccessFailure(minecraft, levelId);
            LOGGER.error("Failed to access level {}", levelId, exception);
            returnHere();
            return;
        } catch (ContentValidationException exception) {
            LOGGER.warn("{}", exception.getMessage());
            minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> minecraft.setScreen(this)));
            return;
        }
        try {
            minecraft.setScreen(EditWorldScreen.create(minecraft, access, changed -> {
                access.safeClose();
                returnHere();
            }));
        } catch (IOException | NbtException | ReportedNbtException exception) {
            access.safeClose();
            SystemToast.onWorldAccessFailure(minecraft, levelId);
            LOGGER.error("Failed to load world data {}", levelId, exception);
            returnHere();
        }
    }

    private void recreateWorld(LevelSummary item) {
        if (!item.canRecreate()) return;
        minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
        try (LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().validateAndCreateAccess(item.getLevelId())) {
            Pair<LevelSettings, WorldCreationContext> data = minecraft.createWorldOpenFlows().recreateWorldData(access);
            LevelSettings settings = data.getFirst();
            WorldCreationContext context = data.getSecond();
            Path packs = CreateWorldScreen.createTempDataPackDirFromExistingWorld(access.getLevelPath(LevelResource.DATAPACK_DIR), minecraft);
            context.validate();
            if (context.options().isOldCustomizedWorld()) {
                minecraft.setScreen(new ConfirmScreen(proceed -> minecraft.setScreen(proceed
                        ? CreateWorldScreen.createFromExisting(minecraft, this::returnHere, settings, context, packs) : this),
                        Component.translatable("selectWorld.recreate.customized.title"),
                        Component.translatable("selectWorld.recreate.customized.text"),
                        CommonComponents.GUI_PROCEED, CommonComponents.GUI_CANCEL));
            } else {
                minecraft.setScreen(CreateWorldScreen.createFromExisting(minecraft, this::returnHere, settings, context, packs));
            }
        } catch (ContentValidationException exception) {
            LOGGER.warn("{}", exception.getMessage());
            minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> minecraft.setScreen(this)));
        } catch (Exception exception) {
            LOGGER.error("Unable to recreate world", exception);
            minecraft.setScreen(new AlertScreen(() -> minecraft.setScreen(this),
                    Component.translatable("selectWorld.recreate.error.title"),
                    Component.translatable("selectWorld.recreate.error.text")));
        }
    }

    @Override
    protected void renderCard(GuiGraphics graphics, LevelSummary item, int x, int y, int w, int h, double hover,
                              boolean selected, double alpha, long now) {
        cardSurface(graphics, x, y, w, h, hover, selected, alpha);
        FaviconTexture icon = icons.get(item.getLevelId());
        cardIcon(graphics, icon == null || icon.isClosed() ? null : icon.textureLocation(), item.getLevelName(), x + 8, y + 8, 48, alpha);
        int textX = x + 66;
        int textW = w - 74;
        String badge = badge(item);
        int badgeW = badge.isEmpty() ? 0 : font.width(badge) + 8;
        graphics.drawString(font, fit(item.getLevelName(), textW - badgeW), textX, y + 11, JilatonUi.alpha(JilatonUi.TEXT, alpha), false);
        if (!badge.isEmpty()) {
            int bx = x + w - badgeW - 8;
            int color = item.isHardcore() || !item.isCompatible() ? JilatonUi.DANGER : JilatonUi.TEXT_3;
            JilatonUi.strokeRounded(graphics, bx, y + 9, badgeW, 12, 2, JilatonUi.alpha(color, alpha * 0.6));
            graphics.drawString(font, badge, bx + 4, y + 11, JilatonUi.alpha(color, alpha), false);
        }
        String mode = capitalize(item.getGameMode().getName()) + (item.hasCommands() ? "  ·  Cheats" : "");
        graphics.drawString(font, fit(mode, textW), textX, y + 26, JilatonUi.alpha(JilatonUi.TEXT_2, alpha), false);
        String played = item.getLastPlayed() > 0 ? "Played " + ago(item.getLastPlayed()) : item.getLevelId();
        graphics.drawString(font, fit(played, textW - 40), textX, y + 41, JilatonUi.alpha(JilatonUi.TEXT_4, alpha), false);
        if (hover > 0.01 && item.primaryActionActive()) {
            String cue = "PLAY ↗";
            graphics.drawString(font, cue, x + w - font.width(cue) - 9, y + 41, JilatonUi.alpha(JilatonUi.TEXT, alpha * hover), false);
        }
    }

    private static String badge(LevelSummary item) {
        if (item.isLocked()) return "IN USE";
        if (!item.isCompatible()) return "INCOMPATIBLE";
        if (item.isHardcore()) return "HARDCORE";
        if (item.isExperimental()) return "EXPERIMENTAL";
        return "";
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    static String ago(long epochMillis) {
        long seconds = Math.max(0, (System.currentTimeMillis() - epochMillis) / 1000);
        if (seconds < 60) return "just now";
        if (seconds < 3600) return seconds / 60 + "m ago";
        if (seconds < 86_400) return seconds / 3600 + "h ago";
        if (seconds < 86_400 * 30) return seconds / 86_400 + "d ago";
        return seconds / (86_400 * 30) + "mo ago";
    }

    @Override
    public void removed() {
        icons.values().forEach(FaviconTexture::close);
        icons.clear();
    }
}
