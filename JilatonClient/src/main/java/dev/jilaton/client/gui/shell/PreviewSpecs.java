package dev.jilaton.client.gui.shell;

import dev.jilaton.client.gui.JilatonUi;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.ItemEspModule;
import dev.jilaton.client.module.impl.LowFireModule;
import dev.jilaton.client.module.impl.NametagsModule;
import dev.jilaton.client.module.impl.PlayerEspModule;
import dev.jilaton.client.module.impl.TracersModule;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.OverlayColors;
import dev.jilaton.client.render.fx.SkyThemes;
import dev.jilaton.client.render.model.CompanionMotion;
import dev.jilaton.client.render.model.WingMotion;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.Setting;
import dev.jilaton.client.setting.TextSetting;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps each Visuals module's current settings to a {@link PreviewSpec}. This is a pure function of the
 * module state so a card preview can never drift from the module it represents: tests assert the mapping
 * for every registered render module and that changing a setting changes the spec.
 */
public final class PreviewSpecs {
    private static final Map<String, PreviewSpec.Motif> MOTIFS = Map.ofEntries(
            Map.entry("custom_sky", PreviewSpec.Motif.SKY),
            Map.entry("ambience", PreviewSpec.Motif.MOTES),
            Map.entry("custom_crosshair", PreviewSpec.Motif.CROSSHAIR),
            Map.entry("custom_fov", PreviewSpec.Motif.LENS),
            Map.entry("zoom", PreviewSpec.Motif.LENS),
            Map.entry("fullbright", PreviewSpec.Motif.WASH),
            Map.entry("no_bob", PreviewSpec.Motif.STEADY),
            Map.entry("hit_particles", PreviewSpec.Motif.BURST),
            Map.entry("hit_effects", PreviewSpec.Motif.BURST),
            Map.entry("damage_particles", PreviewSpec.Motif.DAMAGE),
            Map.entry("jump_circles", PreviewSpec.Motif.RINGS),
            Map.entry("trails", PreviewSpec.Motif.TRAIL),
            Map.entry("target_esp", PreviewSpec.Motif.TARGET),
            Map.entry("player_esp", PreviewSpec.Motif.BOX),
            Map.entry("item_esp", PreviewSpec.Motif.BOX),
            Map.entry("tracers", PreviewSpec.Motif.BOX),
            Map.entry("block_outline", PreviewSpec.Motif.BOX),
            Map.entry("block_placement", PreviewSpec.Motif.BOX),
            Map.entry("nametags", PreviewSpec.Motif.PANEL),
            Map.entry("floating_relics", PreviewSpec.Motif.ORBIT),
            Map.entry("player_effects", PreviewSpec.Motif.FIGURE),
            Map.entry("character-3d", PreviewSpec.Motif.FIGURE),
            Map.entry("cyberspace", PreviewSpec.Motif.FIGURE),
            Map.entry("glass_material", PreviewSpec.Motif.GLOW),
            Map.entry("jilaton_gate", PreviewSpec.Motif.TOTEM),
            Map.entry("totem_spirit", PreviewSpec.Motif.TOTEM),
            Map.entry("pharaoh_headdress", PreviewSpec.Motif.CROWN),
            Map.entry("now_playing", PreviewSpec.Motif.PANEL),
            Map.entry("music_visuals", PreviewSpec.Motif.PANEL),
            Map.entry("spatial_lyrics", PreviewSpec.Motif.LYRICS),
            Map.entry("swing_animations", PreviewSpec.Motif.SWING),
            Map.entry("view_model", PreviewSpec.Motif.HAND),
            Map.entry("freecam", PreviewSpec.Motif.CAMERA),
            Map.entry("free_look", PreviewSpec.Motif.CAMERA),
            Map.entry("smooth_camera", PreviewSpec.Motif.CAMERA),
            Map.entry("finisher", PreviewSpec.Motif.FIGURE),
            Map.entry("pearl_trajectory", PreviewSpec.Motif.PEARL),
            Map.entry("explosive_highlights", PreviewSpec.Motif.CORE),
            Map.entry("wings", PreviewSpec.Motif.WINGS),
            Map.entry("halo", PreviewSpec.Motif.HALO),
            Map.entry("companion", PreviewSpec.Motif.COMPANION),
            Map.entry("cosmetic_knife", PreviewSpec.Motif.KNIFE),
            Map.entry("low_fire", PreviewSpec.Motif.FIRE),
            Map.entry("afterglow", PreviewSpec.Motif.SHARDS),
            Map.entry("radiance", PreviewSpec.Motif.GLOW),
            Map.entry("weather", PreviewSpec.Motif.MOTES));

    private PreviewSpecs() { }

    public static Set<String> mappedIds() {
        return MOTIFS.keySet();
    }

    public static PreviewSpec of(ClientModule module, int accent) {
        if (module == null) return fallback(accent);
        PreviewSpec.Motif motif = MOTIFS.get(module.id());
        if (motif == null) return fallback(accent);
        return switch (module.id()) {
            case "custom_sky" -> sky(module);
            case "ambience" -> motes(module, accent);
            case "custom_crosshair" -> crosshair(module, accent);
            case "custom_fov" -> lens(module, accent, false);
            case "zoom" -> lens(module, accent, true);
            case "fullbright" -> wash(module, accent);
            case "no_bob" -> steady(module, accent);
            case "hit_particles" -> hitParticles(module, accent);
            case "hit_effects" -> hitEffects(module, accent);
            case "damage_particles" -> damage(module);
            case "jump_circles" -> rings(module, accent);
            case "trails" -> trail(module, accent);
            case "target_esp" -> target(module, accent);
            case "player_esp" -> playerEsp((PlayerEspModule) module, accent);
            case "item_esp" -> itemEsp((ItemEspModule) module, accent);
            case "tracers" -> tracers((TracersModule) module, accent);
            case "block_outline" -> blockOutline(module, accent);
            case "block_placement" -> blockPlacement(module, accent);
            case "nametags" -> nametags((NametagsModule) module, accent);
            case "floating_relics" -> relics(module, accent);
            case "player_effects" -> figure(module, accent);
            case "character-3d" -> character(module, accent);
            case "cyberspace" -> cyberspace(module, accent);
            case "glass_material" -> glassMaterial(module, accent);
            case "jilaton_gate" -> jilatonGate(module, accent);
            case "totem_spirit" -> totem(module, accent);
            case "pharaoh_headdress" -> crown(module, accent);
            case "now_playing" -> nowPlaying(module, accent);
            case "music_visuals" -> musicVisuals(module, accent);
            case "spatial_lyrics" -> lyrics(module, accent);
            case "swing_animations" -> swing(module, accent);
            case "view_model" -> hand(module, accent);
            case "freecam" -> freecam(module, accent);
            case "free_look" -> freeLook(module, accent);
            case "smooth_camera" -> smoothCamera(module, accent);
            case "finisher" -> finisher(module, accent);
            case "pearl_trajectory" -> pearl(module, accent);
            case "explosive_highlights" -> core(module, accent);
            case "wings" -> wings(module, accent);
            case "halo" -> halo(module, accent);
            case "companion" -> companion(module, accent);
            case "cosmetic_knife" -> knife(module, accent);
            case "low_fire" -> lowFire(module, accent);
            case "afterglow" -> afterglow(module, accent);
            case "radiance" -> radiance(module, accent);
            case "weather" -> weather(module, accent);
            default -> fallback(accent);
        };
    }

