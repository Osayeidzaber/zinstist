package dev.jilaton.client.module.impl;

import net.minecraft.world.entity.player.Input;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreecamBodyInputTest {
    private static final Input IDLE = new Input(false, false, false, false, false, false, false);

    @Test
    void withoutLiveKeysOnlyTheCarriedIntentIsExposed() {
        FreecamBodyInput body = new FreecamBodyInput();
        body.learn(keys(true, false, false, false, false, true, true));

        Input applied = body.apply(IDLE, false);
        assertEquals(keys(true, false, false, false, false, true, true), applied);
        assertEquals(applied, body.apply(keys(false, false, true, false, true, false, false), false),
                "camera keys must not reach the body while live keys are disabled");
    }

    @Test
    void liveKeysAreMergedWithCarriedIntent() {
        FreecamBodyInput body = new FreecamBodyInput();
        body.learn(keys(true, false, false, true, false, false, false));

        Input applied = body.apply(keys(false, false, true, false, true, false, false), true);

        assertTrue(applied.forward());
        assertTrue(applied.left());
        assertTrue(applied.jump());
        assertFalse(applied.right(), "a live left key releases the carried right key");
    }

    @Test
    void oppositeLiveAxisReleasesOnlyThatCarriedDirection() {
        FreecamBodyInput body = new FreecamBodyInput();
        body.learn(keys(true, false, true, false, false, false, false));

        Input applied = body.apply(keys(false, true, false, false, true, true, true), true);

        assertEquals(keys(false, true, true, false, true, true, true), applied);
    }

    @Test
    void diagonalOppositeKeysReleaseBothCarriedAxesButKeepJump() {
        FreecamBodyInput body = new FreecamBodyInput();
        body.learn(keys(true, false, false, true, true, false, false));

        Input applied = body.apply(keys(false, true, true, false, false, false, false), true);

        assertEquals(keys(false, true, true, false, true, false, false), applied);
    }

    @Test
    void learnAccumulatesAndClearDropsEverything() {
        FreecamBodyInput body = new FreecamBodyInput();
        body.learn(keys(true, false, false, false, false, false, false));
        body.learn(keys(false, false, false, false, false, true, false));
        assertEquals(keys(true, false, false, false, false, true, false), body.apply(IDLE, false));

        body.clear();
        assertEquals(IDLE, body.apply(IDLE, false));
        body.clear();
        Input live = keys(false, true, false, true, true, false, false);
        assertEquals(live, body.apply(live, true), "clear leaves no carried bits to release or merge");
    }

    private static Input keys(boolean forward, boolean backward, boolean left, boolean right,
                              boolean jump, boolean shift, boolean sprint) {
        return new Input(forward, backward, left, right, jump, shift, sprint);
    }
}
