package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.FreecamModule;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public abstract class FreecamCrosshairMixin {
    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
    private boolean jilaton$freecamCrosshair(CameraType perspective) {
        return perspective.isFirstPerson() || (JilatonClient.moduleManager().get("freecam") instanceof FreecamModule camera
                && camera.live(Minecraft.getInstance()) && camera.showBody());
    }
}
