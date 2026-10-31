package dev.jilaton.client.render.character;

import dev.jilaton.client.render.character.kit.BakedDesign;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** .baked/.anc stamping, invalidation and pruning, all against temp folders. */
class CharacterCacheLifecycleTest {
    @TempDir
    Path dropFolder;

    @Test
    void theStampCoversNameSizeMtimeAndBakeVersion() {
        assertEquals("hero" + 10L + 1234L + CustomCharacters.BAKE_VERSION,
            CustomCharacters.stamp("hero", 10L, 1234L));

        String base = CustomCharacters.cacheId("hero", 10L, 1234L);
        assertEquals(base, CustomCharacters.cacheId("hero", 10L, 1234L), "the id must be stable for one stamp");
        assertNotEquals(base, CustomCharacters.cacheId("hero2", 10L, 1234L), "a rename must change the id");
        assertNotEquals(base, CustomCharacters.cacheId("hero", 11L, 1234L), "a size change must change the id");
        assertNotEquals(base, CustomCharacters.cacheId("hero", 10L, 1235L), "an mtime change must change the id");
        assertTrue(base.matches("custom_[a-z0-9_]+_[0-9a-f]+"), "unexpected cache id: " + base);
    }

    @Test
    void replacingOrRenamingAModelNeverReusesTheOldBake() throws IOException {
        Path source = dropFolder.resolve("hero.glb");
        Files.write(source, new byte[]{1, 2, 3});
        BakedDesign first = CustomCharacters.scan(dropFolder).get(0);
        String firstId = first.id();

        Path oldBake = dropFolder.resolve(".baked").resolve(firstId + ".anc");
        Files.createDirectories(oldBake.getParent());
        Files.write(oldBake, new byte[]{9});

        Files.write(source, new byte[]{1, 2, 3, 4});
        Files.setLastModifiedTime(source, FileTime.fromMillis(Files.getLastModifiedTime(source).toMillis() + 5000L));
        BakedDesign replaced = CustomCharacters.scan(dropFolder).get(0);
        assertNotEquals(firstId, replaced.id(), "a replaced model must get a fresh cache id");
        assertFalse(Files.exists(oldBake), "the stale .anc of the replaced model must be pruned");

        Path renamed = dropFolder.resolve("hero_redux.glb");
        Files.move(source, renamed, StandardCopyOption.REPLACE_EXISTING);
        BakedDesign renamedDesign = CustomCharacters.scan(dropFolder).get(0);
        assertEquals("hero_redux", renamedDesign.name());
        assertNotEquals(replaced.id(), renamedDesign.id(), "a renamed model must get a fresh cache id");
        assertFalse(Files.exists(oldBake));
    }

    @Test
    void pruningKeepsTheCurrentBakeAndUnrelatedFiles() throws IOException {
        Path source = dropFolder.resolve("hero.glb");
        Files.write(source, new byte[]{1, 2, 3});
        BakedDesign design = CustomCharacters.scan(dropFolder).get(0);

        Path cache = dropFolder.resolve(".baked");
        Files.createDirectories(cache);
        Path current = cache.resolve(design.id() + ".anc");
        Path stale = cache.resolve("custom_gone_0badf00d.anc");
        Path notes = cache.resolve("notes.txt");
        Files.write(current, new byte[]{1});
        Files.write(stale, new byte[]{2});
        Files.write(notes, new byte[]{3});

        assertEquals(design.id(), CustomCharacters.scan(dropFolder).get(0).id(), "an untouched file keeps its id");
        assertTrue(Files.exists(current), "the live bake must survive pruning");
        assertFalse(Files.exists(stale), "an .anc that matches no current model must be pruned");
        assertTrue(Files.exists(notes), "pruning only owns .anc files");
    }

    @Test
    void anUnavailableFolderIsEmptyAndNeverThrows() throws IOException {
        assertTrue(CustomCharacters.scan(null).isEmpty());
        assertTrue(CustomCharacters.unsupportedGltf(null).isEmpty());

        Path fileInsteadOfFolder = dropFolder.resolve("not_a_folder.txt");
        Files.write(fileInsteadOfFolder, new byte[]{1});
        assertTrue(CustomCharacters.scan(fileInsteadOfFolder).isEmpty(), "a regular file is not a drop folder");
        assertTrue(CustomCharacters.unsupportedGltf(fileInsteadOfFolder).isEmpty());

        Path missing = dropFolder.resolve("missing");
        assertTrue(CustomCharacters.scan(missing).isEmpty());
        assertTrue(CustomCharacters.unsupportedGltf(missing).isEmpty());
        assertFalse(Files.exists(missing.resolve(".baked")), "scanning must not create a cache for a missing folder");
    }

    @Test
    void scanningDoesNotCreateACacheUntilAModelIsOpened() throws IOException {
        Files.write(dropFolder.resolve("hero.glb"), new byte[]{1, 2, 3});
        List<BakedDesign> designs = CustomCharacters.scan(dropFolder);
        assertEquals(1, designs.size());
        assertFalse(Files.exists(dropFolder.resolve(".baked")), "scan is lazy: it must not bake or cache");
    }
}
