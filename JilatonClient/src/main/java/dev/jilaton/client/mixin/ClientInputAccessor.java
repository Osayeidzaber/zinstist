package dev.jilaton.client.mixin;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.client.player.ClientInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface ClientInputAccessor {
    @Accessor("keyPresses")
    void jilaton$setKeyPresses(Input input);

    @Accessor("moveVector")
    void jilaton$setMoveVector(Vec2 moveVector);
}
