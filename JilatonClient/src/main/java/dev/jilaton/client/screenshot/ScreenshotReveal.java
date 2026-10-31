package dev.jilaton.client.screenshot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * OS-native "show me this screenshot in the file manager": Explorer selects the file on Windows, Finder
 * reveals it on macOS and xdg-open opens it on Linux. Every command is a ProcessBuilder argument list with
 * the path as its own argument (never a shell string), so a path containing spaces or brackets stays intact.
 *
 * <p>{@link ScreenshotChatActions} runs {@link #reveal(Path)} on a worker thread and posts the feedback back
 * on the client thread.
 */
public final class ScreenshotReveal {
    private static final int TIMEOUT_SECONDS = 10;
    private static final int MAX_OUTPUT = 4096;

    private ScreenshotReveal() { }

    /** The reveal command for {@code png}; empty when the platform has no file manager integration. */
    public static List<String> command(Path png, ScreenshotClipboard.Os os) {
        String path = png.toAbsolutePath().toString();
        return switch (os) {
            case WINDOWS -> List.of("explorer.exe", "/select,", path);
            case MACOS -> List.of("open", "-R", path);
            case LINUX -> List.of("xdg-open", path);
            case OTHER -> List.of();
        };
    }

    /** Reveals the screenshot; empty on success, otherwise the reason the file manager refused. */
    public static Optional<String> reveal(Path png) {
        return reveal(png, ScreenshotClipboard.os());
    }

    static Optional<String> reveal(Path png, ScreenshotClipboard.Os os) {
        List<String> args = command(png, os);
        if (args.isEmpty()) return Optional.of("no file manager on this platform");
        try {
            ProcessBuilder builder = new ProcessBuilder(args);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            byte[] output;
            try (InputStream stream = process.getInputStream()) {
                output = stream.readNBytes(MAX_OUTPUT + 1);
            }
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.of("file manager timed out");
            }
            // Explorer exits with 1 even when it successfully selects the file, so its status is not a verdict.
            if (process.exitValue() != 0 && os != ScreenshotClipboard.Os.WINDOWS) {
                String reason = new String(output, StandardCharsets.UTF_8).strip();
                if (reason.length() > MAX_OUTPUT) reason = reason.substring(0, MAX_OUTPUT);
                return Optional.of(reason.isEmpty()
                        ? "file manager exit code " + process.exitValue()
                        : reason);
            }
            return Optional.empty();
        } catch (IOException exception) {
            return Optional.of(exception.getMessage() == null ? exception.toString() : exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.of("interrupted");
        }
    }
}
