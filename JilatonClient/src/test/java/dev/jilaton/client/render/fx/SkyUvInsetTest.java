package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyUvInsetTest {
    @Test
    void theInsetIsHalfATexelOfTheActualImage() {
        assertArrayEquals(new float[]{0.5F / 2048F, 0.5F / 1365F}, FxFrame.halfTexel(2048, 1365), 0);
        assertArrayEquals(new float[]{0.5F / 1024F, 0.5F / 683F}, FxFrame.halfTexel(1024, 683), 0);
        assertArrayEquals(new float[]{0.5F / 1536F, 0.5F / 1024F}, FxFrame.halfTexel(1536, 1024), 0);
        assertArrayEquals(new float[]{0.5F / 3072F, 0.5F / 2048F}, FxFrame.halfTexel(3072, 2048), 0);
        assertArrayEquals(new float[]{0.5F / 4096F, 0.5F / 2730F}, FxFrame.halfTexel(4096, 2730), 0);
        // Unknown sizes keep the conservative fallback rather than dividing by zero.
        assertArrayEquals(new float[]{0.5F / 1024F, 0.5F / 683F}, FxFrame.halfTexel(0, 0), 0);
    }

    @Test
    void eachCellEdgeMovesInwardByExactlyTheTexelInset() {
        float[] cell = {1 / 3F, 0.5F, 0, 0.5F, 0, 1, 1 / 3F, 1};
        float u = 0.5F / 2048F, v = 0.5F / 1365F;
        float[] inset = FxFrame.insetUv(cell, u, v);
        assertEquals(1 / 3F - u, inset[0], 1e-7);
        assertEquals(0.5F + v, inset[1], 1e-7);
        assertEquals(u, inset[2], 1e-7);
        assertEquals(1 - v, inset[5], 1e-7);
    }

    @Test
    void bundledSkyLayersReportExactPngDimensionsAndDistinctInsets() throws Exception {
        for (String layer : SkyPackTextures.LAYERS) {
            String path = "assets/jilaton/textures/sky/" + layer + ".png";
            int[] headerDims = FxFrame.readClasspathPngDimensions(path);
            assertNotNull(headerDims, "Missing PNG header for " + path);
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
                BufferedImage img = ImageIO.read(in);
                assertEquals(img.getWidth(), headerDims[0], layer + " width");
                assertEquals(img.getHeight(), headerDims[1], layer + " height");
            }
            float[] inset = FxFrame.halfTexel(headerDims[0], headerDims[1]);
            assertEquals(0.5F / headerDims[0], inset[0], 1e-8F);
            assertEquals(0.5F / headerDims[1], inset[1], 1e-8F);
        }
    }

    @Test
    void dynamicPngHeaderMeasurementWorksAcrossArbitraryAtlasResolutions() throws Exception {
        int[][] sizes = {{512, 341}, {1024, 683}, {1536, 1024}, {2048, 1365}, {3072, 2048}};
        for (int[] size : sizes) {
            BufferedImage img = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            int[] dims = FxFrame.readPngDimensions(new ByteArrayInputStream(baos.toByteArray()));
            assertNotNull(dims);
            assertEquals(size[0], dims[0]);
            assertEquals(size[1], dims[1]);
            float[] inset = FxFrame.halfTexel(dims[0], dims[1]);
            float[] cell = {2 / 3F, 0, 1 / 3F, 0, 1 / 3F, 0.5F, 2 / 3F, 0.5F};
            float[] adjusted = FxFrame.insetUv(cell, inset[0], inset[1]);
            assertTrue(adjusted[0] < 2 / 3F && adjusted[2] > 1 / 3F, "horizontal seam edges pulled inward");
            assertTrue(adjusted[1] > 0F && adjusted[5] < 0.5F, "vertical seam edges pulled inward");
        }
    }
}
