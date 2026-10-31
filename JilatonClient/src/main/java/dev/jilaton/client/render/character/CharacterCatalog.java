package dev.jilaton.client.render.character;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import dev.jilaton.client.render.character.designs.Aoi;
import dev.jilaton.client.render.character.designs.Luna;
import dev.jilaton.client.render.character.designs.Mika;
import dev.jilaton.client.render.character.designs.Miku;
import dev.jilaton.client.render.character.designs.Sakura;
import dev.jilaton.client.render.character.designs.Tralalero;
import dev.jilaton.client.render.character.designs.TungTungSahur;
import dev.jilaton.client.render.character.kit.BakedDesign;
import dev.jilaton.client.render.character.kit.CharacterDesign;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CharacterCatalog {
   private static final List<CharacterDesign> BUILT_IN = List.of(
      new Sakura(),
      new Luna(),
      new Mika(),
      new Aoi(),
      new Miku(),
      BakedDesign.builtIn("cj", "CJ", "/assets/jilaton/characters/cj.anc"),
      BakedDesign.builtIn("neighbor", "Neighbor", "/assets/jilaton/characters/neighbor.anc"),
      new TungTungSahur(),
      new Tralalero()
   );
   private static volatile List<CharacterDesign> designs = BUILT_IN;

   private CharacterCatalog() {
   }

   public static List<CharacterDesign> all() {
      return designs;
   }

   static boolean refresh() {
      return refresh(CustomCharacters.folder());
   }

   /**
    * Rebuilds the catalogue from an explicit drop folder. Package-private seam used by the tick path (through
    * {@link #refresh()}) and by headless tests, so the public API is unchanged.
    */
   static boolean refresh(Path root) {
      List<CharacterDesign> next = new ArrayList<>(BUILT_IN);
      List<String> taken = new ArrayList<>(names(BUILT_IN));

      for (BakedDesign custom : CustomCharacters.scan(root)) {
         if (!taken.contains(custom.name())) {
            taken.add(custom.name());
            next.add(custom);
         }
      }

      List<CharacterDesign> old = designs;
      designs = List.copyOf(next);
      if (old.size() != next.size()) {
         return true;
      }

      for (int i = 0; i < next.size(); i++) {
         if (!old.get(i).id().equals(next.get(i).id())) {
            return true;
         }
      }

      return false;
   }

   static List<String> names() {
      return names(designs);
   }

   private static List<String> names(List<CharacterDesign> list) {
      List<String> out = new ArrayList<>(list.size());

      for (CharacterDesign design : list) {
         out.add(design.name());
      }

      return out;
   }

   static CharacterDesign byName(String name) {
      List<CharacterDesign> list = designs;

      for (CharacterDesign design : list) {
         if (design.name().equals(name)) {
            return design;
         }
      }

      return list.get(0);
   }
}
