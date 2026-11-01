package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Switches to the best hotbar tool while you mine a block and, optionally, back when you stop. */
public final class AutoToolModule extends ClientModule {
    private final BooleanSetting switchBack = addSetting(new BooleanSetting("switch_back", "Switch back after mining", true));
    private final NumberSetting reserve = addSetting(new NumberSetting("reserve", "Keep durability", 0, 50, 1, 5));
    private final BooleanSetting sneakBypass = addSetting(new BooleanSetting("sneak_bypass", "Not while sneaking", false));
    private int previous = -1;
    private int owned = -1;

    public AutoToolModule() {
        super("auto_tool", "AutoTool", "Picks the fastest hotbar tool for the block you are mining.", ModuleCategory.PLAYER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        restore(client);
    }

    @Override
    protected void onTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.screen != null) return;
        boolean held = client.options.keyAttack.isDown();
        boolean mining = held && client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK;
        if (!mining || (sneakBypass.value() && player.isShiftKeyDown()) || player.isCreative()) {
            if (!held) restore(client);
            return;
        }
        BlockState state = client.level.getBlockState(((BlockHitResult) client.hitResult).getBlockPos());
        if (state.isAir()) return;
        float[] speed = new float[9];
        boolean[] correct = new boolean[9];
        int[] left = new int[9];
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            speed[i] = stack.getDestroySpeed(state);
            correct[i] = stack.isCorrectToolForDrops(state);
            left[i] = stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : Integer.MAX_VALUE;
        }
        int current = player.getInventory().getSelectedSlot();
        int best = AutoToolPolicy.best(speed, correct, left, current, (int) Math.round(reserve.value()),
                state.requiresCorrectToolForDrops());
        if (best < 0) return;
        if (previous < 0) previous = current;
        owned = best;
        player.getInventory().setSelectedSlot(best);
    }

    private void restore(Minecraft client) {
        if (switchBack.value() && client.player != null
                && HotbarSlotRestore.shouldRestore(client.player.getInventory().getSelectedSlot(), owned, previous)) {
            client.player.getInventory().setSelectedSlot(previous);
        }
        previous = -1;
        owned = -1;
    }
}