    private static PreviewSpec fallback(int accent) {
        return new PreviewSpec(PreviewSpec.Motif.MOTES, 0, accent, accent, accent,
                0.4, 0.3, 0.4, 0.3, 0.5, 0.7, 0);
    }

    private static PreviewSpec sky(ClientModule module) {
        String name = choice(module, "theme", "Deep Space");
        int zenith = hex(module, "custom_zenith", "020306");
        int horizon = hex(module, "custom_horizon", "11131A");
        int accent = hex(module, "custom_accent", "9AA8FF");
        SkyThemes.Theme theme = SkyThemes.resolve(name, zenith, horizon, accent);
        int detail = 0;
        for (SkyThemes.Feature feature : theme.features()) detail |= 1 << feature.ordinal();
        if (flag(module, "dome", true)) detail |= PreviewSpec.SKY_DOME;
        if (num(module, "twinkle", 0.5) > 0.01) detail |= PreviewSpec.SKY_TWINKLE;
        if (flag(module, "hide_sun", true)) detail |= PreviewSpec.SKY_HIDE_SUN;
        return new PreviewSpec(PreviewSpec.Motif.SKY, 0, theme.zenith(), theme.horizon(), theme.accent(),
                theme.accent2(), theme.starTint(),
                unit(num(module, "star_density", 1), 0, 2),
                unit(num(module, "star_brightness", 1), 0.1, 1.5),
                unit(num(module, "celestial", 1), 0, 1.5),
                unit(num(module, "animation", 1), 0, 3),
                unit(num(module, "fog_tint", 0.75), 0, 1),
                unit(num(module, "color_strength", 0.9), 0, 1),
                detail);
    }

    private static PreviewSpec motes(ClientModule module, int accent) {
        String style = choice(module, "style", "Snow");
        int detail = (flag(module, "overworld_only", false) ? PreviewSpec.MOTES_OVERWORLD : 0)
                | (flag(module, "night_only", false) ? PreviewSpec.MOTES_NIGHT : 0);
        return new PreviewSpec(PreviewSpec.Motif.MOTES, choiceIndex(module, "style"),
                palette(module, accent, "color_mode", "color"), palette(module, accent, "color_mode", "color"),
                accent,
                num(module, "density", 0.45),
                0.5,
                unit(num(module, "size", 1), 0.3, 3),
                unit(num(module, "speed", 1), 0.1, 3),
                unit(num(module, "radius", 16), 6, 32),
                num(module, "opacity", 0.8),
                detail);
    }

    private static PreviewSpec crosshair(ClientModule module, int accent) {
        String color = choice(module, "color", "Theme");
        int tint = switch (color) {
            case "White" -> 0xFFFFFF;
            case "Green" -> 0x69E59B;
            default -> accent;
        };
        int detail = (flag(module, "outline", true) ? PreviewSpec.CROSSHAIR_OUTLINE : 0)
                | (flag(module, "dot", false) ? PreviewSpec.CROSSHAIR_DOT : 0)
                | (flag(module, "third_person", true) ? PreviewSpec.CROSSHAIR_THIRD_PERSON : 0)
                | (flag(module, "cooldown_bar", true) ? PreviewSpec.CROSSHAIR_COOLDOWN : 0);
        return new PreviewSpec(PreviewSpec.Motif.CROSSHAIR, choiceIndex(module, "shape"), tint, tint, tint,
                unit(num(module, "size", 5), 1, 20),
                0.5,
                unit(num(module, "thickness", 1), 1, 5),
                unit(num(module, "dynamic", 3), 0, 10),
                unit(num(module, "gap", 2), 0, 10),
                0.95,
                detail);
    }

    private static PreviewSpec lens(ClientModule module, int accent, boolean zoom) {
        double fov = num(module, "fov", zoom ? 30 : 110);
        int detail = 0;
        if (zoom) {
            if (flag(module, "hold", true)) detail |= PreviewSpec.LENS_HOLD;
            if (flag(module, "scroll", true)) detail |= PreviewSpec.LENS_SCROLL;
            if (flag(module, "smooth", true)) detail |= PreviewSpec.LENS_SMOOTH;
            if (flag(module, "sensitivity", true)) detail |= PreviewSpec.LENS_SENSITIVITY;
        }
        double amount = zoom ? unit(90 - fov, 0, 85) : unit(fov, 30, 160);
        return new PreviewSpec(PreviewSpec.Motif.LENS, zoom ? 1 : 0, accent, accent, accent,
                amount, 0.5, 0.5, 0.25, 0.5, 0.9, detail);
    }

    private static PreviewSpec wash(ClientModule module, int accent) {
        double gamma = Math.max(1.0, Math.min(16.0, num(module, "gamma", 8.0)));
        return new PreviewSpec(PreviewSpec.Motif.WASH, 0, accent, 0xFFFFFF, accent,
                (gamma - 1.0) / 15.0, 0.5, 0.5, 0.2, 0.5, 0.9, 0);
    }

    private static PreviewSpec steady(ClientModule module, int accent) {
        return new PreviewSpec(PreviewSpec.Motif.STEADY, 0, accent, 0xFFFFFF, accent,
                flag(module, "moving_only", true) ? 1 : 0, 0.5, 0.5, 0.2, 0.5, 0.9, 0);
    }

    private static PreviewSpec hitParticles(ClientModule module, int accent) {
        String type = choice(module, "type", "Crit");
        int primary = switch (type) {
            case "Magic Crit" -> 0xB98CFF;
            case "Spark" -> 0xB9E3FF;
            case "Flame" -> 0xFFA23C;
            case "Soul Flame" -> 0x5FD8E8;
            case "End Rod" -> 0xEDEDED;
            case "Totem" -> 0x7DFFBF;
            case "Heart" -> 0xFF6F9C;
            case "Dust" -> palette(module, accent, "dust_color_mode", "dust_color");
            default -> 0xF2F2F2;
        };
        double speed = switch (type) {
            case "Spark" -> 0.16;
            case "Flame", "Soul Flame" -> 0.035;
            case "End Rod" -> 0.09;
            case "Totem" -> 0.45;
            case "Heart" -> 0;
            case "Dust" -> 1;
            default -> 0.8;
        };
        int detail = (choice(module, "origin", "Impact").equals("Body") ? PreviewSpec.BURST_BODY : 0)
                | (flag(module, "only_crits", false) ? PreviewSpec.BURST_ONLY_CRITS : 0);
        return new PreviewSpec(PreviewSpec.Motif.BURST, choiceIndex(module, "type"), primary, 0xFFFFFF, accent,
                unit(num(module, "amount", 10), 1, 40),
                0.5,
                unit(num(module, "dust_size", 1), 0.5, 2),
                speed,
                unit(num(module, "spread", 1), 0.25, 3),
                0.9,
                detail);
    }

