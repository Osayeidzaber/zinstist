package dev.jilaton.client.module.impl;

import net.minecraft.world.entity.player.Input;

import java.util.Objects;

/** Converts one forward movement sample into a W-Tap or S-Tap sprint-reset sample. */
public final class SprintTapInput {
    private SprintTapInput() {
    }

    public static Input apply(Input current, boolean tapBackward) {
        Objects.requireNonNull(current, "current");
        if (!current.forward()) {
            return current;
        }
        return new Input(false, tapBackward || current.backward(), current.left(), current.right(),
                current.jump(), current.shift(), false);
    }
}
