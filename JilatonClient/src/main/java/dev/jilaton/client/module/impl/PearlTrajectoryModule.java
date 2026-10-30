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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Predicts the flight of ender pearls. A held pearl is simulated from the player's eye with vanilla
 * gravity, air drag and water drag, and every thrown pearl gets a fading trail plus a dashed remainder of
 * its arc. Landings are marked with a pulsing ring on the hit face, a block outline, or nothing.
 */
public final class PearlTrajectoryModule extends ClientModule implements FxEmitter {
    private static final double PEARL_POWER = 1.5;
    private static final double SPAWN_EYE_OFFSET = 0.1;
    private static final double GRAVITY = 0.03;
    private static final double AIR_DRAG = 0.99;
    private static final double WATER_DRAG = 0.8;
    private static final int MAX_TICKS = 300;
    private static final double ENTITY_MARGIN = 0.3;
    private static final double PIECE_LENGTH = 0.45;
    private static final double FADE_IN_DISTANCE = 1.8;
    private static final double POP_IN_SECONDS = 0.22;
    private static final double WAVE_PERIOD = 1.4;
    private static final long SAMPLE_INTERVAL = 50_000_000L;
    private static final int MAX_TRACKED = 6;
    private static final int MAX_TRAIL = 200;
    private static final String LANDING_RING = "Ring";
    private static final String LANDING_BOX = "Box";
    private static final String LANDING_NONE = "None";

    private final BooleanSetting predict = addSetting(new BooleanSetting("predict", "Predict held pearl", true));
    private final BooleanSetting trail = addSetting(new BooleanSetting("trail", "Trail", true));
    private final BooleanSetting others = addSetting(new BooleanSetting("others", "Others' pearls", false));
    private final ChoiceSetting landing = addSetting(new ChoiceSetting("landing", "Landing",
            List.of(LANDING_RING, LANDING_BOX, LANDING_NONE), LANDING_RING));
    private final BooleanSetting info = addSetting(new BooleanSetting("info", "Landing info", true));
    private final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Glow", true));
    private final BooleanSetting gradient = addSetting(new BooleanSetting("gradient", "Gradient", true));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting startColor = addSetting(EffectPalette.hexSetting("start_color", "Start", "E8E8E8"));
    private final TextSetting endColor = addSetting(EffectPalette.hexSetting("end_color", "End", "8FA8E8"));
    private final TextSetting hitColor = addSetting(EffectPalette.hexSetting("hit_color", "Hit", "E86A6A"));
    private final NumberSetting lineWidth = addSetting(new NumberSetting("line_width", "Line width", 0.5, 5, 0.5, 2));

    private final Map<Integer, Deque<Vec3>> trails = new HashMap<>();
    private final Map<Integer, Sim> rest = new HashMap<>();
    private final Set<Integer> seen = new HashSet<>();
    private Level bound;
    private long lastSample;
    private long predictedSince = -1L;
    private Sim predicted;

