package dev.jilaton.client.render.fx;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Skybox layers for {@code CustomSkyModule}. The Dramatic Skys layers ship in the jar as
 * {@code assets/jilaton/textures/sky/<layer>.png}; a pack the user installs locally can override them at
 * runtime without rebuilding. Both paths use the same 3:2 atlas: six square cube faces in a 3×2 grid, as
 * consumed by {@link FxFrame#skyCube}.
 *
 * <p>Override search order (first match wins, resolved once per session):
 * <ol>
 *   <li>{@code -Djilaton.skyPack=<zip-or-directory>}</li>
 *   <li>{@code <gameDir>/config/jilaton/sky/} — a zip or the unpacked pack</li>
 *   <li>a pack carrying {@code assets/skybox/day.png} in {@code <gameDir>/resourcepacks/}</li>
 * </ol>
 * Override textures are decoded, downscaled to the bundled size (2048×1365; masks and flare 1024×683) and
 * registered as dynamic textures. Without an
 * override the bundled classpath textures are used, so the sky works with no external file present.
 */
public final class SkyPackTextures {
    public static final String[] LAYERS = {"day", "night", "sun", "stars", "mask", "mask_moon", "sunflare"};
    /** Override texture size, matching the bundled copies: the pack's 3072×2048 atlas at two thirds. */
    private static final int WIDTH = 2048;
    private static final int HEIGHT = 1365;
    /** Masks and the flare ship at half the atlas size, 1024×683 — not 1024×682, which integer division yields. */
    private static final int MASK_WIDTH = 1024;
    private static final int MASK_HEIGHT = 683;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Identifier> BUNDLED = new HashMap<>();
    private static final Map<String, Identifier> OVERRIDE = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();
    private static boolean resolved;
    private static Path zip;
    private static Path directory;

    static {
        for (String layer : LAYERS) {
            BUNDLED.put(layer, Identifier.fromNamespaceAndPath("jilaton", "textures/sky/" + layer + ".png"));
        }
    }

    private SkyPackTextures() { }

    /** True when the user supplied a pack that overrides the bundled layers; resolves it on first call. */
    public static synchronized boolean override() {
        if (!resolved) resolve();
        return zip != null || directory != null;
    }

    /** Texture for a layer: the user's override when present, otherwise the bundled copy. Never null. */
    public static synchronized Identifier get(String layer) {
        Identifier id = OVERRIDE.get(layer);
        if (id != null) return id;
        if (MISSING.contains(layer) || !override()) return BUNDLED.get(layer);
        if (Minecraft.getInstance() == null) return BUNDLED.get(layer);
        id = load(layer);
        if (id == null) MISSING.add(layer);
        return id != null ? id : BUNDLED.get(layer);
    }

    /** Where the layers are coming from, for the checkpoint and the settings tooltip. */
    public static synchronized String source() {
        if (!override()) return "bundled (jilaton:textures/sky)";
        return (zip != null ? zip : directory).toString();
    }

    private static void resolve() {
        resolved = true;
        String explicit = System.getProperty("jilaton.skyPack");
        if (explicit != null && !explicit.isBlank() && accept(Path.of(explicit))) return;
        Path game = FabricLoader.getInstance().getGameDir();
        if (accept(game.resolve("config").resolve("jilaton").resolve("sky"))) return;
        if (findZipIn(game.resolve("resourcepacks"), null)) return;
        LOGGER.info("[Sky] Using the bundled Dramatic Skys layers; drop a pack at {} or pass "
                + "-Djilaton.skyPack=<zip> to override them.", game.resolve("config/jilaton/sky"));
    }

    /** Newest zip in {@code dir} whose name contains {@code hint} and that carries the skybox layers. */
    private static boolean findZipIn(Path dir, String hint) {
        if (!Files.isDirectory(dir)) return false;
        List<Path> candidates = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.zip")) {
            for (Path file : stream) {
                String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                if (hint == null || name.contains(hint)) candidates.add(file);
            }
        } catch (IOException e) {
            LOGGER.warn("[Sky] Could not scan {} for a pack", dir, e);
            return false;
        }
        candidates.sort(Comparator.comparingLong((Path p) -> {
            try {
                return Files.getLastModifiedTime(p).toMillis();
            } catch (IOException e) {
                return 0L;
            }
        }).reversed());
        for (Path candidate : candidates) {
            if (hasSkybox(candidate)) {
                zip = candidate;
                LOGGER.info("[Sky] Overriding the bundled layers with {}", candidate);
                return true;
            }
        }
        return false;
    }

    private static boolean accept(Path path) {
        if (Files.isDirectory(path)) {
            if (Files.isRegularFile(path.resolve("assets/skybox/day.png")) || Files.isRegularFile(path.resolve("day.png"))) {
                directory = path;
                LOGGER.info("[Sky] Overriding the bundled layers with {}", path);
                return true;
            }
            return findZipIn(path, null);
        }
        if (Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip")
                && hasSkybox(path)) {
            zip = path;
            LOGGER.info("[Sky] Overriding the bundled layers with {}", path);
            return true;
        }
        return false;
    }

    private static boolean hasSkybox(Path archive) {
        try (ZipFile file = new ZipFile(archive.toFile())) {
            return file.getEntry("assets/skybox/day.png") != null;
        } catch (IOException e) {
            return false;
        }
    }

    /** Target pixel size of an override layer: the bundled copy's size, so UV insets stay half a texel. */
    static int[] targetSize(String layer) {
        boolean half = layer.startsWith("mask") || layer.equals("sunflare");
        return half ? new int[]{MASK_WIDTH, MASK_HEIGHT} : new int[]{WIDTH, HEIGHT};
    }

    private static Identifier load(String layer) {
        byte[] bytes = read(layer);
        if (bytes == null) return null;
        int[] target = targetSize(layer);
        int width = target[0];
        int height = target[1];
        NativeImage source = null;
        NativeImage image = null;
        DynamicTexture texture = null;
        try {
            source = NativeImage.read(NativeImage.Format.RGBA, new ByteArrayInputStream(bytes));
            if (source.getWidth() == width && source.getHeight() == height) {
                image = source;
                source = null;
            } else {
                image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
                source.resizeSubRectTo(0, 0, source.getWidth(), source.getHeight(), image);
                source.close();
                source = null;
            }
            Identifier id = Identifier.fromNamespaceAndPath("jilaton", "dynamic/sky/" + layer);
            texture = new DynamicTexture(() -> "jilaton sky " + layer, image);
            image = null; // the DynamicTexture owns the pixels
            Minecraft.getInstance().getTextureManager().register(id, texture);
            FxFrame.recordSkyTextureSize(id, width, height);
            LOGGER.info("[Sky] Loaded override layer {} ({}x{}) from {}", layer, width, height, source());
            texture = null; // the texture manager owns it from here
            return id;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("[Sky] Could not load skybox layer {}; keeping the bundled copy", layer, e);
            if (source != null) source.close();
            if (image != null) image.close();
            if (texture != null) texture.close();
            return null;
        }
    }

    private static byte[] read(String layer) {
        String entry = "assets/skybox/" + layer + ".png";
        try {
            if (zip != null) {
                try (ZipFile file = new ZipFile(zip.toFile())) {
                    ZipEntry found = file.getEntry(entry);
                    if (found == null) return null;
                    try (InputStream in = file.getInputStream(found)) {
                        return in.readAllBytes();
                    }
                }
            }
            if (directory != null) {
                Path file = directory.resolve(entry);
                if (!Files.isRegularFile(file)) file = directory.resolve(layer + ".png");
                if (!Files.isRegularFile(file)) return null;
                return Files.readAllBytes(file);
            }
        } catch (IOException e) {
            LOGGER.warn("[Sky] Could not read skybox layer {}", layer, e);
        }
        return null;
    }
}
