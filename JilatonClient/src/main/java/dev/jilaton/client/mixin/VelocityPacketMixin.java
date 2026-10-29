package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.VelocityModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public abstract class VelocityPacketMixin {
    @Redirect(
            method = "handleSetEntityMotion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ClientboundSetEntityMotionPacket;getMovement()Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 jilaton$scaleLocalPlayerVelocity(ClientboundSetEntityMotionPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || packet.getId() != client.player.getId()) {
            return packet.getMovement();
        }

        ClientModule module = JilatonClient.moduleManager().get("velocity");
        if (!(module instanceof VelocityModule velocity) || !velocity.enabled()) {
            return packet.getMovement();
        }

        Vec3 movement = packet.getMovement();
        return new Vec3(
                movement.x * velocity.horizontalScale(),
                movement.y * velocity.verticalScale(),
                movement.z * velocity.horizontalScale()
        );
    }
}
