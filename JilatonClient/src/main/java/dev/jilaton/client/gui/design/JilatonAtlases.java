package dev.jilaton.client.gui.design;

import com.mojang.blaze3d.textures.GpuTextureView;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Glyph atlases that belong to Jilaton's own TTF fonts, recorded as each font texture is created
 * ({@code FontTextureRegistryMixin}) and read when GUI text picks its sampler ({@code GlyphSamplerMixin}).
 * The GPU texture label is only a numeric id, so the font id is captured at creation instead.
 */
public final class JilatonAtlases {
    private static final Set<GpuTextureView> VIEWS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private JilatonAtlases() { }

    public static void register(GpuTextureView view) {
        if (view != null) VIEWS.add(view);
    }

    public static boolean isJilaton(GpuTextureView view) {
        return view != null && VIEWS.contains(view);
    }
}
