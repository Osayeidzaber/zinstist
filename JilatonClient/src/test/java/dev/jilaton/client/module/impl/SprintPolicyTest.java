package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintPolicyTest {
    @Test
    void forwardOnlyRequiresForwardInputAndOmnidirectionalModeAcceptsAnyDirection() {
        assertTrue(sprint(true, true, false, false, false));
        assertFalse(sprint(true, false, false, true, false));
        assertTrue(sprint(false, false, false, true, false));
        assertTrue(sprint(false, false, true, false, false));
        assertFalse(sprint(false, false, false, false, false));
    }

    @Test
    void configurableVanillaConditionsGateSprint() {
        assertFalse(SprintPolicy.shouldSprint(false, true, false, false, false,
                true, false, false, false, false, false, false, false));
        assertTrue(SprintPolicy.shouldSprint(false, true, false, false, false,
                false, false, false, false, false, false, false, false));
        assertFalse(SprintPolicy.shouldSprint(false, true, false, false, false,
                false, true, true, true, false, false, false, false));
        assertFalse(SprintPolicy.shouldSprint(false, true, false, false, false,
                false, true, false, false, true, true, false, false));
        assertFalse(SprintPolicy.shouldSprint(false, true, false, false, false,
                false, true, false, false, false, false, true, true));
    }

    private static boolean sprint(boolean forwardOnly, boolean forward, boolean backward,
                                  boolean left, boolean right) {
        return SprintPolicy.shouldSprint(forwardOnly, forward, backward, left, right,
                true, true, true, false, true, false, true, false);
    }
}
