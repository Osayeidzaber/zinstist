package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
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
 * A soft, blurred glow around nearby players, mobs and items. Two camera-facing halo bands hug the body: a
 * bright ring at the silhouette fading outward plus a wider, fainter one. Filled gradients cannot show the
 * straight edges of an AABB, and a centre-peaked bloom is hidden by the model itself, so the alpha peaks on
 * the silhouette and feathers away from it. Geometry goes through the shared
 * {@link dev.jilaton.client.render.EffectBatch}, not the ESP box path.
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

    private final double[] aura = new double[4];

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
            // A filled radial gradient has no straight edges, and a bloom centred on the model is hidden by
            // it: two halos, brightest at the silhouette and fading outward, read as a glow around the body
            // where six face strips read as a drawn box and overlap into a column.
            Vec3 center = box.getCenter();
            emitAura(f, center.x, center.y, center.z, box.getXsize(), box.getZsize(), box.getYsize(),
                    thickness.value(), rgb, alpha, aura);
        }
    }

    /**
     * Emits the two aura layers around a body box. Kept static and free of Minecraft state so the geometry can
     * be asserted in a unit test; the caller reuses {@code aura} so no per-entity buffer is allocated.
     *
     * <p>The layers are halos, not filled blooms: the aura is drawn into the depth-tested pass after the
     * entities, so the body rejects everything inside its silhouette. A centre-peaked ellipse therefore only
     * ever showed its near-transparent rim outside the model; these are brightest at the silhouette edge and
     * fade outward, which is where the glow is actually visible.</p>
     */
    static void emitAura(FxFrame f, double x, double y, double z, double width, double depth, double height,
                         double glowThickness, int rgb, double alpha, double[] aura) {
        auraExtents(width, depth, height, glowThickness, aura);
        double feather = aura[2] - aura[0];
        // aura extents already include the 0.10 hug margin around the box; the inner edge of the first halo
        // sits on it so the bright ring hugs the silhouette instead of being buried under the model.
        double hugW = aura[0] - feather * 0.5, hugH = aura[1] - feather * 0.5;
        FxShapes.softHalo(f, x, y, z, hugW, hugH, aura[0], aura[1], rgb, alpha * 0.55, false);
        FxShapes.softHalo(f, x, y, z, aura[0], aura[1], aura[2], aura[3], rgb, alpha * 0.25, false);
    }

    /** Feather width of the two aura layers for a glow-thickness setting. */
    static double auraFeather(double glowThickness) {
        return 0.15 + Math.max(0, glowThickness) * 0.5;
    }

    /**
     * Writes the half-width/half-height of the two feathered layers around a body box as
     * {inner width, inner height, outer width, outer height}. The larger horizontal extent drives the
     * width, so wide mobs get a wide glow rather than a narrow column. Callers reuse one buffer.
     */
    static void auraExtents(double width, double depth, double height, double glowThickness, double[] out) {
        if (out == null || out.length < 4) throw new IllegalArgumentException("aura buffer needs 4 entries");
        double feather = auraFeather(glowThickness);
        double halfWidth = Math.max(0, Math.max(width, depth)) * 0.5 + 0.10;
        double halfHeight = Math.max(0, height) * 0.5 + 0.10;
        out[0] = halfWidth + feather * 0.5;
        out[1] = halfHeight + feather * 0.5;
        out[2] = halfWidth + feather;
        out[3] = halfHeight + feather;
    }

    private boolean selected(Entity entity) {
        if (entity instanceof ItemEntity) return items.value();
        if (entity instanceof Player) return players.value();
        if (entity instanceof LivingEntity living) return mobs.value() && !living.isDeadOrDying();
        return false;
    }
}
