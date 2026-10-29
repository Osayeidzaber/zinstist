/*
 * Adapted from LiquidBounce's ModuleNoSlow and MixinLocalPlayer.
 * Copyright (c) 2015 - 2026 CCBlueX. LiquidBounce is licensed under GPL-3.0-or-later.
 * Source: https://github.com/CCBlueX/LiquidBounce/tree/e033f5aaf05be802dd2ca51782f255119d7d5654
 */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.world.item.ItemUseAnimation;

public final class NoSlowModule extends ClientModule {
    private final BooleanSetting removeItemSlowdown;
    private final BooleanSetting allowSprintWhileUsing;
    private final NumberSetting itemUseScale;
    private final NumberSetting consumeScale;
    private final NumberSetting blockScale;
    private final NumberSetting bowScale;
    private final NumberSetting spearScale;
    private final NumberSetting crossbowScale;
    private final NumberSetting otherScale;

    public NoSlowModule() {
        super("no_slow", "NoSlow", "Configures item-use movement slowdown and sprint eligibility.",
                ModuleCategory.MOVEMENT);
        removeItemSlowdown = addSetting(new BooleanSetting("remove_item_slowdown", "Remove item slowdown", true));
        allowSprintWhileUsing = addSetting(new BooleanSetting("allow_sprint", "Allow sprint while using", true));
        itemUseScale = addSetting(new NumberSetting("item_use_scale", "Vanilla slowdown scale", 0.0, 2.0,
                0.05, 1.0));
        consumeScale = actionScale("consume_scale", "Eating and drinking scale");
        blockScale = actionScale("block_scale", "Blocking scale");
        bowScale = actionScale("bow_scale", "Bow scale");
        spearScale = actionScale("spear_scale", "Spear and trident scale");
        crossbowScale = actionScale("crossbow_scale", "Crossbow scale");
        otherScale = actionScale("other_scale", "Other use scale");
    }

    private NumberSetting actionScale(String id, String name) {
        return addSetting(new NumberSetting(id, name, 0.0, 2.0, 0.05, 1.0));
    }

    public boolean removeItemSlowdown() {
        return removeItemSlowdown.value();
    }

    public boolean allowSprintWhileUsing() {
        return allowSprintWhileUsing.value();
    }

    public double itemUseScale() {
        return itemUseScale.value();
    }

    public double scaleFor(ItemUseAnimation animation) {
        return NoSlowPolicy.actionScale(animation, consumeScale.value(), blockScale.value(), bowScale.value(),
                spearScale.value(), crossbowScale.value(), otherScale.value());
    }
}
