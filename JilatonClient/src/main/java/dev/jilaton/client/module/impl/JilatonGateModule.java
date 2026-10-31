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
 * A Jilaton-branded gateway anchored in the world: lit stone pillars and lintel with emissive accent grooves, a
 * filled arched portal membrane that swirls inward, a bright event-horizon rim, floor light spill, orbiting runes,
 * and a solid crest emblem with the J monogram.
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
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "F2F2F2"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.2, 1.0, 0.05, 0.92));

    private boolean anchored;
    private double anchorX, anchorY, anchorZ;
    private double anchorYawRad;
    private Object boundLevel;
    private double particleTimer;

    public JilatonGateModule() {
        super("jilaton_gate", "JilatonGate",
                "3D Jilaton-branded gateway with a swirling portal membrane and floating crest emblem.",
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

    /** World anchor of the gate base, for the dev capture log. */
    public String anchorDescription() {
        return anchored ? String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f yaw %.1f", anchorX, anchorY, anchorZ, Math.toDegrees(anchorYawRad)) : "unanchored";
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
            while (particleTimer >= 0.05) {
                particleTimer -= 0.05;
                // Motes are drawn out of the membrane toward its centre, then drift up and out through the front.
                double rightX = Math.cos(anchorYawRad), rightZ = Math.sin(anchorYawRad);
                double fwdX = -Math.sin(anchorYawRad), fwdZ = Math.cos(anchorYawRad);
                double th = Math.random() * Math.PI * 2;
                double a = 0.9 * s, b = 1.55 * s;
                double u = Math.cos(th) * a, v = Math.sin(th) * b;
                double side = Math.random() < 0.5 ? -1 : 1;
                FxParticles.get().add(anchorX + rightX * u, anchorY + 1.68 * s + v, anchorZ + rightZ * u,
                        -rightX * u * 0.5 + fwdX * side * 0.35, -v * 0.35 + 0.18, -rightZ * u * 0.5 + fwdZ * side * 0.35,
                        0.6, 1.6, Double.NaN, 1.4,
                        0.05 * s, 0.01 * s, accentRgb, 0xFFFFFF, baseAlpha * 0.9,
                        FxParticles.Shape.CRYSTAL, true, false, 1.5, 9201);
            }
        }
    }

    /**
     * Emits the gateway in camera-relative coordinates. Pure geometry, so it is testable headlessly.
     */
    public static void emitGate(EffectBatch batch, String style,
                                double worldX, double worldY, double worldZ, double yawRad,
                                double camX, double camY, double camZ,
                                double scale, int accentRgb, double alpha,
                                boolean drawLogo, boolean drawVortex, double seconds) {
        if (batch == null || alpha <= 0.005 || scale <= 0.05) return;
        Frame g = new Frame(worldX - camX, worldY - camY, worldZ - camZ, yawRad);

        double halfW = 1.15 * scale;
        double pillarW = 0.32 * scale;
        double pillarD = 0.36 * scale;
        double gateH = 3.25 * scale;
        double archH = 0.42 * scale;
        boolean cyber = "Cyber Arch".equals(style);
        boolean voidStyle = "Void Sanctuary".equals(style);

        int stone = cyber ? 0x1A1D25 : voidStyle ? 0x0C0B12 : 0x1C1A26;
        int trim = EffectBatch.mixRgb(stone, 0x6A6880, 0.30);
        double solid = Math.min(1.0, alpha * 1.08);
        double pulse = 0.5 + 0.5 * Math.sin(seconds * 1.7);
        int glow = EffectBatch.mixRgb(accentRgb, 0xFFFFFF, 0.25 + 0.15 * pulse);

        // Floor light spill: the portal lights the ground in front of and behind it.
        if (drawVortex) {
            for (int face = -1; face <= 1; face += 2) {
                int near = EffectBatch.withAlpha(accentRgb, alpha * (0.32 + 0.1 * pulse));
                int far = EffectBatch.withAlpha(accentRgb, 0);
                double w = halfW * 0.95, reach = 1.6 * scale * face;
                batch.quad(g.x(-w, 0), g.cy + 0.02, g.z(-w, 0), g.x(w, 0), g.cy + 0.02, g.z(w, 0),
                        g.x(w * 1.3, reach), g.cy + 0.02, g.z(w * 1.3, reach), g.x(-w * 1.3, reach), g.cy + 0.02, g.z(-w * 1.3, reach),
                        near, near, far, far, false);
            }
        }

        // Pillars: stepped plinth, fluted shaft, capital, each with emissive grooves front and back.
        for (int side = -1; side <= 1; side += 2) {
            double u = side * halfW;
            g.box(batch, u - pillarW * 0.9, 0, -pillarD * 0.9, u + pillarW * 0.9, 0.2 * scale, pillarD * 0.9, trim, solid);
            g.box(batch, u - pillarW * 0.72, 0.2 * scale, -pillarD * 0.72, u + pillarW * 0.72, 0.34 * scale, pillarD * 0.72, stone, solid);
            g.box(batch, u - pillarW * 0.5, 0.34 * scale, -pillarD * 0.5, u + pillarW * 0.5, gateH - 0.18 * scale, pillarD * 0.5, stone, solid);
            g.box(batch, u - pillarW * 0.7, gateH - 0.18 * scale, -pillarD * 0.7, u + pillarW * 0.7, gateH, pillarD * 0.7, trim, solid);
            for (int face = -1; face <= 1; face += 2) {
                double v = face * (pillarD * 0.5 + 0.004);
                double breathe = alpha * (0.7 + 0.3 * pulse);
                g.inlay(batch, u - 0.02 * scale, 0.42 * scale, v, u + 0.02 * scale, gateH - 0.26 * scale, glow, breathe);
                // Chevron notches climbing the groove.
                for (int k = 0; k < 5; k++) {
                    double y = 0.7 * scale + k * 0.48 * scale;
                    double travel = (seconds * 0.6 + k / 5.0) % 1.0;
                    int c = EffectBatch.withAlpha(glow, alpha * (0.25 + 0.75 * Math.pow(1 - Math.abs(travel - 0.5) * 2, 3)));
                    batch.line(g.x(u - pillarW * 0.32, v), g.cy + y, g.z(u - pillarW * 0.32, v), g.x(u, v), g.cy + y + 0.08 * scale, g.z(u, v), c, 1.4F, false);
                    batch.line(g.x(u, v), g.cy + y + 0.08 * scale, g.z(u, v), g.x(u + pillarW * 0.32, v), g.cy + y, g.z(u + pillarW * 0.32, v), c, 1.4F, false);
                }
            }
        }

        // Lintel, crown and threshold.
        g.box(batch, -halfW - pillarW * 0.85, gateH, -pillarD * 0.75, halfW + pillarW * 0.85, gateH + archH * 0.62, pillarD * 0.75, stone, solid);
        for (int face = -1; face <= 1; face += 2) {
            double v = face * (pillarD * 0.75 + 0.004);
            g.inlayH(batch, -halfW, gateH + archH * 0.31, v, halfW, 0.016 * scale, glow, alpha * (0.7 + 0.3 * pulse));
        }
        if (!cyber) {
            g.box(batch, -halfW * 0.62, gateH + archH * 0.62, -pillarD * 0.52, halfW * 0.62, gateH + archH * 1.05, pillarD * 0.52, trim, solid);
        }
        g.box(batch, -halfW - pillarW * 0.3, 0, -pillarD * 0.62, halfW + pillarW * 0.3, 0.06 * scale, pillarD * 0.62, trim, solid);

        if (drawVortex) {
            double a = halfW - pillarW * 0.5;
            double y0 = 0.06 * scale;
            double b = (gateH - y0) * 0.5;
            double midY = y0 + b;
            int rings = 10, segs = 64;
            int core = voidStyle ? 0x010103 : 0x05040C;
            for (int ring = 0; ring < rings; ring++) {
                double t0 = ring / (double) rings, t1 = (ring + 1) / (double) rings;
                for (int i = 0; i < segs; i++) {
                    double th0 = i * Math.PI * 2 / segs, th1 = (i + 1) * Math.PI * 2 / segs;
                    double ca0 = Math.cos(th0), sa0 = Math.sin(th0), ca1 = Math.cos(th1), sa1 = Math.sin(th1);
                    batch.quad(
                            g.x(ca0 * a * t0, 0), g.cy + midY + sa0 * b * t0, g.z(ca0 * a * t0, 0),
                            g.x(ca1 * a * t0, 0), g.cy + midY + sa1 * b * t0, g.z(ca1 * a * t0, 0),
                            g.x(ca1 * a * t1, 0), g.cy + midY + sa1 * b * t1, g.z(ca1 * a * t1, 0),
                            g.x(ca0 * a * t1, 0), g.cy + midY + sa0 * b * t1, g.z(ca0 * a * t1, 0),
                            membrane(core, accentRgb, t0, th0, seconds, alpha),
                            membrane(core, accentRgb, t0, th1, seconds, alpha),
                            membrane(core, accentRgb, t1, th1, seconds, alpha),
                            membrane(core, accentRgb, t1, th0, seconds, alpha), false);
                }
            }
            // Event-horizon rim: a bright core line inside a wide soft corona.
            int rim = EffectBatch.withAlpha(EffectBatch.mixRgb(accentRgb, 0xFFFFFF, 0.6), alpha);
            int corona = EffectBatch.withAlpha(accentRgb, alpha * 0.45);
            int coronaOut = EffectBatch.withAlpha(accentRgb, 0);
            for (int i = 0; i < 96; i++) {
                double th0 = i * Math.PI * 2 / 96, th1 = (i + 1) * Math.PI * 2 / 96;
                double ca0 = Math.cos(th0), sa0 = Math.sin(th0), ca1 = Math.cos(th1), sa1 = Math.sin(th1);
                for (int face = -1; face <= 1; face += 2) {
                    double v = face * 0.012;
                    batch.quad(g.x(ca0 * a, v), g.cy + midY + sa0 * b, g.z(ca0 * a, v),
                            g.x(ca1 * a, v), g.cy + midY + sa1 * b, g.z(ca1 * a, v),
                            g.x(ca1 * a * 0.86, v), g.cy + midY + sa1 * b * 0.9, g.z(ca1 * a * 0.86, v),
                            g.x(ca0 * a * 0.86, v), g.cy + midY + sa0 * b * 0.9, g.z(ca0 * a * 0.86, v),
                            corona, corona, coronaOut, coronaOut, false);
                }
                batch.glowLine(g.x(ca0 * a, 0), g.cy + midY + sa0 * b, g.z(ca0 * a, 0),
                        g.x(ca1 * a, 0), g.cy + midY + sa1 * b, g.z(ca1 * a, 0), rim, 2.6F, false);
            }
            // Three spiral arms streaming into the core, tapered in brightness.
            for (int arm = 0; arm < 3; arm++) {
                double prevU = 0, prevV = 0;
                for (int k = 1; k <= 36; k++) {
                    double t = k / 36.0;
                    double th = -seconds * 1.1 + arm * Math.PI * 2 / 3 + t * 4.8;
                    double u = Math.cos(th) * a * t * 0.94, v = Math.sin(th) * b * t * 0.94;
                    double taper = Math.sin(t * Math.PI);
                    int c = EffectBatch.withAlpha(EffectBatch.mixRgb(accentRgb, 0xFFFFFF, 0.5 * t), alpha * 0.7 * taper);
                    for (int face = -1; face <= 1; face += 2) {
                        batch.line(g.x(prevU, face * 0.008), g.cy + midY + prevV, g.z(prevU, face * 0.008),
                                g.x(u, face * 0.008), g.cy + midY + v, g.z(u, face * 0.008), c, 1.2F + 1.4F * (float) taper, false);
                    }
                    prevU = u;
                    prevV = v;
                }
            }
            // Orbiting rune ticks just outside the rim.
            for (int r = 0; r < 12; r++) {
                double th = seconds * 0.35 + r * Math.PI * 2 / 12;
                double ca = Math.cos(th), sa = Math.sin(th);
                double k0 = 1.05, k1 = 1.11;
                int c = EffectBatch.withAlpha(glow, alpha * (r % 3 == 0 ? 0.95 : 0.5));
                for (int face = -1; face <= 1; face += 2) {
                    double v = face * (pillarD * 0.2);
                    batch.glowLine(g.x(ca * a * k0, v), g.cy + midY + sa * b * k0, g.z(ca * a * k0, v),
                            g.x(ca * a * k1, v), g.cy + midY + sa * b * k1, g.z(ca * a * k1, v), c, 1.6F, false);
                }
            }
        }

        if (drawLogo) {
            double ey = gateH + archH * (cyber ? 0.31 : 0.84) + 0.34 * scale + Math.sin(seconds * 1.6) * 0.04 * scale;
            double er = 0.26 * scale;
            double depth = 0.12 * scale;
            g.crest(batch, ey, er, depth, accentRgb, alpha, seconds);
            int logo = EffectBatch.withAlpha(0xFFFFFF, Math.min(1.0, alpha));
            double jw = er * 0.34, jh = er * 0.48;
            for (int face = -1; face <= 1; face += 2) {
                double v = face * (depth * 0.5 + 0.012);
                batch.glowLine(g.x(-jw * 0.5, v), g.cy + ey + jh, g.z(-jw * 0.5, v), g.x(jw, v), g.cy + ey + jh, g.z(jw, v), logo, 2.2F, false);
                batch.glowLine(g.x(jw * 0.45, v), g.cy + ey + jh, g.z(jw * 0.45, v), g.x(jw * 0.45, v), g.cy + ey - jh * 0.6, g.z(jw * 0.45, v), logo, 2.2F, false);
                batch.glowLine(g.x(jw * 0.45, v), g.cy + ey - jh * 0.6, g.z(jw * 0.45, v), g.x(-jw * 0.35, v), g.cy + ey - jh * 0.9, g.z(-jw * 0.35, v), logo, 2.2F, false);
                batch.glowLine(g.x(-jw * 0.35, v), g.cy + ey - jh * 0.9, g.z(-jw * 0.35, v), g.x(-jw * 0.8, v), g.cy + ey - jh * 0.35, g.z(-jw * 0.8, v), logo, 2.2F, false);
            }
        }
    }

    /** Membrane color at normalized radius {@code t} and angle {@code th}: dark core, swirling bands, bright edge. */
    private static int membrane(int core, int accent, double t, double th, double seconds, double alpha) {
        double swirl = 0.5 + 0.5 * Math.sin(3 * th - 8.0 * t + seconds * 2.2);
        double fine = 0.5 + 0.5 * Math.sin(7 * th + 13.0 * t - seconds * 3.1);
        // Light lives at the rim and in thin swirl bands; the centre stays a deep void.
        double band = Math.pow(swirl, 6) * 0.35 + Math.pow(fine, 10) * 0.12;
        double light = 0.02 + 0.75 * Math.pow(t, 3.5) + band * Math.pow(t, 1.3);
        int rgb = EffectBatch.mixRgb(core, accent, light);
        if (t > 0.88) rgb = EffectBatch.mixRgb(rgb, 0xFFFFFF, (t - 0.88) * 3.0);
        return EffectBatch.withAlpha(rgb, alpha * (0.94 + 0.06 * t));
    }

    /** Gate-local frame: {@code u} runs along the gate (right), {@code v} through it (forward), y is up. */
    private record Frame(double cx, double cy, double cz, double rx, double rz, double fx, double fz) {
        Frame(double cx, double cy, double cz, double yaw) {
            this(cx, cy, cz, Math.cos(yaw), Math.sin(yaw), -Math.sin(yaw), Math.cos(yaw));
        }

        double x(double u, double v) { return cx + rx * u + fx * v; }
        double z(double u, double v) { return cz + rz * u + fz * v; }

        /** Solid box with every face Lambert-lit by its world normal and a faint top bevel. */
        void box(EffectBatch batch, double u0, double y0, double v0, double u1, double y1, double v1, int rgb, double alpha) {
            double py0 = cy + y0, py1 = cy + y1;
            face(batch, u0, v0, u1, v0, py0, py1, -fx, -fz, rgb, alpha);
            face(batch, u1, v1, u0, v1, py0, py1, fx, fz, rgb, alpha);
            face(batch, u0, v1, u0, v0, py0, py1, -rx, -rz, rgb, alpha);
            face(batch, u1, v0, u1, v1, py0, py1, rx, rz, rgb, alpha);
            int top = EffectBatch.withAlpha(EffectBatch.shade(rgb, 0, 1, 0), alpha);
            batch.quad(x(u0, v0), py1, z(u0, v0), x(u1, v0), py1, z(u1, v0), x(u1, v1), py1, z(u1, v1), x(u0, v1), py1, z(u0, v1),
                    top, top, top, top, false);
            int bottom = EffectBatch.withAlpha(EffectBatch.shade(rgb, 0, -1, 0), alpha);
            batch.quad(x(u0, v0), py0, z(u0, v0), x(u1, v0), py0, z(u1, v0), x(u1, v1), py0, z(u1, v1), x(u0, v1), py0, z(u0, v1),
                    bottom, bottom, bottom, bottom, false);
            int bevel = EffectBatch.withAlpha(EffectBatch.mixRgb(rgb, 0xFFFFFF, 0.28), alpha * 0.6);
            double[][] c = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
            for (int i = 0; i < 4; i++) {
                double[] p = c[i], q = c[(i + 1) % 4];
                batch.line(x(p[0], p[1]), py1, z(p[0], p[1]), x(q[0], q[1]), py1, z(q[0], q[1]), bevel, 1.0F, false);
            }
        }

        /** Vertical face, darkened toward its base for a little ambient occlusion. */
        private void face(EffectBatch batch, double ua, double va, double ub, double vb, double py0, double py1,
                          double nx, double nz, int rgb, double alpha) {
            int lit = EffectBatch.shade(rgb, nx, 0, nz);
            int high = EffectBatch.withAlpha(lit, alpha);
            int low = EffectBatch.withAlpha(EffectBatch.mixRgb(lit, 0x000000, 0.4), alpha);
            batch.quad(x(ua, va), py0, z(ua, va), x(ub, vb), py0, z(ub, vb), x(ub, vb), py1, z(ub, vb), x(ua, va), py1, z(ua, va),
                    low, low, high, high, false);
        }

        /** Thin emissive vertical strip at depth {@code v}, with a soft halo. */
        void inlay(EffectBatch batch, double u0, double y0, double v, double u1, double y1, int rgb, double alpha) {
            int core = EffectBatch.withAlpha(rgb, alpha);
            batch.quad(x(u0, v), cy + y0, z(u0, v), x(u1, v), cy + y0, z(u1, v), x(u1, v), cy + y1, z(u1, v), x(u0, v), cy + y1, z(u0, v),
                    core, core, core, core, false);
            double mu = (u0 + u1) * 0.5;
            batch.glowLine(x(mu, v), cy + y0, z(mu, v), x(mu, v), cy + y1, z(mu, v), core, 1.2F, false);
        }

        /** Thin emissive horizontal strip from {@code u0} to {@code u1} at height {@code y}. */
        void inlayH(EffectBatch batch, double u0, double y, double v, double u1, double half, int rgb, double alpha) {
            int core = EffectBatch.withAlpha(rgb, alpha);
            batch.quad(x(u0, v), cy + y - half, z(u0, v), x(u1, v), cy + y - half, z(u1, v),
                    x(u1, v), cy + y + half, z(u1, v), x(u0, v), cy + y + half, z(u0, v), core, core, core, core, false);
            batch.glowLine(x(u0, v), cy + y, z(u0, v), x(u1, v), cy + y, z(u1, v), core, 1.2F, false);
        }

        /** Solid lit octahedral crest that slowly turns, with a glowing outline. */
        void crest(EffectBatch batch, double ey, double er, double depth, int accent, double alpha, double seconds) {
            double spin = Math.sin(seconds * 0.7) * 0.35;
            double cs = Math.cos(spin), sn = Math.sin(spin);
            double[][] ring = {{0, er}, {er, 0}, {0, -er}, {-er, 0}};
            int body = EffectBatch.mixRgb(0x1E1C2C, accent, 0.3);
            int edge = EffectBatch.withAlpha(EffectBatch.mixRgb(accent, 0xFFFFFF, 0.4), alpha);
            for (int face = -1; face <= 1; face += 2) {
                for (int i = 0; i < 4; i++) {
                    double[] p = ring[i], q = ring[(i + 1) % 4];
                    // Rotate the crest about its vertical axis: (u, v) -> (u cos - v sin, u sin + v cos).
                    double pu = p[0] * cs, pv = p[0] * sn, qu = q[0] * cs, qv = q[0] * sn;
                    double tu = -face * depth * sn, tv = face * depth * cs;
                    double nu = (pu + qu) * 0.5 + tu * 0.6, nv = (pv + qv) * 0.5 + tv * 0.6, ny = (p[1] + q[1]) * 0.5;
                    double nl = Math.max(1e-6, Math.sqrt(nu * nu + nv * nv + ny * ny));
                    int c = EffectBatch.withAlpha(EffectBatch.shade(body, (rx * nu + fx * nv) / nl, ny / nl, (rz * nu + fz * nv) / nl), alpha);
                    batch.quad(x(pu, pv), cy + ey + p[1], z(pu, pv), x(qu, qv), cy + ey + q[1], z(qu, qv),
                            x(tu, tv), cy + ey, z(tu, tv), x(tu, tv), cy + ey, z(tu, tv), c, c, c, c, false);
                    batch.line(x(pu, pv), cy + ey + p[1], z(pu, pv), x(tu, tv), cy + ey, z(tu, tv), EffectBatch.withAlpha(edge, alpha * 0.5), 1.0F, false);
                }
            }
            for (int i = 0; i < 4; i++) {
                double[] p = ring[i], q = ring[(i + 1) % 4];
                batch.glowLine(x(p[0] * cs, p[0] * sn), cy + ey + p[1], z(p[0] * cs, p[0] * sn),
                        x(q[0] * cs, q[0] * sn), cy + ey + q[1], z(q[0] * cs, q[0] * sn), edge, 2.0F, false);
            }
        }
    }
}
