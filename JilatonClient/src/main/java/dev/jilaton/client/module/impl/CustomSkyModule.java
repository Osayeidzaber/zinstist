package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.render.fx.SkyPackTextures;
import dev.jilaton.client.render.fx.SkyThemes;
import dev.jilaton.client.render.fx.SkyThemes.Feature;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;

import java.util.Random;

/**
 * Procedural sky themes. Vanilla still draws its sky disc, but the zenith color, horizon fog color, stars
 * and sun/moon are steered through the sky-state and fog hooks. Celestial layers (stars, galaxy band,
 * nebulae, aurora, planet, black hole, moon) are geometry placed beyond the terrain render distance and
 * depth-tested, so terrain occludes them correctly. Only the overworld skybox is affected by default.
 */
public final class CustomSkyModule extends ClientModule implements FxEmitter {
    private static final int MAX_STARS = 2400;
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final boolean VERIFY = System.getProperty("jilaton.visualVerify") != null;
    private int debugFrames;
    private int debugEmits;
    private final ChoiceSetting theme = addSetting(new ChoiceSetting("theme", "Theme", SkyThemes.NAMES, "Deep Space"));
    private final BooleanSetting followDay = addSetting(new BooleanSetting("follow_day", "Only at night (blend with day cycle)", false));
    private final NumberSetting colorStrength = addSetting(new NumberSetting("color_strength", "Sky color strength", 0, 1, 0.05, 0.9));
    private final NumberSetting starDensity = addSetting(new NumberSetting("star_density", "Star density", 0, 2, 0.05, 1));
    private final NumberSetting starBrightness = addSetting(new NumberSetting("star_brightness", "Star brightness", 0.1, 1.5, 0.05, 1));
    private final NumberSetting twinkle = addSetting(new NumberSetting("twinkle", "Twinkle", 0, 1, 0.05, 0.5));
    private final NumberSetting celestial = addSetting(new NumberSetting("celestial", "Celestial intensity", 0, 1.5, 0.05, 1));
    private final NumberSetting animation = addSetting(new NumberSetting("animation", "Animation speed", 0, 3, 0.05, 1));
    private final NumberSetting fogTint = addSetting(new NumberSetting("fog_tint", "Horizon fog tint", 0, 1, 0.05, 0.75));
    private final BooleanSetting dome = addSetting(new BooleanSetting("dome", "Gradient dome", true));
    private final NumberSetting domeBrightness = addSetting(new NumberSetting("dome_brightness", "Dome brightness", 0.2, 2, 0.05, 1));
    private final NumberSetting clouds = addSetting(new NumberSetting("clouds", "Horizon clouds", 0, 1, 0.05, 0.6));
    private final BooleanSetting hideClouds = addSetting(new BooleanSetting("hide_clouds", "Hide vanilla clouds", true));
    private final BooleanSetting hideSun = addSetting(new BooleanSetting("hide_sun", "Hide vanilla sun & moon", true));
    private final BooleanSetting hideSunset = addSetting(new BooleanSetting("hide_sunset", "Hide vanilla sunset glow", false));
    private final BooleanSetting allDimensions = addSetting(new BooleanSetting("all_dimensions", "Also in Nether/End layers", false));
    private final BooleanSetting pack = addSetting(new BooleanSetting("dramatic_skys", "Dramatic Skys layers", true));
    private final TextSetting zenith = addSetting(EffectPalette.hexSetting("custom_zenith", "Custom zenith hex", "020306"));
    private final TextSetting horizon = addSetting(EffectPalette.hexSetting("custom_horizon", "Custom horizon hex", "11131A"));
    private final TextSetting accent = addSetting(EffectPalette.hexSetting("custom_accent", "Custom accent hex", "9AA8FF"));

    private final float[] sx = new float[MAX_STARS], sy = new float[MAX_STARS], sz = new float[MAX_STARS];
    private final float[] size = new float[MAX_STARS], phase = new float[MAX_STARS], tintMix = new float[MAX_STARS];
    // Last values observed from vanilla's sky extraction; read by the render-thread emitter.
    private float starAngle;
    private float sunAngle;
    private float vanillaStars;
    private float rain;
    private int sunsetColor;
    private boolean overworld = true;
    private SkyThemes.Theme liveTheme;
    private double shootingStart = -1, shootingSeed;
    private final Random random = new Random();

    public CustomSkyModule() {
        super("custom_sky", "Sky", "Dramatic Skys layering with blended dawn/dusk, plus procedural themes: deep space, aurora, galaxy, black hole, ringed planet.", ModuleCategory.RENDER);
        Random seeded = new Random(0x51C7L);
        for (int i = 0; i < MAX_STARS; i++) {
            double u = seeded.nextDouble() * 2 - 1, a = seeded.nextDouble() * Math.PI * 2, r = Math.sqrt(1 - u * u);
            sx[i] = (float) (r * Math.cos(a)); sy[i] = (float) u; sz[i] = (float) (r * Math.sin(a));
            double m = seeded.nextDouble();
            size[i] = (float) (0.6 + Math.pow(m, 6) * 2.8);
            phase[i] = (float) (seeded.nextDouble() * Math.PI * 2);
            tintMix[i] = (float) seeded.nextDouble();
        }
    }

    private SkyThemes.Theme current() {
        return SkyThemes.resolve(theme.value(), hex(zenith), hex(horizon), hex(accent));
    }

    private static int hex(TextSetting setting) { return EffectPalette.resolve(EffectPalette.CUSTOM, 0, setting.value()); }

    private boolean applies() { return enabled() && (overworld || allDimensions.value()); }

    /** The pack layers ship in the jar; a locally installed pack only overrides their textures. */
    private boolean packActive() { return pack.value(); }

    /** Ticks through the current day, including the render partial tick. */
    private static double dayTicks(Minecraft client, double partial) {
        long time = client.level == null ? 0 : client.level.getDayTime();
        return ((time % 24000) + 24000) % 24000 + partial;
    }

    private static double dayTicks(FxFrame f) { return dayTicks(f.client, f.partial); }

