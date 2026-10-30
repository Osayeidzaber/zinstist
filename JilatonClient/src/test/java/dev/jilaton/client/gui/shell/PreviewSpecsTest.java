package dev.jilaton.client.gui.shell;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.module.impl.ItemEspModule;
import dev.jilaton.client.module.impl.PlayerEspModule;
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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks on the settings-to-preview mapping. These never boot Minecraft: they read the
 * modules' public setting API exactly like the renderer does, so a preview cannot silently drift.
 */
class PreviewSpecsTest {
    private static final int ACCENT = 0xFF8878FF;

    @Test
    void everyRenderModuleHasAnExplicitMotif() {
        ModuleManager modules = new ModuleManager();
        List<String> renderIds = new ArrayList<>();
        for (ClientModule module : modules.all()) {
            if (module.category() != ModuleCategory.RENDER) continue;
            renderIds.add(module.id());
            assertTrue(PreviewSpecs.mappedIds().contains(module.id()), "missing preview motif for " + module.id());
            PreviewSpec spec = PreviewSpecs.of(module, ACCENT);
            assertNotNull(spec);
            assertNotNull(spec.motif());
        }
        assertEquals(renderIds.size(), PreviewSpecs.mappedIds().size(),
                "mapped ids must match the registered render modules exactly");
    }

    @Test
    void everySpecIsBoundedAndOpaque() {
        for (ClientModule module : new ModuleManager().all()) {
            if (module.category() != ModuleCategory.RENDER) continue;
            PreviewSpec spec = PreviewSpecs.of(module, ACCENT);
            for (int color : new int[]{spec.primary(), spec.secondary(), spec.glow(), spec.accent2(), spec.tint()}) {
                assertEquals(0xFF, color >>> 24, module.id() + " colour must be opaque");
            }
            for (double value : new double[]{spec.amount(), spec.density(), spec.size(), spec.speed(),
                    spec.spread(), spec.opacity()}) {
                assertTrue(value >= 0 && value <= 1, module.id() + " channel out of range: " + value);
            }
            assertTrue(spec.variant() >= 0 && spec.variant() <= 31, module.id() + " variant out of range");
            assertTrue(spec.detail() >= 0, module.id() + " detail must be a nonnegative bitmask");
        }
    }

    @Test
    void skyPreviewUsesTheResolvedThemeAndLayerValues() {
        ModuleManager modules = new ModuleManager();
        ClientModule sky = modules.get("custom_sky");
        assertNotNull(sky);
        for (String name : SkyThemes.NAMES) {
            setChoice(sky, "theme", name);
            SkyThemes.Theme theme = SkyThemes.resolve(name, hex(sky, "custom_zenith"), hex(sky, "custom_horizon"),
                    hex(sky, "custom_accent"));
            PreviewSpec spec = PreviewSpecs.of(sky, ACCENT);
            assertEquals(PreviewSpec.Motif.SKY, spec.motif());
            assertEquals(PreviewSpec.opaque(theme.zenith()), spec.primary(), name + " zenith");
            assertEquals(PreviewSpec.opaque(theme.horizon()), spec.secondary(), name + " horizon");
            assertEquals(PreviewSpec.opaque(theme.accent()), spec.glow(), name + " accent");
            assertEquals(PreviewSpec.opaque(theme.accent2()), spec.accent2(), name + " accent2");
            assertEquals(PreviewSpec.opaque(theme.starTint()), spec.tint(), name + " star tint");
            for (SkyThemes.Feature feature : SkyThemes.Feature.values()) {
                int bit = 1 << feature.ordinal();
                assertEquals(theme.features().contains(feature), (spec.detail() & bit) != 0,
                        name + " feature " + feature);
            }
        }
        setChoice(sky, "theme", "Custom");
        setText(sky, "custom_zenith", "102030");
        setText(sky, "custom_horizon", "405060");
        setText(sky, "custom_accent", "A0B0C0");
        PreviewSpec custom = PreviewSpecs.of(sky, ACCENT);
        assertEquals(PreviewSpec.opaque(0x102030), custom.primary());
        assertEquals(PreviewSpec.opaque(0x405060), custom.secondary());
        assertEquals(PreviewSpec.opaque(0xA0B0C0), custom.glow());
    }

