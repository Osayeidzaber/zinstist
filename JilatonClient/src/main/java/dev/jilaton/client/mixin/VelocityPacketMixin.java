package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.VelocityPolicy;
import dev.jilaton.client.module.impl.VelocityModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ThreadLocalRandom;

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
        boolean moving = client.player.input != null && client.player.input.keyPresses != null
                && (client.player.input.keyPresses.forward() || client.player.input.keyPresses.backward()
                || client.player.input.keyPresses.left() || client.player.input.keyPresses.right());
        velocity.onVelocityPacketReceived(movement, moving);
        if (velocity.responseMode().equals("Reversal")) {
            return movement;
        }
        if (velocity.responseMode().equals("Strafe")) {
            velocity.onAcceptedVelocityPacket();
            return movement;
        }
        if (!VelocityPolicy.shouldModify(client.player.onGround(), moving,
                ThreadLocalRandom.current().nextInt(100), velocity.chancePercent(),
                velocity.groundFilter(), velocity.onlyMove())) {
            return movement;
        }

        velocity.onAcceptedVelocityPacket();

        Vec3 current = client.player.getDeltaMovement();
        // Sample the randomised horizontal scale once per packet so X and Z keep the same factor.
        double horizontalScale = velocity.horizontalScale() * 100.0;
        return new Vec3(
                VelocityPolicy.applyAxis(movement.x, current.x, horizontalScale,
                        velocity.motionHorizontalScale() * 100.0),
                VelocityPolicy.applyAxis(movement.y, current.y, velocity.verticalScale() * 100.0,
                        velocity.motionVerticalScale() * 100.0),
                VelocityPolicy.applyAxis(movement.z, current.z, horizontalScale,
                        velocity.motionHorizontalScale() * 100.0)
        );
    }
}
