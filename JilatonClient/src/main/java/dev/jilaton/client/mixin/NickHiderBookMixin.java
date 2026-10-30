package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.NickHiderModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookViewScreen.class)
public abstract class NickHiderBookMixin {
    @Shadow private int cachedPage;
    @Unique private static String jilaton$lastSignature;

    @Inject(method = "render", at = @At("HEAD"))
    private void jilaton$refreshWhenSettingChanges(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                                   CallbackInfo callback) {
        NickHiderModule module = NickHiderModule.active();
        String signature = module == null ? "off" : "on:" + module.replacement();
        if (!signature.equals(jilaton$lastSignature)) {
            jilaton$lastSignature = signature;
            cachedPage = -1;
        }
    }
}
