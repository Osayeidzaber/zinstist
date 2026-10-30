package dev.jilaton.client.module;

import dev.jilaton.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class ClientModule {
    private static final Logger LOGGER = LoggerFactory.getLogger("Jilaton");
    private final String id;
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final List<Setting> settings = new ArrayList<>();
    private final List<Setting> settingsView = Collections.unmodifiableList(settings);
    private boolean enabled;
    private int keyCode = -1;

    protected ClientModule(String id, String name, String description, ModuleCategory category) {
        this.id = requireId(id);
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.category = Objects.requireNonNull(category, "category");
    }

    private static String requireId(String id) {
        Objects.requireNonNull(id, "id");
        if (!id.matches("[a-z0-9][a-z0-9_-]*")) {
            throw new IllegalArgumentException("Module ids must be stable lowercase identifiers: " + id);
        }
        return id;
    }

    protected final <S extends Setting> S addSetting(S setting) {
        Objects.requireNonNull(setting, "setting");
        if (settings.stream().anyMatch(existing -> existing.id().equals(setting.id()))) {
            throw new IllegalArgumentException("Duplicate setting id: " + id + "/" + setting.id());
        }
        settings.add(setting);
        return setting;
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
        return settingsView;
    }

    public final boolean enabled() {
        return enabled;
    }

    public boolean enabledByDefault() {
        return false;
    }

    public final int keyCode() {
        return keyCode;
    }

    public final void setKeyCode(int keyCode) {
        if (keyCode < -1) {
            throw new IllegalArgumentException("Key code must be -1 (unbound) or a nonnegative key value");
        }
        this.keyCode = keyCode;
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        if (enabled) {
            try {
                onEnable(Minecraft.getInstance());
                this.enabled = true;
            } catch (Throwable failure) {
                this.enabled = false;
                try {
                    onDisable(Minecraft.getInstance());
                } catch (Throwable cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                LOGGER.error("Module '{}' failed to enable and was rolled back", id, failure);
                if (failure instanceof Error error) {
                    throw error;
                }
            }
        } else {
            try {
                onDisable(Minecraft.getInstance());
            } catch (Throwable failure) {
                LOGGER.error("Module '{}' failed while disabling", id, failure);
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
            try {
                onTick(client);
            } catch (Throwable failure) {
                LOGGER.error("Module '{}' failed while ticking and will be disabled", id, failure);
                setEnabled(false);
                if (failure instanceof Error error) {
                    throw error;
                }
            }
        }
    }

    public final void handleMovementInput(MovementInputEvent event) {
        if (!enabled) {
            return;
        }
        try {
            onMovementInput(event);
        } catch (Throwable failure) {
            LOGGER.error("Module '{}' failed while handling movement input and will be disabled", id, failure);
            setEnabled(false);
            if (failure instanceof Error error) {
                throw error;
            }
        }
    }

    public final void handlePlayerMove(PlayerMoveEvent event) {
        if (!enabled) {
            return;
        }
        try {
            onPlayerMove(event);
        } catch (Throwable failure) {
            LOGGER.error("Module '{}' failed while handling player movement and will be disabled", id, failure);
            setEnabled(false);
            if (failure instanceof Error error) {
                throw error;
            }
        }
    }

    public final void handleOutgoingPacket(Packet<?> packet) {
        if (!enabled) {
            return;
        }
        try {
            onOutgoingPacket(packet);
        } catch (Throwable failure) {
            LOGGER.error("Module '{}' failed while handling an outgoing packet and will be disabled", id, failure);
            setEnabled(false);
            if (failure instanceof Error error) {
                throw error;
            }
        }
    }

    protected void onEnable(Minecraft client) {
    }

    protected void onDisable(Minecraft client) {
    }

    protected void onTick(Minecraft client) {
    }

    protected void onMovementInput(MovementInputEvent event) {
    }

    protected void onPlayerMove(PlayerMoveEvent event) {
    }

    protected void onOutgoingPacket(Packet<?> packet) {
    }
}
