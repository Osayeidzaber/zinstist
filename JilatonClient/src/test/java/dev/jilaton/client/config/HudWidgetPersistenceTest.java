package dev.jilaton.client.config;

import dev.jilaton.client.module.ModuleManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HudWidgetPersistenceTest {
    private static final List<String> FULL_WIDGETS = List.of("keystrokes", "performance", "coordinates",
            "armor", "effects", "keybinds", "server", "above_head", "held_item", "staff", "radar");

    @TempDir
    Path directory;

    @Test
    void fullWidgetLayoutSurvivesSaveAndLoad() {
        ModuleManager modules = new ModuleManager();
        ClientConfig source = new ClientConfig(directory);
        Map<String, HudStyle> styles = new HashMap<>();
        Map<String, ClientConfig.HudPosition> positions = new HashMap<>();
        for (int index = 0; index < FULL_WIDGETS.size(); index++) {
            String id = FULL_WIDGETS.get(index);
            HudStyle style = new HudStyle(1.0 + (index % 5) * 0.1, 0.9 - (index % 5) * 0.1,
                    index % 2 == 0 ? null : 0x8B5CF6,
                    HudStyle.Alignment.values()[index % HudStyle.Alignment.values().length], index != 1);
            ClientConfig.HudPosition position = new ClientConfig.HudPosition(0.1 + (index % 4) * 0.2,
                    0.9 - (index % 4) * 0.2);
            styles.put(id, style);
            positions.put(id, position);
            source.setHudStyle(id, style);
            source.setHudPosition(id, position.x(), position.y());
        }

        source.save(modules);
        ClientConfig restored = new ClientConfig(directory);
        restored.load(new ModuleManager());

        for (String id : FULL_WIDGETS) {
            assertEquals(styles.get(id), restored.hudStyle(id));
            assertEquals(positions.get(id), restored.hudPosition(id));
        }
    }

    @Test
    void configWrittenBeforeFullWidgetsLoadsAndAcceptsThemLater() throws Exception {
        Files.writeString(directory.resolve("jilaton.properties"),
                "ui.theme=violet\nhud.style.target.scale=1.5\nhud.position.target.x=0.1\nhud.position.target.y=0.2\n");
        ClientConfig config = new ClientConfig(directory);

        config.load(new ModuleManager());

        for (String id : FULL_WIDGETS) {
            assertEquals(HudStyle.DEFAULT, config.hudStyle(id));
            assertNull(config.hudPosition(id));
            config.setHudStyle(id, new HudStyle(1.5, 0.5, null, HudStyle.Alignment.FREE, true));
            config.setHudPosition(id, 0.5, 0.5);
            assertEquals(new ClientConfig.HudPosition(0.5, 0.5), config.hudPosition(id));
        }
        assertEquals(1.5, config.hudStyle("target").scale());
    }
}
