package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.model.BodyAnchor;
import dev.jilaton.client.render.model.Material;
import dev.jilaton.client.render.model.Mesh;
import dev.jilaton.client.render.model.Meshes;
import dev.jilaton.client.render.model.ModelBatch;
import dev.jilaton.client.render.model.Transform;
import dev.jilaton.client.render.model.WingMotion;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Layered 3D wings on the upper back: every feather is a tapered, folded quad strip with its own normals,
 * fanned from the leading edge down to the tip. The flap is stepped from {@link FxFrame#dt}, and each
 * feather samples the wave later than the one above it, so the wing trails the shoulder instead of moving
 * as one rigid plate. Geometry is code-generated, shared per style and quality, and never billboarded.
 */
public final class WingsModule extends ClientModule implements FxEmitter {
    private static final int MAX_PLAYERS = 16;
    private static final double MAX_DISTANCE_SQR = 64 * 64;

    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Style", WingMotion.STYLES, "Feather"));
    private final ChoiceSetting palette = addSetting(EffectPalette.modeSetting("palette", "Accent"));
    private final TextSetting custom = addSetting(EffectPalette.hexSetting("custom", "Custom accent", "B9A7FF"));
    private final NumberSetting span = addSetting(new NumberSetting("span", "Span", 0.55, 1.7, 0.05, 1.0));
    private final NumberSetting flapSpeed = addSetting(new NumberSetting("flap_speed", "Flap speed", 0.2, 2.6, 0.1, 0.85));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.15, 1, 0.05, 0.9));
    private final BooleanSetting animate = addSetting(new BooleanSetting("animate", "Animate flap", true));
    private final BooleanSetting own = addSetting(new BooleanSetting("own", "Show on yourself", true));
    private final BooleanSetting others = addSetting(new BooleanSetting("others", "Show on others", true));

    private final ModelBatch batch = new ModelBatch();
    private Mesh mesh;
    private String meshKey = "";
    private double phase;
    private Object boundLevel;

    public WingsModule() {
        super("wings", "Wings", "Layered 3D wings on the upper back with a trailing feather flap.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        phase = 0;
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
        }
        boolean still = f.reducedMotion || !animate.value();
        phase = WingMotion.advancedPhase(phase, f.dt, flapSpeed.value(), still);

        String selected = style.value();
        Mesh built = mesh(f, selected);
        int quality = f.quality.ordinal();
        int accent = EffectPalette.resolve(palette.value(), f.accent, custom.value());
        Material material = material(selected, accent);
        double alpha = opacity.value() * Math.min(1, Math.max(0, f.intensity));
        if (alpha <= 0.01) return;
        double scale = span.value();
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
            Transform back = BodyAnchor.forPart(player, BodyAnchor.Part.BACK, f.partial);
            Transform shoulder = back.translate(0, 0.12, 0.02);
            for (int side = -1; side <= 1; side += 2) {
                Transform wing = shoulder.translate(side * 0.10, 0, 0);
                addSide(f, built, material, wing, side, selected, quality, alpha, scale);
            }
        }
        batch.flush(f);
    }

    private void addSide(FxFrame f, Mesh built, Material material, Transform wing, int side,
                         String styleName, int quality, double alpha, double scale) {
        int layers = WingMotion.layers(styleName);
        int featherIndex = 0;
        for (int layer = 0; layer < layers; layer++) {
            int count = WingMotion.feathers(styleName, layer, quality);
            for (int i = 0; i < count; i++) {
                double fan = Math.toRadians(WingMotion.fanDegrees(i, count));
                double dirX = side * Math.cos(fan);
                double dirY = Math.sin(fan);
                double dirZ = -0.16;
                double flap = WingMotion.flapAngle(phase, featherIndex++,
                        WingMotion.baseFlap(styleName), WingMotion.amplitude(styleName));
                Transform pivot = wing.translate(side * (0.02 + 0.02 * layer),
                        WingMotion.rootHeight(layer), WingMotion.layerDepth(layer));
                Transform feather = pivot.rotate(0, 0, Math.toDegrees(flap) * side)
                        .compose(Transform.lookAlong(dirX, dirY, dirZ, 0, 1, 0));
                double length = WingMotion.featherLength(layer, i, count) * WingMotion.spread(styleName) * scale;
                batch.add(f, built, material, feather, length, alpha, false);
            }
        }
    }

    private Mesh mesh(FxFrame f, String styleName) {
        String key = styleName + "/" + f.quality.name();
        if (mesh == null || !key.equals(meshKey)) {
            int segments = WingMotion.segments(styleName, f.quality.ordinal());
            mesh = Meshes.strip(segments, WingMotion.halfWidth(styleName), WingMotion.tipWidth(styleName),
                    WingMotion.fold(styleName), WingMotion.bend(styleName));
            meshKey = key;
        }
        return mesh;
    }

    /** Dark, tinted roots to a pale lit tip; Energy pushes almost all of the colour into the emission. */
    private static Material material(String styleName, int accent) {
        return switch (styleName) {
            case "Blade" -> new Material(EffectBatch.mixRgb(accent, 0x101216, 0.84),
                    EffectBatch.mixRgb(accent, 0xE8ECF4, 0.72), accent, 0.16);
            case "Energy" -> new Material(EffectBatch.mixRgb(accent, 0x07060C, 0.55),
                    EffectBatch.mixRgb(accent, 0xFFFFFF, 0.30), accent, 0.92);
            default -> new Material(EffectBatch.mixRgb(accent, 0x121016, 0.78),
                    EffectBatch.mixRgb(accent, 0xF4F2F6, 0.66), accent, 0.24);
        };
    }
}
