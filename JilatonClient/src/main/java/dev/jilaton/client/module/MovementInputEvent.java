package dev.jilaton.client.module;

import net.minecraft.world.entity.player.Input;

import java.util.Objects;

/** Mutable snapshot of the current client movement input, dispatched once per keyboard sample. */
public final class MovementInputEvent {
    private Input input;

    public MovementInputEvent(Input input) {
        this.input = Objects.requireNonNull(input, "input");
    }

    public Input input() {
        return input;
    }

    public void setInput(Input input) {
        this.input = Objects.requireNonNull(input, "input");
    }
}
