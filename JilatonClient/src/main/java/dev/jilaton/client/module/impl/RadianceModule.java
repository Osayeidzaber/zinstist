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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A soft, blurred glow shell around nearby players, mobs and items: every face is drawn only as a
 * feathered border that fades to zero outward, so the result reads as a ghostly aura instead of a hard
 * ESP box. Geometry goes through the shared {@link EffectBatch}, not through the ESP box path.
 */
public final class RadianceModule extends ClientModule implements FxEmitter {
    private static final double TAU = Math.PI * 2;

    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "A98CFF"));
    private final BooleanSetting players = addSetting(new BooleanSetting("players", "Players", true));
    private final BooleanSetting mobs = addSetting(new BooleanSetting("mobs", "Mobs", true));
    private final BooleanSetting items = addSetting(new BooleanSetting("items", "Items", false));
    private final NumberSetting range = addSetting(new NumberSetting("range", "Range", 8, 64, 1, 24));
    private final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Glow thickness", 0.1, 0.6, 0.02, 0.3));
    private final NumberSetting strength = addSetting(new NumberSetting("strength", "Opacity", 0.04, 0.5, 0.01, 0.28));
    private final BooleanSetting pulse = addSetting(new BooleanSetting("pulse", "Breathe", true));
    private final NumberSetting pulseSpeed = addSetting(new NumberSetting("pulse_speed", "Breath speed", 0, 2, 0.05, 0.55));

    private final double[] corners = new double[12];
    private final double[] min = new double[3];
    private final double[] max = new double[3];

    public RadianceModule() {
        super("radiance", "Radiance", "Soft blurred glow around players, mobs and items; a ghostly aura, not a box.", ModuleCategory.RENDER);
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        if (client.level == null || client.player == null) return;
        int rgb = EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        double reach = range.value();
        double reachSqr = reach * reach;
        double base = strength.value() * Math.min(1, f.intensity);
        double seconds = f.seconds();
        for (Entity entity : client.level.entitiesForRendering()) {
            // In first person the shell would sit around the camera and haze the whole screen; in third
            // person the local player is the most useful subject, so it is only skipped from inside.
            if (entity == client.player && f.firstPerson) continue;
            if (!selected(entity)) continue;
            if (entity.isInvisible() || entity.isSpectator() || !entity.isAlive()) continue;
            if (entity.distanceToSqr(client.player) > reachSqr) continue;
            double alpha = base;
            if (pulse.value() && !f.reducedMotion) {
                double phase = (entity.getId() * 0.618033988749895 % 1) * TAU;
                alpha *= 0.72 + 0.28 * (0.5 + 0.5 * Math.sin(seconds * pulseSpeed.value() * TAU * 0.25 + phase));
            }
            if (alpha <= 0.004) continue;
            Vec3 offset = entity.getPosition(f.partial).subtract(entity.position());
            AABB box = entity.getBoundingBox().move(offset).inflate(0.015);
            // A camera-facing soft disc behind the border strips supplies the bloom; the strips alone are
            // only a few pixels wide and read as a drawn box at any sane opacity.
            Vec3 center = box.getCenter();
            double halo = Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize())) * 0.5 + 0.15;
            FxShapes.softDot(f, center.x, center.y, center.z, halo, rgb, alpha * 0.55, false);
            shell(f, box, rgb, alpha, thickness.value());
        }
    }

    private boolean selected(Entity entity) {
        if (entity instanceof ItemEntity) return items.value();
        if (entity instanceof Player) return players.value();
        if (entity instanceof LivingEntity living) return mobs.value() && !living.isDeadOrDying();
        return false;
    }

    private void shell(FxFrame f, AABB box, int rgb, double alpha, double glowThickness) {
        min[0] = box.minX; min[1] = box.minY; min[2] = box.minZ;
        max[0] = box.maxX; max[1] = box.maxY; max[2] = box.maxZ;
        for (int axis = 0; axis < 3; axis++) {
            int u = (axis + 1) % 3, v = (axis + 2) % 3;
            double du = Math.min(glowThickness, (max[u] - min[u]) * 0.5);
            double dv = Math.min(glowThickness, (max[v] - min[v]) * 0.5);
            for (int side = 0; side < 2; side++) {
                double plane = side == 0 ? min[axis] : max[axis];
                strip(f, axis, plane, u, min[u], min[u] + du, v, min[v], max[v], rgb, alpha, true);
                strip(f, axis, plane, u, max[u] - du, max[u], v, min[v], max[v], rgb, alpha, false);
                strip(f, axis, plane, u, min[u], max[u], v, min[v], min[v] + dv, rgb, alpha, true);
                strip(f, axis, plane, u, min[u], max[u], v, max[v] - dv, max[v], rgb, alpha, false);
            }
        }
    }

    /**
     * One border strip of a face: the edge on the box outline keeps {@code peak} alpha and the inner edge
     * fades to zero, which is what makes the shell read as a blur rather than a drawn box.
     */
    private void strip(FxFrame f, int axis, double plane, int u, double u0, double u1, int v, double v0, double v1,
                       int rgb, double peak, boolean edgeAtU0) {
        fill(0, f, axis, plane, u, u0, v, v0);
        fill(1, f, axis, plane, u, u0, v, v1);
        fill(2, f, axis, plane, u, u1, v, v1);
        fill(3, f, axis, plane, u, u1, v, v0);
        int edge = EffectBatch.withAlpha(rgb, peak);
        int inner = EffectBatch.withAlpha(rgb, 0);
        int a = edgeAtU0 ? edge : inner, b = edgeAtU0 ? inner : edge;
        f.batch.quad(corners[0], corners[1], corners[2], corners[3], corners[4], corners[5],
                corners[6], corners[7], corners[8], corners[9], corners[10], corners[11], a, a, b, b, false);
    }

    private void fill(int slot, FxFrame f, int axis, double plane, int u, double uu, int v, double vv) {
        int base = slot * 3;
        corners[base] = 0;
        corners[base + 1] = 0;
        corners[base + 2] = 0;
        corners[base + axis] = plane;
        corners[base + u] = uu;
        corners[base + v] = vv;
        corners[base] -= f.camX;
        corners[base + 1] -= f.camY;
        corners[base + 2] -= f.camZ;
    }
}
