package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxParticles;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Random;

/**
 * Selectable ambient weather built from the shared particle pool: glassy rain streaks, drifting snow that
 * settles briefly where it lands, and a shower of rising embers. Puddles and terrain wetting are not
 * faked: the current pipeline has no surface-state hook, so only the particle and air layers are drawn.
 */
public final class WeatherModule extends ClientModule implements FxEmitter {
    private static final int OWNER = FxParticles.ownerId("weather");
    private static final int RAIN_RGB = 0xD8E4FF;
    private static final int SNOW_RGB = 0xF2F6FF;
    private static final int EMBER_RGB = 0xFF9A5A;

    private final ChoiceSetting mode = addSetting(new ChoiceSetting("mode", "Weather",
            List.of("Glassy Rain", "Settling Snow", "Ember Shower"), "Glassy Rain"));
    private final ChoiceSetting tint = addSetting(new ChoiceSetting("tint", "Tint",
            List.of("Mode default", "Theme", "Custom"), "Mode default"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "D8E4FF"));
    private final NumberSetting density = addSetting(new NumberSetting("density", "Density", 0.1, 2, 0.05, 0.8));
    private final NumberSetting radius = addSetting(new NumberSetting("radius", "Radius", 6, 40, 1, 16));
    private final NumberSetting speed = addSetting(new NumberSetting("speed", "Fall speed", 0.4, 2.5, 0.05, 1));
    private final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Glow", true));
    private final BooleanSetting covered = addSetting(new BooleanSetting("covered", "Pause under cover", false));

    private final FxParticles.Spec spec = new FxParticles.Spec();
    private final Random random = new Random();
    private double carry;
    private double hazeCarry;

    public WeatherModule() {
        super("weather", "Weather", "Glassy rain, settling snow or an ember shower around the player.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        FxParticles.get().clearOwner(OWNER);
        carry = 0;
        hazeCarry = 0;
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        if (client.level == null || client.player == null) return;
        if (covered.value() && !client.level.canSeeSky(client.player.blockPosition())) return;
        double dt = Math.min(0.1, Math.max(0, f.dt));
        double intensity = Math.min(1, f.intensity) * density.value() * (f.reducedMotion ? 0.55 : 1);
        double reach = radius.value();
        int rgb = tint(f.accent);
        String style = mode.value();

        double rate = switch (style) {
            case "Settling Snow" -> 130;
            case "Ember Shower" -> 90;
            default -> 320;
        };
        carry += rate * intensity * dt;
        int spawn = Math.min(48, (int) carry);
        carry -= spawn;
        for (int i = 0; i < spawn; i++) spawn(f, style, rgb, reach);

        hazeCarry += 5 * intensity * dt;
        int haze = Math.min(4, (int) hazeCarry);
        hazeCarry -= haze;
        for (int i = 0; i < haze; i++) haze(f, style, rgb, reach);
    }

    private int tint(int accent) {
        return switch (tint.value()) {
            case "Theme" -> accent & 0xFFFFFF;
            case "Custom" -> EffectPalette.resolve(EffectPalette.CUSTOM, accent, color.value());
            default -> switch (mode.value()) {
                case "Settling Snow" -> SNOW_RGB;
                case "Ember Shower" -> EMBER_RGB;
                default -> RAIN_RGB;
            };
        };
    }

    private void spawn(FxFrame f, String style, int rgb, double reach) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = Math.sqrt(random.nextDouble()) * reach;
        double x = f.camX + Math.cos(angle) * distance;
        double z = f.camZ + Math.sin(angle) * distance;
        double top = f.camY + 7 + random.nextDouble() * 6;
        double ground = FxParticles.groundY(f.client.level, x, top, z, 20);
        spec.reset();
        spec.owner = OWNER;
        spec.walls = false;
        switch (style) {
            case "Settling Snow" -> {
                spec.x = x;
                spec.y = top - random.nextDouble() * 4;
                spec.z = z;
                spec.vx = (random.nextDouble() - 0.5) * 0.5;
                spec.vz = (random.nextDouble() - 0.5) * 0.5;
                spec.vy = -1.6 * speed.value();
                spec.gravity = 0.05;
                spec.drag = 0.92;
                spec.floor = ground;
                spec.life = 3.2 + random.nextDouble() * 2.8;
                spec.size0 = 0.07 + random.nextDouble() * 0.045;
                spec.size1 = 0.05;
                spec.alpha = 0.9;
                spec.color = rgb;
                spec.shape = FxParticles.Shape.DOT;
                spec.glow = glow.value();
            }
            case "Ember Shower" -> {
                spec.x = x;
                spec.y = Double.isNaN(ground) ? f.camY - 1 + random.nextDouble() * 3 : ground + 0.1;
                spec.z = z;
                spec.vx = (random.nextDouble() - 0.5) * 0.35;
                spec.vz = (random.nextDouble() - 0.5) * 0.35;
                spec.vy = (0.5 + random.nextDouble() * 0.8) * speed.value();
                spec.gravity = -0.3;
                spec.drag = 0.9;
                spec.life = 2 + random.nextDouble() * 1.8;
                spec.size0 = 0.05 + random.nextDouble() * 0.03;
                spec.size1 = 0.015;
                spec.alpha = 0.85;
                spec.color = rgb;
                spec.color1 = EffectBatch.mixRgb(rgb, 0x401000, 0.55);
                spec.shape = FxParticles.Shape.DOT;
                spec.glow = glow.value();
            }
            default -> {
                spec.x = x;
                spec.y = top;
                spec.z = z;
                spec.vx = -0.9 * speed.value();
                spec.vz = 0.4 * speed.value();
                spec.vy = -14 * speed.value();
                spec.gravity = 0.5;
                spec.drag = 1.0;
                spec.floor = ground;
                spec.life = Double.isNaN(ground) ? 1.2 : Math.max(0.2, Math.min(1.4, (top - ground) / (14 * speed.value())));
                spec.size0 = 0.075;
                spec.size1 = 0.035;
                spec.alpha = 0.62;
                spec.color = rgb;
                spec.shape = FxParticles.Shape.SPARK;
                spec.glow = glow.value();
            }
        }
        FxParticles.get().spawn(spec);
    }

    /** Low, wide, slow haze so the air itself has body; one shape per style keeps the palette coherent. */
    private void haze(FxFrame f, String style, int rgb, double reach) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = Math.sqrt(random.nextDouble()) * reach * 0.7;
        double x = f.camX + Math.cos(angle) * distance;
        double z = f.camZ + Math.sin(angle) * distance;
        double ground = FxParticles.groundY(f.client.level, x, f.camY + 1, z, 10);
        boolean ember = style.equals("Ember Shower");
        spec.reset();
        spec.owner = OWNER;
        spec.x = x;
        spec.y = Double.isNaN(ground) ? f.camY - 0.5 : ground + 0.15;
        spec.z = z;
        spec.vx = (random.nextDouble() - 0.5) * 0.2;
        spec.vz = (random.nextDouble() - 0.5) * 0.2;
        spec.vy = ember ? 0.25 : -0.05;
        spec.gravity = ember ? -0.08 : 0;
        spec.drag = 0.95;
        spec.life = 3.5 + random.nextDouble() * 2.5;
        spec.size0 = ember ? 0.5 + random.nextDouble() * 0.6 : 1.1 + random.nextDouble() * 1.1;
        spec.size1 = spec.size0 * 1.6;
        spec.alpha = ember ? 0.05 : 0.045;
        spec.color = rgb;
        spec.shape = FxParticles.Shape.SMOKE;
        spec.glow = false;
        FxParticles.get().spawn(spec);
    }
}
