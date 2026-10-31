package dev.jilaton.client.music;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Owner-only storage for the Spotify refresh token. On filesystems with POSIX permissions the token and its
 * temporary sibling are created 600 (read/write for the owner only); elsewhere the helper is a no-op and the
 * platform's default profile ACLs apply. Nothing here ever prints the token value.
 */
public final class SpotifyTokenPolicy {
    /** 600: the owner may read and write, group and others get nothing. */
    public static final Set<PosixFilePermission> OWNER_ONLY =
            Set.copyOf(PosixFilePermissions.fromString("rw-------"));

    private SpotifyTokenPolicy() { }

    /** Whether the default filesystem exposes POSIX permissions (Linux, macOS, most Unix filesystems). */
    public static boolean posixSupported() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    /** Writes {@code content} so that no intermediate file is readable by another local account where possible. */
    public static void writeOwnerOnly(Path file, String content) throws IOException {
        if (!posixSupported()) {
            Files.writeString(file, content);
            return;
        }
        // Create the file already at 600 rather than writing first and narrowing afterwards.
        Files.deleteIfExists(file);
        Files.createFile(file, PosixFilePermissions.asFileAttribute(OWNER_ONLY));
        Files.writeString(file, content, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /** Narrows an existing token file to owner-only; no-op on filesystems without POSIX permissions. */
    public static void secure(Path file) {
        if (file == null || !posixSupported()) return;
        try {
            Files.setPosixFilePermissions(file, OWNER_ONLY);
        } catch (IOException | UnsupportedOperationException ignored) {
            // Best effort: a read-only or exotic filesystem must not break music playback or disconnect.
        }
    }

    /** Whether the file (if present) is owner-only; true on platforms without POSIX permissions. */
    public static boolean isOwnerOnly(Path file) throws IOException {
        if (!posixSupported()) return true;
        return Files.getPosixFilePermissions(file).equals(OWNER_ONLY);
    }

    /** The atomic-write sibling that must be deleted together with the token. */
    public static Path temporarySibling(Path tokenFile) {
        return tokenFile.resolveSibling(tokenFile.getFileName() + ".tmp");
    }

    /** Deletes the token and any leftover temporary file; returns true when a token existed. */
    public static boolean deleteTokenFiles(Path tokenFile) throws IOException {
        boolean existed = Files.deleteIfExists(tokenFile);
        Files.deleteIfExists(temporarySibling(tokenFile));
        return existed;
    }
}
