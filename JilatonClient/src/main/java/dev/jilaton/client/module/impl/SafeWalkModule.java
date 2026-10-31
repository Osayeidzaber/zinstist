package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.MovementInputEvent;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;

public final class SafeWalkModule extends ClientModule {
    private final NumberSetting edgeDistance;
    private final NumberSetting keepTicks;
    private volatile boolean guardingEdge;
    private int remainingKeepTicks;

    public SafeWalkModule() {
        super("safe_walk", "SafeWalk", "Uses vanilla sneak input before your next step leaves block support.",
                ModuleCategory.MOVEMENT);
        edgeDistance = addSetting(new NumberSetting("edge_distance", "Edge distance", 0.05, 0.6, 0.05, 0.3));
        keepTicks = addSetting(new NumberSetting("keep_ticks", "Keep sneak ticks", 0.0, 5.0, 1.0, 1.0));
    }

    @Override
    protected void onMovementInput(MovementInputEvent event) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        Input input = event.input();
        if (player == null || client.level == null || client.screen != null) {
            clearGuard();
            return;
        }

        boolean moving = hasMovementInput(input) || hasMovementVector(event.moveVectorOverride());
        boolean exemptState = player.getAbilities().flying || player.isFallFlying() || player.isCreative()
                || player.isSpectator() || player.isPassenger() || player.isInWater() || player.isInLava();
        if (!player.onGround() || exemptState || input.shift()) {
            clearGuard();
            return;
        }
        boolean unsupportedAhead = moving && !hasSupportAhead(client, player, input, event.moveVectorOverride());
        boolean edge = SafeWalkPolicy.shouldSneak(player.onGround(), input.shift(), moving,
                exemptState, unsupportedAhead);

        if (edge) {
            remainingKeepTicks = (int) keepTicks.value();
            guardingEdge = true;
        } else if (remainingKeepTicks > 0 && player.onGround() && !exemptState && !input.shift()) {
            remainingKeepTicks--;
            edge = true;
            guardingEdge = true;
        } else {
            remainingKeepTicks = 0;
            guardingEdge = false;
        }

        if (edge && !input.shift()) {
            event.setInput(new Input(input.forward(), input.backward(), input.left(), input.right(),
                    input.jump(), true, input.sprint()));
        }
    }

    @Override
    protected void onDisable(Minecraft client) {
        clearGuard();
    }

    public boolean guardingEdge() {
        return guardingEdge;
    }

    private void clearGuard() {
        guardingEdge = false;
        remainingKeepTicks = 0;
    }

    private static boolean hasMovementInput(Input input) {
        return input.forward() || input.backward() || input.left() || input.right();
    }

    private static boolean hasMovementVector(Vec2 vector) {
        return vector != null && (vector.x * vector.x + vector.y * vector.y) > 1.0E-6F;
    }

    private boolean hasSupportAhead(Minecraft client, Player player, Input input, Vec2 override) {
        double forward;
        double sideways;
        if (hasMovementVector(override)) {
            forward = override.y;
            sideways = override.x;
        } else {
            forward = (input.forward() ? 1.0 : 0.0) - (input.backward() ? 1.0 : 0.0);
            sideways = (input.left() ? 1.0 : 0.0) - (input.right() ? 1.0 : 0.0);
        }
        Vec2 direction = SafeWalkPolicy.aheadDirection(player.getYRot(), forward, sideways);
        if (direction.equals(Vec2.ZERO)) {
            return true;
        }

        double speed = Math.hypot(player.getDeltaMovement().x, player.getDeltaMovement().z);
        double lead = Math.min(edgeDistance.value(), Math.max(0.08, speed));
        double centerX = player.getX() + direction.x * lead;
        double centerZ = player.getZ() + direction.y * lead;
        double halfWidth = Math.max(0.05, player.getBbWidth() * 0.45);
        double footY = player.getY() - 0.05;

        return hasSupport(client, centerX - halfWidth, footY, centerZ - halfWidth)
                && hasSupport(client, centerX - halfWidth, footY, centerZ + halfWidth)
                && hasSupport(client, centerX + halfWidth, footY, centerZ - halfWidth)
                && hasSupport(client, centerX + halfWidth, footY, centerZ + halfWidth);
    }

    private static boolean hasSupport(Minecraft client, double x, double y, double z) {
        BlockPos position = BlockPos.containing(x, y, z);
        BlockState state = client.level.getBlockState(position);
        return !state.getCollisionShape(client.level, position).isEmpty();
    }
}
