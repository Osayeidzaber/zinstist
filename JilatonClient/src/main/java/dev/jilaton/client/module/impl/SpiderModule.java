package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Allows you to climb walls like a spider. Ported from Anubis Client (SpiderModule, GPL-3.0, Horcus and 4ldenz). */
public final class SpiderModule extends ClientModule {
    private static final double CLIMB_VELOCITY_THRESHOLD = 0.2;
    private final NumberSetting climbSpeed = addSetting(new NumberSetting("climb_speed", "Climb Speed", 0, 10, 0.1, 0.2));

    public SpiderModule() {
        super("spider", "Spider", "Allows you to climb walls like a spider.", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player != null && player.horizontalCollision) {
            Vec3 velocity = player.getDeltaMovement();
            if (!(velocity.y >= CLIMB_VELOCITY_THRESHOLD)) player.setDeltaMovement(velocity.x, climbSpeed.value(), velocity.z);
        }
    }
}
