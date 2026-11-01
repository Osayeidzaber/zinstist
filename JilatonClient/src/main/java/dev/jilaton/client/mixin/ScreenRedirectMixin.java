package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.JilatonServersScreen;
import dev.jilaton.client.gui.JilatonWorldsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Every path into the vanilla server or world list (safety notice, "Back to server list" after a disconnect, mods)
 * opens the Jilaton screen instead. The swap happens before the vanilla screen is shown, so it is never
 * initialised, ticked or removed half-built (redirecting from its init crashed in removed()/tick()).
 */
@Mixin(Minecraft.class)
public abstract class ScreenRedirectMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen jilaton$redirect(Screen screen) {
        if (screen != null && screen.getClass() == JoinMultiplayerScreen.class) {
            return new JilatonServersScreen(((ServerListRedirectMixin) screen).jilaton$lastScreen());
        }
        if (screen != null && screen.getClass() == SelectWorldScreen.class) {
            return new JilatonWorldsScreen(((WorldListRedirectMixin) screen).jilaton$lastScreen());
        }
        return screen;
    }
}
