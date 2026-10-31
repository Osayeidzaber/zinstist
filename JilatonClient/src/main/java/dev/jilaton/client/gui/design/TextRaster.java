package dev.jilaton.client.gui.design;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Jilaton's text renderer. Minecraft's TTF path rasterises glyphs into a fixed atlas without real anti-aliasing
 * control and scales them, which makes UI text soft or broken. Here each string is drawn by Java's font renderer
 * (grayscale anti-aliasing, fractional metrics, pure strokes) at its exact on-screen pixel size and cached as a
 * texture, so it is blitted 1:1 and looks like text in a browser. World text uses a variant with a baked dark outline,
 * mipmaps and linear filtering, so lyrics stay smooth at any distance.
 */
public final class TextRaster {
    /** A face's TTF inside the mod jar. */
    public enum Face {
        REGULAR("inter-regular.ttf"), MEDIUM("inter-medium.ttf"), SEMIBOLD("inter-semibold.ttf"), BOLD("inter-bold.ttf"),
        MONO("jetbrains-mono-regular.ttf"), MONO_MEDIUM("jetbrains-mono-medium.ttf");

        final String file;
        Font font;

        Face(String file) {
            this.file = file;
        }
    }

    /** One coloured run of a multi-colour line (world lyrics: sung words bright, the rest dim). */
    public record Run(String text, int argb) { }

    /** A cached text texture: pixel size, and where the baseline and the text's left edge sit inside it. */
    public record Image(Identifier id, int width, int height, int padLeft, float baseline, float advance) { }

    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
    private static final int MAX_GUI = 700, MAX_WORLD = 64;
    private static final Map<String, Image> GUI = lru(MAX_GUI);
    private static final Map<String, Image> WORLD = lru(MAX_WORLD);
    private static int serial;

    private TextRaster() { }

