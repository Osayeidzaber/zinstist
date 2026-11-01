package dev.jilaton.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.jilaton.client.module.impl.SneakModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Sneak (Packet mode): the input sent to the server reports shift held. Ported from Anubis Client (MixinSneakLocalPlayer, GPL-3.0, Horcus and 4ldenz). */
@Mixin(LocalPlayer.class)
public abstract class SneakLocalPlayerMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/player/ClientInput;keyPresses:Lnet/minecraft/world/entity/player/Input;", opcode = 180),
            require = 1)
    private Input jilaton$packetSneak(Input original) {
        SneakModule sneak = SneakModule.active;
        return sneak != null && !original.shift() && sneak.doPacket()
                ? new Input(original.forward(), original.backward(), original.left(), original.right(), original.jump(), true, original.sprint())
                : original;
    }
}
