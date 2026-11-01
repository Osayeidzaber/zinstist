package dev.jilaton.client.render.character;

import dev.jilaton.client.render.character.kit.Bone;
import dev.jilaton.client.render.character.kit.CharacterBuilder;
import dev.jilaton.client.render.character.kit.CharacterDesign;
import dev.jilaton.client.render.character.kit.CharacterModel;
import dev.jilaton.client.render.character.kit.Detail;
import dev.jilaton.client.render.character.kit.PartMesh;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every built-in character must bake into a posable model: the finisher and its crowd can pick any of them. */
class AllCharactersBuildTest {
    @Test
    void everyBuiltInCharacterBuildsWithLimbsAndGeometry() throws Exception {
        for (CharacterDesign design : CharacterCatalog.all()) {
            CharacterModel model = CharacterBuilder.build(design, Detail.MEDIUM);
            assertTrue(model.triangles() > 100, design.name() + " has geometry");
            assertTrue(model.scale > 0, design.name() + " scale");
            // Some designs (Tung Tung Sahur) are one trunk with no separate torso; arms are what the moves need.
            assertTrue(model.part(Bone.BODY) != null || model.part(Bone.HEAD) != null, design.name() + " has a trunk");
            for (Bone bone : new Bone[]{Bone.RIGHT_ARM, Bone.LEFT_ARM}) {
                assertNotNull(model.part(bone), design.name() + " has " + bone);
            }
            for (PartMesh part : model.parts) {
                if (part.extra) assertTrue(part.parent >= 0 && part.parent < model.parts.length, design.name() + " " + part.name);
            }
        }
    }
}
