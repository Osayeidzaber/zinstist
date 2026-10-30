package dev.jilaton.client.module;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

import java.util.Objects;

/** Mutable snapshot of the current client movement input, dispatched once per keyboard sample. */
public final class MovementInputEvent {
    private Input input;
    private Vec2 moveVectorOverride;

    public MovementInputEvent(Input input) {
        this.input = Objects.requireNonNull(input, "input");
    }

    public Input input() {
        return input;
    }

    public void setInput(Input input) {
        this.input = Objects.requireNonNull(input, "input");
    }

    public Vec2 moveVectorOverride() {
        return moveVectorOverride;
    }

    public void setMoveVectorOverride(Vec2 moveVectorOverride) {
        this.moveVectorOverride = Objects.requireNonNull(moveVectorOverride, "moveVectorOverride");
    }
}
