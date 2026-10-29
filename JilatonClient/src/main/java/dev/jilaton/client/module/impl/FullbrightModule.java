package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class FullbrightModule extends ClientModule {
    private final NumberSetting brightness = new NumberSetting("brightness", "Brightness", 1.0, 16.0, 0.5, 12.0);
    private double previousGamma = 1.0;

    public FullbrightModule() {
        super("fullbright", "Fullbright", "Raises local gamma while enabled.", ModuleCategory.RENDER);
    }

    @Override
    public java.util.List<dev.jilaton.client.setting.Setting> settings() {
        return java.util.List.of(brightness);
    }

    @Override
    protected void onEnable(Minecraft client) {
        previousGamma = client.options.gamma().get();
        apply(client);
    }

    @Override
    protected void onDisable(Minecraft client) {
        double applied = brightness.value();
        if (Math.abs(client.options.gamma().get() - applied) < 0.0001) {
            client.options.gamma().set(previousGamma);
        }
    }

    @Override
    protected void onTick(Minecraft client) {
        apply(client);
    }

    public NumberSetting brightness() {
        return brightness;
    }

    private void apply(Minecraft client) {
        client.options.gamma().set(brightness.value());
    }
}
