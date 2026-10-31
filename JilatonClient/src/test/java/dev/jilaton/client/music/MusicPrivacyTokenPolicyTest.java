package dev.jilaton.client.music;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The refresh token is only ever stored owner-only and never left behind after disconnect. */
class MusicPrivacyTokenPolicyTest {
    @TempDir
    Path directory;

    @Test
    void theOwnerOnlySetIsSixHundred() {
        assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                SpotifyTokenPolicy.OWNER_ONLY);
        assertEquals("rw-------", PosixFilePermissions.toString(SpotifyTokenPolicy.OWNER_ONLY));
    }

    @Test
    void temporarySiblingAndDisconnectCleanupAreDeterministic() throws Exception {
        Path token = directory.resolve("spotify-token.json");
        Path temporary = SpotifyTokenPolicy.temporarySibling(token);
        assertEquals(token.getParent(), temporary.getParent());
        assertEquals("spotify-token.json.tmp", temporary.getFileName().toString());

        Files.writeString(token, "{\"refresh_token\":\"secret\"}");
        Files.writeString(temporary, "{}");
        assertTrue(SpotifyTokenPolicy.deleteTokenFiles(token));
        assertFalse(Files.exists(token));
        assertFalse(Files.exists(temporary), "the atomic-write sibling must not survive disconnect");
        assertFalse(SpotifyTokenPolicy.deleteTokenFiles(token));
    }

    @Test
    void ownerOnlyWritesCarryTheTokenAndNeverLoseIt() throws Exception {
        Path token = directory.resolve("token.json");
        SpotifyTokenPolicy.writeOwnerOnly(token, "{\"refresh_token\":\"secret\"}");
        assertEquals("{\"refresh_token\":\"secret\"}", Files.readString(token));
        assertTrue(SpotifyTokenPolicy.isOwnerOnly(token));
        if (SpotifyTokenPolicy.posixSupported()) {
            assertTrue(SpotifyTokenPolicy.isOwnerOnly(token), "the token file must be 600");
            // Re-securing a file widened by another tool restores owner-only.
            Files.setPosixFilePermissions(token, PosixFilePermissions.fromString("rw-r--r--"));
            SpotifyTokenPolicy.secure(token);
            assertTrue(SpotifyTokenPolicy.isOwnerOnly(token));
        }
    }

    @Test
    void secureIsSafeForAMissingFileAndNull() {
        SpotifyTokenPolicy.secure(directory.resolve("absent.json"));
        SpotifyTokenPolicy.secure(null);
        assertTrue(Files.notExists(directory.resolve("absent.json")));
    }
}