    /**
     * Palette time for the render hooks, which run outside {@link FxFrame}: whole ticks plus the frame's
     * partial tick, so the fog and disc colours move every frame instead of stepping at the tick rate
     * (the step was visible as a band crawling through the dawn/dusk gradient).
     */
    private static double paletteTicks(Minecraft client) {
        return dayTicks(client, client.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }

    /** Ramp that wraps around midnight, eased at both ends so dawn and dusk never step. */
    private static double ramp(double tick, double start, double end) {
        if (end >= start) return smoothstep(clamp01((tick - start) / (end - start)));
        double span = 24000 - start + end;
        return smoothstep(clamp01((tick >= start ? tick - start : tick + 24000 - start) / span));
    }

    private static double smoothstep(double t) { return t * t * (3 - 2 * t); }

    private static double fade(double tick, double inStart, double inEnd, double outStart, double outEnd) {
        return ramp(tick, inStart, inEnd) * (1 - ramp(tick, outStart, outEnd));
    }

    /**
     * Continuous 6000→30000 tick axis for the pack's fade windows, anchored at noon where day, night and
     * dawn glow are all at steady state. The pack's day runs from dawn to dusk across midnight
     * (23666→11666); mapping pre-noon ticks onto the next day keeps every window non-wrapping. On the raw
     * tick the evening fade-out (11666→12333) and the dawn fade-in (23666→24333) sat on opposite sides of
     * the wrap, so 22666–23666 fell outside day, night and dusk at once and the sky rendered black.
     */
    private static double dayAxis(double tick) { return tick < 6000 ? tick + 24000 : tick; }

    /** Daylight ramp on the extended axis: 1 by day, 0 by night, eased through both 20-minute skies. */
    private static double dayLight(double t) { return 1 - fade(t, 11666, 12333, 23666, 24333); }

    /**
     * Base sky and fog colours for the pack: they only show through where no pack layer covers the sky, and
     * they define the fog the terrain fades into, so they track the same day/night/sunset windows.
     */
    private int[] packPalette(double tick) {
        double t = dayAxis(tick);
        double day = dayLight(t);
        double glow = Math.max(fade(t, 11666, 12333, 13333, 13666), fade(t, 22333, 22666, 23666, 24333));
        double wet = clamp01(rain);
        int zenith = EffectBatch.mixRgb(0x03050B, 0x24486E, day);
        int horizon = EffectBatch.mixRgb(0x0A111C, 0x9FB8CC, day);
        zenith = EffectBatch.mixRgb(zenith, 0x39243A, glow * 0.45);
        horizon = EffectBatch.mixRgb(horizon, 0xB26A3E, glow * 0.7);
        zenith = EffectBatch.mixRgb(zenith, 0x171B21, wet * 0.72);
        horizon = EffectBatch.mixRgb(horizon, 0x2A313A, wet * 0.72);
        return new int[]{zenith, horizon};
    }

    /**
     * The Dramatic Skys layers, reproduced natively on the pack's own fade schedule. Every layer is
     * screen-blended, so the pack's black backgrounds vanish and only the painted sky remains; the vanilla
     * sun and moon stay visible through them, as they do with the pack installed.
     */
    private void packSky(FxFrame f) {
        if (!overworld) return;
        double tick = dayTicks(f);
        double clear = 1 - clamp01(rain * 1.5);
        double strength = clear * f.intensity * Math.min(1.5, celestial.value());
        if (strength <= 0.02) return;
        double radius = f.client.options.getEffectiveRenderDistance() * 16 * 1.6 + 48;
        double angle = Math.toRadians(tick / 24000.0 * 360.0);
        double t = dayAxis(tick);
        double day = dayLight(t);
        double night = fade(t, 13333, 13666, 22333, 22666);
        double dusk = Math.max(fade(t, 11666, 12333, 13333, 13666), fade(t, 22333, 22666, 23666, 24333));
        layer(f, "stars", radius, 0, angle, night * strength);
        layer(f, "day", radius, angle, 0, day * strength * 0.85);
        layer(f, "night", radius, angle, 0, night * strength * 0.85);
        layer(f, "sun", radius, angle, 0, dusk * strength * 0.9);
        layer(f, "sunflare", radius, 0, angle, dusk * strength * 0.8);
        packCelestials(f, radius);
    }

    private static void layer(FxFrame f, String name, double radius, double yaw, double roll, double strength) {
        if (strength <= 0.004) return;
        f.skyCube(SkyPackTextures.get(name), radius, yaw, roll, (float) strength);
    }

    /** Soft bloom around the real sun and moon, so the painted sky still reads as a live sky. */
    private void packCelestials(FxFrame f, double radius) {
        double a = celestial.value() * Math.min(1, f.intensity);
        if (a <= 0.01) return;
        Sky flat = new Sky(f, radius * 0.995, 1, 0);
        double[] sun = {-Math.sin(sunAngle), Math.cos(sunAngle), 0};
        double[] moon = {-sun[0], -sun[1], -sun[2]};
        if (sun[1] > -0.10) {
            double k = a * clamp01((sun[1] + 0.10) / 0.45);
            flat.disc(sun, 0.20, FxShapes.argb(0xFFB46A, 0.05 * k), FxShapes.argb(0xFFB46A, 0), 24);
            flat.disc(sun, 0.075, FxShapes.argb(0xFFD9A0, 0.12 * k), FxShapes.argb(0xFFE9C4, 0), 24);
        }
        if (moon[1] > -0.10) {
            double k = a * clamp01((moon[1] + 0.10) / 0.45);
            flat.disc(moon, 0.17, FxShapes.argb(0x9FB4D8, 0.045 * k), FxShapes.argb(0x9FB4D8, 0), 24);
            flat.disc(moon, 0.062, FxShapes.argb(0xE8EEFA, 0.10 * k), FxShapes.argb(0xE8EEFA, 0), 24);
        }
    }

    /** 0 in daylight → 1 at night when following the day cycle; always 1 otherwise. */
    private double nightFactor() {
        boolean allDay = current().features().contains(Feature.ALL_DAY);
        double night = followDay.value() && !allDay ? Math.max(0, Math.min(1, vanillaStars * 2.2)) : 1;
        return night * (1 - rain * 0.85);
    }

    /** Sky-extraction hook: records vanilla state, then steers color, stars and sunset glow. */
    public void adjust(SkyRenderState state) {
        overworld = state.skybox == DimensionType.Skybox.OVERWORLD;
        starAngle = state.starAngle;
        sunAngle = state.sunAngle;
        vanillaStars = state.starBrightness;
        rain = 1 - state.rainBrightness;
        sunsetColor = state.sunriseAndSunsetColor & 0xFFFFFF;
        if (!applies() || state.skybox == DimensionType.Skybox.END) return;
        if (packActive()) {
            if (state.skybox != DimensionType.Skybox.OVERWORLD) return;
            int[] palette = packPalette(paletteTicks(Minecraft.getInstance()));
            state.skyColor = 0xFF000000 | palette[0];
            state.starBrightness = 0;
            // The pack's own sunset layer provides the glow; vanilla's horizon tint would double it up.
            state.sunriseAndSunsetColor = 0;
            return;
        }
        double night = nightFactor();
        if (night <= 0.01) return;
        double k = night * colorStrength.value();
        int before = state.skyColor;
        state.starBrightness = 0;
        if (dome.value()) {
            // The dome replaces the vanilla disc, so the disc itself is painted black to avoid a colored fringe.
            state.skyColor = 0xFF000000;
        } else {
            int target = (liveTheme != null ? liveTheme : current()).zenith();
            int tint = EffectBatch.mixRgb(before & 0xFFFFFF, target, k * 0.85);
            state.skyColor = (before & 0xFF000000) | tint;
        }
        if (hideSunset.value() || k > 0.5) state.sunriseAndSunsetColor &= 0x00FFFFFF;
        if (VERIFY && debugFrames++ % 20 == 0)
            LOGGER.info("[SkyDebug] adjust theme={} skybox={} vanillaStars={} rain={} night={} k={} sky={}->{}",
                    theme.value(), state.skybox, vanillaStars, rain, night, k,
                    Integer.toHexString(before), Integer.toHexString(state.skyColor));
    }

    public boolean hideVanillaCelestial() {
        return applies() && !packActive() && hideSun.value() && nightFactor() > 0.02;
    }

    /** Fog hook: pulls the horizon toward the theme so the sky gradient reads correctly. */
    public void adjustFog(Vector4f fog, net.minecraft.client.Camera camera) {
        if (!applies() || camera.getFluidInCamera() != FogType.NONE) return;
        if (packActive()) {
            int horizon = packPalette(paletteTicks(Minecraft.getInstance()))[1];
            float k = (float) Math.min(1, fogTint.value() * 1.15);
            fog.x += (((horizon >> 16) & 0xFF) / 255F - fog.x) * k;
            fog.y += (((horizon >> 8) & 0xFF) / 255F - fog.y) * k;
            fog.z += ((horizon & 0xFF) / 255F - fog.z) * k;
            return;
        }
        double k = nightFactor() * fogTint.value();
        if (k <= 0) return;
        int target = (liveTheme != null ? liveTheme : current()).horizon();
        fog.x = (float) (fog.x + (((target >> 16) & 0xFF) / 255.0 - fog.x) * k);
        fog.y = (float) (fog.y + (((target >> 8) & 0xFF) / 255.0 - fog.y) * k);
        fog.z = (float) (fog.z + ((target & 0xFF) / 255.0 - fog.z) * k);
    }

    public boolean hideVanillaClouds() {
        return applies() && hideClouds.value() && (packActive() || nightFactor() > 0.01);
    }

    @Override
    public void emit(FxFrame f) {
        if (!applies() || f.client.level == null) return;
        if (f.client.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) return;
        if (packActive()) { packSky(f); return; }
        double night = nightFactor();
        if (VERIFY && debugEmits++ % 20 == 0)
            LOGGER.info("[SkyDebug] emit theme={} night={} camera={},{},{}", theme.value(), night,
                    (int) f.camX, (int) f.camY, (int) f.camZ);
        if (night <= 0.01) return;
        SkyThemes.Theme t = smooth(current(), f.dt);
        double intensity = night * f.intensity;
        double radius = f.client.options.getEffectiveRenderDistance() * 16 * 1.6 + 48;
        double time = f.seconds() * animation.value() * (f.reducedMotion ? 0.2 : 1);
        double cosA = Math.cos(starAngle), sinA = Math.sin(starAngle);
        Sky sky = new Sky(f, radius, cosA, sinA);
        if (dome.value()) dome(f, radius, t, intensity);
        sunGlow(f, radius, t, intensity);
        if (t.features().contains(Feature.NEBULA)) nebula(sky, t, intensity);
        if (t.features().contains(Feature.MILKY_WAY)) milkyWay(sky, t, intensity, time);
        if (t.features().contains(Feature.STARS)) stars(sky, t, intensity, time);
        if (t.features().contains(Feature.MOON)) moon(sky, t, intensity);
        if (t.features().contains(Feature.PLANET)) planet(sky, t, intensity, false);
        if (t.features().contains(Feature.BLACK_HOLE)) planet(sky, t, intensity, true);
        if (clouds.value() > 0.01) cloudBands(f, radius, t, intensity, time);
        if (t.features().contains(Feature.AURORA)) aurora(f, radius, t, intensity, time);
        if (t.features().contains(Feature.SUNSET_GLOW)) sunsetGlow(f, radius, t, intensity);
        if (t.features().contains(Feature.SHOOTING_STARS)) shootingStar(sky, intensity, f.seconds());
        if (t.features().contains(Feature.LIGHT_STREAKS)) lightStreaks(f, radius, t, intensity, time);
    }

    /** Smoothly blends palette changes so switching themes fades instead of snapping. */
    private SkyThemes.Theme smooth(SkyThemes.Theme target, double dt) {
        if (liveTheme == null) { liveTheme = target; return target; }
        double k = 1 - Math.exp(-Math.min(0.12, Math.max(0.0, dt)) * 7);
        liveTheme = new SkyThemes.Theme(target.name(),
                lerpColor(liveTheme.zenith(), target.zenith(), k),
                lerpColor(liveTheme.horizon(), target.horizon(), k),
                target.stars(),
                lerpColor(liveTheme.starTint(), target.starTint(), k),
                lerpColor(liveTheme.accent(), target.accent(), k),
                lerpColor(liveTheme.accent2(), target.accent2(), k),
                target.features());
        return liveTheme;
    }

    private static int lerpColor(int a, int b, double k) {
        int r = (int) (((a >> 16) & 0xFF) + ((((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * k));
        int g = (int) (((a >> 8) & 0xFF) + ((((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * k));
        int bl = (int) ((a & 0xFF) + (((b & 0xFF) - (a & 0xFF)) * k));
        return (r << 16) | (g << 8) | bl;
    }

    /**
     * Opaque gradient dome. It replaces the vanilla sky disc entirely: elevation bands run from below the
     * horizon to the zenith, the horizon gets a soft accent bloom and the whole shell is desaturated by rain.
     */
    private void dome(FxFrame f, double radius, SkyThemes.Theme t, double intensity) {
        double R = radius * 0.985;
        int segments = f.quality.ordinal() >= 2 ? 128 : 84;
        int rings = 14;
        double bottom = Math.toRadians(-14), top = Math.PI / 2;
        double bright = domeBrightness.value();
        int weather = EffectBatch.mixRgb(0x141A22, 0x000000, 1 - Math.min(1, rain * 1.2));
        int zenith = EffectBatch.mixRgb(t.zenith(), weather, rain * 0.5);
        int horizon = EffectBatch.mixRgb(t.horizon(), weather, rain * 0.45);
        int accent = t.accent();
        int[] rowColor = new int[rings + 1];
        double[] rowY = new double[rings + 1], rowR = new double[rings + 1];
        for (int r = 0; r <= rings; r++) {
            double el = bottom + (top - bottom) * r / rings;
            double u = Math.max(0, Math.min(1, (el - bottom) / (top - bottom)));
            int c = EffectBatch.mixRgb(horizon, zenith, Math.pow(u, 0.62));
            double band = Math.exp(-Math.pow((el - 0.02) * 5.2, 2));
            c = EffectBatch.mixRgb(c, accent, band * 0.20);
            c = EffectBatch.mixRgb(0x000000, c, Math.min(1, 0.55 + bright * 0.5));
            if (el < 0) c = EffectBatch.mixRgb(c, 0x000000, Math.min(0.8, -el * 0.32));
            rowColor[r] = c;
            rowY[r] = Math.sin(el) * R;
            rowR[r] = Math.cos(el) * R;
        }
        double cosStep = Math.cos(Math.PI * 2 / segments), sinStep = Math.sin(Math.PI * 2 / segments);
        double c0 = 1, s0 = 0;
        for (int i = 0; i < segments; i++) {
            double c1 = c0 * cosStep - s0 * sinStep, s1 = s0 * cosStep + c0 * sinStep;
            for (int r = 0; r < rings; r++) {
                int ca = FxShapes.argb(rowColor[r], 1), cb = FxShapes.argb(rowColor[r + 1], 1);
                f.batch.quad(s0 * rowR[r], rowY[r], c0 * rowR[r],
                        s1 * rowR[r], rowY[r], c1 * rowR[r],
                        s1 * rowR[r + 1], rowY[r + 1], c1 * rowR[r + 1],
                        s0 * rowR[r + 1], rowY[r + 1], c0 * rowR[r + 1],
                        ca, ca, cb, cb, false);
            }
            c0 = c1; s0 = s1;
        }
    }

    /** Soft bloom around the real sun position, strongest while it sits low over the horizon. */
    private void sunGlow(FxFrame f, double radius, SkyThemes.Theme t, double intensity) {
        double[] d = {-Math.sin(sunAngle), Math.cos(sunAngle), 0};
        if (d[1] < -0.12) return;
        double fade = Math.max(0, Math.min(1, (d[1] + 0.12) / 0.35));
        double a = celestial.value() * intensity * fade;
        if (a <= 0.01) return;
        int warm = EffectBatch.mixRgb(t.accent(), sunsetColor == 0 ? t.accent() : sunsetColor, 0.5);
        Sky flat = new Sky(f, radius * 0.99, 1, 0);
        flat.disc(d, 0.40, FxShapes.argb(warm, 0.045 * a), FxShapes.argb(warm, 0), 28);
        flat.disc(d, 0.18, FxShapes.argb(warm, 0.09 * a), FxShapes.argb(warm, 0), 28);
        flat.disc(d, 0.055, FxShapes.argb(0xFFF6E8, 0.55 * a), FxShapes.argb(0xFFD9A8, 0), 24);
    }

    /**
     * Three layered cloud decks painted onto the dome shell just above the horizon. Each deck is a strip of
     * quads whose height, opacity and tone vary with azimuth, so the band breaks up like real distant cloud.
     */
    private void cloudBands(FxFrame f, double radius, SkyThemes.Theme t, double intensity, double time) {
        double R = radius * 0.972;
        int segments = f.quality.ordinal() >= 2 ? 120 : 80;
        double strength = clouds.value() * intensity;
        if (strength <= 0.01) return;
        int low = EffectBatch.mixRgb(t.horizon(), 0x0B0E14, 0.30);
        int lit = EffectBatch.mixRgb(t.horizon(), 0xDCE4F0, 0.42);
        // Four decks at increasing elevation. Lower decks are wide and dense, upper decks thin and lit,
        // so the horizon reads as depth instead of a single painted stripe.
        double[] elevations = {4.5, 9.5, 15.0, 21.5};
        for (int band = 0; band < elevations.length; band++) {
            double rr = R * (1 - band * 0.004);
            double scale = strength * (band == 3 ? 0.45 : band == 0 ? 0.95 : 0.75);
            double prevX = 0, prevZ = 0, prevY0 = 0, prevY1 = 0;
            boolean has = false;
            for (int i = 0; i <= segments; i++) {
                double az = i * Math.PI * 2 / segments;
                double n = 0.5 + 0.5 * Math.sin(az * (2.3 + band * 1.7) + time * (0.05 + band * 0.03) + band * 1.9)
                        * Math.sin(az * (5.9 - band) - time * 0.04 + band * 0.7 + 1.3);
                double e0 = Math.toRadians(elevations[band]) + n * Math.toRadians(1.4 + band * 0.9);
                double e1 = e0 + Math.toRadians(3.2 + band * 1.4) + n * Math.toRadians(band >= 2 ? 7.5 : 5.5);
                double x = Math.sin(az) * rr, z = Math.cos(az) * rr;
                double y0 = Math.sin(e0) * rr, y1 = Math.sin(e1) * rr;
                if (has) {
                    double edge = 0.55 + 0.45 * Math.sin(az * 2.0 + band * 1.3 + 0.6);
                    double alpha = Math.min(0.85, scale * (0.09 + 0.30 * n) * edge);
                    int cb = FxShapes.argb(low, alpha * 0.8);
                    int ct = FxShapes.argb(EffectBatch.mixRgb(lit, 0xFFFFFF, band == 3 ? 0.12 : 0), alpha);
                    f.batch.quad(prevX, prevY0, prevZ, x, y0, z, x, y1, z, prevX, prevY1, prevZ, cb, cb, ct, ct, false);
                }
                prevX = x; prevY0 = y0; prevY1 = y1; prevZ = z;
                has = true;
            }
        }
    }

    /** Long thin streaks of light sliding diagonally through the sky, as in the haze references. */
    private void lightStreaks(FxFrame f, double radius, SkyThemes.Theme t, double intensity, double time) {
        int count = 6 + (int) (8 * f.quality.density());
        double near = radius * 0.35;
        for (int i = 0; i < count; i++) {
            double seed = i * 7.31;
            double period = 3.5 + (i % 5) * 0.9;
            double phase = (time / period + i * 0.137) % 1.0;
            double az = (seed * 2.399) % (Math.PI * 2);
            double alt = 0.25 + ((seed * 0.61) % 0.5);
            double length = near * (0.18 + (i % 3) * 0.08);
            double hx = -Math.sin(az) * near, hz = Math.cos(az) * near, hy = near * (alt + 0.35 - phase * 0.7);
            double dx = Math.cos(az) * 0.35, dy = -1, dz = Math.sin(az) * 0.35;
            double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double fade = Math.sin(phase * Math.PI);
            int head = FxShapes.argb(i % 2 == 0 ? t.accent() : 0xFFFFFF, Math.min(1, 0.65 * celestial.value() * intensity * fade));
            f.batch.line(hx - dx / dl * length, hy - dy / dl * length, hz - dz / dl * length, hx, hy, hz,
                    head, 1.6F, false);
            // Faint tail segment so the streak tapers instead of ending abruptly.
            f.batch.line(hx - dx / dl * length * 1.8, hy - dy / dl * length * 1.8, hz - dz / dl * length * 1.8,
                    hx - dx / dl * length, hy - dy / dl * length, hz - dz / dl * length, (head & 0x00FFFFFF) | ((head >>> 24) / 3 << 24), 1.2F, false);
        }
    }

    /** Sky-space → camera-relative world transform matching vanilla's celestial rotation. */
    private record Sky(FxFrame f, double radius, double cos, double sin) {
        double[] dir(double x, double y, double z) {
            double ry = y * cos - z * sin, rz = y * sin + z * cos;
            return new double[]{-rz, ry, x};
        }

        /** Disc of angular radius {@code r} centered on {@code d} (a camera-relative unit vector). */
        void disc(double[] d, double r, int center, int rim, int sides) {
            double[] u = perp(d), v = cross(d, u);
            double cx = d[0] * radius, cy = d[1] * radius, cz = d[2] * radius, s = r * radius;
            for (int i = 0; i < sides; i++) {
                double a = i * Math.PI * 2 / sides, b = (i + 1) * Math.PI * 2 / sides;
                f.batch.quad(cx, cy, cz,
                        cx + (u[0] * Math.cos(a) + v[0] * Math.sin(a)) * s, cy + (u[1] * Math.cos(a) + v[1] * Math.sin(a)) * s,
                        cz + (u[2] * Math.cos(a) + v[2] * Math.sin(a)) * s,
                        cx + (u[0] * Math.cos(b) + v[0] * Math.sin(b)) * s, cy + (u[1] * Math.cos(b) + v[1] * Math.sin(b)) * s,
                        cz + (u[2] * Math.cos(b) + v[2] * Math.sin(b)) * s,
                        cx, cy, cz, center, rim, rim, center, false);
            }
        }
        /** Small crisp billboard dot of world half-size {@code w}, for stars packed inside a band. */
        void dot(double[] d, double w, int color) {
            double[] u = perp(d), v = cross(d, u);
            double cx = d[0] * radius, cy = d[1] * radius, cz = d[2] * radius;
            f.batch.quad(cx + u[0] * w, cy + u[1] * w, cz + u[2] * w, cx + v[0] * w, cy + v[1] * w, cz + v[2] * w,
                    cx - u[0] * w, cy - u[1] * w, cz - u[2] * w, cx - v[0] * w, cy - v[1] * w, cz - v[2] * w,
                    color, color, color, color, false);
        }
    }

    private static double[] perp(double[] d) {
        double[] u = Math.abs(d[1]) < 0.95 ? cross(d, new double[]{0, 1, 0}) : cross(d, new double[]{1, 0, 0});
        double l = Math.sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2]);
        return new double[]{u[0] / l, u[1] / l, u[2] / l};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] norm(double x, double y, double z) {
        double l = Math.sqrt(x * x + y * y + z * z);
        return new double[]{x / l, y / l, z / l};
    }

    private static double horizonFade(double y) { return Math.max(0, Math.min(1, (y + 0.04) / 0.22)); }

    private void stars(Sky sky, SkyThemes.Theme t, double intensity, double time) {
        FxFrame f = sky.f();
        int n = (int) Math.min(MAX_STARS, 1700 * starDensity.value() * t.stars() * (0.5 + f.quality.density() * 0.5));
        double brightness = starBrightness.value() * intensity;
        double base = sky.radius() * 0.0032;
        for (int i = 0; i < n; i++) {
            double[] d = sky.dir(sx[i], sy[i], sz[i]);
            double fade = horizonFade(d[1]);
            if (fade <= 0) continue;
            double tw = 1 - twinkle.value() * 0.55 * (0.5 + 0.5 * Math.sin(time * (1.5 + tintMix[i] * 2) + phase[i]));
            double a = Math.min(1, (0.45 + size[i] * 0.26) * brightness * tw * fade);
            int rgb = EffectBatch.mixRgb(t.starTint(), tintMix[i] > 0.85 ? t.accent() : t.starTint(), 0.5);
            double s = base * Math.min(size[i], 2.6);
            double cx = d[0] * sky.radius(), cy = d[1] * sky.radius(), cz = d[2] * sky.radius();
            double[] u = perp(d), v = cross(d, u);
            if (size[i] > 2.05) {
                // Only the handful of genuinely bright stars bloom; everything else stays a crisp point.
                FxShapes.softDot(f, cx, cy, cz, s * 0.95, rgb, a * 0.42, false);
                FxShapes.softDot(f, cx, cy, cz, s * 0.30, 0xFFFFFF, a, false);
            } else {
                double core = s * (size[i] > 1.5 ? 0.34 : 0.26);
                int c = FxShapes.argb(rgb, a);
                f.batch.quad(cx + u[0] * core, cy + u[1] * core, cz + u[2] * core, cx + v[0] * core, cy + v[1] * core, cz + v[2] * core,
                        cx - u[0] * core, cy - u[1] * core, cz - u[2] * core, cx - v[0] * core, cy - v[1] * core, cz - v[2] * core, c, c, c, c, false);
            }
            if (size[i] > 2.7) {
                // Faint four-point glint on the brightest stars only.
                double arm = s * 1.7;
                int glint = FxShapes.argb(rgb, a * 0.16);
                f.batch.line(cx - u[0] * arm, cy - u[1] * arm, cz - u[2] * arm, cx + u[0] * arm, cy + u[1] * arm, cz + u[2] * arm, glint, 1F, false);
                f.batch.line(cx - v[0] * arm, cy - v[1] * arm, cz - v[2] * arm, cx + v[0] * arm, cy + v[1] * arm, cz + v[2] * arm, glint, 1F, false);
            }
        }
    }

    private void milkyWay(Sky sky, SkyThemes.Theme t, double intensity, double time) {
        // Great circle tilted against the celestial equator, built from layered soft clouds plus a dust lane.
        double[] n = norm(0.35, 0.42, 0.84);
        double[] u = perp(n), v = cross(n, u);
        int clouds = (int) (110 * sky.f().quality.density()) + 40;
        Random r = new Random(77);
        double a0 = celestial.value() * intensity;
        for (int i = 0; i < clouds; i++) {
            double a = i * Math.PI * 2 / clouds + r.nextGaussian() * 0.02;
            double off = r.nextGaussian() * 0.075;
            double l = Math.sqrt(1 + off * off);
            double[] d = sky.dir((u[0] * Math.cos(a) + v[0] * Math.sin(a) + n[0] * off) / l,
                    (u[1] * Math.cos(a) + v[1] * Math.sin(a) + n[1] * off) / l,
                    (u[2] * Math.cos(a) + v[2] * Math.sin(a) + n[2] * off) / l);
            double fade = horizonFade(d[1]);
            if (fade <= 0) continue;
            double core = 0.5 + 0.5 * Math.sin(a * 2 + 1.2);
            int rgb = EffectBatch.mixRgb(t.accent(), t.accent2(), 0.5 + 0.5 * Math.sin(a * 3));
            double alpha = (0.15 + core * 0.19) * a0 * fade;
            sky.disc(d, 0.10 + r.nextDouble() * 0.11, FxShapes.argb(rgb, alpha), FxShapes.argb(rgb, 0), 14);
            if (r.nextDouble() < 0.6)
                sky.disc(d, 0.022 + r.nextDouble() * 0.02, FxShapes.argb(0xFFFFFF, Math.min(1, alpha * 1.5)), FxShapes.argb(0xFFFFFF, 0), 8);
        }
        // Dark dust lane running through the band for depth.
        for (int i = 0; i < 34; i++) {
            double a = i * Math.PI * 2 / 34 + 0.1;
            double off = Math.sin(a * 3.1) * 0.02;
            double l = Math.sqrt(1 + off * off);
            double[] d = sky.dir((u[0] * Math.cos(a) + v[0] * Math.sin(a) + n[0] * off) / l,
                    (u[1] * Math.cos(a) + v[1] * Math.sin(a) + n[1] * off) / l,
                    (u[2] * Math.cos(a) + v[2] * Math.sin(a) + n[2] * off) / l);
            double fade = horizonFade(d[1]);
            if (fade <= 0) continue;
            sky.disc(d, 0.05, FxShapes.argb(0x05060C, 0.10 * a0 * fade), FxShapes.argb(0x05060C, 0), 10);
        }
        // Crisp stars packed into the band: the glow alone reads as fog, the points give it grain.
        int bandStars = (int) ((sky.f().quality.ordinal() >= 2 ? 460 : 240) * a0);
        double dotBase = sky.radius() * 0.0018;
        for (int i = 0; i < bandStars; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double off = r.nextGaussian() * 0.12;
            double l = Math.sqrt(1 + off * off);
            double[] d = sky.dir((u[0] * Math.cos(a) + v[0] * Math.sin(a) + n[0] * off) / l,
                    (u[1] * Math.cos(a) + v[1] * Math.sin(a) + n[1] * off) / l,
                    (u[2] * Math.cos(a) + v[2] * Math.sin(a) + n[2] * off) / l);
            double fade = horizonFade(d[1]);
            if (fade <= 0) continue;
            double w = dotBase * (0.55 + r.nextDouble() * 1.5);
            int c = EffectBatch.mixRgb(0xFFFFFF, t.accent2(), r.nextDouble() * 0.5);
            sky.dot(d, w, FxShapes.argb(c, Math.min(1, (0.35 + r.nextDouble() * 0.55) * a0 * fade)));
        }
    }

    private void nebula(Sky sky, SkyThemes.Theme t, double intensity) {
        Random r = new Random(4242);
        double a0 = celestial.value() * intensity;
        // A tilted ribbon of many small overlapping puffs reads as a painterly cloud bank; a handful of
        // big soft discs behind it provide the haze that the small puffs alone cannot produce.
        double[] n = norm(-0.34, 0.36, 0.87);
        double[] u = perp(n), v = cross(n, u);
        int haze = (int) (18 + 14 * sky.f().quality.density());
        for (int i = 0; i < haze; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double off = r.nextGaussian() * 0.10;
            double[] d = norm(u[0] * Math.cos(a) + v[0] * Math.sin(a) + n[0] * off,
                    u[1] * Math.cos(a) + v[1] * Math.sin(a) + n[1] * off,
                    u[2] * Math.cos(a) + v[2] * Math.sin(a) + n[2] * off);
            double fade = horizonFade(d[1]);
            if (fade <= 0) continue;
            int rgb = EffectBatch.mixRgb(t.accent(), t.accent2(), r.nextDouble());
            sky.disc(d, 0.16 + r.nextDouble() * 0.20, FxShapes.argb(rgb, (0.050 + r.nextDouble() * 0.045) * a0 * fade),
                    FxShapes.argb(rgb, 0), 22);
        }
        int puffs = (int) (340 * sky.f().quality.density()) + 150;
        for (int i = 0; i < puffs; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double off = r.nextGaussian() * 0.085;
            double l = Math.sqrt(1 + off * off);
            double[] d = sky.dir((u[0] * Math.cos(a) + v[0] * Math.sin(a) + n[0] * off) / l,
                    (u[1] * Math.cos(a) + v[1] * Math.sin(a) + n[1] * off) / l,
                    (u[2] * Math.cos(a) + v[2] * Math.sin(a) + n[2] * off) / l);
            double fade = horizonFade(d[1]);
            if (fade <= 0) continue;
            double band = 0.55 + 0.45 * Math.sin(a * 2.3 + 0.8);
            int rgb = EffectBatch.mixRgb(t.accent(), t.accent2(), 0.25 + 0.5 * Math.sin(a * 1.7 + 2.0));
            double alpha = (0.055 + r.nextDouble() * 0.095) * (0.5 + band * 0.6) * a0 * fade;
            sky.disc(d, 0.020 + r.nextDouble() * 0.042, FxShapes.argb(rgb, alpha), FxShapes.argb(rgb, 0), 12);
            if (r.nextDouble() < 0.32) {
                int core = EffectBatch.mixRgb(rgb, 0xFFFFFF, 0.62);
                sky.disc(d, 0.010 + r.nextDouble() * 0.016, FxShapes.argb(core, alpha * 1.7), FxShapes.argb(core, 0), 8);
            }
        }
        // Dark dust threads through the bright bank.
        for (int i = 0; i < 42; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double off = Math.sin(a * 2.7) * 0.03;
            double l = Math.sqrt(1 + off * off);
            double[] d = sky.dir((u[0] * Math.cos(a) + v[0] * Math.sin(a) + n[0] * off) / l,
                    (u[1] * Math.cos(a) + v[1] * Math.sin(a) + n[1] * off) / l,
                    (u[2] * Math.cos(a) + v[2] * Math.sin(a) + n[2] * off) / l);
            double fade = horizonFade(d[1]);
            if (fade <= 0) continue;
            sky.disc(d, 0.03 + r.nextDouble() * 0.05, FxShapes.argb(0x05030A, 0.05 * a0 * fade), FxShapes.argb(0x05030A, 0), 10);
        }
        // Crisp cluster stars embedded in the brightest stretch of the bank.
        int starCount = (int) (110 * a0);
        double dotBase = sky.radius() * 0.0016;
        for (int i = 0; i < starCount; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double off = r.nextGaussian() * 0.10;
            double l = Math.sqrt(1 + off * off);
            double[] d = sky.dir((u[0] * Math.cos(a) + v[0] * Math.sin(a) + n[0] * off) / l,
                    (u[1] * Math.cos(a) + v[1] * Math.sin(a) + n[1] * off) / l,
                    (u[2] * Math.cos(a) + v[2] * Math.sin(a) + n[2] * off) / l);
            double fade = horizonFade(d[1]);
            if (fade <= 0) continue;
            int c = EffectBatch.mixRgb(0xFFFFFF, t.accent2(), r.nextDouble() * 0.4);
            sky.dot(d, dotBase * (0.6 + r.nextDouble() * 1.6),
                    FxShapes.argb(c, Math.min(1, (0.35 + r.nextDouble() * 0.6) * a0 * fade)));
        }
    }

    private void moon(Sky sky, SkyThemes.Theme t, double intensity) {
        double[] d = norm(0.30, 0.55, 0.78);
        double fade = horizonFade(d[1]);
        if (fade <= 0) return;
        double a = celestial.value() * intensity * fade;
        if (a <= 0.01) return;
        Sky flat = new Sky(sky.f(), sky.radius(), 1, 0);
        flat.disc(d, 0.34, FxShapes.argb(t.accent2(), 0.045 * a), FxShapes.argb(t.accent2(), 0), 28);
        flat.disc(d, 0.16, FxShapes.argb(t.accent2(), 0.075 * a), FxShapes.argb(t.accent2(), 0), 28);
        double r = 0.055;
        flat.disc(d, r * 1.09, FxShapes.argb(0xC9D1DE, Math.min(1, 0.55 * a)), FxShapes.argb(0xC9D1DE, 0), 32);
        flat.disc(d, r, FxShapes.argb(0xF6F8FC, Math.min(1, a)), FxShapes.argb(0xD6DCE8, Math.min(1, a)), 32);
        // Mare-style patches, offset inside the disc for surface detail.
        double[] u = perp(d), v = cross(d, u);
        double[][] maria = {{-0.3, 0.2, 0.32}, {0.22, -0.18, 0.26}, {0.05, 0.36, 0.18}, {0.34, 0.22, 0.14}, {-0.12, -0.34, 0.2}};
        for (double[] m : maria) {
            double[] cd = {d[0] + u[0] * m[0] * r + v[0] * m[1] * r, d[1] + u[1] * m[0] * r + v[1] * m[1] * r,
                    d[2] + u[2] * m[0] * r + v[2] * m[1] * r};
            double cl = Math.sqrt(cd[0] * cd[0] + cd[1] * cd[1] + cd[2] * cd[2]);
            cd = new double[]{cd[0] / cl, cd[1] / cl, cd[2] / cl};
            int c = FxShapes.argb(0xB6BECB, Math.min(1, 0.5 * a));
            flat.disc(cd, r * m[2] * 0.5, c, c, 12);
        }
    }

    /**
     * Ringed planet or black hole. The planet body is shaded as a lit sphere with a terminator crescent,
     * separated rings with a Cassini gap are drawn back-half first and front-half last so they pass behind
     * and in front of the body correctly, and a small companion moon sits beside the planet.
     */
    private void planet(Sky sky, SkyThemes.Theme t, double intensity, boolean blackHole) {
        double[] d = blackHole ? norm(-0.62, 0.44, 0.65) : norm(0.58, 0.42, 0.70);
        double fade = horizonFade(d[1]);
        if (fade <= 0) return;
        double a = celestial.value() * intensity * fade;
        if (a <= 0.01) return;
        double body = blackHole ? 0.078 : 0.088;
        double[] u = perp(d), v = cross(d, u);
        // Ring inclination: b must carry a component along d, otherwise the ring plane faces the camera and
        // projects as a flat bullseye instead of an ellipse.
        double tilt = blackHole ? 1.02 : 1.16;
        double[] b = {v[0] * Math.cos(tilt) + d[0] * Math.sin(tilt), v[1] * Math.cos(tilt) + d[1] * Math.sin(tilt),
                v[2] * Math.cos(tilt) + d[2] * Math.sin(tilt)};
        if (blackHole) {
            int warm = EffectBatch.mixRgb(t.accent(), 0xFFE6B8, 0.55);
            int hot = EffectBatch.mixRgb(warm, 0xFFFFFF, 0.55);
            ringBand(sky, d, u, b, body * 1.55, body * 1.95, warm, 0.18 * a, false);
            ringBand(sky, d, u, b, body * 2.02, body * 2.38, warm, 0.11 * a, false);
            sky.disc(d, body * 3.4, FxShapes.argb(warm, 0.075 * a), FxShapes.argb(warm, 0), 40);
            sky.disc(d, body * 1.30, FxShapes.argb(warm, 0.42 * a), FxShapes.argb(hot, 0.20 * a), 44);
            // Thin photon ring hugging the hole, then the event horizon itself.
            ringBillboard(sky, d, u, v, body * 1.10, body * 0.018, FxShapes.argb(hot, Math.min(1, a)));
            sky.disc(d, body, FxShapes.argb(0x000000, Math.min(1, a)), FxShapes.argb(0x000000, Math.min(1, a)), 48);
            ringBillboard(sky, d, u, v, body * 1.02, body * 0.012, FxShapes.argb(hot, Math.min(1, 0.9 * a)));
            ringBand(sky, d, u, b, body * 1.55, body * 1.95, warm, 0.20 * a, true);
            ringBand(sky, d, u, b, body * 2.02, body * 2.38, warm, 0.12 * a, true);
            ringBillboard(sky, d, u, v, body * 1.56, body * 0.008, FxShapes.argb(hot, 0.45 * a));
            return;
        }
        // Four ring groups: faint C ring, bright B ring, Cassini gap, A ring and a hairline F ring.
        int ringWarm = EffectBatch.mixRgb(t.accent(), 0xFFEFD2, 0.55);
        int ringHot = EffectBatch.mixRgb(ringWarm, 0xFFFFFF, 0.40);
        ringBand(sky, d, u, b, body * 1.40, body * 1.53, t.accent2(), 0.12 * a, false);
        ringBand(sky, d, u, b, body * 1.55, body * 1.62, ringHot, 0.60 * a, false);
        ringBand(sky, d, u, b, body * 1.62, body * 1.94, ringWarm, 0.44 * a, false);
        ringBand(sky, d, u, b, body * 2.02, body * 2.36, ringWarm, 0.28 * a, false);
        ringBand(sky, d, u, b, body * 2.40, body * 2.44, t.accent2(), 0.12 * a, false);
        // Atmosphere haze behind the body.
        sky.disc(d, body * 1.36, FxShapes.argb(t.accent2(), 0.08 * a), FxShapes.argb(t.accent2(), 0), 32);
        // Single radial gradient disc: warm centre falling off to a dark limb, then an offset terminator.
        int lit = EffectBatch.mixRgb(t.accent(), 0xFFF2D8, 0.72);
        int limb = EffectBatch.mixRgb(0x0A0806, t.accent(), 0.22);
        sky.disc(d, body * 0.995, FxShapes.argb(EffectBatch.mixRgb(lit, 0xFFFFFF, 0.16), Math.min(1, a)),
                FxShapes.argb(limb, Math.min(1, a)), 56);
        // Gas bands: chords across the disc in warm cream and dusty brown.
        double[] offsets = {-0.66, -0.44, -0.24, -0.04, 0.16, 0.34, 0.54, 0.72};
        for (int i = 0; i < offsets.length; i++) {
            int c = i % 2 == 0
                    ? FxShapes.argb(EffectBatch.mixRgb(lit, 0xFFFFFF, 0.34), 0.12 * a)
                    : FxShapes.argb(EffectBatch.mixRgb(0x2A1E12, lit, 0.20), 0.13 * a);
            bodyBand(sky, d, u, v, body, offsets[i], 0.13, c);
        }
        bodyBand(sky, d, u, v, body, 0.05, 0.30, FxShapes.argb(EffectBatch.mixRgb(0x241A10, lit, 0.18), 0.12 * a));
        double[] shadowDir = norm(d[0] - u[0] * 0.42 * body - v[0] * 0.10 * body,
                d[1] - u[1] * 0.42 * body - v[1] * 0.10 * body, d[2] - u[2] * 0.42 * body - v[2] * 0.10 * body);
        sky.disc(shadowDir, body * 0.98, FxShapes.argb(0x03040A, Math.min(0.82, 0.82 * a)),
                FxShapes.argb(0x03040A, Math.min(0.66, 0.66 * a)), 48);
        // Thin atmospheric rim and a small moon.
        ringBillboard(sky, d, u, v, body * 1.004, body * 0.012, FxShapes.argb(t.accent2(), 0.45 * a));
        double[] md = norm(d[0] + u[0] * 0.34 + v[0] * 0.18, d[1] + u[1] * 0.34 + v[1] * 0.18, d[2] + u[2] * 0.34 + v[2] * 0.18);
        sky.disc(md, body * 0.085, FxShapes.argb(0xD8DDE8, Math.min(1, a)), FxShapes.argb(0x6A7180, Math.min(1, a)), 14);
        ringBand(sky, d, u, b, body * 1.40, body * 1.53, t.accent2(), 0.12 * a, true);
        ringBand(sky, d, u, b, body * 1.55, body * 1.62, ringHot, 0.60 * a, true);
        ringBand(sky, d, u, b, body * 1.62, body * 1.94, ringWarm, 0.44 * a, true);
        ringBand(sky, d, u, b, body * 2.02, body * 2.36, ringWarm, 0.28 * a, true);
        ringBand(sky, d, u, b, body * 2.40, body * 2.44, t.accent2(), 0.12 * a, true);
    }

    /** Flat chord across the planet body, used for gas bands; {@code offset} is in body radii along v. */
    private void bodyBand(Sky sky, double[] d, double[] u, double[] v, double body, double offset, double thick, int color) {
        double R = sky.radius();
        double cx = d[0] * R, cy = d[1] * R, cz = d[2] * R;
        double half = Math.sqrt(Math.max(0, body * body - (offset * body) * (offset * body))) * 0.96 * R;
        double lo = (offset - thick * 0.5) * body * R, hi = (offset + thick * 0.5) * body * R;
        sky.f().batch.quad(cx - u[0] * half + v[0] * lo, cy - u[1] * half + v[1] * lo, cz - u[2] * half + v[2] * lo,
                cx + u[0] * half + v[0] * lo, cy + u[1] * half + v[1] * lo, cz + u[2] * half + v[2] * lo,
                cx + u[0] * half + v[0] * hi, cy + u[1] * half + v[1] * hi, cz + u[2] * half + v[2] * hi,
                cx - u[0] * half + v[0] * hi, cy - u[1] * half + v[1] * hi, cz - u[2] * half + v[2] * hi,
                color, color, color, color, false);
    }

    /** Flat ring band with soft radial edges, split into the half behind or in front of the body. */
    private void ringBand(Sky sky, double[] d, double[] u, double[] b, double r0, double r1, int rgb, double alpha, boolean front) {
        FxFrame f = sky.f();
        int segments = f.quality.ordinal() >= 2 ? 96 : 64;
        double R = sky.radius();
        double cx = d[0] * R, cy = d[1] * R, cz = d[2] * R;
        double span = r1 - r0;
        double[][] spans = {{r0, r0 + span * 0.16, 0.35}, {r0 + span * 0.16, r1 - span * 0.20, 1.0}, {r1 - span * 0.20, r1, 0.28}};
        for (int i = 0; i < segments; i++) {
            double p = i * Math.PI * 2 / segments, q = (i + 1) * Math.PI * 2 / segments;
            double mid = (p + q) / 2;
            double depth = Math.sin(mid) * (b[0] * d[0] + b[1] * d[1] + b[2] * d[2]);
            if ((depth < 0) != front) continue;
            double[] pp = {u[0] * Math.cos(p) + b[0] * Math.sin(p), u[1] * Math.cos(p) + b[1] * Math.sin(p), u[2] * Math.cos(p) + b[2] * Math.sin(p)};
            double[] qq = {u[0] * Math.cos(q) + b[0] * Math.sin(q), u[1] * Math.cos(q) + b[1] * Math.sin(q), u[2] * Math.cos(q) + b[2] * Math.sin(q)};
            double shade = 0.88 + 0.12 * Math.cos(mid * 7);
            for (double[] s : spans) {
                int c = FxShapes.argb(rgb, Math.min(1, alpha * s[2] * shade));
                double ra = s[0] * R, rb = s[1] * R;
                f.batch.quad(cx + pp[0] * ra, cy + pp[1] * ra, cz + pp[2] * ra, cx + qq[0] * ra, cy + qq[1] * ra, cz + qq[2] * ra,
                        cx + qq[0] * rb, cy + qq[1] * rb, cz + qq[2] * rb, cx + pp[0] * rb, cy + pp[1] * rb, cz + pp[2] * rb,
                        c, c, c, c, false);
            }
        }
    }

    private void ringBillboard(Sky sky, double[] d, double[] u, double[] v, double r, double w, int color) {
        FxFrame f = sky.f();
        double R = sky.radius();
        double cx = d[0] * R, cy = d[1] * R, cz = d[2] * R;
        int segments = 48;
        int clear = color & 0x00FFFFFF;
        for (int i = 0; i < segments; i++) {
            double p = i * Math.PI * 2 / segments, q = (i + 1) * Math.PI * 2 / segments;
            double[] pp = {u[0] * Math.cos(p) + v[0] * Math.sin(p), u[1] * Math.cos(p) + v[1] * Math.sin(p), u[2] * Math.cos(p) + v[2] * Math.sin(p)};
            double[] qq = {u[0] * Math.cos(q) + v[0] * Math.sin(q), u[1] * Math.cos(q) + v[1] * Math.sin(q), u[2] * Math.cos(q) + v[2] * Math.sin(q)};
            double ri = (r - w) * R, rm = r * R, ro = (r + w * 2) * R;
            f.batch.quad(cx + pp[0] * ri, cy + pp[1] * ri, cz + pp[2] * ri, cx + qq[0] * ri, cy + qq[1] * ri, cz + qq[2] * ri,
                    cx + qq[0] * rm, cy + qq[1] * rm, cz + qq[2] * rm, cx + pp[0] * rm, cy + pp[1] * rm, cz + pp[2] * rm, clear, clear, color, color, false);
            f.batch.quad(cx + pp[0] * rm, cy + pp[1] * rm, cz + pp[2] * rm, cx + qq[0] * rm, cy + qq[1] * rm, cz + qq[2] * rm,
                    cx + qq[0] * ro, cy + qq[1] * ro, cz + qq[2] * ro, cx + pp[0] * ro, cy + pp[1] * ro, cz + pp[2] * ro, color, color, clear, clear, false);
        }
    }

    /**
     * Full 360° aurora built from organic curtains rather than a picket fence: smooth fold noise drives the
     * height and brightness, the bottom hem glows, the crown fades into the second accent colour and a few
     * sparse pleats give the sheet depth. All phases are periodic in azimuth so the seam is invisible.
     */
    private void aurora(FxFrame f, double radius, SkyThemes.Theme t, double intensity, double time) {
        double a = celestial.value() * intensity;
        if (a <= 0.01) return;
        int columns = f.quality.ordinal() >= 2 ? 160 : 104;
        int bright = EffectBatch.mixRgb(t.accent(), 0xFFFFFF, 0.68);
        int teal = EffectBatch.mixRgb(t.accent(), t.accent2(), 0.50);
        int violet = EffectBatch.mixRgb(t.accent2(), 0x3A1A5C, 0.30);
        // Localised sheets instead of a full ring: each sheet rises well above the horizon, is brightest along
        // a glowing hem and dissolves into the second accent colour at the crown.
        double[] centers = {-0.70, 1.30, 2.75};
        double[] fr = {0.0, 0.14, 0.42, 0.74, 1.0};
        double[] aa = {0.42, 0.60, 0.36, 0.12, 0.0};
        int[] cc = {bright, bright, teal, violet, violet};
        for (int layer = 0; layer < 3; layer++) {
            double R = radius * (0.90 - layer * 0.035);
            double center = centers[layer] + time * (0.006 + layer * 0.003);
            double halfSpread = 1.02 - layer * 0.16;
            double baseEl = 0.10 + layer * 0.04;
            double peak = (0.42 + layer * 0.12) * (0.78 + 0.22 * Math.sin(time * 0.13 + layer * 1.7));
            double[][] prev = null;
            int[] prevC = new int[5];
            for (int i = 0; i <= columns; i++) {
                double u = i / (double) columns;
                double az = center - halfSpread + 2 * halfSpread * u;
                double env = Math.pow(Math.max(0, Math.cos((u - 0.5) * Math.PI)), 1.6);
                double fold = 0.5 + 0.5 * Math.sin(az * 4.1 + time * 0.32 + layer * 2.0)
                        * Math.sin(az * 1.9 - time * 0.21 + layer * 0.7);
                double wave = 0.010 * Math.sin(az * 7.0 + time * 0.55 + layer * 1.3) + 0.005 * Math.sin(az * 15.0 - time * 0.9);
                double bottom = baseEl + wave + 0.008 * Math.sin(az * 2.2 + layer);
                double top = bottom + peak * (0.35 + 0.65 * fold) * (0.35 + 0.65 * env);
                double ray = 0.5 + 0.5 * Math.sin(az * 41.0 + time * 1.1 + layer * 2.3) * Math.sin(az * 17.0 - time * 0.7);
                double strength = a * env * (0.42 + 0.58 * ray);
                double[][] pts = new double[5][];
                int[] cols = new int[5];
                for (int r = 0; r < 5; r++) {
                    double el = bottom + (top - bottom) * fr[r];
                    double ce = Math.cos(el), se = Math.sin(el);
                    pts[r] = new double[]{Math.sin(az) * ce * R, se * R, Math.cos(az) * ce * R};
                    cols[r] = FxShapes.argb(cc[r], Math.min(1, strength * aa[r]));
                }
                if (prev != null) {
                    for (int r = 0; r < 4; r++) {
                        f.batch.quad(prev[r][0], prev[r][1], prev[r][2], pts[r][0], pts[r][1], pts[r][2],
                                pts[r + 1][0], pts[r + 1][1], pts[r + 1][2], prev[r + 1][0], prev[r + 1][1], prev[r + 1][2],
                                prevC[r], cols[r], cols[r + 1], prevC[r + 1], false);
                    }
                }
                prev = pts;
                prevC = cols;
            }
        }
    }

    /** Warm band hugging the horizon on the side of the sky the sun is on, fading as it climbs. */
    private void sunsetGlow(FxFrame f, double radius, SkyThemes.Theme t, double intensity) {
        double center = -Math.sin(sunAngle) >= 0 ? Math.PI / 2 : -Math.PI / 2;
        double warmth = 0.4 + 0.6 * (1 - Math.abs(Math.cos(sunAngle)));
        double a = celestial.value() * intensity * warmth;
        if (a <= 0.01) return;
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            double p = center + Math.toRadians(-105 + 210.0 * i / segments);
            double q = center + Math.toRadians(-105 + 210.0 * (i + 1) / segments);
            double wp = Math.pow(Math.cos((i / (double) segments - 0.5) * Math.PI), 2);
            double wq = Math.pow(Math.cos(((i + 1) / (double) segments - 0.5) * Math.PI), 2);
            double xp = Math.sin(p) * radius, zp = Math.cos(p) * radius;
            double xq = Math.sin(q) * radius, zq = Math.cos(q) * radius;
            double low = -radius * 0.02, mid = radius * 0.09, high = radius * 0.32;
            f.batch.quad(xp, low, zp, xq, low, zq, xq, mid, zq, xp, mid, zp,
                    FxShapes.argb(t.accent(), a * 0.55 * wp), FxShapes.argb(t.accent(), a * 0.55 * wq),
                    FxShapes.argb(t.accent2(), a * 0.28 * wq), FxShapes.argb(t.accent2(), a * 0.28 * wp), false);
            f.batch.quad(xp, mid, zp, xq, mid, zq, xq, high, zq, xp, high, zp,
                    FxShapes.argb(t.accent2(), a * 0.28 * wp), FxShapes.argb(t.accent2(), a * 0.28 * wq),
                    FxShapes.argb(t.accent2(), 0), FxShapes.argb(t.accent2(), 0), false);
        }
    }

    private void shootingStar(Sky sky, double intensity, double now) {
        if (shootingStart < 0 || now - shootingStart > 1.4) {
            if (random.nextDouble() < sky.f().dt * 0.08 * animation.value()) { shootingStart = now; shootingSeed = random.nextDouble(); }
            else return;
        }
        double t = (now - shootingStart) / 1.1;
        if (t > 1) return;
        double az = shootingSeed * Math.PI * 2;
        double y0 = 0.75 - t * 0.35, spread = t * 0.5;
        double x = Math.cos(az + spread) * Math.sqrt(1 - y0 * y0), z = Math.sin(az + spread) * Math.sqrt(1 - y0 * y0);
        double R = sky.radius();
        double tx = Math.cos(az + spread - 0.08) * Math.sqrt(1 - (y0 + 0.04) * (y0 + 0.04));
        double tz = Math.sin(az + spread - 0.08) * Math.sqrt(1 - (y0 + 0.04) * (y0 + 0.04));
        double alpha = Math.sin(t * Math.PI) * intensity;
        sky.f().batch.line(tx * R, (y0 + 0.04) * R, tz * R, x * R, y0 * R, z * R, FxShapes.argb(0xFFFFFF, alpha), 2F, false);
    }
}
