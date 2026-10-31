package dev.jilaton.client.gui.design;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

import java.util.HashMap;
import java.util.Map;

/**
 * Pixel-exact shapes for the redesign. A board rectangle is mapped through the current pose to physical window
 * pixels and rounded there, so edges never land between pixels (no 1-px/2-px wobble at fractional scales). Corners
 * and soft glows come from small textures rasterised at their exact on-screen pixel size with 4x4 supersampled
 * coverage, so every curve is anti-aliased and drawn 1:1 with no filtering.
 */
public final class Paint {
    private static final int MAX_CACHE = 192;
    private static final Map<Long, Identifier> CORNERS = new HashMap<>();
    private static final Map<Long, Identifier> GLOWS = new HashMap<>();
    private static int serial;

    private Paint() { }

    private static double guiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    /** Physical pixels per board unit under the current pose (the pose has no rotation). */
    public static double pixelScale(GuiGraphics g) {
        return g.pose().m00() * guiScale();
    }

    /** Board rect to physical pixels: {x0, y0, x1, y1}. */
    private static int[] toPixels(GuiGraphics g, float x, float y, float w, float h) {
        Matrix3x2f m = g.pose();
        double s = guiScale();
        int x0 = (int) Math.round((m.m00() * x + m.m20()) * s);
        int y0 = (int) Math.round((m.m11() * y + m.m21()) * s);
        int x1 = (int) Math.round((m.m00() * (x + w) + m.m20()) * s);
        int y1 = (int) Math.round((m.m11() * (y + h) + m.m21()) * s);
        return new int[]{x0, y0, x1, y1};
    }

    private static void beginPixels(GuiGraphics g) {
        float inv = (float) (1 / guiScale());
        g.pose().pushMatrix();
        g.pose().identity();
        g.pose().scale(inv, inv);
    }

    private static void endPixels(GuiGraphics g) {
        g.pose().popMatrix();
    }

