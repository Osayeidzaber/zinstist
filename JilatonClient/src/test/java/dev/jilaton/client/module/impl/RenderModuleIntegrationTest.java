package dev.jilaton.client.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.*;
import net.minecraft.world.InteractionHand;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class RenderModuleIntegrationTest {
    @TempDir Path directory;

    @Test void allNewRenderSettingsAndKeybindsPersistThroughProfiles() throws Exception {
        ModuleManager source = new ModuleManager();
        String[] ids = {"free_look", "freecam", "view_model", "custom_crosshair", "damage_particles", "hit_particles", "nametags", "custom_fov", "hud"};
        for (String id : ids) {
            ClientModule module = source.get(id);
            assertNotNull(module);
            module.setKeyCode(85);
            for (var setting : module.settings()) {
                if (setting instanceof NumberSetting number) number.setValue(number.maximum());
                if (setting instanceof BooleanSetting bool) bool.setValue(!bool.value());
                if (setting instanceof ChoiceSetting choice) choice.setValue(choice.choices().getLast());
            }
        }
        ClientConfig config = new ClientConfig(directory);
        config.saveProfile("render", source);
        ModuleManager restored = new ModuleManager();
        assertTrue(config.loadProfile("render", restored));
        for (String id : ids) {
            ClientModule expected = source.get(id), actual = restored.get(id);
            assertEquals(expected.keyCode(), actual.keyCode());
            assertEquals(expected.settings().size(), actual.settings().size());
            for (int i = 0; i < expected.settings().size(); i++) {
                var a = expected.settings().get(i);
                var b = actual.settings().get(i);
                assertEquals(a.id(), b.id());
                if (a instanceof NumberSetting number) assertEquals(number.value(), ((NumberSetting) b).value());
                if (a instanceof BooleanSetting bool) assertEquals(bool.value(), ((BooleanSetting) b).value());
                if (a instanceof ChoiceSetting choice) assertEquals(choice.value(), ((ChoiceSetting) b).value());
                if (a instanceof TextSetting text) assertEquals(text.value(), ((TextSetting) b).value());
            }
        }
    }

    @Test void viewModelHandTransformsAreIsolatedAndVanillaPoseIsIdentityByDefault() {
        ViewModelModule model = new ViewModelModule();
        PoseStack pose = new PoseStack();
        model.apply(pose, InteractionHand.MAIN_HAND, false);
        assertEquals(new Matrix4f(), pose.last().pose());
        number(model, "main_x").setValue(1);
        number(model, "main_scale").setValue(1.5);
        pose.pushPose();
        model.apply(pose, InteractionHand.MAIN_HAND, false);
        assertEquals(1, pose.last().pose().m30(), 0.001);
        assertEquals(1.5, pose.last().pose().m00(), 0.001);
        pose.popPose();
        model.apply(pose, InteractionHand.OFF_HAND, false);
        assertEquals(new Matrix4f(), pose.last().pose());
    }

    @Test void twoHandedMapBlendsEnabledHandsAndDisabledHandsDoNotLeakTransforms() {
        ViewModelModule model = new ViewModelModule();
        number(model, "main_x").setValue(1);
        number(model, "off_x").setValue(-0.5);
        PoseStack pose = new PoseStack();
        model.apply(pose, InteractionHand.MAIN_HAND, true);
        assertEquals(0.25, pose.last().pose().m30(), 0.001);
        model.settings().stream().filter(s -> s.id().equals("main_enabled")).map(s -> (BooleanSetting) s).findFirst().orElseThrow().setValue(false);
        pose = new PoseStack();
        model.apply(pose, InteractionHand.MAIN_HAND, true);
        assertEquals(-0.5, pose.last().pose().m30(), 0.001);
    }

    @Test void crosshairExpansionTracksCooldownAndClampsInvalidInputs() {
        assertEquals(0, CustomCrosshairModule.expansion(1, 3));
        assertEquals(3, CustomCrosshairModule.expansion(0, 3));
        assertEquals(2, CustomCrosshairModule.expansion(0.5F, 4));
        assertEquals(0, CustomCrosshairModule.expansion(2, 4));
        assertEquals(10, CustomCrosshairModule.expansion(-1, 50));
        assertEquals(0, CustomCrosshairModule.expansion(Float.NaN, 4));
    }

    private NumberSetting number(ClientModule module, String id) {
        return module.settings().stream().filter(s -> s.id().equals(id)).map(s -> (NumberSetting) s).findFirst().orElseThrow();
    }
}
