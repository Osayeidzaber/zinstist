package dev.jilaton.client.screenshot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotPathsTest {
    @TempDir
    Path dir;

    @Test void vanillaTimestampedNamesResolveInsideTheScreenshotsDirectory() {
        Optional<Path> resolved = ScreenshotPaths.resolve("2026-01-01_12.00.00.png", dir);
        assertTrue(resolved.isPresent());
        assertEquals(dir.resolve("2026-01-01_12.00.00.png").normalize(), resolved.get());
    }

    @Test void plainNamesWithSpacesAndBracketsAreAccepted() {
        assertTrue(ScreenshotPaths.resolve("school trip (1).png", dir).isPresent());
        assertTrue(ScreenshotPaths.resolve("shot[2].png", dir).isPresent());
    }

    @Test void traversalAndSeparatorsAreRejected() {
        assertTrue(ScreenshotPaths.resolve("../secret.png", dir).isEmpty());
        assertTrue(ScreenshotPaths.resolve("sub/inner.png", dir).isEmpty());
        assertTrue(ScreenshotPaths.resolve("sub\\inner.png", dir).isEmpty());
        assertTrue(ScreenshotPaths.resolve("a/../b.png", dir).isEmpty());
    }

    @Test void absoluteAndDevicePathsAreRejectedByTheNamePattern() {
        assertTrue(ScreenshotPaths.resolve("C:\\evil.png", dir).isEmpty());
        assertTrue(ScreenshotPaths.resolve("C:evil.png", dir).isEmpty());
    }

    @Test void onlyPngImagesPass() {
        assertTrue(ScreenshotPaths.resolve("notes.txt", dir).isEmpty());
        assertTrue(ScreenshotPaths.resolve("notes.PNG", dir).isEmpty());
        assertTrue(ScreenshotPaths.resolve(".png", dir).isEmpty());
        assertTrue(ScreenshotPaths.resolve("shot.png.exe", dir).isEmpty());
    }

    @Test void malformedInputIsRejectedNotThrown() {
        assertTrue(ScreenshotPaths.resolve(null, dir).isEmpty());
        assertTrue(ScreenshotPaths.resolve("", dir).isEmpty());
        assertTrue(ScreenshotPaths.resolve("shot.png", null).isEmpty());
        assertTrue(ScreenshotPaths.resolve("bad\0name.png", dir).isEmpty());
    }
}
