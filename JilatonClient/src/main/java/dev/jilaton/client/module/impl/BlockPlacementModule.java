package dev.jilaton.client.module.impl;

import dev.jilaton.client.gui.Motion;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

/**
 * Renders animated 3D wireframe outlines, smooth face glow, corner locks, and expanding box pulses
 * at the real world coordinates of newly placed blocks, fading out cleanly without clutter.
 */
public final class BlockPlacementModule extends ClientModule implements FxEmitter {
    public static final List<String> STYLES = List.of("Pulse Box", "Expanding Wire", "Corner Lock", "Cyber Seal");
    private static final int MAX_PLACEMENTS = 48;

    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Style", STYLES, "Pulse Box"));
    private final NumberSetting duration = addSetting(new NumberSetting("duration", "Duration (seconds)", 0.2, 2.0, 0.05, 0.9));
    private final NumberSetting expand = addSetting(new NumberSetting("expand", "Expansion", 0.0, 0.5, 0.02, 0.16));
    private final NumberSetting lineWidth = addSetting(new NumberSetting("line_width", "Line width", 1.0, 4.5, 0.25, 2.2));
    private final BooleanSetting fill = addSetting(new BooleanSetting("fill", "Face glow fill", true));
    private final BooleanSetting particles = addSetting(new BooleanSetting("particles", "Corner sparks", true));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "F2F2F2"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.1, 1.0, 0.05, 0.88));

    private final ArrayDeque<PlacedBlock> placements = new ArrayDeque<>();
    private Object boundLevel;

    public BlockPlacementModule() {
        super("block_placement", "BlockPlacement",
                "Temporary wireframe outlines, smooth face glow, and expanding pulses on placed blocks.",
                ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        placements.clear();
        boundLevel = null;
    }

    /** Called from UseBlockCallback when the local player places a block. */
    public void onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (!enabled() || player == null || world == null || hitResult == null) return;
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof BlockItem)) return;
        BlockPos clicked = hitResult.getBlockPos();
        BlockPos targetPos = world.getBlockState(clicked).canBeReplaced()
                ? clicked
                : clicked.relative(hitResult.getDirection());
        recordPlacement(targetPos, System.nanoTime());
    }

    public void recordPlacement(BlockPos pos, long nowNanos) {
        if (pos == null) return;
        // Deduplicate rapid callbacks on the same block within 60 ms
        for (PlacedBlock existing : placements) {
            if (existing.pos.equals(pos) && (nowNanos - existing.bornNanos) < 60_000_000L) {
                return;
            }
        }
        placements.addLast(new PlacedBlock(pos.immutable(), nowNanos, false));
        while (placements.size() > MAX_PLACEMENTS) {
            placements.removeFirst();
        }
    }

    public int activeCount() {
        return placements.size();
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        if (client.level == null || client.player == null) {
            placements.clear();
            return;
        }
        if (client.level != boundLevel) {
            // Only a real world switch drops old placements; the first frame after enabling must keep any
            // placement recorded before it.
            if (boundLevel != null) placements.clear();
            boundLevel = client.level;
        }
        if (placements.isEmpty()) return;

        int rgb = EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        double lifeSeconds = duration.value();
        double baseAlpha = opacity.value() * Math.min(1.0, f.intensity);
        float width = (float) lineWidth.value();
        double maxExpand = expand.value();
        String currentStyle = style.value();

        Iterator<PlacedBlock> it = placements.iterator();
        while (it.hasNext()) {
            PlacedBlock placed = it.next();
            double ageSeconds = (f.now - placed.bornNanos) / 1_000_000_000.0;
            double progress = ageSeconds / lifeSeconds;
            if (progress >= 1.0) {
                it.remove();
                continue;
            }
            if (progress < 0) progress = 0;

            AABB bounds = resolveBounds(client.level, placed.pos);
            if (!placed.sparked && particles.value() && !f.reducedMotion) {
                placed.sparked = true;
                spawnCornerSparks(placed.pos, rgb);
            }

            emitPlacementEffect(f.batch, currentStyle,
                    bounds.minX, bounds.minY, bounds.minZ,
                    bounds.maxX, bounds.maxY, bounds.maxZ,
                    f.camX, f.camY, f.camZ,
                    progress, maxExpand, rgb, baseAlpha, width, fill.value());
        }
    }

    private static AABB resolveBounds(Level level, BlockPos pos) {
        if (level != null && level.isLoaded(pos)) {
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return shape.bounds().move(pos).inflate(0.003);
            }
        }
        return new AABB(pos).inflate(0.003);
    }

    private static void spawnCornerSparks(BlockPos pos, int rgb) {
        FxParticles particles = FxParticles.get();
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                particles.add(cx + dx * 0.5, cy + 0.5, cz + dz * 0.5,
                        dx * 0.65, 0.55, dz * 0.65,
                        1.2, 1.8, Double.NaN, 0.42,
                        0.055, 0.01, rgb, 0xFFFFFF, 0.85,
                        FxParticles.Shape.SPARK, true, false, 0, 9102);
            }
        }
    }

    /**
     * Pure geometry emitter for a placed block at progress {@code [0..1]}, testable headlessly. Every style shares
     * the same base: lit faces that flash then drain from the top down, a two-tone edge (dark under-stroke so it
     * reads on bright terrain, bright core on top) and a ripple that spreads over the supporting surface.
     */
    public static void emitPlacementEffect(EffectBatch batch, String style,
                                           double wMinX, double wMinY, double wMinZ,
                                           double wMaxX, double wMaxY, double wMaxZ,
                                           double camX, double camY, double camZ,
                                           double progress, double maxExpand,
                                           int rgb, double baseAlpha, float lineWidth, boolean faceFill) {
        if (batch == null || progress < 0 || progress >= 1.0 || baseAlpha <= 0.005) return;
        double eased = Motion.SETTLE.apply(progress);
        double fade = Math.pow(1.0 - progress, 1.3);
        double alpha = baseAlpha * fade;
        if (alpha <= 0.005) return;

        double minX = wMinX - camX, minY = wMinY - camY, minZ = wMinZ - camZ;
        double maxX = wMaxX - camX, maxY = wMaxY - camY, maxZ = wMaxZ - camZ;
        double cx = (minX + maxX) * 0.5, cz = (minZ + maxZ) * 0.5;
        double size = Math.max(maxX - minX, maxZ - minZ);
        int bright = EffectBatch.mixRgb(rgb, 0xFFFFFF, 0.35);
        // The first 18% is a hard flash, then the glow settles.
        double flash = progress < 0.18 ? 1.0 - progress / 0.18 : 0.0;

        // Ground ripple around the base.
        double rr = size * (0.55 + 0.9 * eased);
        int rippleIn = EffectBatch.withAlpha(rgb, 0);
        int rippleOut = EffectBatch.withAlpha(rgb, alpha * 0.4);
        batch.band(cx, minY + 0.012, cz, rr * 0.78, rr, 40, rippleIn, rippleOut, false);
        batch.glowRing(cx, minY + 0.014, cz, rr, 40, EffectBatch.withAlpha(bright, alpha * 0.8), lineWidth * 0.7F, false, 0, 0);

        if (faceFill) {
            // Faces drain from the top: the lit part shrinks downward as the effect ages.
            double level = minY + (maxY - minY) * (1.0 - eased * 0.85);
            litFaces(batch, minX, minY, minZ, maxX, level, maxZ, rgb, alpha * (0.22 + 0.45 * flash));
        }

        switch (style == null ? "Pulse Box" : style) {
            case "Expanding Wire" -> {
                stroke(batch, minX, minY, minZ, maxX, maxY, maxZ, bright, alpha, lineWidth);
                for (int shell = 0; shell < 2; shell++) {
                    double local = Math.max(0, Math.min(1, (progress - shell * 0.18) / 0.82));
                    double inf = Motion.SETTLE.apply(local) * Math.max(0.08, maxExpand * 1.8) * (shell + 1);
                    double a = alpha * (1 - local) * (shell == 0 ? 0.9 : 0.5);
                    edges(batch, minX - inf, minY - inf * 0.2, minZ - inf, maxX + inf, maxY + inf, maxZ + inf,
                            EffectBatch.withAlpha(rgb, a), lineWidth * 0.8F);
                }
            }
            case "Corner Lock" -> {
                double inf = (1.0 - eased) * Math.max(0.12, maxExpand * 1.5);
                double x0 = minX - inf, y0 = minY - inf, z0 = minZ - inf;
                double x1 = maxX + inf, y1 = maxY + inf, z1 = maxZ + inf;
                double arm = Math.min(0.3, (maxX - minX) * 0.3);
                int core = EffectBatch.withAlpha(bright, alpha);
                int under = EffectBatch.withAlpha(0x000000, alpha * 0.45);
                for (int ix = 0; ix < 2; ix++) for (int iy = 0; iy < 2; iy++) for (int iz = 0; iz < 2; iz++) {
                    double px = ix == 0 ? x0 : x1, py = iy == 0 ? y0 : y1, pz = iz == 0 ? z0 : z1;
                    double sx = ix == 0 ? arm : -arm, sy = iy == 0 ? arm : -arm, sz = iz == 0 ? arm : -arm;
                    double[][] ends = {{px + sx, py, pz}, {px, py + sy, pz}, {px, py, pz + sz}};
                    for (double[] e : ends) {
                        batch.line(px, py, pz, e[0], e[1], e[2], under, lineWidth * 2.4F, false);
                        batch.glowLine(px, py, pz, e[0], e[1], e[2], core, lineWidth * 1.2F, false);
                    }
                }
                if (progress > 0.35) stroke(batch, minX, minY, minZ, maxX, maxY, maxZ, bright, alpha * 0.6, lineWidth * 0.7F);
            }
            case "Cyber Seal" -> {
                stroke(batch, minX, minY, minZ, maxX, maxY, maxZ, rgb, alpha * 0.8, lineWidth * 0.8F);
                double scanY = minY + (maxY - minY) * eased;
                int scan = EffectBatch.withAlpha(0xFFFFFF, alpha);
                double o = 0.02;
                double[][] ring = {{minX - o, minZ - o}, {maxX + o, minZ - o}, {maxX + o, maxZ + o}, {minX - o, maxZ + o}};
                for (int i = 0; i < 4; i++) {
                    double[] p = ring[i], q = ring[(i + 1) % 4];
                    batch.glowLine(p[0], scanY, p[1], q[0], scanY, q[1], scan, lineWidth * 1.2F, false);
                }
                // Grid etched on the faces below the scan line.
                int grid = EffectBatch.withAlpha(rgb, alpha * 0.55);
                for (int k = 1; k < 4; k++) {
                    double y = minY + (maxY - minY) * k / 4.0;
                    if (y > scanY) break;
                    for (int i = 0; i < 4; i++) {
                        double[] p = ring[i], q = ring[(i + 1) % 4];
                        batch.line(p[0], y, p[1], q[0], y, q[1], grid, 1.0F, false);
                    }
                }
                int fillTop = EffectBatch.withAlpha(rgb, alpha * 0.4);
                batch.quad(minX - o, scanY, minZ - o, maxX + o, scanY, minZ - o, maxX + o, scanY, maxZ + o, minX - o, scanY, maxZ + o,
                        fillTop, fillTop, fillTop, fillTop, false);
            }
            default -> {
                double pulse = Math.sin(progress * Math.PI) * Math.max(0.04, maxExpand);
                stroke(batch, minX - pulse, minY - pulse * 0.3, minZ - pulse, maxX + pulse, maxY + pulse, maxZ + pulse,
                        bright, alpha, lineWidth);
                // Light beam from the top face that shoots up and thins out.
                double beamH = (maxY - minY) * (0.4 + 1.6 * eased);
                int beam = EffectBatch.withAlpha(rgb, alpha * 0.5 * (1 - eased));
                int beamTop = EffectBatch.withAlpha(rgb, 0);
                double in = (maxX - minX) * 0.5 * eased;
                double[][] top = {{minX + in, minZ + in}, {maxX - in, minZ + in}, {maxX - in, maxZ - in}, {minX + in, maxZ - in}};
                for (int i = 0; i < 4; i++) {
                    double[] p = top[i], q = top[(i + 1) % 4];
                    batch.quad(p[0], maxY, p[1], q[0], maxY, q[1], q[0], maxY + beamH, q[1], p[0], maxY + beamH, p[1],
                            beam, beam, beamTop, beamTop, false);
                }
            }
        }
    }

    /** Edges as a dark under-stroke plus a bright glowing core, so they read on sky, grass and stone alike. */
    private static void stroke(EffectBatch batch, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                               int rgb, double alpha, float width) {
        edgesRaw(batch, minX, minY, minZ, maxX, maxY, maxZ, EffectBatch.withAlpha(0x000000, alpha * 0.4), width * 2.4F, false);
        edgesRaw(batch, minX, minY, minZ, maxX, maxY, maxZ, EffectBatch.withAlpha(rgb, alpha), width, true);
    }

    private static void edges(EffectBatch batch, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                              int color, float width) {
        edgesRaw(batch, minX, minY, minZ, maxX, maxY, maxZ, color, width, true);
    }

    private static void edgesRaw(EffectBatch batch, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                                 int color, float width, boolean glow) {
        double[][] e = {
                {minX, minY, minZ, maxX, minY, minZ}, {maxX, minY, minZ, maxX, minY, maxZ}, {maxX, minY, maxZ, minX, minY, maxZ}, {minX, minY, maxZ, minX, minY, minZ},
                {minX, maxY, minZ, maxX, maxY, minZ}, {maxX, maxY, minZ, maxX, maxY, maxZ}, {maxX, maxY, maxZ, minX, maxY, maxZ}, {minX, maxY, maxZ, minX, maxY, minZ},
                {minX, minY, minZ, minX, maxY, minZ}, {maxX, minY, minZ, maxX, maxY, minZ}, {maxX, minY, maxZ, maxX, maxY, maxZ}, {minX, minY, maxZ, minX, maxY, maxZ}};
        for (double[] s : e) {
            if (glow) batch.glowLine(s[0], s[1], s[2], s[3], s[4], s[5], color, width, false);
            else batch.line(s[0], s[1], s[2], s[3], s[4], s[5], color, width, false);
        }
    }

    /** Lit faces with a bottom-up gradient: brighter at the base, fading toward the top edge. */
    private static void litFaces(EffectBatch batch, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                                 int rgb, double alpha) {
        if (maxY <= minY) return;
        int[] sides = {EffectBatch.shade(rgb, 0, 0, -1), EffectBatch.shade(rgb, 0, 0, 1), EffectBatch.shade(rgb, -1, 0, 0), EffectBatch.shade(rgb, 1, 0, 0)};
        double[][][] faces = {
                {{minX, minZ}, {maxX, minZ}}, {{maxX, maxZ}, {minX, maxZ}}, {{minX, maxZ}, {minX, minZ}}, {{maxX, minZ}, {maxX, maxZ}}};
        for (int i = 0; i < 4; i++) {
            int lo = EffectBatch.withAlpha(EffectBatch.mixRgb(sides[i], 0xFFFFFF, 0.2), alpha);
            int hi = EffectBatch.withAlpha(sides[i], alpha * 0.35);
            double[] a = faces[i][0], b = faces[i][1];
            batch.quad(a[0], minY, a[1], b[0], minY, b[1], b[0], maxY, b[1], a[0], maxY, a[1], lo, lo, hi, hi, false);
        }
        int top = EffectBatch.withAlpha(EffectBatch.mixRgb(rgb, 0xFFFFFF, 0.4), alpha * 0.7);
        batch.quad(minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, top, top, top, top, false);
    }

    private static final class PlacedBlock {
        final BlockPos pos;
        final long bornNanos;
        boolean sparked;

        PlacedBlock(BlockPos pos, long bornNanos, boolean sparked) {
            this.pos = pos;
            this.bornNanos = bornNanos;
            this.sparked = sparked;
        }
    }
}
