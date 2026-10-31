package dev.jilaton.client.render.fx;

import dev.jilaton.client.render.EffectBatch;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end checks for the dynamic half-texel inset of the sky-cube atlas (JA-018). */
class SkyInsetVerificationTest {
    private static final int[][] ATLAS_SIZES = {{1024, 683}, {2048, 1365}, {3072, 2048}};

    @Test
    void everyCornerOfEverySkyCellMovesInwardByExactlyHalfATexel() {
        for (int[] size : ATLAS_SIZES) {
            float u = 0.5F / size[0], v = 0.5F / size[1];
            for (int cell = 0; cell < FxFrame.SKY_UVS.length; cell++) {
                float[] src = FxFrame.SKY_UVS[cell];
                float[] out = FxFrame.insetUv(src, u, v);
                float minU = Math.min(Math.min(src[0], src[2]), Math.min(src[4], src[6]));
                float maxU = Math.max(Math.max(src[0], src[2]), Math.max(src[4], src[6]));
                float minV = Math.min(Math.min(src[1], src[3]), Math.min(src[5], src[7]));
                float maxV = Math.max(Math.max(src[1], src[3]), Math.max(src[5], src[7]));
                for (int corner = 0; corner < 4; corner++) {
                    String where = size[0] + "x" + size[1] + " cell " + cell + " corner " + corner;
                    float originalU = src[corner * 2], originalV = src[corner * 2 + 1];
                    float movedU = out[corner * 2] - originalU;
                    float movedV = out[corner * 2 + 1] - originalV;
                    assertEquals(originalU == minU ? u : -u, movedU, 1e-7F, where + " u");
                    assertEquals(originalV == minV ? v : -v, movedV, 1e-7F, where + " v");
                    assertTrue(out[corner * 2] > minU && out[corner * 2] < maxU, where + " inside u span");
                    assertTrue(out[corner * 2 + 1] > minV && out[corner * 2 + 1] < maxV, where + " inside v span");
                }
            }
        }
    }

    @Test
    void theInsetClampsToAQuarterOfTheCellSoWideTexturesCannotCollapseACell() {
        float[] cell = FxFrame.SKY_UVS[0]; // u 0..1/3, v 0.5..1
        float[] out = FxFrame.insetUv(cell, 1F, 1F);
        float clampU = (1 / 3F) * 0.25F, clampV = 0.5F * 0.25F;
        assertEquals(1 / 3F - clampU, out[0], 1e-7F);
        assertEquals(0.5F + clampV, out[1], 1e-7F);
        assertEquals(clampU, out[2], 1e-7F);
        assertEquals(1 - clampV, out[5], 1e-7F);
        // Half of the cell survives on each axis, so the face is never degenerate.
        assertEquals((1 / 3F) * 0.5F, out[0] - out[2], 1e-7F);
        assertEquals(0.5F * 0.5F, out[5] - out[1], 1e-7F);
        // A zero texel inset is the identity, not a collapsed rectangle.
        assertArrayEquals(cell, FxFrame.insetUv(cell, 0, 0), 0);
    }

    @Test
    void missingTexturePlaceholdersAndNonAtlasShapesAreRejected() {
        assertTrue(FxFrame.plausibleSkyTexture(1024, 683));
        assertTrue(FxFrame.plausibleSkyTexture(2048, 1365));
        assertTrue(FxFrame.plausibleSkyTexture(3072, 2048));
        // The texture manager reports a missing texture as a 16x16 placeholder: never a sky measurement.
        assertFalse(FxFrame.plausibleSkyTexture(16, 16));
        assertFalse(FxFrame.plausibleSkyTexture(0, 0));
        assertFalse(FxFrame.plausibleSkyTexture(64, 64));
        assertFalse(FxFrame.plausibleSkyTexture(2048, 2048));
        assertFalse(FxFrame.plausibleSkyTexture(3072, 1536));
        assertFalse(FxFrame.plausibleSkyTexture(32, 21));
    }

