package dev.jilaton.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Brands the window title, which otherwise reads "Minecraft 1.21.11". */
@Mixin(Minecraft.class)
public abstract class WindowTitleMixin {
    private static final String VANILLA_TITLE = "Minecraft";
    private static final String BRAND_TITLE = "Jilaton Client";

    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
    private void jilaton$brandTitle(CallbackInfoReturnable<String> cir) {
        String title = cir.getReturnValue();
        if (title != null && title.startsWith(VANILLA_TITLE)) {
            cir.setReturnValue(BRAND_TITLE + title.substring(VANILLA_TITLE.length()));
        }
    }
}
