package dev.jilaton.client.render.fx;

import dev.jilaton.client.render.EffectBatch;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything a world effect needs for one frame: camera basis, timing, palette, quality and the output
 * collectors. Coordinates handed to the {@code *World} helpers are absolute; the frame converts them to
 * camera-relative space so the draw phase never touches the level.
 */
public final class FxFrame {
    public final Minecraft client;
    public final EffectBatch batch;
    public final double camX, camY, camZ;
    public final float rightX, rightY, rightZ;
    public final float upX, upY, upZ;
    public final float forwardX, forwardY, forwardZ;
    public final long now;
    /** Seconds since the previous rendered frame, clamped to avoid simulation jumps after stalls. */
    public final double dt;
    public final float partial;
    public final int accent;
    public final VisualQuality quality;
    public final boolean reducedMotion;
    public final double intensity;
    public final boolean firstPerson;
    private final List<WorldText> texts = new ArrayList<>();
    private final List<WorldImage> images = new ArrayList<>();
    private final List<SkyQuad> sky = new ArrayList<>(8);

    public FxFrame(Minecraft client, EffectBatch batch, double camX, double camY, double camZ,
                   float[] right, float[] up, float[] forward, long now, double dt, float partial, int accent,
                   VisualQuality quality, boolean reducedMotion, double intensity, boolean firstPerson) {
        this.client = client;
        this.batch = batch;
        this.camX = camX;
        this.camY = camY;
        this.camZ = camZ;
        this.rightX = right[0]; this.rightY = right[1]; this.rightZ = right[2];
        this.upX = up[0]; this.upY = up[1]; this.upZ = up[2];
        this.forwardX = forward[0]; this.forwardY = forward[1]; this.forwardZ = forward[2];
        this.now = now;
        this.dt = dt;
        this.partial = partial;
        this.accent = accent;
        this.quality = quality;
        this.reducedMotion = reducedMotion;
        this.intensity = intensity;
        this.firstPerson = firstPerson;
    }

    public double seconds() { return now / 1_000_000_000.0; }

    public List<WorldText> texts() { return texts; }
    public List<WorldImage> images() { return images; }
    public List<SkyQuad> sky() { return sky; }

    /** Cube net of the bundled skybox: six faces with their texture coordinates, unit cube, y up. */
    private static final float[][] SKY_FACES = {
            {-1, 1, -1, -1, 1, 1, -1, -1, 1, -1, -1, -1},
            {-1, 1, 1, 1, 1, 1, 1, -1, 1, -1, -1, 1},
            {1, 1, 1, 1, 1, -1, 1, -1, -1, 1, -1, 1},
            {1, 1, -1, -1, 1, -1, -1, -1, -1, 1, -1, -1},
            {1, 1, 1, -1, 1, 1, -1, 1, -1, 1, 1, -1},
            {1, -1, -1, -1, -1, -1, -1, -1, 1, 1, -1, 1}
    };
    private static final float[][] SKY_UVS = {
            {1 / 3F, 0.5F, 0, 0.5F, 0, 1, 1 / 3F, 1},
            {1, 0, 2 / 3F, 0, 2 / 3F, 0.5F, 1, 0.5F},
            {1, 0.5F, 2 / 3F, 0.5F, 2 / 3F, 1, 1, 1},
            {2 / 3F, 0.5F, 1 / 3F, 0.5F, 1 / 3F, 1, 2 / 3F, 1},
            {2 / 3F, 0, 1 / 3F, 0, 1 / 3F, 0.5F, 2 / 3F, 0.5F},
            {1 / 3F, 0, 0, 0, 0, 0.5F, 1 / 3F, 0.5F}
    };
    /**
     * Half a texel, pulled in from every cell edge. Sampling the cells at their exact borders blends each
     * face's outermost pixel with the neighbouring cell, which shows up as a faint line wherever two cube faces
     * meet; the inset removes that atlas bleed. Dimensions are measured dynamically per texture from the GPU
     * texture or PNG IHDR header (covering 2048x1365, 1024x683, 3072x2048, or any custom sky pack).
     */
    private static final float FALLBACK_INSET_U = 0.5F / 1024F;
    private static final float FALLBACK_INSET_V = 0.5F / 683F;
    private static final java.util.Map<Identifier, float[]> SKY_INSETS = new java.util.concurrent.ConcurrentHashMap<>();

    public static void recordSkyTextureSize(Identifier texture, int width, int height) {
        if (texture != null && width > 0 && height > 0) {
            SKY_INSETS.put(texture, halfTexel(width, height));
        }
    }

    public static void clearSkyInsets() {
        SKY_INSETS.clear();
    }

