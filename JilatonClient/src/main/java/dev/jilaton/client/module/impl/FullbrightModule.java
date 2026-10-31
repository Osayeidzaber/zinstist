package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;

public final class FullbrightModule extends ClientModule {
    private final NumberSetting gamma;

    public FullbrightModule() {
        super("fullbright", "Fullbright",
                "Brightens the local lightmap without touching the saved gamma option.",
                ModuleCategory.RENDER);
        // Same range as the reference client: the lightmap uniform is a plain float, not the
        // clamped brightness slider, so values above 1 are legal and are not written to options.
        gamma = addSetting(new NumberSetting("gamma", "Gamma", 1.0, 16.0, 0.5, 8.0));
    }

    public NumberSetting gamma() {
        return gamma;
    }

    public float lightmapGamma() {
        return (float) Math.max(1.0, gamma.value());
    }
}
