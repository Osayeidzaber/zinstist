package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;

import java.util.List;

/**
 * Replaces the vanilla first-person fire overlay with a softly tinted one (white by default) that sits lower and smaller,
 * so the view and the hotbar stay readable while burning. The drawing lives in {@code LowFireMixin};
 * this module only carries the settings and the resolved tint.
 */
public final class LowFireModule extends ClientModule {
    private static final int WHITE = 0xF5F5F5;

    private final ChoiceSetting tint = addSetting(new ChoiceSetting("tint", "Tint", List.of("White", "Theme", "Custom"), "White"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "F5F5F5"));
    private final NumberSetting lower = addSetting(new NumberSetting("lower", "Lowered by", 0, 0.45, 0.01, 0.2));
    private final NumberSetting size = addSetting(new NumberSetting("size", "Overlay size", 0.55, 1.2, 0.05, 0.95));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.2, 1, 0.05, 0.85));

    public LowFireModule() {
        super("low_fire", "Low Fire", "Violet fire overlay that sits lower so the view and hotbar stay clear.", ModuleCategory.RENDER);
    }

    /** 24-bit tint for the overlay; {@code accent} is the client theme accent. */
    public int fireTint(int accent) {
        return switch (tint.value()) {
            case "Theme" -> accent & 0xFFFFFF;
            case "Custom" -> EffectPalette.resolve(EffectPalette.CUSTOM, accent, color.value());
            default -> WHITE;
        };
    }

    public double lowering() { return lower.value(); }
    public double scale() { return size.value(); }
    public double alpha() { return opacity.value(); }
}
