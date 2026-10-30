package dev.jilaton.client.music;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Album-art textures keyed by source (file path or image URL). Decoding uses stb_image, which handles both
 * PNG and JPEG; images are downscaled to at most 256px and uploaded on the render thread. A small LRU
 * releases textures that are no longer needed.
 */
public final class Artwork {
    private static final int MAX_SIZE = 256;
    private static final int CACHE = 12;
    private static final AtomicInteger IDS = new AtomicInteger();
    private static final Map<String, Identifier> LOADED = new LinkedHashMap<>(16, 0.75F, true);
    private static final Map<String, Boolean> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

    private Artwork() { }

    /** Returns the texture if ready; otherwise starts decoding {@code bytes} and returns null. */
    public static synchronized Identifier get(String key) { return LOADED.get(key); }

    public static void load(String key, byte[] bytes) {
        if (key == null || bytes == null || get(key) != null || PENDING.putIfAbsent(key, Boolean.TRUE) != null) return;
        NativeImage image;
        try {
            image = decode(bytes);
        } catch (RuntimeException | LinkageError failure) {
            PENDING.remove(key);
            return;
        }
        if (image == null) { PENDING.remove(key); return; }
        Minecraft.getInstance().execute(() -> {
            try {
                Identifier id = Identifier.fromNamespaceAndPath("jilaton", "music/art_" + IDS.incrementAndGet());
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Jilaton album art", image));
                synchronized (Artwork.class) {
                    LOADED.put(key, id);
                    while (LOADED.size() > CACHE) {
                        var eldest = LOADED.entrySet().iterator().next();
                        Minecraft.getInstance().getTextureManager().release(eldest.getValue());
                        LOADED.remove(eldest.getKey());
                    }
                }
            } finally {
                PENDING.remove(key);
            }
        });
    }

    static NativeImage decode(byte[] bytes) {
        ByteBuffer data = MemoryUtil.memAlloc(bytes.length);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            data.put(bytes).flip();
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1), comp = stack.mallocInt(1);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(data, w, h, comp, 4);
            if (pixels == null) return null;
            try {
                int width = w.get(0), height = h.get(0);
                int step = Math.max(1, Math.max(width, height) / MAX_SIZE);
                int outW = Math.max(1, width / step), outH = Math.max(1, height / step);
                NativeImage image = new NativeImage(outW, outH, false);
                for (int y = 0; y < outH; y++) for (int x = 0; x < outW; x++) {
                    int index = ((y * step) * width + x * step) * 4;
                    int r = pixels.get(index) & 0xFF, g = pixels.get(index + 1) & 0xFF, b = pixels.get(index + 2) & 0xFF, a = pixels.get(index + 3) & 0xFF;
                    image.setPixel(x, y, a << 24 | r << 16 | g << 8 | b);
                }
                return image;
            } finally {
                STBImage.stbi_image_free(pixels);
            }
        } finally {
            MemoryUtil.memFree(data);
        }
    }

    public static synchronized void clear() {
        for (Identifier id : LOADED.values()) Minecraft.getInstance().getTextureManager().release(id);
        LOADED.clear();
    }
}
