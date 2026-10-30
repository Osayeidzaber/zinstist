package dev.jilaton.client.render.fx;

import java.util.List;
import java.util.Map;

/** Original sky palettes. Colors are 24-bit RGB; features select which procedural layers draw. */
public final class SkyThemes {
    public enum Feature { STARS, MILKY_WAY, AURORA, PLANET, BLACK_HOLE, MOON, SUNSET_GLOW, SHOOTING_STARS, NEBULA, LIGHT_STREAKS, ALL_DAY }

    public record Theme(String name, int zenith, int horizon, double stars, int starTint, int accent, int accent2, List<Feature> features) { }

    public static final List<String> NAMES = List.of("Deep Space", "Midnight", "Black Hole", "Aurora", "Galaxy",
            "Ringed Planet", "Monochrome", "Dark Sunset", "Atmospheric Night", "Nebula", "Storm", "Violet Haze",
            "Crimson Haze", "Custom");

    private static final Map<String, Theme> THEMES = Map.ofEntries(Map.entry("Violet Haze", new Theme("Violet Haze", 0x5A2C86, 0x9A62C8, 0.3, 0xFFFFFF, 0xE2C6FF, 0xB07CFF,
                    List.of(Feature.ALL_DAY, Feature.LIGHT_STREAKS))),
            Map.entry("Crimson Haze", new Theme("Crimson Haze", 0x7A2230, 0xB4505A, 0.3, 0xFFFFFF, 0xFFC2C8, 0xFF6B7A,
                    List.of(Feature.ALL_DAY, Feature.LIGHT_STREAKS))),
            Map.entry("Deep Space", new Theme("Deep Space", 0x020306, 0x0A0D16, 1.0, 0xFFFFFF, 0x6C7BFF, 0xB58CFF,
                    List.of(Feature.STARS, Feature.NEBULA, Feature.SHOOTING_STARS))),
            Map.entry("Midnight", new Theme("Midnight", 0x03060E, 0x101A2C, 0.7, 0xDCE6FF, 0xE8EEF8, 0x8FA6D0,
                    List.of(Feature.STARS, Feature.MOON))),
            Map.entry("Black Hole", new Theme("Black Hole", 0x010101, 0x07070A, 0.9, 0xFFFFFF, 0xFFC27A, 0xFF7A3D,
                    List.of(Feature.STARS, Feature.BLACK_HOLE))),
            Map.entry("Aurora", new Theme("Aurora", 0x02060A, 0x0B1A1E, 0.8, 0xE9FFFB, 0x4DFFB0, 0x6E8CFF,
                    List.of(Feature.STARS, Feature.AURORA))),
            Map.entry("Galaxy", new Theme("Galaxy", 0x04030A, 0x120C1E, 1.3, 0xFFFFFF, 0xC9A7FF, 0x7FB6FF,
                    List.of(Feature.STARS, Feature.MILKY_WAY, Feature.SHOOTING_STARS))),
            Map.entry("Ringed Planet", new Theme("Ringed Planet", 0x030407, 0x0E1016, 0.9, 0xFFFFFF, 0xD9C3A0, 0x8B9BB5,
                    List.of(Feature.STARS, Feature.PLANET))),
            Map.entry("Monochrome", new Theme("Monochrome", 0x000000, 0x0D0D0D, 0.8, 0xF2F2F2, 0xF2F2F2, 0x8A8A8A,
                    List.of(Feature.STARS, Feature.MOON))),
            Map.entry("Dark Sunset", new Theme("Dark Sunset", 0x07040A, 0x3A1A16, 0.4, 0xFFE9D6, 0xFF8A4C, 0xB8486B,
                    List.of(Feature.STARS, Feature.SUNSET_GLOW))),
            Map.entry("Atmospheric Night", new Theme("Atmospheric Night", 0x050811, 0x1A2333, 0.6, 0xE6EEFF, 0x9FB4D8, 0x546A8F,
                    List.of(Feature.STARS, Feature.MOON, Feature.MILKY_WAY))),
            Map.entry("Nebula", new Theme("Nebula", 0x05030B, 0x1B1226, 1.4, 0xFFFFFF, 0xE8A8C0, 0x7FD6C8,
                    List.of(Feature.STARS, Feature.NEBULA, Feature.MILKY_WAY, Feature.SHOOTING_STARS))),
            Map.entry("Storm", new Theme("Storm", 0x0A0D12, 0x2A323A, 0.15, 0xC8D4E0, 0x9FB2C4, 0x5C6B7A,
                    List.of(Feature.STARS, Feature.LIGHT_STREAKS))));

    private SkyThemes() { }

    public static Theme resolve(String name, int customZenith, int customHorizon, int customAccent) {
        Theme theme = THEMES.get(name);
        if (theme != null) return theme;
        return new Theme("Custom", customZenith, customHorizon, 1.0, 0xFFFFFF, customAccent, customAccent,
                List.of(Feature.STARS, Feature.NEBULA, Feature.MOON));
    }
}