    private static PreviewSpec hitEffects(ClientModule module, int accent) {
        String preset = choice(module, "preset", "Minimal");
        String mode = choice(module, "color_mode", EffectPalette.MONO);
        String hex = text(module, "color", "F2F2F2");
        String secondaryHex = text(module, "secondary", "8A8A8A");
        int primary = switch (preset) {
            case "Cosmic" -> 0xB79CFF;
            case "Splash" -> mode.equals(EffectPalette.CUSTOM) ? EffectPalette.resolve(EffectPalette.CUSTOM, accent, hex) : 0x9BE8FF;
            case "Monochrome" -> 0xF2F2F2;
            default -> EffectPalette.resolve(mode, accent, hex);
        };
        int secondary = switch (preset) {
            case "Cosmic" -> 0x6FA8FF;
            case "Splash" -> 0xE8FBFF;
            case "Monochrome" -> 0x9A9A9A;
            default -> EffectPalette.resolve(EffectPalette.CUSTOM, 0, secondaryHex);
        };
        int detail = PreviewSpec.BURST_EFFECTS
                | (flag(module, "glow", true) ? PreviewSpec.BURST_GLOW : 0)
                | (flag(module, "crit_boost", true) ? PreviewSpec.BURST_CRIT_BOOST : 0)
                | (flag(module, "self", false) ? PreviewSpec.BURST_SELF : 0)
                | presetBurstBits(preset);
        if (preset.equals("Custom")) detail |= customBurstBits(module);
        return new PreviewSpec(PreviewSpec.Motif.BURST, choiceIndex(module, "preset"), primary, secondary, accent,
                unit(num(module, "count", 1), 0.25, 3),
                unit(num(module, "gravity", 1), 0, 2),
                unit(num(module, "size", 1), 0.4, 2.5),
                unit(num(module, "speed", 1), 0.3, 2.5),
                unit(num(module, "lifetime", 1), 0.3, 3),
                num(module, "opacity", 0.9),
                detail);
    }

    private static int presetBurstBits(String preset) {
        // Mirrors HitEffectsModule.PRESETS (module/impl/HitEffectsModule.java:41-47); the map is private.
        return switch (preset) {
            case "Cinematic" -> PreviewSpec.BURST_STYLE_BURST | PreviewSpec.BURST_STYLE_SHOCKWAVE
                    | PreviewSpec.BURST_STYLE_RINGS | PreviewSpec.BURST_STYLE_SMOKE
                    | PreviewSpec.BURST_STYLE_SPARKS | PreviewSpec.BURST_STYLE_MARKERS;
            case "Monochrome" -> PreviewSpec.BURST_STYLE_SPARKS | PreviewSpec.BURST_STYLE_SHOCKWAVE
                    | PreviewSpec.BURST_STYLE_INK;
            case "Cosmic" -> PreviewSpec.BURST_STYLE_STARS | PreviewSpec.BURST_STYLE_ENERGY
                    | PreviewSpec.BURST_STYLE_RINGS | PreviewSpec.BURST_STYLE_BURST;
            case "Splash" -> PreviewSpec.BURST_STYLE_INK | PreviewSpec.BURST_STYLE_SHOCKWAVE
                    | PreviewSpec.BURST_STYLE_BURST | PreviewSpec.BURST_STYLE_MARKERS;
            case "Impact" -> PreviewSpec.BURST_STYLE_DOME | PreviewSpec.BURST_STYLE_SHOCKWAVE
                    | PreviewSpec.BURST_STYLE_SPARKS | PreviewSpec.BURST_STYLE_BURST;
            default -> PreviewSpec.BURST_STYLE_SPARKS | PreviewSpec.BURST_STYLE_RIPPLE;
        };
    }

    private static int customBurstBits(ClientModule module) {
        int bits = 0;
        if (flag(module, "style_sparks", true)) bits |= PreviewSpec.BURST_STYLE_SPARKS;
        if (flag(module, "style_burst", false)) bits |= PreviewSpec.BURST_STYLE_BURST;
        if (flag(module, "style_ripple", true)) bits |= PreviewSpec.BURST_STYLE_RIPPLE;
        if (flag(module, "style_shockwave", false)) bits |= PreviewSpec.BURST_STYLE_SHOCKWAVE;
        if (flag(module, "style_stars", false)) bits |= PreviewSpec.BURST_STYLE_STARS;
        if (flag(module, "style_smoke", false)) bits |= PreviewSpec.BURST_STYLE_SMOKE;
        if (flag(module, "style_ink", false)) bits |= PreviewSpec.BURST_STYLE_INK;
        if (flag(module, "style_rings", false)) bits |= PreviewSpec.BURST_STYLE_RINGS;
        if (flag(module, "style_energy", false)) bits |= PreviewSpec.BURST_STYLE_ENERGY;
        if (flag(module, "style_markers", false)) bits |= PreviewSpec.BURST_STYLE_MARKERS;
        if (flag(module, "style_dome", false)) bits |= PreviewSpec.BURST_STYLE_DOME;
        return bits;
    }

    private static PreviewSpec damage(ClientModule module) {
        boolean healing = flag(module, "healing", true);
        double scale = num(module, "scale", 1);
        return new PreviewSpec(PreviewSpec.Motif.DAMAGE, 0, healing ? 0x69E59B : 0xFF788A, 0xFFFFFF, 0xFF788A,
                unit(scale, 0.5, 3),
                0.5,
                unit(scale, 0.5, 3),
                unit(5 - num(module, "lifetime", 2), 0, 5),
                unit(num(module, "rise", 1.5), 0, 3),
                1,
                healing ? PreviewSpec.DAMAGE_HEALING : 0);
    }

    private static PreviewSpec rings(ClientModule module, int accent) {
        String trigger = choice(module, "trigger", "Both");
        int detail = (flag(module, "fill", true) ? PreviewSpec.RINGS_FILL : 0)
                | (trigger.equals("Jump") || trigger.equals("Both") ? PreviewSpec.RINGS_JUMP : 0)
                | (trigger.equals("Land") || trigger.equals("Both") ? PreviewSpec.RINGS_LAND : 0);
        return new PreviewSpec(PreviewSpec.Motif.RINGS, choiceIndex(module, "style"),
                palette(module, accent, "color_mode", "color"), accent, accent,
                unit(num(module, "radius", 1.4), 0.4, 4),
                flag(module, "fill", true) ? 1 : 0,
                unit(num(module, "width", 2), 1, 5),
                unit(3 - num(module, "lifetime", 1.1), 0, 3),
                unit(num(module, "lifetime", 1.1), 0.3, 3),
                0.9,
                detail);
    }

