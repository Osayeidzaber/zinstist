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

import java.util.List;
import java.util.Random;

/**
 * Environmental motes that drift through a box around the camera. Motes wrap at the box edges instead of
 * respawning, so density stays constant while moving and nothing is allocated per frame.
 */
public final class AmbienceModule extends ClientModule implements FxEmitter {
    private static final int MAX = 600;
    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Style",
            List.of("Snow", "Dust", "Fireflies", "Embers", "Stardust", "Rain streaks", "Light streaks"), "Snow"));
    private final NumberSetting density = addSetting(new NumberSetting("density", "Density", 0.1, 1, 0.05, 0.45));
    private final NumberSetting radius = addSetting(new NumberSetting("radius", "Radius", 6, 32, 1, 16));
    private final NumberSetting speed = addSetting(new NumberSetting("speed", "Speed", 0.1, 3, 0.05, 1));
    private final NumberSetting size = addSetting(new NumberSetting("size", "Size", 0.3, 3, 0.05, 1));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.05, 1, 0.05, 0.8));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "F2F2F2"));
    private final BooleanSetting overworldOnly = addSetting(new BooleanSetting("overworld_only", "Overworld only", false));
    private final BooleanSetting nightOnly = addSetting(new BooleanSetting("night_only", "Only at night / indoors", false));
    private final double[] x = new double[MAX], y = new double[MAX], z = new double[MAX], phase = new double[MAX];
    private boolean seeded;
    private final Random random = new Random(7);

    public AmbienceModule() {
        super("ambience", "Ambience", "Drifting snow, dust, fireflies, embers and stardust around the camera.", ModuleCategory.RENDER);
    }

    @Override protected void onDisable(Minecraft client) { seeded = false; }

    @Override
    public void emit(FxFrame f) {
        var level = f.client.level;
        if (level == null) return;
        if (overworldOnly.value() && level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        if (nightOnly.value() && level.isBrightOutside()) return;
        double r = radius.value();
        int n = (int) Math.min(MAX, MAX * density.value() * f.quality.density());
        if (!seeded) {
            for (int i = 0; i < MAX; i++) {
                x[i] = f.camX + (random.nextDouble() * 2 - 1) * r;
                y[i] = f.camY + (random.nextDouble() * 2 - 1) * r * 0.6;
                z[i] = f.camZ + (random.nextDouble() * 2 - 1) * r;
                phase[i] = random.nextDouble() * Math.PI * 2;
            }
            seeded = true;
        }
        int rgb = EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        String s = style.value();
        double a = opacity.value() * f.intensity;
        double dt = f.dt * speed.value() * (f.reducedMotion ? 0.3 : 1);
        double t = f.seconds();
        // Base sizes are tuned for a few pixels at 8-16 blocks; 2.4x keeps them visible on dark scenes.
        double sz = size.value() * 2.4;
        for (int i = 0; i < n; i++) {
            double ph = phase[i];
            switch (s) {
                case "Snow" -> { y[i] -= dt * (0.6 + 0.3 * Math.sin(ph)); x[i] += Math.sin(t * 0.7 + ph) * dt * 0.3; z[i] += Math.cos(t * 0.5 + ph) * dt * 0.25; }
                case "Dust" -> { x[i] += Math.sin(t * 0.2 + ph) * dt * 0.15; y[i] += Math.sin(t * 0.3 + ph * 2) * dt * 0.08; z[i] += Math.cos(t * 0.25 + ph) * dt * 0.15; }
                case "Fireflies" -> { x[i] += Math.sin(t * 0.9 + ph) * dt * 0.5; y[i] += Math.sin(t * 1.3 + ph * 3) * dt * 0.3; z[i] += Math.cos(t * 0.8 + ph) * dt * 0.5; }
                case "Embers" -> { y[i] += dt * (0.5 + 0.4 * Math.sin(ph)); x[i] += Math.sin(t * 1.4 + ph) * dt * 0.25; }
                case "Rain streaks" -> { y[i] -= dt * 14; x[i] += dt * 1.2; }
                case "Light streaks" -> { y[i] -= dt * 6; x[i] += dt * 2.5 * Math.cos(ph); z[i] += dt * 2.5 * Math.sin(ph); }
                default -> { y[i] += Math.sin(t * 0.4 + ph) * dt * 0.05; }
            }
            // Wrap into the box around the camera.
            x[i] = wrap(x[i], f.camX, r);
            y[i] = wrap(y[i], f.camY, r * 0.6);
            z[i] = wrap(z[i], f.camZ, r);
            double dx = x[i] - f.camX, dy = y[i] - f.camY, dz = z[i] - f.camZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 0.8) continue;
            double edge = Math.max(0, 1 - dist / r);
            double fade = Math.min(1, (dist - 0.8) / 1.5) * Math.min(1, edge * 3);
            switch (s) {
                case "Fireflies" -> {
                    double pulse = 0.4 + 0.6 * Math.max(0, Math.sin(t * 1.7 + ph * 5));
                    int warm = colorMode.value().equals(EffectPalette.MONO) ? 0xE8F7A0 : rgb;
                    FxShapes.glowDot(f, x[i], y[i], z[i], 0.035 * sz, warm, a * fade * pulse, true, false);
                }
                case "Embers" -> FxShapes.glowDot(f, x[i], y[i], z[i], 0.03 * sz,
                        colorMode.value().equals(EffectPalette.MONO) ? 0xFF8A3D : rgb, a * fade, true, false);
                case "Stardust" -> {
                    double tw = 0.5 + 0.5 * Math.sin(t * 3 + ph * 7);
                    FxShapes.star(f, x[i], y[i], z[i], 0.05 * sz * (0.6 + tw * 0.4), ph + t * 0.3, 4, rgb, a * fade * tw, false);
                }
                case "Light streaks" -> {
                    if (i % 6 != 0) continue;
                    FxShapes.spark(f, x[i], y[i], z[i], Math.cos(ph) * 2.5, -6, Math.sin(ph) * 2.5, 3.5 * sz, 0.02 * sz, rgb, a * fade * 0.8, false);
                }
                case "Rain streaks" -> FxShapes.spark(f, x[i], y[i], z[i], 0.08, -1, 0, 0.9 * sz, 0.012 * sz, rgb, a * fade * 0.6, false);
                case "Dust" -> FxShapes.softDot(f, x[i], y[i], z[i], 0.025 * sz, rgb, a * fade * 0.7, false);
                default -> FxShapes.softDot(f, x[i], y[i], z[i], 0.045 * sz * (0.7 + 0.3 * Math.sin(ph)), rgb, a * fade, false);
            }
        }
    }

    private static double wrap(double value, double center, double half) {
        double span = half * 2;
        double offset = value - (center - half);
        offset -= Math.floor(offset / span) * span;
        return center - half + offset;
    }
}
