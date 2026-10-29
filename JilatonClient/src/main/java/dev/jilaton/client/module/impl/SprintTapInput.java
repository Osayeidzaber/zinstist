package dev.jilaton.client.module.impl;

import net.minecraft.world.entity.player.Input;

import java.util.Objects;

/** Converts one forward movement sample into a backward sprint-reset sample. */
public final class SprintTapInput {
    private SprintTapInput() {
    }

    public static Input apply(Input current) {
        Objects.requireNonNull(current, "current");
        if (!current.forward()) {
            return current;
        }
        return new Input(false, true, current.left(), current.right(), current.jump(), current.shift(), false);
    }
}
