package dev.jilaton.client.module;

import dev.jilaton.client.module.impl.FullbrightModule;
import dev.jilaton.client.module.impl.SprintModule;
import net.minecraft.client.Minecraft;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModuleManager {
    private final Map<String, ClientModule> modules = new LinkedHashMap<>();

    public ModuleManager() {
        register(new SprintModule());
        register(new FullbrightModule());
    }

    private void register(ClientModule module) {
        if (modules.putIfAbsent(module.id(), module) != null) {
            throw new IllegalStateException("Duplicate module id: " + module.id());
        }
    }

    public Collection<ClientModule> all() {
        return modules.values();
    }

    public ClientModule get(String id) {
        return modules.get(id);
    }

    public void tick(Minecraft client) {
        modules.values().forEach(module -> module.tick(client));
    }
}
