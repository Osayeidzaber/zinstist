package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.render.model.BodyAnchor;
import dev.jilaton.client.render.model.HaloMotion;
import dev.jilaton.client.render.model.Material;
import dev.jilaton.client.render.model.Mesh;
import dev.jilaton.client.render.model.Meshes;
import dev.jilaton.client.render.model.ModelBatch;
import dev.jilaton.client.render.model.Transform;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A real torus above the head with a travelling seal bead. The ring is tilted forward and spun from the
 * frame delta; the bead carries a glow through the shared {@link FxShapes} path. Its height and tilt are
 * clamped by {@link HaloMotion} so the ring never reaches the name-tag anchor.
 */
public final class HaloModule extends ClientModule implements FxEmitter {
    private static final int MAX_PLAYERS = 16;
    private static final double MAX_DISTANCE_SQR = 64 * 64;

    private final NumberSetting radius = addSetting(new NumberSetting("radius", "Radius", 0.16, 0.40, 0.01, 0.24));
    private final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Thickness", 0.008, 0.05, 0.002, 0.018));
    private final NumberSetting tilt = addSetting(new NumberSetting("tilt", "Tilt", -30, 30, 1, 12));
    private final NumberSetting spin = addSetting(new NumberSetting("spin", "Spin speed", 0, 2, 0.05, 0.45));
    private final NumberSetting bob = addSetting(new NumberSetting("bob", "Bob", 0, 0.04, 0.002, 0.012));
    private final ChoiceSetting palette = addSetting(EffectPalette.modeSetting("palette", "Accent"));
    private final TextSetting custom = addSetting(EffectPalette.hexSetting("custom", "Custom accent", "D8C8FF"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.15, 1, 0.05, 0.9));
    private final BooleanSetting own = addSetting(new BooleanSetting("own", "Show on yourself", true));
    private final BooleanSetting others = addSetting(new BooleanSetting("others", "Show on others", true));

    private final ModelBatch batch = new ModelBatch();
    private final Mesh bead = Meshes.diamond(0.032, 0.05, -0.038);
    private Mesh ring;
    private String ringKey = "";
    private double phase;
    private double pulsePhase;
    private Object boundLevel;

    public HaloModule() {
        super("halo", "Halo", "Slow-spinning torus above the head with a glowing seal, held clear of name tags.",
                ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        phase = 0;
        pulsePhase = 0;
        boundLevel = null;
        batch.reset();
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        if (client.level == null || client.player == null) return;
        if (client.level != boundLevel) {
            boundLevel = client.level;
            phase = 0;
            pulsePhase = 0;
        }
        phase = HaloMotion.advancePhase(phase, f.dt, spin.value(), f.reducedMotion);
        pulsePhase = HaloMotion.advancePulse(pulsePhase, f.dt, f.reducedMotion);
        double pulse = HaloMotion.pulse(pulsePhase, f.reducedMotion);

        int accent = EffectPalette.resolve(palette.value(), f.accent, custom.value());
        double alpha = opacity.value() * Math.min(1, Math.max(0, f.intensity));
        if (alpha <= 0.01) return;

        double r = radius.value();
        double t = thickness.value();
        double bobOffset = HaloMotion.bob(phase, bob.value(), f.reducedMotion);
        double effectiveTilt = HaloMotion.clampTilt(tilt.value(), r, t, bob.value());
        Mesh built = ring(f, r, t);
        Material ringMaterial = new Material(EffectBatch.mixRgb(accent, 0x14121A, 0.82),
                EffectBatch.mixRgb(accent, 0xFFFFFF, 0.72), accent, 0.58 + 0.26 * pulse);
        Material beadMaterial = new Material(EffectBatch.mixRgb(accent, 0xFFFFFF, 0.35),
                0xFFFFFF, accent, 0.55 + 0.45 * pulse);
        boolean firstPersonView = client.options.getCameraType().isFirstPerson();

        batch.reset();
        int drawn = 0;
        for (Player player : client.level.players()) {
            if (!player.isAlive() || player.isInvisible()) continue;
            boolean self = player == client.player;
            if (self && (firstPersonView || !own.value())) continue;
            if (!self && !others.value()) continue;
            Vec3 pos = player.getPosition(f.partial);
            if (pos.distanceToSqr(f.camX, f.camY, f.camZ) > MAX_DISTANCE_SQR) continue;
            if (++drawn > MAX_PLAYERS) break;

            double cx = pos.x;
            double cy = pos.y + HaloMotion.centreHeight(player.getBbHeight()) + bobOffset;
            double cz = pos.z;
            Transform ringFrame = Transform.translation(cx, cy, cz)
                    .compose(Transform.rotation(BodyAnchor.bodyYaw(player, f.partial), effectiveTilt, 0));
            batch.add(f, built, ringMaterial, ringFrame, 1.0, alpha, false);

            double[] beadLocal = HaloMotion.ringPoint(r, phase);
            Transform beadFrame = ringFrame.translate(beadLocal[0], beadLocal[1], beadLocal[2]);
            batch.add(f, bead, beadMaterial, beadFrame, 1.0, alpha, false);

            double[] world = ringFrame.point(beadLocal[0], beadLocal[1], beadLocal[2]);
            FxShapes.glowDot(f, world[0], world[1], world[2], 0.05 * (0.85 + 0.3 * pulse),
                    accent, (0.42 + 0.26 * pulse) * alpha, true, false);
        }
        batch.flush(f);
    }

    private Mesh ring(FxFrame f, double r, double t) {
        String key = r + "/" + t + "/" + f.quality.name();
        if (ring == null || !key.equals(ringKey)) {
            ring = Meshes.torus(r, t, HaloMotion.majorSegments(f.quality.ordinal()),
                    HaloMotion.minorSegments(f.quality.ordinal()));
            ringKey = key;
        }
        return ring;
    }
}
