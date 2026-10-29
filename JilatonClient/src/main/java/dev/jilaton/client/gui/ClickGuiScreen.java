package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ClickGuiScreen extends Screen {
    private enum Page {
        MODULES,
        CONFIGS,
        KEYBINDS
    }

    private static final int ACCENT = 0xFF8878FF;
    private static final int PANEL = 0xFF080C19;
    private static final int SIDEBAR = 0xFF0B1020;
    private static final int CARD = 0xFF101629;
    private static final int CARD_HOVER = 0xFF151D34;
    private static final int BORDER = 0xFF1B2339;
    private final ModuleManager modules;
    private final ClientConfig config;
    private final Set<String> expanded = new HashSet<>();
    private EditBox search;
    private EditBox profileName;
    private List<String> profiles = List.of();
    private String selectedProfile;
    private String profileStatus = "Profiles are stored on this computer.";
    private boolean profileStatusError;
    private Page page = Page.MODULES;
    private String selectedCategory = "All";
    private String bindingModuleId;
    private boolean showEnabledOnly;
    private boolean compactCards;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentWidth;
    private int searchContainerX;
    private int searchContainerWidth;
    private int profileSaveX;
    private int profileLoadX;
    private int profileDeleteX;
    private double scrollOffset;
    private double targetScrollOffset;
    private long lastFrameNanos;

    public ClickGuiScreen(ModuleManager modules, ClientConfig config) {
        super(Component.literal("Jilaton Client"));
        this.modules = modules;
        this.config = config;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(800, width - 32);
        panelHeight = Math.min(510, height - 32);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        int sidebarWidth = Math.min(174, Math.max(116, panelWidth / 4));
        contentX = panelX + sidebarWidth + 15;
        contentWidth = panelWidth - sidebarWidth - 28;
        searchContainerWidth = Math.min(218, Math.max(80, contentWidth - 180));
        searchContainerX = contentX + contentWidth - searchContainerWidth;
        search = new EditBox(font, searchContainerX + 18, panelY + 18,
                Math.max(58, searchContainerWidth - 24), 22, Component.literal("Search modules"));
        search.setHint(Component.literal("Search modules"));
        search.setBordered(false);
        search.setTextColor(0xFFE5E9F7);
        search.setResponder(value -> {
            targetScrollOffset = 0.0;
            scrollOffset = 0.0;
        });
        search.setVisible(page != Page.CONFIGS);
        addRenderableWidget(search);

        int profileInputWidth = Math.min(214, Math.max(80, contentWidth - 224));
        profileSaveX = contentX + profileInputWidth + 8;
        profileLoadX = profileSaveX + 70;
        profileDeleteX = profileLoadX + 70;
        profileName = new EditBox(font, contentX, viewportTop() + 15, profileInputWidth, 22, Component.literal("Profile name"));
        profileName.setHint(Component.literal("Profile name"));
        profileName.setMaxLength(32);
        profileName.setVisible(page == Page.CONFIGS);
        addRenderableWidget(profileName);
        refreshProfiles();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        double frameSeconds = lastFrameNanos == 0L ? 0.0 : Math.min(0.05, (now - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = now;
        int maxScroll = maximumScroll();
        targetScrollOffset = Math.max(0.0, Math.min(maxScroll, targetScrollOffset));
        double animation = 1.0 - Math.exp(-frameSeconds / 0.075);
        scrollOffset += (targetScrollOffset - scrollOffset) * animation;
        if (Math.abs(targetScrollOffset - scrollOffset) < 0.15) {
            scrollOffset = targetScrollOffset;
        }

        graphics.fill(0, 0, width, height, 0x9906080D);
        fillRounded(graphics, panelX, panelY, panelWidth, panelHeight, 9, PANEL);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 3, ACCENT);
        graphics.fill(panelX + 1, panelY + 4, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xFF0A0E1B);
        graphics.fill(contentX - 10, panelY + 8, contentX - 9, panelY + panelHeight - 8, BORDER);
        renderHeader(graphics);
        renderSidebar(graphics, mouseX, mouseY);
        switch (page) {
            case MODULES -> renderModules(graphics, mouseX, mouseY);
            case CONFIGS -> renderConfigs(graphics, mouseX, mouseY);
            case KEYBINDS -> renderKeybinds(graphics, mouseX, mouseY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics graphics) {
        graphics.fill(panelX + 1, panelY + 4, contentX - 10, panelY + panelHeight - 1, SIDEBAR);
        fillRounded(graphics, panelX + 17, panelY + 15, 22, 22, 6, 0xFF252044);
        graphics.drawString(font, "J", panelX + 24, panelY + 22, ACCENT);
        graphics.drawString(font, "JILATON", panelX + 46, panelY + 18, 0xFFF1F3FC);
        graphics.drawString(font, "CLIENT  /  0.1", panelX + 46, panelY + 32, 0xFF7F879B);
        String status = bindingModuleId == null ? "JILATON  ·  PVP MODULES" : "PRESS KEY  ·  ESC CANCEL  ·  DEL CLEAR";
        graphics.drawString(font, status, panelX + 20, panelY + panelHeight - 25, bindingModuleId == null ? 0xFF707A91 : ACCENT);

        String title = switch (page) {
            case MODULES -> selectedCategory.equals("All") ? "All Modules"
                    : selectedCategory.equals("Visuals") ? "Visual Modules" : selectedCategory + " Modules";
            case CONFIGS -> "Config Profiles";
            case KEYBINDS -> "Keybinds";
        };
        graphics.drawString(font, title, contentX, panelY + 20, 0xFFF2F4FC);
        int count = 0;
        int active = 0;
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        for (ClientModule module : modules.all()) {
            if (matches(module, query) && (page != Page.MODULES || categoryMatches(module))
                    && (!showEnabledOnly || page != Page.MODULES || module.enabled())) {
                count++;
                if (module.enabled()) active++;
            }
        }
        String subtitle = switch (page) {
            case MODULES -> count + " modules  ·  " + active + " enabled";
            case CONFIGS -> profiles.size() + (profiles.size() == 1 ? " saved profile" : " saved profiles");
            case KEYBINDS -> active + " modules enabled  ·  click a row to edit its key";
        };
        graphics.drawString(font, subtitle, contentX, panelY + 38, 0xFF8791A9);

        if (page != Page.CONFIGS) {
            fillRounded(graphics, searchContainerX, panelY + 16, searchContainerWidth, 26, 6, 0xFF0D1425);
            graphics.fill(searchContainerX + 9, panelY + 24, searchContainerX + 15, panelY + 30, 0xFF8D96AD);
            graphics.fill(searchContainerX + 13, panelY + 29, searchContainerX + 17, panelY + 31, 0xFF8D96AD);
        }
    }

    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        int sidebarWidth = contentX - panelX - 20;
        int dividerX = panelX + sidebarWidth;
        graphics.fill(dividerX, panelY + 62, dividerX + 1, panelY + panelHeight - 18, BORDER);
        graphics.drawString(font, "MODULES", panelX + 19, panelY + 73, 0xFF737F98);

        String[] categories = {"All", "Combat", "Movement", "Visuals", "Client"};
        int rowY = panelY + 92;
        for (String category : categories) {
            int rowX = panelX + 12;
            boolean selected = page == Page.MODULES && selectedCategory.equals(category);
            int rowWidth = dividerX - rowX - 12;
            int moduleCount = category.equals("All") ? modules.all().size() : (int) modules.all().stream()
                    .filter(module -> module.category().displayName().equals(category)).count();
            boolean hovered = mouseX >= rowX && mouseX <= rowX + rowWidth && mouseY >= rowY - 5 && mouseY <= rowY + 15;
            if (selected) {
                fillRounded(graphics, rowX, rowY - 6, rowWidth, 24, 6, 0xFF201D3D);
                graphics.fill(rowX, rowY - 2, rowX + 2, rowY + 14, ACCENT);
            } else if (hovered) {
                fillRounded(graphics, rowX, rowY - 6, rowWidth, 24, 6, 0xFF131A2B);
            }
            graphics.drawString(font, category, rowX + 12, rowY, selected ? 0xFFF1EFFF : 0xFFAAB2C5);
            String countText = Integer.toString(moduleCount);
            graphics.drawString(font, countText, rowX + rowWidth - font.width(countText) - 10, rowY,
                    selected ? 0xFFCBC5FF : 0xFF6F7890);
            rowY += 28;
        }

        graphics.drawString(font, "GENERAL", panelX + 19, panelY + 246, 0xFF737F98);
        renderSidebarLink(graphics, mouseX, mouseY, "Configs", Page.CONFIGS, panelY + 266, dividerX);
        renderSidebarLink(graphics, mouseX, mouseY, "Keybinds", Page.KEYBINDS, panelY + 294, dividerX);

        graphics.drawString(font, "RIGHT SHIFT", panelX + 19, panelY + panelHeight - 46, 0xFF8993AA);
        graphics.drawString(font, "TOGGLE MENU", panelX + 19, panelY + panelHeight - 32, 0xFF626D84);
    }

    private void renderSidebarLink(GuiGraphics graphics, int mouseX, int mouseY, String label, Page target, int y, int dividerX) {
        int x = panelX + 12;
        int width = dividerX - x - 12;
        boolean selected = page == target;
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y - 6 && mouseY <= y + 18;
        if (selected) {
            fillRounded(graphics, x, y - 6, width, 24, 6, 0xFF201D3D);
            graphics.fill(x, y - 2, x + 2, y + 14, ACCENT);
        } else if (hovered) {
            fillRounded(graphics, x, y - 6, width, 24, 6, 0xFF131A2B);
        }
        graphics.drawString(font, label, x + 12, y, selected ? 0xFFF1EFFF : 0xFFAAB2C5);
    }

    private void setPage(Page nextPage) {
        page = nextPage;
        search.setVisible(page != Page.CONFIGS);
        profileName.setVisible(page == Page.CONFIGS);
        setFocused(null);
        scrollOffset = 0.0;
        targetScrollOffset = 0.0;
    }

    private void renderModules(GuiGraphics graphics, int mouseX, int mouseY) {
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        renderToolbar(graphics, mouseX, mouseY);
        int viewportTop = viewportTop();
        int viewportBottom = viewportBottom();
        int viewportHeight = viewportBottom - viewportTop;
        int cardY = viewportTop - (int) Math.round(scrollOffset);
        int cardX = contentX;
        int cardWidth = contentWidth - 2;
        boolean any = false;

        graphics.enableScissor(cardX, viewportTop, cardX + cardWidth, viewportBottom);

        for (ClientModule module : modules.all()) {
            if (!matches(module, query) || !categoryMatches(module) || (showEnabledOnly && !module.enabled())) {
                continue;
            }
            any = true;
            boolean isExpanded = expanded.contains(module.id());
            int baseHeight = cardBaseHeight();
            int settingsHeight = isExpanded ? module.settings().size() * 34 + 10 : 0;
            int cardHeight = baseHeight + settingsHeight;
            boolean hovered = mouseX >= cardX && mouseX <= cardX + cardWidth && mouseY >= cardY && mouseY <= cardY + cardHeight;
            fillRounded(graphics, cardX, cardY, cardWidth, cardHeight, 7, hovered ? CARD_HOVER : CARD);
            graphics.fill(cardX, cardY + 8, cardX + 2, cardY + baseHeight - 8, module.enabled() ? ACCENT : 0xFF2D3750);
            graphics.drawString(font, module.name(), cardX + 14, cardY + 10, 0xFFF0F3FC);
            graphics.drawString(font, module.description(), cardX + 14, cardY + 28, 0xFF8993AA);
            drawToggle(graphics, cardX + cardWidth - 40, cardY + 13, module.enabled());
            fillRounded(graphics, cardX + cardWidth - 108, cardY + 10, 48, 20, 5,
                    bindingModuleId != null && bindingModuleId.equals(module.id()) ? 0xFF423B70 : 0xFF1A2236);
            String keyName = bindingModuleId != null && bindingModuleId.equals(module.id()) ? "PRESS" : keyName(module.keyCode());
            if (keyName.length() > 6) {
                keyName = keyName.substring(0, 5) + "…";
            }
            int keyTextX = cardX + cardWidth - 108 + (50 - font.width(keyName)) / 2;
            graphics.drawString(font, keyName, keyTextX, cardY + 16, 0xFFC5C9D7);
            graphics.drawString(font, isExpanded ? "⌄" : "›", cardX + cardWidth - 134, cardY + 13, 0xFFB1BAD0);
            if (!compactCards) {
                String category = module.category().displayName().toUpperCase(Locale.ROOT);
                int pillWidth = font.width(category) + 12;
                fillRounded(graphics, cardX + 14, cardY + 48, pillWidth, 14, 5, 0xFF171E31);
                graphics.drawString(font, category, cardX + 20, cardY + 51, 0xFF8995AF);
            }

            if (isExpanded) {
                int settingY = cardY + baseHeight + 8;
                for (Setting setting : module.settings()) {
                    renderSetting(graphics, setting, cardX + 14, settingY, cardWidth - 28, mouseX);
                    settingY += 34;
                }
            }
            cardY += cardHeight + 8;
        }

        if (!any) {
            graphics.drawString(font, "No modules match this view.", contentX + 8, viewportTop + 37, 0xFF9A9BA8);
        }
        graphics.disableScissor();

        int totalHeight = totalContentHeight();
        if (totalHeight > viewportHeight) {
            int trackX = panelX + panelWidth - 13;
            int trackTop = viewportTop;
            int trackHeight = viewportHeight;
            int thumbHeight = Math.max(24, (int) ((long) trackHeight * trackHeight / totalHeight));
            int maxThumbTravel = trackHeight - thumbHeight;
            int scrollRange = maxScroll(totalHeight, viewportHeight);
            int thumbY = trackTop + (scrollRange == 0 ? 0 : (int) Math.round(maxThumbTravel * scrollOffset / scrollRange));
            graphics.fill(trackX, trackTop, trackX + 2, trackTop + trackHeight, BORDER);
            fillRounded(graphics, trackX - 1, thumbY, 4, thumbHeight, 2, ACCENT);
        }
    }

    private void renderToolbar(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = panelY + 65;
        fillRounded(graphics, contentX, y, 64, 22, 6, showEnabledOnly ? 0xFF171E31 : 0xFF292445);
        graphics.drawString(font, "ALL", contentX + 13, y + 7, showEnabledOnly ? 0xFF8993AA : 0xFFD8D2FF);
        fillRounded(graphics, contentX + 70, y, 90, 22, 6, showEnabledOnly ? 0xFF292445 : 0xFF171E31);
        graphics.drawString(font, "ENABLED", contentX + 80, y + 7, showEnabledOnly ? 0xFFD8D2FF : 0xFF8993AA);

        int densityX = contentX + contentWidth - 82;
        fillRounded(graphics, densityX, y, 78, 22, 6, 0xFF11182A);
        graphics.drawString(font, compactCards ? "COMPACT" : "ROOMY", densityX + 9, y + 7,
                compactCards ? 0xFFB8ADFF : 0xFF9CA6BC);
        if (mouseX >= densityX && mouseX <= densityX + 78 && mouseY >= y && mouseY <= y + 22) {
            graphics.fill(densityX, y + 21, densityX + 78, y + 22, ACCENT);
        }
    }

    private void renderConfigs(GuiGraphics graphics, int mouseX, int mouseY) {
        int top = viewportTop();
        int inputY = top + 15;
        drawActionButton(graphics, "SAVE", profileSaveX, inputY, 64, ACCENT, mouseX, mouseY);
        drawActionButton(graphics, "LOAD", profileLoadX, inputY, 64, selectedProfile == null ? 0xFF20283A : 0xFF34305C, mouseX, mouseY);
        drawActionButton(graphics, "DELETE", profileDeleteX, inputY, 72, selectedProfile == null ? 0xFF20283A : 0xFF3A2634, mouseX, mouseY);
        graphics.drawString(font, profileStatus, contentX, inputY + 31, profileStatusError ? 0xFFFF858D : 0xFF8993AA);

        int listTop = top + 63;
        int rowY = listTop - (int) Math.round(scrollOffset);
        graphics.enableScissor(contentX, listTop, contentX + contentWidth, viewportBottom());
        if (profiles.isEmpty()) {
            graphics.drawString(font, "No profiles yet. Enter a name and save the current setup.", contentX + 2, listTop + 16, 0xFF8993AA);
        } else {
            for (String profile : profiles) {
                boolean selected = profile.equals(selectedProfile);
                boolean hovered = mouseX >= contentX && mouseX <= contentX + contentWidth
                        && mouseY >= rowY && mouseY <= rowY + 34;
                fillRounded(graphics, contentX, rowY, contentWidth - 4, 34, 6,
                        selected ? 0xFF211E3B : hovered ? CARD_HOVER : CARD);
                graphics.drawString(font, profile, contentX + 13, rowY + 12, selected ? 0xFFE6E1FF : 0xFFE0E5F2);
                graphics.drawString(font, selected ? "SELECTED" : "PROFILE", contentX + contentWidth - 72, rowY + 12,
                        selected ? 0xFFB4A9FF : 0xFF77839C);
                rowY += 42;
            }
        }
        graphics.disableScissor();
        drawScrollbar(graphics, listTop, viewportBottom(), profiles.size() * 42 - 8);
    }

    private void renderKeybinds(GuiGraphics graphics, int mouseX, int mouseY) {
        int top = viewportTop();
        int rowY = top - (int) Math.round(scrollOffset);
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        graphics.enableScissor(contentX, top, contentX + contentWidth, viewportBottom());
        int visible = 0;
        for (ClientModule module : modules.all()) {
            if (!matches(module, query)) {
                continue;
            }
            visible++;
            boolean hovered = mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= rowY && mouseY <= rowY + 44;
            fillRounded(graphics, contentX, rowY, contentWidth - 4, 44, 6, hovered ? CARD_HOVER : CARD);
            graphics.drawString(font, module.name(), contentX + 13, rowY + 8, 0xFFE9EDF7);
            graphics.drawString(font, module.category().displayName(), contentX + 13, rowY + 25, 0xFF7F8AA2);
            fillRounded(graphics, contentX + contentWidth - 122, rowY + 10, 108, 24, 6,
                    bindingModuleId != null && bindingModuleId.equals(module.id()) ? 0xFF393260 : 0xFF1A2236);
            String binding = bindingModuleId != null && bindingModuleId.equals(module.id()) ? "PRESS A KEY" : keyName(module.keyCode());
            int bindingWidth = font.width(binding);
            graphics.drawString(font, binding, contentX + contentWidth - 122 + (108 - bindingWidth) / 2,
                    rowY + 18, 0xFFD1D6E4);
            rowY += 52;
        }
        if (visible == 0) {
            graphics.drawString(font, "No modules match this search.", contentX + 2, top + 18, 0xFF8993AA);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, top, viewportBottom(), visible * 52 - 8);
    }

    private void drawActionButton(GuiGraphics graphics, String label, int x, int y, int width, int color, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 22;
        fillRounded(graphics, x, y, width, 22, 6, hovered ? lighten(color) : color);
        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + 7, 0xFFF2F3FB);
    }

    private static int lighten(int color) {
        int red = Math.min(255, ((color >> 16) & 0xFF) + 14);
        int green = Math.min(255, ((color >> 8) & 0xFF) + 14);
        int blue = Math.min(255, (color & 0xFF) + 14);
        return (color & 0xFF000000) | (red << 16) | (green << 8) | blue;
    }

    private void drawScrollbar(GuiGraphics graphics, int top, int bottom, int contentHeight) {
        int viewportHeight = bottom - top;
        if (contentHeight <= viewportHeight || contentHeight <= 0) {
            return;
        }
        int trackX = contentX + contentWidth - 2;
        int thumbHeight = Math.max(22, (int) ((long) viewportHeight * viewportHeight / contentHeight));
        int thumbTravel = viewportHeight - thumbHeight;
        int scrollRange = contentHeight - viewportHeight;
        int thumbY = top + (scrollRange <= 0 ? 0 : (int) Math.round(thumbTravel * scrollOffset / scrollRange));
        graphics.fill(trackX, top, trackX + 2, bottom, BORDER);
        fillRounded(graphics, trackX - 1, thumbY, 4, thumbHeight, 2, ACCENT);
    }

    private void refreshProfiles() {
        try {
            profiles = config.listProfiles();
            if (selectedProfile != null && !profiles.contains(selectedProfile)) {
                selectedProfile = null;
            }
        } catch (IOException exception) {
            profiles = List.of();
            profileStatus = "Could not read profiles: " + exception.getMessage();
            profileStatusError = true;
        }
    }

    private void saveProfile() {
        String name = profileName.getValue().trim();
        try {
            config.saveProfile(name, modules);
            selectedProfile = name;
            profileStatus = "Saved profile “" + name + "”.";
            profileStatusError = false;
            refreshProfiles();
        } catch (IllegalArgumentException | IOException exception) {
            profileStatus = exception instanceof IllegalArgumentException
                    ? "Use 1–32 letters, numbers, underscores, or hyphens."
                    : "Could not save profile: " + exception.getMessage();
            profileStatusError = true;
        }
    }

    private void loadProfile() {
        if (selectedProfile == null) {
            return;
        }
        try {
            if (config.loadProfile(selectedProfile, modules)) {
                config.save(modules);
                profileStatus = "Loaded profile “" + selectedProfile + "”.";
                profileStatusError = false;
            } else {
                profileStatus = "That profile no longer exists.";
                profileStatusError = true;
                refreshProfiles();
            }
        } catch (IOException exception) {
            profileStatus = "Could not load profile: " + exception.getMessage();
            profileStatusError = true;
        }
    }

    private void deleteProfile() {
        if (selectedProfile == null) {
            return;
        }
        try {
            if (config.deleteProfile(selectedProfile)) {
                profileStatus = "Deleted profile “" + selectedProfile + "”.";
                profileStatusError = false;
                selectedProfile = null;
                refreshProfiles();
            } else {
                profileStatus = "That profile no longer exists.";
                profileStatusError = true;
                refreshProfiles();
            }
        } catch (IOException exception) {
            profileStatus = "Could not delete profile: " + exception.getMessage();
            profileStatusError = true;
        }
    }

    private void renderSetting(GuiGraphics graphics, Setting setting, int x, int y, int width, int mouseX) {
        graphics.drawString(font, setting.label(), x, y + 3, 0xFFD1D0DA);
        if (setting instanceof BooleanSetting booleanSetting) {
            drawToggle(graphics, x + width - 36, y, booleanSetting.value());
        } else if (setting instanceof NumberSetting numberSetting) {
            int sliderX = x + width - 124;
            int sliderWidth = 76;
            double fraction = (numberSetting.value() - numberSetting.minimum()) / (numberSetting.maximum() - numberSetting.minimum());
            int filled = (int) Math.round(sliderWidth * fraction);
            graphics.fill(sliderX, y + 8, sliderX + sliderWidth, y + 11, 0xFF41424F);
            graphics.fill(sliderX, y + 8, sliderX + filled, y + 11, ACCENT);
            graphics.fill(sliderX + filled - 3, y + 5, sliderX + filled + 3, y + 14, 0xFFECE7FF);
            graphics.drawString(font, String.format(Locale.ROOT, "%.1f", numberSetting.value()), sliderX + sliderWidth + 10, y + 3, 0xFFB9B6C6);
        }
    }

    private static void drawToggle(GuiGraphics graphics, int x, int y, boolean enabled) {
        int color = enabled ? 0xFF7562D8 : 0xFF293247;
        fillRounded(graphics, x, y, 32, 16, 8, color);
        int knobX = enabled ? x + 19 : x + 3;
        fillRounded(graphics, knobX, y + 3, 10, 10, 5, 0xFFF4F1FB);
    }

    private static void fillRounded(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int inset = Math.min(radius, Math.min(width / 2, height / 2));
        for (int row = 0; row < inset; row++) {
            int cut = inset - row - 1;
            graphics.fill(x + cut, y + row, x + width - cut, y + row + 1, color);
        }
        if (height > inset * 2) {
            graphics.fill(x, y + inset, x + width, y + height - inset, color);
        }
        for (int row = 0; row < inset; row++) {
            int cut = row;
            graphics.fill(x + cut, y + height - inset + row, x + width - cut, y + height - inset + row + 1, color);
        }
    }

    private static boolean matches(ClientModule module, String query) {
        return query.isEmpty() || module.name().toLowerCase(Locale.ROOT).contains(query)
                || module.description().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean categoryMatches(ClientModule module) {
        return selectedCategory.equals("All") || selectedCategory.equals(module.category().displayName());
    }

    private int viewportTop() {
        return panelY + 94;
    }

    private int viewportBottom() {
        return panelY + panelHeight - 18;
    }

    private int cardBaseHeight() {
        return compactCards ? 54 : 66;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int dividerX = contentX - 20;
        String[] categories = {"All", "Combat", "Movement", "Visuals", "Client"};
        int rowY = panelY + 92;
        for (String category : categories) {
            if (mouseX >= panelX + 12 && mouseX <= dividerX - 12 && mouseY >= rowY - 6 && mouseY <= rowY + 18) {
                setPage(Page.MODULES);
                selectedCategory = category;
                return true;
            }
            rowY += 28;
        }

        if (mouseX >= panelX + 12 && mouseX <= dividerX - 12
                && mouseY >= panelY + 260 && mouseY <= panelY + 284) {
            setPage(Page.CONFIGS);
            refreshProfiles();
            return true;
        }
        if (mouseX >= panelX + 12 && mouseX <= dividerX - 12
                && mouseY >= panelY + 288 && mouseY <= panelY + 312) {
            setPage(Page.KEYBINDS);
            return true;
        }

        if (page == Page.CONFIGS) {
            int inputY = viewportTop() + 15;
            if (mouseY >= inputY && mouseY <= inputY + 22) {
                if (mouseX >= profileSaveX && mouseX <= profileSaveX + 64) {
                    saveProfile();
                    return true;
                }
                if (mouseX >= profileLoadX && mouseX <= profileLoadX + 64) {
                    loadProfile();
                    return true;
                }
                if (mouseX >= profileDeleteX && mouseX <= profileDeleteX + 72) {
                    deleteProfile();
                    return true;
                }
            }
            int profileRowY = viewportTop() + 63 - (int) Math.round(scrollOffset);
            for (String profile : profiles) {
                if (mouseX >= contentX && mouseX <= contentX + contentWidth - 4
                        && mouseY >= profileRowY && mouseY <= profileRowY + 34) {
                    selectedProfile = profile;
                    profileName.setValue(profile);
                    return true;
                }
                profileRowY += 42;
            }
            return false;
        }

        if (page == Page.KEYBINDS) {
            String query = search.getValue().trim().toLowerCase(Locale.ROOT);
            int bindingRowY = viewportTop() - (int) Math.round(scrollOffset);
            for (ClientModule module : modules.all()) {
                if (!matches(module, query)) {
                    continue;
                }
                if (mouseX >= contentX && mouseX <= contentX + contentWidth - 4
                        && mouseY >= bindingRowY && mouseY <= bindingRowY + 44) {
                    bindingModuleId = module.id();
                    return true;
                }
                bindingRowY += 52;
            }
            return false;
        }

        int toolbarY = panelY + 65;
        if (mouseY >= toolbarY && mouseY <= toolbarY + 22) {
            if (mouseX >= contentX && mouseX <= contentX + 64) {
                showEnabledOnly = false;
                targetScrollOffset = 0.0;
                scrollOffset = 0.0;
                return true;
            }
            if (mouseX >= contentX + 70 && mouseX <= contentX + 160) {
                showEnabledOnly = true;
                targetScrollOffset = 0.0;
                scrollOffset = 0.0;
                return true;
            }
            int densityX = contentX + contentWidth - 82;
            if (mouseX >= densityX && mouseX <= densityX + 78) {
                compactCards = !compactCards;
                targetScrollOffset = Math.min(targetScrollOffset, maximumScroll());
                return true;
            }
        }

        if (mouseY < viewportTop() || mouseY > viewportBottom()
                || mouseX < contentX || mouseX > contentX + contentWidth) {
            return false;
        }

        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        int cardY = viewportTop() - (int) Math.round(scrollOffset);
        int cardX = contentX;
        int cardWidth = contentWidth - 2;
        for (ClientModule module : modules.all()) {
            if (!matches(module, query) || !categoryMatches(module) || (showEnabledOnly && !module.enabled())) {
                continue;
            }
            boolean isExpanded = expanded.contains(module.id());
            int baseHeight = cardBaseHeight();
            int cardHeight = baseHeight + (isExpanded ? module.settings().size() * 34 + 10 : 0);
            if (mouseX >= cardX && mouseX <= cardX + cardWidth && mouseY >= cardY && mouseY <= cardY + baseHeight) {
                if (mouseX >= cardX + cardWidth - 54) {
                    module.toggle();
                    config.save(modules);
                } else if (mouseX >= cardX + cardWidth - 114) {
                    bindingModuleId = module.id();
                } else if (mouseX >= cardX + cardWidth - 150) {
                    if (isExpanded) {
                        expanded.remove(module.id());
                    } else {
                        expanded.add(module.id());
                    }
                }
                return true;
            }
            if (isExpanded && mouseX >= cardX && mouseX <= cardX + cardWidth && mouseY >= cardY + baseHeight) {
                int settingY = cardY + baseHeight + 8;
                for (Setting setting : module.settings()) {
                    if (mouseY >= settingY && mouseY <= settingY + 22) {
                        if (setting instanceof BooleanSetting booleanSetting) {
                            if (mouseX >= cardX + cardWidth - 14 - 36) {
                                booleanSetting.toggle();
                            }
                        } else if (setting instanceof NumberSetting numberSetting) {
                            int sliderX = cardX + 14 + cardWidth - 28 - 124;
                            if (mouseX >= sliderX && mouseX <= sliderX + 76) {
                                double fraction = Math.max(0.0, Math.min(1.0, (mouseX - sliderX) / 76.0));
                                numberSetting.setValue(numberSetting.minimum() + fraction * (numberSetting.maximum() - numberSetting.minimum()));
                            }
                        }
                        if ((setting instanceof BooleanSetting booleanSetting && mouseX >= cardX + cardWidth - 14 - 36)
                                || (setting instanceof NumberSetting numberSetting
                                && mouseX >= cardX + 14 + cardWidth - 28 - 124
                                && mouseX <= cardX + 14 + cardWidth - 28 - 48)) {
                            config.save(modules);
                            return true;
                        }
                    }
                    settingY += 34;
                }
            }
            cardY += cardHeight + 8;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (super.mouseDragged(event, dragX, dragY)) {
            return true;
        }
        if (page != Page.MODULES || event.button() != 0) {
            return false;
        }
        if (event.x() < contentX || event.x() > contentX + contentWidth
                || event.y() < viewportTop() || event.y() > viewportBottom()) {
            return false;
        }
        for (ClientModule module : modules.all()) {
            if (!expanded.contains(module.id())) {
                continue;
            }
            for (Setting setting : module.settings()) {
                if (setting instanceof NumberSetting numberSetting) {
                    int sliderX = contentX + 14 + contentWidth - 2 - 28 - 124;
                    int sliderY = findSettingY(module, setting);
                    if (sliderY >= 0 && mouseY >= sliderY && mouseY <= sliderY + 22 && mouseX >= sliderX && mouseX <= sliderX + 76) {
                        double fraction = Math.max(0.0, Math.min(1.0, (mouseX - sliderX) / 76.0));
                        numberSetting.setValue(numberSetting.minimum() + fraction * (numberSetting.maximum() - numberSetting.minimum()));
                        config.save(modules);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= viewportTop() && mouseY <= viewportBottom()) {
            targetScrollOffset = Math.max(0.0, Math.min(maximumScroll(), targetScrollOffset - verticalAmount * 34.0));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (bindingModuleId == null) {
            return super.keyPressed(event);
        }

        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            bindingModuleId = null;
            return true;
        }

        ClientModule module = modules.get(bindingModuleId);
        if (module != null) {
            int key = event.key() == GLFW.GLFW_KEY_BACKSPACE || event.key() == GLFW.GLFW_KEY_DELETE
                    ? -1 : event.key();
            module.setKeyCode(key);
            config.save(modules);
        }
        bindingModuleId = null;
        return true;
    }

    private static String keyName(int keyCode) {
        if (keyCode < 0) {
            return "UNBOUND";
        }
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString().toUpperCase(Locale.ROOT);
    }

    private int findSettingY(ClientModule targetModule, Setting targetSetting) {
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        int cardY = viewportTop() - (int) Math.round(scrollOffset);
        for (ClientModule module : modules.all()) {
            if (!matches(module, query) || !categoryMatches(module) || (showEnabledOnly && !module.enabled())) {
                continue;
            }
            if (module == targetModule) {
                int index = module.settings().indexOf(targetSetting);
                return index < 0 ? -1 : cardY + cardBaseHeight() + 8 + index * 34;
            }
            cardY += cardBaseHeight() + (expanded.contains(module.id()) ? module.settings().size() * 34 + 10 : 0) + 8;
        }
        return -1;
    }

    private int maximumScroll() {
        int viewportHeight = viewportBottom() - viewportTop();
        int contentHeight = switch (page) {
            case MODULES -> totalContentHeight();
            case CONFIGS -> profiles.size() * 42 - 8;
            case KEYBINDS -> keybindContentHeight();
        };
        if (page == Page.CONFIGS) {
            viewportHeight -= 63;
        }
        return maxScroll(contentHeight, viewportHeight);
    }

    private int keybindContentHeight() {
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        return (int) modules.all().stream().filter(module -> matches(module, query)).count() * 52 - 8;
    }

    private int totalContentHeight() {
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        int total = 0;
        for (ClientModule module : modules.all()) {
            if (matches(module, query) && categoryMatches(module) && (!showEnabledOnly || module.enabled())) {
                total += cardBaseHeight() + (expanded.contains(module.id()) ? module.settings().size() * 34 + 10 : 0) + 8;
            }
        }
        return total;
    }

    private static int maxScroll(int contentHeight, int viewportHeight) {
        return Math.max(0, contentHeight - viewportHeight);
    }

    @Override
    public void onClose() {
        config.save(modules);
        super.onClose();
    }
}