    public PearlTrajectoryModule() {
        super("pearl_trajectory", "Pearl Trajectory",
                "Predicts where a held pearl lands and previews the flight of thrown pearls.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        reset();
    }

    private void reset() {
        trails.clear();
        rest.clear();
        seen.clear();
        bound = null;
        predicted = null;
        predictedSince = -1L;
        lastSample = 0L;
    }

    @Override
    public void emit(FxFrame f) {
        ClientLevel level = f.client.level;
        Player player = f.client.player;
        if (level == null || player == null) {
            if (bound != null) reset();
            return;
        }
        if (level != bound) {
            reset();
            bound = level;
        }

        double time = f.seconds() * (f.reducedMotion ? 0.35 : 1);
        float width = (float) this.lineWidth.value();
        int start = EffectPalette.resolve(this.colorMode.value(), f.accent, this.startColor.value());
        int end = this.gradient.value() ? EffectPalette.resolve(this.colorMode.value(), f.accent, this.endColor.value()) : start;
        int hit = EffectPalette.resolve(this.colorMode.value(), f.accent, this.hitColor.value());
        boolean resample = f.now - this.lastSample >= SAMPLE_INTERVAL;
        if (resample) this.lastSample = f.now;

        Vec3 eye = player.getEyePosition(f.partial);
        if (this.predict.value() && holdsPearl(player)) {
            if (this.predictedSince < 0L) this.predictedSince = f.now;
            if (resample || this.predicted == null) this.predicted = simulate(level, player, f.partial);
            Sim prediction = this.predicted;
            if (prediction != null) {
                double pop = easeOutBack(Math.min(1.0, (f.now - this.predictedSince) / 1.0E9 / POP_IN_SECONDS));
                drawPath(f, prediction.points, width, true, false, 1.0, start, end);
                drawLanding(f, prediction, pop, time, width, 1.0, start, end, hit);
                addLabel(f, prediction, eye, null, start);
            }
        } else {
            this.predictedSince = -1L;
            this.predicted = null;
        }

        this.seen.clear();
        int tracked = 0;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof ThrownEnderpearl pearl) || !tracked(pearl, player)) continue;
            int id = pearl.getId();
            this.seen.add(id);
            Vec3 head = pearl.getPosition(f.partial);
            if (this.trail.value()) {
                Deque<Vec3> history = this.trails.computeIfAbsent(id, ignored -> new ArrayDeque<>());
                if (resample) {
                    Vec3 last = history.peekLast();
                    if (last == null || last.distanceToSqr(head) > 1.0E-4) history.addLast(head);
                    while (history.size() > MAX_TRAIL) history.removeFirst();
                }
                drawTrail(f, history, head, width, start);
            }
            Sim sim = this.rest.get(id);
            if (sim == null || resample) {
                sim = simulate(level, pearl);
                this.rest.put(id, sim);
            }
            List<Vec3> ahead = new ArrayList<>(sim.points);
            ahead.set(0, head);
            drawPath(f, ahead, width * 0.8F, false, true, 0.7, start, end);
            drawLanding(f, sim, 1.0, time, width, 0.75, start, end, hit);
            Entity owner = pearl.getOwner();
            String ownerName = owner == null || owner == player ? null : owner.getName().getString();
            addLabel(f, sim, eye, ownerName, start);
            if (++tracked >= MAX_TRACKED) break;
        }
        this.trails.keySet().retainAll(this.seen);
        this.rest.keySet().retainAll(this.seen);
    }

    private static boolean holdsPearl(Player player) {
        return player.getMainHandItem().is(Items.ENDER_PEARL) || player.getOffhandItem().is(Items.ENDER_PEARL);
    }

    private boolean tracked(ThrownEnderpearl pearl, Player self) {
        if (pearl.isRemoved()) return false;
        Entity owner = pearl.getOwner();
        return owner == null || owner == self || this.others.value();
    }

    private static Sim simulate(Level level, Player player, float partial) {
        Vec3 eye = player.getEyePosition(partial);
        Vec3 start = new Vec3(eye.x, eye.y - SPAWN_EYE_OFFSET, eye.z);
        Vec3 aim = player.getViewVector(partial).scale(PEARL_POWER);
        Vec3 motion = player.getDeltaMovement();
        Vec3 velocity = aim.add(motion.x, player.onGround() ? 0 : motion.y, motion.z);
        return simulate(level, start, velocity, player, null);
    }

    private static Sim simulate(Level level, ThrownEnderpearl pearl) {
        return simulate(level, pearl.position(), pearl.getDeltaMovement(), pearl, pearl.getOwner());
    }

    /**
     * One pearl tick is gravity, then drag, then a move; the block clip and the entity sweep run against
     * that move so the landing point is the first surface or target the pearl would actually touch.
     */
    private static Sim simulate(Level level, Vec3 start, Vec3 velocity, Entity ignore, Entity alsoIgnore) {
        List<Vec3> points = new ArrayList<>(64);
        points.add(start);
        Vec3 pos = start;
        BlockHitResult blockHit = null;
        Entity entityHit = null;
        int ticks = 0;
        while (ticks < MAX_TICKS) {
            ticks++;
            double drag = isInWater(level, pos) ? WATER_DRAG : AIR_DRAG;
            velocity = velocity.add(0, -GRAVITY, 0).scale(drag);
            Vec3 next = pos.add(velocity);
            BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ignore));
            Vec3 end = hit.getType() == HitResult.Type.MISS ? next : hit.getLocation();
            Entity entity = firstEntityOnSegment(level, pos, end, ignore, alsoIgnore);
            if (entity != null) {
                Vec3 at = entity.getBoundingBox().inflate(ENTITY_MARGIN).clip(pos, end).orElse(end);
                points.add(at);
                entityHit = entity;
                break;
            }
            if (hit.getType() != HitResult.Type.MISS) {
                points.add(end);
                blockHit = hit;
                break;
            }
            pos = next;
            points.add(pos);
            if (pos.y < level.getMinY() - 64) break;
        }
        return new Sim(List.copyOf(points), points.getLast(), blockHit, entityHit, ticks);
    }

    private static Entity firstEntityOnSegment(Level level, Vec3 from, Vec3 to, Entity ignore, Entity alsoIgnore) {
        AABB sweep = new AABB(from, to).inflate(1.0);
        Entity closest = null;
        double closestSq = Double.MAX_VALUE;
        for (Entity entity : level.getEntities(ignore, sweep,
                candidate -> candidate instanceof LivingEntity living && living.isAlive() && !(candidate instanceof Player))) {
            if (entity == alsoIgnore) continue;
            Vec3 hit = entity.getBoundingBox().inflate(ENTITY_MARGIN).clip(from, to).orElse(null);
            if (hit == null) continue;
            double distanceSq = from.distanceToSqr(hit);
            if (distanceSq < closestSq) {
                closestSq = distanceSq;
                closest = entity;
            }
        }
        return closest;
    }

    private static boolean isInWater(Level level, Vec3 pos) {
        return level.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER);
    }

    private void drawPath(FxFrame f, List<Vec3> points, float width, boolean fadeIn, boolean dashed,
                          double alphaMul, int start, int end) {
        if (points.size() < 2) return;
        double total = 0;
        for (int i = 1; i < points.size(); i++) total += points.get(i - 1).distanceTo(points.get(i));
        if (total <= 1.0E-6) return;
        boolean glowing = this.glow.value();
        double travelled = 0;
        int piece = 0;
        for (int i = 1; i < points.size(); i++) {
            Vec3 a = points.get(i - 1);
            Vec3 b = points.get(i);
            double length = a.distanceTo(b);
            int pieces = Math.max(1, (int) Math.ceil(length / PIECE_LENGTH));
            for (int p = 0; p < pieces; p++) {
                if (!dashed || (piece & 1) == 0) {
                    double t0 = (double) p / pieces;
                    double t1 = (double) (p + 1) / pieces;
                    double mid = travelled + length * (t0 + t1) * 0.5;
                    double alpha = alphaMul;
                    if (fadeIn) alpha *= smoothstep(mid / FADE_IN_DISTANCE);
                    if (alpha >= 0.02) {
                        int color = EffectBatch.withAlpha(EffectBatch.mixRgb(start, end, (float) (mid / total)), alpha);
                        Vec3 from = a.lerp(b, t0);
                        Vec3 to = a.lerp(b, t1);
                        if (glowing) {
                            f.batch.glowLine(from.x, from.y, from.z, to.x, to.y, to.z, color, width, false);
                        } else {
                            f.batch.line(from.x, from.y, from.z, to.x, to.y, to.z, color, width, false);
                        }
                    }
                }
                piece++;
            }
            travelled += length;
        }
    }

    private void drawTrail(FxFrame f, Deque<Vec3> history, Vec3 head, float width, int rgb) {
        if (history.isEmpty()) return;
        boolean glowing = this.glow.value();
        int total = history.size();
        int segment = 0;
        Vec3 previous = null;
        for (Vec3 point : history) {
            if (previous != null) {
                segment++;
                drawTrailSegment(f, previous, point, segment / (double) total, width, rgb, glowing);
            }
            previous = point;
        }
        if (previous != null) drawTrailSegment(f, previous, head, 1.0, width, rgb, glowing);
    }

    private static void drawTrailSegment(FxFrame f, Vec3 from, Vec3 to, double t, float width, int rgb, boolean glowing) {
        int color = EffectBatch.withAlpha(rgb, 0.15 + 0.85 * t);
        if (glowing) {
            f.batch.glowLine(from.x, from.y, from.z, to.x, to.y, to.z, color, width, false);
        } else {
            f.batch.line(from.x, from.y, from.z, to.x, to.y, to.z, color, width, false);
        }
    }

    private void drawLanding(FxFrame f, Sim sim, double scale, double time, float width, double alphaMul,
                             int start, int end, int hitRgb) {
        if (sim.entityHit != null) {
            drawBox(f, sim.entityHit.getBoundingBox().inflate(0.05), hitRgb, alphaMul, width, 0.12 * alphaMul);
            return;
        }
        BlockHitResult hit = sim.blockHit;
        if (hit == null || LANDING_NONE.equals(this.landing.value())) return;
        ClientLevel level = f.client.level;
        if (level == null) return;
        int color = this.gradient.value() ? end : start;
        AABB box = blockBox(level, hit.getBlockPos()).inflate(0.002);
        if (LANDING_BOX.equals(this.landing.value())) {
            drawBox(f, box, color, alphaMul, width, 0.16 * alphaMul);
            return;
        }
        Vec3 normal = hit.getDirection().getUnitVec3();
        Vec3 centre = sim.landing.add(normal.scale(0.02));
        double pulse = 1.0 + 0.05 * Math.sin(time * 5.0);
        FxShapes.tiltedRing(f, centre.x, centre.y, centre.z, normal.x, normal.y, normal.z,
                0.38 * scale * pulse, 0, 1, color, alphaMul, width, false);
        drawCross(f, centre, normal, 0.09 * scale, color, width, alphaMul);
        double phase = (time % WAVE_PERIOD) / WAVE_PERIOD;
        double wave = lerp(0.3, 0.95, easeOutCubic(phase)) * scale;
        FxShapes.tiltedRing(f, centre.x, centre.y, centre.z, normal.x, normal.y, normal.z,
                wave, 0, 1, color, alphaMul * 0.75 * (1.0 - phase), width * 0.8F, false);
        if (this.glow.value()) {
            FxShapes.softDot(f, centre.x, centre.y, centre.z, 0.12 * scale, color, alphaMul * 0.3, false);
        }
    }

    private static AABB blockBox(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
    }

    private static void drawBox(FxFrame f, AABB box, int rgb, double edgeAlpha, float width, double fillAlpha) {
        double x0 = box.minX, y0 = box.minY, z0 = box.minZ, x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
        int edge = EffectBatch.withAlpha(rgb, edgeAlpha);
        f.batch.line(x0, y0, z0, x1, y0, z0, edge, width, false);
        f.batch.line(x1, y0, z0, x1, y0, z1, edge, width, false);
        f.batch.line(x1, y0, z1, x0, y0, z1, edge, width, false);
        f.batch.line(x0, y0, z1, x0, y0, z0, edge, width, false);
        f.batch.line(x0, y1, z0, x1, y1, z0, edge, width, false);
        f.batch.line(x1, y1, z0, x1, y1, z1, edge, width, false);
        f.batch.line(x1, y1, z1, x0, y1, z1, edge, width, false);
        f.batch.line(x0, y1, z1, x0, y1, z0, edge, width, false);
        f.batch.line(x0, y0, z0, x0, y1, z0, edge, width, false);
        f.batch.line(x1, y0, z0, x1, y1, z0, edge, width, false);
        f.batch.line(x1, y0, z1, x1, y1, z1, edge, width, false);
        f.batch.line(x0, y0, z1, x0, y1, z1, edge, width, false);
        if (fillAlpha <= 0.004) return;
        int fill = EffectBatch.withAlpha(rgb, fillAlpha);
        f.batch.quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, fill, fill, fill, fill, false);
        f.batch.quad(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, fill, fill, fill, fill, false);
        f.batch.quad(x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, fill, fill, fill, fill, false);
        f.batch.quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, fill, fill, fill, fill, false);
        f.batch.quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, fill, fill, fill, fill, false);
        f.batch.quad(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, fill, fill, fill, fill, false);
    }

    private static void drawCross(FxFrame f, Vec3 centre, Vec3 normal, double half, int rgb, float width, double alpha) {
        Vec3[] basis = planeBasis(normal);
        int color = EffectBatch.withAlpha(rgb, alpha);
        Vec3 u = basis[0].scale(half);
        Vec3 v = basis[1].scale(half);
        f.batch.line(centre.x - u.x, centre.y - u.y, centre.z - u.z, centre.x + u.x, centre.y + u.y, centre.z + u.z,
                color, width, false);
        f.batch.line(centre.x - v.x, centre.y - v.y, centre.z - v.z, centre.x + v.x, centre.y + v.y, centre.z + v.z,
                color, width, false);
    }

    private static Vec3[] planeBasis(Vec3 normal) {
        Vec3 u = Math.abs(normal.y) > 0.9 ? new Vec3(1, 0, 0) : normal.cross(new Vec3(0, 1, 0)).normalize();
        return new Vec3[]{u, normal.cross(u).normalize()};
    }

    private void addLabel(FxFrame f, Sim sim, Vec3 eye, String owner, int rgb) {
        if (!this.info.value() || !sim.landed()) return;
        String flight = String.format(Locale.ROOT, "%.1fs", sim.ticks / 20.0);
        String primary = String.format(Locale.ROOT, "%.1fm", eye.distanceTo(sim.landing));
        String secondary = sim.entityHit != null
                ? sim.entityHit.getName().getString() + "  \u00b7  " + flight
                : owner != null ? owner + "  \u00b7  " + flight : flight;
        f.text(new FxFrame.WorldText(sim.landing.x, sim.landing.y + 0.35, sim.landing.z,
                List.of(new FxFrame.Run(primary, FxShapes.argb(EffectBatch.mixRgb(rgb, 0xFFFFFF, 0.5), 0.95)),
                        new FxFrame.Run("  " + secondary, FxShapes.argb(0xB4B4B4, 0.8))),
                0.02F, FxShapes.argb(0x000000, 0.35), 0, true, 0, true));
    }

    private static float smoothstep(double t) {
        double clamped = Math.max(0, Math.min(1, t));
        return (float) (clamped * clamped * (3 - 2 * clamped));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double easeOutCubic(double t) {
        double inv = 1 - t;
        return 1 - inv * inv * inv;
    }

    private static double easeOutBack(double t) {
        double c1 = 1.70158, c3 = c1 + 1, u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }

    private record Sim(List<Vec3> points, Vec3 landing, BlockHitResult blockHit, Entity entityHit, int ticks) {
        boolean landed() {
            return this.blockHit != null || this.entityHit != null;
        }
    }
}
