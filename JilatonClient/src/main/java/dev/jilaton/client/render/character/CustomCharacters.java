package dev.jilaton.client.render.character;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import dev.jilaton.client.render.character.gltf.CharacterBake;
import dev.jilaton.client.render.character.gltf.ImageDecoder;
import dev.jilaton.client.render.character.kit.BakedDesign;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class CustomCharacters {
   private static final Logger LOGGER = LoggerFactory.getLogger("Jilaton Characters");
   private static final long MAX_FILE = 268435456L;
   private static final int MAX_TEXTURE = 8192;
   /** Bumped whenever the bake format changes, so every cached .anc is invalidated. */
   static final String BAKE_VERSION = "3";
   private static final int MAX_REPORTS = 256;
   private static final Set<String> REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();

   private CustomCharacters() {
   }

   static Path folder() {
      try {
         FabricLoader loader = FabricLoader.getInstance();
         Path config = loader == null ? null : loader.getConfigDir();
         return config == null ? null : config.resolve("jilaton").resolve("characters");
      } catch (Throwable unavailable) {
         return null;
      }
   }

   static Path ensureFolder() {
      Path root = folder();
      if (root == null) {
         return null;
      }

      try {
         Files.createDirectories(root);
         return root;
      } catch (IOException | RuntimeException e) {
         LOGGER.warn("[Characters] Could not create the character folder {}: {}", root, e.toString());
         return null;
      }
   }

   private static Path cache(Path root) {
      return root == null ? null : root.resolve(".baked");
   }

   static List<BakedDesign> scan() {
      return scan(folder());
   }

   static List<BakedDesign> scan(Path root) {
      if (root == null) {
         return List.of();
      }

      Path cacheDir = cache(root);
      List<Path> files = new ArrayList<>();

      try (DirectoryStream<Path> dir = Files.newDirectoryStream(root, "*.{glb,GLB}")) {
         for (Path file : dir) {
            if (Files.isRegularFile(file)) {
               files.add(file);
            }
         }
      } catch (IOException ignored) {
         return List.of();
      }

      for (Path gltf : unsupportedGltf(root)) {
         reportUnsupported(gltf);
      }

      files.sort(null);
      List<BakedDesign> out = new ArrayList<>();
      Set<String> ids = new HashSet<>();

      for (Path file : files) {
         String name = baseName(file);

         try {
            String id = cacheId(name, Files.size(file), Files.getLastModifiedTime(file).toMillis());
            ids.add(id);
            out.add(new BakedDesign(id, name, () -> open(file, id, cacheDir)));
         } catch (IOException e) {
            LOGGER.warn("[Characters] Skipping custom character {}: {}", file, e.toString());
         }
      }

      prune(ids, cacheDir);
      return out;
   }

   /**
    * Cache identity of a drop-folder model. The stamp covers the display name, byte size, modification time and
    * the bake format version, so a replaced or renamed file never reuses an older bake.
    */
   static String cacheId(String name, long size, long modifiedMillis) {
      return "custom_" + slug(name) + "_" + Integer.toHexString(stamp(name, size, modifiedMillis).hashCode());
   }

   static String stamp(String name, long size, long modifiedMillis) {
      return name + size + modifiedMillis + BAKE_VERSION;
   }

   private static String baseName(Path file) {
      String name = file.getFileName().toString();
      int dot = name.lastIndexOf(46);
      return dot > 0 ? name.substring(0, dot) : name;
   }

   /**
    * .gltf scene files are listed here only so they can be reported: the importer reads self-contained .glb
    * binaries and cannot resolve external .bin buffers or image files. They are never returned as designs.
    */
   static List<Path> unsupportedGltf(Path root) {
      if (root == null) {
         return List.of();
      }

      List<Path> out = new ArrayList<>();

      try (DirectoryStream<Path> dir = Files.newDirectoryStream(root, "*.{gltf,GLTF}")) {
         for (Path file : dir) {
            if (Files.isRegularFile(file)) {
               out.add(file);
            }
         }
      } catch (IOException ignored) {
         return List.of();
      }

      out.sort(null);
      return out;
   }

   private static void reportUnsupported(Path file) {
      String key = file.toAbsolutePath().normalize().toString();

      try {
         key = key + ":" + Files.size(file) + ":" + Files.getLastModifiedTime(file).toMillis();
      } catch (IOException unreadable) {
         // Still report the name; the file may be locked or removed while scanning.
      }

      if (REPORTED.contains(key)) {
         return;
      }

      if (REPORTED.size() >= MAX_REPORTS) {
         REPORTED.clear();
      }

      REPORTED.add(key);
      LOGGER.warn(
         "[Characters] Ignoring {}: .gltf scene files are not supported because their external .bin and image "
            + "files cannot be read. Export the model as one self-contained .glb.",
         file.getFileName()
      );
      CharacterModule.unsupportedModel(file.getFileName().toString());
   }

   private static String slug(String name) {
      String s = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
      return s.isEmpty() ? "model" : (s.length() > 32 ? s.substring(0, 32) : s);
   }

   private static void prune(Set<String> keep, Path dirPath) {
      if (dirPath == null) {
         return;
      }

      try (DirectoryStream<Path> dir = Files.newDirectoryStream(dirPath, "*.anc")) {
         for (Path file : dir) {
            String id = file.getFileName().toString().replace(".anc", "");
            if (!keep.contains(id)) {
               Files.deleteIfExists(file);
            }
         }
      } catch (IOException var7) {
      }
   }

   private static synchronized InputStream open(Path file, String id, Path cacheDir) throws IOException {
      if (cacheDir == null) {
         throw new IOException("the character folder is unavailable");
      }

      Path baked = cacheDir.resolve(id + ".anc");
      if (Files.exists(baked)) {
         return Files.newInputStream(baked);
      }

      if (Files.size(file) > 268435456L) {
         throw new IOException("the file is larger than 256 MB");
      }

      String name = file.getFileName().toString();
      CharacterModule.baking(name);
      long start = System.nanoTime();
      CharacterBake.Result result = CharacterBake.bake(Files.readAllBytes(file), new CharacterBake.Options(), CustomCharacters::decode);
      LOGGER.info("[Characters] Baked custom character {} in {} ms, moving {}", new Object[]{name, (System.nanoTime() - start) / 1000000L, result.limbs()});
      if (result.limbs().size() < 3) {
         CharacterModule.notice(name, result.limbs());
      }

      Files.createDirectories(cacheDir);
      Path temp = cacheDir.resolve(id + ".tmp");
      Files.write(temp, result.data());
      Files.move(temp, baked, StandardCopyOption.REPLACE_EXISTING);
      return new ByteArrayInputStream(result.data());
   }

   private static ImageDecoder.Picture decode(byte[] data) throws IOException {
      ByteBuffer encoded = MemoryUtil.memAlloc(data.length);

      ImageDecoder.Picture var29;
      try {
         MemoryStack stack = MemoryStack.stackPush();

         try {
            encoded.put(data).flip();
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer rgba = STBImage.stbi_load_from_memory(encoded, w, h, channels, 4);
            if (rgba == null) {
               throw new IOException("a texture can't be read: " + STBImage.stbi_failure_reason());
            }

            try {
               int width = w.get(0);
               int height = h.get(0);
               if (width > 8192 || height > 8192) {
                  throw new IOException("a texture is over 8192 px");
               }

               int[] argb = new int[width * height];

               for (int i = 0; i < argb.length; i++) {
                  int r = rgba.get(i * 4) & 255;
                  int g = rgba.get(i * 4 + 1) & 255;
                  int b = rgba.get(i * 4 + 2) & 255;
                  int a = rgba.get(i * 4 + 3) & 255;
                  argb[i] = a << 24 | r << 16 | g << 8 | b;
               }

               var29 = new ImageDecoder.Picture(width, height, argb);
            } finally {
               STBImage.stbi_image_free(rgba);
            }
         } catch (Throwable var27) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var25) {
                  var27.addSuppressed(var25);
               }
            }

            throw var27;
         }

         if (stack != null) {
            stack.close();
         }
      } finally {
         MemoryUtil.memFree(encoded);
      }

      return var29;
   }
}
