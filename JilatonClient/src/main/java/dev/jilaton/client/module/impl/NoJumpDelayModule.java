package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;

import java.util.concurrent.ThreadLocalRandom;

public final class NoJumpDelayModule extends ClientModule {
    private final NumberSetting cooldown = addSetting(
            new NumberSetting("cooldown", "Cooldown", 0.0, 9.0, 1.0, 0.0));
    private final NumberSetting variance = addSetting(
            new NumberSetting("variance", "Variance", 0.0, 4.0, 1.0, 0.0));

    public NoJumpDelayModule() {
        super("no_jump_delay", "No Jump Delay", "Controls the local player's cooldown between jumps.",
                ModuleCategory.MOVEMENT);
    }

    public int rearm(int vanillaTicks) {
        int spread = (int) variance.value();
        int offset = spread == 0 ? 0 : ThreadLocalRandom.current().nextInt(spread + 1);
        return NoJumpDelayPolicy.rearm(vanillaTicks, (int) cooldown.value(), offset);
    }
}
