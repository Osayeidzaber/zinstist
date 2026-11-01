package dev.jilaton.client.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Parent of the vanilla world list, read by {@link ScreenRedirectMixin} when it swaps in the Jilaton screen. */
@Mixin(SelectWorldScreen.class)
public interface WorldListRedirectMixin {
    @Accessor("lastScreen")
    Screen jilaton$lastScreen();
}