    @Test
    void skyDensityAndLayerTogglesChangeThePreview() {
        ClientModule sky = new ModuleManager().get("custom_sky");
        assertNotNull(sky);
        PreviewSpec before = PreviewSpecs.of(sky, ACCENT);
        setNumber(sky, "star_density", number(sky, "star_density").maximum());
        PreviewSpec denser = PreviewSpecs.of(sky, ACCENT);
        assertNotEquals(before.amount(), denser.amount());

        setFlag(sky, "dome", !flag(sky, "dome"));
        assertNotEquals(denser.detail() & PreviewSpec.SKY_DOME,
                PreviewSpecs.of(sky, ACCENT).detail() & PreviewSpec.SKY_DOME);
        setNumber(sky, "twinkle", 0);
        assertEquals(0, PreviewSpecs.of(sky, ACCENT).detail() & PreviewSpec.SKY_TWINKLE);
    }

    @Test
    void colourModeModulesFollowTheirOwnResolvers() {
        ModuleManager modules = new ModuleManager();
        String[][] paletteModules = {
                {"ambience", "color_mode", "color"},
                {"target_esp", "color_mode", "color"},
                {"jump_circles", "color_mode", "color"},
                {"trails", "color_mode", "color"},
                {"player_effects", "color_mode", "color"},
                {"spatial_lyrics", "color_mode", "color"},
                {"music_visuals", "color_mode", "color"},
                {"explosive_highlights", "color_mode", "color"}};
        for (String[] entry : paletteModules) {
            ClientModule module = modules.get(entry[0]);
            assertNotNull(module, entry[0]);
            for (String mode : List.of(EffectPalette.MONO, EffectPalette.THEME, EffectPalette.CUSTOM)) {
                setChoice(module, entry[1], mode);
                setText(module, entry[2], "123456");
                int expected = EffectPalette.resolve(mode, ACCENT, "123456");
                assertEquals(PreviewSpec.opaque(expected), PreviewSpecs.of(module, ACCENT).primary(),
                        entry[0] + " in " + mode);
            }
        }
        ClientModule pearl = modules.get("pearl_trajectory");
        assertNotNull(pearl);
        setChoice(pearl, "color_mode", EffectPalette.CUSTOM);
        setText(pearl, "start_color", "112233");
        setText(pearl, "end_color", "445566");
        PreviewSpec pearlSpec = PreviewSpecs.of(pearl, ACCENT);
        assertEquals(PreviewSpec.opaque(EffectPalette.resolve(EffectPalette.CUSTOM, ACCENT, "112233")),
                pearlSpec.primary());
        assertEquals(PreviewSpec.opaque(EffectPalette.resolve(EffectPalette.CUSTOM, ACCENT, "445566")),
                pearlSpec.secondary());
    }

    @Test
    void espColoursComeFromTheModulesOwnAccessors() {
        ModuleManager modules = new ModuleManager();
        ItemEspModule item = (ItemEspModule) modules.get("item_esp");
        assertNotNull(item);
        for (String mode : List.of("Gold", "Theme", "Rainbow")) {
            setChoice(item, "colors", mode);
            assertEquals(PreviewSpec.opaque(item.color(ACCENT, 0)), PreviewSpecs.of(item, ACCENT).primary(),
                    "item_esp " + mode);
        }
        PlayerEspModule player = (PlayerEspModule) modules.get("player_esp");
        assertNotNull(player);
        for (String mode : List.of("Theme", "Health", "Distance", "Rainbow")) {
            setChoice(player, "colors", mode);
            int expected = OverlayColors.resolve(mode, ACCENT, 20, 20, 8, 0, false, false, false);
            assertEquals(PreviewSpec.opaque(expected), PreviewSpecs.of(player, ACCENT).primary(),
                    "player_esp " + mode);
        }
    }

