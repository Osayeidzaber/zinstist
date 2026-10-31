package dev.jilaton.client.render.character;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.ChoiceSetting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JA-003 regression: a dropped .glb must travel the whole chain to a selectable, persisted choice, and a deleted
 * file must fall back deterministically without a crash. Scan is lazy, so the fixture deliberately contains bytes
 * that are never parsed.
 */
class GlbSelectionRegressionTest {
    private static final String CUSTOM = "My Hero";

    @TempDir
    Path dropFolder;

    @TempDir
    Path configFolder;

    @AfterEach
    void restoreCatalog() {
        CharacterCatalog.refresh(null);
    }

    @Test
    void droppedGlbBecomesSelectablePersistsAndFallsBackWhenDeleted() throws IOException {
        Path model = dropFolder.resolve(CUSTOM + ".glb");
        Files.write(model, "not parsed during selection".getBytes(StandardCharsets.UTF_8));

        ModuleManager modules = new ModuleManager();
        CharacterModule character = characterModule(modules);
        ChoiceSetting setting = characterSetting(character);
        assertFalse(setting.choices().contains(CUSTOM), "the custom name must not appear before the scan");
        assertEquals(List.of("Sakura"), CharacterCatalog.names().stream().limit(1).toList());

        assertTrue(character.refreshCharacters(dropFolder), "the tick refresh must report the new design");
        assertTrue(CharacterCatalog.names().contains(CUSTOM));
        assertTrue(setting.choices().contains(CUSTOM), "the choice must offer the scanned display name");
        setting.setValue(CUSTOM);
        assertEquals(CUSTOM, setting.value());

        new ClientConfig(configFolder).save(modules);

        // Restart: options load before the drop folder is scanned, so the saved custom value is deferred.
        CharacterCatalog.refresh(null);
        ModuleManager restarted = new ModuleManager();
        CharacterModule restartedCharacter = characterModule(restarted);
        ChoiceSetting restartedSetting = characterSetting(restartedCharacter);
        new ClientConfig(configFolder).load(restarted);
        assertEquals(CUSTOM, restartedSetting.persistedValue(), "a deferred value must survive the save/load");
        assertFalse(restartedSetting.choices().contains(CUSTOM));

        assertTrue(restartedCharacter.refreshCharacters(dropFolder), "the first scan after load resolves the value");
        assertEquals(CUSTOM, restartedSetting.value(), "the saved custom character must win once offered");

        // Delete the file: the next refresh drops the design and falls back to a deterministic built-in.
        Files.delete(model);
        assertTrue(restartedCharacter.refreshCharacters(dropFolder), "removing a design must be reported");
        assertFalse(CharacterCatalog.names().contains(CUSTOM));
        assertEquals("Sakura", restartedSetting.value(), "a deleted custom character falls back to the first built-in");
        assertEquals("Sakura", restartedSetting.persistedValue());
        assertEquals("sakura", CharacterCatalog.byName(restartedSetting.value()).id());

        new ClientConfig(configFolder).save(restarted);
        ModuleManager fallenBack = new ModuleManager();
        new ClientConfig(configFolder).load(fallenBack);
        assertEquals("Sakura", characterSetting(characterModule(fallenBack)).value());
    }

    @Test
    void aFileNamedLikeABuiltInNeverShadowsIt() throws IOException {
        Files.write(dropFolder.resolve("Sakura.glb"), new byte[]{1, 2, 3});

        assertEquals(1, CustomCharacters.scan(dropFolder).size(), "the scanner still sees the file itself");
        assertFalse(CharacterCatalog.refresh(dropFolder), "the catalogue must not change for a built-in collision");
        assertEquals(9, CharacterCatalog.all().size(), "no duplicate built-in display name may be added");
        assertEquals("sakura", CharacterCatalog.byName("Sakura").id());
    }

    @Test
    void aGltfSceneFileIsReportedButNeverSelectable() throws IOException {
        Files.write(dropFolder.resolve("scene.gltf"), "{\"asset\":{\"version\":\"2.0\"}}".getBytes(StandardCharsets.UTF_8));

        List<Path> unsupported = CustomCharacters.unsupportedGltf(dropFolder);
        assertEquals(1, unsupported.size());
        assertEquals("scene.gltf", unsupported.get(0).getFileName().toString());
        assertTrue(CustomCharacters.scan(dropFolder).isEmpty(), "a .gltf must never be returned as a design");
        assertFalse(CharacterCatalog.refresh(dropFolder), "a .gltf must not change the catalogue");
        assertFalse(CharacterCatalog.names().contains("scene"));
    }

    private static CharacterModule characterModule(ModuleManager modules) {
        ClientModule module = modules.get("character-3d");
        assertTrue(module instanceof CharacterModule, "character-3d must be registered as CharacterModule");
        return (CharacterModule) module;
    }

    private static ChoiceSetting characterSetting(ClientModule module) {
        for (var setting : module.settings()) {
            if (setting.id().equals("character") && setting instanceof ChoiceSetting choice) {
                return choice;
            }
        }
        throw new AssertionError("character setting is missing");
    }
}
