package dev.jilaton.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * The loading-screen wordmark, read straight from the mod's own classpath.
 * Vanilla's startup logo does the same with the vanilla pack so the overlay can draw before the
 * first resource reload (which is what the overlay is covering) has finished.
 */
public final class JilatonLoadingLogo extends ReloadableTexture {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("jilaton", "textures/brand/loading-logo.png");
    private static final String PATH = "/assets/jilaton/textures/brand/loading-logo.png";

    public JilatonLoadingLogo() {
        super(ID);
    }

    @Override
    public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
        try (InputStream stream = JilatonLoadingLogo.class.getResourceAsStream(PATH)) {
            if (stream == null) throw new IOException("Missing " + PATH);
            NativeImage image = NativeImage.read(stream);
            return new TextureContents(image, new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0.0F));
        }
    }
}