    private static PreviewSpec trail(ClientModule module, int accent) {
        return new PreviewSpec(PreviewSpec.Motif.TRAIL, choiceIndex(module, "style"),
                palette(module, accent, "color_mode", "color"), accent, accent,
                num(module, "height", 0.55),
                0.5,
                unit(num(module, "width", 2), 1, 5),
                unit(3 - num(module, "lifetime", 0.7), 0, 3),
                unit(num(module, "lifetime", 0.7), 0.2, 3),
                num(module, "opacity", 0.85),
                flag(module, "first_person", false) ? PreviewSpec.TRAIL_FIRST_PERSON : 0);
    }

    private static PreviewSpec target(ClientModule module, int accent) {
        boolean flash = flag(module, "hit_flash", true);
        int primary = palette(module, accent, "color_mode", "color");
        int secondary = flash ? hex(module, "flash_color", "FFFFFF") : primary;
        int detail = (flag(module, "through_walls", true) ? PreviewSpec.TARGET_WALLS : 0)
                | (flash ? PreviewSpec.TARGET_FLASH : 0)
                | (flag(module, "players_only", false) ? PreviewSpec.TARGET_PLAYERS : 0);
        return new PreviewSpec(PreviewSpec.Motif.TARGET, choiceIndex(module, "style"), primary, secondary, accent,
                unit(num(module, "scale", 1), 0.6, 1.8),
                0.5,
                0.5,
                unit(num(module, "speed", 1), 0.2, 2.5),
                unit(num(module, "memory", 4), 0.5, 15),
                num(module, "opacity", 1),
                detail);
    }

    private static PreviewSpec playerEsp(PlayerEspModule module, int accent) {
        int primary = overlayColor(module, accent);
        int detail = (module.outline() ? PreviewSpec.BOX_OUTLINE : 0)
                | (module.glow() ? PreviewSpec.BOX_GLOW : 0)
                | (module.boxes() ? PreviewSpec.BOX_BOXES : 0)
                | (flag(module, "through_walls", true) ? PreviewSpec.BOX_THROUGH : 0);
        return new PreviewSpec(PreviewSpec.Motif.BOX, choiceIndex(module, "mode"), primary, accent, accent,
                module.fillOpacity(),
                0.5,
                unit(num(module, "line_width", 1.5), 1, 8),
                0.35,
                module.expansion() / 0.5,
                1,
                detail);
    }

    private static PreviewSpec itemEsp(ItemEspModule module, int accent) {
        int primary = module.color(accent, 0);
        int detail = (module.glow() ? PreviewSpec.BOX_GLOW : 0)
                | (module.boxes() ? PreviewSpec.BOX_BOXES : 0)
                | (module.tracers() ? PreviewSpec.BOX_TRACERS : 0)
                | (module.throughWalls() ? PreviewSpec.BOX_THROUGH : 0)
                | PreviewSpec.BOX_ITEM;
        return new PreviewSpec(PreviewSpec.Motif.BOX, choiceIndex(module, "mode"), primary, accent, accent,
                module.fillOpacity(),
                0.5,
                unit(module.lineWidth(), 1, 8),
                0.35,
                0.1,
                1,
                detail);
    }

    private static PreviewSpec tracers(TracersModule module, int accent) {
        int primary = overlayColor(module, accent);
        double height = 1.8;
        int detail = PreviewSpec.BOX_TRACERS
                | (module.stem() ? PreviewSpec.BOX_STEM : 0)
                | (flag(module, "through_walls", true) ? PreviewSpec.BOX_THROUGH : 0);
        return new PreviewSpec(PreviewSpec.Motif.BOX, 2, primary, accent, accent,
                0.45, 0.5, unit(num(module, "line_width", 1.5), 1, 8), 0.35,
                module.endpoint(height) / height, 1, detail);
    }

    private static PreviewSpec blockOutline(ClientModule module, int accent) {
        int detail = PreviewSpec.BOX_OUTLINE
                | (flag(module, "glow", true) ? PreviewSpec.BOX_GLOW : 0);
        return new PreviewSpec(PreviewSpec.Motif.BOX, 3, palette(module, accent, "color_mode", "color"),
                accent, accent,
                unit(num(module, "fill", 0.06), 0, 0.3),
                0.5,
                unit(num(module, "width", 2.5), 1, 6),
                unit(0.6 - num(module, "fade_time", 0.2), 0, 0.6),
                unit(num(module, "glide", 0.6), 0, 1),
                1,
                detail);
    }

    private static PreviewSpec blockPlacement(ClientModule module, int accent) {
        int detail = PreviewSpec.BOX_OUTLINE
                | (flag(module, "fill", true) ? PreviewSpec.BOX_BOXES : 0)
                | (flag(module, "particles", true) ? PreviewSpec.BOX_GLOW : 0);
        return new PreviewSpec(PreviewSpec.Motif.BOX, 4 + choiceIndex(module, "style"),
                palette(module, accent, "color_mode", "color"),
                accent, accent,
                unit(num(module, "expand", 0.16), 0, 0.5),
                0.5,
                unit(num(module, "line_width", 2.2), 1, 4.5),
                unit(2.0 - num(module, "duration", 0.65), 0.2, 2.0),
                unit(num(module, "duration", 0.65), 0.2, 2.0),
                num(module, "opacity", 0.88),
                detail);
    }

    private static PreviewSpec cyberspace(ClientModule module, int accent) {
        int primary = palette(module, accent, "color_mode", "color");
        int secondary = hex(module, "secondary", "8878FF");
        int detail = (flag(module, "skeleton", true) ? 1 : 0)
                | (flag(module, "block_wireframe", true) ? 2 : 0)
                | (flag(module, "through_walls", true) ? 4 : 0);
        return new PreviewSpec(PreviewSpec.Motif.FIGURE, 8 + choiceIndex(module, "preset"),
                primary, secondary, accent,
                unit(num(module, "grid_radius", 16), 6, 36),
                0.7,
                unit(num(module, "line_width", 1.8), 1, 4),
                unit(num(module, "scan_speed", 1.0), 0.2, 3.0),
                0.65,
                num(module, "opacity", 0.85),
                detail);
    }

    private static PreviewSpec glassMaterial(ClientModule module, int accent) {
        int primary = palette(module, accent, "color_mode", "color");
        int detail = (flag(module, "specular", true) ? PreviewSpec.GLOW_PULSE : 0);
        return new PreviewSpec(PreviewSpec.Motif.GLOW, choiceIndex(module, "preset"),
                primary, 0xFFFFFF, accent,
                unit(num(module, "rim_strength", 0.82), 0.1, 1.0),
                unit(num(module, "refraction", 0.65), 0, 1.0),
                unit(num(module, "shell_scale", 1.04), 0.9, 1.25),
                0.5,
                0.6,
                num(module, "opacity", 0.48),
                detail);
    }