    /** Pixel-snapped plain rectangle. */
    public static void rect(GuiGraphics g, float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0 || color >>> 24 == 0) return;
        int[] p = toPixels(g, x, y, w, h);
        if (p[2] <= p[0] || p[3] <= p[1]) return;
        beginPixels(g);
        g.fill(p[0], p[1], p[2], p[3], color);
        endPixels(g);
    }

    /** Horizontal hairline exactly one physical pixel tall. */
    public static void hline(GuiGraphics g, float x, float y, float w, int color) {
        int[] p = toPixels(g, x, y, w, 1);
        beginPixels(g);
        g.fill(p[0], p[1], p[2], p[1] + 1, color);
        endPixels(g);
    }

    /** Anti-aliased rounded rectangle fill. */
    public static void round(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
        shape(g, x, y, w, h, radius, 0, color);
    }

    /** Anti-aliased rounded outline; {@code stroke} in board px, never thinner than one physical pixel. */
    public static void outline(GuiGraphics g, float x, float y, float w, float h, float radius, float stroke, int color) {
        int t = Math.max(1, (int) Math.round(stroke * pixelScale(g)));
        shape(g, x, y, w, h, radius, t, color);
    }

    /** Anti-aliased circle. */
    public static void circle(GuiGraphics g, float cx, float cy, float r, int color) {
        shape(g, cx - r, cy - r, r * 2, r * 2, r, 0, color);
    }

    /** Anti-aliased ring of {@code stroke} board px. */
    public static void ring(GuiGraphics g, float cx, float cy, float r, float stroke, int color) {
        int t = Math.max(1, (int) Math.round(stroke * pixelScale(g)));
        shape(g, cx - r, cy - r, r * 2, r * 2, r, t, color);
    }

    private static void shape(GuiGraphics g, float x, float y, float w, float h, float radius, int stroke, int color) {
        if (w <= 0 || h <= 0 || color >>> 24 == 0) return;
        int[] p = toPixels(g, x, y, w, h);
        int pw = p[2] - p[0], ph = p[3] - p[1];
        if (pw <= 0 || ph <= 0) return;
        int r = (int) Math.round(radius * pixelScale(g));
        r = Math.max(0, Math.min(r, Math.min(pw, ph) / 2));
        int t = stroke;
        beginPixels(g);
        int x0 = p[0], y0 = p[1], x1 = p[2], y1 = p[3];
        if (r == 0) {
            if (t == 0) g.fill(x0, y0, x1, y1, color);
            else {
                g.fill(x0, y0, x1, y0 + t, color);
                g.fill(x0, y1 - t, x1, y1, color);
                g.fill(x0, y0 + t, x0 + t, y1 - t, color);
                g.fill(x1 - t, y0 + t, x1, y1 - t, color);
            }
            endPixels(g);
            return;
        }
        Identifier tex = corner(r, t);
        int size = r * 2;
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x0, y0, 0, 0, r, r, size, size, color);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x1 - r, y0, r, 0, r, r, size, size, color);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x0, y1 - r, 0, r, r, r, size, size, color);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x1 - r, y1 - r, r, r, r, r, size, size, color);
        if (t == 0) {
            if (x1 - r > x0 + r) {
                g.fill(x0 + r, y0, x1 - r, y0 + r, color);
                g.fill(x0 + r, y1 - r, x1 - r, y1, color);
            }
            if (y1 - r > y0 + r) g.fill(x0, y0 + r, x1, y1 - r, color);
        } else {
            if (x1 - r > x0 + r) {
                g.fill(x0 + r, y0, x1 - r, y0 + t, color);
                g.fill(x0 + r, y1 - t, x1 - r, y1, color);
            }
            if (y1 - r > y0 + r) {
                g.fill(x0, y0 + r, x0 + t, y1 - r, color);
                g.fill(x1 - t, y0 + r, x1, y1 - r, color);
            }
        }
        endPixels(g);
    }

    /** White disc (t == 0) or ring of thickness t, radius r, as a 2r x 2r texture with supersampled coverage. */
    private static Identifier corner(int r, int t) {
        long key = ((long) r << 32) | t;
        Identifier id = CORNERS.get(key);
        if (id != null) return id;
        if (CORNERS.size() > MAX_CACHE) clear(CORNERS);
        int size = r * 2;
        NativeImage image = new NativeImage(size, size, true);
        double outer = r, inner = r - t;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                int hits = 0;
                for (int sy = 0; sy < 4; sy++) {
                    for (int sx = 0; sx < 4; sx++) {
                        double dx = px + (sx + 0.5) / 4 - r, dy = py + (sy + 0.5) / 4 - r;
                        double d = Math.sqrt(dx * dx + dy * dy);
                        if (d <= outer && (t == 0 || d >= inner)) hits++;
                    }
                }
                int a = Math.round(hits * 255F / 16F);
                image.setPixel(px, py, (a << 24) | 0xFFFFFF);
            }
        }
        id = register("corner_" + r + "_" + t, image);
        CORNERS.put(key, id);
        return id;
    }

    /**
     * Soft radial light centred at (cx, cy), {@code r} board px, peaking at {@code peak} opacity of {@code rgb}. The
     * falloff is baked into the texture with ordered dithering, so even a faint glow over a large area shows no bands.
     */
    public static void glow(GuiGraphics g, float cx, float cy, float r, int rgb, double peak) {
        if (peak <= 0) return;
        int[] p = toPixels(g, cx - r, cy - r, r * 2, r * 2);
        int size = Math.max(8, p[2] - p[0]);
        int quant = Math.min(2048, ((size + 7) / 8) * 8);
        int peakByte = (int) Math.round(Math.max(0, Math.min(1, peak)) * 255);
        long key = ((long) quant << 32) | peakByte;
        Identifier id = GLOWS.get(key);
        if (id == null) {
            if (GLOWS.size() > 24) clear(GLOWS);
            NativeImage image = new NativeImage(quant, quant, true);
            double half = quant / 2.0;
            int[] bayer = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
            for (int py = 0; py < quant; py++) {
                for (int px = 0; px < quant; px++) {
                    double dx = (px + 0.5 - half) / half, dy = (py + 0.5 - half) / half;
                    double d = Math.min(1, Math.sqrt(dx * dx + dy * dy));
                    double falloff = Math.pow(1 - d * d, 2.2);
                    double a = peakByte * falloff + (bayer[(py & 3) * 4 + (px & 3)] / 16.0 - 0.47);
                    int ai = (int) Math.max(0, Math.min(255, Math.floor(a + 0.5)));
                    image.setPixel(px, py, (ai << 24) | 0xFFFFFF);
                }
            }
            id = register("glow_" + quant + "_" + peakByte, image);
            GLOWS.put(key, id);
        }
        beginPixels(g);
        g.blit(RenderPipelines.GUI_TEXTURED, id, p[0], p[1], 0, 0, p[2] - p[0], p[3] - p[1], quant, quant, quant, quant,
                0xFF000000 | (rgb & 0xFFFFFF));
        endPixels(g);
    }

    private static final Map<String, int[]> SOURCES = new HashMap<>();
    private static final Map<String, Identifier> IMAGES = new HashMap<>();

    /**
     * Draws a crop of a classpath PNG resampled (area average, premultiplied) to its exact on-screen pixel size, so a
     * large source shrinks smoothly instead of being point-sampled. {@code from}..{@code to} (0..1) draws only that
     * horizontal slice, which is how the loading wordmark's shine sweeps across it.
     */
    public static void image(GuiGraphics g, String path, int sx, int sy, int sw, int sh, float x, float y, float w, float h,
                             int color, double from, double to) {
        int[] p = toPixels(g, x, y, w, h);
        int pw = p[2] - p[0], ph = p[3] - p[1];
        if (pw <= 0 || ph <= 0 || color >>> 24 == 0) return;
        String key = path + "#" + sx + "," + sy + "," + sw + "," + sh + "@" + pw + "x" + ph;
        Identifier id = IMAGES.get(key);
        if (id == null) {
            int[] src = source(path);
            if (src == null) return;
            if (IMAGES.size() > 16) {
                for (Identifier old : IMAGES.values()) Minecraft.getInstance().getTextureManager().release(old);
                IMAGES.clear();
            }
            id = register("image_" + pw + "x" + ph, resample(src, sx, sy, sw, sh, pw, ph));
            IMAGES.put(key, id);
        }
        int c0 = (int) Math.round(Math.max(0, Math.min(1, from)) * pw), c1 = (int) Math.round(Math.max(0, Math.min(1, to)) * pw);
        if (c1 <= c0) return;
        beginPixels(g);
        g.blit(RenderPipelines.GUI_TEXTURED, id, p[0] + c0, p[1], c0, 0, c1 - c0, ph, pw, ph, color);
        endPixels(g);
    }

    /** {width, height, argb...} of a classpath PNG, read once. */
    private static int[] source(String path) {
        int[] cached = SOURCES.get(path);
        if (cached != null) return cached;
        try (var stream = Paint.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            try (NativeImage image = NativeImage.read(stream)) {
                int w = image.getWidth(), h = image.getHeight();
                int[] out = new int[2 + w * h];
                out[0] = w;
                out[1] = h;
                for (int yy = 0; yy < h; yy++)
                    for (int xx = 0; xx < w; xx++) out[2 + yy * w + xx] = image.getPixel(xx, yy);
                SOURCES.put(path, out);
                return out;
            }
        } catch (java.io.IOException exception) {
            return null;
        }
    }

    private static NativeImage resample(int[] src, int sx, int sy, int sw, int sh, int pw, int ph) {
        int width = src[0];
        NativeImage out = new NativeImage(pw, ph, true);
        double fx = sw / (double) pw, fy = sh / (double) ph;
        for (int oy = 0; oy < ph; oy++) {
            double y0 = sy + oy * fy, y1 = y0 + fy;
            for (int ox = 0; ox < pw; ox++) {
                double x0 = sx + ox * fx, x1 = x0 + fx;
                double a = 0, r = 0, gr = 0, b = 0, area = 0;
                for (int yy = (int) Math.floor(y0); yy < Math.ceil(y1); yy++) {
                    double wy = Math.min(y1, yy + 1) - Math.max(y0, yy);
                    for (int xx = (int) Math.floor(x0); xx < Math.ceil(x1); xx++) {
                        double wgt = wy * (Math.min(x1, xx + 1) - Math.max(x0, xx));
                        int argb = src[2 + yy * width + xx];
                        double pa = (argb >>> 24) / 255.0 * wgt;
                        a += pa;
                        r += ((argb >> 16) & 0xFF) * pa;
                        gr += ((argb >> 8) & 0xFF) * pa;
                        b += (argb & 0xFF) * pa;
                        area += wgt;
                    }
                }
                int ai = (int) Math.round(a / area * 255);
                int ri = a > 0 ? (int) Math.round(r / a) : 0, gi = a > 0 ? (int) Math.round(gr / a) : 0,
                        bi = a > 0 ? (int) Math.round(b / a) : 0;
                out.setPixel(ox, oy, ai << 24 | ri << 16 | gi << 8 | bi);
            }
        }
        return out;
    }

    private static Identifier register(String name, NativeImage image) {
        Identifier id = Identifier.fromNamespaceAndPath("jilaton", "dynamic/paint_" + name + "_" + (serial++));
        DynamicTexture texture = new DynamicTexture(() -> "Jilaton " + name, image);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return id;
    }

    private static void clear(Map<Long, Identifier> cache) {
        for (Identifier id : cache.values()) Minecraft.getInstance().getTextureManager().release(id);
        cache.clear();
    }
}
