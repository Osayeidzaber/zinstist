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
        /** The same face rasterised at each of {@link #LEVELS} oversample factors. */
        final FontDescription[] sharp = new FontDescription[LEVELS.length];

        Face(String path) {
            description = resource(path);
            for (int i = 0; i < LEVELS.length; i++)
                sharp[i] = LEVELS[i] == 4 ? description : resource(path + "_o" + LEVEL_TAGS[i]);
        }

        private static FontDescription resource(String path) {
            return new FontDescription.Resource(Identifier.fromNamespaceAndPath("jilaton", path));
        }
    }

    /**
     * Oversample factors shipped for every face (base 4 plus {@code assets/jilaton/font/*_o<tag>.json}). Text is drawn
     * from the smallest raster at least as large as its real on-screen size, so glyphs only ever shrink a little and,
     * with the bilinear atlas sampling of {@code FontTextureSamplerMixin}, small text stays whole and smooth.
     */
    static final float[] LEVELS = {1, 1.5F, 2, 3, 4, 5, 6, 8, 10};
    private static final String[] LEVEL_TAGS = {"1", "1h", "2", "3", "4", "5", "6", "8", "10"};

    private static Font inter;
    /** Extra pose scale that the current screen draws under ({@link #withScale}), for picking a sharp raster. */
    private static float contextScale = 1;

    /** Runs {@code body} with text known to be drawn under an extra pose scale of {@code scale}. */
    public static void withScale(float scale, Runnable body) {
        float previous = contextScale;
        contextScale = scale;
        try {
            body.run();
        } finally {
            contextScale = previous;
        }
    }

    /** Index into {@link #LEVELS} of the smallest raster (9 * oversample px) that covers {@code emPixels}. */
    static int level(double emPixels) {
        double wanted = emPixels / 9.0;
        for (int i = 0; i < LEVELS.length; i++) if (LEVELS[i] >= wanted * 0.97) return i;
        return LEVELS.length - 1;
    }

    private static FontDescription sharpFor(FontDescription description) {
        int level = level(9 * contextScale * Minecraft.getInstance().getWindow().getGuiScale());
        if (FontDescription.DEFAULT.equals(description)) return Face.REGULAR.sharp[level];
        for (Face face : Face.values()) if (face.description.equals(description)) return face.sharp[level];
        return description;
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
                    // Each face resolves to the raster that matches today's on-screen size, so plain
                    // drawString text in Jilaton screens and the HUD is sampled about 1:1 too.
                    return base.glyphs(sharpFor(description));
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

    /** Draws text at {@code size} GUI pixels tall (the base face is 9), sharp at any pose scale. */
    public static void draw(GuiGraphics graphics, String value, Face face, float x, float y, float size, int color) {
        Font font = font();
        float scale = size / 9F;
        Component text;
        if (enabled()) {
            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            var pose = graphics.pose();
            double pixelsPerUnit = pose.m00() * guiScale;
            // Glyph raster is 9 * oversample px tall; pick the level nearest the on-screen em height.
            final int level = level(size * pixelsPerUnit);
            text = Component.literal(value).withStyle(style -> style.withFont(face.sharp[level]));
            // Start on a whole physical pixel so 1:1 glyphs are not split across two pixels.
            if (pixelsPerUnit > 0) {
                double px = (pose.m00() * x + pose.m20()) * guiScale;
                double py = (pose.m11() * y + pose.m21()) * guiScale;
                x += (float) ((Math.round(px) - px) / pixelsPerUnit);
                y += (float) ((Math.round(py) - py) / pixelsPerUnit);
            }
        } else {
            text = Component.literal(value);
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    /** Width in GUI pixels of {@code value} drawn by {@link #draw} at {@code size}. */
    public static float width(String value, Face face, float size) {
        return font().width(text(value, face)) * size / 9F;
    }
}
