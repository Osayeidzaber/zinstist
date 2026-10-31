package dev.jilaton.client.screenshot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The [Open] decision per platform. Paths are argument-list entries, never shell text, so spaces and
 * brackets in a screenshot name cannot split or break the command.
 */
class ScreenshotActionRevealTest {
    @TempDir
    Path directory;

    @Test
    void windowsSelectsTheFileInExplorer() {
        Path png = directory.resolve("shot.png");
        List<String> command = ScreenshotReveal.command(png, ScreenshotClipboard.Os.WINDOWS);
        assertEquals(List.of("explorer.exe", "/select,", png.toAbsolutePath().toString()), command);
    }

    @Test
    void macosAndLinuxHandTheFileToTheirFileManager() {
        Path png = directory.resolve("shot.png");
        assertEquals(List.of("open", "-R", png.toAbsolutePath().toString()),
                ScreenshotReveal.command(png, ScreenshotClipboard.Os.MACOS));
        assertEquals(List.of("xdg-open", png.toAbsolutePath().toString()),
                ScreenshotReveal.command(png, ScreenshotClipboard.Os.LINUX));
        assertTrue(ScreenshotReveal.command(png, ScreenshotClipboard.Os.OTHER).isEmpty());
    }

    @Test
    void aPathWithSpacesStaysASingleArgument() {
        Path png = directory.resolve("school trip").resolve("shot (1).png");
        for (ScreenshotClipboard.Os os : List.of(ScreenshotClipboard.Os.WINDOWS,
                ScreenshotClipboard.Os.MACOS, ScreenshotClipboard.Os.LINUX)) {
            List<String> command = ScreenshotReveal.command(png, os);
            assertEquals(png.toAbsolutePath().toString(), command.get(command.size() - 1),
                    os + " must pass the path as one argument");
            assertTrue(command.stream().noneMatch(argument -> argument.contains("\"")),
                    os + " must not wrap the path in quotes");
        }
    }
}