    private static PreviewSpec jilatonGate(ClientModule module, int accent) {
        int primary = palette(module, accent, "color_mode", "color");
        int detail = (flag(module, "logo_emblem", true) ? PreviewSpec.TOTEM_CREST : 0)
                | (flag(module, "vortex", true) ? PreviewSpec.TOTEM_SEAL : 0)
                | (flag(module, "particles", true) ? PreviewSpec.TOTEM_POSE : 0);
        return new PreviewSpec(PreviewSpec.Motif.TOTEM, 4 + choiceIndex(module, "style"),
                primary, 0x0B0B10, accent,
                unit(num(module, "scale", 1.0), 0.6, 2.0),
                0.7,
                unit(num(module, "distance", 4.5), 2.5, 12.0),
                0.55,
                0.65,
                num(module, "opacity", 0.92),
                detail);
    }

    private static PreviewSpec nametags(NametagsModule module, int accent) {
        int detail = (module.compact() ? PreviewSpec.PANEL_COMPACT : 0)
                | (module.compactBackground() ? PreviewSpec.PANEL_BACKGROUND : 0)
                | (flag(module, "health", true) ? PreviewSpec.PANEL_HEALTH : 0)
                | (module.durability() ? PreviewSpec.PANEL_DURABILITY : 0)
                | (flag(module, "ping", true) ? PreviewSpec.PANEL_DEVICE : 0)
                | (flag(module, "armor", true) ? PreviewSpec.PANEL_ART : 0);
        return new PreviewSpec(PreviewSpec.Motif.PANEL, 8, accent, 0xFFFFFF, accent,
                unit(module.scale(), 0.5, 2),
                0.5,
                module.opacity(),
                0.3,
                module.minimumScale(),
                module.opacity(),
                detail);
    }

    private static PreviewSpec relics(ClientModule module, int accent) {
        String palette = choice(module, "palette", "Style");
        int style = choiceIndex(module, "style");
        int styleRgb = relicStyleColor(style);
        int primary = switch (palette) {
            case "Theme" -> JilatonUi.mix(styleRgb, accent, 0.65);
            case "Custom" -> JilatonUi.mix(styleRgb, EffectPalette.resolve(EffectPalette.CUSTOM, accent,
                    text(module, "tint", "C8C8C8")), 0.8);
            default -> styleRgb;
        };
        int secondary = style == 2 ? 0x3FF2FF : style == 3 ? 0xFF3F72 : 0xFFFFFF;
        int detail = (flag(module, "block_bounce", true) ? PreviewSpec.ORBIT_BOUNCE : 0)
                | (flag(module, "through_walls", false) ? PreviewSpec.ORBIT_WALLS : 0)
                | (num(module, "trail", 1) > 0.05 ? PreviewSpec.ORBIT_TRAIL : 0);
        return new PreviewSpec(PreviewSpec.Motif.ORBIT, style, primary, secondary, accent,
                unit(num(module, "count", 8), 1, 32),
                unit(num(module, "trail", 1), 0.2, 3),
                unit(num(module, "size", 0.3), 0.15, 0.75),
                unit(num(module, "drift", 0.5), 0, 2.5),
                unit(num(module, "spread", 2.6), 1.2, 6),
                0.95,
                detail);
    }

    private static int relicStyleColor(int style) {
        // Mirrors FloatingRelicsModule.drawHead/drawTrail (lines 412-429) because its style palette is private.
        return switch (style) {
            case 1 -> 0xFF8A3C;
            case 2, 3 -> 0xFFFFFF;
            case 4 -> 0x5FB0FF;
            case 5 -> 0xFFB7CE;
            case 6 -> 0x9B4DFF;
            case 7 -> 0x7FE0C0;
            default -> 0x5C8DFF;
        };
    }

    private static PreviewSpec figure(ClientModule module, int accent) {
        int halo = choiceIndex(module, "halo");
        int relics = choiceIndex(module, "relics");
        int wings = choiceIndex(module, "wings");
        int companion = choiceIndex(module, "companion");
        int aura = choiceIndex(module, "aura");
        int detail = (halo & 0x7)
                | ((relics & 0x7) << PreviewSpec.FIGURE_RELIC_SHIFT)
                | ((wings & 0x7) << PreviewSpec.FIGURE_WING_SHIFT)
                | ((companion & 0x3) << PreviewSpec.FIGURE_COMPANION_SHIFT)
                | ((aura & 0x3) << PreviewSpec.FIGURE_AURA_SHIFT)
                | (flag(module, "relic_trail", true) ? PreviewSpec.FIGURE_RELIC_TRAIL : 0)
                | (flag(module, "first_person", false) ? PreviewSpec.FIGURE_FIRST_PERSON : 0);
        return new PreviewSpec(PreviewSpec.Motif.FIGURE, choiceIndex(module, "mode"),
                palette(module, accent, "color_mode", "color"), accent, accent,
                num(module, "opacity", 0.85),
                unit(num(module, "relic_count", 3), 1, 8),
                unit(num(module, "wing_size", 1), 0.5, 2),
                unit(num(module, "orbit_speed", 0.8), 0.1, 3),
                unit(num(module, "wing_spread", 0.85), 0.3, 1.5),
                num(module, "opacity", 0.85),
                detail);
    }

    /** The 3D character card shows the selected character at the selected level of detail. */
    private static PreviewSpec character(ClientModule module, int accent) {
        int detail = choiceIndex(module, "detail");
        double weight = unit(1 + detail, 1, 3);
        return new PreviewSpec(PreviewSpec.Motif.FIGURE, Math.min(31, choiceIndex(module, "character")),
                accent, accent, accent,
                weight,
                0.5,
                weight,
                0.5,
                0.5,
                0.85,
                0);
    }

    private static PreviewSpec totem(ClientModule module, int accent) {
        boolean manual = choice(module, "color_mode", "Theme").equals("Manual");
        int body = manual ? EffectPalette.resolve(EffectPalette.CUSTOM, accent, text(module, "body_color", "4B4030")) : accent;
        int aura = manual ? EffectPalette.resolve(EffectPalette.CUSTOM, accent, text(module, "aura_color", "2B3F5C"))
                : JilatonUi.mix(accent, 0x6E7BFF, 0.55);
        int detail = (flag(module, "ka_pose", true) ? PreviewSpec.TOTEM_POSE : 0)
                | (flag(module, "crest", true) ? PreviewSpec.TOTEM_CREST : 0)
                | (flag(module, "seal", true) ? PreviewSpec.TOTEM_SEAL : 0)
                | (flag(module, "through_walls", false) ? PreviewSpec.TOTEM_WALLS : 0);
        return new PreviewSpec(PreviewSpec.Motif.TOTEM, choiceIndex(module, "look"), body, aura, 0xFFFFFF,
                unit(num(module, "ascent", 1.6), 0, 4),
                0.5,
                0.5,
                unit(6 - num(module, "ka_time", 2.4), 0, 6),
                unit(num(module, "ka_time", 2.4), 0.6, 6),
                num(module, "opacity", 0.65),
                detail);
    }

