package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxParticles;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.render.fx.TerrainWave;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * World-space impact effects. "Confirmed" mode waits for the server's hurt animation on an entity the
 * local player just attacked, so effects mark hits that landed rather than every swing. Each style is a
 * distinct piece of geometry; several can be layered, and presets pick a curated combination.
 */
public final class HitEffectsModule extends ClientModule implements FxEmitter {
    public enum Style { SPARKS, BURST, RIPPLE, SHOCKWAVE, STARS, SMOKE, INK, RINGS, ENERGY, MARKERS, DOME, WORLD_WAVE }

    private static final Map<String, Set<Style>> PRESETS = Map.of(
            "Minimal", EnumSet.of(Style.SPARKS, Style.RIPPLE),
            "Cinematic", EnumSet.of(Style.BURST, Style.SHOCKWAVE, Style.RINGS, Style.SMOKE, Style.SPARKS, Style.MARKERS),
            "Monochrome", EnumSet.of(Style.SPARKS, Style.SHOCKWAVE, Style.INK),
            "Cosmic", EnumSet.of(Style.STARS, Style.ENERGY, Style.RINGS, Style.BURST),
            "Splash", EnumSet.of(Style.INK, Style.SHOCKWAVE, Style.BURST, Style.MARKERS),
            "Impact", EnumSet.of(Style.DOME, Style.SHOCKWAVE, Style.WORLD_WAVE, Style.SPARKS, Style.BURST));
    private static final int OWNER = FxParticles.ownerId("hit_effects");

