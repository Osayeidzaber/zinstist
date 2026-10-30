package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.CombatTargetTracker;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxParticles;
import dev.jilaton.client.render.model.BodyAnchor;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Futuristic digital world construct: renders a terrain-conforming cyber grid, sonar scan pulse,
 * nearby block edge telemetry, and a pose-aligned 3D humanoid skeleton overlay for players and targets.
 */
public final class CyberspaceModule extends ClientModule implements FxEmitter {
    public static final List<String> PRESETS = List.of("Cyber Grid", "Neon Matrix", "Minimal Wire", "Full Construct");

    private final ChoiceSetting preset = addSetting(new ChoiceSetting("preset", "Preset", PRESETS, "Cyber Grid"));
    private final NumberSetting gridRadius = addSetting(new NumberSetting("grid_radius", "Grid radius", 6, 36, 2, 16));
    private final NumberSetting scanSpeed = addSetting(new NumberSetting("scan_speed", "Scan speed", 0.2, 3.0, 0.1, 1.0));
    private final BooleanSetting blockWireframe = addSetting(new BooleanSetting("block_wireframe", "Block edge telemetry", true));
    private final BooleanSetting skeleton = addSetting(new BooleanSetting("skeleton", "Player skeleton overlay", true));
    private final BooleanSetting skeletonSelf = addSetting(new BooleanSetting("skeleton_self", "Own skeleton (3rd person)", true));
    private final ChoiceSetting skeletonTargets = addSetting(new ChoiceSetting("skeleton_targets", "Skeleton targets",
            List.of("Everyone", "Players", "Combat target"), "Everyone"));
    private final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Skeleton through walls", true));
    private final NumberSetting lineWidth = addSetting(new NumberSetting("line_width", "Line width", 1.0, 4.0, 0.25, 1.8));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "4AF2EA"));
    private final TextSetting secondaryColor = addSetting(EffectPalette.hexSetting("secondary", "Secondary RGB hex", "8878FF"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.1, 1.0, 0.05, 0.85));
    private final BooleanSetting screenGrade = addSetting(new BooleanSetting("screen_grade", "Screen colour grade", true));
    private static final net.minecraft.resources.Identifier GRADE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("jilaton", "cyberspace");

    public CyberspaceModule() {
        super("cyberspace", "Cyberspace",
                "Cyber colour grade, digital world grid, sonar scan waves, block telemetry and 3D player skeletons.",
                ModuleCategory.RENDER);
    }

    /**
     * Keeps the full-screen grade in vanilla's post-effect slot while enabled. Vanilla resets that slot when the
     * camera entity changes (respawn, spectating), so it is re-applied every tick rather than set once.
     */
    @Override
    protected void onTick(Minecraft client) {
        if (!(client.gameRenderer instanceof dev.jilaton.client.mixin.GameRendererPostEffectAccess post)) return;
        boolean ours = GRADE.equals(post.jilaton$postEffectId());
        boolean want = screenGrade.value() && client.level != null;
        if (want && !ours && post.jilaton$postEffectId() == null) post.jilaton$setPostEffect(GRADE);
        else if (!want && ours) client.gameRenderer.clearPostEffect();
    }

    @Override
    protected void onDisable(Minecraft client) {
        if (client.gameRenderer instanceof dev.jilaton.client.mixin.GameRendererPostEffectAccess post
                && GRADE.equals(post.jilaton$postEffectId())) client.gameRenderer.clearPostEffect();
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        if (client.level == null || client.player == null) return;
        double baseAlpha = opacity.value() * Math.min(1.0, f.intensity);
        if (baseAlpha <= 0.01) return;

        int primaryRgb = resolvePrimary(f.accent);
        int secondaryRgb = EffectPalette.resolve(EffectPalette.CUSTOM, f.accent, secondaryColor.value());
        double sec = f.reducedMotion ? 0.0 : f.seconds() * scanSpeed.value();
        float width = (float) lineWidth.value();

        Vec3 playerPos = client.player.getPosition(f.partial);
        int radius = (int) Math.round(gridRadius.value());
        String mode = preset.value();

        if (!mode.equals("Minimal Wire")) {
            emitCyberGrid(f, client.level, playerPos, radius, sec, primaryRgb, secondaryRgb, baseAlpha, width, mode);
        }
        if (blockWireframe.value() && (mode.equals("Cyber Grid") || mode.equals("Full Construct") || mode.equals("Minimal Wire"))) {
            emitBlockTelemetry(f, client.level, playerPos, Math.min(10, radius / 2 + 2), sec, primaryRgb, secondaryRgb, baseAlpha);
        }
        if (skeleton.value()) {
            emitSkeletons(f, client, playerPos, primaryRgb, secondaryRgb, baseAlpha, width);
        }
    }

    private int resolvePrimary(int accent) {
        String mode = colorMode.value();
        if (mode.equals(EffectPalette.MONO)) {
            return switch (preset.value()) {
                case "Neon Matrix" -> 0x38F89C;
                case "Full Construct" -> 0x5CE6FF;
                default -> EffectPalette.MONO_RGB;
            };
        }
        return EffectPalette.resolve(mode, accent, color.value());
    }

    private void emitCyberGrid(FxFrame f, Level level, Vec3 center, int radius, double sec,
                               int primaryRgb, int secondaryRgb, double baseAlpha, float width, String mode) {
        int cx = (int) Math.floor(center.x);
        int cz = (int) Math.floor(center.z);
        int step = radius > 20 ? 2 : 1;
        double scanWaveR = (sec * 5.5) % Math.max(4.0, radius);
        double maxR = radius;

        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                double dist = Math.hypot(dx, dz);
                if (dist > maxR) continue;
                double wx = cx + dx;
                double wz = cz + dz;
                double wy = FxParticles.groundY(level, wx + 0.5, center.y + 2.5, wz + 0.5, 6);
                if (!Double.isFinite(wy)) continue;

                double radialFade = Math.pow(1.0 - dist / maxR, 1.35);
                double waveDelta = Math.abs(dist - scanWaveR);
                double waveBoost = waveDelta < 1.8 ? (1.0 - waveDelta / 1.8) : 0.0;
                double cellAlpha = baseAlpha * radialFade * (0.42 + 0.5 * waveBoost);
                if (cellAlpha <= 0.015) continue;

                int cellRgb = EffectBatch.mixRgb(primaryRgb, secondaryRgb, waveBoost * 0.65);
                int lineCol = EffectBatch.withAlpha(cellRgb, cellAlpha);
                double rx = wx - f.camX;
                double ry = wy - f.camY + 0.025;
                double rz = wz - f.camZ;
                double s = step;

                f.batch.line(rx, ry, rz, rx + s, ry, rz, lineCol, Math.max(1.0F, width * 0.65F), false);
                f.batch.line(rx, ry, rz, rx, ry, rz + s, lineCol, Math.max(1.0F, width * 0.65F), false);

                if (mode.equals("Full Construct") && waveBoost > 0.45 && ((dx + dz) & 3) == 0) {
                    double pillarH = 0.35 + waveBoost * 0.55;
                    int pillarTop = EffectBatch.withAlpha(secondaryRgb, 0);
                    f.batch.line(rx + 0.5 * s, ry, rz + 0.5 * s,
                            rx + 0.5 * s, ry + pillarH, rz + 0.5 * s, lineCol, 1.2F, false);
                    f.batch.quad(rx + 0.45 * s, ry, rz + 0.5 * s,
                            rx + 0.55 * s, ry, rz + 0.5 * s,
                            rx + 0.55 * s, ry + pillarH, rz + 0.5 * s,
                            rx + 0.45 * s, ry + pillarH, rz + 0.5 * s,
                            lineCol, lineCol, pillarTop, pillarTop, false);
                }
            }
        }
    }

    private void emitBlockTelemetry(FxFrame f, Level level, Vec3 center, int range, double sec,
                                    int primaryRgb, int secondaryRgb, double baseAlpha) {
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int emitted = 0;
        for (int dx = -range; dx <= range && emitted < 42; dx += 2) {
            for (int dz = -range; dz <= range && emitted < 42; dz += 2) {
                for (int dy = -1; dy <= 3 && emitted < 42; dy++) {
                    pos.set(cx + dx, cy + dy, cz + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    pos.set(cx + dx, cy + dy + 1, cz + dz);
                    if (!level.getBlockState(pos).isAir()) continue;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > range || dist < 1.5) continue;
                    double fade = (1.0 - dist / range) * baseAlpha * 0.7;
                    if (fade <= 0.02) continue;

                    double bx = cx + dx - f.camX;
                    double by = cy + dy + 1.004 - f.camY;
                    double bz = cz + dz - f.camZ;
                    int c = EffectBatch.withAlpha(EffectBatch.mixRgb(primaryRgb, secondaryRgb, 0.35), fade);
                    double tick = 0.22;
                    f.batch.line(bx, by, bz, bx + tick, by, bz, c, 1.25F, false);
                    f.batch.line(bx, by, bz, bx, by, bz + tick, c, 1.25F, false);
                    f.batch.line(bx + 1, by, bz + 1, bx + 1 - tick, by, bz + 1, c, 1.25F, false);
                    f.batch.line(bx + 1, by, bz + 1, bx + 1, by, bz + 1 - tick, c, 1.25F, false);
                    emitted++;
                }
            }
        }
    }

    private void emitSkeletons(FxFrame f, Minecraft client, Vec3 viewerPos,
                               int primaryRgb, int secondaryRgb, double baseAlpha, float width) {
        int targetId = CombatTargetTracker.targetId();
        String filter = skeletonTargets.value();
        double maxDistSqr = 48.0 * 48.0;
        boolean walls = throughWalls.value();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) continue;
            boolean isSelf = living == client.player;
            if (isSelf && (f.firstPerson || !skeletonSelf.value())) continue;
            boolean isTarget = living.getId() == targetId;
            if (filter.equals("Players") && !(living instanceof Player)) continue;
            if (filter.equals("Combat target") && !isTarget && !isSelf) continue;
            if (!isSelf && living.distanceToSqr(viewerPos) > maxDistSqr) continue;

            BodyAnchor.SkeletonJoints joints = BodyAnchor.skeleton(living, f.partial);
            emitSkeleton(f.batch, joints, f.camX, f.camY, f.camZ,
                    primaryRgb, secondaryRgb, baseAlpha, width, isTarget, walls);
        }
    }

    /**
     * Emits a pose-accurate 3D humanoid skeleton from solved {@link BodyAnchor.SkeletonJoints}.
     * Pure geometry helper so headless tests can verify joint connectivity and target lock indicators.
     */
    public static void emitSkeleton(EffectBatch batch, BodyAnchor.SkeletonJoints j,
                                    double camX, double camY, double camZ,
                                    int primaryRgb, int secondaryRgb, double alpha,
                                    float lineWidth, boolean targetLocked, boolean walls) {
        if (batch == null || j == null || alpha <= 0.005) return;
        int boneRgb = targetLocked ? EffectBatch.mixRgb(secondaryRgb, 0xFF5E7E, 0.55) : primaryRgb;
        int jointRgb = targetLocked ? 0xFFFFFF : EffectBatch.mixRgb(primaryRgb, secondaryRgb, 0.55);
        int boneColor = EffectBatch.withAlpha(boneRgb, alpha * 0.92);
        int jointColor = EffectBatch.withAlpha(jointRgb, Math.min(1.0, alpha));

        // Spine & Cranium
        bone(batch, v(j.headTop()), v(j.headCenter()), camX, camY, camZ, boneColor, lineWidth, walls);
        bone(batch, v(j.headCenter()), v(j.neck()), camX, camY, camZ, boneColor, lineWidth, walls);
        bone(batch, v(j.neck()), v(j.spine()), camX, camY, camZ, boneColor, lineWidth, walls);
        bone(batch, v(j.spine()), v(j.pelvis()), camX, camY, camZ, boneColor, lineWidth, walls);

        // Cranial halo diamond around headCenter
        Vec3 hc = v(j.headCenter());
        double hx = hc.x - camX, hy = hc.y - camY, hz = hc.z - camZ;
        double hr = 0.19;
        batch.glowLine(hx - hr, hy, hz, hx, hy + hr * 0.7, hz - hr, jointColor, lineWidth * 0.85F, walls);
        batch.glowLine(hx, hy + hr * 0.7, hz - hr, hx + hr, hy, hz, jointColor, lineWidth * 0.85F, walls);
        batch.glowLine(hx + hr, hy, hz, hx, hy - hr * 0.7, hz + hr, jointColor, lineWidth * 0.85F, walls);
        batch.glowLine(hx, hy - hr * 0.7, hz + hr, hx - hr, hy, hz, jointColor, lineWidth * 0.85F, walls);

        // Clavicles & Arms
        bone(batch, v(j.neck()), v(j.leftShoulder()), camX, camY, camZ, boneColor, lineWidth, walls);
        bone(batch, v(j.leftShoulder()), v(j.leftElbow()), camX, camY, camZ, boneColor, lineWidth, walls);
        bone(batch, v(j.leftElbow()), v(j.leftWrist()), camX, camY, camZ, boneColor, lineWidth, walls);

        bone(batch, v(j.neck()), v(j.rightShoulder()), camX, camY, camZ, boneColor, lineWidth, walls);
        bone(batch, v(j.rightShoulder()), v(j.rightElbow()), camX, camY, camZ, boneColor, lineWidth, walls);
        bone(batch, v(j.rightElbow()), v(j.rightWrist()), camX, camY, camZ, boneColor, lineWidth, walls);

        // Ribcage cross-brace
        bone(batch, v(j.leftShoulder()), v(j.spine()), camX, camY, camZ,
                EffectBatch.withAlpha(secondaryRgb, alpha * 0.45), lineWidth * 0.7F, walls);
        bone(batch, v(j.rightShoulder()), v(j.spine()), camX, camY, camZ,
                EffectBatch.withAlpha(secondaryRgb, alpha * 0.45), lineWidth * 0.7F, walls);

        // Pelvis & Legs
        bone(batch, v(j.pelvis()), v(j.leftHip()), camX, camY, camZ, boneColor, lineWidth, walls);
        bone(batch, v(j.leftHip()), v(j.leftKnee()), camX, camY, camZ, boneColor, lineWidth, walls);
        bone(batch, v(j.leftKnee()), v(j.leftAnkle()), camX, camY, camZ, boneColor, lineWidth, walls);

        bone(batch, v(j.pelvis()), v(j.rightHip()), camX, camY, camZ, boneColor, lineWidth, walls);
        bone(batch, v(j.rightHip()), v(j.rightKnee()), camX, camY, camZ, boneColor, lineWidth, walls);
        bone(batch, v(j.rightKnee()), v(j.rightAnkle()), camX, camY, camZ, boneColor, lineWidth, walls);

        // Joint nodes at key articulations
        for (Vec3 node : new Vec3[]{v(j.neck()), v(j.leftShoulder()), v(j.rightShoulder()),
                v(j.leftElbow()), v(j.rightElbow()), v(j.leftWrist()), v(j.rightWrist()),
                v(j.pelvis()), v(j.leftKnee()), v(j.rightKnee())}) {
            jointNode(batch, node, camX, camY, camZ, 0.045, jointColor, walls);
        }

        if (targetLocked) {
            Vec3 sm = v(j.spine());
            double tx = sm.x - camX, ty = sm.y - camY, tz = sm.z - camZ;
            int lockColor = EffectBatch.withAlpha(0xFF5E7E, Math.min(1.0, alpha));
            batch.ring(tx, ty, tz, 0.42, 16, lockColor, lineWidth, walls, 0, 0);
        }
    }

    /** Joint positions are solved as {@code double[]{x, y, z}} by {@link BodyAnchor}; this draws them as points. */
    private static Vec3 v(double[] p) { return new Vec3(p[0], p[1], p[2]); }

    private static void bone(EffectBatch batch, Vec3 a, Vec3 b,
                             double camX, double camY, double camZ,
                             int color, float width, boolean walls) {
        batch.glowLine(a.x - camX, a.y - camY, a.z - camZ,
                b.x - camX, b.y - camY, b.z - camZ, color, width, walls);
    }

    private static void jointNode(EffectBatch batch, Vec3 p,
                                  double camX, double camY, double camZ,
                                  double r, int color, boolean walls) {
        double x = p.x - camX, y = p.y - camY, z = p.z - camZ;
        batch.line(x - r, y, z, x + r, y, z, color, 1.5F, walls);
        batch.line(x, y - r, z, x, y + r, z, color, 1.5F, walls);
        batch.line(x, y, z - r, x, y, z + r, color, 1.5F, walls);
    }
}
