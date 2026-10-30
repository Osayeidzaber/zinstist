package dev.jilaton.client.mixin;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LoadingLogoAssetTest {
    @Test void wordmarkShipsAtTheSizeTheOverlayDrawsIt() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/assets/jilaton/textures/brand/loading-logo.png")) {
            assertNotNull(stream, "The startup overlay hardcodes this texture; it must stay on the classpath");
            BufferedImage image = ImageIO.read(stream);
            assertEquals(2048, image.getWidth());
            assertEquals(512, image.getHeight());
        }
    }
}