    @Test
    void wingsPreviewUsesTheModulesOwnGeometry() {
        ClientModule wings = new ModuleManager().get("wings");
        assertNotNull(wings);
        setChoice(wings, "palette", EffectPalette.THEME);
        for (String style : WingMotion.STYLES) {
            setChoice(wings, "style", style);
            PreviewSpec spec = PreviewSpecs.of(wings, ACCENT);
            assertEquals(PreviewSpec.Motif.WINGS, spec.motif());
            assertEquals(WingMotion.STYLES.indexOf(style), spec.variant(), style + " style index");
            assertEquals(PreviewSpec.opaque(ACCENT), spec.glow(), style + " accent");
        }
        setChoice(wings, "style", WingMotion.STYLES.get(0));
        setNumber(wings, "span", 1.7);
        double wide = PreviewSpecs.of(wings, ACCENT).size();
        setNumber(wings, "span", 0.55);
        assertTrue(wide > PreviewSpecs.of(wings, ACCENT).size(), "span must widen the preview");
        setNumber(wings, "flap_speed", 2.6);
        double fast = PreviewSpecs.of(wings, ACCENT).speed();
        setNumber(wings, "flap_speed", 0.2);
        assertTrue(fast > PreviewSpecs.of(wings, ACCENT).speed(), "flap speed must drive the preview");
        setNumber(wings, "opacity", 0.15);
        assertEquals(0.15, PreviewSpecs.of(wings, ACCENT).opacity(), 1e-9);
        setChoice(wings, "palette", EffectPalette.CUSTOM);
        setText(wings, "custom", "336699");
        assertEquals(PreviewSpec.opaque(EffectPalette.resolve(EffectPalette.CUSTOM, ACCENT, "336699")),
                PreviewSpecs.of(wings, ACCENT).glow());
        setFlag(wings, "animate", false);
        assertEquals(0, PreviewSpecs.of(wings, ACCENT).detail() & PreviewSpec.WINGS_ANIMATE);
    }

    @Test
    void cosmeticModulesUseTheirOwnPaletteAndShape() {
        ModuleManager modules = new ModuleManager();
        ClientModule halo = modules.get("halo");
        ClientModule companion = modules.get("companion");
        ClientModule knife = modules.get("cosmetic_knife");
        assertNotNull(halo, "halo");
        assertNotNull(companion, "companion");
        assertNotNull(knife, "cosmetic_knife");
        for (ClientModule module : List.of(halo, companion, knife)) {
            for (String mode : List.of(EffectPalette.MONO, EffectPalette.THEME, EffectPalette.CUSTOM)) {
                setChoice(module, "palette", mode);
                setText(module, "custom", "123456");
                assertEquals(PreviewSpec.opaque(EffectPalette.resolve(mode, ACCENT, "123456")),
                        PreviewSpecs.of(module, ACCENT).primary(), module.id() + " in " + mode);
            }
        }
        for (String shape : CompanionMotion.SHAPES) {
            setChoice(companion, "shape", shape);
            PreviewSpec spec = PreviewSpecs.of(companion, ACCENT);
            assertEquals(PreviewSpec.Motif.COMPANION, spec.motif());
            assertEquals(CompanionMotion.SHAPES.indexOf(shape), spec.variant(), shape + " variant");
        }
        setNumber(knife, "scale", 1.4);
        double large = PreviewSpecs.of(knife, ACCENT).size();
        setNumber(knife, "scale", 0.7);
        assertTrue(large > PreviewSpecs.of(knife, ACCENT).size(), "knife scale must size the card");
        setNumber(halo, "radius", 0.40);
        double wide = PreviewSpecs.of(halo, ACCENT).amount();
        setNumber(halo, "radius", 0.16);
        assertTrue(wide > PreviewSpecs.of(halo, ACCENT).amount(), "halo radius must widen the ring");
    }