    private static PreviewSpec crown(ClientModule module, int accent) {
        boolean manual = choice(module, "color_mode", "Theme").equals("Manual");
        int gold = manual ? EffectPalette.resolve(EffectPalette.CUSTOM, accent, text(module, "gold", "C8A24A"))
                : JilatonUi.mix(accent, 0xFFFFFF, 0.35);
        int inlay = manual ? EffectPalette.resolve(EffectPalette.CUSTOM, accent, text(module, "inlay", "243B6B"))
                : JilatonUi.mix(accent, 0x0A0F1E, 0.72);
        int gem = manual ? EffectPalette.resolve(EffectPalette.CUSTOM, accent, text(module, "gem", "5AA6E8"))
                : JilatonUi.mix(accent, 0xFFFFFF, 0.6);
        int detail = (flag(module, "collar", true) ? PreviewSpec.CROWN_COLLAR : 0)
                | (flag(module, "gem_glow", true) ? PreviewSpec.CROWN_GEM_GLOW : 0)
                | (flag(module, "glint", true) ? PreviewSpec.CROWN_GLINT : 0);
        return new PreviewSpec(PreviewSpec.Motif.CROWN, choiceIndex(module, "style"), gold, inlay, gem,
                unit(num(module, "stripes", 5), 3, 9),
                0.5,
                unit(num(module, "size", 1), 0.8, 1.3),
                0.4,
                0.5,
                num(module, "opacity", 0.85),
                detail);
    }

    private static PreviewSpec nowPlaying(ClientModule module, int accent) {
        int detail = (flag(module, "artwork", true) ? PreviewSpec.PANEL_ART : 0)
                | (flag(module, "device", true) ? PreviewSpec.PANEL_DEVICE : 0)
                | (flag(module, "hide_idle", true) ? PreviewSpec.PANEL_HIDE_IDLE : 0);
        return new PreviewSpec(PreviewSpec.Motif.PANEL, choiceIndex(module, "style"), accent, 0xFFFFFF, accent,
                0.7, 0.5, 0.9, 0.3, 0.5, 1, detail);
    }

    private static PreviewSpec musicVisuals(ClientModule module, int accent) {
        String notes = choice(module, "notes", "Stars");
        int detail = (flag(module, "album_panel", true) ? PreviewSpec.PANEL_ART : 0)
                | (flag(module, "track_label", true) ? PreviewSpec.PANEL_DEVICE : 0)
                | (flag(module, "visualizer", true) ? PreviewSpec.PANEL_BARS : 0)
                | (flag(module, "third_person_only", true) ? PreviewSpec.PANEL_COMPACT : 0);
        if (!notes.equals("Off")) detail |= PreviewSpec.PANEL_NOTES;
        double angle = num(module, "panel_angle", -42);
        return new PreviewSpec(PreviewSpec.Motif.PANEL, 16 + choiceIndex(module, "notes"),
                palette(module, accent, "color_mode", "color"), accent, accent,
                unit(num(module, "panel_size", 0.6), 0.3, 1.5),
                0.5,
                0.8,
                0.4,
                unit(angle + 90, 0, 180),
                num(module, "opacity", 0.85),
                detail);
    }

    private static PreviewSpec lyrics(ClientModule module, int accent) {
        int detail = (flag(module, "panel", false) ? PreviewSpec.LYRICS_PANEL : 0)
                | (flag(module, "floating", true) ? PreviewSpec.LYRICS_FLOATING : 0)
                | (flag(module, "accents", true) ? PreviewSpec.LYRICS_ACCENTS : 0)
                | (flag(module, "occlusion", false) ? PreviewSpec.LYRICS_OCCLUSION : 0)
                | (flag(module, "third_person_only", false) ? PreviewSpec.LYRICS_THIRD_PERSON : 0)
                | ((choiceIndex(module, "align") & 0x3) << PreviewSpec.LYRICS_ALIGN_SHIFT);
        return new PreviewSpec(PreviewSpec.Motif.LYRICS, choiceIndex(module, "mode"),
                palette(module, accent, "color_mode", "color"), 0xDCDCDC, accent,
                unit(num(module, "radius", 2.3), 1.5, 4),
                unit(num(module, "lines", 3), 1, 5),
                unit(num(module, "font_size", 1), 0.5, 2),
                unit(1.2 - num(module, "speed", 0.45), 0, 1.2),
                unit(num(module, "spread", 16), 6, 40),
                num(module, "opacity", 0.95),
                detail);
    }

    private static PreviewSpec swing(ClientModule module, int accent) {
        return new PreviewSpec(PreviewSpec.Motif.SWING, choiceIndex(module, "mode"), accent, 0xFFFFFF, accent,
                num(module, "scale", 0.9), 0.5, 0.5, 0.5, 0.5, 0.9, 0);
    }

    private static PreviewSpec hand(ClientModule module, int accent) {
        boolean main = flag(module, "main_enabled", true);
        boolean off = flag(module, "off_enabled", true);
        double scale = num(module, "main_scale", 1);
        int detail = (main ? PreviewSpec.HAND_MAIN : 0) | (off ? PreviewSpec.HAND_OFF : 0);
        return new PreviewSpec(PreviewSpec.Motif.HAND, 0, accent, accent, 0xFFFFFF,
                unit(scale, 0.25, 2),
                0.5,
                unit(num(module, "main_rz", 0), -180, 180),
                0.3,
                unit(num(module, "main_x", 0), -2, 2),
                1,
                detail);
    }

    private static PreviewSpec freecam(ClientModule module, int accent) {
        int detail = (flag(module, "smooth", true) ? PreviewSpec.CAMERA_SMOOTH : 0)
                | (flag(module, "body", true) ? PreviewSpec.CAMERA_BODY : 0)
                | (flag(module, "hands", true) ? PreviewSpec.CAMERA_HANDS : 0);
        return new PreviewSpec(PreviewSpec.Motif.CAMERA, 0, accent, 0xFFFFFF, accent,
                unit(num(module, "speed", 1), 0.1, 50),
                0.5,
                0.5,
                0.4,
                unit(num(module, "vertical_speed", 1), 0.2, 5),
                1,
                detail);
    }

    private static PreviewSpec freeLook(ClientModule module, int accent) {
        int detail = PreviewSpec.CAMERA_HOLD;
        boolean front = choice(module, "perspective", "Back").equals("Front");
        return new PreviewSpec(PreviewSpec.Motif.CAMERA, 1, accent, 0xFFFFFF, accent,
                unit(num(module, "sensitivity", 1), 0.1, 2),
                0.5,
                0.5,
                0.4,
                front ? 1 : 0.3,
                flag(module, "limit_pitch", true) ? 1 : 0.6,
                detail);
    }

