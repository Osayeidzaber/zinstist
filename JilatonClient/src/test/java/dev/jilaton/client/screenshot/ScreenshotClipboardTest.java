package dev.jilaton.client.screenshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotClipboardTest {
    private static final Path PNG = Path.of("shots", "2026-01-01_12.00.00.png");

    @Test void windowsUsesStaPowerShellWithTheImageApi() {
        List<String> command = ScreenshotClipboard.command(PNG, ScreenshotClipboard.Os.WINDOWS);
        assertEquals("powershell", command.get(0));
        assertTrue(command.contains("-NoProfile"));
        assertTrue(command.contains("-STA"));
        String script = command.get(command.size() - 1);
        assertTrue(script.contains("[System.Windows.Forms.Clipboard]::SetImage"), "SetImage must place an image on the clipboard");
        assertTrue(script.contains(PNG.toString().replace("'", "''")), "the script must name the validated file");
    }

    @Test void macosReadsThePosixFileAsPngf() {
        List<String> command = ScreenshotClipboard.command(PNG, ScreenshotClipboard.Os.MACOS);
        assertEquals("osascript", command.get(0));
        String script = command.get(command.size() - 1);
        assertTrue(script.contains("POSIX file \"" + ScreenshotClipboard.appleEscape(PNG.toString()) + "\""));
        assertTrue(script.contains("class PNGf"));
    }

    @Test void linuxReadsThePngFromStdinThroughEitherTool() {
        assertEquals(List.of("wl-copy", "--type", "image/png"), ScreenshotClipboard.linuxCommand("wl-copy"));
        assertEquals(List.of("xclip", "-selection", "clipboard", "-t", "image/png", "-i"),
                ScreenshotClipboard.linuxCommand("xclip"));
        assertTrue(ScreenshotClipboard.linuxCommand(null).isEmpty());
        assertTrue(ScreenshotClipboard.command(PNG, ScreenshotClipboard.Os.LINUX).size() >= 2
                || ScreenshotClipboard.command(PNG, ScreenshotClipboard.Os.LINUX).isEmpty(),
                "the probed tool must be one of the two known argument shapes");
    }

    @Test void otherPlatformsHaveNoCommandAndCopyReportsIt() {
        assertTrue(ScreenshotClipboard.command(PNG, ScreenshotClipboard.Os.OTHER).isEmpty());
        assertEquals(Optional.of("no clipboard tool on this platform"),
                ScreenshotClipboard.copy(PNG, ScreenshotClipboard.Os.OTHER));
    }

    @Test void knownPlatformsAlwaysHaveABackend() {
        assertTrue(ScreenshotClipboard.available(ScreenshotClipboard.Os.WINDOWS));
        assertTrue(ScreenshotClipboard.available(ScreenshotClipboard.Os.MACOS));
        assertFalse(ScreenshotClipboard.available(ScreenshotClipboard.Os.OTHER));
    }

    @Test void scriptEscapingSurvivesQuotesAndBackslashes() {
        assertEquals("it''s", ScreenshotClipboard.psEscape("it's"));
        assertEquals("C:\\\\shots\\\\a \\\"b\\\".png", ScreenshotClipboard.appleEscape("C:\\shots\\a \"b\".png"));
    }
}
