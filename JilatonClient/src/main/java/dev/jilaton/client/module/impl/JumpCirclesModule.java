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
    private static final double[] RIPPLE_DELAYS = {0, 0.14, 0.28};
    private static final double[] SHOCKWAVE_DELAYS = {0, 0.10, 0.22, 0.36};
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
    private Object boundLevel;

    public JumpCirclesModule() {
        super("jump_circles", "JumpCircles", "Expanding ground ripples when players jump or land.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        circles.clear();
        grounded.clear();
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
            String mode = trigger.value();
            if (jumped && !mode.equals("Land") || landed && !mode.equals("Jump")) {
                circles.addLast(new Circle(player.getX(), player.getY(), player.getZ(), now));
                while (circles.size() > (int) limit.value()) circles.removeFirst();
            }
        }
        grounded.keySet().retainAll(seen);
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
                    ring(batch, sampler, circle, camera, age, 1.0, rgb, false);
                    ring(batch, sampler, circle, camera, age, 0.66, rgb, false);
                }
                case "Shockwave" -> {
                    for (int i = 0; i < SHOCKWAVE_DELAYS.length; i++) {
                        double delay = SHOCKWAVE_DELAYS[i];
                        double local = (age - delay) / (1 - delay);
                        if (local > 0) {
                            ring(batch, sampler, circle, camera, local, 1.15 - delay * 0.65, rgb, i == 0);
                        }
                    }
                }
                default -> {
                    for (double delay : RIPPLE_DELAYS) {
                        double local = (age - delay) / (1 - delay);
                        if (local > 0) ring(batch, sampler, circle, camera, local, 1.0 - delay * 0.8, rgb, false);
                    }
                }
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
        double crest = shockSpokes ? Math.sin(Math.min(1, Math.max(0, progress)) * Math.PI) * 0.12 * (1 - progress) : 0;
        double innerR = r * 0.62;
        int lineColor = EffectBatch.withAlpha(rgb, alpha);
        int innerFill = EffectBatch.withAlpha(rgb, 0);
        int outerFill = EffectBatch.withAlpha(rgb, alpha * 0.3);

        double[] ox = new double[count + 1];
        double[] oy = new double[count + 1];
        double[] oz = new double[count + 1];
        double[] ix = featheredFill ? new double[count + 1] : null;
        double[] iy = featheredFill ? new double[count + 1] : null;
        double[] iz = featheredFill ? new double[count + 1] : null;

        for (int i = 0; i <= count; i++) {
            double angle = i * (Math.PI * 2.0 / count);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double wx = worldX + cos * r;
            double wz = worldZ + sin * r;
            double wy = heightSampler != null ? heightSampler.applyAsDouble(wx, wz) : worldY;
            if (!Double.isFinite(wy)) wy = worldY;
            ox[i] = wx - camX;
            oy[i] = wy - camY + 0.03 + crest;
            oz[i] = wz - camZ;

            if (featheredFill) {
                double iwx = worldX + cos * innerR;
                double iwz = worldZ + sin * innerR;
                double iwy = heightSampler != null ? heightSampler.applyAsDouble(iwx, iwz) : worldY;
                if (!Double.isFinite(iwy)) iwy = worldY;
                ix[i] = iwx - camX;
                iy[i] = iwy - camY + 0.025;
                iz[i] = iwz - camZ;
            }
        }

        for (int i = 0; i < count; i++) {
            batch.glowLine(ox[i], oy[i], oz[i], ox[i + 1], oy[i + 1], oz[i + 1], lineColor, lineWidth, false);
            if (featheredFill) {
                batch.quad(ix[i], iy[i], iz[i], ox[i], oy[i], oz[i],
                        ox[i + 1], oy[i + 1], oz[i + 1], ix[i + 1], iy[i + 1], iz[i + 1],
                        innerFill, outerFill, outerFill, innerFill, false);
            }
            if (shockSpokes && (i % Math.max(1, count / 12) == 0)) {
                double angle = i * (Math.PI * 2.0 / count);
                double tailR = Math.max(0.05, r - 0.24);
                double twx = worldX + Math.cos(angle) * tailR;
                double twz = worldZ + Math.sin(angle) * tailR;
                double twy = heightSampler != null ? heightSampler.applyAsDouble(twx, twz) : worldY;
                if (!Double.isFinite(twy)) twy = worldY;
                int spokeColor = EffectBatch.withAlpha(rgb, alpha * 0.55);
                batch.line(twx - camX, twy - camY + 0.03, twz - camZ,
                        ox[i], oy[i], oz[i], spokeColor, Math.max(1.0F, lineWidth * 0.75F), false);
            }
        }
    }

    /** Visible for tests: number of live circles. */
    int activeCircles() {
        return circles.size();
    }

    private record Circle(double x, double y, double z, long born) { }
}
