package dev.jilaton.client.module;

import dev.jilaton.client.module.impl.AimAssistModule;
import dev.jilaton.client.module.impl.AutoArmorModule;
import dev.jilaton.client.module.impl.AutoClickerModule;
import dev.jilaton.client.module.impl.AutoGappleModule;
import dev.jilaton.client.module.impl.AutoTotemModule;
import dev.jilaton.client.module.impl.FullbrightModule;
import dev.jilaton.client.module.impl.HudModule;
import dev.jilaton.client.module.impl.SprintModule;
import dev.jilaton.client.module.impl.TriggerbotModule;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModuleManager {
    private final Map<String, ClientModule> modules = new LinkedHashMap<>();
    private final Collection<ClientModule> modulesView = Collections.unmodifiableCollection(modules.values());
    private final Map<String, Boolean> previousKeyStates = new HashMap<>();

    public ModuleManager() {
        register(new AimAssistModule());
        register(new TriggerbotModule());
        register(new AutoClickerModule());
        register(new AutoTotemModule());
        register(new AutoGappleModule());
        register(new AutoArmorModule());
        register(new SprintModule());
        register(new FullbrightModule());
        register(new HudModule());
    }

    private void register(ClientModule module) {
        if (modules.putIfAbsent(module.id(), module) != null) {
            throw new IllegalStateException("Duplicate module id: " + module.id());
        }
    }

    public Collection<ClientModule> all() {
        return modulesView;
    }

    public ClientModule get(String id) {
        return modules.get(id);
    }

    public void tick(Minecraft client) {
        updateKeybinds(client);
        modules.values().forEach(module -> module.tick(client));
    }

    public void disableAll() {
        modules.values().forEach(module -> module.setEnabled(false));
    }

    private void updateKeybinds(Minecraft client) {
        if (client.screen != null || client.getWindow() == null) {
            previousKeyStates.replaceAll((id, wasDown) -> false);
            return;
        }

        long window = client.getWindow().handle();
        for (ClientModule module : modules.values()) {
            int keyCode = module.keyCode();
            boolean down = keyCode >= 0 && GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
            boolean wasDown = previousKeyStates.getOrDefault(module.id(), false);
            if (down && !wasDown) {
                module.toggle();
            }
            previousKeyStates.put(module.id(), down);
        }
    }
}
