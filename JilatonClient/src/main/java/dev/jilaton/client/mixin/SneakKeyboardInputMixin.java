package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.SneakModule;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sneak (Vanilla mode): holds shift in the polled input. Ported from Anubis Client (MixinSneakKeyboardInput, GPL-3.0, Horcus and 4ldenz). */
@Mixin(KeyboardInput.class)
public abstract class SneakKeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void jilaton$forceSneak(CallbackInfo ci) {
        SneakModule sneak = SneakModule.active;
        if (sneak == null || !sneak.doVanilla()) return;
        ClientInput input = (ClientInput) (Object) this;
        Input keys = input.keyPresses;
        if (!keys.shift()) {
            input.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(), keys.jump(), true, keys.sprint());
        }
    }
}
