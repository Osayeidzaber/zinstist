package dev.jilaton.client.mixin;

import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

/**
 * The narrow session hook the account manager uses (development/GUI_REDESIGN.md §11): it swaps the user and the
 * profile behind {@code getGameProfile()} together, and only {@code SessionSwitcher} calls it, from the title screen.
 */
@Mixin(Minecraft.class)
public interface MinecraftSessionAccessor {
    @Mutable
    @Accessor("user")
    void jilaton$setUser(User user);

    @Mutable
    @Accessor("profileFuture")
    void jilaton$setProfileFuture(CompletableFuture<ProfileResult> future);

    @Accessor("profileFuture")
    CompletableFuture<ProfileResult> jilaton$profileFuture();
}
