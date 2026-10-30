package dev.jilaton.client.render;

import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.TextSetting;

import java.util.List;
import java.util.Locale;

/** Color modes shared by Jilaton's world effects. Monochrome is the default identity. */
public final class EffectPalette {
    public static final String MONO = "Mono";
    public static final String THEME = "Theme";
    public static final String CUSTOM = "Custom";
    public static final int MONO_RGB = 0xF2F2F2;

    private EffectPalette() { }

    public static ChoiceSetting modeSetting(String id, String label) {
        return new ChoiceSetting(id, label, List.of(MONO, THEME, CUSTOM), MONO);
    }

    public static TextSetting hexSetting(String id, String label, String defaultValue) {
        return new TextSetting(id, label, 7, defaultValue, EffectPalette::normalizeHex);
    }

    public static String normalizeHex(String value) {
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (!normalized.matches("[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("Use six RGB hex digits");
        return normalized.toUpperCase(Locale.ROOT);
    }

    /** Resolves to a 24-bit RGB value; malformed custom input falls back to monochrome. */
    public static int resolve(String mode, int accent, String customHex) {
        return switch (mode) {
            case THEME -> accent & 0xFFFFFF;
            case CUSTOM -> {
                try {
                    yield Integer.parseInt(normalizeHex(customHex), 16);
                } catch (IllegalArgumentException exception) {
                    yield MONO_RGB;
                }
            }
            default -> MONO_RGB;
        };
    }
}
