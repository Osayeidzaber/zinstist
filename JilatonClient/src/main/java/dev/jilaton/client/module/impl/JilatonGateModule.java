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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Renders a 3D Jilaton-branded obsidian monolith gateway in world space with architectural
 * pillars, animated vortex portal interior, a floating 3D Jilaton emblem, and rising motes.
 */
public final class JilatonGateModule extends ClientModule implements FxEmitter {
    public static final List<String> STYLES = List.of("Obsidian Monolith", "Cyber Arch", "Void Sanctuary");

    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Gate style", STYLES, "Obsidian Monolith"));
    private final ChoiceSetting anchorMode = addSetting(new ChoiceSetting("anchor", "Placement",
            List.of("World Anchor", "Follow Ahead"), "World Anchor"));
    private final NumberSetting distance = addSetting(new NumberSetting("distance", "Spawn distance", 2.5, 12.0, 0.5, 4.5));
    private final NumberSetting scale = addSetting(new NumberSetting("scale", "Gate scale", 0.6, 2.0, 0.05, 1.0));
    private final BooleanSetting logoEmblem = addSetting(new BooleanSetting("logo_emblem", "3D Jilaton emblem", true));
    private final BooleanSetting vortex = addSetting(new BooleanSetting("vortex", "Animated portal interior", true));
    private final BooleanSetting particles = addSetting(new BooleanSetting("particles", "Sanctuary particles", true));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "8878FF"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.2, 1.0, 0.05, 0.92));

    private boolean anchored;
    private double anchorX, anchorY, anchorZ;
    private double anchorYawRad;
    private Object boundLevel;
    private double particleTimer;

    public JilatonGateModule() {
        super("jilaton_gate", "JilatonGate",
                "3D Jilaton-branded obsidian gateway with animated vortex interior and floating 3D emblem.",
                ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable(Minecraft client) {
        anchored = false;
        if (client != null && client.player != null && client.level != null) {
            captureAnchor(client);
        }
    }

    @Override
    protected void onDisable(Minecraft client) {
        anchored = false;
        boundLevel = null;
    }

    private void captureAnchor(Minecraft client) {
        Vec3 pos = client.player.position();
        double yawRad = Math.toRadians(client.player.getYRot());
        double dist = distance.value();
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        anchorX = pos.x + fwdX * dist;
        anchorZ = pos.z + fwdZ * dist;
        double ground = FxParticles.groundY(client.level, anchorX, pos.y + 2.0, anchorZ, 8);
        anchorY = Double.isFinite(ground) ? ground : pos.y;
        anchorYawRad = yawRad;
        anchored = true;
        boundLevel = client.level;
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        if (client.level == null || client.player == null) return;
        if (!anchored || client.level != boundLevel || anchorMode.value().equals("Follow Ahead")) {
            captureAnchor(client);
        }
        double distSqr = client.player.distanceToSqr(anchorX, anchorY, anchorZ);
        if (distSqr > 64.0 * 64.0 && anchorMode.value().equals("World Anchor")) {
            captureAnchor(client);
        }

        int accentRgb = EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        double baseAlpha = opacity.value() * Math.min(1.0, f.intensity);
        double sec = f.reducedMotion ? 0.0 : f.seconds();
        double s = scale.value();

        emitGate(f.batch, style.value(),
                anchorX, anchorY, anchorZ, anchorYawRad,
                f.camX, f.camY, f.camZ,
                s, accentRgb, baseAlpha,
                logoEmblem.value(), vortex.value(), sec);

        if (particles.value() && !f.reducedMotion) {
            particleTimer += f.dt;
            if (particleTimer >= 0.08) {
                particleTimer = 0;
                double rightX = Math.cos(anchorYawRad);
                double rightZ = Math.sin(anchorYawRad);
                double spread = (Math.random() - 0.5) * 1.6 * s;
                double px = anchorX + rightX * spread;
                double pz = anchorZ + rightZ * spread;
                double py = anchorY + 0.15 + Math.random() * 2.6 * s;
                FxParticles.get().add(px, py, pz,
                        0, 0.45 + Math.random() * 0.35, 0,
                        -0.1, 1.2, Double.NaN, 1.1,
                        0.06 * s, 0.015 * s, accentRgb, 0xFFFFFF, baseAlpha * 0.85,
                        FxParticles.Shape.CRYSTAL, true, false, 1.5, 9201);
            }
        }
    }

    /**
     * Emits the 3D Jilaton gateway structure, obsidian pillars, animated portal membrane,
     * and 3D Jilaton emblem in camera-relative coordinates. Pure geometry for headless testing.
     */
    public static void emitGate(EffectBatch batch, String style,
                                double worldX, double worldY, double worldZ, double yawRad,
                                double camX, double camY, double camZ,
                                double scale, int accentRgb, double alpha,
                                boolean drawLogo, boolean drawVortex, double seconds) {
        if (batch == null || alpha <= 0.005 || scale <= 0.05) return;
        double cx = worldX - camX;
        double cy = worldY - camY;
        double cz = worldZ - camZ;

        double rx = Math.cos(yawRad);
        double rz = Math.sin(yawRad);
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);

        double halfW = 1.15 * scale;
        double pillarW = 0.26 * scale;
        double pillarD = 0.28 * scale;
        double gateH = 3.25 * scale;
        double archH = 0.48 * scale;

        int obsidianDark = EffectBatch.withAlpha(0x0B0B10, Math.min(1.0, alpha * 0.92));
        int obsidianEdge = EffectBatch.withAlpha(EffectBatch.mixRgb(accentRgb, 0x252534, 0.55), alpha * 0.95);
        int brightRim = EffectBatch.withAlpha(EffectBatch.mixRgb(accentRgb, 0xFFFFFF, 0.45), alpha);

        // Left & Right 3D Obsidian Pillars + Base Plinths
        for (int side = -1; side <= 1; side += 2) {
            double uCenter = side * halfW;
            emitOrientedBox(batch, cx, cy, cz, rx, rz, fx, fz,
                    uCenter - pillarW * 0.75, 0, -pillarD * 0.75,
                    uCenter + pillarW * 0.75, 0.28 * scale, pillarD * 0.75,
                    obsidianDark, brightRim, 1.6F);
            emitOrientedBox(batch, cx, cy, cz, rx, rz, fx, fz,
                    uCenter - pillarW * 0.5, 0.28 * scale, -pillarD * 0.5,
                    uCenter + pillarW * 0.5, gateH, pillarD * 0.5,
                    obsidianDark, obsidianEdge, 1.8F);
        }

        // Top Lintel & Crown Arch
        emitOrientedBox(batch, cx, cy, cz, rx, rz, fx, fz,
                -halfW - pillarW * 0.65, gateH, -pillarD * 0.6,
                halfW + pillarW * 0.65, gateH + archH * 0.65, pillarD * 0.6,
                obsidianDark, brightRim, 2.0F);
        if (!"Cyber Arch".equals(style)) {
            emitOrientedBox(batch, cx, cy, cz, rx, rz, fx, fz,
                    -halfW * 0.65, gateH + archH * 0.65, -pillarD * 0.42,
                    halfW * 0.65, gateH + archH * 1.15, pillarD * 0.42,
                    obsidianDark, brightRim, 1.7F);
        }

        // Threshold floor seal
        emitOrientedBox(batch, cx, cy, cz, rx, rz, fx, fz,
                -halfW, 0.01, -pillarD * 0.55,
                halfW, 0.08 * scale, pillarD * 0.55,
                obsidianDark, obsidianEdge, 1.5F);

        // Animated Portal Interior (Energy Membrane + Swirling Vortex)
        if (drawVortex) {
            double innerW = halfW - pillarW * 0.52;
            double innerY0 = 0.08 * scale;
            double innerY1 = gateH;
            int strips = 6;
            for (int i = 0; i < strips; i++) {
                double t0 = i / (double) strips;
                double t1 = (i + 1) / (double) strips;
                double y0 = innerY0 + (innerY1 - innerY0) * t0;
                double y1 = innerY0 + (innerY1 - innerY0) * t1;
                double pulse0 = 0.5 + 0.5 * Math.sin(seconds * 2.4 + t0 * 4.5);
                double pulse1 = 0.5 + 0.5 * Math.sin(seconds * 2.4 + t1 * 4.5);
                int col0 = EffectBatch.withAlpha(EffectBatch.mixRgb(0x0D0C18, accentRgb, 0.35 + 0.45 * pulse0), alpha * 0.42);
                int col1 = EffectBatch.withAlpha(EffectBatch.mixRgb(0x0D0C18, accentRgb, 0.35 + 0.45 * pulse1), alpha * 0.42);
                batch.quad(
                        cx - rx * innerW, cy + y0, cz - rz * innerW,
                        cx + rx * innerW, cy + y0, cz + rz * innerW,
                        cx + rx * innerW, cy + y1, cz + rz * innerW,
                        cx - rx * innerW, cy + y1, cz - rz * innerW,
                        col0, col0, col1, col1, false
                );
            }

            // Swirling vortex rings inside the portal plane
            double midY = (innerY0 + innerY1) * 0.5;
            for (int ringIdx = 0; ringIdx < 3; ringIdx++) {
                double rr = (0.32 + ringIdx * 0.24) * scale;
                int segs = 20;
                double spin = seconds * (1.6 - ringIdx * 0.45) * (ringIdx % 2 == 0 ? 1 : -1);
                int ringCol = EffectBatch.withAlpha(
                        EffectBatch.mixRgb(accentRgb, 0xFFFFFF, 0.3 + ringIdx * 0.2),
                        alpha * (0.78 - ringIdx * 0.16));
                for (int i = 0; i < segs; i++) {
                    double a0 = spin + i * (Math.PI * 2.0 / segs);
                    double a1 = spin + (i + 1) * (Math.PI * 2.0 / segs);
                    double u0 = Math.cos(a0) * rr, v0 = Math.sin(a0) * rr * 1.35;
                    double u1 = Math.cos(a1) * rr, v1 = Math.sin(a1) * rr * 1.35;
                    double zOff = Math.sin(a0 * 2 + seconds * 2.0) * 0.06 * scale;
                    batch.glowLine(
                            cx + rx * u0 + fx * zOff, cy + midY + v0, cz + rz * u0 + fz * zOff,
                            cx + rx * u1 + fx * zOff, cy + midY + v1, cz + rz * u1 + fz * zOff,
                            ringCol, 1.5F, false
                    );
                }
            }
        }

        // 3D Jilaton Logo Emblem at the Portal Crown & Center
        if (drawLogo) {
            double emblemY = cy + gateH + archH * 0.42 + Math.sin(seconds * 2.0) * 0.04 * scale;
            double er = 0.34 * scale;
            int logoCol = EffectBatch.withAlpha(0xFFFFFF, Math.min(1.0, alpha * 0.98));
            int logoGlow = EffectBatch.withAlpha(accentRgb, alpha * 0.88);
            double depth = 0.08 * scale;

            // Outer 3D Octahedral Diamond Crest
            double[] top = {cx, emblemY + er, cz};
            double[] bot = {cx, emblemY - er, cz};
            double[] lft = {cx - rx * er, emblemY, cz - rz * er};
            double[] rgt = {cx + rx * er, emblemY, cz + rz * er};
            double[] frt = {cx + fx * depth, emblemY, cz + fz * depth};
            double[] bck = {cx - fx * depth, emblemY, cz - fz * depth};

            batch.glowLine(top[0], top[1], top[2], lft[0], lft[1], lft[2], logoGlow, 2.0F, false);
            batch.glowLine(lft[0], lft[1], lft[2], bot[0], bot[1], bot[2], logoGlow, 2.0F, false);
            batch.glowLine(bot[0], bot[1], bot[2], rgt[0], rgt[1], rgt[2], logoGlow, 2.0F, false);
            batch.glowLine(rgt[0], rgt[1], rgt[2], top[0], top[1], top[2], logoGlow, 2.0F, false);
            batch.line(top[0], top[1], top[2], frt[0], frt[1], frt[2], logoCol, 1.4F, false);
            batch.line(bot[0], bot[1], bot[2], frt[0], frt[1], frt[2], logoCol, 1.4F, false);
            batch.line(top[0], top[1], top[2], bck[0], bck[1], bck[2], logoCol, 1.4F, false);
            batch.line(bot[0], bot[1], bot[2], bck[0], bck[1], bck[2], logoCol, 1.4F, false);

            // Geometric 'J' Monogram inside the crest
            double jw = er * 0.36;
            double jh = er * 0.52;
            // Top serif of J
            batch.glowLine(cx - rx * jw * 0.5, emblemY + jh, cz - rz * jw * 0.5,
                    cx + rx * jw, emblemY + jh, cz + rz * jw, logoCol, 2.2F, false);
            // Vertical stem of J
            batch.glowLine(cx + rx * jw * 0.45, emblemY + jh, cz + rz * jw * 0.45,
                    cx + rx * jw * 0.45, emblemY - jh * 0.65, cz + rz * jw * 0.45, logoCol, 2.2F, false);
            // Lower hook of J
            batch.glowLine(cx + rx * jw * 0.45, emblemY - jh * 0.65, cz + rz * jw * 0.45,
                    cx - rx * jw * 0.35, emblemY - jh * 0.9, cz - rz * jw * 0.35, logoCol, 2.2F, false);
            batch.glowLine(cx - rx * jw * 0.35, emblemY - jh * 0.9, cz - rz * jw * 0.35,
                    cx - rx * jw * 0.85, emblemY - jh * 0.35, cz - rz * jw * 0.85, logoCol, 2.2F, false);
        }
    }

    private static void emitOrientedBox(EffectBatch batch,
                                        double cx, double cy, double cz,
                                        double rx, double rz, double fx, double fz,
                                        double u0, double y0, double v0,
                                        double u1, double y1, double v1,
                                        int fillColor, int edgeColor, float lineWidth) {
        double x00 = cx + rx * u0 + fx * v0, z00 = cz + rz * u0 + fz * v0;
        double x10 = cx + rx * u1 + fx * v0, z10 = cz + rz * u1 + fz * v0;
        double x11 = cx + rx * u1 + fx * v1, z11 = cz + rz * u1 + fz * v1;
        double x01 = cx + rx * u0 + fx * v1, z01 = cz + rz * u0 + fz * v1;
        double py0 = cy + y0, py1 = cy + y1;

        // Faces
        batch.quad(x00, py0, z00, x10, py0, z10, x10, py1, z10, x00, py1, z00,
                fillColor, fillColor, fillColor, fillColor, false);
        batch.quad(x01, py0, z01, x11, py0, z11, x11, py1, z11, x01, py1, z01,
                fillColor, fillColor, fillColor, fillColor, false);
        batch.quad(x00, py0, z00, x01, py0, z01, x01, py1, z01, x00, py1, z00,
                fillColor, fillColor, fillColor, fillColor, false);
        batch.quad(x10, py0, z10, x11, py0, z11, x11, py1, z11, x10, py1, z10,
                fillColor, fillColor, fillColor, fillColor, false);
        batch.quad(x00, py1, z00, x10, py1, z10, x11, py1, z11, x01, py1, z01,
                fillColor, fillColor, fillColor, fillColor, false);

        // Edges
        batch.glowLine(x00, py0, z00, x10, py0, z10, edgeColor, lineWidth, false);
        batch.glowLine(x10, py0, z10, x11, py0, z11, edgeColor, lineWidth, false);
        batch.glowLine(x11, py0, z11, x01, py0, z01, edgeColor, lineWidth, false);
        batch.glowLine(x01, py0, z01, x00, py0, z00, edgeColor, lineWidth, false);

        batch.glowLine(x00, py1, z00, x10, py1, z10, edgeColor, lineWidth, false);
        batch.glowLine(x10, py1, z10, x11, py1, z11, edgeColor, lineWidth, false);
        batch.glowLine(x11, py1, z11, x01, py1, z01, edgeColor, lineWidth, false);
        batch.glowLine(x01, py1, z01, x00, py1, z00, edgeColor, lineWidth, false);

        batch.glowLine(x00, py0, z00, x00, py1, z00, edgeColor, lineWidth, false);
        batch.glowLine(x10, py0, z10, x10, py1, z10, edgeColor, lineWidth, false);
        batch.glowLine(x11, py0, z11, x11, py1, z11, edgeColor, lineWidth, false);
        batch.glowLine(x01, py0, z01, x01, py1, z01, edgeColor, lineWidth, false);
    }
}
