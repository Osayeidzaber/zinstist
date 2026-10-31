package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.render.model.CompanionMotion;
import dev.jilaton.client.render.model.Material;
import dev.jilaton.client.render.model.Mesh;
import dev.jilaton.client.render.model.Meshes;
import dev.jilaton.client.render.model.ModelBatch;
import dev.jilaton.client.render.model.Transform;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A small floating companion that orbits the local player through a damped spring, so it trails and
 * settles instead of being welded to the player. It banks into sideways motion and is assembled from
 * three different primitives (faceted core, tilted ring, orbiting shards, plus a tail on the Comet).
 */
public final class CompanionModule extends ClientModule implements FxEmitter {
    private static final double SNAP_DISTANCE_SQR = 8 * 8;

    private final ChoiceSetting shape = addSetting(new ChoiceSetting("shape", "Shape", CompanionMotion.SHAPES, "Sentinel"));
    private final NumberSetting size = addSetting(new NumberSetting("size", "Size", 0.55, 1.60, 0.05, 1.0));
    private final NumberSetting orbitRadius = addSetting(new NumberSetting("orbit_radius", "Orbit radius", 0.70, 2.40, 0.05, 1.35));
    private final NumberSetting orbitSpeed = addSetting(new NumberSetting("orbit_speed", "Orbit speed", 0.20, 1.80, 0.05, 0.75));
    private final NumberSetting spring = addSetting(new NumberSetting("spring", "Spring", 4, 18, 1, 11));
    private final NumberSetting follow = addSetting(new NumberSetting("follow", "Follow damping", 0.4, 1.4, 0.05, 0.9));
    private final NumberSetting height = addSetting(new NumberSetting("height", "Height", 0.30, 1.60, 0.05, 0.95));
    private final ChoiceSetting palette = addSetting(EffectPalette.modeSetting("palette", "Accent"));
    private final TextSetting custom = addSetting(EffectPalette.hexSetting("custom", "Custom accent", "9FB4FF"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.15, 1, 0.05, 0.95));

    private final Mesh core = Meshes.diamond(0.085, 0.12, -0.10);
    private final Mesh ring = Meshes.torus(0.14, 0.011, 20, 4);
    private final Mesh shard = Meshes.diamond(0.030, 0.045, -0.030);
    private final Mesh tail = Meshes.diamond(0.022, 0.060, -0.040);
    private final ModelBatch batch = new ModelBatch();

    private final double[] state = new double[6];
    private double orbitPhase;
    private boolean spawned;
    private Object boundLevel;

    public CompanionModule() {
        super("companion", "Companion", "A spring-followed companion with a faceted core and banking turns.",
                ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        spawned = false;
        orbitPhase = 0;
        boundLevel = null;
        batch.reset();
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        Player player = client.player;
        if (client.level == null || player == null) return;
        if (client.level != boundLevel) {
            boundLevel = client.level;
            orbitPhase = 0;
            spawned = false;
        }
        double alpha = opacity.value() * Math.min(1, Math.max(0, f.intensity));
        if (alpha <= 0.01) return;

        orbitPhase = CompanionMotion.advanceOrbit(orbitPhase, f.dt, orbitSpeed.value(), f.reducedMotion);
        Vec3 pos = player.getPosition(f.partial);
        double[] target = CompanionMotion.orbitTarget(pos.x, pos.y, pos.z, orbitPhase,
                orbitRadius.value(), height.value());
        if (!spawned) {
            state[0] = target[0];
            state[1] = target[1];
            state[2] = target[2];
            state[3] = 0;
            state[4] = 0;
            state[5] = 0;
            spawned = true;
        } else {
            double dx = state[0] - target[0], dy = state[1] - target[1], dz = state[2] - target[2];
            if (dx * dx + dy * dy + dz * dz > SNAP_DISTANCE_SQR) {
                state[0] = target[0];
                state[1] = target[1];
                state[2] = target[2];
                state[3] = 0;
                state[4] = 0;
                state[5] = 0;
            } else {
                double stiffness = spring.value();
                double[] next = CompanionMotion.springStep(state[0], state[1], state[2],
                        state[3], state[4], state[5], target[0], target[1], target[2],
                        stiffness, CompanionMotion.dampingFor(stiffness, follow.value()), f.dt);
                System.arraycopy(next, 0, state, 0, 6);
            }
        }

        double vx = state[3], vy = state[4], vz = state[5];
        double speed = Math.hypot(vx, vz);
        double lateral = vx * f.rightX + vz * f.rightZ;
        double bank = CompanionMotion.bankDegrees(lateral, speed, 16);
        String selected = shape.value();
        int accent = EffectPalette.resolve(palette.value(), f.accent, custom.value());
        Material coreMaterial = new Material(EffectBatch.mixRgb(accent, 0x0B0A10, 0.70),
                EffectBatch.mixRgb(accent, 0xFFFFFF, 0.65), accent, 0.55);
        Material ringMaterial = new Material(EffectBatch.mixRgb(accent, 0x101018, 0.80),
                EffectBatch.mixRgb(accent, 0xFFFFFF, 0.40), accent, 0.80);
        Material shardMaterial = new Material(EffectBatch.mixRgb(accent, 0x120F18, 0.55),
                EffectBatch.mixRgb(accent, 0xFFFFFF, 0.35), accent, 0.90);

        double s = size.value();
        Transform base = CompanionMotion.bankedFrame(state[0], state[1], state[2], vx, vy, vz, bank);
        batch.reset();
        FxShapes.softDot(f, state[0], state[1], state[2], 0.10 * s, accent, 0.16 * alpha, false);
        batch.add(f, core, coreMaterial, base, s, alpha, false);
        if (CompanionMotion.hasRing(selected)) {
            Transform tilted = CompanionMotion.ringFrame(base, orbitPhase);
            batch.add(f, ring, ringMaterial, tilted, s, alpha, false);
        }
        int shards = CompanionMotion.shards(selected);
        double orbit = CompanionMotion.shardOrbit(selected) * 0.14;
        for (int i = 0; i < shards; i++) {
            double angle = orbitPhase * 1.9 + i * Math.PI * 2 / shards;
            Transform shardFrame = CompanionMotion.shardFrame(base, angle, orbit,
                    Math.sin(orbitPhase * 1.3 + i) * 0.02);
            batch.add(f, shard, shardMaterial, shardFrame, 0.38 * s, alpha, false);
        }
        if (CompanionMotion.hasTail(selected)) {
            for (int i = 1; i <= 3; i++) {
                Transform tailFrame = base.translate(0, 0, -0.07 * i);
                batch.add(f, tail, shardMaterial, tailFrame, s * (0.55 - 0.10 * i), alpha * (1 - 0.2 * i), false);
            }
        }
        batch.flush(f);
    }
}
