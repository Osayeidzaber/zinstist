package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Glowing orbs orbit the player, dodging terrain and trailing styled light. Physics are stepped in
 * reference-tick units from {@link FxFrame#dt}, so motion is frame-rate independent. Geometry is drawn
 * through the shared {@link EffectBatch} with original Jilaton primitives.
 */
public final class FloatingRelicsModule extends ClientModule implements FxEmitter {
    private static final int MAX_RELICS = 32;
    private static final int MAX_TRAIL = 96;
    private static final long SAMPLE_INTERVAL = 33_000_000L;
    private static final double REFERENCE_TICKS = 20.0;
    private static final double MAX_SPEED_SQR = 0.9 * 0.9;
    private static final long STAGGER = 180_000_000L;
    private static final double APPEAR = 0.6;

    private static final double GRAVITY = 0.016;
    private static final double BUOYANCY = 0.045;
    private static final double AIR_DRAG = 0.982;
    private static final double GROUND_GRIP = 0.86;
    private static final double ORBIT_PULL = 0.012;
    private static final double SWIRL = 0.0025;
    private static final double FOLLOW = 0.035;
    private static final double WANDER = 0.003;
    private static final double LEASH = 3.0;

    private static final List<String> STYLES = List.of("Comet", "Fireball", "Rainbow", "Neon", "Electric", "Sakura", "Void", "Aurora");

    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Style", STYLES, "Comet"));
    private final ChoiceSetting palette = addSetting(new ChoiceSetting("palette", "Palette", List.of("Style", "Theme", "Custom"), "Style"));
    private final TextSetting tint = addSetting(EffectPalette.hexSetting("tint", "Custom tint", "C8C8C8"));
    private final NumberSetting count = addSetting(new NumberSetting("count", "Count", 1, MAX_RELICS, 1, 8));
    private final NumberSetting size = addSetting(new NumberSetting("size", "Size", 0.15, 0.75, 0.05, 0.3));
    private final NumberSetting spread = addSetting(new NumberSetting("spread", "Orbit radius", 1.2, 6, 0.1, 2.6));
    private final NumberSetting trail = addSetting(new NumberSetting("trail", "Trail length (seconds)", 0.2, 3, 0.1, 1.0));
    private final NumberSetting drift = addSetting(new NumberSetting("drift", "Drift", 0, 2.5, 0.1, 0.5));
    private final NumberSetting bounce = addSetting(new NumberSetting("bounce", "Bounce", 0.1, 0.95, 0.05, 0.6));
    private final BooleanSetting blockBounce = addSetting(new BooleanSetting("block_bounce", "Bounce off blocks", true));
    private final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Through walls", false));

    private final Relic[] relics = new Relic[MAX_RELICS];
    private final Random random = new Random(0x4A17A0L);
    private int live;
    private double ageTicks;
    private Object boundLevel;

    private double px, py, pz;
    private double moveX, moveZ;
    private float playerHeight;
    private double restitution;
    private double extentBase;
    private long now;
    private boolean walls;

    public FloatingRelicsModule() {
        super("floating_relics", "Floating Relics", "Glowing orbs orbit you, bounce off terrain and trail styled light.", ModuleCategory.RENDER);
        for (int i = 0; i < MAX_RELICS; i++) relics[i] = new Relic();
    }

    @Override
    protected void onDisable(Minecraft client) {
        reset();
    }

    private void reset() {
        live = 0;
        ageTicks = 0;
        boundLevel = null;
        for (Relic relic : relics) {
            relic.placed = false;
            relic.head = 0;
            relic.filled = 0;
        }
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        Player player = client.player;
        Level level = client.level;
        if (player == null || level == null || player.isSpectator()) {
            reset();
            return;
        }
        if (level != boundLevel) {
            boundLevel = level;
            live = 0;
        }
        now = f.now;
        double dt = Double.isFinite(f.dt) ? Math.min(0.1, Math.max(0, f.dt)) : 0;
        double ticks = dt * REFERENCE_TICKS;
        ageTicks += ticks;

        Vec3 motion = player.getDeltaMovement();
        px = player.getX();
        py = player.getY();
        pz = player.getZ();
        playerHeight = player.getBbHeight();
        moveX = motion.x;
        moveZ = motion.z;
        restitution = bounce.value();
        extentBase = size.value() * 0.5;
        walls = throughWalls.value();

        int want = (int) Math.round(count.value());
        want = Math.max(1, Math.min(MAX_RELICS, want));
        if (want < live) {
            for (int i = want; i < live; i++) relics[i].placed = false;
            live = want;
        } else if (want > live) {
            for (int i = live; i < want; i++) wake(relics[i], (i - live) * STAGGER);
            live = want;
        }

        if (ticks > 1e-5) {
            for (int i = 0; i < live; i++) step(relics[i], level, ticks);
            for (int i = 0; i < live; i++) {
                for (int j = i + 1; j < live; j++) knock(relics[i], relics[j]);
            }
            for (int i = 0; i < live; i++) if (relics[i].placed) remember(relics[i]);
        }

        int index = Math.max(0, STYLES.indexOf(style.value()));
        double intensity = Math.min(1, Math.max(0, f.intensity));
        if (intensity <= 0.01) return;
        int samples = (int) Math.round(trail.value() * 30);
        double time = f.seconds();
        for (int i = 0; i < live; i++) {
            Relic relic = relics[i];
            if (!relic.placed) continue;
            double since = (now - relic.placedAt) / 1e9;
            double grow = appear(since / APPEAR);
            double extent = extentBase * relic.grain * grow;
            if (extent < 0.004) continue;
            int n = Math.min(relic.filled, samples + 1);
            if (n >= 2) drawTrail(f, relic, index, n, extent, time, intensity);
            drawHead(f, index, relic.x, relic.y, relic.z, extent, time, relic.phase, intensity);
        }
    }

    private void wake(Relic relic, long delay) {
        relic.placed = false;
        relic.wakeAt = now + delay;
        relic.orbit = 0.55F + 0.45F * random.nextFloat();
        relic.lift = random.nextFloat();
        relic.grain = 0.82F + 0.3F * random.nextFloat();
        relic.phase = random.nextFloat() * (float) (Math.PI * 2);
        relic.head = 0;
        relic.filled = 0;
        relic.lastSample = 0;
    }

    private void place(Relic relic) {
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = spread.value() * relic.orbit;
        relic.x = px + Math.cos(angle) * radius;
        relic.y = py + 0.15;
        relic.z = pz + Math.sin(angle) * radius;
        relic.vx = -Math.sin(angle) * 0.03;
        relic.vy = 0.14 + 0.08 * random.nextDouble();
        relic.vz = Math.cos(angle) * 0.03;
        relic.placed = true;
        relic.placedAt = now;
        relic.head = 0;
        relic.filled = 0;
        relic.lastSample = 0;
    }

    private void step(Relic r, Level level, double ticks) {
        if (!r.placed) {
            if (now < r.wakeAt) return;
            place(r);
        }
        double dx = r.x - px, dz = r.z - pz;
        double flat = Math.sqrt(dx * dx + dz * dz);
        double hover = py + 0.35 + r.lift * (playerHeight + 0.2) + 0.16 * Math.sin((ageTicks + r.phase * 16.0) * 0.045);
        double leash = spread.value() * LEASH + 8.0;
        if (flat > leash || Math.abs(r.y - hover) > leash) {
            place(r);
            return;
        }
        double nx = flat > 1e-4 ? dx / flat : 1.0;
        double nz = flat > 1e-4 ? dz / flat : 0.0;
        double pull = (spread.value() * r.orbit - flat) * ORBIT_PULL;
        double swirl = drift.value() * SWIRL;
        double wander = (ageTicks + r.phase * 20.0) * 0.06;
        r.vx += (nx * pull - nz * swirl + moveX * FOLLOW) * ticks;
        r.vz += (nz * pull + nx * swirl + moveZ * FOLLOW) * ticks;
        r.vx += Math.sin(wander) * WANDER * ticks;
        r.vz += Math.sin(wander * 1.31) * WANDER * ticks;
        r.vy -= GRAVITY * ticks;
        double sink = hover - r.y;
        if (sink > 0) r.vy += Math.min(sink, 1.5) * BUOYANCY * ticks;
        double drag = Math.pow(AIR_DRAG, ticks);
        r.vx *= drag;
        r.vy *= drag;
        r.vz *= drag;
        double speed = r.vx * r.vx + r.vy * r.vy + r.vz * r.vz;
        if (speed > MAX_SPEED_SQR) {
            double k = 0.9 / Math.sqrt(speed);
            r.vx *= k;
            r.vy *= k;
            r.vz *= k;
        }
        double stepX = r.vx * ticks, stepY = r.vy * ticks, stepZ = r.vz * ticks;
        boolean collide = blockBounce.value();
        double half = size.value() * r.grain * 0.5;

        double next = r.x + stepX;
        if (collide && solid(level, next + Math.copySign(half, stepX), r.y, r.z)) r.vx = -r.vx * restitution;
        else r.x = next;

        next = r.y + stepY;
        if (collide && solid(level, r.x, next + Math.copySign(half, stepY), r.z)) {
            if (r.vy < 0) {
                r.vx *= GROUND_GRIP;
                r.vz *= GROUND_GRIP;
            }
            r.vy = -r.vy * restitution;
        } else r.y = next;

        next = r.z + stepZ;
        if (collide && solid(level, r.x, r.y, next + Math.copySign(half, stepZ))) r.vz = -r.vz * restitution;
        else r.z = next;

        if (collide && solid(level, r.x, r.y, r.z)) {
            r.y += 0.2;
            r.vy = Math.max(r.vy, 0.08);
        }
    }

    private void knock(Relic a, Relic b) {
        if (!a.placed || !b.placed) return;
        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        double d2 = dx * dx + dy * dy + dz * dz;
        double reach = size.value() * (a.grain + b.grain) * 0.5 * 0.95;
        if (d2 >= reach * reach || d2 < 1e-8) return;
        double d = Math.sqrt(d2);
        double nx = dx / d, ny = dy / d, nz = dz / d;
        double push = (reach - d) * 0.5;
        a.x -= nx * push;
        a.y -= ny * push;
        a.z -= nz * push;
        b.x += nx * push;
        b.y += ny * push;
        b.z += nz * push;
        double closing = (a.vx - b.vx) * nx + (a.vy - b.vy) * ny + (a.vz - b.vz) * nz;
        if (closing <= 0) return;
        double impulse = closing * (1 + restitution) * 0.5;
        a.vx -= impulse * nx;
        a.vy -= impulse * ny;
        a.vz -= impulse * nz;
        b.vx += impulse * nx;
        b.vy += impulse * ny;
        b.vz += impulse * nz;
    }

    private static boolean solid(Level level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        AABB box = state.getCollisionShape(level, pos).bounds();
        if (box.getXsize() <= 0 || box.getYsize() <= 0 || box.getZsize() <= 0) return false;
        return box.contains(x - pos.getX(), y - pos.getY(), z - pos.getZ());
    }

    private void remember(Relic relic) {
        if (relic.filled > 0 && now - relic.lastSample < SAMPLE_INTERVAL) return;
        relic.history[relic.head * 3] = relic.x;
        relic.history[relic.head * 3 + 1] = relic.y;
        relic.history[relic.head * 3 + 2] = relic.z;
        relic.head = (relic.head + 1) % MAX_TRAIL;
        relic.filled = Math.min(MAX_TRAIL, relic.filled + 1);
        relic.lastSample = now;
    }

    private static double history(Relic relic, int offset, int axis) {
        int index = Math.floorMod(relic.head - offset - 1, MAX_TRAIL);
        return relic.history[index * 3 + axis];
    }

    // ---- drawing -------------------------------------------------------------------------------

    private void drawTrail(FxFrame f, Relic relic, int look, int n, double extent, double time, double intensity) {
        double width = extent * 0.7;
        for (int i = 0; i < n - 1; i++) {
            double t = (double) (i + 1) / (n - 1);
            double tb = (double) i / (n - 1);
            double ax = history(relic, n - 1 - i, 0) - f.camX, ay = history(relic, n - 1 - i, 1) - f.camY, az = history(relic, n - 1 - i, 2) - f.camZ;
            double bx = history(relic, n - 2 - i, 0) - f.camX, by = history(relic, n - 2 - i, 1) - f.camY, bz = history(relic, n - 2 - i, 2) - f.camZ;
            int back = i;
            switch (look) {
                case 1 -> { // Fireball
                    if (back % 3 == 0) {
                        int col = base(f, EffectBatch.mixRgb(0xC83C00, 0xFFB040, t));
                        FxShapes.softDot(f, bx + f.camX, by + f.camY, bz + f.camZ, extent * (0.35 + 0.8 * t), col, 0.55 * t * t * intensity, walls);
                    }
                    if (back % 4 == 0) {
                        long seed = (long) relic.seed * 7919L + back * 104729L + (long) (time * 6.0);
                        double sx = bx + FxShapes.hash(seed, 1) * extent * 3.2;
                        double sy = by + FxShapes.hash(seed, 2) * extent * 2.0 + (1 - t) * extent * 2.4;
                        double sz = bz + FxShapes.hash(seed, 3) * extent * 3.2;
                        FxShapes.softDot(f, sx + f.camX, sy + f.camY, sz + f.camZ, extent * 0.1, 0xFFF1C0, 0.9 * t * intensity, walls);
                    }
                }
                case 2 -> { // Rainbow
                    int col = hsv(t * 0.85 - time * 0.25, 0.85, 1.0);
                    f.batch.glowLine(ax, ay, az, bx, by, bz, EffectBatch.withAlpha(col, 0.88 * t * intensity), (float) (width * t), walls);
                }
                case 3 -> { // Neon
                    int col = base(f, EffectBatch.mixRgb(0xFF3F72, 0x3FF2FF, t));
                    f.batch.glowLine(ax, ay, az, bx, by, bz, EffectBatch.withAlpha(col, 0.2 * t * intensity), (float) (width * 2.4 * t), walls);
                    f.batch.glowLine(ax, ay, az, bx, by, bz, EffectBatch.withAlpha(col, 0.78 * t * intensity), (float) (width * 0.8 * t), walls);
                    f.batch.line(ax, ay, az, bx, by, bz, EffectBatch.withAlpha(0xFFFFFF, 0.8 * t * intensity), (float) (width * 0.22), walls);
                }
                case 4 -> { // Electric
                    long seed = (long) relic.seed * 7919L + back * 104729L + (long) (time * 14.0);
                    double reachA = extent * 0.7 * (1 - tb * 0.5), reachB = extent * 0.7 * (1 - t * 0.5);
                    double fax = ax + FxShapes.hash(seed + 104729L, 1) * reachA;
                    double fay = ay + FxShapes.hash(seed + 104729L, 2) * reachA;
                    double faz = az + FxShapes.hash(seed + 104729L, 3) * reachA;
                    double fbx = bx + FxShapes.hash(seed, 1) * reachB;
                    double fby = by + FxShapes.hash(seed, 2) * reachB;
                    double fbz = bz + FxShapes.hash(seed, 3) * reachB;
                    f.batch.glowLine(fax, fay, faz, fbx, fby, fbz, EffectBatch.withAlpha(0x5FB0FF, 0.3 * t * intensity), (float) (width * 0.7), walls);
                    f.batch.line(fax, fay, faz, fbx, fby, fbz, EffectBatch.withAlpha(0xF0F8FF, 0.92 * t * intensity), (float) (width * 0.2), walls);
                }
                case 5 -> { // Sakura
                    if (back % 6 == 0) {
                        long seed = (long) relic.seed * 7919L + back * 104729L;
                        double fall = (1 - t) * extent * 3.0;
                        double sx = bx + FxShapes.hash(seed, 1) * extent * (1.4 - t);
                        double sz = bz + FxShapes.hash(seed, 3) * extent * (1.4 - t);
                        int col = base(f, EffectBatch.mixRgb(0xFFE4EE, 0xFF8FB8, t));
                        FxShapes.star(f, sx + f.camX, by - fall + f.camY, sz + f.camZ, extent * (0.45 + 0.45 * t),
                                time * 2.1 + back * 0.7, 5, col, 0.9 * t * intensity, walls);
                    }
                }
                case 6 -> { // Void
                    double dx = bx - ax, dy = by - ay, dz = bz - az;
                    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (length < 1e-9) continue;
                    dx /= length;
                    dy /= length;
                    dz /= length;
                    double ux = -dz, uy = 0, uz = dx;
                    double ul = Math.sqrt(ux * ux + uz * uz);
                    if (ul < 1e-6) {
                        ux = 1;
                        uz = 0;
                        ul = 1;
                    }
                    ux /= ul;
                    uz /= ul;
                    double vx = dy * uz - dz * uy, vy = dz * ux - dx * uz, vz = dx * uy - dy * ux;
                    double radius = extent * (0.9 - 0.5 * t);
                    for (int strand = 0; strand < 2; strand++) {
                        double angle = tb * 9 + time * 4 + strand * Math.PI;
                        double c = Math.cos(angle) * radius, s = Math.sin(angle) * radius;
                        double pax = ax + ux * c + vx * s, pay = ay + uy * c + vy * s, paz = az + uz * c + vz * s;
                        angle = t * 9 + time * 4 + strand * Math.PI;
                        c = Math.cos(angle) * radius;
                        s = Math.sin(angle) * radius;
                        double pbx = bx + ux * c + vx * s, pby = by + uy * c + vy * s, pbz = bz + uz * c + vz * s;
                        int col = base(f, strand == 0 ? 0xBE7CFF : 0xFF8FB8);
                        f.batch.glowLine(pax, pay, paz, pbx, pby, pbz, EffectBatch.withAlpha(col, 0.85 * t * intensity), (float) (width * 0.34), walls);
                    }
                }
                case 7 -> { // Aurora
                    int col = base(f, EffectBatch.mixRgb(0x2FA36B, 0xB06CFF, 0.5 + 0.5 * Math.sin(t * 6 + time * 1.5)));
                    double shimmer = 0.45 + 0.55 * Math.abs(Math.sin(i * 0.9 + time * 3.0));
                    double tallA = extent * 3.2 * tb * (0.7 + 0.3 * shimmer), tallB = extent * 3.2 * t * (0.7 + 0.3 * shimmer);
                    int footer = EffectBatch.withAlpha(col, 0.42 * t * shimmer * intensity);
                    int roof = EffectBatch.withAlpha(col, 0);
                    f.batch.quad(ax, ay, az, bx, by, bz, bx, by + tallB, bz, ax, ay + tallA, az, footer, footer, roof, roof, walls);
                    f.batch.quad(ax, ay + tallA, az, bx, by + tallB, bz, bx, by + tallB * 1.6, bz, ax, ay + tallA * 1.6, az,
                            EffectBatch.withAlpha(col, 0.12 * t * shimmer * intensity), EffectBatch.withAlpha(col, 0.12 * t * shimmer * intensity), roof, roof, walls);
                }
                default -> { // Comet
                    int col = base(f, EffectBatch.mixRgb(0x3F7BFF, 0xD6E1FF, t));
                    f.batch.glowLine(ax, ay, az, bx, by, bz, EffectBatch.withAlpha(col, 0.72 * t * t * intensity), (float) (width * t), walls);
                }
            }
        }
    }

    private void drawHead(FxFrame f, int look, double x, double y, double z, double extent, double time, float phase, double intensity) {
        switch (look) {
            case 1 -> { // Fireball
                FxShapes.glowDot(f, x, y, z, extent * 1.1, base(f, 0xFF8A3C), 0.85 * intensity, true, walls);
                FxShapes.softDot(f, x, y, z, extent * 0.45, base(f, 0xFFD38A), intensity, walls);
            }
            case 2, 3 -> FxShapes.glowDot(f, x, y, z, extent * 0.9, 0xFFFFFF, intensity, true, walls);
            case 4 -> { // Electric
                FxShapes.glowDot(f, x, y, z, extent * 1.05, base(f, 0x5FB0FF), 0.8 * intensity, true, walls);
                FxShapes.star(f, x, y, z, extent * 0.9, time * 5.0 + phase, 6, 0xFFFFFF, intensity, walls);
            }
            case 5 -> { // Sakura
                FxShapes.glowDot(f, x, y, z, extent * 0.8, base(f, 0xFFB7CE), 0.75 * intensity, true, walls);
                FxShapes.star(f, x, y, z, extent * 0.85, time * 1.6 + phase, 5, base(f, 0xFFD3DF), intensity, walls);
            }
            case 6 -> { // Void
                FxShapes.glowDot(f, x, y, z, extent * 1.2, base(f, 0x9B4DFF), 0.65 * intensity, true, walls);
                FxShapes.billboardRing(f, x, y, z, extent * 0.68, extent * 0.16, base(f, 0xD8C4FF), 0.9 * intensity, walls);
                FxShapes.softDot(f, x, y, z, extent * 0.5, 0x05060C, 0.85 * intensity, walls);
            }
            case 7 -> FxShapes.glowDot(f, x, y, z, extent * 0.85, base(f, 0x7FE0C0), 0.9 * intensity, true, walls);
            default -> { // Comet
                FxShapes.glowDot(f, x, y, z, extent * 1.15, base(f, 0x5C8DFF), 0.75 * intensity, true, walls);
                FxShapes.softDot(f, x, y, z, extent * 0.4, 0xFFFFFF, intensity, walls);
            }
        }
    }

    private int base(FxFrame f, int rgb) {
        return switch (palette.value()) {
            case "Theme" -> EffectBatch.mixRgb(rgb, f.accent, 0.65);
            case "Custom" -> EffectBatch.mixRgb(rgb, EffectPalette.resolve(EffectPalette.CUSTOM, f.accent, tint.value()), 0.8);
            default -> rgb;
        };
    }

    private static int hsv(double h, double s, double v) {
        double hue = (h % 1.0 + 1.0) % 1.0 * 6.0;
        int sector = (int) Math.floor(hue);
        double f = hue - sector;
        double p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        double r, g, b;
        switch (sector % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (int) Math.round(r * 255) << 16 | (int) Math.round(g * 255) << 8 | (int) Math.round(b * 255);
    }

    private static double appear(double t) {
        if (t >= 1) return 1;
        if (t <= 0) return 0;
        double inv = 1 - t;
        return 1 - inv * inv * inv + 0.22 * Math.sin(t * Math.PI) * inv;
    }

    private static final class Relic {
        double x, y, z;
        double vx, vy, vz;
        float orbit, lift, grain, phase;
        boolean placed;
        long wakeAt, placedAt, lastSample;
        final double[] history = new double[MAX_TRAIL * 3];
        int head, filled;
        final int seed;

        private Relic() {
            seed = System.identityHashCode(this) & 0x7FFFFFFF;
        }
    }
}
