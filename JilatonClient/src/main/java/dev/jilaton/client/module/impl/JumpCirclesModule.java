package dev.jilaton.client.module.impl;

import dev.jilaton.client.gui.Motion;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxParticles;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;

/**
 * Expanding ground shockwaves and ripples where players jump or land, driven by observed
 * ground-state changes and contoured to actual terrain elevation per vertex.
 */
public final class JumpCirclesModule extends ClientModule {
    private static final double TRACK_RANGE_SQR = 48 * 48;
    private final ChoiceSetting trigger = addSetting(new ChoiceSetting("trigger", "Trigger", List.of("Land", "Jump", "Both"), "Both"));
    private final ChoiceSetting targets = addSetting(new ChoiceSetting("targets", "Players", List.of("Self", "Others", "Everyone"), "Self"));
    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Style", List.of("Ring", "Double", "Ripple", "Shockwave"), "Ripple"));
    private final BooleanSetting terrainFollow = addSetting(new BooleanSetting("terrain_follow", "Follow terrain height", true));
    private final NumberSetting lifetime = addSetting(new NumberSetting("lifetime", "Lifetime (seconds)", 0.3, 3, 0.1, 1.1));
    private final NumberSetting radius = addSetting(new NumberSetting("radius", "Radius", 0.4, 4, 0.1, 1.4));
    private final NumberSetting width = addSetting(new NumberSetting("width", "Line width", 1, 5, 0.5, 2));
    private final BooleanSetting fill = addSetting(new BooleanSetting("fill", "Feathered fill", true));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "F2F2F2"));
    private final NumberSetting limit = addSetting(new NumberSetting("limit", "Max circles", 4, 64, 1, 24));
    private final ArrayDeque<Circle> circles = new ArrayDeque<>();
    private final Map<Integer, Boolean> grounded = new HashMap<>();
    private final Map<Integer, Long> lastEvent = new HashMap<>();
    private final Map<Integer, Long> landingGrace = new HashMap<>();
    private Object boundLevel;

    public JumpCirclesModule() {
        super("jump_circles", "JumpCircles", "Expanding ground ripples when players jump or land.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        circles.clear();
        grounded.clear();
        lastEvent.clear();
        landingGrace.clear();
        boundLevel = null;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.level == null || client.player == null) {
            onDisable(client);
            return;
        }
        if (client.level != boundLevel) {
            circles.clear();
            grounded.clear();
            lastEvent.clear();
            landingGrace.clear();
            boundLevel = client.level;
        }
        long now = System.nanoTime();
        Set<Integer> seen = new HashSet<>();
        for (Player player : client.level.players()) {
            boolean self = player == client.player;
            if (self ? targets.value().equals("Others") : targets.value().equals("Self")) continue;
            if (!self && (player.isSpectator() || player.distanceToSqr(client.player) > TRACK_RANGE_SQR)) continue;
            seen.add(player.getId());
            boolean onGround = player.onGround();
            Boolean before = grounded.put(player.getId(), onGround);
            if (before == null || before == onGround || player.isPassenger() || player.getAbilities().flying) continue;
            boolean jumped = before && player.getDeltaMovement().y > 0.08;
            boolean landed = !before;
            if (!JumpCirclePolicy.emits(jumped, landed, trigger.value())) continue;
            if (JumpCirclePolicy.withinCooldown(now, lastEvent.getOrDefault(player.getId(), 0L))) continue;
            if (landed && JumpCirclePolicy.landingCovered(now, landingGrace.getOrDefault(player.getId(), 0L))) continue;
            circles.addLast(new Circle(player.getX(), player.getY(), player.getZ(), now));
            lastEvent.put(player.getId(), now);
            if (jumped) landingGrace.put(player.getId(), now + JumpCirclePolicy.LANDING_GRACE_NANOS);
            while (circles.size() > (int) limit.value()) circles.removeFirst();
        }
        grounded.keySet().retainAll(seen);
        lastEvent.keySet().retainAll(seen);
        landingGrace.keySet().retainAll(seen);
    }

    public void emit(EffectBatch batch, Vec3 camera, int accent, long now) {
        emit(batch, null, camera, accent, now);
    }

    public void emit(EffectBatch batch, Level level, Vec3 camera, int accent, long now) {
        if (!enabled() || circles.isEmpty()) return;
        int rgb = EffectPalette.resolve(colorMode.value(), accent, color.value());
        double life = lifetime.value();
        boolean follow = level != null && terrainFollow.value();
        Iterator<Circle> iterator = circles.iterator();
        while (iterator.hasNext()) {
            Circle circle = iterator.next();
            double age = (now - circle.born()) / 1_000_000_000.0 / life;
            if (age >= 1) {
                iterator.remove();
                continue;
            }
            DoubleBinaryOperator sampler = follow
                    ? (wx, wz) -> sampleGround(level, wx, circle.y(), wz)
                    : null;
            switch (style.value()) {
                case "Ring" -> ring(batch, sampler, circle, camera, age, 1.0, rgb, false);
                case "Double" -> {
                    // Two concentric fronts spawned together read as one double-edged ring, not a sequence.
                    ring(batch, sampler, circle, camera, age, 1.0, rgb, false);
                    ring(batch, sampler, circle, camera, age, 0.66, rgb, false);
                }
                case "Shockwave" -> ring(batch, sampler, circle, camera, age, 2.3, rgb, true);
                default -> ring(batch, sampler, circle, camera, age, 1.0, rgb, false);
            }
        }
    }

    private static double sampleGround(Level level, double wx, double refY, double wz) {
        double surface = FxParticles.groundY(level, wx, refY + 1.5, wz, 5);
        return Double.isFinite(surface) ? surface : refY;
    }

    private void ring(EffectBatch batch, DoubleBinaryOperator sampler, Circle circle, Vec3 camera,
                      double progress, double size, int rgb, boolean shockSpokes) {
        double r = radius.value() * size * Motion.SETTLE.apply(progress);
        double alpha = Math.pow(1 - progress, 1.6);
        if (r < 0.02 || alpha < 0.01) return;
        int segments = r > 2 ? 72 : 48;
        if (sampler == null && !shockSpokes) {
            double x = circle.x() - camera.x, y = circle.y() - camera.y + 0.03, z = circle.z() - camera.z;
            if (fill.value()) {
                batch.band(x, y, z, r * 0.62, r, segments, EffectBatch.withAlpha(rgb, 0),
                        EffectBatch.withAlpha(rgb, alpha * 0.3), false);
            }
            batch.glowRing(x, y, z, r, segments, EffectBatch.withAlpha(rgb, alpha), (float) width.value(), false, 0, 0);
            return;
        }
        emitTerrainShockwave(batch, sampler, circle.x(), circle.y(), circle.z(),
                camera.x, camera.y, camera.z, r, segments, rgb, alpha,
                (float) width.value(), fill.value(), shockSpokes, progress);
    }

    /**
     * Emits a 3D ground shockwave ring that samples terrain elevation at each angular vertex
     * so the wavefront climbs and descends blocks, stairs, and slopes instead of floating flat.
     */
    public static void emitTerrainShockwave(EffectBatch batch, DoubleBinaryOperator heightSampler,
                                            double worldX, double worldY, double worldZ,
                                            double camX, double camY, double camZ,
                                            double r, int segments, int rgb, double alpha,
                                            float lineWidth, boolean featheredFill,
                                            boolean shockSpokes, double progress) {
        int count = Math.max(12, segments);
        double p = Math.min(1, Math.max(0, progress));
        // The main wavefront is a raised wall of energy that is tallest just after impact and sinks as it spreads.
        double wall = shockSpokes ? 0.42 * Math.pow(1 - p, 1.4) * Math.min(1, p * 6) : 0.06 * (1 - p);
        int bright = EffectBatch.mixRgb(rgb, 0xFFFFFF, 0.4);
        int lineColor = EffectBatch.withAlpha(bright, alpha);
        int under = EffectBatch.withAlpha(0x000000, alpha * 0.35);
        int wallBase = EffectBatch.withAlpha(rgb, alpha * 0.75);
        int wallTop = EffectBatch.withAlpha(rgb, 0);
        int trailIn = EffectBatch.withAlpha(rgb, 0);
        int trailOut = EffectBatch.withAlpha(rgb, alpha * 0.45);
        int shadowIn = EffectBatch.withAlpha(0x000000, alpha * 0.22);
        int shadowOut = EffectBatch.withAlpha(0x000000, 0);

        double[] radii = {r * 0.5, r, r + Math.max(0.08, r * 0.08)};
        double[][] gx = new double[3][count + 1], gy = new double[3][count + 1], gz = new double[3][count + 1];
        for (int i = 0; i <= count; i++) {
            double angle = i * (Math.PI * 2.0 / count);
            double cos = Math.cos(angle), sin = Math.sin(angle);
            for (int k = 0; k < 3; k++) {
                double wx = worldX + cos * radii[k], wz = worldZ + sin * radii[k];
                double wy = heightSampler != null ? heightSampler.applyAsDouble(wx, wz) : worldY;
                if (!Double.isFinite(wy)) wy = worldY;
                gx[k][i] = wx - camX;
                gy[k][i] = wy - camY + 0.03;
                gz[k][i] = wz - camZ;
            }
        }

        for (int i = 0; i < count; i++) {
            int j = i + 1;
            // Contact shadow just ahead of the front, then the bright leading edge with a dark under-stroke.
            batch.quad(gx[1][i], gy[1][i], gz[1][i], gx[2][i], gy[2][i], gz[2][i], gx[2][j], gy[2][j], gz[2][j], gx[1][j], gy[1][j], gz[1][j],
                    shadowIn, shadowOut, shadowOut, shadowIn, false);
            batch.line(gx[1][i], gy[1][i], gz[1][i], gx[1][j], gy[1][j], gz[1][j], under, lineWidth * 2.6F, false);
            batch.glowLine(gx[1][i], gy[1][i], gz[1][i], gx[1][j], gy[1][j], gz[1][j], lineColor, lineWidth * 1.2F, false);
            if (wall > 0.01) {
                batch.quad(gx[1][i], gy[1][i], gz[1][i], gx[1][j], gy[1][j], gz[1][j],
                        gx[1][j], gy[1][j] + wall, gz[1][j], gx[1][i], gy[1][i] + wall, gz[1][i],
                        wallBase, wallBase, wallTop, wallTop, false);
            }
            if (featheredFill) {
                batch.quad(gx[0][i], gy[0][i], gz[0][i], gx[1][i], gy[1][i], gz[1][i],
                        gx[1][j], gy[1][j], gz[1][j], gx[0][j], gy[0][j], gz[0][j],
                        trailIn, trailOut, trailOut, trailIn, false);
            }
            if (shockSpokes && (i % Math.max(1, count / 18) == 0)) {
                // Debris streaks thrown outward, riding just behind the front.
                double angle = (i + 0.5) * (Math.PI * 2.0 / count);
                double tailR = Math.max(0.05, r * 0.78), headR = r * 0.96;
                double twx = worldX + Math.cos(angle) * tailR, twz = worldZ + Math.sin(angle) * tailR;
                double hwx = worldX + Math.cos(angle) * headR, hwz = worldZ + Math.sin(angle) * headR;
                double twy = heightSampler != null ? heightSampler.applyAsDouble(twx, twz) : worldY;
                double hwy = heightSampler != null ? heightSampler.applyAsDouble(hwx, hwz) : worldY;
                if (!Double.isFinite(twy)) twy = worldY;
                if (!Double.isFinite(hwy)) hwy = worldY;
                double lift = wall * 0.5;
                batch.glowLine(twx - camX, twy - camY + 0.04, twz - camZ, hwx - camX, hwy - camY + 0.04 + lift, hwz - camZ,
                        EffectBatch.withAlpha(bright, alpha * 0.7), Math.max(1.0F, lineWidth * 0.8F), false);
            }
        }
    }

    /** Visible for tests: number of live circles. */
    int activeCircles() {
        return circles.size();
    }

    private record Circle(double x, double y, double z, long born) { }
}
