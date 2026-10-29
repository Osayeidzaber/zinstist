package dev.jilaton.client.config;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.Setting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public final class ClientConfig {
    private static final String PROFILE_EXTENSION = ".properties";
    private static final String PROFILE_NAME_PATTERN = "[A-Za-z0-9][A-Za-z0-9_-]{0,31}";
    private final Path path;
    private final Path profilesDirectory;

    public ClientConfig(Path configDirectory) {
        this.path = configDirectory.resolve("jilaton.properties");
        this.profilesDirectory = configDirectory.resolve("jilaton-profiles");
    }

    public void load(ModuleManager modules) {
        Properties values = read(path);
        if (values != null) {
            apply(values, modules);
        }
    }

    public void save(ModuleManager modules) {
        try {
            write(path, snapshot(modules));
        } catch (IOException exception) {
            System.err.println("Jilaton could not save its configuration: " + exception.getMessage());
        }
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
                module.setEnabled(Boolean.parseBoolean(value));
            } else {
                System.err.println("Jilaton ignored invalid module state " + key);
            }
        }
    }

    private static Properties snapshot(ModuleManager modules) {
        Properties values = new Properties();
        for (ClientModule module : modules.all()) {
            values.setProperty("module." + module.id() + ".enabled", Boolean.toString(module.enabled()));
            values.setProperty("module." + module.id() + ".keycode", Integer.toString(module.keyCode()));
            for (Setting setting : module.settings()) {
                if (setting instanceof BooleanSetting booleanSetting) {
                    values.setProperty(settingKey(module, setting), Boolean.toString(booleanSetting.value()));
                } else if (setting instanceof NumberSetting numberSetting) {
                    values.setProperty(settingKey(module, setting), Double.toString(numberSetting.value()));
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
        } catch (IOException exception) {
            System.err.println("Jilaton could not read its configuration: " + exception.getMessage());
            return null;
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

    private static String settingKey(ClientModule module, Setting setting) {
        return "module." + module.id() + ".setting." + setting.id();
    }
}
