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
    private final NumberSetting duration = addSetting(new NumberSetting("duration", "Duration (seconds)", 0.2, 2.0, 0.05, 0.65));
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
            placements.clear();
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
     * Pure geometry emitter for a placed block at progress {@code [0..1]}, testable headlessly.
     */
    public static void emitPlacementEffect(EffectBatch batch, String style,
                                           double wMinX, double wMinY, double wMinZ,
                                           double wMaxX, double wMaxY, double wMaxZ,
                                           double camX, double camY, double camZ,
                                           double progress, double maxExpand,
                                           int rgb, double baseAlpha, float lineWidth, boolean faceFill) {
        if (batch == null || progress < 0 || progress >= 1.0 || baseAlpha <= 0.005) return;
        double eased = Motion.SETTLE.apply(progress);
        double fade = Math.pow(1.0 - progress, 1.45);
        double alpha = baseAlpha * fade;
        if (alpha <= 0.005) return;

        double minX = wMinX - camX, minY = wMinY - camY, minZ = wMinZ - camZ;
        double maxX = wMaxX - camX, maxY = wMaxY - camY, maxZ = wMaxZ - camZ;
        int coreColor = EffectBatch.withAlpha(rgb, alpha);

        switch (style == null ? "Pulse Box" : style) {
            case "Expanding Wire" -> {
                drawBoxEdges(batch, minX, minY, minZ, maxX, maxY, maxZ,
                        EffectBatch.withAlpha(rgb, alpha * 0.55), Math.max(1.0F, lineWidth * 0.75F));
                double inf = eased * Math.max(0.04, maxExpand * 1.4);
                drawBoxEdges(batch, minX - inf, minY - inf, minZ - inf,
                        maxX + inf, maxY + inf, maxZ + inf, coreColor, lineWidth);
                if (faceFill) {
                    drawBoxFaces(batch, minX, minY, minZ, maxX, maxY, maxZ,
                            EffectBatch.withAlpha(rgb, alpha * 0.14));
                }
            }
            case "Corner Lock" -> {
                double inf = (1.0 - eased) * Math.max(0.05, maxExpand);
                double x0 = minX - inf, y0 = minY - inf, z0 = minZ - inf;
                double x1 = maxX + inf, y1 = maxY + inf, z1 = maxZ + inf;
                double arm = Math.min(0.32, (x1 - x0) * 0.34);
                for (int cx = 0; cx < 2; cx++) {
                    double px = cx == 0 ? x0 : x1;
                    double sx = cx == 0 ? arm : -arm;
                    for (int cy = 0; cy < 2; cy++) {
                        double py = cy == 0 ? y0 : y1;
                        double sy = cy == 0 ? arm : -arm;
                        for (int cz = 0; cz < 2; cz++) {
                            double pz = cz == 0 ? z0 : z1;
                            double sz = cz == 0 ? arm : -arm;
                            batch.glowLine(px, py, pz, px + sx, py, pz, coreColor, lineWidth, false);
                            batch.glowLine(px, py, pz, px, py + sy, pz, coreColor, lineWidth, false);
                            batch.glowLine(px, py, pz, px, py, pz + sz, coreColor, lineWidth, false);
                        }
                    }
                }
                if (faceFill) {
                    drawBoxFaces(batch, minX, minY, minZ, maxX, maxY, maxZ,
                            EffectBatch.withAlpha(rgb, alpha * 0.16));
                }
            }
            case "Cyber Seal" -> {
                drawBoxEdges(batch, minX, minY, minZ, maxX, maxY, maxZ, coreColor, lineWidth);
                double scanY = minY + (maxY - minY) * eased;
                int scanCol = EffectBatch.withAlpha(0xFFFFFF, alpha * 0.85);
                batch.glowLine(minX - 0.02, scanY, minZ - 0.02, maxX + 0.02, scanY, minZ - 0.02, scanCol, lineWidth, false);
                batch.glowLine(maxX + 0.02, scanY, minZ - 0.02, maxX + 0.02, scanY, maxZ + 0.02, scanCol, lineWidth, false);
                batch.glowLine(maxX + 0.02, scanY, maxZ + 0.02, minX - 0.02, scanY, maxZ + 0.02, scanCol, lineWidth, false);
                batch.glowLine(minX - 0.02, scanY, maxZ + 0.02, minX - 0.02, scanY, minZ - 0.02, scanCol, lineWidth, false);
                if (faceFill) {
                    int fillCol = EffectBatch.withAlpha(rgb, alpha * 0.22);
                    batch.quad(minX, scanY, minZ, maxX, scanY, minZ, maxX, scanY, maxZ, minX, scanY, maxZ,
                            fillCol, fillCol, fillCol, fillCol, false);
                }
            }
            default -> {
                double pulse = Math.sin(progress * Math.PI) * maxExpand;
                double x0 = minX - pulse, y0 = minY - pulse, z0 = minZ - pulse;
                double x1 = maxX + pulse, y1 = maxY + pulse, z1 = maxZ + pulse;
                drawBoxEdges(batch, x0, y0, z0, x1, y1, z1, coreColor, lineWidth);
                if (faceFill) {
                    drawBoxFaces(batch, x0, y0, z0, x1, y1, z1, EffectBatch.withAlpha(rgb, alpha * 0.18));
                }
            }
        }
    }

    private static void drawBoxEdges(EffectBatch batch,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ,
                                     int color, float width) {
        batch.glowLine(minX, minY, minZ, maxX, minY, minZ, color, width, false);
        batch.glowLine(maxX, minY, minZ, maxX, minY, maxZ, color, width, false);
        batch.glowLine(maxX, minY, maxZ, minX, minY, maxZ, color, width, false);
        batch.glowLine(minX, minY, maxZ, minX, minY, minZ, color, width, false);

        batch.glowLine(minX, maxY, minZ, maxX, maxY, minZ, color, width, false);
        batch.glowLine(maxX, maxY, minZ, maxX, maxY, maxZ, color, width, false);
        batch.glowLine(maxX, maxY, maxZ, minX, maxY, maxZ, color, width, false);
        batch.glowLine(minX, maxY, maxZ, minX, maxY, minZ, color, width, false);

        batch.glowLine(minX, minY, minZ, minX, maxY, minZ, color, width, false);
        batch.glowLine(maxX, minY, minZ, maxX, maxY, minZ, color, width, false);
        batch.glowLine(maxX, minY, maxZ, maxX, maxY, maxZ, color, width, false);
        batch.glowLine(minX, minY, maxZ, minX, maxY, maxZ, color, width, false);
    }

    private static void drawBoxFaces(EffectBatch batch,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ,
                                     int color) {
        batch.quad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, color, color, color, color, false);
        batch.quad(minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, color, color, color, color, false);
        batch.quad(minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, color, color, color, color, false);
        batch.quad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color, color, color, color, false);
        batch.quad(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, color, color, color, color, false);
        batch.quad(maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color, color, color, color, false);
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
