package dev.jilaton.client.mixin;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets Jilaton build a {@link Font} over vanilla's glyph provider with Inter as its default face. */
@Mixin(Font.class)
public interface FontProviderAccess {
    @Accessor("provider")
    Font.Provider jilaton$provider();
}
