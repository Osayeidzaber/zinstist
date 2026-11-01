package dev.jilaton.client.module.impl;

import net.minecraft.world.entity.player.Input;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutoWalkInputTest {
    private static final Input REST = new Input(false, false, false, false, false, false, false);

    @Test void allDirectionsWorkAndChangingDirectionDoesNotCarryPreviousAutomation() {
        for (String direction : new String[]{"Forward", "Backward", "Left", "Right"}) {
            Input input = AutoWalkModule.walking(REST, direction, false, false);
            assertEquals(direction.equals("Forward"), input.forward());
            assertEquals(direction.equals("Backward"), input.backward());
            assertEquals(direction.equals("Left"), input.left());
            assertEquals(direction.equals("Right"), input.right());
        }
        assertEquals(REST, AutoWalkModule.walking(REST, "Forward", true, false));
    }

    @Test void safetySuppressesAutomationWhilePreservingPhysicalInput() {
        Input manual = new Input(false, true, true, false, true, true, true);
        assertEquals(manual, AutoWalkModule.walking(manual, "Forward", true, false));
        assertTrue(AutoWalkModule.walking(REST, "Forward", false, true).jump());
        assertFalse(AutoWalkModule.manualMovement(REST));
        assertTrue(AutoWalkModule.manualMovement(manual));
    }

    @Test void restartNeverResumesAutomatedWalking() {
        assertFalse(new AutoWalkModule().persistEnabled());
    }
}
