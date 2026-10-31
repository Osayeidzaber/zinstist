package dev.jilaton.client.config;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.module.impl.FriendManager;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.Setting;
import dev.jilaton.client.setting.TextSetting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;

public final class ClientConfig {
    private static final String PROFILE_EXTENSION = ".properties";
    private static final String PROFILE_NAME_PATTERN = "[A-Za-z0-9][A-Za-z0-9_-]{0,31}";
    private static final Set<String> THEME_IDS = Set.of("obsidian", "graphite", "blue", "cyan", "rose", "amber", "custom");
    private static final Set<String> GUI_PAGES = Set.of("MODULES", "SETTINGS", "CONFIGS", "THEME");
    private static final Set<String> GUI_CATEGORIES = Set.of("All", "Combat", "Movement", "Player", "Visuals", "Client");
    private static final Set<String> HUD_WIDGET_IDS = Set.of("watermark", "modules", "performance", "coordinates", "keystrokes", "target", "armor", "effects", "cps", "notifications", "music", "keybinds", "server", "above_head", "held_item", "staff", "radar");
    private final Path path;
    private final Path profilesDirectory;
    private final Map<String, HudPosition> hudPositions = new HashMap<>();
    private final Map<String, HudStyle> hudStyles = new HashMap<>();
    /** Violet is the redesign's one accent (GUI_REDESIGN.md §0); a saved theme still wins. */
    private String themeId = "obsidian";
    private String customAccent = "F2F2F2";
    private boolean reducedMotion;
    private double animationIntensity = 1.0;
    private double backdropOpacity = 0.55;
    private boolean backdropBlur = true;
    /** UI typeface: "inter" (shipped TTF) or "minecraft" (vanilla pixel font). */
    private String uiFont = "inter";

    public String uiFont() { return uiFont; }
    public void setUiFont(String font) { uiFont = "minecraft".equals(font) ? "minecraft" : "inter"; }
    /** ClickGUI Visuals card order, as module ids; empty means registration order. */
    private final List<String> visualOrder = new ArrayList<>();
    /** ClickGUI general module order across categories; empty means registration order. */
    private final List<String> moduleOrder = new ArrayList<>();
    private String guiPage = "MODULES";
    private String guiCategory = "All";
    private String guiSelectedModule = "";
    private String guiExpandedModule = "";
    private boolean guiEnabledOnly;
    private String guiSearchQuery = "";
    private String guiSelectedProfile = "";
    private double guiScroll;
    private final Map<String, Double> categoryScrolls = new HashMap<>();

    public List<String> visualOrder() { return List.copyOf(visualOrder); }
    public void setVisualOrder(List<String> order) {
        visualOrder.clear();
        if (order == null) return;
        for (String id : order) if (id != null && id.matches("[a-z0-9][a-z0-9_-]*") && !visualOrder.contains(id)) visualOrder.add(id);
    }

    public List<String> moduleOrder() {
        return moduleOrder.isEmpty() ? List.copyOf(visualOrder) : List.copyOf(moduleOrder);
    }

    public void setModuleOrder(List<String> order) {
        moduleOrder.clear();
        if (order == null) return;
        for (String id : order) if (id != null && id.matches("[a-z0-9][a-z0-9_-]*") && !moduleOrder.contains(id)) moduleOrder.add(id);
    }

    public String guiPage() { return guiPage; }
    public void setGuiPage(String page) {
        if (page != null && GUI_PAGES.contains(page)) {
            this.guiPage = page;
        }
    }

    public String guiCategory() { return guiCategory; }
    public void setGuiCategory(String category) {
        if (category != null && GUI_CATEGORIES.contains(category)) {
            this.guiCategory = category;
        }
    }

    public String guiSelectedModule() { return guiSelectedModule; }
    public void setGuiSelectedModule(String moduleId) {
        this.guiSelectedModule = moduleId != null && moduleId.matches("[a-z0-9][a-z0-9_-]*") ? moduleId : "";
    }

    public String guiExpandedModule() { return guiExpandedModule; }
    public void setGuiExpandedModule(String moduleId) {
        this.guiExpandedModule = moduleId != null && moduleId.matches("[a-z0-9][a-z0-9_-]*") ? moduleId : "";
    }

    public boolean guiEnabledOnly() { return guiEnabledOnly; }
    public void setGuiEnabledOnly(boolean enabledOnly) { this.guiEnabledOnly = enabledOnly; }

