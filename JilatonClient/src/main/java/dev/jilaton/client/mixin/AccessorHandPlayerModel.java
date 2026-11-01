package dev.jilaton.client.mixin;

import net.minecraft.client.model.player.PlayerModel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(PlayerModel.class)
public interface AccessorHandPlayerModel {
   @Accessor("slim")
   boolean jilaton$handSlim();
}
