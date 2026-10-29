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
import java.util.Properties;

public final class ClientConfig {
    private final Path path;

    public ClientConfig(Path configDirectory) {
        this.path = configDirectory.resolve("jilaton.properties");
    }

    public void load(ModuleManager modules) {
        Properties values = new Properties();
        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                values.load(input);
            } catch (IOException exception) {
                System.err.println("Jilaton could not read its configuration: " + exception.getMessage());
                return;
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
                        booleanSetting.setValue(Boolean.parseBoolean(value));
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
            module.setEnabled(Boolean.parseBoolean(values.getProperty(
                    "module." + module.id() + ".enabled", Boolean.toString(module.enabledByDefault()))));
        }
    }

    public void save(ModuleManager modules) {
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

        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream output = Files.newOutputStream(temporary)) {
                values.store(output, "Jilaton Client settings");
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("Jilaton could not save its configuration: " + exception.getMessage());
        }
    }

    private static String settingKey(ClientModule module, Setting setting) {
        return "module." + module.id() + ".setting." + setting.id();
    }
}
