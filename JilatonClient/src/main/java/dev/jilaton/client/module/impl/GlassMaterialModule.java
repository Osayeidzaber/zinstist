package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.CombatTargetTracker;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.model.BodyAnchor;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Turns players into glass. Vanilla draws the body as a faint translucent ghost (its own invisibility path, so
 * skin, pose and layering stay correct), and on top of it every bone of the posed skeleton gets a glass volume:
 * faces fade by the view angle (Fresnel: clear face-on, bright at grazing angles), a key-light specular glint
 * slides over the surface, and the silhouette edges carry a thin rim.
 */
public final class GlassMaterialModule extends ClientModule implements FxEmitter {
    public static final List<String> PRESETS = List.of(
            "Frosted", "Clear Crystal", "Obsidian Glass", "Holographic", "Cyber Prism"
    );

    private static volatile GlassMaterialModule active;

    private final ChoiceSetting preset = addSetting(new ChoiceSetting("preset", "Material preset", PRESETS, "Frosted"));
    private final ChoiceSetting targets = addSetting(new ChoiceSetting("targets", "Targets",
            List.of("Self + Target", "Self", "Target", "All Players"), "Self + Target"));
    private final NumberSetting rimStrength = addSetting(new NumberSetting("rim_strength", "Rim highlight", 0.1, 1.0, 0.05, 0.82));
    private final NumberSetting refraction = addSetting(new NumberSetting("refraction", "Internal refraction", 0.0, 1.0, 0.05, 0.65));
    private final BooleanSetting specular = addSetting(new BooleanSetting("specular", "Specular sheen", true));
    private final BooleanSetting ghostBody = addSetting(new BooleanSetting("ghost_body", "See-through body", true));
    private final NumberSetting shellScale = addSetting(new NumberSetting("shell_scale", "Shell scale", 0.9, 1.25, 0.02, 1.04));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Tint mode"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "B8E8FF"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Glass opacity", 0.1, 0.95, 0.05, 0.6));

    public GlassMaterialModule() {
        super("glass_material", "GlassMaterial",
                "Turns players into frosted, crystal, obsidian or holographic glass with Fresnel edges and specular glints.",
                ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable(Minecraft client) { active = this; }

    @Override
    protected void onDisable(Minecraft client) { if (active == this) active = null; }

    /** Render-state hook: draw a glass player's vanilla body through the translucent invisibility path. */
    public static void applyGhost(Entity entity, AvatarRenderState state) {
        GlassMaterialModule module = active;
        if (module == null || !module.enabled() || !module.ghostBody.value() || !(entity instanceof LivingEntity living)) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !module.includes(living, client)) return;
        if (living == client.player && client.options.getCameraType().isFirstPerson()) return;
        state.isInvisible = true;
        state.isInvisibleToPlayer = false;
    }

    private boolean includes(LivingEntity living, Minecraft client) {
        boolean isSelf = living == client.player;
        boolean isTarget = living.getId() == CombatTargetTracker.targetId();
        boolean include = switch (targets.value()) {
            case "Self" -> isSelf;
            case "Target" -> isTarget;
            case "All Players" -> living instanceof Player;
            default -> isSelf || isTarget;
        };
        return include && (isSelf || living.distanceToSqr(client.player) <= 48.0 * 48.0);
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        if (client.level == null || client.player == null) return;
        double baseAlpha = opacity.value() * Math.min(1.0, f.intensity);
        if (baseAlpha <= 0.01) return;

        String style = preset.value();
        double sec = f.reducedMotion ? 0.0 : f.seconds();
        int tintRgb = resolveGlassTint(style, f.accent, sec);
        int rimRgb = resolveRimColor(style, tintRgb, f.accent, sec);

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) continue;
            if (living == client.player && f.firstPerson) continue;
            if (!includes(living, client)) continue;
            Vec3 pos = living.getPosition(f.partial);
            BodyAnchor.SkeletonJoints joints = BodyAnchor.skeleton(living, f.partial);
            emitGlassShell(f.batch, pos.x, pos.y, pos.z,
                    living.getBbWidth() * shellScale.value(),
                    living.getBbHeight() * shellScale.value(),
                    joints,
                    f.camX, f.camY, f.camZ,
                    tintRgb, rimRgb, baseAlpha,
                    rimStrength.value(), refraction.value(),
                    specular.value(), sec);
        }
    }

    public int resolveGlassTint(String style, int accent, double sec) {
        String mode = colorMode.value();
        if (!mode.equals(EffectPalette.MONO)) {
            return EffectPalette.resolve(mode, accent, color.value());
        }
        return switch (style) {
            case "Clear Crystal" -> 0xE8F6FF;
            case "Obsidian Glass" -> 0x1A1726;
            case "Holographic" -> EffectBatch.mixRgb(0x64F0FF, 0xD488FF, 0.5 + 0.5 * Math.sin(sec * 1.8));
            case "Cyber Prism" -> 0x4AF2EA;
            default -> 0xBFDCF0;
        };
    }

    private static int resolveRimColor(String style, int tintRgb, int accent, double sec) {
        return switch (style) {
            case "Obsidian Glass" -> EffectBatch.mixRgb(accent & 0xFFFFFF, 0xD8D4EC, 0.55);
            case "Holographic" -> EffectBatch.mixRgb(0xFFFFFF, 0x9FF8FF, 0.5 + 0.5 * Math.cos(sec * 2.1));
            case "Cyber Prism" -> EffectBatch.mixRgb(tintRgb, 0xFFFFFF, 0.65);
            default -> EffectBatch.mixRgb(tintRgb, 0xFFFFFF, 0.8);
        };
    }

    /**
     * Emits glass volumes for every bone of the skeleton (or one torso volume when there is none), in
     * camera-relative coordinates. {@code width}/{@code height} scale the bone thickness with the entity.
     */
    public static void emitGlassShell(EffectBatch batch,
                                      double wx, double wy, double wz,
                                      double width, double height,
                                      BodyAnchor.SkeletonJoints joints,
                                      double camX, double camY, double camZ,
                                      int tintRgb, int rimRgb, double alpha,
                                      double rimStrength, double refraction,
                                      boolean specular, double seconds) {
        if (batch == null || alpha <= 0.005) return;
        double unit = Math.max(0.5, height / 1.8) * Math.max(0.85, Math.min(1.25, width / 0.6));
        Glass glass = new Glass(batch, camX, camY, camZ, tintRgb, rimRgb, alpha, rimStrength, refraction, specular, seconds);
        if (joints == null) {
            glass.bone(new double[]{wx, wy, wz}, new double[]{wx, wy + height, wz}, new double[]{1, 0, 0}, width * 0.5, width * 0.3);
            return;
        }
        double[] right = sub(joints.rightShoulder(), joints.leftShoulder());
        double limb = 0.135 * unit;
        // Head: a cube around the head centre, oriented by neck -> head top.
        double[] headBase = lerp(joints.headCenter(), joints.headTop(), -1.0);
        glass.bone(headBase, joints.headTop(), right, 0.27 * unit, 0.27 * unit);
        glass.bone(joints.pelvis(), joints.neck(), right, 0.27 * unit, 0.14 * unit);
        glass.bone(joints.rightShoulder(), joints.rightElbow(), right, limb, limb);
        glass.bone(joints.rightElbow(), joints.rightWrist(), right, limb * 0.96, limb * 0.96);
        glass.bone(joints.leftShoulder(), joints.leftElbow(), right, limb, limb);
        glass.bone(joints.leftElbow(), joints.leftWrist(), right, limb * 0.96, limb * 0.96);
        glass.bone(joints.rightHip(), joints.rightKnee(), right, limb, limb);
        glass.bone(joints.rightKnee(), joints.rightAnkle(), right, limb * 0.96, limb * 0.96);
        glass.bone(joints.leftHip(), joints.leftKnee(), right, limb, limb);
        glass.bone(joints.leftKnee(), joints.leftAnkle(), right, limb * 0.96, limb * 0.96);
        if (refraction > 0.05) {
            // Caustic seam through the core of the body: light bent through the glass.
            int seam = EffectBatch.withAlpha(rimRgb, alpha * 0.5 * refraction);
            glass.seam(joints.headCenter(), joints.neck(), seam);
            glass.seam(joints.neck(), joints.spine(), seam);
            glass.seam(joints.spine(), joints.pelvis(), seam);
        }
    }

    private static double[] sub(double[] a, double[] b) { return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }

    private static double[] lerp(double[] a, double[] b, double t) {
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private record Glass(EffectBatch batch, double camX, double camY, double camZ, int tint, int rim, double alpha,
                         double rimStrength, double refraction, boolean specular, double seconds) {

        /** A glass box from joint {@code a} to joint {@code b}, half-extents {@code hw} sideways and {@code hd} deep. */
        void bone(double[] a, double[] b, double[] rightHint, double hw, double hd) {
            double ax = a[0] - camX, ay = a[1] - camY, az = a[2] - camZ;
            double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-4) return;
            dx /= len; dy /= len; dz /= len;
            // Right axis: the shoulder line made perpendicular to the bone; forward completes the frame.
            double dot = rightHint[0] * dx + rightHint[1] * dy + rightHint[2] * dz;
            double rx = rightHint[0] - dot * dx, ry = rightHint[1] - dot * dy, rz = rightHint[2] - dot * dz;
            double rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
            if (rl < 1e-4) { rx = 1; ry = 0; rz = 0; rl = 1; }
            rx /= rl; ry /= rl; rz /= rl;
            double fx = dy * rz - dz * ry, fy = dz * rx - dx * rz, fz = dx * ry - dy * rx;
            // Extend a little past both joints so neighbouring bones overlap instead of gapping at the elbows.
            double pad = Math.min(hw, hd) * 0.5;
            double sx = ax - dx * pad, sy = ay - dy * pad, sz = az - dz * pad;
            double ex = ax + dx * (len + pad), ey = ay + dy * (len + pad), ez = az + dz * (len + pad);
            double[][] c = new double[8][];
            int i = 0;
            for (int end = 0; end < 2; end++) {
                double px = end == 0 ? sx : ex, py = end == 0 ? sy : ey, pz = end == 0 ? sz : ez;
                for (int[] k : new int[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
                    c[i++] = new double[]{px + rx * hw * k[0] + fx * hd * k[1], py + ry * hw * k[0] + fy * hd * k[1], pz + rz * hw * k[0] + fz * hd * k[1]};
                }
            }
            face(c[0], c[1], c[5], c[4], -fx, -fy, -fz);
            face(c[1], c[2], c[6], c[5], rx, ry, rz);
            face(c[2], c[3], c[7], c[6], fx, fy, fz);
            face(c[3], c[0], c[4], c[7], -rx, -ry, -rz);
            face(c[4], c[5], c[6], c[7], dx, dy, dz);
            face(c[3], c[2], c[1], c[0], -dx, -dy, -dz);
            // Silhouette edges: an edge is on the outline when one adjacent face looks at the camera and the other away.
            double[][] n = {{-fx, -fy, -fz}, {rx, ry, rz}, {fx, fy, fz}, {-rx, -ry, -rz}};
            double[] cap0 = {-dx, -dy, -dz}, cap1 = {dx, dy, dz};
            for (int k = 0; k < 4; k++) {
                int next = (k + 1) % 4;
                edge(c[next], c[next + 4], n[k], n[next]);
                edge(c[k], c[next], n[k], cap0);
                edge(c[k + 4], c[next + 4], n[k], cap1);
            }
        }

        private double facing(double[] p, double nx, double ny, double nz) {
            double l = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
            return l < 1e-6 ? 0 : -(p[0] * nx + p[1] * ny + p[2] * nz) / l;
        }

        private void face(double[] a, double[] b, double[] c, double[] d, double nx, double ny, double nz) {
            double[] mid = {(a[0] + c[0]) * 0.5, (a[1] + c[1]) * 0.5, (a[2] + c[2]) * 0.5};
            double ndv = facing(mid, nx, ny, nz);
            if (ndv < -0.05) return; // back faces would double the tint; the ghost body already shows through
            double fresnel = Math.pow(1 - Math.max(0, Math.min(1, ndv)), 2.2);
            double light = Math.max(0, nx * 0.32 + ny * 0.86 + nz * 0.40);
            int body = EffectBatch.mixRgb(EffectBatch.shade(tint, nx, ny, nz), rim, fresnel * rimStrength);
            double base = alpha * (0.16 + 0.6 * fresnel * rimStrength);
            // Vertical gradient: denser at the bottom of each face like thick glass.
            int top = EffectBatch.withAlpha(body, base);
            int bottom = EffectBatch.withAlpha(EffectBatch.mixRgb(body, tint, 0.4), base * 1.25);
            batch.quad(a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], d[0], d[1], d[2], bottom, bottom, top, top, false);
            if (specular && light > 0.5) {
                // A glint band that slides along the face over time.
                double t = 0.5 + 0.5 * Math.sin(seconds * 1.3 + (a[0] + a[2]) * 3.0);
                double t0 = Math.max(0, t - 0.12), t1 = Math.min(1, t + 0.12);
                int glint = EffectBatch.withAlpha(0xFFFFFF, alpha * 0.7 * (light - 0.5) * 2 * rimStrength);
                int clear = EffectBatch.withAlpha(0xFFFFFF, 0);
                double[] p0 = mix(a, d, t0), p1 = mix(b, c, t0), p2 = mix(b, c, t1), p3 = mix(a, d, t1);
                double[] m0 = mix(a, d, t), m1 = mix(b, c, t);
                batch.quad(p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], m1[0], m1[1], m1[2], m0[0], m0[1], m0[2], clear, clear, glint, glint, false);
                batch.quad(m0[0], m0[1], m0[2], m1[0], m1[1], m1[2], p2[0], p2[1], p2[2], p3[0], p3[1], p3[2], glint, glint, clear, clear, false);
            }
        }

        private void edge(double[] a, double[] b, double[] n0, double[] n1) {
            double[] mid = {(a[0] + b[0]) * 0.5, (a[1] + b[1]) * 0.5, (a[2] + b[2]) * 0.5};
            double f0 = facing(mid, n0[0], n0[1], n0[2]), f1 = facing(mid, n1[0], n1[1], n1[2]);
            boolean outline = f0 > 0 != f1 > 0;
            double k = outline ? 0.95 : 0.22;
            int c = EffectBatch.withAlpha(rim, Math.min(1, alpha * k * (0.4 + 0.6 * rimStrength)));
            if (outline) batch.glowLine(a[0], a[1], a[2], b[0], b[1], b[2], c, 1.3F, false);
            else batch.line(a[0], a[1], a[2], b[0], b[1], b[2], c, 1.0F, false);
        }

        void seam(double[] a, double[] b, int color) {
            batch.glowLine(a[0] - camX, a[1] - camY, a[2] - camZ, b[0] - camX, b[1] - camY, b[2] - camZ, color, 1.2F, false);
        }

        private static double[] mix(double[] a, double[] b, double t) {
            return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
        }
    }
}
