package dev.jilaton.client.render.character;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface CharacterHolder {
   WornCharacter jilaton$character();

   void jilaton$setCharacter(WornCharacter var1);

   float jilaton$seat();

   void jilaton$setSeat(float var1);
}