    /**
     * Adds one textured sky-cube layer centred on the eye. {@code yaw} spins the box about the vertical
     * axis, {@code roll} about the celestial axis; both are radians. Positions are emitted camera-relative.
     * Layers are screen-blended, so {@code strength} scales the emitted colour: a black pixel contributes nothing.
     */
    public void skyCube(Identifier texture, double radius, double yaw, double roll, float strength) {
        if (sky.size() >= 48 || strength <= 0.004) return;
        int tone = (int) (Math.min(1F, strength) * 255);
        int color = (tone << 24) | (tone << 16) | (tone << 8) | tone;
        float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw);
        float cr = (float) Math.cos(roll), sr = (float) Math.sin(roll);
        double r = radius;
        float[] inset = texelInset(texture);
        for (int f = 0; f < SKY_FACES.length; f++) {
            float[] src = SKY_FACES[f];
            float[] pos = new float[12];
            for (int v = 0; v < 4; v++) {
                float x = src[v * 3], y = src[v * 3 + 1], z = src[v * 3 + 2];
                float x1 = x * cy + z * sy, z1 = z * cy - x * sy;
                pos[v * 3] = (float) ((x1 * cr - y * sr) * r);
                pos[v * 3 + 1] = (float) ((x1 * sr + y * cr) * r);
                pos[v * 3 + 2] = (float) (z1 * r);
            }
            sky.add(new SkyQuad(texture, pos, insetUv(SKY_UVS[f], inset[0], inset[1]), color));
        }
    }

    /** Half a texel of {@code texture} in UV units, measured dynamically from the GPU texture or PNG header. */
    private float[] texelInset(Identifier texture) {
        if (texture == null) return new float[]{FALLBACK_INSET_U, FALLBACK_INSET_V};
        float[] known = SKY_INSETS.get(texture);
        if (known != null) return known;
        if (client != null) {
            try {
                var gpu = client.getTextureManager().getTexture(texture).getTexture();
                int w = gpu.getWidth(0);
                int h = gpu.getHeight(0);
                if (w > 0 && h > 0) {
                    float[] measured = halfTexel(w, h);
                    SKY_INSETS.put(texture, measured);
                    return measured;
                }
            } catch (RuntimeException ignored) {
            }
        }
        int[] dims = readClasspathPngDimensions("assets/" + texture.getNamespace() + "/" + texture.getPath());
        if (dims != null && dims[0] > 0 && dims[1] > 0) {
            float[] measured = halfTexel(dims[0], dims[1]);
            SKY_INSETS.put(texture, measured);
            return measured;
        }
        return new float[]{FALLBACK_INSET_U, FALLBACK_INSET_V};
    }

    /** Reads width and height from the 24-byte PNG IHDR header of a classpath resource without full decoding. */
    static int[] readClasspathPngDimensions(String resourcePath) {
        if (resourcePath == null || resourcePath.isEmpty()) return null;
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        try (java.io.InputStream in = FxFrame.class.getClassLoader().getResourceAsStream(normalized)) {
            return readPngDimensions(in);
        } catch (java.io.IOException ignored) {
            return null;
        }
    }

    static int[] readPngDimensions(java.io.InputStream in) throws java.io.IOException {
        if (in == null) return null;
        byte[] header = in.readNBytes(24);
        if (header.length < 24) return null;
        if ((header[0] & 0xFF) != 0x89 || header[1] != 0x50 || header[2] != 0x4E || header[3] != 0x47) return null;
        int width = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16)
                | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
        int height = ((header[20] & 0xFF) << 24) | ((header[21] & 0xFF) << 16)
                | ((header[22] & 0xFF) << 8) | (header[23] & 0xFF);
        return width > 0 && height > 0 ? new int[]{width, height} : null;
    }

    static float[] halfTexel(int width, int height) {
        if (width <= 0 || height <= 0) return new float[]{FALLBACK_INSET_U, FALLBACK_INSET_V};
        return new float[]{0.5F / width, 0.5F / height};
    }

    /** Pulls each face's UVs half a texel inward from its atlas cell so neighbouring cells cannot bleed in. */
    static float[] insetUv(float[] cell, float texelU, float texelV) {
        float[] uv = cell.clone();
        float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE, minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            minU = Math.min(minU, cell[i * 2]);
            maxU = Math.max(maxU, cell[i * 2]);
            minV = Math.min(minV, cell[i * 2 + 1]);
            maxV = Math.max(maxV, cell[i * 2 + 1]);
        }
        float centreU = (minU + maxU) * 0.5F, centreV = (minV + maxV) * 0.5F;
        float insetU = Math.min(texelU, (maxU - minU) * 0.25F);
        float insetV = Math.min(texelV, (maxV - minV) * 0.25F);
        for (int i = 0; i < 4; i++) {
            uv[i * 2] += uv[i * 2] < centreU ? insetU : -insetU;
            uv[i * 2 + 1] += uv[i * 2 + 1] < centreV ? insetV : -insetV;
        }
        return uv;
    }

    /** World coordinates in, camera-relative coordinates stored: the draw phase only sees positions relative to the camera. */
    public void text(WorldText text) {
        if (texts.size() >= 96) return;
        texts.add(new WorldText(text.x() - camX, text.y() - camY, text.z() - camZ, text.runs(), text.scale(),
                text.background(), text.glow(), text.billboard(), text.yaw(), text.seeThrough(), text.align()));
    }

    public void image(WorldImage image) {
        if (images.size() >= 16) return;
        images.add(new WorldImage(image.x() - camX, image.y() - camY, image.z() - camZ, image.texture(), image.size(),
                image.alpha(), image.billboard(), image.yaw()));
    }

    public double distanceSqr(double x, double y, double z) {
        double dx = x - camX, dy = y - camY, dz = z - camZ;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Text placed in the world. Runs allow per-word coloring for progressive lyric highlighting. */
    public record WorldText(double x, double y, double z, List<Run> runs, float scale, int background,
                            int glow, boolean billboard, float yaw, boolean seeThrough, int align) {
        public WorldText(double x, double y, double z, List<Run> runs, float scale, int background,
                         int glow, boolean billboard, float yaw, boolean seeThrough) {
            this(x, y, z, runs, scale, background, glow, billboard, yaw, seeThrough, 0);
        }

        public String plain() {
            StringBuilder builder = new StringBuilder();
            for (Run run : runs) builder.append(run.text());
            return builder.toString();
        }
    }

    public record Run(String text, int color) { }

    /** A textured square in the world, e.g. album art. Size is the edge length in blocks. */
    public record WorldImage(double x, double y, double z, Identifier texture, float size, float alpha,
                             boolean billboard, float yaw) { }

    /** One textured face of a skybox layer. Positions are camera-relative: positions.length == 12, uvs == 8. */
    public record SkyQuad(Identifier texture, float[] positions, float[] uvs, int color) { }
}
