package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.JilatonPauseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Esc opens the Jilaton pause menu. Vanilla's stays reachable through its "More options" row, and the F3+Esc
 * pause (no menu) is left alone.
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenRedirectMixin {
    @Shadow public abstract boolean showsPauseMenu();

    @Unique private boolean jilaton$vanilla;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void jilaton$redirect(CallbackInfo callback) {
        if (JilatonPauseScreen.vanillaRequested) {
            JilatonPauseScreen.vanillaRequested = false;
            jilaton$vanilla = true;
        }
        if (jilaton$vanilla || !showsPauseMenu()) return;
        Minecraft.getInstance().setScreen(new JilatonPauseScreen());
        callback.cancel();
    }
}
