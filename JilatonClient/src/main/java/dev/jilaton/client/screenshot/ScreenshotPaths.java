package dev.jilaton.client.screenshot;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Guards the file name a chat action may act on: only a plain .png name that lands directly inside the
 * screenshots directory resolves, so a crafted chat message can never point Copy or Open at another file.
 */
public final class ScreenshotPaths {
    /** One safe path segment: no separators, no traversal, no device prefixes; must end in .png. */
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 ._()\\[\\]-]*\\.png");

    private ScreenshotPaths() { }

    /** The file inside {@code screenshotsDir} for {@code name}, or empty when the name is not a plain .png in it. */
    public static Optional<Path> resolve(String name, Path screenshotsDir) {
        if (name == null || screenshotsDir == null || !NAME.matcher(name).matches()) return Optional.empty();
        try {
            Path root = screenshotsDir.normalize();
            Path resolved = root.resolve(name).normalize();
            if (!resolved.getParent().equals(root)) return Optional.empty();
            return Optional.of(resolved);
        } catch (InvalidPathException | UnsupportedOperationException exception) {
            return Optional.empty();
        }
    }
}
