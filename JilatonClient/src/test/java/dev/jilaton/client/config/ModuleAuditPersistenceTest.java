package dev.jilaton.client.config;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.Setting;
import dev.jilaton.client.setting.TextSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless round trip of every registered module setting through {@link ClientConfig}. The config is
 * written to a temporary directory; no Fabric config directory is involved.
 */
class ModuleAuditPersistenceTest {
    @Test
    void everySettingSurvivesSaveAndReloadIntoSeparateManagers(@TempDir Path directory) throws Exception {
        ModuleManager first = new ModuleManager();
        for (ClientModule module : first.all()) {
            for (Setting setting : module.settings()) {
                if (setting instanceof BooleanSetting booleanSetting) {
                    booleanSetting.setValue(!booleanSetting.value());
                } else if (setting instanceof NumberSetting numberSetting) {
                    numberSetting.setValue(numberSetting.minimum()
                            + (numberSetting.maximum() - numberSetting.minimum()) * 0.5);
                } else if (setting instanceof ChoiceSetting choiceSetting) {
                    choiceSetting.setValue(choiceSetting.choices().get(choiceSetting.choices().size() - 1));
                }
            }
        }
        ClientModule fullbright = require(first, "fullbright");
        ClientModule reach = require(first, "reach");
        fullbright.setEnabled(true);
        reach.setEnabled(true);
        fullbright.setKeyCode(66);

        Path configFile = directory.resolve("jilaton.properties");
        new ClientConfig(directory).save(first);
        assertTrue(Files.isRegularFile(configFile), "config file was not written");
        String persisted = Files.readString(configFile);
        assertTrue(persisted.contains("module.fullbright.setting.gamma="), "gamma was not serialized");
        assertTrue(persisted.contains("module.fullbright.enabled=true"), "module state was not serialized");

        ModuleManager second = new ModuleManager();
        new ClientConfig(directory).load(second);

        for (ClientModule before : first.all()) {
            ClientModule after = second.get(before.id());
            assertNotNull(after, "module disappeared after reload: " + before.id());
            assertEquals(before.keyCode(), after.keyCode(), "key code changed: " + before.id());
            assertEquals(before.enabled(), after.enabled(), "enabled state changed: " + before.id());
            assertEquals(before.settings().size(), after.settings().size(),
                    "setting count changed: " + before.id());
            for (Setting setting : before.settings()) {
                Setting reloaded = after.settings().stream()
                        .filter(candidate -> candidate.id().equals(setting.id()))
                        .findFirst().orElseThrow(() -> new AssertionError(
                                "setting disappeared: " + before.id() + "/" + setting.id()));
                assertEquals(capture(setting), capture(reloaded),
                        "value changed for " + before.id() + "/" + setting.id());
            }
        }
        assertTrue(second.get("fullbright").enabled(), "fullbright did not stay enabled");
        assertTrue(second.get("reach").enabled(), "reach did not stay enabled");
        assertFalse(second.get("freecam").enabled(), "a non-persisted module was restored enabled");
    }

    @Test
    void loadingAMissingFileLeavesTheFreshRegistryUsable(@TempDir Path directory) {
        ModuleManager manager = new ModuleManager();
        new ClientConfig(directory.resolve("absent")).load(manager);
        assertEquals(manager.all().size(),
                manager.all().stream().map(ClientModule::id).distinct().count());
    }

    private static ClientModule require(ModuleManager manager, String id) {
        ClientModule module = manager.get(id);
        assertNotNull(module, "missing module: " + id);
        return module;
    }

    private static String capture(Setting setting) {
        if (setting instanceof BooleanSetting booleanSetting) {
            return "boolean:" + booleanSetting.value();
        }
        if (setting instanceof NumberSetting numberSetting) {
            return "number:" + Double.toString(numberSetting.value());
        }
        if (setting instanceof ChoiceSetting choiceSetting) {
            return "choice:" + choiceSetting.value();
        }
        if (setting instanceof TextSetting textSetting) {
            return "text:" + textSetting.value();
        }
        throw new AssertionError("unknown setting type: " + setting.getClass().getName());
    }
}
