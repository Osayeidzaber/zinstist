package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.FreecamModule;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FreecamCameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow private boolean detached;

    @Inject(method = "setup", at = @At("RETURN"))
    private void jilaton$detachedCamera(Level level, Entity entity, boolean detached, boolean mirrored, float partial, CallbackInfo callback) {
        if (JilatonClient.moduleManager().get("freecam") instanceof FreecamModule camera && camera.live(Minecraft.getInstance())) {
            var position = camera.cameraPosition(partial);
            setPosition(position.x, position.y, position.z);
            setRotation(camera.yaw(), camera.pitch());
            this.detached = camera.showBody();
        }
    }
}
