package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.NickHiderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BookViewScreen.BookAccess.class)
public abstract class NickHiderBookPageMixin {
    @Inject(method = "getPage", at = @At("RETURN"), cancellable = true)
    private void jilaton$hideOwnName(int page, CallbackInfoReturnable<Component> callback) {
        NickHiderModule module = NickHiderModule.active();
        Component text = callback.getReturnValue();
        if (module == null || text == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        callback.setReturnValue(module.displayName(text, NickHiderModule.localName(client)));
    }
}
