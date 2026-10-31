package dev.jilaton.client.screenshot;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotChatActionsTest {
    @TempDir
    Path dir;

    /** The exact shape vanilla delivers: translatable with one underlined filename argument. */
    private Component vanillaMessage(Path png) {
        MutableComponent argument = Component.literal(png.getFileName().toString())
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(png.toFile())));
        return Component.translatable("screenshot.success", argument);
    }

    @Test void copyIdIsTheInterceptedIdentifier() {
        assertEquals("jilaton:screenshot/copy", ScreenshotChatActions.COPY_ID.toString());
    }

    @Test void decoratedMessageKeepsTheOriginalAndAppendsBothButtons() {
        Path png = dir.resolve("2026-01-01_12.00.00.png");
        Component decorated = ScreenshotChatActions.decorate(vanillaMessage(png), dir, true);
        assertTrue(decorated.getString().contains("2026-01-01_12.00.00.png"));
        assertTrue(decorated.getString().endsWith("[Copy]  [Open]"));

        List<ClickEvent> events = clickEvents(decorated);
        ClickEvent.Custom copy = (ClickEvent.Custom) events.stream()
                .filter(event -> event instanceof ClickEvent.Custom).findFirst().orElseThrow();
        assertEquals(ScreenshotChatActions.COPY_ID, copy.id());
        assertEquals(Optional.of(StringTag.valueOf("2026-01-01_12.00.00.png")), copy.payload());

        ClickEvent.OpenFile open = (ClickEvent.OpenFile) events.stream()
                .filter(event -> event instanceof ClickEvent.OpenFile).reduce((first, second) -> second).orElseThrow();
        assertEquals(png, open.file().toPath());
    }

    @Test void withoutAClipboardBackendOnlyOpenIsOffered() {
        Path png = dir.resolve("shot.png");
        Component decorated = ScreenshotChatActions.decorate(vanillaMessage(png), dir, false);
        assertTrue(decorated.getString().endsWith("[Open]"));
        assertTrue(clickEvents(decorated).stream().noneMatch(event -> event instanceof ClickEvent.Custom));
    }

    @Test void failureAndForeignMessagesAreReturnedUntouched() {
        Component failure = Component.translatable("screenshot.failure", "disk full");
        assertSame(failure, ScreenshotChatActions.decorate(failure, dir, true));
        Component plain = Component.literal("Hello, world");
        assertSame(plain, ScreenshotChatActions.decorate(plain, dir, true));
        Component noFile = Component.translatable("screenshot.success",
                Component.literal("name.png").withStyle(ChatFormatting.UNDERLINE));
        assertSame(noFile, ScreenshotChatActions.decorate(noFile, dir, true));
    }

    @Test void pathsOutsideTheScreenshotsDirectoryAreNotDecorated() {
        MutableComponent argument = Component.literal("secret.png")
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(Path.of("..", "secret.png").toFile())));
        Component hostile = Component.translatable("screenshot.success", argument);
        assertSame(hostile, ScreenshotChatActions.decorate(hostile, dir));
        Component outside = Component.translatable("screenshot.success", Component.literal("secret.png")
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(dir.getParent().resolve("secret.png").toFile()))));
        assertSame(outside, ScreenshotChatActions.decorate(outside, dir));
    }

    @Test void decoratingWrapsTheConsumerWithoutChangingOtherMessages() {
        List<Component> delivered = new ArrayList<>();
        Consumer<Component> consumer = ScreenshotChatActions.decorating(delivered::add);
        Component plain = Component.literal("plain");
        consumer.accept(plain);
        assertEquals(1, delivered.size());
        assertSame(plain, delivered.get(0));
    }

    private static List<ClickEvent> clickEvents(Component message) {
        List<ClickEvent> events = new ArrayList<>();
        for (Component sibling : message.getSiblings()) {
            if (sibling.getStyle().getClickEvent() != null) events.add(sibling.getStyle().getClickEvent());
        }
        return events;
    }
}
