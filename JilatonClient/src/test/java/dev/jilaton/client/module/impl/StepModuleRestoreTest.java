package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Disabling Step must hand collision height back to vanilla on the next query. */
class StepModuleRestoreTest {
    @Test
    void disablingRestoresTheVanillaStepHeight() {
        StepModule module = new StepModule();
        assertEquals(0.6F, module.effectiveHeight(0.6F));

        module.setEnabled(true);
        try {
            assertEquals(1.0F, module.effectiveHeight(0.6F));
            assertEquals(2.0F, module.effectiveHeight(2.0F), "vanilla heights above the setting still win");
        } finally {
            module.setEnabled(false);
        }

        assertEquals(0.6F, module.effectiveHeight(0.6F));
        assertEquals(2.0F, module.effectiveHeight(2.0F));
    }
}