    @Test
    void panelVariantsDoNotCollide() {
        ModuleManager modules = new ModuleManager();
        ClientModule nowPlaying = modules.get("now_playing");
        ClientModule nametags = modules.get("nametags");
        ClientModule music = modules.get("music_visuals");
        assertNotNull(nowPlaying);
        assertNotNull(nametags);
        assertNotNull(music);
        Set<Integer> variants = new HashSet<>();
        for (String style : List.of("Minimal", "Compact", "Expanded")) {
            setChoice(nowPlaying, "style", style);
            int variant = PreviewSpecs.of(nowPlaying, ACCENT).variant();
            assertTrue(variant >= 0 && variant <= 2, "now_playing variant " + variant);
            variants.add(variant);
        }
        assertEquals(3, variants.size(), "each now_playing style needs its own layout");
        variants.add(PreviewSpecs.of(nametags, ACCENT).variant());
        for (String notes : List.of("Off", "Stars", "Orbs")) {
            setChoice(music, "notes", notes);
            variants.add(PreviewSpecs.of(music, ACCENT).variant());
        }
        assertEquals(7, variants.size(), "now_playing, nametags and music layouts must not overlap");
    }

    @Test
    void reselectingAChoiceChangesTheSpecImmediately() {
        ModuleManager modules = new ModuleManager();
        String[][] choices = {
                {"ambience", "style"}, {"custom_crosshair", "shape"}, {"target_esp", "style"},
                {"player_esp", "mode"}, {"item_esp", "mode"}, {"jump_circles", "style"},
                {"trails", "style"}, {"floating_relics", "style"}, {"player_effects", "halo"},
                {"totem_spirit", "look"}, {"pharaoh_headdress", "style"}, {"spatial_lyrics", "mode"},
                {"swing_animations", "mode"}, {"free_look", "perspective"}, {"pearl_trajectory", "landing"},
                {"explosive_highlights", "style"}};
        for (String[] entry : choices) {
            ClientModule module = modules.get(entry[0]);
            assertNotNull(module, entry[0]);
            Setting setting = setting(module, entry[1]);
            assertTrue(setting instanceof ChoiceSetting, entry[0] + "/" + entry[1]);
            ChoiceSetting choice = (ChoiceSetting) setting;
            String original = choice.value();
            PreviewSpec before = PreviewSpecs.of(module, ACCENT);
            String next = choice.choices().stream().filter(value -> !value.equals(original)).findFirst().orElseThrow();
            choice.setValue(next);
            PreviewSpec after = PreviewSpecs.of(module, ACCENT);
            assertNotEquals(before, after, entry[0] + " preview did not react to " + entry[1]);
            choice.setValue(original);
            assertEquals(before, PreviewSpecs.of(module, ACCENT), entry[0] + " preview must be a pure function");
        }
    }

    @Test
    void everyVisualModuleRespondsToAtLeastOneSetting() {
        for (ClientModule module : new ModuleManager().all()) {
            if (module.category() != ModuleCategory.RENDER) continue;
            PreviewSpec base = PreviewSpecs.of(module, ACCENT);
            assertTrue(respondsToASetting(module, base), module.id() + " preview ignores every setting");
        }
    }

    private static boolean respondsToASetting(ClientModule module, PreviewSpec base) {
        for (Setting setting : module.settings()) {
            if (setting instanceof BooleanSetting bool) {
                bool.setValue(!bool.value());
                boolean changed = !PreviewSpecs.of(module, ACCENT).equals(base);
                bool.setValue(!bool.value());
                if (changed) return true;
            } else if (setting instanceof ChoiceSetting choice) {
                String original = choice.value();
                for (String value : choice.choices()) {
                    if (value.equals(original)) continue;
                    choice.setValue(value);
                    boolean changed = !PreviewSpecs.of(module, ACCENT).equals(base);
                    choice.setValue(original);
                    if (changed) return true;
                }
            } else if (setting instanceof NumberSetting number) {
                double original = number.value();
                number.setValue(original == number.maximum() ? number.minimum() : number.maximum());
                boolean changed = !PreviewSpecs.of(module, ACCENT).equals(base);
                number.setValue(original);
                if (changed) return true;
            } else if (setting instanceof TextSetting text) {
                String original = text.value();
                text.setValue("010203");
                boolean changed = !PreviewSpecs.of(module, ACCENT).equals(base);
                text.setValue(original);
                if (changed) return true;
            }
        }
        return false;
    }

