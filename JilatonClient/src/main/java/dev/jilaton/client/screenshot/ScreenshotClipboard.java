package dev.jilaton.client.screenshot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * OS-native "put this image on the clipboard". Every command is a ProcessBuilder argument list with the
 * path baked into a script argument — never a shell string — output is bounded and the tool is killed
 * after ten seconds. Execution is synchronous; {@code ScreenshotChatActions} runs it off the render
 * thread and posts the feedback message back on the client thread.
 */
public final class ScreenshotClipboard {
    public enum Os { WINDOWS, MACOS, LINUX, OTHER }

    private static final int TIMEOUT_SECONDS = 10;
    private static final int MAX_OUTPUT = 4096;

    private ScreenshotClipboard() { }

    public static Os os() {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (name.contains("win")) return Os.WINDOWS;
        if (name.contains("mac") || name.contains("darwin")) return Os.MACOS;
        if (name.contains("nux") || name.contains("nix") || name.contains("aix")) return Os.LINUX;
        return Os.OTHER;
    }

    /** Whether this machine has a clipboard backend, which decides whether the message gains a [Copy] button. */
    public static boolean available() {
        return available(os());
    }

    static boolean available(Os os) {
        return switch (os) {
            case WINDOWS, MACOS -> true;
            case LINUX -> linuxTool() != null;
            case OTHER -> false;
        };
    }

    /** wl-copy is preferred under Wayland, xclip is the X11 fallback. */
    private static String linuxTool() {
        for (String tool : List.of("wl-copy", "xclip")) {
            for (String dir : System.getenv("PATH").split(File.pathSeparator)) {
                if (dir.isEmpty()) continue;
                if (Files.isExecutable(Path.of(dir, tool))) return tool;
            }
        }
        return null;
    }

    /**
     * The argument list that copies {@code png}. Linux tools read the PNG from stdin, so the caller
     * redirects the file into the process; the other platforms receive the path inside the script.
     */
    public static List<String> command(Path png, Os os) {
        return switch (os) {
            case WINDOWS -> List.of("powershell", "-NoProfile", "-STA", "-Command", windowsScript(png));
            case MACOS -> List.of("osascript", "-e", macosScript(png));
            case LINUX -> linuxCommand(linuxTool());
            case OTHER -> List.of();
        };
    }

    static List<String> linuxCommand(String tool) {
        if ("wl-copy".equals(tool)) return List.of("wl-copy", "--type", "image/png");
        if ("xclip".equals(tool)) return List.of("xclip", "-selection", "clipboard", "-t", "image/png", "-i");
        return List.of();
    }

    static String windowsScript(Path png) {
        String file = psEscape(png.toString());
        return "Add-Type -AssemblyName System.Windows.Forms; "
                + "Add-Type -AssemblyName System.Drawing; "
                + "$bytes=[IO.File]::ReadAllBytes('" + file + "'); "
                + "$stream=New-Object IO.MemoryStream(,$bytes); "
                + "$image=[System.Drawing.Image]::FromStream($stream); "
                + "[System.Windows.Forms.Clipboard]::SetImage($image); "
                + "$image.Dispose(); $stream.Dispose()";
    }

    static String macosScript(Path png) {
        return "set the clipboard to (read (POSIX file \"" + appleEscape(png.toString())
                + "\") as \u00abclass PNGf\u00bb)";
    }

    /** PowerShell single-quoted strings escape a quote by doubling it. */
    static String psEscape(String value) {
        return value.replace("'", "''");
    }

    /** AppleScript double-quoted strings escape backslashes and quotes. */
    static String appleEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Copies the PNG; empty on success, otherwise the reason the clipboard tool refused. */
    public static Optional<String> copy(Path png) {
        return copy(png, os());
    }

    static Optional<String> copy(Path png, Os os) {
        List<String> args = command(png, os);
        if (args.isEmpty()) return Optional.of("no clipboard tool on this platform");
        try {
            ProcessBuilder builder = new ProcessBuilder(args);
            builder.redirectErrorStream(true);
            if (os == Os.LINUX) builder.redirectInput(png.toFile());
            Process process = builder.start();
            byte[] output;
            try (InputStream stream = process.getInputStream()) {
                output = stream.readNBytes(MAX_OUTPUT + 1);
            }
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.of("clipboard tool timed out");
            }
            if (process.exitValue() != 0) {
                String reason = new String(output, StandardCharsets.UTF_8).strip();
                if (reason.length() > MAX_OUTPUT) reason = reason.substring(0, MAX_OUTPUT);
                return Optional.of(reason.isEmpty()
                        ? "clipboard tool exit code " + process.exitValue()
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
