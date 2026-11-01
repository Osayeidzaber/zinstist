package dev.jilaton.client.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Parent of the vanilla server list, read by {@link ScreenRedirectMixin} when it swaps in the Jilaton screen. */
@Mixin(JoinMultiplayerScreen.class)
public interface ServerListRedirectMixin {
    @Accessor("lastScreen")
    Screen jilaton$lastScreen();
}
