package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.mixin.FontProviderAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Jilaton's typography: Inter for UI text and JetBrains Mono for numbers, shipped as TTF font providers
 * ({@code assets/jilaton/font/*.json}). {@link #font()} is a {@link Font} whose default face is Inter, so a Jilaton
 * screen that declares {@code private final Font font = JilatonType.font();} renders every existing draw call and
 * widget in Inter without touching vanilla screens. With the "Minecraft" font option it returns the vanilla font.
 */
public final class JilatonType {
    public enum Face {
        REGULAR("ui"), MEDIUM("ui_medium"), SEMIBOLD("ui_semibold"), BOLD("ui_bold"), MONO("mono"), MONO_MEDIUM("mono_medium");

        final FontDescription description;

        Face(String path) {
            description = new FontDescription.Resource(Identifier.fromNamespaceAndPath("jilaton", path));
        }
    }

    private static Font inter;

    private JilatonType() { }

    /** Whether the Inter typeface is selected (Theme page, {@code ui.font}). */
    public static boolean enabled() {
        return !"minecraft".equals(JilatonClient.clientConfig().uiFont());
    }

    /** The font Jilaton screens and the HUD draw with. */
    public static Font font() {
        Font vanilla = Minecraft.getInstance().font;
        if (!enabled()) return vanilla;
        if (inter == null) {
            Font.Provider base = ((FontProviderAccess) vanilla).jilaton$provider();
            FontDescription ui = Face.REGULAR.description;
            inter = new Font(new Font.Provider() {
                @Override
                public GlyphSource glyphs(FontDescription description) {
                    return base.glyphs(FontDescription.DEFAULT.equals(description) ? ui : description);
                }

                @Override
                public EffectGlyph effect() {
                    return base.effect();
                }
            });
        }
        return inter;
    }

    /** Text in a specific face; plain text when the Minecraft font is selected. */
    public static Component text(String value, Face face) {
        if (!enabled()) return Component.literal(value);
        return Component.literal(value).withStyle(style -> style.withFont(face.description));
    }

    /** Draws text at {@code size} GUI pixels tall (the base face is 9). */
    public static void draw(GuiGraphics graphics, String value, Face face, float x, float y, float size, int color) {
        Font font = font();
        float scale = size / 9F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, text(value, face), 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    /** Width in GUI pixels of {@code value} drawn by {@link #draw} at {@code size}. */
    public static float width(String value, Face face, float size) {
        return font().width(text(value, face)) * size / 9F;
    }
}
