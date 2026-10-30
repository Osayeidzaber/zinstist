package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class NoFallModule extends ClientModule {
    private final BooleanSetting useSafeFallDistance;
    private final BooleanSetting resetFallDistance;
    private final NumberSetting triggerDistance;
    private volatile boolean spoofGroundForNextMovement;

    public NoFallModule() {
        super("no_fall", "NoFall", "Marks falling movement packets grounded after the safe-fall threshold.",
                ModuleCategory.MOVEMENT);
        useSafeFallDistance = addSetting(new BooleanSetting("use_safe_distance", "Use safe fall distance", true));
        triggerDistance = addSetting(new NumberSetting("fall_distance", "Trigger distance", 0.0, 10.0, 0.5, 3.0));
        resetFallDistance = addSetting(new BooleanSetting("reset_fall_distance", "Reset local fall distance", true));
    }

    @Override
    protected void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null || client.screen != null) {
            spoofGroundForNextMovement = false;
            return;
        }

        boolean exemptState = player.getAbilities().flying || player.isFallFlying() || player.isCreative()
                || player.isSpectator() || player.isPassenger() || player.isInWater() || player.isInLava();
        double threshold = useSafeFallDistance.value()
                ? player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE)
                : triggerDistance.value();
        spoofGroundForNextMovement = NoFallPolicy.shouldSpoof(player.onGround(),
                player.getDeltaMovement().y < 0.0, exemptState, player.fallDistance, threshold);
        if (spoofGroundForNextMovement && resetFallDistance.value()) {
            player.resetFallDistance();
        }
    }

    @Override
    protected void onDisable(Minecraft client) {
        spoofGroundForNextMovement = false;
    }

    public boolean shouldSpoofGround() {
        return spoofGroundForNextMovement;
    }
}
