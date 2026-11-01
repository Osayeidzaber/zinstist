package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Finishes breaking blocks before the full break time ({@code SpeedMineMixin} scales the client break progress
 * and can skip the delay between blocks). Shovel and axe blocks other than gravel, and spawners, are left alone,
 * as in Anubis. Servers that check break timing may reject early breaks. Ported from Anubis Client (SpeedMineModule, GPL-3.0, Horcus and 4ldenz).
 */
public final class SpeedMineModule extends ClientModule {
    private static volatile SpeedMineModule active;
    private final NumberSetting breakAt = addSetting(new NumberSetting("break_at", "Break At", 0.3, 1.0, 0.01, 0.7));
    private final BooleanSetting skipDelay = addSetting(new BooleanSetting("skip_delay", "Skip Break Delay", true));
    private final BooleanSetting keepInstantDelay = addSetting(new BooleanSetting("keep_instant_delay", "Keep Instant Delay", true));

    public SpeedMineModule() {
        super("speed_mine", "SpeedMine", "Finishes breaking blocks before the full break time.", ModuleCategory.PLAYER);
    }

    @Override
    protected void onEnable(Minecraft client) {
        active = this;
    }

    @Override
    protected void onDisable(Minecraft client) {
        active = null;
    }

    public static float scaleProgress(BlockState state, float progress) {
        SpeedMineModule module = active;
        return module != null && affects(state) ? progress / (float) module.breakAt.value() : progress;
    }

    public static boolean dropsDelay(BlockState state, float progress) {
        SpeedMineModule module = active;
        return module != null && module.skipDelay.value() && affects(state) && (!module.keepInstantDelay.value() || progress < 1.0F);
    }

    static boolean affects(BlockState state) {
        if (state.isAir() || state.is(Blocks.SPAWNER)) return false;
        return state.is(Blocks.GRAVEL) || !state.is(BlockTags.MINEABLE_WITH_SHOVEL) && !state.is(BlockTags.MINEABLE_WITH_AXE);
    }
}
