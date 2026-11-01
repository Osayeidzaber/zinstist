package dev.jilaton.client.render.character;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import dev.jilaton.client.render.character.kit.CharacterBuilder;
import dev.jilaton.client.render.character.kit.CharacterDesign;
import dev.jilaton.client.render.character.kit.CharacterModel;
import dev.jilaton.client.render.character.kit.Detail;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class CharacterCache {
   private static final Logger LOGGER = LoggerFactory.getLogger("Jilaton Characters");
   private static final int KEEP = 12;
   private static final ForkJoinPool POOL = new ForkJoinPool(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 2)), pool -> {
      ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
      thread.setName("Jilaton Character Builder " + thread.getPoolIndex());
      thread.setDaemon(true);
      thread.setPriority(4);
      return thread;
   }, null, false);
   private static final Map<String, CharacterCache.Entry> ENTRIES = new LinkedHashMap<>(8, 0.75F, true);
   private static boolean listening;
   private static CharacterCache.Entry shown;

   private CharacterCache() {
   }

   static void listen() {
      if (!listening) {
         listening = true;
         ResourceLoader.get(PackType.CLIENT_RESOURCES)
            .registerReloader(Identifier.fromNamespaceAndPath("jilaton", "character_textures"), (ResourceManagerReloadListener)manager -> releaseTextures());
      }
   }

   static void prepare(CharacterDesign design, Detail detail) {
      String key = design.id() + "/" + detail.name();
      if (!ENTRIES.containsKey(key)) {
         CharacterCache.Entry created = new CharacterCache.Entry(design, detail);
         ENTRIES.put(key, created);
         trim();
         CompletableFuture.runAsync(() -> build(created), POOL);
      }
   }

   static WornCharacter ready(CharacterDesign design, Detail detail) {
      String key = design.id() + "/" + detail.name();
      CharacterCache.Entry entry = ENTRIES.get(key);
      if (entry == null) {
         CharacterCache.Entry created = new CharacterCache.Entry(design, detail);
         ENTRIES.put(key, created);
         trim();
         CompletableFuture.runAsync(() -> build(created), POOL);
         return previous();
      }

      if (entry.worn != null) {
         shown = entry;
         return entry.worn;
      }

      CharacterModel model = entry.model;
      if (entry.failed) {
         return null;
      }

      if (model == null) {
         return previous();
      }

      Minecraft.getInstance()
         .getTextureManager()
         .register(entry.texture, new CharacterTexture(entry.texture.toString(), model.atlasWidth, model.atlasHeight, model.atlasMips));
      entry.worn = new WornCharacter(model, entry.texture);
      shown = entry;
      return entry.worn;
   }

   /** Like {@link #ready} but never substitutes another character while this one is still baking. */
   static WornCharacter exact(CharacterDesign design, Detail detail) {
      CharacterCache.Entry entry = ENTRIES.get(design.id() + "/" + detail.name());
      if (entry == null) {
         prepare(design, detail);
         return null;
      }
      if (entry.failed || entry.model == null && entry.worn == null) {
         return null;
      }
      CharacterCache.Entry before = shown;
      WornCharacter worn = ready(design, detail);
      shown = before;
      return worn;
   }

   private static WornCharacter previous() {
      CharacterCache.Entry last = shown;
      return last != null && ENTRIES.containsValue(last) ? last.worn : null;
   }

   private static void build(CharacterCache.Entry entry) {
      try {
         CharacterModel model = CharacterBuilder.build(entry.design, entry.detail);
         LOGGER.info(
            "[Characters] 3D Character '{}' ({}) built: {} triangles in {} ms",
            new Object[]{entry.design.name(), entry.detail.label, model.triangles(), model.buildMillis}
         );
         entry.model = model;
      } catch (Throwable failure) {
         entry.failed = true;
         LOGGER.error("[Characters] 3D Character '{}' ({}) failed to build", new Object[]{entry.design.name(), entry.detail.label, failure});
         CharacterModule.failed(entry.design, failure);
      }
   }

   private static void trim() {
      Iterator<CharacterCache.Entry> it = ENTRIES.values().iterator();

      while (ENTRIES.size() > KEEP && it.hasNext()) {
         CharacterCache.Entry eldest = it.next();
         release(eldest);
         it.remove();
      }
   }

   static void releaseTextures() {
      for (CharacterCache.Entry entry : ENTRIES.values()) {
         release(entry);
      }
   }

   static void clear() {
      releaseTextures();
      ENTRIES.clear();
      shown = null;
   }

   private static void release(CharacterCache.Entry entry) {
      if (entry.worn != null) {
         entry.worn = null;
         Minecraft.getInstance().getTextureManager().release(entry.texture);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Entry {
      final CharacterDesign design;
      final Detail detail;
      final Identifier texture;
      volatile CharacterModel model;
      volatile boolean failed;
      WornCharacter worn;

      Entry(CharacterDesign design, Detail detail) {
         this.design = design;
         this.detail = detail;
         this.texture = Identifier.fromNamespaceAndPath("jilaton", "character/" + design.id() + "_" + detail.name().toLowerCase(Locale.ROOT));
      }
   }
}