    @Test
    void pngHeadersAreReadBigEndianAndMalformedHeadersAreRejected() throws Exception {
        int[] dims = FxFrame.readPngDimensions(new ByteArrayInputStream(pngHeader(4096, 1365)));
        assertNotNull(dims);
        assertEquals(4096, dims[0]);
        assertEquals(1365, dims[1]);

        byte[] header = pngHeader(2048, 1365);
        assertNull(FxFrame.readPngDimensions(new ByteArrayInputStream(Arrays.copyOf(header, 23))));
        byte[] wrongSignature = header.clone();
        wrongSignature[0] = 0;
        assertNull(FxFrame.readPngDimensions(new ByteArrayInputStream(wrongSignature)));
        assertNull(FxFrame.readPngDimensions(new ByteArrayInputStream(pngHeader(0, 683))));
        assertNull(FxFrame.readPngDimensions(new ByteArrayInputStream(pngHeader(-2, 683))));
        assertNull(FxFrame.readPngDimensions(new ByteArrayInputStream(new byte[]{1, 2, 3, 4})));
        assertNull(FxFrame.readPngDimensions(null));
    }

    @Test
    void overrideTargetSizesMatchTheBundledPngHeaders() {
        for (String layer : SkyPackTextures.LAYERS) {
            int[] bundled = FxFrame.readClasspathPngDimensions("assets/jilaton/textures/sky/" + layer + ".png");
            assertNotNull(bundled, "Missing bundled header for " + layer);
            assertArrayEquals(bundled, SkyPackTextures.targetSize(layer), layer);
        }
        // The mask layers and the flare are half-size at 1024x683; integer halving would wrongly give 682.
        assertArrayEquals(new int[]{1024, 683}, SkyPackTextures.targetSize("mask"));
        assertArrayEquals(new int[]{1024, 683}, SkyPackTextures.targetSize("mask_moon"));
        assertArrayEquals(new int[]{1024, 683}, SkyPackTextures.targetSize("sunflare"));
        assertArrayEquals(new int[]{2048, 1365}, SkyPackTextures.targetSize("day"));
        assertArrayEquals(new int[]{2048, 1365}, SkyPackTextures.targetSize("night"));
        assertArrayEquals(new int[]{2048, 1365}, SkyPackTextures.targetSize("sun"));
        assertArrayEquals(new int[]{2048, 1365}, SkyPackTextures.targetSize("stars"));
    }

    @Test
    void measuredSizeOverridesAnEarlyFallbackAndBundledLayersReadTheirPngHeader() {
        FxFrame.clearSkyInsets();
        try {
            Identifier notBundled = Identifier.fromNamespaceAndPath("jilaton", "textures/sky/not_bundled.png");
            FxFrame frame = frame();
            frame.skyCube(notBundled, 100, 0, 0, 1F);
            assertEquals(6, frame.sky().size());
            assertEquals(1 / 3F - 0.5F / 1024F, frame.sky().get(0).uvs()[0], 0F);

            // A measured override must replace the fallback that was used before the texture loaded.
            FxFrame.recordSkyTextureSize(notBundled, 3072, 2048);
            frame.skyCube(notBundled, 100, 0, 0, 1F);
            float[] measured = frame.sky().get(6).uvs();
            assertEquals(1 / 3F - 0.5F / 3072F, measured[0], 0F);
            assertEquals(0.5F + 0.5F / 2048F, measured[1], 0F);

            // With no GPU available the bundled layer falls back to its PNG header, never to 1024x683.
            Identifier day = Identifier.fromNamespaceAndPath("jilaton", "textures/sky/day.png");
            FxFrame bundledFrame = frame();
            bundledFrame.skyCube(day, 100, 0, 0, 1F);
            float[] header = bundledFrame.sky().get(0).uvs();
            assertEquals(1 / 3F - 0.5F / 2048F, header[0], 0F);
            assertEquals(0.5F + 0.5F / 1365F, header[1], 0F);
        } finally {
            FxFrame.clearSkyInsets();
        }
    }

    private static FxFrame frame() {
        return new FxFrame(null, new EffectBatch(), 0, 0, 0,
                new float[]{1, 0, 0}, new float[]{0, 1, 0}, new float[]{0, 0, 1},
                0, 0, 0, 0xFFFFFF, VisualQuality.BALANCED, false, 1, false);
    }

    private static byte[] pngHeader(int width, int height) {
        byte[] header = new byte[24];
        header[0] = (byte) 0x89;
        header[1] = 0x50;
        header[2] = 0x4E;
        header[3] = 0x47;
        header[4] = 0x0D;
        header[5] = 0x0A;
        header[6] = 0x1A;
        header[7] = 0x0A;
        header[11] = 13;
        header[12] = 'I';
        header[13] = 'H';
        header[14] = 'D';
        header[15] = 'R';
        putInt(header, 16, width);
        putInt(header, 20, height);
        return header;
    }

    private static void putInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }
}
