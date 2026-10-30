package dev.jilaton.client.render.character;

import dev.jilaton.client.render.character.kit.CharacterDesign;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterCatalogTest {
    private static final List<String> BAKED = List.of("cj", "neighbor");

    @Test
    void catalogListsEveryBuiltInDesign() {
        List<CharacterDesign> designs = CharacterCatalog.all();
        assertEquals(9, designs.size(), "the catalog should expose the seven rigged designs plus two baked models");
        List<String> ids = new ArrayList<>();
        for (CharacterDesign design : designs) {
            ids.add(design.id());
        }
        assertTrue(ids.contains("tung_tung_sahur"), "tung tung sahur is missing");
        assertTrue(ids.containsAll(BAKED), "the baked models are missing");
    }

    @Test
    void designIdsAreUnique() {
        Set<String> seen = new HashSet<>();
        for (CharacterDesign design : CharacterCatalog.all()) {
            assertTrue(seen.add(design.id()), "duplicate character id: " + design.id());
        }
    }

    @Test
    void bakedModelResourcesExistAndAreNotEmpty() throws IOException {
        for (String id : BAKED) {
            String path = "/assets/jilaton/characters/" + id + ".anc";
            try (InputStream in = CharacterCatalogTest.class.getResourceAsStream(path)) {
                assertNotNull(in, path + " is not on the classpath");
                long total = 0;
                byte[] buffer = new byte[8192];
                for (int read = in.read(buffer); read >= 0; read = in.read(buffer)) {
                    total += read;
                }
                assertTrue(total > 0, path + " is empty");
            }
        }
    }
}
