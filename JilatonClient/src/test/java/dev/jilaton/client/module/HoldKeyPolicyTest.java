package dev.jilaton.client.module;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Hold-bound modules (FreeLook, Zoom) follow physical press/release edges. A programmatic enable
 * (ClickGUI toggle, config restore, verification harness) must survive ticks where the bound key
 * was never pressed; only a real release, an unfocused window, or an open screen may revoke it.
 */
class HoldKeyPolicyTest {
    @Test
    void programmaticEnableSurvivesTicksWithoutAKeyPress() {
        // Scene 32 regression: free_look is enabled with no ALT press, and the next tick used to revoke it.
        assertEquals(ModuleManager.HoldKeyAction.NONE,
                ModuleManager.holdKeyAction(false, false, true));
    }

    @Test
    void pressingThenHoldingTheKeyEnablesAndKeepsTheModuleOn() {
        assertEquals(ModuleManager.HoldKeyAction.ENABLE,
                ModuleManager.holdKeyAction(true, false, true));
        assertEquals(ModuleManager.HoldKeyAction.NONE,
                ModuleManager.holdKeyAction(true, true, true));
    }

    @Test
    void releasingTheKeyDisablesTheModule() {
        assertEquals(ModuleManager.HoldKeyAction.DISABLE,
                ModuleManager.holdKeyAction(false, true, true));
    }

    @Test
    void losingWindowFocusReleasesAHeldModule() {
        assertEquals(ModuleManager.HoldKeyAction.DISABLE,
                ModuleManager.holdKeyAction(true, true, false));
    }

    @Test
    void anUnfocusedWindowNeverEnablesAHoldModule() {
        assertEquals(ModuleManager.HoldKeyAction.DISABLE,
                ModuleManager.holdKeyAction(true, false, false));
        assertEquals(ModuleManager.HoldKeyAction.DISABLE,
                ModuleManager.holdKeyAction(false, false, false));
    }
}