    @Test
    void keySettingsMapToNamedChannels() {
        ModuleManager modules = new ModuleManager();
        assertChannel(modules, "custom_crosshair", "size", number -> number.value() > 5 ? 20 : 1, "amount");
        assertChannel(modules, "custom_fov", "fov", number -> number.value() > 60 ? 160 : 30, "amount");
        assertChannel(modules, "zoom", "fov", number -> number.value() > 60 ? 90 : 5, "amount");
        assertChannel(modules, "hit_particles", "amount", number -> number.value() > 20 ? 40 : 1, "amount");
        assertChannel(modules, "hit_effects", "count", number -> number.value() > 1 ? 3 : 0.25, "amount");
        assertChannel(modules, "nametags", "scale", number -> number.value() > 1 ? 2 : 0.5, "amount");
        assertChannel(modules, "spatial_lyrics", "lines", number -> number.value() > 3 ? 5 : 1, "density");
        assertChannel(modules, "floating_relics", "count", number -> number.value() > 8 ? 32 : 1, "amount");
        assertChannel(modules, "player_effects", "relic_count", number -> number.value() > 4 ? 8 : 1, "density");
        assertChannel(modules, "view_model", "main_scale", number -> number.value() > 1 ? 2 : 0.25, "amount");
        assertChannel(modules, "trails", "width", number -> number.value() > 3 ? 5 : 1, "size");

        ClientModule effects = modules.get("player_effects");
        assertNotNull(effects);
        setChoice(effects, "halo", "Crown");
        assertTrue((PreviewSpecs.of(effects, ACCENT).detail() & PreviewSpec.FIGURE_HALO) == 5);
        setChoice(effects, "wings", "Glow");
        assertTrue((PreviewSpecs.of(effects, ACCENT).detail() & PreviewSpec.FIGURE_WINGS)
                == 5 << PreviewSpec.FIGURE_WING_SHIFT);

        ClientModule nametags = modules.get("nametags");
        assertNotNull(nametags);
        boolean health = flag(nametags, "health");
        assertTrue(((PreviewSpecs.of(nametags, ACCENT).detail() & PreviewSpec.PANEL_HEALTH) != 0) == health);
        setFlag(nametags, "compact_background", true);
        assertTrue((PreviewSpecs.of(nametags, ACCENT).detail() & PreviewSpec.PANEL_BACKGROUND) != 0);

        ClientModule pearl = modules.get("pearl_trajectory");
        assertNotNull(pearl);
        setFlag(pearl, "gradient", true);
        assertTrue((PreviewSpecs.of(pearl, ACCENT).detail() & PreviewSpec.PEARL_GRADIENT) != 0);
        setFlag(pearl, "gradient", false);
        assertEquals(PreviewSpecs.of(pearl, ACCENT).secondary(), PreviewSpecs.of(pearl, ACCENT).primary());
    }

    private interface NumberChange {
        double apply(NumberSetting number);
    }

    private static void assertChannel(ModuleManager modules, String moduleId, String settingId, NumberChange change,
                                      String channel) {
        ClientModule module = modules.get(moduleId);
        assertNotNull(module, moduleId);
        NumberSetting number = number(module, settingId);
        double original = number.value();
        PreviewSpec before = PreviewSpecs.of(module, ACCENT);
        number.setValue(change.apply(number));
        PreviewSpec after = PreviewSpecs.of(module, ACCENT);
        assertNotEquals(channelValue(before, channel), channelValue(after, channel),
                moduleId + "/" + settingId + " must drive " + channel);
        number.setValue(original);
    }

