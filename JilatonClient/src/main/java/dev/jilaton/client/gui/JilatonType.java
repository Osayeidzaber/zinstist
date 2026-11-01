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
        REGULAR("ui"), MEDIUM("ui_medium"), SEMIBOLD("ui_semibold"), BOLD("ui_bold"), MONO("mono"), MONO_MEDIUM("mono_medium"),
        SERIF("serif"), SERIF_ITALIC("serif_italic"), DISPLAY("display"), DISPLAY_HEAVY("display_heavy");

        final FontDescription description;

        Face(String path) {
            description = new FontDescription.Resource(Identifier.fromNamespaceAndPath("jilaton", path));
        }
    }

    private static Font inter;
    /** Pose scale, relative to GUI units, that text is currently drawn under ({@link #withScale}, {@code Canvas}). */
    private static float contextScale = 1;

    /** Runs {@code body} with text drawn under a further pose scale of {@code scale} (it multiplies the current one). */
    public static void withScale(float scale, Runnable body) {
        float previous = contextScale;
        contextScale = previous * scale;
        try {
            body.run();
        } finally {
            contextScale = previous;
        }
    }

    /** Multiplies the context scale by {@code scale}; returns the previous value for {@link #restoreScale}. */
    public static float enterScale(float scale) {
        float previous = contextScale;
        contextScale = previous * scale;
        return previous;
    }

    public static void restoreScale(float previous) {
        contextScale = previous;
    }

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
            inter = new Font(new Font.Provider() {
                @Override
                public GlyphSource glyphs(FontDescription description) {
                    // Drawing and measuring go through JilatonText; these glyphs only back what still asks the
                    // font directly (widget internals), in Inter.
                    return base.glyphs(FontDescription.DEFAULT.equals(description) ? Face.REGULAR.description : description);
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

    /** The Inter font if it has been created, for {@code JilatonText.handles}; never creates it. */
    public static Font interIfCreated() {
        return inter;
    }

    /** The face a style's font id stands for; {@code bold} upgrades Regular and Medium. */
    public static Face faceOf(FontDescription description, boolean bold) {
        Face found = Face.REGULAR;
        if (description != null && !FontDescription.DEFAULT.equals(description))
            for (Face face : Face.values()) if (face.description.equals(description)) found = face;
        if (bold && (found == Face.REGULAR || found == Face.MEDIUM)) return Face.BOLD;
        return found;
    }

    /**
     * Draws text {@code size} GUI pixels tall (the base face is 9) with Jilaton's text renderer
     * ({@link dev.jilaton.client.gui.design.JilatonText}): rasterised at its exact on-screen size and placed on whole
     * pixels, so it is sharp at any scale.
     */
    public static void draw(GuiGraphics graphics, String value, Face face, float x, float y, float size, int color) {
        if (enabled()) {
            dev.jilaton.client.gui.design.JilatonText.draw(graphics,
                    java.util.List.of(new dev.jilaton.client.gui.design.JilatonText.Segment(value, face, color)), x, y, size, false);
            return;
        }
        float scale = size / 9F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.drawString(font(), Component.literal(value), 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    /** Width in GUI pixels of {@code value} drawn by {@link #draw} at {@code size}. */
    public static float width(String value, Face face, float size) {
        if (!enabled()) return font().width(value) * size / 9F;
        return dev.jilaton.client.gui.design.JilatonText.width(value, face, size);
    }
}
