package dev.jilaton.client.module.impl;

import net.minecraft.world.entity.player.Input;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintTapInputTest {
    @Test
    void forwardSampleBecomesBackwardAndPreservesOtherControls() {
        Input tapped = SprintTapInput.apply(new Input(true, false, true, false, true, true, true));

        assertFalse(tapped.forward());
        assertTrue(tapped.backward());
        assertTrue(tapped.left());
        assertFalse(tapped.right());
        assertTrue(tapped.jump());
        assertTrue(tapped.shift());
        assertFalse(tapped.sprint());
    }

    @Test
    void sampleWithoutForwardInputIsUnchanged() {
        Input original = new Input(false, true, false, true, false, false, false);

        assertSame(original, SprintTapInput.apply(original));
    }
}
