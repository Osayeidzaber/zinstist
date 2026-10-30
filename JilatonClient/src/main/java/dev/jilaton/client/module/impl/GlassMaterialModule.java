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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Translucent 3D glass material shell and articulated prism facets for players and combat targets,
 * featuring Fresnel edge rim lighting, specular reflection bands, and internal refraction seams.
 */
public final class GlassMaterialModule extends ClientModule implements FxEmitter {
    public static final List<String> PRESETS = List.of(
            "Frosted", "Clear Crystal", "Obsidian Glass", "Holographic", "Cyber Prism"
    );

    private final ChoiceSetting preset = addSetting(new ChoiceSetting("preset", "Material preset", PRESETS, "Frosted"));
    private final ChoiceSetting targets = addSetting(new ChoiceSetting("targets", "Targets",
            List.of("Self + Target", "Self", "Target", "All Players"), "Self + Target"));
    private final NumberSetting rimStrength = addSetting(new NumberSetting("rim_strength", "Rim highlight", 0.1, 1.0, 0.05, 0.82));
    private final NumberSetting refraction = addSetting(new NumberSetting("refraction", "Internal refraction", 0.0, 1.0, 0.05, 0.65));
    private final BooleanSetting specular = addSetting(new BooleanSetting("specular", "Specular sheen", true));
    private final NumberSetting shellScale = addSetting(new NumberSetting("shell_scale", "Shell scale", 0.9, 1.25, 0.02, 1.04));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Tint mode"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "B8E8FF"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Glass opacity", 0.1, 0.95, 0.05, 0.48));

    public GlassMaterialModule() {
        super("glass_material", "GlassMaterial",
                "Translucent frosted, crystal, obsidian, and holographic 3D glass material shells with Fresnel rim highlights.",
                ModuleCategory.RENDER);
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
        int targetId = CombatTargetTracker.targetId();
        String targetMode = targets.value();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) continue;
            boolean isSelf = living == client.player;
            if (isSelf && f.firstPerson) continue;
            boolean isTarget = living.getId() == targetId;
            boolean include = switch (targetMode) {
                case "Self" -> isSelf;
                case "Target" -> isTarget;
                case "All Players" -> living instanceof Player;
                default -> isSelf || isTarget;
            };
            if (!include) continue;
            if (!isSelf && living.distanceToSqr(client.player) > 48.0 * 48.0) continue;

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
            case "Clear Crystal" -> 0xE2F4FF;
            case "Obsidian Glass" -> 0x16141F;
            case "Holographic" -> {
                double wave = 0.5 + 0.5 * Math.sin(sec * 1.8);
                yield EffectBatch.mixRgb(0x64F0FF, 0xD488FF, wave);
            }
            case "Cyber Prism" -> 0x4AF2EA;
            default -> 0xC6E2F5;
        };
    }

    private static int resolveRimColor(String style, int tintRgb, int accent, double sec) {
        return switch (style) {
            case "Obsidian Glass" -> EffectBatch.mixRgb(accent & 0xFFFFFF, 0xD8D4EC, 0.55);
            case "Holographic" -> {
                double wave = 0.5 + 0.5 * Math.cos(sec * 2.1);
                yield EffectBatch.mixRgb(0xFFFFFF, 0x9FF8FF, wave);
            }
            case "Cyber Prism" -> EffectBatch.mixRgb(tintRgb, 0xFFFFFF, 0.65);
            default -> EffectBatch.mixRgb(tintRgb, 0xFFFFFF, 0.75);
        };
    }

    /**
     * Emits a multi-faceted 3D translucent glass prism shell around a character with Fresnel edge
     * rim highlights, specular sweep bands, and internal refraction seams anchored to the skeleton.
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
        double cx = wx - camX;
        double cy = wy - camY;
        double cz = wz - camZ;
        double rx = Math.max(0.28, width * 0.62);
        double ry0 = cy + 0.04;
        double ry1 = cy + height * 0.98;
        double midY = (ry0 + ry1) * 0.5;
        double topR = rx * 0.84;
        double botR = rx * 0.88;

        int sides = 8;
        double rot = seconds * 0.22;
        int faceTopCol = EffectBatch.withAlpha(tintRgb, alpha * 0.26);
        int faceMidCol = EffectBatch.withAlpha(tintRgb, alpha * 0.42);
        int faceBotCol = EffectBatch.withAlpha(tintRgb, alpha * 0.18);
        int rimCol = EffectBatch.withAlpha(rimRgb, Math.min(1.0, alpha * (0.55 + 0.45 * rimStrength)));
        int refractCol = EffectBatch.withAlpha(rimRgb, alpha * 0.45 * refraction);

        for (int i = 0; i < sides; i++) {
            double a0 = rot + i * (Math.PI * 2.0 / sides);
            double a1 = rot + (i + 1) * (Math.PI * 2.0 / sides);
            double c0 = Math.cos(a0), s0 = Math.sin(a0);
            double c1 = Math.cos(a1), s1 = Math.sin(a1);

            double bx0 = cx + c0 * botR, bz0 = cz + s0 * botR;
            double bx1 = cx + c1 * botR, bz1 = cz + s1 * botR;
            double mx0 = cx + c0 * rx,   mz0 = cz + s0 * rx;
            double mx1 = cx + c1 * rx,   mz1 = cz + s1 * rx;
            double tx0 = cx + c0 * topR, tz0 = cz + s0 * topR;
            double tx1 = cx + c1 * topR, tz1 = cz + s1 * topR;

            // Lower facet quad
            batch.quad(bx0, ry0, bz0, bx1, ry0, bz1, mx1, midY, mz1, mx0, midY, mz0,
                    faceBotCol, faceBotCol, faceMidCol, faceMidCol, false);
            // Upper facet quad
            batch.quad(mx0, midY, mz0, mx1, midY, mz1, tx1, ry1, tz1, tx0, ry1, tz0,
                    faceMidCol, faceMidCol, faceTopCol, faceTopCol, false);

            // Fresnel vertical & waist rim edges
            batch.glowLine(bx0, ry0, bz0, mx0, midY, mz0, rimCol, 1.4F, false);
            batch.glowLine(mx0, midY, mz0, tx0, ry1, tz0, rimCol, 1.4F, false);
            batch.line(bx0, ry0, bz0, bx1, ry0, bz1, rimCol, 1.2F, false);
            batch.line(tx0, ry1, tz0, tx1, ry1, tz1, rimCol, 1.35F, false);
            if (refraction > 0.05) {
                batch.line(mx0, midY, mz0, mx1, midY, mz1, refractCol, 1.1F, false);
                batch.line(bx0, ry0, bz0, mx1, midY, mz1, refractCol, 0.9F, false);
            }

            if (specular && (i % 3 == 0)) {
                double sheenPhase = 0.2 + 0.6 * (0.5 + 0.5 * Math.sin(seconds * 1.4 + i));
                double sy0 = ry0 + (ry1 - ry0) * Math.max(0.05, sheenPhase - 0.14);
                double sy1 = ry0 + (ry1 - ry0) * Math.min(0.95, sheenPhase + 0.14);
                int specCol = EffectBatch.withAlpha(0xFFFFFF, alpha * 0.48 * rimStrength);
                int specFade = EffectBatch.withAlpha(0xFFFFFF, 0);
                batch.quad(mx0, sy0, mz0, mx1, sy0, mz1, mx1, sy1, mz1, mx0, sy1, mz0,
                        specFade, specCol, specFade, specCol, false);
            }
        }

        // Articulated internal refraction prism around head & shoulders if skeleton is present
        if (joints != null && refraction > 0.1) {
            Vec3 hc = joints.headCenter();
            Vec3 ls = joints.leftShoulder();
            Vec3 rs = joints.rightShoulder();
            Vec3 pel = joints.pelvis();
            int innerCol = EffectBatch.withAlpha(rimRgb, alpha * 0.52 * refraction);
            batch.glowLine(hc.x - camX, hc.y - camY, hc.z - camZ,
                    ls.x - camX, ls.y - camY, ls.z - camZ, innerCol, 1.3F, false);
            batch.glowLine(hc.x - camX, hc.y - camY, hc.z - camZ,
                    rs.x - camX, rs.y - camY, rs.z - camZ, innerCol, 1.3F, false);
            batch.glowLine(ls.x - camX, ls.y - camY, ls.z - camZ,
                    pel.x - camX, pel.y - camY, pel.z - camZ, innerCol, 1.2F, false);
            batch.glowLine(rs.x - camX, rs.y - camY, rs.z - camZ,
                    pel.x - camX, pel.y - camY, pel.z - camZ, innerCol, 1.2F, false);
        }
    }
}
