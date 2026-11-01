package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.MovementInputEvent;
import dev.jilaton.client.module.PlayerMoveEvent;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Anubis AutoWalk behavior adapted to Jilaton's movement event, without mutating physical key state.
 * Horcus / 4ldenz, GPL-3.0-only. See ATTRIBUTION.md. */
public final class AutoWalkModule extends ClientModule {
    private final ChoiceSetting direction = addSetting(new ChoiceSetting("direction", "Direction",
            List.of("Forward", "Backward", "Left", "Right"), "Forward"));
    private final BooleanSetting autoJump = addSetting(new BooleanSetting("auto_jump", "Jump into walls", true));
    private final BooleanSetting safe = addSetting(new BooleanSetting("safe", "Stop at drops and liquid", true));
    private final BooleanSetting disableOnInput = addSetting(new BooleanSetting("disable_on_input", "Disable on movement input", false));
    private final BooleanSetting disableOnY = addSetting(new BooleanSetting("disable_on_y", "Disable on height change", false));
    private final BooleanSetting waitForChunks = addSetting(new BooleanSetting("wait_for_chunks", "Wait for loaded chunks", true));
    private Object player, world;
    private double previousY;

    public AutoWalkModule() {
        super("auto_walk", "AutoWalk", "Walks for you and stops at cliffs and water if you want.", ModuleCategory.MOVEMENT);
    }

    @Override public boolean persistEnabled() { return false; }

    @Override protected void onEnable(Minecraft client) { capture(client); }
    @Override protected void onDisable(Minecraft client) { player = world = null; }

    private void capture(Minecraft client) {
        player = client == null ? null : client.player;
        world = client == null ? null : client.level;
        previousY = player == null ? 0 : client.player.getY();
    }

    @Override protected void onTick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            if (player != null) setEnabled(false);
            return;
        }
        if (player == null) capture(client);
        if (player != client.player || world != client.level || !client.player.isAlive()
                || (disableOnY.value() && client.player.getY() != previousY)) {
            setEnabled(false);
            return;
        }
        previousY = client.player.getY();
    }

    @Override protected void onMovementInput(MovementInputEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (!canWalk(client)) return;
        Input input = event.input();
        if (disableOnInput.value() && manualMovement(input)) { setEnabled(false); return; }
        boolean stop = safe.value() && client.player.onGround() && dangerAhead(client);
        boolean jump = autoJump.value() && !stop && client.player.horizontalCollision && client.player.onGround();
        event.setInput(walking(input, direction.value(), stop, jump));
    }

    private boolean canWalk(Minecraft client) {
        if (client == null || client.player == null || client.level == null || client.screen != null
                || !client.isWindowActive() || !client.player.isAlive() || client.player.isPassenger()) return false;
        ClientModule camera = JilatonClient.moduleManager().get("freecam");
        return camera == null || !camera.enabled();
    }

    static boolean manualMovement(Input input) {
        return input.forward() || input.backward() || input.left() || input.right() || input.jump() || input.shift();
    }

    static Input walking(Input input, String direction, boolean stop, boolean jump) {
        return new Input(input.forward() || (!stop && direction.equals("Forward")),
                input.backward() || (!stop && direction.equals("Backward")),
                input.left() || (!stop && direction.equals("Left")),
                input.right() || (!stop && direction.equals("Right")),
                input.jump() || jump, input.shift(), input.sprint());
    }

    @Override protected void onPlayerMove(PlayerMoveEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (!waitForChunks.value() || event.type() != MoverType.SELF || !canWalk(client)) return;
        Vec3 movement = event.movement();
        int chunkX = ((int) Math.floor(client.player.getX() + movement.x * 2)) >> 4;
        int chunkZ = ((int) Math.floor(client.player.getZ() + movement.z * 2)) >> 4;
        if (!client.level.getChunkSource().hasChunk(chunkX, chunkZ)) {
            event.setMovement(new Vec3(0, movement.y, 0));
        }
    }

    /** True if the spot one step ahead is liquid or drops more than three blocks. */
    private boolean dangerAhead(Minecraft client) {
        var player = client.player;
        double yaw = Math.toRadians(player.getYRot() + switch (direction.value()) {
            case "Backward" -> 180;
            case "Left" -> -90;
            case "Right" -> 90;
            default -> 0;
        });
        double x = player.getX() - Math.sin(yaw) * 0.9, z = player.getZ() + Math.cos(yaw) * 0.9;
        BlockPos feet = BlockPos.containing(x, player.getY(), z);
        if (!client.level.getFluidState(feet).isEmpty() || !client.level.getFluidState(feet.below()).isEmpty()) return true;
        for (int d = 1; d <= 3; d++) {
            BlockPos pos = feet.below(d);
            if (!client.level.getBlockState(pos).getCollisionShape(client.level, pos).isEmpty()) return false;
        }
        return true;
    }

}
