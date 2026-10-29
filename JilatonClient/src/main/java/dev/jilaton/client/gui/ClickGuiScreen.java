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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ClickGuiScreen extends Screen {
    private static final int ACCENT = 0xFF9B7BFF;
    private static final int PANEL = 0xF20F1016;
    private static final int CARD = 0xFF191A23;
    private static final int CARD_HOVER = 0xFF20212C;
    private final ModuleManager modules;
    private final ClientConfig config;
    private final Set<String> expanded = new HashSet<>();
    private EditBox search;
    private String selectedCategory = "All";
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentWidth;

    public ClickGuiScreen(ModuleManager modules, ClientConfig config) {
        super(Component.literal("Jilaton Client"));
        this.modules = modules;
        this.config = config;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(840, width - 24);
        panelHeight = Math.min(500, height - 24);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        int sidebarWidth = Math.min(162, Math.max(128, panelWidth / 4));
        contentX = panelX + sidebarWidth + 20;
        contentWidth = panelWidth - sidebarWidth - 40;
        search = new EditBox(font, contentX, panelY + 39, Math.max(110, contentWidth - 8), 20, Component.literal("Search modules"));
        search.setHint(Component.literal("Search modules"));
        addRenderableWidget(search);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x9906080D);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, ACCENT);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xFF101117);
        renderHeader(graphics);
        renderSidebar(graphics, mouseX, mouseY);
        renderModules(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics graphics) {
        graphics.drawString(font, "JILATON", panelX + 22, panelY + 20, 0xFFF5F2FF);
        graphics.drawString(font, "CLIENT", panelX + 88, panelY + 20, ACCENT);
        graphics.drawString(font, "PVP  /  CUSTOMIZE", panelX + 22, panelY + 36, 0xFF777986);
    }

    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        int sidebarWidth = contentX - panelX - 20;
        int dividerX = panelX + sidebarWidth;
        graphics.fill(dividerX, panelY + 70, dividerX + 1, panelY + panelHeight - 20, 0xFF292A34);
        graphics.drawString(font, "LIBRARY", panelX + 22, panelY + 76, 0xFF777986);

        String[] categories = {"All", "Combat", "Movement", "Visuals"};
        int rowY = panelY + 95;
        for (String category : categories) {
            int rowX = panelX + 14;
            boolean selected = selectedCategory.equals(category);
            boolean hovered = mouseX >= rowX && mouseX <= dividerX - 10 && mouseY >= rowY - 5 && mouseY <= rowY + 15;
            if (selected) {
                graphics.fill(rowX, rowY - 6, dividerX - 10, rowY + 16, 0xFF292435);
                graphics.fill(rowX, rowY - 6, rowX + 2, rowY + 16, ACCENT);
            } else if (hovered) {
                graphics.fill(rowX, rowY - 6, dividerX - 10, rowY + 16, 0xFF191A22);
            }
            graphics.drawString(font, category, rowX + 12, rowY, selected ? 0xFFF4F0FF : 0xFFAAAAB5);
            rowY += 30;
        }

        graphics.drawString(font, "SHIFT", panelX + 22, panelY + panelHeight - 30, 0xFF777986);
        graphics.drawString(font, "OPEN MENU", panelX + 62, panelY + panelHeight - 30, 0xFF777986);
    }

    private void renderModules(GuiGraphics graphics, int mouseX, int mouseY) {
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        int cardY = panelY + 75;
        int cardX = contentX;
        int cardWidth = contentWidth - 8;
        boolean any = false;

        for (ClientModule module : modules.all()) {
            if (!matches(module, query)) {
                continue;
            }
            if (!selectedCategory.equals("All") && !selectedCategory.equals(module.category().displayName())) {
                continue;
            }
            any = true;
            boolean isExpanded = expanded.contains(module.id());
            int baseHeight = 76;
            int settingsHeight = isExpanded ? module.settings().size() * 34 + 10 : 0;
            int cardHeight = baseHeight + settingsHeight;
            boolean hovered = mouseX >= cardX && mouseX <= cardX + cardWidth && mouseY >= cardY && mouseY <= cardY + cardHeight;
            graphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, hovered ? CARD_HOVER : CARD);
            graphics.fill(cardX, cardY, cardX + 2, cardY + baseHeight, module.enabled() ? ACCENT : 0xFF383945);
            graphics.drawString(font, module.name(), cardX + 14, cardY + 13, 0xFFF3F1FA);
            graphics.drawString(font, module.description(), cardX + 14, cardY + 32, 0xFF9A9BA8);
            graphics.drawString(font, module.category().displayName().toUpperCase(Locale.ROOT), cardX + 14, cardY + 52, 0xFF777986);
            drawToggle(graphics, cardX + cardWidth - 48, cardY + 14, module.enabled());
            graphics.drawString(font, isExpanded ? "−" : "+", cardX + cardWidth - 78, cardY + 15, 0xFFA8A9B5);

            if (isExpanded) {
                int settingY = cardY + baseHeight + 8;
                for (Setting setting : module.settings()) {
                    renderSetting(graphics, setting, cardX + 14, settingY, cardWidth - 28, mouseX);
                    settingY += 34;
                }
            }
            cardY += cardHeight + 10;
        }

        if (!any) {
            graphics.drawString(font, "No modules match this view.", contentX + 8, panelY + 112, 0xFF9A9BA8);
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
        int color = enabled ? 0xFF7861C4 : 0xFF3D3E49;
        graphics.fill(x, y, x + 32, y + 16, color);
        int knobX = enabled ? x + 19 : x + 3;
        graphics.fill(knobX, y + 3, knobX + 10, y + 13, 0xFFF4F1FB);
    }

    private static boolean matches(ClientModule module, String query) {
        return query.isEmpty() || module.name().toLowerCase(Locale.ROOT).contains(query)
                || module.description().toLowerCase(Locale.ROOT).contains(query);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int dividerX = contentX - 20;
        String[] categories = {"All", "Combat", "Movement", "Visuals"};
        int rowY = panelY + 95;
        for (String category : categories) {
            if (mouseX >= panelX + 14 && mouseX <= dividerX - 10 && mouseY >= rowY - 6 && mouseY <= rowY + 16) {
                selectedCategory = category;
                return true;
            }
            rowY += 30;
        }

        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        int cardY = panelY + 75;
        int cardX = contentX;
        int cardWidth = contentWidth - 8;
        for (ClientModule module : modules.all()) {
            if (!matches(module, query) || (!selectedCategory.equals("All") && !selectedCategory.equals(module.category().displayName()))) {
                continue;
            }
            boolean isExpanded = expanded.contains(module.id());
            int baseHeight = 76;
            int cardHeight = baseHeight + (isExpanded ? module.settings().size() * 34 + 10 : 0);
            if (mouseX >= cardX && mouseX <= cardX + cardWidth && mouseY >= cardY && mouseY <= cardY + baseHeight) {
                if (mouseX >= cardX + cardWidth - 54) {
                    module.toggle();
                    config.save(modules);
                } else if (mouseX >= cardX + cardWidth - 92) {
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
                            booleanSetting.toggle();
                        } else if (setting instanceof NumberSetting numberSetting) {
                            int sliderX = cardX + cardWidth - 14 - 124;
                            int sliderWidth = 76;
                            double fraction = Math.max(0.0, Math.min(1.0, (mouseX - sliderX) / sliderWidth));
                            numberSetting.setValue(numberSetting.minimum() + fraction * (numberSetting.maximum() - numberSetting.minimum()));
                        }
                        config.save(modules);
                        return true;
                    }
                    settingY += 34;
                }
            }
            cardY += cardHeight + 10;
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
        if (event.button() != 0) {
            return false;
        }
        for (ClientModule module : modules.all()) {
            if (!expanded.contains(module.id())) {
                continue;
            }
            for (Setting setting : module.settings()) {
                if (setting instanceof NumberSetting numberSetting) {
                    int sliderX = contentX + contentWidth - 8 - 14 - 124;
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

    private int findSettingY(ClientModule targetModule, Setting targetSetting) {
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        int cardY = panelY + 75;
        for (ClientModule module : modules.all()) {
            if (!matches(module, query) || (!selectedCategory.equals("All") && !selectedCategory.equals(module.category().displayName()))) {
                continue;
            }
            if (module == targetModule) {
                int index = module.settings().indexOf(targetSetting);
                return index < 0 ? -1 : cardY + 76 + 8 + index * 34;
            }
            cardY += 76 + (expanded.contains(module.id()) ? module.settings().size() * 34 + 10 : 0) + 10;
        }
        return -1;
    }

    @Override
    public void onClose() {
        config.save(modules);
        super.onClose();
    }
}
