package dev.jilaton.client.module;

import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.Setting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Audits the live module registry without a game client. Every assertion here runs in the normal
 * unit-test JVM, where {@link Minecraft#getInstance()} is null, so a module constructor that touched
 * a Minecraft registry or created client state would fail this suite instead of the game.
 */
class ModuleAuditRegistryTest {
    @Test
    void everyRegisteredModuleHasStableIdentityAndValidSettings() {
        ModuleManager manager = new ModuleManager();
        assertFalse(manager.all().isEmpty(), "module registry must not be empty");

        Set<String> ids = new HashSet<>();
        for (ClientModule module : manager.all()) {
            assertTrue(ids.add(module.id()), "duplicate module id: " + module.id());
            assertTrue(module.id().matches("[a-z0-9][a-z0-9_-]*"), "unstable module id: " + module.id());
            assertNotNull(module.name(), "null name for " + module.id());
            assertFalse(module.name().isBlank(), "blank name for " + module.id());
            assertNotNull(module.description(), "null description for " + module.id());
            assertFalse(module.description().isBlank(), "blank description for " + module.id());
            assertNotNull(module.category(), "null category for " + module.id());
            assertTrue(module.keyCode() >= -1, "invalid key code for " + module.id());

            Set<String> settingIds = new HashSet<>();
            for (Setting setting : module.settings()) {
                String where = module.id() + "/" + setting.id();
                assertTrue(settingIds.add(setting.id()), "duplicate setting id: " + where);
                assertNotNull(setting.id(), "null setting id on " + module.id());
                assertFalse(setting.id().isBlank(), "blank setting id on " + module.id());
                assertTrue(setting.id().matches("[a-z0-9][a-z0-9_-]*"), "unstable setting id: " + where);
                assertNotNull(setting.label(), "null setting label: " + where);
                assertFalse(setting.label().isBlank(), "blank setting label: " + where);

                if (setting instanceof NumberSetting number) {
                    assertTrue(Double.isFinite(number.minimum()), "non-finite minimum: " + where);
                    assertTrue(Double.isFinite(number.maximum()), "non-finite maximum: " + where);
                    assertTrue(number.minimum() <= number.maximum(), "inverted range: " + where);
                    assertTrue(Double.isFinite(number.value()), "non-finite value: " + where);
                    assertTrue(number.value() >= number.minimum() - 1.0E-9
                            && number.value() <= number.maximum() + 1.0E-9, "default out of range: " + where);
                } else if (setting instanceof ChoiceSetting choice) {
                    assertFalse(choice.choices().isEmpty(), "no choices: " + where);
                    assertTrue(choice.choices().contains(choice.value()), "default not offered: " + where);
                } else if (setting instanceof TextSetting text) {
                    assertTrue(text.maximumLength() > 0, "non-positive maximum length: " + where);
                    assertNotNull(text.value(), "null text value: " + where);
                    assertTrue(text.value().length() <= text.maximumLength(), "text too long: " + where);
                } else {
                    assertTrue(setting instanceof BooleanSetting, "unknown setting type: " + where);
                }
            }
        }
    }

    @Test
    void enablingThenDisablingTwiceReturnsToTheOriginalState() {
        ModuleManager manager = new ModuleManager();
        for (ClientModule module : manager.all()) {
            boolean original = module.enabled();
            int originalKey = module.keyCode();
            for (int cycle = 0; cycle < 2; cycle++) {
                try {
                    module.setEnabled(true);
                } catch (Throwable headlessFailure) {
                    // A few modules reach client-only services from onEnable (for example
                    // JilatonClient's static config, whose FabricLoader config dir is null in tests).
                    // ClientModule tears the module down before rethrowing an Error, so the only
                    // contract to check here is that the rollback left the registry disabled.
                    assertFalse(module.enabled(), "failed enable left the module on: " + module.id());
                }
                // Regardless of whether the enable succeeded or rolled back, disabling must always
                // leave the module off, reusable and with its key binding intact.
                module.setEnabled(false);
                assertFalse(module.enabled(), "disable did not restore the off state: " + module.id());
                assertFalse(manager.all().stream().anyMatch(ClientModule::enabled),
                        "a module stayed enabled after the lifecycle cycle: " + module.id());
            }
            assertEquals(original, module.enabled(), "lifecycle changed the original state: " + module.id());
            assertEquals(originalKey, module.keyCode(), "lifecycle changed the key binding: " + module.id());
        }
    }

    @Test
    void constructionDoesNotCreateAClientOrDependOnRegistryBootstrap() {
        // The whole registry is constructed while Minecraft has no instance: that is only possible if no
        // constructor reaches Minecraft.getInstance(), ItemStack/Items or another bootstrapped registry.
        assertNull(Minecraft.getInstance());
        ModuleManager manager = new ModuleManager();
        assertNull(Minecraft.getInstance(), "module construction created a Minecraft client");
        assertTrue(manager.all().size() >= 90, "module registry unexpectedly small: " + manager.all().size());
        assertEquals(manager.all().size(),
                manager.all().stream().map(ClientModule::id).distinct().count(),
                "module ids are not unique");
    }
}
