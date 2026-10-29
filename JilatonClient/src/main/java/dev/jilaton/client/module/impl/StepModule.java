package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;

public final class StepModule extends ClientModule {
    private final NumberSetting height = addSetting(
            new NumberSetting("height", "Height", 0.6, 2.5, 0.1, 1.0));

    public StepModule() {
        super("step", "Step", "Raises vanilla step height for the local player.", ModuleCategory.MOVEMENT);
    }

    public float effectiveHeight(float vanillaHeight) {
        return StepPolicy.effectiveHeight(enabled(), true, vanillaHeight, height.value());
    }
}
