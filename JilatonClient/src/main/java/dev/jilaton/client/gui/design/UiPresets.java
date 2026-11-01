package dev.jilaton.client.gui.design;

import dev.jilaton.client.config.ClientConfig;

import java.util.List;

/**
 * Interface presets: each one sets a coherent bundle of appearance settings at once, so switching changes how the
 * whole client looks and moves rather than only recolouring it. Individual settings stay editable afterwards.
 */
public final class UiPresets {
    public record Preset(String name, String blurb, String background, double intensity, String theme, double speed,
                         boolean blur, double backdrop, double scale, boolean fullEffects, boolean reducedMotion) { }

    public static final List<Preset> ALL = List.of(
            new Preset("Jilaton", "Dark Veil, white accent, full motion", "Dark Veil", 1.0, "obsidian", 1.0, true, 0.55, 1.0, true, false),
            new Preset("Editorial", "Hairline rings, quiet black, slower motion", "Dark", 1.0, "graphite", 0.8, true, 0.7, 1.0, true, false),
            new Preset("Cyber", "Threads shader, cyan accent, quick motion", "Threads", 1.2, "cyan", 1.4, true, 0.45, 1.0, true, false),
            new Preset("Monochrome", "Liquid Chrome in pure greys", "Liquid Chrome", 0.9, "graphite", 1.0, true, 0.6, 1.0, true, false),
            new Preset("Glass", "Aurora behind light, see-through panels", "Aurora", 1.1, "blue", 1.0, true, 0.3, 1.0, true, false),
            new Preset("Cinematic", "Beams, deep backdrop, slow and smooth", "Beams", 1.3, "amber", 0.65, true, 0.75, 0.95, true, false),
            new Preset("Performance", "No shaders, no blur, instant transitions", "Dark", 0.6, "obsidian", 2.0, false, 0.85, 1.0, false, true));

    private UiPresets() {
    }

    public static void apply(ClientConfig config, Preset p) {
        config.setMenuBackground(p.background());
        config.setShaderIntensity(p.intensity());
        config.setThemeId(p.theme());
        config.setTransitionSpeed(p.speed());
        config.setBackdropBlur(p.blur());
        config.setBackdropOpacity(p.backdrop());
        config.setGuiScale(p.scale());
        config.setFullEffects(p.fullEffects());
        config.setReducedMotion(p.reducedMotion());
    }

    /** The preset the current settings match exactly, or null when the user has customised them. */
    public static Preset current(ClientConfig config) {
        for (Preset p : ALL) {
            if (p.background().equals(config.menuBackground()) && p.theme().equals(config.themeId())
                    && Math.abs(p.speed() - config.transitionSpeed()) < 1e-6 && p.blur() == config.backdropBlur()
                    && p.fullEffects() == config.fullEffects() && p.reducedMotion() == config.reducedMotion()
                    && Math.abs(p.intensity() - config.shaderIntensity()) < 1e-6) return p;
        }
        return null;
    }
}
