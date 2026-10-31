package dev.jilaton.client.render.character;

import dev.jilaton.client.render.character.kit.CharacterBuilder;
import dev.jilaton.client.render.character.kit.CharacterDesign;
import dev.jilaton.client.render.character.kit.CharacterModel;
import dev.jilaton.client.render.character.kit.Detail;
import dev.jilaton.client.render.character.kit.PartMesh;
import dev.jilaton.client.render.character.kit.Surface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterResolutionTest {
    private static List<CharacterModel> models;

    @BeforeAll
    static void buildEveryDesign() {
        models = new ArrayList<>();
        for (CharacterDesign design : CharacterCatalog.all()) {
            models.add(CharacterBuilder.build(design, Detail.LOW));
        }
    }

    @Test
    void everyCatalogDesignResolvesToRealGeometry() {
        assertEquals(CharacterCatalog.all().size(), models.size());
        assertEquals(9, models.size(), "the catalog should still list nine designs");
        for (CharacterModel model : models) {
            assertTrue(model.parts.length > 0, model.id + " resolved to no parts");
            assertTrue(model.triangles() > 0, model.id + " resolved to no triangles");
            assertTrue(model.maxY > model.minY, model.id + " has no height");
            assertVertexCount(model);
        }
    }

    @Test
    void everyCatalogDesignShipsATextureAtlas() {
        for (CharacterModel model : models) {
            assertTrue(model.atlasWidth > 0, model.id + " has no atlas width");
            assertTrue(model.atlasHeight > 0, model.id + " has no atlas height");
            assertTrue(model.atlasMips.length > 1, model.id + " has no mip chain");
            assertEquals(
                model.atlasWidth * model.atlasHeight,
                model.atlasMips[0].length,
                model.id + " has a mip that does not match its atlas size"
            );
        }
    }

    @Test
    void bakedDesignsComeFromTheBundledAncAssets() {
        for (String id : List.of("cj", "neighbor")) {
            CharacterModel model = models.stream().filter(m -> m.id.equals(id)).findFirst().orElse(null);
            assertNotNull(model, id + " is listed but never resolved");
            assertTrue(model.triangles() > 0, id + " baked to no triangles");
            assertTrue(model.atlasWidth > 0 && model.atlasHeight > 0, id + " has no atlas");
        }
    }

    @Test
    void partsUseKnownBonesAndFinitePivots() {
        for (CharacterModel model : models) {
            for (PartMesh part : model.parts) {
                assertNotNull(part.bone, model.id + " has a part without a bone");
                assertTrue(Float.isFinite(part.pivotX) && Float.isFinite(part.pivotY) && Float.isFinite(part.pivotZ),
                    model.id + "/" + part.name + " has a non-finite pivot");
                assertTrue(Float.isFinite(part.jointX) && Float.isFinite(part.jointY) && Float.isFinite(part.jointZ),
                    model.id + "/" + part.name + " has a non-finite joint");
            }
        }
    }

    private static void assertVertexCount(CharacterModel model) {
        int vertices = 0;
        for (PartMesh part : model.parts) {
            for (Surface surface : new Surface[]{part.base, part.outline, part.decals}) {
                if (surface == null) {
                    continue;
                }
                vertices += surface.vertexCount();
                for (float value : surface.pos) {
                    assertTrue(Float.isFinite(value), model.id + "/" + part.name + " has a non-finite vertex");
                }
            }
        }
        assertTrue(vertices > 0, model.id + " has no vertices");
    }
}