    public String guiSearchQuery() { return guiSearchQuery; }
    public void setGuiSearchQuery(String query) {
        if (query == null) {
            this.guiSearchQuery = "";
        } else {
            String trimmed = query.strip();
            this.guiSearchQuery = trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
        }
    }

    public String guiSelectedProfile() { return guiSelectedProfile; }
    public void setGuiSelectedProfile(String profile) {
        this.guiSelectedProfile = isValidProfileName(profile) ? profile : "";
    }

    public double guiScroll() { return guiScroll; }
    public void setGuiScroll(double scroll) {
        this.guiScroll = Double.isFinite(scroll) ? Math.max(0.0, scroll) : 0.0;
    }

    public double categoryScroll(String category) {
        if (category == null) return 0.0;
        return categoryScrolls.getOrDefault(category, 0.0);
    }

    public void setCategoryScroll(String category, double scroll) {
        if (category != null && GUI_CATEGORIES.contains(category)) {
            double clamped = Double.isFinite(scroll) ? Math.max(0.0, scroll) : 0.0;
            categoryScrolls.put(category, clamped);
        }
    }

    public boolean reducedMotion() { return reducedMotion; }
    public void setReducedMotion(boolean reducedMotion) { this.reducedMotion = reducedMotion; }
    /** Spatial animation scale: 0 keeps fades only, 1 is the default, 1.5 is the most expressive. */
    public double animationIntensity() { return animationIntensity; }
    public void setAnimationIntensity(double intensity) {
        if (!Double.isFinite(intensity)) throw new IllegalArgumentException("Animation intensity must be finite");
        animationIntensity = Math.max(0, Math.min(1.5, Math.round(intensity * 20) / 20.0));
    }
    /** In-world ClickGUI scrim darkness, 0.2 (see-through) to 0.95 (near-black). */
    public double backdropOpacity() { return backdropOpacity; }
    public void setBackdropOpacity(double opacity) {
        if (!Double.isFinite(opacity)) throw new IllegalArgumentException("Backdrop opacity must be finite");
        backdropOpacity = Math.max(0.2, Math.min(0.95, Math.round(opacity * 20) / 20.0));
    }
    /** Whether the in-world ClickGUI blurs the world behind it (vanilla blur, the player's own radius). */
    public boolean backdropBlur() { return backdropBlur; }
    public void setBackdropBlur(boolean backdropBlur) { this.backdropBlur = backdropBlur; }

    public ClientConfig(Path configDirectory) {
        this.path = configDirectory.resolve("jilaton.properties");
        this.profilesDirectory = configDirectory.resolve("jilaton-profiles");
    }

    public void load(ModuleManager modules) {
        Properties values = read(path);
        apply(values == null ? new Properties() : values, modules);
    }

    public void save(ModuleManager modules) {
        try {
            write(path, snapshot(modules));
        } catch (IOException exception) {
            System.err.println("Jilaton could not save its configuration: " + exception.getMessage());
        }
    }

