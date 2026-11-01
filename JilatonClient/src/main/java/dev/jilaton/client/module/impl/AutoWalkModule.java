package dev.jilaton.client.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jilaton.client.mixin.KeyMappingAccessor;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

/** Holds the walk key for you, optionally jumping into walls and stopping at drops or liquid. */
public final class AutoWalkModule extends ClientModule {
    private final ChoiceSetting direction = addSetting(new ChoiceSetting("direction", "Direction",
            List.of("Forward", "Backward"), "Forward"));
    private final BooleanSetting autoJump = addSetting(new BooleanSetting("auto_jump", "Jump into walls", true));
    private final BooleanSetting safe = addSetting(new BooleanSetting("safe", "Stop at drops and liquid", true));
    private boolean jumping;

    public AutoWalkModule() {
        super("auto_walk", "AutoWalk", "Walks for you and stops at cliffs and water if you want.", ModuleCategory.MOVEMENT);
    }

    private KeyMapping key(Minecraft client) {
        return direction.value().equals("Backward") ? client.options.keyDown : client.options.keyUp;
    }

    @Override
    protected void onDisable(Minecraft client) {
        if (client.options == null) return;
        release(client.options.keyUp);
        release(client.options.keyDown);
        if (jumping) release(client.options.keyJump);
        jumping = false;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        KeyMapping key = key(client);
        if (client.screen != null) {
            release(key);
            return;
        }
        boolean stop = safe.value() && client.player.onGround() && dangerAhead(client);
        key.setDown(!stop || physicallyDown(key));
        boolean jump = autoJump.value() && !stop && client.player.horizontalCollision && client.player.onGround();
        if (jump || jumping) {
            client.options.keyJump.setDown(jump || physicallyDown(client.options.keyJump));
            jumping = jump;
        }
    }

    /** True if the spot one step ahead is liquid or drops more than three blocks. */
    private boolean dangerAhead(Minecraft client) {
        var player = client.player;
        double yaw = Math.toRadians(player.getYRot());
        double sign = direction.value().equals("Backward") ? -1 : 1;
        double x = player.getX() - Math.sin(yaw) * 0.9 * sign, z = player.getZ() + Math.cos(yaw) * 0.9 * sign;
        BlockPos feet = BlockPos.containing(x, player.getY(), z);
        if (!client.level.getFluidState(feet).isEmpty() || !client.level.getFluidState(feet.below()).isEmpty()) return true;
        for (int d = 1; d <= 3; d++) {
            BlockPos pos = feet.below(d);
            if (!client.level.getBlockState(pos).getCollisionShape(client.level, pos).isEmpty()) return false;
        }
        return true;
    }

    private static void release(KeyMapping key) {
        key.setDown(physicallyDown(key));
    }

    static boolean physicallyDown(KeyMapping key) {
        InputConstants.Key bound = ((KeyMappingAccessor) key).jilaton$getBoundKey();
        return bound.getType() == InputConstants.Type.KEYSYM
                && InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), bound.getValue());
    }
}
