package dev.jilaton.client.mixin;

import com.mojang.blaze3d.platform.IconSet;
import dev.jilaton.client.gui.JilatonWindowIcon;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.List;

/** Swaps vanilla's taskbar/Alt-Tab icons for the Jilaton mark while keeping vanilla's GLFW upload path. */
@Mixin(IconSet.class)
public abstract class IconSetMixin {
    @Inject(method = "getStandardIcons", at = @At("HEAD"), cancellable = true)
    private void jilaton$brandWindowIcon(PackResources resources, CallbackInfoReturnable<List<IoSupplier<InputStream>>> cir) {
        cir.setReturnValue(JilatonWindowIcon.suppliers());
    }
}