    public String themeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        if (themeId == null || !THEME_IDS.contains(themeId)) {
            throw new IllegalArgumentException("Unknown Jilaton theme: " + themeId);
        }
        this.themeId = themeId;
    }

    public String customAccent() {
        return customAccent;
    }

    public HudPosition hudPosition(String widgetId) {
        requireHudWidgetId(widgetId);
        return hudPositions.get(widgetId);
    }

    public void setHudPosition(String widgetId, double xFraction, double yFraction) {
        requireHudWidgetId(widgetId);
        hudPositions.put(widgetId, new HudPosition(clampFraction(xFraction), clampFraction(yFraction)));
    }

    public void clearHudPosition(String widgetId) {
        requireHudWidgetId(widgetId);
        hudPositions.remove(widgetId);
    }

    public HudStyle hudStyle(String widgetId) {
        requireHudWidgetId(widgetId);
        return hudStyles.getOrDefault(widgetId, HudStyle.DEFAULT);
    }

    public void setHudStyle(String widgetId, HudStyle style) {
        requireHudWidgetId(widgetId);
        hudStyles.put(widgetId, java.util.Objects.requireNonNull(style));
    }

    public void resetHudWidget(String widgetId) {
        clearHudPosition(widgetId);
        hudStyles.remove(widgetId);
    }

    public void setCustomAccent(String customAccent) {
        if (customAccent == null || !customAccent.matches("[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException("Accent color must be six hexadecimal digits");
        }
        this.customAccent = customAccent.toUpperCase(java.util.Locale.ROOT);
    }

    public List<String> listProfiles() throws IOException {
        if (!Files.isDirectory(profilesDirectory)) {
            return List.of();
        }

        try (var files = Files.list(profilesDirectory)) {
            List<String> profiles = new ArrayList<>();
            files.filter(Files::isRegularFile)
                    .map(file -> file.getFileName().toString())
                    .filter(fileName -> fileName.endsWith(PROFILE_EXTENSION))
                    .map(fileName -> fileName.substring(0, fileName.length() - PROFILE_EXTENSION.length()))
                    .filter(ClientConfig::isValidProfileName)
                    .forEach(profiles::add);
            profiles.sort(Comparator.naturalOrder());
            return List.copyOf(profiles);
        }
    }

    public void saveProfile(String name, ModuleManager modules) throws IOException {
        write(profilePath(name), snapshot(modules));
    }

    public boolean loadProfile(String name, ModuleManager modules) throws IOException {
        Properties values = readProfilePath(name);
        if (values == null) {
            return false;
        }
        apply(values, modules);
        return true;
    }

    public boolean deleteProfile(String name) throws IOException {
        return Files.deleteIfExists(profilePath(name));
    }

    private void apply(Properties values, ModuleManager modules) {
        hudPositions.clear();
        hudStyles.clear();
        categoryScrolls.clear();
        reducedMotion = Boolean.parseBoolean(values.getProperty("ui.reduced_motion", "false"));
        setUiFont(values.getProperty("ui.font", "inter"));
        String configuredOrder = values.getProperty("ui.visual_order", "");
        setVisualOrder(configuredOrder.isBlank() ? List.of() : List.of(configuredOrder.split(",")));
        String configuredModuleOrder = values.getProperty("ui.module_order", "");
        setModuleOrder(configuredModuleOrder.isBlank() ? List.of() : List.of(configuredModuleOrder.split(",")));
        setGuiPage(values.getProperty("ui.last_page", "MODULES"));
        setGuiCategory(values.getProperty("ui.last_category", "All"));
        setGuiSelectedModule(values.getProperty("ui.selected_module", ""));
        setGuiExpandedModule(values.getProperty("ui.expanded_module", ""));
        setGuiEnabledOnly(Boolean.parseBoolean(values.getProperty("ui.enabled_only", "false")));
        setGuiSearchQuery(values.getProperty("ui.search_query", ""));
        setGuiSelectedProfile(values.getProperty("ui.selected_profile", ""));
        try {
            setGuiScroll(Double.parseDouble(values.getProperty("ui.scroll", "0.0")));
        } catch (NumberFormatException exception) {
            guiScroll = 0.0;
        }
        for (String category : GUI_CATEGORIES) {
            String key = "ui.scroll." + category.toLowerCase(java.util.Locale.ROOT);
            String rawScroll = values.getProperty(key);
            if (rawScroll != null) {
                try {
                    setCategoryScroll(category, Double.parseDouble(rawScroll));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        try {
            setAnimationIntensity(Double.parseDouble(values.getProperty("ui.animation_intensity", "1.0")));
        } catch (IllegalArgumentException exception) {
            animationIntensity = 1.0;
        }
        try {
            setBackdropOpacity(Double.parseDouble(values.getProperty("ui.backdrop_opacity", "0.55")));
        } catch (IllegalArgumentException exception) {
            backdropOpacity = 0.55;
        }
        backdropBlur = Boolean.parseBoolean(values.getProperty("ui.backdrop_blur", "true"));
        String configuredFriends = values.getProperty("friends.names");
        if (configuredFriends != null) {
            FriendManager.replace(List.of(configuredFriends.split(",")));
        }
        String configuredTheme = values.getProperty("ui.theme");
        // Violet was the accent before the black-and-white redesign; it is gone, so old configs come back white.
        if ("violet".equals(configuredTheme)) configuredTheme = "obsidian";
        if (configuredTheme != null) {
            try {
                setThemeId(configuredTheme);
            } catch (IllegalArgumentException exception) {
                System.err.println("Jilaton ignored invalid UI theme");
            }
        }
        String configuredAccent = values.getProperty("ui.custom_accent");
        if (configuredAccent != null) {
            try {
                setCustomAccent(configuredAccent);
            } catch (IllegalArgumentException exception) {
                System.err.println("Jilaton ignored invalid custom accent color");
            }
        }

        for (String widgetId : HUD_WIDGET_IDS) {
            String stylePrefix = "hud.style." + widgetId + ".";
            try {
                HudStyle defaults = HudStyle.DEFAULT;
                String accent = values.getProperty(stylePrefix + "accent", "theme");
                if (!accent.equals("theme") && !accent.matches("[0-9a-fA-F]{6}")) {
                    throw new IllegalArgumentException("Invalid HUD accent");
                }
                String visible = values.getProperty(stylePrefix + "visible", "true");
                if (!visible.equals("true") && !visible.equals("false")) {
                    throw new IllegalArgumentException("Invalid HUD visibility");
                }
                setHudStyle(widgetId, new HudStyle(
                        Double.parseDouble(values.getProperty(stylePrefix + "scale", Double.toString(defaults.scale()))),
                        Double.parseDouble(values.getProperty(stylePrefix + "panel_opacity", Double.toString(defaults.panelOpacity()))),
                        accent.equals("theme") ? null : Integer.parseInt(accent, 16),
                        HudStyle.Alignment.valueOf(values.getProperty(stylePrefix + "alignment", "FREE")),
                        Boolean.parseBoolean(visible)));
            } catch (IllegalArgumentException exception) {
                System.err.println("Jilaton ignored invalid HUD appearance for " + widgetId);
            }
            String prefix = "hud.position." + widgetId + ".";
            String x = values.getProperty(prefix + "x");
            String y = values.getProperty(prefix + "y");
            if (x == null && y == null) {
                continue;
            }
            try {
                if (x == null || y == null) {
                    throw new IllegalArgumentException("Incomplete HUD position");
                }
                setHudPosition(widgetId, Double.parseDouble(x), Double.parseDouble(y));
            } catch (IllegalArgumentException exception) {
                System.err.println("Jilaton ignored invalid HUD position for " + widgetId);
            }
        }

        for (ClientModule module : modules.all()) {
            for (Setting setting : module.settings()) {
                String key = settingKey(module, setting);
                String value = values.getProperty(key);
                if (value == null) {
                    continue;
                }
                try {
                    if (setting instanceof BooleanSetting booleanSetting) {
                        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                            booleanSetting.setValue(Boolean.parseBoolean(value));
                        } else {
                            System.err.println("Jilaton ignored invalid setting " + key);
                        }
                    } else if (setting instanceof NumberSetting numberSetting) {
                        numberSetting.setValue(Double.parseDouble(value));
                    } else if (setting instanceof ChoiceSetting choiceSetting) {
                        choiceSetting.setValue(value);
                    } else if (setting instanceof TextSetting textSetting) {
                        textSetting.setValue(value);
                    }
                } catch (IllegalArgumentException exception) {
                    System.err.println("Jilaton ignored invalid setting " + key);
                }
            }

            String keyCode = values.getProperty("module." + module.id() + ".keycode");
            if (keyCode != null) {
                try {
                    module.setKeyCode(Integer.parseInt(keyCode));
                } catch (IllegalArgumentException exception) {
                    System.err.println("Jilaton ignored invalid key binding for " + module.id());
                }
            }
        }

        for (ClientModule module : modules.all()) {
            String key = "module." + module.id() + ".enabled";
            String value = values.getProperty(key, Boolean.toString(module.enabledByDefault()));
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                module.setEnabled(module.persistEnabled() && Boolean.parseBoolean(value));
            } else {
                System.err.println("Jilaton ignored invalid module state " + key);
            }
        }
    }

    private Properties snapshot(ModuleManager modules) {
        Properties values = new Properties();
        values.setProperty("ui.theme", themeId);
        values.setProperty("ui.custom_accent", customAccent);
        values.setProperty("ui.reduced_motion", Boolean.toString(reducedMotion));
        values.setProperty("ui.animation_intensity", Double.toString(animationIntensity));
        values.setProperty("ui.backdrop_opacity", Double.toString(backdropOpacity));
        values.setProperty("ui.backdrop_blur", Boolean.toString(backdropBlur));
        values.setProperty("ui.font", uiFont);
        values.setProperty("ui.visual_order", String.join(",", visualOrder));
        values.setProperty("ui.module_order", String.join(",", moduleOrder));
        values.setProperty("ui.last_page", guiPage);
        values.setProperty("ui.last_category", guiCategory);
        values.setProperty("ui.selected_module", guiSelectedModule);
        values.setProperty("ui.expanded_module", guiExpandedModule);
        values.setProperty("ui.enabled_only", Boolean.toString(guiEnabledOnly));
        values.setProperty("ui.search_query", guiSearchQuery);
        values.setProperty("ui.selected_profile", guiSelectedProfile);
        values.setProperty("ui.scroll", Double.toString(guiScroll));
        for (String category : GUI_CATEGORIES) {
            double catScroll = categoryScroll(category);
            if (catScroll > 0.0) {
                values.setProperty("ui.scroll." + category.toLowerCase(java.util.Locale.ROOT), Double.toString(catScroll));
            }
        }
        values.setProperty("friends.names", String.join(",", FriendManager.names()));
        for (String widgetId : HUD_WIDGET_IDS) {
            HudStyle style = hudStyle(widgetId);
            String stylePrefix = "hud.style." + widgetId + ".";
            values.setProperty(stylePrefix + "scale", Double.toString(style.scale()));
            values.setProperty(stylePrefix + "panel_opacity", Double.toString(style.panelOpacity()));
            values.setProperty(stylePrefix + "accent", style.accent() == null ? "theme"
                    : String.format(java.util.Locale.ROOT, "%06X", style.accent()));
            values.setProperty(stylePrefix + "alignment", style.alignment().name());
            values.setProperty(stylePrefix + "visible", Boolean.toString(style.visible()));
            HudPosition position = hudPositions.get(widgetId);
            if (position != null) {
                String prefix = "hud.position." + widgetId + ".";
                values.setProperty(prefix + "x", Double.toString(position.x()));
                values.setProperty(prefix + "y", Double.toString(position.y()));
            }
        }
        for (ClientModule module : modules.all()) {
            values.setProperty("module." + module.id() + ".enabled", Boolean.toString(module.persistEnabled() && module.enabled()));
            values.setProperty("module." + module.id() + ".keycode", Integer.toString(module.keyCode()));
            for (Setting setting : module.settings()) {
                if (setting instanceof BooleanSetting booleanSetting) {
                    values.setProperty(settingKey(module, setting), Boolean.toString(booleanSetting.value()));
                } else if (setting instanceof NumberSetting numberSetting) {
                    values.setProperty(settingKey(module, setting), Double.toString(numberSetting.value()));
                } else if (setting instanceof ChoiceSetting choiceSetting) {
                    values.setProperty(settingKey(module, setting), choiceSetting.persistedValue());
                } else if (setting instanceof TextSetting textSetting) {
                    values.setProperty(settingKey(module, setting), textSetting.value());
                }
            }
        }

        return values;
    }

    private Properties read(Path source) {
        if (!Files.isRegularFile(source)) {
            return new Properties();
        }
        try (InputStream input = Files.newInputStream(source)) {
            Properties values = new Properties();
            values.load(input);
            return values;
        } catch (IOException | IllegalArgumentException exception) {
            // Properties.load throws IllegalArgumentException on a malformed unicode escape. Either way the
            // defaults take over and the next save rewrites this file, so keep the original bytes first.
            System.err.println("Jilaton could not read its configuration: " + exception.getMessage());
            backUpUnreadable(source);
            return null;
        }
    }

    private static void backUpUnreadable(Path source) {
        Path backup = source.resolveSibling(source.getFileName() + ".unreadable-" + System.currentTimeMillis());
        try {
            Files.copy(source, backup, StandardCopyOption.COPY_ATTRIBUTES);
            System.err.println("Jilaton kept the unreadable configuration as " + backup.getFileName());
        } catch (IOException | RuntimeException backupFailure) {
            System.err.println("Jilaton could not back up its unreadable configuration: " + backupFailure.getMessage());
        }
    }

    private Properties readProfilePath(String name) throws IOException {
        Path profile = profilePath(name);
        if (!Files.isRegularFile(profile)) {
            return null;
        }
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(profile)) {
            values.load(input);
        }
        return values;
    }

    private void write(Path destination, Properties values) throws IOException {
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        try {
            Files.createDirectories(destination.getParent());
            try (OutputStream output = Files.newOutputStream(temporary)) {
                values.store(output, "Jilaton Client settings");
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    private Path profilePath(String name) {
        if (!isValidProfileName(name)) {
            throw new IllegalArgumentException("Profile names must contain 1-32 letters, digits, underscores, or hyphens");
        }
        return profilesDirectory.resolve(name + PROFILE_EXTENSION);
    }

    private static boolean isValidProfileName(String name) {
        return name != null && name.matches(PROFILE_NAME_PATTERN);
    }

    private static double clampFraction(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("HUD position must be finite");
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static void requireHudWidgetId(String widgetId) {
        if (!HUD_WIDGET_IDS.contains(widgetId)) {
            throw new IllegalArgumentException("Unknown HUD widget: " + widgetId);
        }
    }

    public record HudPosition(double x, double y) {
    }

    private static String settingKey(ClientModule module, Setting setting) {
        return "module." + module.id() + ".setting." + setting.id();
    }
}
