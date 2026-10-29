package dev.jilaton.client.module;

import dev.jilaton.client.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.List;

public abstract class ClientModule {
    private final String id;
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private boolean enabled;

    protected ClientModule(String id, String name, String description, ModuleCategory category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public final String id() {
        return id;
    }

    public final String name() {
        return name;
    }

    public final String description() {
        return description;
    }

    public final ModuleCategory category() {
        return category;
    }

    public List<Setting> settings() {
        return List.of();
    }

    public final boolean enabled() {
        return enabled;
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        if (enabled) {
            onEnable(Minecraft.getInstance());
            this.enabled = true;
        } else {
            try {
                onDisable(Minecraft.getInstance());
            } finally {
                this.enabled = false;
            }
        }
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void tick(Minecraft client) {
        if (enabled) {
            onTick(client);
        }
    }

    protected void onEnable(Minecraft client) {
    }

    protected void onDisable(Minecraft client) {
    }

    protected void onTick(Minecraft client) {
    }
}
