package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MouseAimStateTest {
    @Test void onlyTheLegacyModeReservesAttackInputFromAura() {
        AimAssistModule module = new AimAssistModule();
        module.setEnabled(true);
        try {
            assertTrue(module.claimsAttackInput());
            module.settings().stream().filter(s -> s.id().equals("mode"))
                    .map(dev.jilaton.client.setting.ChoiceSetting.class::cast).findFirst().orElseThrow().setValue("Mouse assist");
            assertFalse(module.claimsAttackInput());
        } finally { module.setEnabled(false); }
        assertFalse(module.claimsAttackInput());
    }

    @Test void idleMouseAndOppositeInputNeverPullTowardTheTarget() {
        MouseAimState state = new MouseAimState();
        assertEquals(MouseAimState.STILL, state.nudge(0, 0, 90, 30, true, .016, 60, 1, 1, 1000));
        assertEquals(MouseAimState.STILL, state.nudge(-1, -1, 90, 30, true, .016, 60, 1, 1, 1001));
        var turn = state.nudge(1, 1, 90, 30, true, .016, 60, 1, 1, 1002);
        assertTrue(turn.yaw() > 0);
        assertTrue(turn.pitch() > 0);
        assertEquals(MouseAimState.STILL, state.nudge(0, 0, 90, 30, true, .016, 60, 1, 1, 1153));
    }

    @Test void correctionsRetainSubCountFractionsAndStopWhenTargetIsLost() {
        MouseAimState state = new MouseAimState();
        double total = 0;
        for (int i = 0; i < 20; i++) total += state.nudge(1, 0, 90, 0, true, .001, 10, 1, 1, i).yaw();
        assertEquals(1, total);
        assertEquals(MouseAimState.STILL, state.nudge(1, 1, 90, 90, false, .05, 60, 1, 1, 21));
        assertEquals(MouseAimState.STILL, state.nudge(0, 0, 90, 90, true, .001, 10, 1, 1, 22));
    }

    @Test void frameTimeIsCappedAndMouseSensitivityQuantizationIsPreserved() {
        MouseAimState state = new MouseAimState();
        var turn = state.nudge(1, 1, 90, 90, true, 5, 60, 1, .5, 1000);
        // Output is still in turn() mouse units; vanilla multiplies it by 0.15 degrees.
        assertTrue(turn.yaw() * .15 <= 3.00001);
        assertEquals(0, turn.yaw() % .5);
        assertEquals(MouseAimState.STILL, state.nudge(1, 1, 90, 90, true, Double.NaN, 60, 1, 1, 1001));
    }
}
