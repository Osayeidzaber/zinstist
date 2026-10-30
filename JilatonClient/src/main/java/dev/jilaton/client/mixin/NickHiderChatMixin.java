package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.NickHiderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public abstract class NickHiderChatMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), argsOnly = true)
    private Component jilaton$hideOwnName(Component message) {
        NickHiderModule module = NickHiderModule.active();
        if (module == null) {
            return message;
        }
        Minecraft client = Minecraft.getInstance();
        return module.displayName(message, NickHiderModule.localName(client));
    }

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true)
    private Component jilaton$hideOwnNameSigned(Component message) {
        return jilaton$hideOwnName(message);
    }
}
