package dev.jilaton.client.gui.design;

import com.mojang.blaze3d.textures.GpuTextureView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Glyph atlases that belong to Jilaton's own TTF fonts, with each one's oversample factor, recorded as the font
 * texture is created ({@code FontTextureRegistryMixin}) and read when GUI text picks its sampler
 * ({@code GlyphSamplerMixin}). The GPU texture label is only a numeric id, so the font id is captured at creation.
 */
public final class JilatonAtlases {
    private static final Map<GpuTextureView, Float> OVERSAMPLE = Collections.synchronizedMap(new WeakHashMap<>());

    private JilatonAtlases() { }

    /** Records an atlas of font {@code fontId} (for example {@code jilaton:ui_o1h}). */
    public static void register(GpuTextureView view, String fontId) {
        if (view != null) OVERSAMPLE.put(view, oversample(fontId));
    }

    /** Oversample factor of a Jilaton atlas, or 0 when {@code view} is not one. */
    public static float oversample(GpuTextureView view) {
        Float value = view == null ? null : OVERSAMPLE.get(view);
        return value == null ? 0 : value;
    }

    /** Factor encoded in a font id's {@code _o<tag>} suffix; the untagged base faces are 4. */
    static float oversample(String fontId) {
        int at = fontId.lastIndexOf("_o");
        if (at < 0) return 4;
        return switch (fontId.substring(at + 2)) {
            case "1q" -> 1.25F;
            case "1h" -> 1.5F;
            case "1t" -> 1.75F;
            case "2h" -> 2.5F;
            case "3h" -> 3.5F;
            default -> {
                try {
                    yield Float.parseFloat(fontId.substring(at + 2));
                } catch (NumberFormatException exception) {
                    yield 4;
                }
            }
        };
    }
}