    private final ChoiceSetting preset = addSetting(new ChoiceSetting("preset", "Preset",
            List.of("Minimal", "Cinematic", "Monochrome", "Cosmic", "Splash", "Impact", "Custom"), "Minimal"));
    private final ChoiceSetting trigger = addSetting(new ChoiceSetting("trigger", "Trigger",
            List.of("Confirmed hits", "Attack attempts", "Any nearby damage"), "Confirmed hits"));
    private final BooleanSetting sparks = addSetting(new BooleanSetting("style_sparks", "Custom: white sparks", true));
    private final BooleanSetting burst = addSetting(new BooleanSetting("style_burst", "Custom: glow burst", false));
    private final BooleanSetting ripple = addSetting(new BooleanSetting("style_ripple", "Custom: impact ripple", true));
    private final BooleanSetting shockwave = addSetting(new BooleanSetting("style_shockwave", "Custom: ground shockwave", false));
    private final BooleanSetting stars = addSetting(new BooleanSetting("style_stars", "Custom: stars", false));
    private final BooleanSetting smoke = addSetting(new BooleanSetting("style_smoke", "Custom: soft smoke", false));
    private final BooleanSetting ink = addSetting(new BooleanSetting("style_ink", "Custom: ink splash", false));
    private final BooleanSetting rings = addSetting(new BooleanSetting("style_rings", "Custom: 3D rings", false));
    private final BooleanSetting energy = addSetting(new BooleanSetting("style_energy", "Custom: energy spiral", false));
    private final BooleanSetting markers = addSetting(new BooleanSetting("style_markers", "Custom: combo markers", false));
    private final BooleanSetting dome = addSetting(new BooleanSetting("style_dome", "Custom: dome shockwave", false));
    private final BooleanSetting worldWave = addSetting(new BooleanSetting("style_world_wave", "Custom: terrain shockwave", false));
    private final NumberSetting worldWaveRadius = addSetting(new NumberSetting("world_wave_radius", "Terrain wave radius", 16, 64, 1, 44));
    private final NumberSetting worldWaveThickness = addSetting(new NumberSetting("world_wave_thickness", "Terrain wave thickness", 1, 8, 0.5, 3));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "F2F2F2"));
    private final TextSetting secondary = addSetting(EffectPalette.hexSetting("secondary", "Secondary RGB hex", "8A8A8A"));
    private final NumberSetting count = addSetting(new NumberSetting("count", "Particle count", 0.25, 3, 0.05, 1));
    private final NumberSetting size = addSetting(new NumberSetting("size", "Size", 0.4, 2.5, 0.05, 1));
    private final NumberSetting speed = addSetting(new NumberSetting("speed", "Speed", 0.3, 2.5, 0.05, 1));
    private final NumberSetting lifetime = addSetting(new NumberSetting("lifetime", "Lifetime", 0.3, 3, 0.05, 1));
    private final NumberSetting gravity = addSetting(new NumberSetting("gravity", "Gravity", 0, 2, 0.05, 1));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.1, 1, 0.05, 0.9));
    private final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Glow", true));
    private final BooleanSetting critBoost = addSetting(new BooleanSetting("crit_boost", "Stronger critical hits", true));
    private final BooleanSetting self = addSetting(new BooleanSetting("self", "Also when I am hurt", false));

    private final ArrayDeque<Impact> impacts = new ArrayDeque<>();
    private final Map<Integer, Integer> hurtTimes = new HashMap<>();
    private final FxParticles.Spec spec = new FxParticles.Spec();
    private final Random random = new Random();
    private int pendingTarget = -1;
    private long pendingAt;
    private boolean pendingCrit;
    private double pendingFall;
    private Vec3 pendingImpact;
    private int comboTarget = -1;
    private int combo;
    private long lastComboAt;
    private Object boundLevel;
    /** Block samples the world wave may spend this frame, shared by every live wave. */
    private int waveBudget;
    /** Scratch corners reused for every block top of the wave, so the hot scan never allocates. */
    private final double[] waveTop = new double[12];
    /** Reused height probe the wave scan reads through; its column reference is set per wave. */
    private final SurfaceProbe probe = new SurfaceProbe();

    public HitEffectsModule() {
        super("hit_effects", "HitEffects", "World-space impact effects: sparks, ripples, shockwaves, ink splashes, rings and combos.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        impacts.clear();
        hurtTimes.clear();
        pendingTarget = -1;
        combo = 0;
        FxParticles.get().clearOwner(OWNER);
    }

    /** Called from the attack hook before the attack packet is sent. */
    public void beforeAttack(Minecraft client, Player player, Entity target) {
        if (!enabled() || player != client.player || !(target instanceof LivingEntity living) || client.level == null) return;
        boolean crit = player.getAttackStrengthScale(0.5F) > 0.9F && player.fallDistance > 0 && !player.onGround()
                && !player.onClimbable() && !player.isInWater() && !player.isPassenger()
                && !player.hasEffect(MobEffects.BLINDNESS) && !player.isSprinting();
        Vec3 impact = client.hitResult instanceof EntityHitResult hit && hit.getEntity() == target ? hit.getLocation()
                : nearest(target.getBoundingBox(), player.getEyePosition());
        if (trigger.value().equals("Attack attempts")) {
            spawnImpact(client, living, impact, crit, player.fallDistance);
            return;
        }
        pendingTarget = target.getId();
        pendingAt = System.nanoTime();
        pendingCrit = crit;
        pendingFall = player.fallDistance;
        pendingImpact = impact;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.level == null || client.player == null) { onDisable(client); return; }
        if (client.level != boundLevel) { onDisable(client); boundLevel = client.level; }
        long now = System.nanoTime();
        boolean any = trigger.value().equals("Any nearby damage");
        if (trigger.value().equals("Attack attempts")) return;
        Map<Integer, Integer> next = new HashMap<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity.distanceToSqr(client.player) > 48 * 48) continue;
            int hurt = living.hurtTime;
            if (hurt > 0 || hurtTimes.containsKey(entity.getId())) next.put(entity.getId(), hurt);
            Integer before = hurtTimes.get(entity.getId());
            boolean rising = hurt > 0 && (before == null || hurt > before);
            if (!rising) continue;
            boolean mine = entity.getId() == pendingTarget && (now - pendingAt) < 700_000_000L;
            boolean isSelf = entity == client.player;
            if (mine) {
                spawnImpact(client, living, pendingImpact, pendingCrit, pendingFall);
                pendingTarget = -1;
            } else if (any && !isSelf || isSelf && self.value()) {
                AABB box = living.getBoundingBox();
                spawnImpact(client, living, new Vec3(box.getCenter().x, box.minY + living.getBbHeight() * 0.6, box.getCenter().z), false, 0);
            }
        }
        hurtTimes.clear();
        hurtTimes.putAll(next);
    }

    private Set<Style> styles() {
        Set<Style> fixed = PRESETS.get(preset.value());
        if (fixed != null) return fixed;
        EnumSet<Style> set = EnumSet.noneOf(Style.class);
        if (sparks.value()) set.add(Style.SPARKS);
        if (burst.value()) set.add(Style.BURST);
        if (ripple.value()) set.add(Style.RIPPLE);
        if (shockwave.value()) set.add(Style.SHOCKWAVE);
        if (stars.value()) set.add(Style.STARS);
        if (smoke.value()) set.add(Style.SMOKE);
        if (ink.value()) set.add(Style.INK);
        if (rings.value()) set.add(Style.RINGS);
        if (energy.value()) set.add(Style.ENERGY);
        if (markers.value()) set.add(Style.MARKERS);
        if (dome.value()) set.add(Style.DOME);
        if (worldWave.value()) set.add(Style.WORLD_WAVE);
        return set;
    }

    private int primary(int accent) {
        return switch (preset.value()) {
            case "Cosmic" -> 0xB79CFF;
            case "Splash" -> colorMode.value().equals(EffectPalette.CUSTOM) ? EffectPalette.resolve(EffectPalette.CUSTOM, accent, color.value()) : 0x9BE8FF;
            case "Monochrome" -> 0xF2F2F2;
            default -> EffectPalette.resolve(colorMode.value(), accent, color.value());
        };
    }

    private int secondaryRgb() {
        return switch (preset.value()) {
            case "Cosmic" -> 0x6FA8FF;
            case "Splash" -> 0xE8FBFF;
            case "Monochrome" -> 0x9A9A9A;
            default -> EffectPalette.resolve(EffectPalette.CUSTOM, 0, secondary.value());
        };
    }

    private void spawnImpact(Minecraft client, LivingEntity target, Vec3 at, boolean crit, double fall) {
        if (at == null) return;
        long now = System.nanoTime();
        if (target.getId() == comboTarget && now - lastComboAt < 2_000_000_000L) combo++;
        else { combo = 1; comboTarget = target.getId(); }
        lastComboAt = now;
        double floorY = FxParticles.groundY(client.level, at.x, target.getY() + 0.2, at.z, 6);
        if (Double.isNaN(floorY)) floorY = target.getY();
        impacts.addLast(new Impact(at.x, at.y, at.z, floorY, now, crit && critBoost.value(), random.nextLong(), combo,
                styles(), at.subtract(client.player.getEyePosition()).normalize(), fall));
        while (impacts.size() > 32) impacts.removeFirst();
    }

    @Override
    public void emit(FxFrame f) {
        if (impacts.isEmpty()) return;
        int rgb = primary(f.accent), rgb2 = secondaryRgb();
        double life = lifetime.value(), alphaScale = opacity.value() * f.intensity, s = size.value();
        waveBudget = TerrainWave.MAX_BLOCKS_PER_FRAME;
        int liveWaves = 0;
        for (Impact impact : impacts) if (impact.styles.contains(Style.WORLD_WAVE)) liveWaves++;
        int waveShare = TerrainWave.blockBudget(liveWaves);
        Iterator<Impact> iterator = impacts.iterator();
        while (iterator.hasNext()) {
            Impact impact = iterator.next();
            if (!impact.spawned) { burstParticles(f, impact, rgb, rgb2); impact.spawned = true; }
            double age = (f.now - impact.born) / 1e9;
            double t = Math.min(1, age / (life * 1.2));
            // Every style but the terrain wave has finished by life*1.2; the wave keeps expanding to its own
            // MAX_LIFE_SECONDS, so a 44-block front is never cut off just before it arrives.
            double impactLife = impact.styles.contains(Style.WORLD_WAVE) ? TerrainWave.impactLife(life) : life * 1.2;
            if (age >= impactLife) { iterator.remove(); continue; }
            double power = impact.crit ? 1.35 : 1;
            if (impact.styles.contains(Style.RIPPLE)) {
                double flash = Math.max(0, 1 - t * 5.5);
                if (flash > 0) {
                    FxShapes.glowDot(f, impact.x, impact.y, impact.z, 0.15 * s * power, rgb, alphaScale * 0.50 * flash, true, true);
                }
                for (int k = 0; k < 2; k++) {
                    double local = Math.max(0, t * 1.6 - k * 0.22);
                    if (local <= 0 || local >= 1) continue;
                    double eased = 1 - Math.pow(1 - local, 3);
                    double base = (0.12 + eased * 0.7) * s * power;
                    FxShapes.billboardRing(f, impact.x, impact.y, impact.z, base * 1.06, 0.055 * s,
                            k == 0 ? rgb : rgb2, alphaScale * 0.16 * Math.pow(1 - local, 2), true);
                    FxShapes.billboardRing(f, impact.x, impact.y, impact.z, base, 0.030 * s,
                            k == 0 ? rgb : rgb2, alphaScale * Math.pow(1 - local, 1.5), true);
                }
            }
            if (impact.styles.contains(Style.SHOCKWAVE)) {
                double local = Math.min(1, t * 1.4);
                double eased = 1 - Math.pow(1 - local, 4);
                double r = (0.3 + eased * 1.9) * s * power;
                double a = alphaScale * Math.pow(1 - local, 1.3);
                f.batch.band(impact.x - f.camX, impact.floorY + 0.04 - f.camY, impact.z - f.camZ, r * 0.78, r,
                        f.quality.circleSegments(), FxShapes.argb(rgb, 0), FxShapes.argb(rgb, a * 0.4), false);
                f.batch.ring(impact.x - f.camX, impact.floorY + 0.04 - f.camY, impact.z - f.camZ, r,
                        f.quality.circleSegments(), FxShapes.argb(rgb2, a), 2.2F, false, 0, 0);
            }
            if (impact.styles.contains(Style.WORLD_WAVE)) {
                waveBudget -= worldWave(f, impact, age, rgb, alphaScale, Math.min(waveShare, waveBudget));
            }
            if (impact.styles.contains(Style.RINGS)) {
                double eased = 1 - Math.pow(1 - Math.min(1, t * 1.3), 3);
                double a = alphaScale * (1 - t);
                for (int k = 0; k < 3; k++) {
                    double tilt = FxShapes.hash(impact.seed, k) * Math.PI;
                    double nx = Math.cos(tilt) * Math.sin(k + 0.4), ny = Math.cos(k * 1.3 + 0.2), nz = Math.sin(tilt);
                    FxShapes.tiltedRing(f, impact.x, impact.y, impact.z, nx, ny, nz, (0.25 + eased * 0.55) * s * power,
                            age * (2.4 + k) * (f.reducedMotion ? 0.2 : 1), 0.7, k == 1 ? rgb2 : rgb, a, 2F, true);
                }
            }
            if (impact.styles.contains(Style.DOME)) dome(f, impact, t, rgb, rgb2, alphaScale, s);
            if (impact.styles.contains(Style.MARKERS) && t < 0.85) {
                double rise = f.reducedMotion ? 0 : Math.min(1, t * 2) * 0.45;
                double a = alphaScale * (t < 0.6 ? 1 : 1 - (t - 0.6) / 0.25);
                String label = impact.combo > 1 ? impact.combo + "x" : impact.crit ? "crit" : "hit";
                int textColor = FxShapes.argb(impact.crit ? rgb : 0xF2F2F2, a);
                f.text(new FxFrame.WorldText(impact.x, impact.y + 0.45 + rise, impact.z,
                        List.of(new FxFrame.Run(label, textColor)), (float) (0.018 * s * (impact.combo > 2 ? 1.25 : 1)),
                        0, impact.combo > 2 ? FxShapes.argb(rgb, a * 0.6) : 0, true, 0, false));
            }
        }
    }

    /**
     * One large terrain-following wavefront: every sampled block under the front gets a glowing top face and
     * a bright leading-edge outline. Sampling is capped per frame and per wave, and blocks behind the camera
     * are culled before the column lookup, so a 64-block wave stays a handful of hundred reads. The scan
     * itself lives in {@link TerrainWave#emitBand} so it can be driven from a unit test. Returns the number of
     * block tops emitted so the caller can keep the frame-wide budget exact.
     */
    private int worldWave(FxFrame f, Impact impact, double age, int rgb, double alphaScale, int budget) {
        if (f.client.level == null || budget <= 0) return 0;
        double progress = TerrainWave.progress(age, TerrainWave.MAX_LIFE_SECONDS);
        if (progress >= 1) return 0;
        double radius = TerrainWave.radius(progress, worldWaveRadius.value());
        probe.level = f.client.level;
        probe.nearY = impact.floorY;
        return TerrainWave.emitBand(f.batch, probe, impact.x, impact.z, f.camX, f.camY, f.camZ,
                radius, worldWaveThickness.value(), alphaScale * TerrainWave.alpha(progress), rgb,
                f.forwardX, f.forwardZ, budget, waveTop);
    }

    /** Walkable surface of a column near {@code nearY}; falls back to the heightmap for cliffs and platforms. */
    private static double surface(net.minecraft.world.level.Level level, double x, double nearY, double z) {
        double local = FxParticles.groundY(level, x, nearY + 3, z, 8);
        if (Double.isFinite(local)) return local;
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(x), (int) Math.floor(z));
        if (top <= level.getMinY()) return Double.NaN;
        double exact = FxParticles.groundY(level, x, top + 0.5, z, 2);
        return Double.isFinite(exact) ? exact : top;
    }

    /** Translucent hemisphere rising from the ground; brightest at its rim, larger after long falls (mace slams). */
    private void dome(FxFrame f, Impact impact, double t, int rgb, int rgb2, double alphaScale, double s) {
        double local = Math.min(1, t * 1.25);
        if (local >= 1) return;
        double eased = 1 - Math.pow(1 - local, 3);
        double size = s * (1.2 + Math.min(4, impact.fall * 0.35)) * (impact.crit ? 1.2 : 1);
        double r = 0.3 + eased * size * 2;
        double fade = Math.pow(1 - local, 1.4);
        int segments = f.quality.circleSegments();
        int rings = f.quality.ordinal() >= 2 ? 8 : 5;
        double cx = impact.x - f.camX, cy = impact.floorY + 0.02 - f.camY, cz = impact.z - f.camZ;
        for (int ring = 0; ring < rings; ring++) {
            double p0 = ring * Math.PI / 2 / rings, p1 = (ring + 1) * Math.PI / 2 / rings;
            // Fresnel-like falloff: the near-ground band reads strongest, the cap is almost clear.
            double a0 = alphaScale * fade * (0.08 + 0.32 * Math.pow(Math.cos(p0), 3));
            double a1 = alphaScale * fade * (0.08 + 0.32 * Math.pow(Math.cos(p1), 3));
            int c0 = FxShapes.argb(dev.jilaton.client.render.EffectBatch.mixRgb(rgb, rgb2, ring / (double) rings), a0);
            int c1 = FxShapes.argb(dev.jilaton.client.render.EffectBatch.mixRgb(rgb, rgb2, (ring + 1) / (double) rings), a1);
            double y0 = Math.sin(p0) * r, y1 = Math.sin(p1) * r, r0 = Math.cos(p0) * r, r1 = Math.cos(p1) * r;
            for (int i = 0; i < segments; i++) {
                double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
                f.batch.quad(cx + Math.cos(a) * r0, cy + y0, cz + Math.sin(a) * r0, cx + Math.cos(b) * r0, cy + y0, cz + Math.sin(b) * r0,
                        cx + Math.cos(b) * r1, cy + y1, cz + Math.sin(b) * r1, cx + Math.cos(a) * r1, cy + y1, cz + Math.sin(a) * r1,
                        c0, c0, c1, c1, false);
            }
        }
        f.batch.ring(cx, cy + 0.02, cz, r, segments, FxShapes.argb(0xFFFFFF, alphaScale * fade), 2.5F, false, 0, 0);
    }

    private void burstParticles(FxFrame f, Impact impact, int rgb, int rgb2) {
        double density = count.value() * f.quality.density() * (impact.crit ? 1.6 : 1);
        double sp = speed.value(), s = size.value(), life = lifetime.value(), g = gravity.value();
        double a = opacity.value() * f.intensity;
        Set<Style> st = impact.styles;
        if (st.contains(Style.SPARKS)) {
            int n = (int) Math.ceil(14 * density);
            for (int i = 0; i < n; i++) {
                spec.reset();
                position(impact, 0.05);
                direction(impact, 0.55, 3.2 * sp * (0.5 + random.nextDouble()));
                spec.gravity = 6 * g; spec.drag = 0.86; spec.life = life * (0.35 + random.nextDouble() * 0.3);
                spec.size0 = 0.05 * s; spec.size1 = 0.015 * s; spec.color = 0xFFFFFF; spec.color1 = rgb; spec.alpha = a;
                spec.shape = FxParticles.Shape.SPARK; spec.glow = glow.value(); spec.walls = true; spec.owner = OWNER;
                spec.floor = impact.floorY + 0.03;
                FxParticles.get().spawn(spec);
            }
        }
        if (st.contains(Style.BURST)) {
            int n = (int) Math.ceil(18 * density);
            for (int i = 0; i < n; i++) {
                spec.reset();
                position(impact, 0.08);
                direction(impact, 0.9, 2.2 * sp * (0.3 + random.nextDouble()));
                spec.gravity = 1.2 * g; spec.drag = 0.8; spec.life = life * (0.5 + random.nextDouble() * 0.4);
                spec.size0 = 0.07 * s; spec.size1 = 0.02 * s; spec.color = rgb; spec.color1 = rgb2; spec.alpha = a;
                spec.glow = glow.value(); spec.walls = true; spec.owner = OWNER;
                FxParticles.get().spawn(spec);
            }
        }
        if (st.contains(Style.STARS)) {
            int n = (int) Math.ceil(8 * density);
            for (int i = 0; i < n; i++) {
                spec.reset();
                position(impact, 0.1);
                direction(impact, 1, 1.6 * sp * (0.4 + random.nextDouble()));
                spec.vy += 0.6; spec.gravity = 0.6 * g; spec.drag = 0.82; spec.life = life * (0.7 + random.nextDouble() * 0.5);
                spec.size0 = 0.11 * s; spec.size1 = 0.03 * s; spec.color = random.nextBoolean() ? rgb : rgb2; spec.alpha = a;
                spec.shape = FxParticles.Shape.STAR; spec.spin = (random.nextDouble() - 0.5) * 6; spec.rot = random.nextDouble() * 6;
                spec.glow = glow.value(); spec.walls = true; spec.owner = OWNER;
                FxParticles.get().spawn(spec);
            }
        }
        if (st.contains(Style.SMOKE)) {
            int n = (int) Math.ceil(6 * density);
            for (int i = 0; i < n; i++) {
                spec.reset();
                position(impact, 0.15);
                direction(impact, 1, 0.5 * sp);
                spec.vy = 0.25 + random.nextDouble() * 0.3; spec.drag = 0.9; spec.life = life * (1.1 + random.nextDouble() * 0.6);
                spec.size0 = 0.15 * s; spec.size1 = 0.5 * s; spec.color = 0xBDBDBD; spec.color1 = 0x404040; spec.alpha = a * 0.45;
                spec.shape = FxParticles.Shape.SMOKE; spec.owner = OWNER;
                FxParticles.get().spawn(spec);
            }
        }
        if (st.contains(Style.INK)) {
            // A main splat under the target plus droplets that arc out and land as smaller splats.
            FxParticles.get().decal(impact.x + (random.nextDouble() - 0.5) * 0.4, impact.floorY + 0.015,
                    impact.z + (random.nextDouble() - 0.5) * 0.4, (0.55 + random.nextDouble() * 0.3) * s * (impact.crit ? 1.3 : 1),
                    life * 2.6, rgb, a * 0.85, OWNER);
            int n = (int) Math.ceil(9 * density);
            for (int i = 0; i < n; i++) {
                spec.reset();
                position(impact, 0.05);
                direction(impact, 1.1, 3.4 * sp * (0.4 + random.nextDouble()));
                spec.vy = Math.abs(spec.vy) * 0.6 + 1.2; spec.gravity = 9 * Math.max(0.3, g); spec.drag = 0.97;
                spec.life = 2.5; spec.size0 = spec.size1 = (0.05 + random.nextDouble() * 0.06) * s;
                spec.color = rgb; spec.alpha = a * 0.9; spec.shape = FxParticles.Shape.DROPLET; spec.walls = false;
                spec.floor = impact.floorY; spec.owner = OWNER;
                FxParticles.get().spawn(spec);
            }
        }
        if (st.contains(Style.ENERGY)) {
            int n = (int) Math.ceil(16 * density);
            for (int i = 0; i < n; i++) {
                spec.reset();
                double angle = i * Math.PI * 2 / n;
                double r = 0.15;
                spec.x = impact.x + Math.cos(angle) * r; spec.y = impact.y - 0.3 + i * 0.6 / n; spec.z = impact.z + Math.sin(angle) * r;
                double tangential = 2.2 * sp;
                spec.vx = -Math.sin(angle) * tangential + Math.cos(angle) * 0.8; spec.vz = Math.cos(angle) * tangential + Math.sin(angle) * 0.8;
                spec.vy = 1.2 * sp; spec.drag = 0.88; spec.life = life * 0.7;
                spec.size0 = 0.06 * s; spec.size1 = 0.01 * s; spec.color = rgb; spec.color1 = rgb2; spec.alpha = a;
                spec.shape = FxParticles.Shape.SPARK; spec.glow = glow.value(); spec.walls = true; spec.owner = OWNER;
                FxParticles.get().spawn(spec);
            }
        }
        if (impact.crit && critBoost.value()) {
            for (int i = 0; i < Math.ceil(6 * density); i++) {
                spec.reset();
                position(impact, 0.05);
                direction(impact, 1, 2.6 * sp);
                spec.drag = 0.8; spec.life = life * 0.6; spec.size0 = 0.13 * s; spec.size1 = 0;
                spec.color = 0xFFFFFF; spec.alpha = a; spec.shape = FxParticles.Shape.STAR; spec.rot = random.nextDouble() * 6;
                spec.spin = 4; spec.glow = true; spec.walls = true; spec.owner = OWNER;
                FxParticles.get().spawn(spec);
            }
        }
    }

    private void position(Impact impact, double jitter) {
        spec.x = impact.x + random.nextGaussian() * jitter;
        spec.y = impact.y + random.nextGaussian() * jitter;
        spec.z = impact.z + random.nextGaussian() * jitter;
    }

    /** Velocity biased away from the attacker (along the hit normal) with {@code randomness} spread. */
    private void direction(Impact impact, double randomness, double magnitude) {
        double x = impact.dir.x + random.nextGaussian() * randomness;
        double y = impact.dir.y + random.nextGaussian() * randomness + 0.35;
        double z = impact.dir.z + random.nextGaussian() * randomness;
        double l = Math.sqrt(x * x + y * y + z * z);
        if (l < 1e-6) { y = 1; l = 1; }
        spec.vx = x / l * magnitude; spec.vy = y / l * magnitude; spec.vz = z / l * magnitude;
    }

    private static Vec3 nearest(AABB box, Vec3 from) {
        return new Vec3(Math.max(box.minX, Math.min(box.maxX, from.x)), Math.max(box.minY, Math.min(box.maxY, from.y)),
                Math.max(box.minZ, Math.min(box.maxZ, from.z)));
    }

    /** Visible for tests. */
    static Set<Style> presetStyles(String preset) { return PRESETS.get(preset); }

    int activeImpacts() { return impacts.size(); }

    /** Reusable column lookup for the wave scan, so the hot path never allocates a lambda or probe. */
    private static final class SurfaceProbe implements TerrainWave.HeightSampler {
        net.minecraft.world.level.Level level;
        double nearY;

        @Override
        public double surfaceY(double x, double z) {
            return surface(level, x, nearY, z);
        }
    }

    private static final class Impact {
        final double x, y, z, floorY;
        final long born, seed;
        final boolean crit;
        final int combo;
        final Set<Style> styles;
        final Vec3 dir;
        final double fall;
        boolean spawned;

        Impact(double x, double y, double z, double floorY, long born, boolean crit, long seed, int combo, Set<Style> styles, Vec3 dir, double fall) {
            this.x = x; this.y = y; this.z = z; this.floorY = floorY; this.born = born; this.crit = crit;
            this.seed = seed; this.combo = combo; this.styles = styles; this.dir = dir; this.fall = fall;
        }
    }
}
