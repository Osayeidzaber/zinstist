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
     * Oversample factors shipped for every face (base 4 plus {@code assets/jilaton/font/*_o<tag>.json}): glyph rasters
     * of 9 * factor physical pixels per em. Text is drawn at a size snapped so its em lands exactly on one of these
     * rasters, so every glyph is sampled 1:1 (sharp, no dropped rows) at any window size and GUI scale. Neighbouring
     * levels are at most 25% apart, so a size moves by at most about 12% when snapped.
     */
    static final float[] LEVELS = {1, 1.25F, 1.5F, 1.75F, 2, 2.5F, 3, 3.5F, 4, 5, 6, 8, 10};
    private static final String[] LEVEL_TAGS = {"1", "1q", "1h", "1t", "2", "2h", "3", "3h", "4", "5", "6", "8", "10"};
    /** On-screen em height below which Regular and Mono draw one weight heavier, so thin strokes stay visible. */
    private static final double HEAVIER_BELOW_PX = 15;

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

    private static double guiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    /** Index into {@link #LEVELS} of the raster nearest (in ratio) to an em of {@code emPixels} physical pixels. */
    static int level(double emPixels) {
        double wanted = Math.max(1e-3, emPixels / 9.0);
        int best = 0;
        for (int i = 1; i < LEVELS.length; i++)
            if (Math.abs(Math.log(LEVELS[i] / wanted)) < Math.abs(Math.log(LEVELS[best] / wanted))) best = i;
        return best;
    }

    /** The largest level whose raster fits {@code emPixels}, for picking a layout scale that lands exactly on one. */
    public static float levelAtMost(double emPixels) {
        float best = LEVELS[0];
        for (float level : LEVELS) if (9 * level <= emPixels * 1.001) best = level;
        return best;
    }

    /** The smallest level whose raster covers {@code emPixels}. */
    public static float levelAtLeast(double emPixels) {
        for (float level : LEVELS) if (9 * level >= emPixels * 0.999) return level;
        return LEVELS[LEVELS.length - 1];
    }

    /** {@code size} snapped so that, at {@code pixelsPerUnit}, the em is exactly one raster level. */
    static float snap(float size, double pixelsPerUnit) {
        if (!enabled() || pixelsPerUnit <= 0) return size;
        return (float) (9 * LEVELS[level(size * pixelsPerUnit)] / pixelsPerUnit);
    }

    /** Thin faces get one step heavier at small on-screen sizes. */
    static Face weighted(Face face, double emPixels) {
        if (emPixels >= HEAVIER_BELOW_PX) return face;
        return switch (face) {
            case REGULAR -> Face.MEDIUM;
            case MONO -> Face.MONO_MEDIUM;
            default -> face;
        };
    }

    private static FontDescription sharpFor(FontDescription description) {
        double em = 9 * contextScale * guiScale();
        int level = level(em);
        if (FontDescription.DEFAULT.equals(description)) return weighted(Face.REGULAR, em).sharp[level];
        for (Face face : Face.values()) if (face.description.equals(description)) return weighted(face, em).sharp[level];
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

    /** The Inter font if it has been created, for {@code JilatonText.handles}; never creates it. */
    public static Font interIfCreated() {
        return inter;
    }

    /** The face a style's font id stands for (any raster level of it); {@code bold} upgrades Regular and Medium. */
    public static Face faceOf(FontDescription description, boolean bold) {
        Face found = Face.REGULAR;
        if (description != null && !FontDescription.DEFAULT.equals(description)) {
            search:
            for (Face face : Face.values()) {
                if (face.description.equals(description)) { found = face; break; }
                for (FontDescription level : face.sharp) if (level.equals(description)) { found = face; break search; }
            }
        }
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