    private static PreviewSpec finisher(ClientModule module, int accent) {
        return new PreviewSpec(PreviewSpec.Motif.FIGURE, Math.min(31, choiceIndex(module, "character")),
                accent, 0xFFFFFF, accent,
                unit(num(module, "speed", 1.0), 0.6, 1.6),
                unit(choiceIndex(module, "move"), 0, 3),
                unit(num(module, "volume", 0.7), 0, 1),
                unit(num(module, "shake_strength", 0.45), 0, 1),
                0.5,
                0.85,
                0);
    }

    private static PreviewSpec smoothCamera(ClientModule module, int accent) {
        return new PreviewSpec(PreviewSpec.Motif.CAMERA, 1, accent, 0xFFFFFF, accent,
                1 - unit(num(module, "smoothing", 0.55), 0, 1) * 0.8,
                0.5,
                0.5,
                unit(num(module, "distance", 5), 2, 10),
                0.3,
                1,
                PreviewSpec.CAMERA_HOLD);
    }

    private static PreviewSpec pearl(ClientModule module, int accent) {
        String mode = choice(module, "color_mode", EffectPalette.MONO);
        int start = EffectPalette.resolve(mode, accent, text(module, "start_color", "E8E8E8"));
        int end = flag(module, "gradient", true)
                ? EffectPalette.resolve(mode, accent, text(module, "end_color", "8FA8E8")) : start;
        int hit = EffectPalette.resolve(mode, accent, text(module, "hit_color", "E86A6A"));
        int detail = (flag(module, "trail", true) ? PreviewSpec.PEARL_TRAIL : 0)
                | (flag(module, "others", false) ? PreviewSpec.PEARL_OTHERS : 0)
                | (flag(module, "info", true) ? PreviewSpec.PEARL_INFO : 0)
                | (flag(module, "glow", true) ? PreviewSpec.PEARL_GLOW : 0)
                | (flag(module, "gradient", true) ? PreviewSpec.PEARL_GRADIENT : 0)
                | ((choiceIndex(module, "landing") & 0x3) << PreviewSpec.PEARL_LANDING_SHIFT);
        return new PreviewSpec(PreviewSpec.Motif.PEARL, 0, start, end, hit,
                1,
                0.5,
                unit(num(module, "line_width", 2), 0.5, 5),
                0.5,
                0.6,
                1,
                detail);
    }

    private static PreviewSpec core(ClientModule module, int accent) {
        String mode = choice(module, "color_mode", EffectPalette.MONO);
        int rgb = mode.equals(EffectPalette.MONO) ? 0xF2F2F2 : EffectPalette.resolve(mode, accent, text(module, "color", "E7B8FF"));
        int detail = (flag(module, "sparkles", true) ? PreviewSpec.CORE_SPARKLES : 0)
                | (flag(module, "crystals", true) ? PreviewSpec.CORE_CRYSTALS : 0)
                | (flag(module, "anchors", true) ? PreviewSpec.CORE_ANCHORS : 0);
        return new PreviewSpec(PreviewSpec.Motif.CORE, choiceIndex(module, "style"), rgb, 0xFFFFFF, accent,
                unit(num(module, "range", 16), 4, 32),
                0.5,
                0.6,
                unit(num(module, "speed", 1), 0.2, 3),
                0.6,
                num(module, "opacity", 0.85),
                detail);
    }

    /**
     * Feather colours mirror WingsModule's own {@code material()} blends: the roots sit near-black and the
     * tips carry the accent, which is what the card has to show for the wing to read as the module's.
     */
    private static PreviewSpec wings(ClientModule module, int accent) {
        String style = choice(module, "style", WingMotion.STYLES.get(0));
        int rgb = EffectPalette.resolve(choice(module, "palette", EffectPalette.MONO), accent,
                text(module, "custom", "B9A7FF"));
        int root = switch (style) {
            case "Blade" -> EffectBatch.mixRgb(rgb, 0x101216, 0.84);
            case "Energy" -> EffectBatch.mixRgb(rgb, 0x07060C, 0.55);
            default -> EffectBatch.mixRgb(rgb, 0x121016, 0.78);
        };
        int tip = switch (style) {
            case "Blade" -> EffectBatch.mixRgb(rgb, 0xE8ECF4, 0.72);
            case "Energy" -> EffectBatch.mixRgb(rgb, 0xFFFFFF, 0.30);
            default -> EffectBatch.mixRgb(rgb, 0xF4F2F6, 0.66);
        };
        int detail = (flag(module, "animate", true) ? PreviewSpec.WINGS_ANIMATE : 0)
                | (flag(module, "own", true) ? PreviewSpec.WINGS_OWN : 0)
                | (flag(module, "others", true) ? PreviewSpec.WINGS_OTHERS : 0);
        return new PreviewSpec(PreviewSpec.Motif.WINGS, Math.max(0, WingMotion.STYLES.indexOf(style)),
                root, tip, rgb,
                0.5,
                0.5,
                unit(num(module, "span", 1.0), 0.55, 1.7),
                unit(num(module, "flap_speed", 0.85), 0.2, 2.6),
                0.55,
                num(module, "opacity", 0.9),
                detail);
    }

    private static PreviewSpec halo(ClientModule module, int accent) {
        int rgb = EffectPalette.resolve(choice(module, "palette", EffectPalette.MONO), accent,
                text(module, "custom", "D8C8FF"));
        int detail = (flag(module, "own", true) ? PreviewSpec.HALO_OWN : 0)
                | (flag(module, "others", true) ? PreviewSpec.HALO_OTHERS : 0);
        return new PreviewSpec(PreviewSpec.Motif.HALO, 0, rgb, JilatonUi.mix(rgb, 0xFFFFFF, 0.5), rgb,
                unit(num(module, "radius", 0.40), 0.16, 0.40),
                unit(num(module, "thickness", 0.05), 0.008, 0.05),
                unit(num(module, "bob", 0.04), 0, 0.04),
                unit(num(module, "spin", 2), 0, 2),
                unit(num(module, "tilt", 30) + 30, 0, 60),
                num(module, "opacity", 0.9),
                detail);
    }

    private static PreviewSpec companion(ClientModule module, int accent) {
        String shape = choice(module, "shape", CompanionMotion.SHAPES.get(0));
        int rgb = EffectPalette.resolve(choice(module, "palette", EffectPalette.MONO), accent,
                text(module, "custom", "9FB4FF"));
        int index = Math.max(0, CompanionMotion.SHAPES.indexOf(shape));
        int detail = (index == 1 ? PreviewSpec.COMPANION_RING : 0)
                | (index == 2 ? PreviewSpec.COMPANION_TAIL : 0);
        return new PreviewSpec(PreviewSpec.Motif.COMPANION, index, rgb, JilatonUi.mix(rgb, 0xFFFFFF, 0.55), rgb,
                unit(num(module, "orbit_radius", 2.4), 0.7, 2.4),
                unit(num(module, "spring", 18), 4, 18),
                unit(num(module, "size", 1.6), 0.55, 1.6),
                unit(num(module, "orbit_speed", 1.8), 0.2, 1.8),
                unit(num(module, "height", 1.6), 0.3, 1.6),
                num(module, "opacity", 0.95),
                detail);
    }

