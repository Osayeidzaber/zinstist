package dev.jilaton.client.screenshot;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The chat side of the screenshot actions: vanilla's "Saved screenshot as …" message gains "[Copy]" and
 * "[Open]" buttons, the copy click arrives as a {@link ClickEvent.Custom} that the mixin intercepts before
 * vanilla could forward it to the server, and the copy itself runs on a worker thread with the feedback
 * posted back through {@link Minecraft#execute}.
 */
public final class ScreenshotChatActions {
    public static final Identifier COPY_ID = Identifier.fromNamespaceAndPath("jilaton", "screenshot/copy");

    private ScreenshotChatActions() { }

    /** Wraps the vanilla screenshot message consumer so the delivered message gains the action buttons. */
    public static Consumer<Component> decorating(Consumer<Component> consumer) {
        return message -> consumer.accept(decorate(message));
    }

    /** {@code message} with "  [Copy]  [Open]" appended, or the message itself when it is not a real screenshot. */
    public static Component decorate(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return message;
        return decorate(message, minecraft.gameDirectory.toPath().resolve("screenshots"));
    }

    /** A forged chat message may name any directory; only the game's own screenshots folder gets buttons. */
    static Component decorate(Component message, Path screenshotsDir) {
        return decorate(message, screenshotsDir, ScreenshotClipboard.available());
    }

    static Component decorate(Component message, Path screenshotsDir, boolean clipboard) {
        Path file = screenshotFile(message);
        if (file == null || screenshotsDir == null
                || !file.getParent().equals(screenshotsDir.normalize())) return message;
        MutableComponent decorated = message.copy();
        if (clipboard) {
            decorated.append("  ").append(button("[Copy]", new ClickEvent.Custom(COPY_ID,
                    Optional.of(StringTag.valueOf(file.getFileName().toString())))));
        }
        decorated.append("  ").append(button("[Open]", new ClickEvent.OpenFile(file.toFile())));
        return decorated;
    }

    private static MutableComponent button(String label, ClickEvent event) {
        return Component.literal(label).withStyle(ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(event));
    }

    /** The saved .png behind a vanilla success message, or null for any other message. */
    static Path screenshotFile(Component message) {
        if (!(message.getContents() instanceof TranslatableContents contents)) return null;
        if (!"screenshot.success".equals(contents.getKey()) || contents.getArgs().length != 1) return null;
        if (!(contents.getArgs()[0] instanceof Component argument)) return null;
        if (!(argument.getStyle().getClickEvent() instanceof ClickEvent.OpenFile open)) return null;
        Path file = open.file().toPath();
        return ScreenshotPaths.resolve(file.getFileName().toString(), file.getParent()).orElse(null);
    }

    /** Runs the copy for a custom click on our id: validates, works off-thread, reports back on the client thread. */
    public static void handleCopy(Minecraft minecraft, ClickEvent.Custom event) {
        if (!COPY_ID.equals(event.id())) return;
        if (!(event.payload().orElse(null) instanceof StringTag tag)) return;
        Consumer<Component> feedback = message -> minecraft.execute(() -> minecraft.gui.getChat().addMessage(message));
        Path file = ScreenshotPaths.resolve(tag.value(), minecraft.gameDirectory.toPath().resolve("screenshots")).orElse(null);
        if (file == null) {
            feedback.accept(Component.literal("Copy failed: " + tag.value() + " is not a screenshot"));
            return;
        }
        if (!ScreenshotClipboard.available()) {
            feedback.accept(Component.literal("Copy failed: no clipboard tool on this platform"));
            return;
        }
        Thread worker = new Thread(() -> feedback.accept(ScreenshotClipboard.copy(file).map(
                failure -> (Component) Component.literal("Copy failed: " + failure))
                .orElseGet(() -> Component.literal("Copied screenshot"))), "jilaton-screenshot-copy");
        worker.setDaemon(true);
        worker.start();
    }
}
