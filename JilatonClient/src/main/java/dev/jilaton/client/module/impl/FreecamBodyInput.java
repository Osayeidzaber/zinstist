package dev.jilaton.client.module.impl;

import net.minecraft.world.entity.player.Input;

/** Carried body intent is separate from the keys currently steering the camera. */
public final class FreecamBodyInput {
    private int held;
    public void clear() { held = 0; }
    public void learn(Input input) { held |= bits(input); }

    public Input apply(Input keys, boolean liveKeys) {
        if (!liveKeys) return input(held);
        // Opposite live directions release the carried direction instead of canceling both.
        if (keys.backward()) held &= ~1;
        if (keys.forward()) held &= ~2;
        if (keys.right()) held &= ~4;
        if (keys.left()) held &= ~8;
        return input(held | bits(keys));
    }

    private static int bits(Input input) {
        return (input.forward() ? 1 : 0) | (input.backward() ? 2 : 0) | (input.left() ? 4 : 0)
                | (input.right() ? 8 : 0) | (input.jump() ? 16 : 0) | (input.shift() ? 32 : 0) | (input.sprint() ? 64 : 0);
    }
    private static Input input(int bits) {
        return new Input((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0,
                (bits & 16) != 0, (bits & 32) != 0, (bits & 64) != 0);
    }
}
