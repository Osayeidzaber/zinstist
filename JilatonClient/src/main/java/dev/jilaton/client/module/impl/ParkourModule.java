package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;

public final class ParkourModule extends ClientModule {
    private final NumberSetting edgeLead;

    public ParkourModule() {
        super("parkour", "Parkour", "Jumps when your movement would carry you off an unsupported edge.",
                ModuleCategory.MOVEMENT);
        edgeLead = addSetting(new NumberSetting("edge_lead", "Edge lead", 0.2, 0.8, 0.05, 0.45));
    }

    @Override
    protected void onTick(Minecraft client) {
        Player player = client.player;
        ClientModule safeWalk = JilatonClient.moduleManager().get("safe_walk");
        if (player == null || client.level == null || client.screen != null || !player.onGround()
                || player.isInWater() || player.isInLava() || player.isPassenger()
                || player.getAbilities().flying || player.horizontalCollision
                || client.options.keyJump.isDown() || client.options.keyShift.isDown()
                || safeWalk instanceof SafeWalkModule module && module.enabled() && module.guardingEdge()) {
            return;
        }

        double forward = (client.options.keyUp.isDown() ? 1.0 : 0.0)
                - (client.options.keyDown.isDown() ? 1.0 : 0.0);
        double sideways = (client.options.keyLeft.isDown() ? 1.0 : 0.0)
                - (client.options.keyRight.isDown() ? 1.0 : 0.0);
        if (forward == 0.0 && sideways == 0.0) {
            return;
        }

        Vec2 direction = SafeWalkPolicy.aheadDirection(player.getYRot(), forward, sideways);
        if (direction.equals(Vec2.ZERO)) {
            return;
        }
        double directionX = direction.x;
        double directionZ = direction.y;

        double lead = player.getBbWidth() * 0.5 + edgeLead.value();
        double sideOffset = player.getBbWidth() * 0.24;
        double sampleY = player.getY() - 0.08;
        boolean leftHasSupport = hasSupport(client, player.getX() + directionX * lead - directionZ * sideOffset,
                sampleY, player.getZ() + directionZ * lead + directionX * sideOffset);
        boolean rightHasSupport = hasSupport(client, player.getX() + directionX * lead + directionZ * sideOffset,
                sampleY, player.getZ() + directionZ * lead - directionX * sideOffset);

        if (!leftHasSupport && !rightHasSupport) {
            player.jumpFromGround();
        }
    }

    private static boolean hasSupport(Minecraft client, double x, double y, double z) {
        BlockPos position = BlockPos.containing(x, y, z);
        BlockState state = client.level.getBlockState(position);
        return !state.getCollisionShape(client.level, position).isEmpty();
    }
}
