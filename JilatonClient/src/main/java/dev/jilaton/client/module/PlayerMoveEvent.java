package dev.jilaton.client.module;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** Mutable local-player movement vector dispatched immediately before vanilla applies movement. */
public final class PlayerMoveEvent {
    private final MoverType type;
    private Vec3 movement;

    public PlayerMoveEvent(MoverType type, Vec3 movement) {
        this.type = Objects.requireNonNull(type, "type");
        this.movement = Objects.requireNonNull(movement, "movement");
    }

    public MoverType type() {
        return type;
    }

    public Vec3 movement() {
        return movement;
    }

    public void setMovement(Vec3 movement) {
        this.movement = Objects.requireNonNull(movement, "movement");
    }
}
