package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.MovementInputEvent;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void jilaton$dispatchMovementInput(CallbackInfo callbackInfo) {
        ClientInputAccessor inputAccessor = (ClientInputAccessor) this;
        Input original = ((net.minecraft.client.player.ClientInput) (Object) this).keyPresses;
        MovementInputEvent event = JilatonClient.moduleManager().dispatchMovementInput(original);
        if (event == null) {
            return;
        }

        Input adjusted = event.input();
        if (adjusted != original) {
            inputAccessor.jilaton$setKeyPresses(adjusted);
        }
        if (event.moveVectorOverride() != null) {
            inputAccessor.jilaton$setMoveVector(event.moveVectorOverride());
        } else if (adjusted != original) {
            float forward = impulse(adjusted.forward(), adjusted.backward());
            float sideways = impulse(adjusted.left(), adjusted.right());
            inputAccessor.jilaton$setMoveVector(new Vec2(sideways, forward).normalized());
        }
    }

    private static float impulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }
}
