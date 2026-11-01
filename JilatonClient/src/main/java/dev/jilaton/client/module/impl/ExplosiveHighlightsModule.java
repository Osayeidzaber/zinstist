package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Glowing cores, orbiting light "swooshes" and rising sparkles on end crystals and charged respawn anchors.
 * Purely visual: it never changes what is targeted or placed. Anchors are found by a bounded block scan
 * every half second; crystals come from the rendered entity list.
 */
public final class ExplosiveHighlightsModule extends ClientModule implements FxEmitter {
    private static final int OWNER = FxParticles.ownerId("explosive_highlights");
    private final BooleanSetting crystals = addSetting(new BooleanSetting("crystals", "End crystals", true));
    private final BooleanSetting anchors = addSetting(new BooleanSetting("anchors", "Charged respawn anchors", true));
    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Style", List.of("Glass cubes", "Swoosh", "Halo", "Core only"), "Glass cubes"));
    private final NumberSetting range = addSetting(new NumberSetting("range", "Range", 4, 32, 1, 16));
    private final NumberSetting speed = addSetting(new NumberSetting("speed", "Rotation speed", 0.2, 3, 0.05, 1));
    private final BooleanSetting sparkles = addSetting(new BooleanSetting("sparkles", "Rising sparkles", true));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Core color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "E7B8FF"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.1, 1, 0.05, 0.85));
    private final List<BlockPos> chargedAnchors = new ArrayList<>();
    private final FxParticles.Spec spec = new FxParticles.Spec();
    private final Random random = new Random();
    private int scanCooldown;

    public ExplosiveHighlightsModule() {
        super("explosive_highlights", "Crystal & Anchor FX", "Glowing cores and orbiting light arcs on end crystals and charged anchors.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        chargedAnchors.clear();
        FxParticles.get().clearOwner(OWNER);
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.level == null || client.player == null) { chargedAnchors.clear(); return; }
        if (!anchors.value()) { chargedAnchors.clear(); return; }
        if (--scanCooldown > 0) return;
        scanCooldown = 10;
        chargedAnchors.clear();
        int r = (int) Math.min(12, range.value());
        BlockPos origin = client.player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) for (int dy = -r / 2; dy <= r / 2; dy++) for (int dz = -r; dz <= r; dz++) {
            pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
            BlockState state = client.level.getBlockState(pos);
            if (state.is(Blocks.RESPAWN_ANCHOR) && state.getValue(RespawnAnchorBlock.CHARGE) > 0) {
                chargedAnchors.add(pos.immutable());
                if (chargedAnchors.size() >= 32) return;
            }
        }
    }

    @Override
    public void emit(FxFrame f) {
        var level = f.client.level;
        if (level == null || f.client.player == null) return;
        int rgb = colorMode.value().equals(EffectPalette.MONO) ? 0xF2F2F2 : EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        double a = opacity.value() * f.intensity;
        double time = f.seconds() * speed.value() * (f.reducedMotion ? 0.25 : 1);
        double maxSqr = range.value() * range.value();
        if (crystals.value()) {
            int drawn = 0;
            for (Entity entity : level.entitiesForRendering()) {
                if (!(entity instanceof EndCrystal) || entity.distanceToSqr(f.client.player) > maxSqr) continue;
                Vec3 p = entity.getPosition(f.partial);
                decorate(f, p.x, p.y + 1.15, p.z, 0.75, entity.getId(), rgb, a, time);
                if (++drawn >= 24) break;
            }
        }
        for (BlockPos pos : chargedAnchors) {
            int charge = level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR) ? level.getBlockState(pos).getValue(RespawnAnchorBlock.CHARGE) : 0;
            if (charge <= 0) continue;
            decorate(f, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0.65 + charge * 0.06, pos.hashCode(), rgb, a * (0.6 + charge * 0.1), time);
        }
    }

    private void decorate(FxFrame f, double x, double y, double z, double radius, int seed, int rgb, double a, double time) {
        double pulse = 0.85 + 0.15 * Math.sin(time * 3 + seed);
        FxShapes.softDot(f, x, y, z, radius * 0.9 * pulse, rgb, a * 0.35, false);
        FxShapes.softDot(f, x, y, z, radius * 0.28, 0xFFFFFF, a * 0.9, false);
        switch (style.value()) {
            case "Swoosh" -> {
                for (int k = 0; k < 3; k++) {
                    double tilt = 0.35 + k * 0.5 + Math.sin(time * 0.4 + seed + k) * 0.15;
                    double nx = Math.sin(tilt) * Math.cos(k * 2.1 + seed), nz = Math.sin(tilt) * Math.sin(k * 2.1 + seed);
                    FxShapes.tiltedRing(f, x, y, z, nx, Math.cos(tilt), nz, radius * (1 + k * 0.18),
                            time * (2.2 + k * 0.6) * (k % 2 == 0 ? 1 : -1), 0.42, 0xFFFFFF, a, 2.2F, false);
                }
            }
            case "Glass cubes" -> {
                // Three nested frosted cubes tumbling on different axes, the outer one slowest.
                for (int k = 0; k < 3; k++) {
                    double spin = time * (0.9 - k * 0.2) * (k % 2 == 0 ? 1 : -1) + seed;
                    FxShapes.glassCube(f, x, y, z, radius * (0.42 + k * 0.17), spin, 0.6 + spin * 0.7 + k, spin * 0.4 + k * 1.3,
                            k == 0 ? 0xFFFFFF : rgb, a * (1 - k * 0.2), false);
                }
            }
            case "Halo" -> {
                f.batch.ring(x - f.camX, y - 0.35 - f.camY, z - f.camZ, radius * 1.05, f.quality.circleSegments(),
                        FxShapes.argb(0xFFFFFF, a), 2F, false, 0.6, time * 2);
                f.batch.band(x - f.camX, y - 0.35 - f.camY, z - f.camZ, radius * 0.8, radius * 1.25, f.quality.circleSegments(),
                        FxShapes.argb(rgb, 0), FxShapes.argb(rgb, a * 0.3), false);
            }
            default -> { }
        }
        if (sparkles.value() && random.nextDouble() < f.dt * 6 * f.quality.density()) {
            spec.reset();
            double angle = random.nextDouble() * Math.PI * 2;
            spec.x = x + Math.cos(angle) * radius * 0.6; spec.y = y - 0.2; spec.z = z + Math.sin(angle) * radius * 0.6;
            spec.vy = 0.6 + random.nextDouble() * 0.5; spec.vx = -Math.sin(angle) * 0.3; spec.vz = Math.cos(angle) * 0.3;
            spec.drag = 0.93; spec.life = 1.2; spec.size0 = 0.07; spec.size1 = 0.01;
            spec.shape = FxParticles.Shape.STAR; spec.spin = 3; spec.color = 0xFFFFFF; spec.color1 = rgb;
            spec.alpha = a; spec.glow = true; spec.owner = OWNER;
            FxParticles.get().spawn(spec);
        }
    }
}
