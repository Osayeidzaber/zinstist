package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.PlayerMoveEvent;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/** Caps downward travel speed while falling; it does not alter fall damage or movement packets. */
public final class FastFallModule extends ClientModule {
    private final NumberSetting maximumDownwardSpeed;
    private final BooleanSetting requireSneak;

    public FastFallModule() {
        super("fast_fall", "FastFall", "Accelerates downward movement while falling, with optional sneak activation.",
                ModuleCategory.MOVEMENT);
        maximumDownwardSpeed = addSetting(new NumberSetting("downward_speed", "Downward speed",
                0.3, 2.0, 0.05, 0.9));
        requireSneak = addSetting(new BooleanSetting("require_sneak", "Require sneak to activate", true));
    }

    @Override
    protected void onPlayerMove(PlayerMoveEvent event) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || event.type() != MoverType.SELF || client.screen != null || player.onGround()
                || player.isPassenger() || player.isFallFlying() || player.getAbilities().flying
                || player.isSpectator() || player.isInWater() || player.isInLava()
                || player.hasEffect(MobEffects.SLOW_FALLING)
                || player.getDeltaMovement().y >= 0.0
                || requireSneak.value() && !client.options.keyShift.isDown()) {
            return;
        }

        Vec3 movement = event.movement();
        double acceleratedY = FastFallPolicy.accelerateDownwardMovement(movement.y,
                maximumDownwardSpeed.value(), true);
        if (acceleratedY != movement.y) {
            event.setMovement(new Vec3(movement.x, acceleratedY, movement.z));
        }
    }
}
