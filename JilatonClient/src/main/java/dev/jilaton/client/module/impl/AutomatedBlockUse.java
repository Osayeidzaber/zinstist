package dev.jilaton.client.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

/** Prevents a synthetic interaction from being mistaken for the user's macro trigger. */
public final class AutomatedBlockUse {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private AutomatedBlockUse() {
    }

    public static boolean active() {
        return DEPTH.get() > 0;
    }

    public static InteractionResult use(Minecraft client, LocalPlayer player, BlockHitResult hit) {
        if (client.gameMode == null) {
            return InteractionResult.PASS;
        }
        DEPTH.set(DEPTH.get() + 1);
        try {
            return client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        } finally {
            int remaining = DEPTH.get() - 1;
            if (remaining == 0) {
                DEPTH.remove();
            } else {
                DEPTH.set(remaining);
            }
        }
    }

    public static BlockHitResult hitOn(BlockPos pos, net.minecraft.core.Direction face, double inset) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (face.getAxis()) {
            case X -> x = pos.getX() + (face.getStepX() > 0 ? 1.0 - inset : inset);
            case Y -> y = pos.getY() + (face.getStepY() > 0 ? 1.0 - inset : inset);
            case Z -> z = pos.getZ() + (face.getStepZ() > 0 ? 1.0 - inset : inset);
        }
        return new BlockHitResult(new net.minecraft.world.phys.Vec3(x, y, z), face, pos, false);
    }
}