    private static double channelValue(PreviewSpec spec, String channel) {
        return switch (channel) {
            case "amount" -> spec.amount();
            case "density" -> spec.density();
            case "size" -> spec.size();
            case "speed" -> spec.speed();
            case "spread" -> spec.spread();
            default -> spec.opacity();
        };
    }

    @Test
    void reducedMotionFreezesAnimationPhase() {
        assertEquals(0.25, VisualPreview.advancePhase(0.25, 1.0, 1.0, true), 1e-9);
        double advanced = VisualPreview.advancePhase(0.25, 0.05, 1.0, false);
        assertTrue(advanced > 0.25);
        assertEquals(0.0, VisualPreview.advancePhase(0.0, 0.0, 1.0, false), 1e-9);
        assertTrue(VisualPreview.advancePhase(0, 0.05, 0, false) > 0, "slow previews still drift");
        assertEquals(0.4, VisualPreview.advanceSheen(0.4, 1.0, true), 1e-9);
        assertTrue(VisualPreview.advanceSheen(0.4, 0.05, false) > 0.4);
        double fromInvalid = VisualPreview.advancePhase(Double.NaN, 0.05, 1.0, false);
        assertTrue(fromInvalid >= 0 && fromInvalid < 1, "an invalid phase must restart inside the cycle");
    }

    @Test
    void specNormalisesOutOfRangeValues() {
        PreviewSpec spec = new PreviewSpec(PreviewSpec.Motif.BOX, 99, -1, 0x12345678, 0,
                Double.NaN, -3, 4, Double.POSITIVE_INFINITY, 0.5, 2, -5);
        assertEquals(31, spec.variant());
        assertEquals(0xFF, spec.primary() >>> 24);
        assertEquals(0xFF, spec.secondary() >>> 24);
        assertEquals(0, spec.amount());
        assertEquals(0, spec.density());
        assertEquals(1, spec.size());
        assertEquals(0, spec.speed());
        assertEquals(1, spec.opacity());
        assertEquals(-5, spec.detail());
    }

    // ---------------------------------------------------------------- helpers

    private static Setting setting(ClientModule module, String id) {
        for (Setting setting : module.settings()) if (setting.id().equals(id)) return setting;
        throw new AssertionError("missing setting " + module.id() + "/" + id);
    }

    private static NumberSetting number(ClientModule module, String id) {
        Setting setting = setting(module, id);
        assertTrue(setting instanceof NumberSetting, module.id() + "/" + id + " is not numeric");
        return (NumberSetting) setting;
    }

    private static void setNumber(ClientModule module, String id, double value) {
        number(module, id).setValue(value);
    }

    private static void setChoice(ClientModule module, String id, String value) {
        Setting setting = setting(module, id);
        assertTrue(setting instanceof ChoiceSetting, module.id() + "/" + id + " is not a choice");
        ((ChoiceSetting) setting).setValue(value);
    }

    private static boolean flag(ClientModule module, String id) {
        Setting setting = setting(module, id);
        assertTrue(setting instanceof BooleanSetting, module.id() + "/" + id + " is not a flag");
        return ((BooleanSetting) setting).value();
    }

    private static void setFlag(ClientModule module, String id, boolean value) {
        Setting setting = setting(module, id);
        assertTrue(setting instanceof BooleanSetting, module.id() + "/" + id + " is not a flag");
        ((BooleanSetting) setting).setValue(value);
    }

    private static void setText(ClientModule module, String id, String value) {
        Setting setting = setting(module, id);
        assertTrue(setting instanceof TextSetting, module.id() + "/" + id + " is not text");
        ((TextSetting) setting).setValue(value);
    }

    private static int hex(ClientModule module, String id) {
        Setting setting = setting(module, id);
        return EffectPalette.resolve(EffectPalette.CUSTOM, 0, ((TextSetting) setting).value());
    }
}
