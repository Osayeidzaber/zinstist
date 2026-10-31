package dev.jilaton.client.module.impl;

import dev.jilaton.client.setting.TextSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NickHiderPolicyTest {
    @Test
    void plainAndEmbeddedNamesAreReplacedWhileStyleSurvives() {
        Component embedded = Component.literal("Purixy joined the game");
        assertEquals("You joined the game", NickHiderPolicy.replace(embedded, "Purixy", "You").getString());

        Component styled = Component.literal("Purixy").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
        Component rewritten = NickHiderPolicy.replace(styled, "Purixy", "You");
        assertEquals("You", rewritten.getString());
        assertTrue(rewritten.getStyle().isBold());
        assertEquals(ChatFormatting.AQUA.getColor().intValue(), rewritten.getStyle().getColor().getValue());
    }

    @Test
    void namesThatOnlyContainThePlayersNameAreLeftAlone() {
        for (String other : new String[]{"Purixy2", "NotPurixy", "Nightfall"}) {
            Component component = Component.literal(other);
            assertEquals(other, NickHiderPolicy.replace(component, "Purixy", "You").getString());
            assertEquals(other, NickHiderPolicy.replace(other, "Purixy", "You"));
        }
    }

    @Test
    void translatableArgumentsAndSiblingsAreRewritten() {
        Component chat = Component.translatable("chat.type.text", Component.literal("Purixy"), "gg, rematch?");
        String rendered = NickHiderPolicy.replace(chat, "Purixy", "You").getString();
        assertTrue(rendered.contains("You"));
        assertFalse(rendered.contains("Purixy"));

        MutableComponent siblings = Component.empty()
                .append(Component.literal("Purixy"))
                .append(Component.literal(" was slain by Nightfall"));
        assertEquals("You was slain by Nightfall",
                NickHiderPolicy.replace(siblings, "Purixy", "You").getString());
    }

    @Test
    void identicalOrBlankInputsReturnTheOriginalComponent() {
        Component source = Component.literal("Purixy");
        assertSame(source, NickHiderPolicy.replace(source, "Purixy", "Purixy"));
        assertSame(source, NickHiderPolicy.replace(source, "", "You"));
        assertSame(source, NickHiderPolicy.replace(source, "Purixy", null));
        assertEquals("Purixy", NickHiderPolicy.replace("Purixy", "Purixy", "Purixy"));
    }

    @Test
    void blankReplacementOrBlankRealNameNeverErasesText() {
        Component source = Component.literal("Purixy joined the game");
        assertEquals("Purixy joined the game", NickHiderPolicy.replace(source, "Purixy", "").getString());
        assertEquals("Purixy joined the game", NickHiderPolicy.replace(source, "Purixy", "   ").getString());
        assertSame(source, NickHiderPolicy.replace(source, "Purixy", "  "));
        assertEquals("Purixy joined the game", NickHiderPolicy.replace(source, "   ", "You").getString());
        assertEquals("Purixy joined the game", NickHiderPolicy.replace("Purixy joined the game", "Purixy", " "));
    }

    @Test
    void longNamesAreReplacedWholeWithoutTruncatingTheRestOfTheText() {
        String longName = "A".repeat(64);
        String message = longName + " whispers: the tournament starts in five minutes";
        assertEquals("You whispers: the tournament starts in five minutes",
                NickHiderPolicy.replace(message, longName, "You"));
        String longReplacement = "B".repeat(64);
        assertEquals(longReplacement + " whispers: the tournament starts in five minutes",
                NickHiderPolicy.replace(message, longName, longReplacement),
                "the policy must not truncate a long replacement");
        assertEquals("X" + longName + "Y", NickHiderPolicy.replace("X" + longName + "Y", longName, "You"),
                "a name inside a longer word stays untouched");
    }

    @Test
    void moduleDisplayNameRewritesSelfOnlyWithTheConfiguredReplacement() {
        NickHiderModule module = new NickHiderModule();
        assertEquals("You left the game",
                module.displayName(Component.literal("Purixy left the game"), "Purixy").getString());
        assertEquals("Nightfall left the game",
                module.displayName(Component.literal("Nightfall left the game"), "Purixy").getString());
    }

    @Test
    void moduleStartsOffAndSanitizesTheReplacementName() {
        NickHiderModule module = new NickHiderModule();
        assertFalse(module.enabled());
        assertEquals("You", module.replacement());
        assertTrue(module.description().contains("still see your real name"));

        TextSetting setting = (TextSetting) module.settings().get(0);
        setting.setValue("  Alias\u0000 ");
        assertEquals("Alias", module.replacement());
    }
}
