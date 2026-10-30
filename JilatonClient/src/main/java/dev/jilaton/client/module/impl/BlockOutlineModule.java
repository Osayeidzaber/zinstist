package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Glowing outline around the block under the crosshair. The box glides between blocks on its own
 * critically-damped springs driven by frame time, and fades out shortly after the crosshair leaves a
 * block, so losing the selection never snaps the outline off.
 */
public final class BlockOutlineModule extends ClientModule implements FxEmitter {
    private static final double SNAP_EPSILON = 1e-3;
    private static final double MAX_STEP = 1.0 / 120.0;
    private static final int VIOLET = 0x9A7CFF;

    private final ChoiceSetting colorMode = addSetting(new ChoiceSetting("color_mode", "Color",
            List.of("Violet", "Mono", "Theme", "Custom"), "Violet"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "9A7CFF"));
    private final NumberSetting width = addSetting(new NumberSetting("width", "Line width", 1, 6, 0.5, 2.5));
    private final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Glow halo", true));
    private final NumberSetting fill = addSetting(new NumberSetting("fill", "Face fill", 0, 0.3, 0.01, 0.06));
    private final NumberSetting fadeTime = addSetting(new NumberSetting("fade_time", "Fade-out (seconds)", 0.05, 0.6, 0.05, 0.2));
    private final NumberSetting glide = addSetting(new NumberSetting("glide", "Glide strength", 0, 1, 0.05, 0.6));

    private final Spring[] box = {new Spring(), new Spring(), new Spring(), new Spring(), new Spring(), new Spring()};
    private double fade;
    private boolean tracking;

    public BlockOutlineModule() {
        super("block_outline", "Block Outline", "Violet glowing outline of the targeted block, gliding between blocks.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        tracking = false;
        fade = 0;
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        if (client.level == null || client.player == null) return;
        AABB selection = selection(client);
        double dt = Math.min(0.1, Math.max(0, f.dt));
        if (selection != null) {
            pointAt(selection);
            tracking = true;
            fade = Math.min(1, fade + dt / 0.1);
        } else if (tracking) {
            fade -= dt / fadeTime.value();
            if (fade <= 0) {
                fade = 0;
                tracking = false;
                return;
            }
        }
        if (!tracking || fade <= 0.004) return;
        advance(f, dt);
        draw(f, outlineColor(f.accent), fade * Math.min(1, f.intensity));
    }

    private int outlineColor(int accent) {
        return switch (colorMode.value()) {
            case "Mono" -> EffectPalette.MONO_RGB;
            case "Theme" -> accent & 0xFFFFFF;
            case "Custom" -> EffectPalette.resolve(EffectPalette.CUSTOM, accent, color.value());
            default -> VIOLET;
        };
    }

    /** The selected block's solid bounds, or null when the crosshair is on an entity, air or a screen is open. */
    private static AABB selection(Minecraft client) {
        if (client.screen != null) return null;
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = hit.getBlockPos();
        if (client.level == null || !client.level.isLoaded(pos)) return null;
        VoxelShape shape = client.level.getBlockState(pos).getCollisionShape(client.level, pos);
        AABB bounds = shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
        return bounds.inflate(0.002);
    }

    private void pointAt(AABB target) {
        double[] values = {target.minX, target.minY, target.minZ, target.maxX, target.maxY, target.maxZ};
        if (!tracking) {
            snap(values);
            return;
        }
        for (int i = 0; i < box.length; i++) box[i].target = values[i];    }

    private void advance(FxFrame f, double dt) {
        if (f.reducedMotion) {
            for (Spring spring : box) spring.snap();
            return;
        }
        double stiffness = 140 + glide.value() * 620;
        double damping = 2 * Math.sqrt(stiffness) * 0.9;
        double remaining = dt;
        while (remaining > 1e-6) {
            double step = Math.min(remaining, MAX_STEP);
            for (Spring spring : box) spring.step(stiffness, damping, step);
            remaining -= step;
        }
    }

    private void snap(double[] values) {
        for (int i = 0; i < box.length; i++) box[i].snap(values[i]);
    }

    private void draw(FxFrame f, int rgb, double alpha) {
        // The springs hold world positions; the batch wants camera-relative vertices, so the camera offset
        // is applied once here instead of at every edge.
        double minX = box[0].value - f.camX, minY = box[1].value - f.camY, minZ = box[2].value - f.camZ;
        double maxX = box[3].value - f.camX, maxY = box[4].value - f.camY, maxZ = box[5].value - f.camZ;
        if (maxX - minX < SNAP_EPSILON || maxY - minY < SNAP_EPSILON || maxZ - minZ < SNAP_EPSILON) return;
        double fillAlpha = fill.value();
        if (fillAlpha > 0.002) faces(f, rgb, alpha * fillAlpha, minX, minY, minZ, maxX, maxY, maxZ);
        int core = EffectBatch.withAlpha(rgb, alpha * 0.92);
        float lineWidth = (float) width.value();
        edge(f, minX, minY, minZ, maxX, minY, minZ, core, lineWidth);
        edge(f, maxX, minY, minZ, maxX, minY, maxZ, core, lineWidth);
        edge(f, maxX, minY, maxZ, minX, minY, maxZ, core, lineWidth);
        edge(f, minX, minY, maxZ, minX, minY, minZ, core, lineWidth);
        edge(f, minX, maxY, minZ, maxX, maxY, minZ, core, lineWidth);
        edge(f, maxX, maxY, minZ, maxX, maxY, maxZ, core, lineWidth);
        edge(f, maxX, maxY, maxZ, minX, maxY, maxZ, core, lineWidth);
        edge(f, minX, maxY, maxZ, minX, maxY, minZ, core, lineWidth);
        edge(f, minX, minY, minZ, minX, maxY, minZ, core, lineWidth);
        edge(f, maxX, minY, minZ, maxX, maxY, minZ, core, lineWidth);
        edge(f, maxX, minY, maxZ, maxX, maxY, maxZ, core, lineWidth);
        edge(f, minX, minY, maxZ, minX, maxY, maxZ, core, lineWidth);
    }

    private void edge(FxFrame f, double x1, double y1, double z1, double x2, double y2, double z2, int color, float lineWidth) {
        if (glow.value()) f.batch.glowLine(x1, y1, z1, x2, y2, z2, color, lineWidth, false);
        else f.batch.line(x1, y1, z1, x2, y2, z2, color, lineWidth, false);
    }

    private static void faces(FxFrame f, int rgb, double alpha, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        int color = EffectBatch.withAlpha(rgb, alpha);
        f.batch.quad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, color, color, color, color, false);
        f.batch.quad(minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, color, color, color, color, false);
        f.batch.quad(minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, color, color, color, color, false);
        f.batch.quad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color, color, color, color, false);
        f.batch.quad(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, color, color, color, color, false);
        f.batch.quad(maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color, color, color, color, false);
    }

    /** One axis of the glide, integrated on frame time; overshoot is damped to just under critical. */
    private static final class Spring {
        private double value;
        private double velocity;
        private double target;

        void snap(double to) {
            value = to;
            target = to;
            velocity = 0;
        }

        void snap() {
            value = target;
            velocity = 0;
        }

        void step(double stiffness, double damping, double dt) {
            velocity += ((target - value) * stiffness - velocity * damping) * dt;
            value += velocity * dt;
        }
    }
}
