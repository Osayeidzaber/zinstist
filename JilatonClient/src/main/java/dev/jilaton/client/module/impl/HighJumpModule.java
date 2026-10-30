package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HighJumpModule extends ClientModule {
    private final NumberSetting motion;

    public HighJumpModule() {
        super("high_jump", "HighJump", "Sets vertical motion on a successful vanilla jump.", ModuleCategory.MOVEMENT);
        motion = addSetting(new NumberSetting("motion", "Jump motion", 0.2, 10.0, 0.05, 0.8));
    }

    public void applyJump(Player player) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled() || player == null || player != client.player || client.level == null
                || player.isPassenger() || player.isFallFlying() || player.getAbilities().flying
                || player.isSpectator() || player.isInWater() || player.isInLava()) {
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        double jumpMotion = HighJumpPolicy.motionAfterJump(velocity.y, motion.value());
        if (jumpMotion != velocity.y) {
            player.setDeltaMovement(velocity.x, jumpMotion, velocity.z);
        }
    }

    public NumberSetting motion() {
        return motion;
    }
}
