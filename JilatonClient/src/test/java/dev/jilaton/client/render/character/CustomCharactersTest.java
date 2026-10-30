package dev.jilaton.client.render.character;

import dev.jilaton.client.render.character.kit.BakedDesign;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CustomCharactersTest {
    @Test
    void folderLookupIsSafeWithoutAClient() {
        Path folder = assertDoesNotThrow(CustomCharacters::folder, "folder() must not throw outside a running client");
        if (folder != null) {
            assertTrue(folder.endsWith("characters"), "unexpected drop folder: " + folder);
        }
    }

    @Test
    void scanIsEmptyWithoutAConfigDirectory() {
        assumeTrue(CustomCharacters.folder() == null, "a config directory exists, so the null branch cannot be reached");
        List<BakedDesign> designs = assertDoesNotThrow(() -> CustomCharacters.scan(), "scan() must not throw without a config dir");
        assertNotNull(designs);
        assertTrue(designs.isEmpty(), "no folder should mean no custom designs");
    }

    @Test
    void ensureFolderIsANoOpWithoutAConfigDirectory() {
        assumeTrue(CustomCharacters.folder() == null, "a config directory exists, so the null branch cannot be reached");
        Path created = assertDoesNotThrow(() -> CustomCharacters.ensureFolder(), "there is nowhere to create the folder");
        assertNull(created, "there is nowhere to create the folder");
    }

    @Test
    void catalogRefreshRunsHeadlessly() {
        boolean changed = assertDoesNotThrow(CharacterCatalog::refresh, "refresh() must survive a headless scan");
        assertFalse(changed, "a catalog with no custom designs should not report a change");
        assertTrue(CharacterCatalog.all().size() >= 9, "the built-in designs must survive a scan");
    }
}