    private static PreviewSpec knife(ClientModule module, int accent) {
        int rgb = EffectPalette.resolve(choice(module, "palette", EffectPalette.MONO), accent,
                text(module, "custom", "C9D2E8"));
        return new PreviewSpec(PreviewSpec.Motif.KNIFE, choiceIndex(module, "style"), rgb,
                JilatonUi.mix(rgb, 0xFFFFFF, 0.6), rgb,
                unit(num(module, "side", 0.34), 0.16, 0.34),
                unit(num(module, "height", 0.10), -0.34, 0.10),
                unit(num(module, "scale", 1.4), 0.7, 1.4),
                unit(num(module, "sway", 2), 0, 2),
                unit(num(module, "equip_seconds", 0.6), 0.12, 0.60),
                num(module, "opacity", 1.0),
                0);
    }

    private static PreviewSpec lowFire(ClientModule module, int accent) {
        int tint = ((LowFireModule) module).fireTint(accent);
        return new PreviewSpec(PreviewSpec.Motif.FIRE, 0, tint, tint, tint,
                unit(num(module, "lower", 0.2), 0, 0.45),
                0.5,
                unit(num(module, "size", 0.95), 0.55, 1.2),
                0.5,
                0.5,
                num(module, "opacity", 0.85),
                choiceIndex(module, "tint") == 2 ? PreviewSpec.FIRE_CUSTOM : 0);
    }

    private static PreviewSpec afterglow(ClientModule module, int accent) {
        int detail = (flag(module, "glow", true) ? PreviewSpec.SHARDS_GLOW : 0)
                | (flag(module, "players", true) ? PreviewSpec.SHARDS_PLAYERS : 0)
                | (flag(module, "mobs", true) ? PreviewSpec.SHARDS_MOBS : 0);
        return new PreviewSpec(PreviewSpec.Motif.SHARDS, 0,
                palette(module, accent, "color_mode", "color"), accent, accent,
                unit(num(module, "count", 36), 10, 90),
                unit(num(module, "dissolve", 0.4), 0.1, 1.2),
                unit(num(module, "life", 1.3), 0.4, 2.5),
                unit(num(module, "rise", 0.85), 0.1, 2),
                unit(num(module, "spread", 0.35), 0.05, 1.2),
                0.9,
                detail);
    }

    private static PreviewSpec radiance(ClientModule module, int accent) {
        int detail = (flag(module, "pulse", true) ? PreviewSpec.GLOW_PULSE : 0)
                | (flag(module, "items", false) ? PreviewSpec.GLOW_ITEMS : 0)
                | (flag(module, "mobs", true) ? PreviewSpec.GLOW_MOBS : 0);
        return new PreviewSpec(PreviewSpec.Motif.GLOW, 0,
                palette(module, accent, "color_mode", "color"), accent, accent,
                unit(num(module, "range", 24), 8, 64),
                unit(num(module, "thickness", 0.3), 0.1, 0.6),
                unit(num(module, "strength", 0.18), 0.04, 0.5),
                unit(num(module, "pulse_speed", 0.55), 0, 2),
                0.6,
                1,
                detail);
    }

    private static PreviewSpec weather(ClientModule module, int accent) {
        String mode = choice(module, "mode", "Glassy Rain");
        int variant = 10 + switch (mode) {
            case "Settling Snow" -> 1;
            case "Ember Shower" -> 2;
            default -> 0;
        };
        String tintMode = choice(module, "tint", "Mode default");
        int rgb = switch (tintMode) {
            case "Theme" -> accent;
            case "Custom" -> EffectPalette.resolve(EffectPalette.CUSTOM, accent, text(module, "color", "D8E4FF"));
            default -> switch (mode) {
                case "Settling Snow" -> 0xF2F6FF;
                case "Ember Shower" -> 0xFF9A5A;
                default -> 0xD8E4FF;
            };
        };
        int detail = (flag(module, "glow", true) ? PreviewSpec.WEATHER_GLOW : 0)
                | (flag(module, "covered", false) ? PreviewSpec.WEATHER_COVERED : 0);
        return new PreviewSpec(PreviewSpec.Motif.MOTES, variant, rgb, accent, accent,
                unit(num(module, "density", 0.8), 0.1, 2),
                0.5,
                0.5,
                unit(num(module, "speed", 1), 0.4, 2.5),
                unit(num(module, "radius", 20), 6, 40),
                0.9,
                detail);
    }

    private static int overlayColor(ClientModule module, int accent) {
        String mode = choice(module, "colors", "Theme");
        return OverlayColors.resolve(mode, accent, 20, 20, 8, 0, false, false, false);
    }

    private static int palette(ClientModule module, int accent, String modeId, String hexId) {
        return EffectPalette.resolve(choice(module, modeId, EffectPalette.MONO), accent,
                text(module, hexId, "F2F2F2"));
    }

    private static int hex(ClientModule module, String id, String fallback) {
        try {
            return EffectPalette.resolve(EffectPalette.CUSTOM, 0, text(module, id, fallback));
        } catch (RuntimeException failure) {
            return EffectPalette.resolve(EffectPalette.CUSTOM, 0, fallback);
        }
    }

    private static Setting setting(ClientModule module, String id) {
        for (Setting setting : module.settings()) if (setting.id().equals(id)) return setting;
        return null;
    }

    private static String choice(ClientModule module, String id, String fallback) {
        Setting setting = setting(module, id);
        return setting instanceof ChoiceSetting choice ? choice.value() : fallback;
    }

    private static int choiceIndex(ClientModule module, String id) {
        Setting setting = setting(module, id);
        if (!(setting instanceof ChoiceSetting choice)) return 0;
        return Math.max(0, choice.choices().indexOf(choice.value()));
    }

    private static double num(ClientModule module, String id, double fallback) {
        Setting setting = setting(module, id);
        return setting instanceof NumberSetting number ? number.value() : fallback;
    }

    private static boolean flag(ClientModule module, String id, boolean fallback) {
        Setting setting = setting(module, id);
        return setting instanceof BooleanSetting bool ? bool.value() : fallback;
    }

    private static String text(ClientModule module, String id, String fallback) {
        Setting setting = setting(module, id);
        return setting instanceof TextSetting text ? text.value() : fallback;
    }

    private static double unit(double value, double minimum, double maximum) {
        if (!Double.isFinite(value) || maximum <= minimum) return 0;
        return Math.max(0, Math.min(1, (value - minimum) / (maximum - minimum)));
    }

    static List<String> motifs() {
        return MOTIFS.values().stream().map(Enum::name).toList();
    }
}