    private static Map<String, Image> lru(int max) {
        return new LinkedHashMap<>(64, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                if (size() <= max) return false;
                Minecraft.getInstance().getTextureManager().release(eldest.getValue().id());
                return true;
            }
        };
    }

    private static Font font(Face face) {
        if (face.font == null) {
            try (InputStream stream = TextRaster.class.getResourceAsStream("/assets/jilaton/font/" + face.file)) {
                face.font = stream == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 1)
                        : Font.createFont(Font.TRUETYPE_FONT, stream);
            } catch (Exception exception) {
                face.font = new Font(Font.SANS_SERIF, Font.PLAIN, 1);
            }
        }
        return face.font;
    }

    /** Advance width in pixels of {@code text} at an em of {@code emPx} pixels. Linear in {@code emPx}. */
    public static float advance(String text, Face face, float emPx) {
        if (text.isEmpty()) return 0;
        return (float) font(face).deriveFont(100F).getStringBounds(text, FRC).getWidth() * emPx / 100F;
    }

    /** White text at {@code emPx} for the GUI (tinted by the blit colour), cached. */
    public static Image gui(String text, Face face, float emPx) {
        float em = Math.round(emPx * 4) / 4F;
        String key = face.ordinal() + "|" + em + "|" + text;
        Image image = GUI.get(key);
        if (image == null) {
            image = render(List.of(new Run(text, 0xFFFFFFFF)), face, em, 0, false, "gui");
            GUI.put(key, image);
        }
        return image;
    }

    /** Multi-colour world text with a dark outline of {@code outlinePx}, mipmapped and linearly filtered. */
    public static Image world(List<Run> runs, Face face, float emPx, float outlinePx) {
        StringBuilder key = new StringBuilder().append(face.ordinal()).append('|').append(emPx).append('|').append(outlinePx);
        for (Run run : runs) key.append('|').append(Integer.toHexString(run.argb())).append(':').append(run.text());
        Image image = WORLD.get(key.toString());
        if (image == null) {
            image = render(runs, face, emPx, outlinePx, true, "world");
            WORLD.put(key.toString(), image);
        }
        return image;
    }

    private static Image render(List<Run> runs, Face face, float emPx, float outlinePx, boolean mipmaps, String kind) {
        Font font = font(face).deriveFont(emPx);
        StringBuilder all = new StringBuilder();
        for (Run run : runs) all.append(run.text());
        String text = all.toString();
        LineMetrics metrics = font.getLineMetrics(text.isEmpty() ? " " : text, FRC);
        float advance = (float) font.getStringBounds(text.isEmpty() ? " " : text, FRC).getWidth();
        int pad = (int) Math.ceil(outlinePx) + 2;
        int width = Math.max(1, (int) Math.ceil(advance) + pad * 2);
        int height = Math.max(1, (int) Math.ceil(metrics.getAscent() + metrics.getDescent()) + pad * 2);
        if (mipmaps) {
            // Power-of-two friendly padding keeps every mip level aligned.
            width = (width + 3) & ~3;
            height = (height + 3) & ~3;
        }
        float baseline = pad + metrics.getAscent();

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setFont(font);
        if (outlinePx > 0 && !text.isEmpty()) {
            Shape outline = new TextLayout(text, font, FRC).getOutline(AffineTransform.getTranslateInstance(pad, baseline));
            g.setColor(new Color(6, 6, 6, 235));
            g.setStroke(new BasicStroke(outlinePx * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(outline);
        }
        float x = pad;
        for (Run run : runs) {
            if (run.text().isEmpty()) continue;
            g.setColor(new Color(run.argb(), true));
            g.drawString(run.text(), x, baseline);
            x += (float) font.getStringBounds(run.text(), FRC).getWidth();
        }
        g.dispose();

        NativeImage image = new NativeImage(width, height, false);
        for (int py = 0; py < height; py++)
            for (int px = 0; px < width; px++) image.setPixel(px, py, canvas.getRGB(px, py));
        Identifier id = Identifier.fromNamespaceAndPath("jilaton", "dynamic/text_" + kind + "_" + (serial++));
        Minecraft.getInstance().getTextureManager().register(id, new Texture(id.toString(), image, mipmaps));
        return new Image(id, width, height, pad, baseline, advance);
    }

    /** A texture with optional box-filtered mip levels and linear filtering when mipmapped. */
    private static final class Texture extends AbstractTexture {
        Texture(String label, NativeImage base, boolean mipmaps) {
            int levels = 1;
            if (mipmaps) {
                int size = Math.min(base.getWidth(), base.getHeight());
                while (levels < 6 && (size >> levels) >= 4) levels++;
            }
            var device = RenderSystem.getDevice();
            texture = device.createTexture(() -> label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    TextureFormat.RGBA8, base.getWidth(), base.getHeight(), 1, levels);
            textureView = device.createTextureView(texture);
            sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                    mipmaps ? FilterMode.LINEAR : FilterMode.NEAREST, mipmaps ? FilterMode.LINEAR : FilterMode.NEAREST,
                    mipmaps);
            NativeImage level = base;
            var encoder = device.createCommandEncoder();
            for (int mip = 0; mip < levels; mip++) {
                encoder.writeToTexture(texture, level, mip, 0, 0, 0, level.getWidth(), level.getHeight(), 0, 0);
                if (mip + 1 < levels) {
                    NativeImage next = halve(level);
                    if (level != base) level.close();
                    level = next;
                }
            }
            if (level != base) level.close();
            base.close();
        }

        /** Premultiplied 2x2 box filter. */
        private static NativeImage halve(NativeImage source) {
            int w = Math.max(1, source.getWidth() / 2), h = Math.max(1, source.getHeight() / 2);
            NativeImage out = new NativeImage(w, h, false);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double a = 0, r = 0, g = 0, b = 0;
                    for (int dy = 0; dy < 2; dy++) {
                        for (int dx = 0; dx < 2; dx++) {
                            int argb = source.getPixel(Math.min(source.getWidth() - 1, x * 2 + dx),
                                    Math.min(source.getHeight() - 1, y * 2 + dy));
                            double alpha = (argb >>> 24) / 255.0;
                            a += alpha;
                            r += ((argb >> 16) & 0xFF) * alpha;
                            g += ((argb >> 8) & 0xFF) * alpha;
                            b += (argb & 0xFF) * alpha;
                        }
                    }
                    int ai = (int) Math.round(a / 4 * 255);
                    int ri = a > 0 ? (int) Math.round(r / a) : 0, gi = a > 0 ? (int) Math.round(g / a) : 0,
                            bi = a > 0 ? (int) Math.round(b / a) : 0;
                    out.setPixel(x, y, ai << 24 | ri << 16 | gi << 8 | bi);
                }
            }
            return out;
        }
    }
}
