package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxParticles;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Cosmetic effects attached to players. Halos, orbiting relics, wings and companions are drawn as real
 * world-space geometry that follows interpolated body position, yaw and crouch, so they move with the model
 * instead of snapping per tick. Footstep, sprint, jump and aura layers are particle emitters driven from
 * the tick loop. Self effects that would sit in front of the camera are hidden in first person.
 */
public final class PlayerEffectsModule extends ClientModule implements FxEmitter {
    private static final int OWNER = FxParticles.ownerId("player_effects");
    private static final int MAX_TRACKS = 64;
    private final ChoiceSetting targets = addSetting(new ChoiceSetting("targets", "Players", List.of("Self", "Self + friends", "Everyone"), "Self"));
    private final ChoiceSetting mode = addSetting(new ChoiceSetting("mode", "Intensity", List.of("Minimal", "Balanced", "Cinematic"), "Balanced"));
    private final ChoiceSetting halo = addSetting(new ChoiceSetting("halo", "Halo", List.of("Off", "Ring", "Double", "Aureole", "Sigil", "Crown"), "Aureole"));
    private final NumberSetting haloTilt = addSetting(new NumberSetting("halo_tilt", "Halo tilt", 0, 60, 1, 12));
    private final ChoiceSetting relics = addSetting(new ChoiceSetting("relics", "Orbiting relics", List.of("Off", "Crystals", "Stars", "Orbs", "Blades", "Runes"), "Off"));
    private final NumberSetting relicCount = addSetting(new NumberSetting("relic_count", "Relic count", 1, 8, 1, 3));
    private final NumberSetting orbitRadius = addSetting(new NumberSetting("orbit_radius", "Orbit radius", 0.4, 2, 0.05, 0.85));
    private final NumberSetting orbitSpeed = addSetting(new NumberSetting("orbit_speed", "Orbit speed", 0.1, 3, 0.05, 0.8));
    private final BooleanSetting relicTrail = addSetting(new BooleanSetting("relic_trail", "Relic trails", true));
    private final ChoiceSetting wings = addSetting(new ChoiceSetting("wings", "Wings", List.of("Off", "Minimal", "Geometric", "Particle", "Dark Angel", "Glow"), "Off"));
    private final NumberSetting wingSize = addSetting(new NumberSetting("wing_size", "Wing size", 0.5, 2, 0.05, 1));
    private final NumberSetting wingSpread = addSetting(new NumberSetting("wing_spread", "Wing spread", 0.3, 1.5, 0.05, 0.85));
    private final ChoiceSetting companion = addSetting(new ChoiceSetting("companion", "Companion", List.of("Off", "Wisp", "Ember", "Sentinel"), "Off"));
    private final ChoiceSetting companionSide = addSetting(new ChoiceSetting("companion_side", "Companion side", List.of("Left", "Right"), "Right"));
    private final ChoiceSetting footsteps = addSetting(new ChoiceSetting("footsteps", "Footsteps", List.of("Off", "Dots", "Rings", "Stars"), "Off"));
    private final BooleanSetting sprintDust = addSetting(new BooleanSetting("sprint_dust", "Sprint particles", true));
    private final BooleanSetting jumpBurst = addSetting(new BooleanSetting("jump_burst", "Jump particles", true));
    private final ChoiceSetting aura = addSetting(new ChoiceSetting("aura", "Aura", List.of("Off", "Rising", "Shroud", "Radiance"), "Off"));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "F2F2F2"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.1, 1, 0.05, 0.85));
    private final BooleanSetting firstPerson = addSetting(new BooleanSetting("first_person", "Show own effects in first person", false));
    private final Map<Integer, Track> tracks = new HashMap<>();
    private final FxParticles.Spec spec = new FxParticles.Spec();
    private final Random random = new Random();
    private Object boundLevel;

    public PlayerEffectsModule() {
        super("player_effects", "PlayerEffects", "Halos, orbiting relics, particle wings, companions, footsteps and auras.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        tracks.clear();
        FxParticles.get().clearOwner(OWNER);
    }

    private double density() {
        return switch (mode.value()) { case "Minimal" -> 0.5; case "Cinematic" -> 1.6; default -> 1; };
    }

    private boolean accepts(Minecraft client, Player player) {
        if (player == client.player) return true;
        if (player.isSpectator() || player.isInvisible()) return false;
        return switch (targets.value()) {
            case "Everyone" -> true;
            case "Self + friends" -> FriendManager.contains(player);
            default -> false;
        };
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.level == null || client.player == null) { onDisable(client); return; }
        if (client.level != boundLevel) { onDisable(client); boundLevel = client.level; }
        int rgb = EffectPalette.resolve(colorMode.value(), dev.jilaton.client.gui.ClientTheme.resolveAccent(
                dev.jilaton.client.JilatonClient.clientConfig().themeId(), dev.jilaton.client.JilatonClient.clientConfig().customAccent()), color.value());
        double a = opacity.value();
        Map<Integer, Track> seen = new HashMap<>();
        for (Player player : client.level.players()) {
            if (!accepts(client, player) || player.distanceToSqr(client.player) > 64 * 64) continue;
            Track track = tracks.getOrDefault(player.getId(), new Track(player.position(), player.onGround()));
            seen.put(player.getId(), track);
            Vec3 pos = player.position();
            double moved = pos.subtract(track.last).horizontalDistance();
            track.stride += moved;
            if (!footsteps.value().equals("Off") && player.onGround() && track.stride > 0.85) {
                track.stride = 0;
                track.left = !track.left;
                double yaw = Math.toRadians(player.getYRot());
                double side = track.left ? 0.16 : -0.16;
                double fx = pos.x + Math.cos(yaw) * side, fz = pos.z + Math.sin(yaw) * side;
                spec.reset();
                spec.x = fx; spec.y = pos.y + 0.03; spec.z = fz; spec.drag = 1; spec.life = 2.4;
                spec.color = rgb; spec.alpha = a * 0.7; spec.owner = OWNER;
                switch (footsteps.value()) {
                    case "Rings" -> { spec.shape = FxParticles.Shape.RING; spec.size0 = 0.08; spec.size1 = 0.22; }
                    case "Stars" -> { spec.shape = FxParticles.Shape.STAR; spec.size0 = 0.12; spec.size1 = 0.05; spec.rot = yaw; }
                    default -> { spec.size0 = 0.07; spec.size1 = 0.04; }
                }
                FxParticles.get().spawn(spec);
            }
            if (sprintDust.value() && player.isSprinting() && player.onGround() && moved > 0.15 && random.nextDouble() < 0.8 * density()) {
                spec.reset();
                spec.x = pos.x + random.nextGaussian() * 0.15; spec.y = pos.y + 0.08; spec.z = pos.z + random.nextGaussian() * 0.15;
                Vec3 back = pos.subtract(track.last).normalize().scale(-0.6);
                spec.vx = back.x + random.nextGaussian() * 0.2; spec.vy = 0.25 + random.nextDouble() * 0.2; spec.vz = back.z + random.nextGaussian() * 0.2;
                spec.drag = 0.85; spec.life = 0.8; spec.size0 = 0.08; spec.size1 = 0.22; spec.color = rgb; spec.color1 = 0x5A5A5A;
                spec.alpha = a * 0.35; spec.shape = FxParticles.Shape.SMOKE; spec.owner = OWNER;
                FxParticles.get().spawn(spec);
            }
            if (jumpBurst.value() && track.onGround && !player.onGround() && player.getDeltaMovement().y > 0.08) {
                int n = (int) (14 * density());
                for (int i = 0; i < n; i++) {
                    double angle = i * Math.PI * 2 / n;
                    spec.reset();
                    spec.x = pos.x + Math.cos(angle) * 0.25; spec.y = pos.y + 0.05; spec.z = pos.z + Math.sin(angle) * 0.25;
                    spec.vx = Math.cos(angle) * 1.4; spec.vz = Math.sin(angle) * 1.4; spec.vy = 0.3;
                    spec.drag = 0.8; spec.life = 0.55; spec.size0 = 0.05; spec.size1 = 0.01; spec.color = rgb; spec.alpha = a;
                    spec.shape = FxParticles.Shape.SPARK; spec.glow = true; spec.owner = OWNER;
                    FxParticles.get().spawn(spec);
                }
            }
            auraTick(pos, player, rgb, a);
            track.last = pos;
            track.onGround = player.onGround();
        }
        tracks.clear();
        tracks.putAll(seen);
    }

    private void auraTick(Vec3 pos, Player player, int rgb, double a) {
        if (aura.value().equals("Off")) return;
        int count = switch (aura.value()) { case "Radiance" -> 3; case "Shroud" -> 2; default -> 2; };
        for (int i = 0; i < count; i++) {
            if (random.nextDouble() > 0.65 * density()) continue;
            double angle = random.nextDouble() * Math.PI * 2;
            double r = 0.42 + random.nextDouble() * 0.18;
            spec.reset();
            switch (aura.value()) {
                case "Shroud" -> {
                    spec.x = pos.x + Math.cos(angle) * r; spec.y = pos.y + 0.15 + random.nextDouble() * 1.1;
                    spec.z = pos.z + Math.sin(angle) * r;
                    spec.vx = -Math.sin(angle) * 0.35; spec.vz = Math.cos(angle) * 0.35; spec.vy = 0.06;
                    spec.drag = 0.96; spec.life = 1.6; spec.size0 = 0.05; spec.size1 = 0.015;
                    spec.alpha = a * 0.55; spec.glow = true;
                }
                case "Radiance" -> {
                    spec.x = pos.x + random.nextGaussian() * 0.32; spec.y = pos.y + random.nextDouble() * 0.6;
                    spec.z = pos.z + random.nextGaussian() * 0.32;
                    spec.vy = 1.2 + random.nextDouble() * 0.7; spec.vx = random.nextGaussian() * 0.05; spec.vz = random.nextGaussian() * 0.05;
                    spec.drag = 0.97; spec.life = 1.3; spec.size0 = 0.035; spec.size1 = 0.0;
                    spec.alpha = a * 0.8; spec.glow = true;
                }
                default -> {
                    spec.x = pos.x + Math.cos(angle) * r; spec.y = pos.y + random.nextDouble() * 0.35;
                    spec.z = pos.z + Math.sin(angle) * r;
                    spec.vy = 0.8 + random.nextDouble() * 0.5; spec.vx = -Math.sin(angle) * 0.2; spec.vz = Math.cos(angle) * 0.2;
                    spec.drag = 0.95; spec.life = 1.4; spec.size0 = 0.045; spec.size1 = 0.0;
                    spec.alpha = a * 0.8; spec.glow = true;
                }
            }
            spec.color = rgb; spec.owner = OWNER;
            FxParticles.get().spawn(spec);
        }
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        if (client.level == null || client.player == null) return;
        int rgb = EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        double a = opacity.value() * f.intensity;
        double time = f.seconds() * (f.reducedMotion ? 0.25 : 1);
        if (tracks.size() > MAX_TRACKS) return;
        for (Player player : client.level.players()) {
            if (!tracks.containsKey(player.getId())) continue;
            boolean hiddenSelf = player == client.player && f.firstPerson && !firstPerson.value();
            if (hiddenSelf) continue;
            Vec3 pos = player.getPosition(f.partial);
            double bodyYaw = Math.toRadians(Mth.rotLerp(f.partial, player.yBodyRotO, player.yBodyRot));
            double height = player.getBbHeight();
            double crouch = player.isCrouching() ? -0.25 : 0;
            if (!halo.value().equals("Off")) halo(f, pos, height + crouch, bodyYaw, rgb, a, time);
            if (!relics.value().equals("Off")) relics(f, pos, height + crouch, rgb, a, time);
            if (!wings.value().equals("Off")) wings(f, player, pos, bodyYaw, height + crouch, rgb, a, time);
            if (!companion.value().equals("Off")) companion(f, pos, height + crouch, bodyYaw, rgb, a, time);
            if (!aura.value().equals("Off")) auraGeometry(f, pos, height, rgb, a, time);
        }
    }

    // ---------------------------------------------------------------- halos

    private void halo(FxFrame f, Vec3 pos, double height, double bodyYaw, int rgb, double a, double time) {
        double x = pos.x, z = pos.z;
        double y = pos.y + height + 0.24 + Math.sin(time * 1.7 + pos.x * 0.3) * 0.018;
        double r = 0.40;
        int seg = f.quality.circleSegments();
        double tilt = Math.toRadians(haloTilt.value());
        double[] n = {Math.sin(tilt), Math.cos(tilt), 0};
        switch (halo.value()) {
            case "Ring" -> {
                f.batch.band(x - f.camX, y - f.camY, z - f.camZ, r - 0.045, r + 0.05, seg,
                        FxShapes.argb(rgb, a * 0.10), FxShapes.argb(rgb, 0), true);
                FxShapes.tiltedRing(f, x, y, z, n[0], n[1], n[2], r, time * 1.1, 1, rgb, a, 2.0F, true);
                FxShapes.tiltedRing(f, x, y, z, n[0], n[1], n[2], r * 0.86, -time * 0.75, 1, rgb, a * 0.35, 1.2F, true);
                double[] p = orbitPoint(n, r, time * 1.4);
                FxShapes.glowDot(f, x + p[0], y + p[1], z + p[2], 0.028, rgb, a, true, true);
            }
            case "Double" -> {
                f.batch.band(x - f.camX, y - f.camY, z - f.camZ, r - 0.06, r + 0.08, seg,
                        FxShapes.argb(rgb, a * 0.08), FxShapes.argb(rgb, 0), true);
                FxShapes.tiltedRing(f, x, y, z, Math.sin(tilt), Math.cos(tilt), 0, r, time, 1, rgb, a, 1.8F, true);
                FxShapes.tiltedRing(f, x, y + 0.055, z, -Math.sin(tilt), Math.cos(tilt), 0, r * 0.72, -time * 1.3, 1, rgb, a * 0.7, 1.5F, true);
                for (int i = 0; i < 2; i++) {
                    double[] p = orbitPoint(i == 0 ? n : new double[]{-n[0], n[1], 0}, i == 0 ? r : r * 0.72, time * (i == 0 ? 1.4 : -1.8) + i * 2);
                    FxShapes.glowDot(f, x + p[0], y + (i == 0 ? 0 : 0.055) + p[1], z + p[2], 0.024, rgb, a * 0.9, true, true);
                }
            }
            case "Aureole" -> {
                FxShapes.glowDot(f, x, y, z, 0.30, rgb, a * 0.20, true, true);
                f.batch.band(x - f.camX, y - f.camY, z - f.camZ, r - 0.11, r + 0.03, seg,
                        FxShapes.argb(rgb, 0), FxShapes.argb(rgb, a * 0.18), true);
                FxShapes.tiltedRing(f, x, y, z, n[0], n[1], n[2], r, time * 0.8, 1.25, rgb, a, 2.6F, true);
                FxShapes.tiltedRing(f, x, y + 0.005, z, -n[0], n[1], 0, r * 0.78, -time * 1.15, 1, rgb, a * 0.55, 1.8F, true);
                for (int i = 0; i < 4; i++) {
                    double[] p = orbitPoint(new double[]{Math.sin(tilt + i * 0.4), Math.cos(tilt + i * 0.4), 0},
                            r * (1.02 + i * 0.05), time * (1.2 + i * 0.35) + i * 2.1);
                    FxShapes.glowDot(f, x + p[0], y + p[1], z + p[2], 0.032, rgb, a * 0.9, true, true);
                }
            }
            case "Sigil" -> {
                f.batch.ring(x - f.camX, y - f.camY, z - f.camZ, r, seg, FxShapes.argb(rgb, a), 1.8F, true, 0.7, time);
                f.batch.ring(x - f.camX, y - f.camY, z - f.camZ, r * 0.72, seg / 2, FxShapes.argb(rgb, a * 0.5), 1.2F, true, 0.5, -time * 1.4);
                int ticks = 12;
                for (int i = 0; i < ticks; i++) {
                    double ang = time * 0.6 + i * Math.PI * 2 / ticks;
                    double ca = Math.cos(ang), sa = Math.sin(ang);
                    double inner = r * (i % 3 == 0 ? 1.02 : 1.07), outer = r * 1.14;
                    f.batch.line(x + ca * inner - f.camX, y - f.camY, z + sa * inner - f.camZ,
                            x + ca * outer - f.camX, y - f.camY, z + sa * outer - f.camZ,
                            FxShapes.argb(rgb, a * (i % 3 == 0 ? 0.9 : 0.45)), i % 3 == 0 ? 1.6F : 1.1F, true);
                }
            }
            case "Crown" -> {
                f.batch.ring(x - f.camX, y - f.camY, z - f.camZ, r * 0.95, seg, FxShapes.argb(rgb, a * 0.45), 1.6F, true, 0.6, time * 0.3);
                int points = 7;
                for (int i = 0; i < points; i++) {
                    double ang = time * 0.5 + i * Math.PI * 2 / points;
                    double bob = Math.sin(time * 1.6 + i * 1.3) * 0.045;
                    double cx = x + Math.cos(ang) * r, cz = z + Math.sin(ang) * r, cy = y + 0.06 + bob;
                    FxShapes.glowDot(f, cx, cy - 0.03, cz, 0.045, rgb, a * 0.6, true, true);
                    FxShapes.crystal(f, cx, cy + 0.075, cz, 0.038, 0.115 + Math.sin(time + i) * 0.012, ang, rgb, a, true);
                }
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- relics

    private void relics(FxFrame f, Vec3 pos, double height, int rgb, double a, double time) {
        int n = (int) relicCount.value();
        double radius = orbitRadius.value();
        double baseY = pos.y + height * 0.55;
        for (int i = 0; i < n; i++) {
            double phase = i * Math.PI * 2 / n;
            double angle = time * orbitSpeed.value() * 1.6 + phase;
            double ox = pos.x + Math.cos(angle) * radius;
            double oz = pos.z + Math.sin(angle) * radius;
            double oy = baseY + Math.sin(time * 1.3 + phase) * 0.12;
            if (relicTrail.value()) {
                int samples = f.quality.ordinal() >= 2 ? 5 : 3;
                for (int k = 1; k <= samples; k++) {
                    double ta = angle - k * 0.11;
                    double tx = pos.x + Math.cos(ta) * radius, tz = pos.z + Math.sin(ta) * radius;
                    double ty = baseY + Math.sin(time * 1.3 + phase - k * 0.08) * 0.12;
                    FxShapes.softDot(f, tx, ty, tz, 0.032 - k * 0.004, rgb, a * (0.32 - k * 0.05), true);
                }
            }
            switch (relics.value()) {
                case "Stars" -> {
                    FxShapes.star(f, ox, oy, oz, 0.11, time * 1.5 + phase, 5, rgb, a, true);
                    FxShapes.glowDot(f, ox, oy, oz, 0.035, rgb, a * 0.75, true, true);
                }
                case "Orbs" -> {
                    FxShapes.glowDot(f, ox, oy, oz, 0.07, rgb, a, true, true);
                    FxShapes.billboardRing(f, ox, oy, oz, 0.115, 0.016, rgb, a * 0.6, true);
                }
                case "Blades" -> {
                    FxShapes.crystal(f, ox, oy, oz, 0.034, 0.24, angle + Math.PI / 2, rgb, a, true);
                    FxShapes.glowDot(f, ox, oy, oz, 0.022, rgb, a * 0.7, true, true);
                }
                case "Runes" -> {
                    double[] normal = {Math.cos(angle) * 0.4, 0.9, Math.sin(angle) * 0.4};
                    FxShapes.tiltedRing(f, ox, oy, oz, normal[0], normal[1], normal[2], 0.10, time * 1.8 + phase, 1, rgb, a * 0.85, 1.7F, true);
                    FxShapes.star(f, ox, oy, oz, 0.06, -time * 1.2 + phase, 4, rgb, a, true);
                }
                default -> {
                    FxShapes.crystal(f, ox, oy, oz, 0.075, 0.19, time * 1.2 + phase, rgb, a, true);
                    FxShapes.glowDot(f, ox, oy, oz, 0.032, rgb, a * 0.7, true, true);
                }
            }
        }
    }

    // ---------------------------------------------------------------- wings

    private void wings(FxFrame f, Player player, Vec3 pos, double bodyYaw, double height, int rgb, double a, double time) {
        double speed = player.getDeltaMovement().horizontalDistance();
        boolean airborne = !player.onGround();
        double flap = Math.sin(time * (airborne ? 6.2 : 2.0)) * (airborne ? 0.42 : 0.10) + Math.min(0.30, speed * 1.5);
        double size = wingSize.value();
        double spread = wingSpread.value() + flap * 0.35;
        double fx = -Math.sin(bodyYaw), fz = Math.cos(bodyYaw);
        double rx = fz, rz = -fx;
        int ribs = switch (wings.value()) {
            case "Geometric" -> 5;
            case "Particle" -> f.quality.ordinal() >= 2 ? 12 : 8;
            default -> f.quality.ordinal() >= 2 ? 10 : 7;
        };
        for (int side = -1; side <= 1; side += 2) {
            double bx = pos.x + rx * side * 0.14, by = pos.y + height * 0.80 + 0.02, bz = pos.z + rz * side * 0.14;
            double lift = flap * 0.30 * size;
            double[] base = {bx, by, bz};
            double[] topC = {bx + rx * side * 0.26 * size, by + (0.58 + lift * 0.6) * size, bz + rz * side * 0.26 * size - fx * 0.06 * size};
            double[] botC = {bx + rx * side * 0.30 * size, by + (0.02 + lift * 0.2) * size, bz + rz * side * 0.30 * size - fx * 0.20 * size};
            double[] topTip = {bx + rx * side * 0.98 * size * spread, by + (0.34 + lift) * size - 0.05, bz + rz * side * 0.98 * size * spread - fx * 0.26 * size};
            double[] botTip = {bx + rx * side * 0.74 * size * spread, by + (-0.28 - lift * 0.25) * size, bz + rz * side * 0.74 * size * spread - fx * 0.48 * size};
            double[][] top = new double[ribs + 1][];
            double[][] bot = new double[ribs + 1][];
            for (int i = 0; i <= ribs; i++) {
                double t = i / (double) ribs;
                top[i] = bezier(base, topC, topTip, t);
                bot[i] = bezier(base, botC, botTip, t);
            }
            // Scalloped trailing edge: alternating notches turn the smooth curve into feather lobes.
            double notch = switch (wings.value()) {
                case "Dark Angel" -> 0.26;
                case "Minimal" -> 0.14;
                case "Glow" -> 0.12;
                case "Particle" -> 0.10;
                default -> 0.0;
            };
            if (notch > 0 && ribs > 2) {
                for (int i = 1; i < ribs; i += 2) {
                    for (int k = 0; k < 3; k++) bot[i][k] += (top[i][k] - bot[i][k]) * notch;
                }
            }
            switch (wings.value()) {
                case "Minimal" -> wingMinimal(f, top, bot, rgb, a, time);
                case "Geometric" -> wingGeometric(f, top, bot, rgb, a);
                case "Particle" -> wingParticle(f, top, bot, rgb, a, time);
                case "Dark Angel" -> wingDark(f, top, bot, rgb, a, time);
                default -> wingGlow(f, top, bot, rgb, a, time);
            }
        }
    }

    private void wingMinimal(FxFrame f, double[][] top, double[][] bot, int rgb, double a, double time) {
        int n = top.length - 1;
        for (int i = 0; i < n; i++) {
            glowLine(f, top[i], top[i + 1], FxShapes.argb(rgb, a * (0.95 - 0.35 * i / (double) n)), 1.6F);
            line(f, bot[i], bot[i + 1], FxShapes.argb(rgb, a * 0.35), 1.1F);
            line(f, top[i], bot[i], FxShapes.argb(rgb, a * 0.14), 1F);
            if (i % 2 == 0)
                quad(f, top[i], top[i + 1], bot[i + 1], bot[i], FxShapes.argb(rgb, a * 0.045), FxShapes.argb(rgb, a * 0.03),
                        FxShapes.argb(rgb, 0), FxShapes.argb(rgb, 0));
        }
        FxShapes.glowDot(f, top[n][0], top[n][1], top[n][2], 0.032, rgb, a * 0.8, true, true);
    }

    private void wingGeometric(FxFrame f, double[][] top, double[][] bot, int rgb, double a) {
        int n = top.length - 1;
        for (int i = 0; i < n; i++) {
            int c0 = FxShapes.argb(rgb, a * 0.20), c1 = FxShapes.argb(rgb, a * 0.055);
            quad(f, top[i], top[i + 1], bot[i + 1], bot[i], c0, c0, c1, c1);
            glowLine(f, top[i], top[i + 1], FxShapes.argb(rgb, a), 1.7F);
            line(f, bot[i], bot[i + 1], FxShapes.argb(rgb, a * 0.55), 1.1F);
            line(f, top[i], bot[i], FxShapes.argb(rgb, a * 0.42), 1.2F);
        }
        FxShapes.crystal(f, top[n][0], top[n][1], top[n][2], 0.03, 0.09, 0.6, rgb, a, true);
    }

    private void wingParticle(FxFrame f, double[][] top, double[][] bot, int rgb, double a, double time) {
        int n = top.length - 1;
        int step = Math.max(1, n / 6);
        for (int i = 0; i <= n; i++) {
            double shimmer = 0.65 + 0.35 * Math.sin(time * 3 + i * 1.7);
            double fade = 1 - 0.55 * i / (double) n;
            FxShapes.softDot(f, top[i][0], top[i][1], top[i][2], 0.022 + 0.01 * shimmer, rgb, a * 0.9 * fade * shimmer, true);
            if (i % step == 0 && i < n) {
                for (int k = 1; k <= 3; k++) {
                    double t = k / 4.0;
                    double px = top[i][0] + (bot[i][0] - top[i][0]) * t;
                    double py = top[i][1] + (bot[i][1] - top[i][1]) * t;
                    double pz = top[i][2] + (bot[i][2] - top[i][2]) * t;
                    FxShapes.softDot(f, px, py, pz, 0.016, rgb, a * 0.4 * fade * shimmer, true);
                }
            }
        }
        line(f, top[0], top[n], FxShapes.argb(rgb, a * 0.22), 1.2F);
    }

    private void wingDark(FxFrame f, double[][] top, double[][] bot, int rgb, double a, double time) {
        int n = top.length - 1;
        int deep = EffectBatch.mixRgb(0x000000, rgb, 0.16);
        int slate = EffectBatch.mixRgb(0x0E1220, rgb, 0.30);
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            int cInner = FxShapes.argb(deep, a * (0.70 + 0.22 * (1 - t)));
            int cOuter = FxShapes.argb(slate, a * (0.44 + 0.16 * t));
            quad(f, top[i], top[i + 1], bot[i + 1], bot[i], cInner, cInner, cOuter, cOuter);
            line(f, top[i], bot[i], FxShapes.argb(slate, a * 0.20), 1F);
        }
        int bone = EffectBatch.mixRgb(rgb, 0xFFFFFF, 0.18);
        for (int i = 0; i < n; i++) {
            glowLine(f, top[i], top[i + 1], FxShapes.argb(bone, a * (0.46 + 0.34 * i / (double) n)), 1.4F);
        }
        // Trailing edge: one soft glow along the scalloped hem instead of ornaments.
        for (int i = 1; i < n; i += 2) {
            glowLine(f, bot[i], bot[i + 1], FxShapes.argb(rgb, a * (0.20 + 0.05 * Math.sin(time * 1.6 + i))), 1F);
        }
        FxShapes.glowDot(f, top[n][0], top[n][1], top[n][2], 0.020, bone, a * 0.65, true, true);
    }

    private void wingGlow(FxFrame f, double[][] top, double[][] bot, int rgb, double a, double time) {
        int n = top.length - 1;
        for (int layer = 0; layer < 3; layer++) {
            double offset = layer * 0.035;
            double alpha = a * (0.55 - layer * 0.14);
            for (int i = 0; i < n; i++) {
                double[] a0 = {top[i][0], top[i][1] - offset, top[i][2]};
                double[] a1 = {top[i + 1][0], top[i + 1][1] - offset, top[i + 1][2]};
                glowLine(f, a0, a1, FxShapes.argb(rgb, alpha), 1.4F);
            }
        }
        for (int i = 0; i < n; i++) {
            double f0 = 1 - 0.6 * i / (double) n;
            quad(f, top[i], top[i + 1], bot[i + 1], bot[i], FxShapes.argb(rgb, a * 0.10 * f0), FxShapes.argb(rgb, a * 0.07 * f0),
                    FxShapes.argb(rgb, 0), FxShapes.argb(rgb, 0));
        }
        for (int i = 0; i <= n; i += 2) {
            double shimmer = 0.7 + 0.3 * Math.sin(time * 2.6 + i * 1.4);
            FxShapes.glowDot(f, top[i][0], top[i][1], top[i][2], 0.024, rgb, a * 0.5 * shimmer, true, true);
        }
    }

    private static double[] bezier(double[] p0, double[] p1, double[] p2, double t) {
        double u = 1 - t;
        return new double[]{u * u * p0[0] + 2 * u * t * p1[0] + t * t * p2[0],
                u * u * p0[1] + 2 * u * t * p1[1] + t * t * p2[1],
                u * u * p0[2] + 2 * u * t * p1[2] + t * t * p2[2]};
    }

    // ---------------------------------------------------------------- companion

    private void companion(FxFrame f, Vec3 pos, double height, double bodyYaw, int rgb, double a, double time) {
        double fx = -Math.sin(bodyYaw), fz = Math.cos(bodyYaw);
        double rx = fz, rz = -fx;
        double side = companionSide.value().equals("Left") ? -1 : 1;
        double ax = pos.x + rx * side * 0.62 - fx * 0.28;
        double az = pos.z + rz * side * 0.62 - fz * 0.28;
        double ay = pos.y + height * 0.74 + Math.sin(time * 1.2) * 0.07;
        switch (companion.value()) {
            case "Wisp" -> {
                FxShapes.glowDot(f, ax, ay, az, 0.055, rgb, a, true, true);
                FxShapes.billboardRing(f, ax, ay, az, 0.10, 0.012, rgb, a * 0.5, true);
                for (int i = 1; i <= 5; i++) {
                    double t = i * 0.16;
                    double tx = ax + Math.sin(time * 2.2 - t * 3) * 0.10;
                    double ty = ay + Math.sin(time * 1.7 - t * 2) * 0.06 - t * 0.05;
                    double tz = az + Math.cos(time * 2.2 - t * 3) * 0.10;
                    FxShapes.softDot(f, tx, ty, tz, 0.022 - i * 0.002, rgb, a * (0.4 - i * 0.06), true);
                }
            }
            case "Ember" -> {
                double pulse = 0.7 + 0.3 * Math.sin(time * 4.5);
                FxShapes.glowDot(f, ax, ay, az, 0.04 * pulse, EffectBatch.mixRgb(rgb, 0xFFB060, 0.55), a, true, true);
                for (int i = 0; i < 4; i++) {
                    double p = (time * 0.7 + i * 0.25) % 1.0;
                    FxShapes.softDot(f, ax + Math.sin(time * 2 + i) * 0.04, ay + p * 0.24, az + Math.cos(time * 2 + i) * 0.04,
                            0.012 * (1 - p), EffectBatch.mixRgb(rgb, 0xFF8A40, 0.6), a * (1 - p) * 0.8, true);
                }
            }
            default -> {
                FxShapes.crystal(f, ax, ay, az, 0.045, 0.10, time * 1.4, rgb, a, true);
                FxShapes.tiltedRing(f, ax, ay, az, 0.3, 0.9, 0.2, 0.12, time * 1.5, 1, rgb, a * 0.7, 1.2F, true);
                FxShapes.glowDot(f, ax, ay, az, 0.02, rgb, a * 0.6, true, true);
            }
        }
    }

    // ---------------------------------------------------------------- aura geometry

    private void auraGeometry(FxFrame f, Vec3 pos, double height, int rgb, double a, double time) {
        double x = pos.x, z = pos.z;
        int seg = f.quality.circleSegments();
        switch (aura.value()) {
            case "Shroud" -> {
                for (int i = 0; i < 3; i++) {
                    double base = time * (0.4 + i * 0.15) + i * 2.1;
                    double h = pos.y + 0.25 + i * 0.42 + Math.sin(time * 0.8 + i) * 0.06;
                    double[] n = {Math.cos(base * 0.3) * 0.35, 0.94, Math.sin(base * 0.3) * 0.35};
                    FxShapes.tiltedRing(f, x, h, z, n[0], n[1], n[2], 0.62, base, 1, rgb, a * 0.22, 1.1F, true);
                }
            }
            case "Radiance" -> {
                int rays = 10;
                for (int i = 0; i < rays; i++) {
                    double ang = time * 0.25 + i * Math.PI * 2 / rays;
                    double ca = Math.cos(ang), sa = Math.sin(ang);
                    double r0 = 0.34, r1 = 0.55 + 0.08 * Math.sin(time * 1.7 + i);
                    f.batch.line(x + ca * r0 - f.camX, pos.y + 0.1 - f.camY, z + sa * r0 - f.camZ,
                            x + ca * r1 - f.camX, pos.y + height + 0.55 - f.camY, z + sa * r1 - f.camZ,
                            FxShapes.argb(rgb, a * 0.14), 1.6F, true);
                }
                f.batch.band(x - f.camX, pos.y + 0.04 - f.camY, z - f.camZ, 0.28, 0.44, seg,
                        FxShapes.argb(rgb, a * 0.12), FxShapes.argb(rgb, 0), true);
            }
            default -> {
                f.batch.ring(x - f.camX, pos.y + 0.05 - f.camY, z - f.camZ, 0.42, seg,
                        FxShapes.argb(rgb, a * 0.10), 1.2F, true, 0.5, time * 0.6);
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private static double[] orbitPoint(double[] normal, double radius, double angle) {
        double nx = normal[0], ny = normal[1], nz = normal[2];
        double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= nl; ny /= nl; nz /= nl;
        double ux, uy, uz;
        if (Math.abs(ny) < 0.9) { ux = nz; uy = 0; uz = -nx; } else { ux = 0; uy = -nz; uz = ny; }
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul; uy /= ul; uz /= ul;
        double vx = ny * uz - nz * uy, vy = nz * ux - nx * uz, vz = nx * uy - ny * ux;
        double ca = Math.cos(angle) * radius, sa = Math.sin(angle) * radius;
        return new double[]{ux * ca + vx * sa, uy * ca + vy * sa, uz * ca + vz * sa};
    }

    private static void line(FxFrame f, double[] a, double[] b, int color, float width) {
        f.batch.line(a[0] - f.camX, a[1] - f.camY, a[2] - f.camZ, b[0] - f.camX, b[1] - f.camY, b[2] - f.camZ, color, width, true);
    }

    private static void glowLine(FxFrame f, double[] a, double[] b, int color, float width) {
        f.batch.glowLine(a[0] - f.camX, a[1] - f.camY, a[2] - f.camZ, b[0] - f.camX, b[1] - f.camY, b[2] - f.camZ, color, width, true);
    }

    private static void quad(FxFrame f, double[] a, double[] b, double[] c, double[] d, int ca, int cb, int cc, int cd) {
        f.batch.quad(a[0] - f.camX, a[1] - f.camY, a[2] - f.camZ, b[0] - f.camX, b[1] - f.camY, b[2] - f.camZ,
                c[0] - f.camX, c[1] - f.camY, c[2] - f.camZ, d[0] - f.camX, d[1] - f.camY, d[2] - f.camZ,
                ca, cb, cc, cd, true);
    }

    private static final class Track {
        Vec3 last;
        boolean onGround;
        boolean left;
        double stride;

        Track(Vec3 last, boolean onGround) { this.last = last; this.onGround = onGround; }
    }
}
