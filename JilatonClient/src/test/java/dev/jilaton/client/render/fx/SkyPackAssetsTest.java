package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The bundled Dramatic Skys layers must ship and must keep the 3×2 cube-face atlas aspect the sky samples. */
class SkyPackAssetsTest {
    @Test
    void everyLayerShipsInTheJarWithCubeAtlasAspect() throws Exception {
        for (String layer : SkyPackTextures.LAYERS) {
            String path = "assets/jilaton/textures/sky/" + layer + ".png";
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(in, "Missing bundled sky layer " + path);
                BufferedImage image = ImageIO.read(in);
                assertNotNull(image, "Cannot decode " + path);
                // A 3:2 atlas of six square cube faces in a 3x2 grid; the shipped copies are 2048x1365 and
                // 1024x683, but the invariant is the atlas shape, not today's pixel count.
                assertTrue(image.getWidth() >= 1024, path + " width " + image.getWidth());
                assertEquals(1.5, image.getWidth() / (double) image.getHeight(), 0.002, path + " aspect");
            }
        }
    }

    @Test
    void layerSetIsUniqueAndCoversTheAtlas() {
        Set<String> unique = new HashSet<>();
        for (String layer : SkyPackTextures.LAYERS) assertTrue(unique.add(layer), "Duplicate layer " + layer);
        assertEquals(7, unique.size());
    }
}
