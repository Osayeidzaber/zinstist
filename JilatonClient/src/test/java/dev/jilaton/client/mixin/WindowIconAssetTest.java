package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.JilatonWindowIcon;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WindowIconAssetTest {
    @Test void everyIconTheWindowUploadsShipsAtItsAdvertisedSize() throws Exception {
        for (int size : JilatonWindowIcon.SIZES) {
            String path = JilatonWindowIcon.path(size);
            try (InputStream stream = getClass().getResourceAsStream(path)) {
                assertNotNull(stream, "Missing window icon " + path);
                BufferedImage image = ImageIO.read(stream);
                assertEquals(size, image.getWidth(), path + " width");
                assertEquals(size, image.getHeight(), path + " height");
            }
        }
    }
}
