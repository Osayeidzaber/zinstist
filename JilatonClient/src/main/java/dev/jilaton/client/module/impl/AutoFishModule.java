package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;

/**
 * Reels in when a fish bites and casts again. A bite is the server's bobber splash sound played right at your
 * own hook, the same cue you listen for by ear, so it works on any server without reading hidden entity data.
 */
public final class AutoFishModule extends ClientModule {
    private static final double BITE_RADIUS_SQR = 1.6 * 1.6;

    private final NumberSetting reelDelay = addSetting(new NumberSetting("reel_delay", "Reel delay (ticks)", 0, 10, 1, 2));
    private final NumberSetting recastDelay = addSetting(new NumberSetting("recast_delay", "Recast delay (ticks)", 4, 40, 1, 14));
    private final BooleanSetting recast = addSetting(new BooleanSetting("recast", "Cast again", true));
    private final BooleanSetting rodSwap = addSetting(new BooleanSetting("rod_swap", "Swap to another rod when one breaks", true));
    private int reelIn = -1;
    private int castIn = -1;

    public AutoFishModule() {
        super("auto_fish", "AutoFish", "Reels in on a bite and casts again.", ModuleCategory.PLAYER);
    }

    /** Sound packet hook. */
    public static void onSound(SoundEvent sound, double x, double y, double z) {
        if (sound != SoundEvents.FISHING_BOBBER_SPLASH) return;
        if (JilatonClient.moduleManager() != null
                && JilatonClient.moduleManager().get("auto_fish") instanceof AutoFishModule module && module.enabled()) {
            module.splash(x, y, z);
        }
    }

    private void splash(double x, double y, double z) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || reelIn >= 0) return;
        FishingHook hook = player.fishing;
        if (hook == null || hook.distanceToSqr(x, y, z) > BITE_RADIUS_SQR) return;
        reelIn = (int) Math.round(reelDelay.value());
    }

    @Override
    protected void onDisable(Minecraft client) {
        reelIn = -1;
        castIn = -1;
    }

    @Override
    protected void onTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;
        if (reelIn >= 0 && reelIn-- == 0) {
            InteractionHand hand = rodHand(player);
            if (hand != null && player.fishing != null) {
                client.gameMode.useItem(player, hand);
                if (recast.value()) castIn = (int) Math.round(recastDelay.value());
            }
            reelIn = -1;
        }
        if (castIn >= 0 && castIn-- == 0) {
            castIn = -1;
            if (rodHand(player) == null && rodSwap.value()) swapToRod(player);
            InteractionHand hand = rodHand(player);
            if (hand != null && player.fishing == null && client.screen == null) client.gameMode.useItem(player, hand);
        }
    }

    private static InteractionHand rodHand(LocalPlayer player) {
        if (player.getMainHandItem().is(Items.FISHING_ROD)) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().is(Items.FISHING_ROD)) return InteractionHand.OFF_HAND;
        return null;
    }

    private static void swapToRod(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(Items.FISHING_ROD)) {
                player.getInventory().setSelectedSlot(i);
                return;
            }
        }
    }
}
