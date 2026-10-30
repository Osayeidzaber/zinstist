package dev.jilaton.client.config;

import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.Setting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientConfigRecoveryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void aMalformedEscapeNeitherCrashesStartupNorLosesTheOriginalFile() throws Exception {
        Path file = temporaryDirectory.resolve("jilaton.properties");
        byte[] original = "module.kill_aura.enabled=true\nui.theme=\\u12\n".getBytes(StandardCharsets.ISO_8859_1);
        Files.write(file, original);

        assertDoesNotThrow(() -> new ClientConfig(temporaryDirectory).load(new ModuleManager()));

        List<Path> backups;
        try (var files = Files.list(temporaryDirectory)) {
            backups = files.filter(path -> path.getFileName().toString().startsWith("jilaton.properties.unreadable-")).toList();
        }
        assertEquals(1, backups.size());
        assertArrayEquals(original, Files.readAllBytes(backups.get(0)));
    }

    @Test
    void aSavedCustomCharacterSurvivesASessionWhereItWasNotScannedYet() {
        ModuleManager modules = new ModuleManager();
        character(modules).setValue("bacon-hair");
        ClientConfig config = new ClientConfig(temporaryDirectory);
        config.save(modules);

        ModuleManager restored = new ModuleManager();
        ClientConfig reloaded = new ClientConfig(temporaryDirectory);
        reloaded.load(restored);
        assertEquals("bacon-hair", character(restored).persistedValue());

        reloaded.save(restored);
        ModuleManager again = new ModuleManager();
        new ClientConfig(temporaryDirectory).load(again);
        assertEquals("bacon-hair", character(again).persistedValue());
    }

    @Test
    void theClickGuiVisualOrderRoundTripsAndDropsDuplicatesAndJunk() {
        ClientConfig config = new ClientConfig(temporaryDirectory);
        config.setVisualOrder(List.of("trails", "halo", "trails", "Bad Id", "wings"));
        config.save(new ModuleManager());

        ClientConfig restored = new ClientConfig(temporaryDirectory);
        restored.load(new ModuleManager());
        assertEquals(List.of("trails", "halo", "wings"), restored.visualOrder());
    }

    private static ChoiceSetting character(ModuleManager modules) {
        for (Setting setting : modules.get("character-3d").settings()) {
            if (setting.id().equals("character") && setting instanceof ChoiceSetting choice) return choice;
        }
        throw new AssertionError("character-3d has no character setting");
    }
}
