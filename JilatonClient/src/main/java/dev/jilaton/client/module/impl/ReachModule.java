package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;

public final class ReachModule extends ClientModule {
    private final NumberSetting entityBonus;
    private final NumberSetting blockBonus;

    public ReachModule() {
        super("reach", "Reach", "Adjusts local entity and block interaction ranges.", ModuleCategory.PLAYER);
        // Slider ceilings stop where the server stops listening: 3.0 + 3.0 = 6.0, 4.5 + 1.0 = 5.5.
        entityBonus = addSetting(new NumberSetting("entity_bonus", "Entity range increase", 0.0, 3.0, 0.1, 1.0));
        blockBonus = addSetting(new NumberSetting("block_bonus", "Block range increase", 0.0, 1.0, 0.5, 0.5));
    }

    public NumberSetting entityBonus() {
        return entityBonus;
    }

    public NumberSetting blockBonus() {
        return blockBonus;
    }
}
