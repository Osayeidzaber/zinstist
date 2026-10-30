package dev.jilaton.client.gui.shell;

import java.util.Objects;

/**
 * Render-ready description of one Visuals card preview, resolved from the module's own settings.
 * All colours are opaque ARGB and every numeric channel is normalised to 0..1 so the renderer and
 * the headless mapping tests share the same bounds.
 */
public record PreviewSpec(Motif motif, int variant, int primary, int secondary, int glow,
                          int accent2, int tint,
                          double amount, double density, double size, double speed,
                          double spread, double opacity, int detail) {
    public enum Motif {
        SKY, MOTES, CROSSHAIR, LENS, WASH, STEADY, BURST, RINGS, TRAIL, TARGET, BOX, ORBIT,
        FIGURE, CROWN, TOTEM, PANEL, LYRICS, SWING, HAND, CAMERA, PEARL, CORE, DAMAGE,
        FIRE, SHARDS, GLOW, WINGS, HALO, COMPANION, KNIFE
    }

    // Sky feature layers occupy bits 0..10 (SkyThemes.Feature ordinal); the extras follow.
    public static final int SKY_DOME = 1 << 11;
    public static final int SKY_TWINKLE = 1 << 12;
    public static final int SKY_HIDE_SUN = 1 << 13;

    public static final int MOTES_OVERWORLD = 1;
    public static final int MOTES_NIGHT = 2;

    public static final int CROSSHAIR_OUTLINE = 1;
    public static final int CROSSHAIR_DOT = 2;
    public static final int CROSSHAIR_THIRD_PERSON = 4;
    public static final int CROSSHAIR_COOLDOWN = 8;

    public static final int LENS_HOLD = 1;
    public static final int LENS_SCROLL = 2;
    public static final int LENS_SMOOTH = 4;
    public static final int LENS_SENSITIVITY = 8;

    public static final int BURST_BODY = 1;
    public static final int BURST_ONLY_CRITS = 2;
    public static final int BURST_GLOW = 4;
    public static final int BURST_CRIT_BOOST = 8;
    public static final int BURST_SELF = 16;
    /** Marks hit_effects (layered impact geometry) so the renderer does not read it as a particle type. */
    public static final int BURST_EFFECTS = 1 << 16;
    public static final int BURST_STYLE_SPARKS = 32;
    public static final int BURST_STYLE_BURST = 64;
    public static final int BURST_STYLE_RIPPLE = 128;
    public static final int BURST_STYLE_SHOCKWAVE = 256;
    public static final int BURST_STYLE_STARS = 512;
    public static final int BURST_STYLE_SMOKE = 1024;
    public static final int BURST_STYLE_INK = 2048;
    public static final int BURST_STYLE_RINGS = 4096;
    public static final int BURST_STYLE_ENERGY = 8192;
    public static final int BURST_STYLE_MARKERS = 16384;
    public static final int BURST_STYLE_DOME = 32768;

    public static final int RINGS_FILL = 1;
    public static final int RINGS_JUMP = 2;
    public static final int RINGS_LAND = 4;

    public static final int TRAIL_FIRST_PERSON = 1;

    public static final int TARGET_WALLS = 1;
    public static final int TARGET_FLASH = 2;
    public static final int TARGET_PLAYERS = 4;

    public static final int BOX_OUTLINE = 1;
    public static final int BOX_GLOW = 2;
    public static final int BOX_BOXES = 4;
    public static final int BOX_TRACERS = 8;
    public static final int BOX_THROUGH = 16;
    public static final int BOX_ITEM = 32;
    public static final int BOX_STEM = 64;

    public static final int ORBIT_TRAIL = 1;
    public static final int ORBIT_BOUNCE = 2;
    public static final int ORBIT_WALLS = 4;

    public static final int FIGURE_HALO = 0x7;
    public static final int FIGURE_RELICS = 0x7 << 3;
    public static final int FIGURE_WINGS = 0x7 << 6;
    public static final int FIGURE_COMPANION = 0x3 << 9;
    public static final int FIGURE_AURA = 0x3 << 11;
    public static final int FIGURE_RELIC_TRAIL = 1 << 13;
    public static final int FIGURE_FIRST_PERSON = 1 << 14;
    public static final int FIGURE_HALO_SHIFT = 0;
    public static final int FIGURE_RELIC_SHIFT = 3;
    public static final int FIGURE_WING_SHIFT = 6;
    public static final int FIGURE_COMPANION_SHIFT = 9;
    public static final int FIGURE_AURA_SHIFT = 11;

    public static final int CROWN_COLLAR = 1;
    public static final int CROWN_GEM_GLOW = 2;
    public static final int CROWN_GLINT = 4;

    public static final int TOTEM_POSE = 1;
    public static final int TOTEM_CREST = 2;
    public static final int TOTEM_SEAL = 4;
    public static final int TOTEM_WALLS = 8;

    public static final int PANEL_ART = 1;
    public static final int PANEL_DEVICE = 2;
    public static final int PANEL_BARS = 4;
    public static final int PANEL_NOTES = 8;
    public static final int PANEL_COMPACT = 16;
    public static final int PANEL_HEALTH = 32;
    public static final int PANEL_DURABILITY = 64;
    public static final int PANEL_BACKGROUND = 128;
    public static final int PANEL_HIDE_IDLE = 256;

    public static final int LYRICS_PANEL = 1;
    public static final int LYRICS_FLOATING = 2;
    public static final int LYRICS_ACCENTS = 4;
    public static final int LYRICS_OCCLUSION = 8;
    public static final int LYRICS_THIRD_PERSON = 16;
    public static final int LYRICS_ALIGN = 0x3 << 5;
    public static final int LYRICS_ALIGN_SHIFT = 5;

    public static final int HAND_MAIN = 1;
    public static final int HAND_OFF = 2;

    public static final int CAMERA_SMOOTH = 1;
    public static final int CAMERA_BODY = 2;
    public static final int CAMERA_HANDS = 4;
    public static final int CAMERA_HOLD = 8;

    public static final int PEARL_TRAIL = 1;
    public static final int PEARL_OTHERS = 2;
    public static final int PEARL_INFO = 4;
    public static final int PEARL_GLOW = 8;
    public static final int PEARL_GRADIENT = 16;
    public static final int PEARL_LANDING = 0x3 << 5;
    public static final int PEARL_LANDING_SHIFT = 5;

    public static final int CORE_SPARKLES = 1;
    public static final int CORE_CRYSTALS = 2;
    public static final int CORE_ANCHORS = 4;

    public static final int DAMAGE_HEALING = 1;

    public static final int FIRE_CUSTOM = 1;

    public static final int SHARDS_GLOW = 1;
    public static final int SHARDS_PLAYERS = 2;
    public static final int SHARDS_MOBS = 4;

    public static final int GLOW_PULSE = 1;
    public static final int GLOW_ITEMS = 2;
    public static final int GLOW_MOBS = 4;

    public static final int WEATHER_GLOW = 1;
    public static final int WEATHER_COVERED = 2;

    public static final int WINGS_ANIMATE = 1;
    public static final int WINGS_OWN = 2;
    public static final int WINGS_OTHERS = 4;

    public static final int HALO_OWN = 1;
    public static final int HALO_OTHERS = 2;

    public static final int COMPANION_RING = 1;
    public static final int COMPANION_TAIL = 2;

    /** Convenience form for motifs that only use the three base colours. */
    public PreviewSpec(Motif motif, int variant, int primary, int secondary, int glow,
                       double amount, double density, double size, double speed,
                       double spread, double opacity, int detail) {
        this(motif, variant, primary, secondary, glow, glow, secondary,
                amount, density, size, speed, spread, opacity, detail);
    }

    public PreviewSpec {
        Objects.requireNonNull(motif, "motif");
        variant = Math.max(0, Math.min(31, variant));
        primary = opaque(primary);
        secondary = opaque(secondary);
        glow = opaque(glow);
        accent2 = opaque(accent2);
        tint = opaque(tint);
        amount = unit(amount);
        density = unit(density);
        size = unit(size);
        speed = unit(speed);
        spread = unit(spread);
        opacity = unit(opacity);
    }

    public static int opaque(int rgb) {
        return 0xFF000000 | rgb & 0xFFFFFF;
    }

    public static double unit(double value) {
        return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
    }
}
