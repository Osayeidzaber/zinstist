package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Outlines chests, barrels, shulkers and other storage blocks nearby, each kind in its own colour. */
public final class StorageEspModule extends ClientModule implements FxEmitter {
    private static final int RESCAN_TICKS = 20;
    private static final int MAX_BOXES = 1024;
    private static final int[] BOX_EDGES = {0, 1, 2, 3, 4, 5, 6, 7, 0, 2, 1, 3, 4, 6, 5, 7, 0, 4, 1, 5, 2, 6, 3, 7};

    private final NumberSetting range = addSetting(new NumberSetting("range", "Range (chunks)", 1, 12, 1, 6));
    private final BooleanSetting chests = addSetting(new BooleanSetting("chests", "Chests", true));
    private final BooleanSetting enderChests = addSetting(new BooleanSetting("ender_chests", "Ender chests", true));
    private final BooleanSetting barrels = addSetting(new BooleanSetting("barrels", "Barrels", true));
    private final BooleanSetting shulkers = addSetting(new BooleanSetting("shulkers", "Shulker boxes", true));
    private final BooleanSetting machines = addSetting(new BooleanSetting("machines", "Furnaces, hoppers, droppers", false));
    private final BooleanSetting fill = addSetting(new BooleanSetting("fill", "Fill", true));
    private final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Through walls", true));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.1, 1, 0.05, 0.85));
    private final TextSetting chestColor = addSetting(EffectPalette.hexSetting("chest_color", "Chest color", "F0C060"));
    private final TextSetting enderColor = addSetting(EffectPalette.hexSetting("ender_color", "Ender chest color", "60D8C8"));
    private final TextSetting shulkerColor = addSetting(EffectPalette.hexSetting("shulker_color", "Shulker color", "FF70B0"));
    private final TextSetting otherColor = addSetting(EffectPalette.hexSetting("other_color", "Other color", "BFBFBF"));

    private final List<Found> found = new ArrayList<>();
    private int scanIn;

    public StorageEspModule() {
        super("storage_esp", "StorageESP", "Highlights chests, barrels, shulkers and other containers.", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable(Minecraft client) {
        scanIn = 0;
    }

    @Override
    protected void onDisable(Minecraft client) {
        found.clear();
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.level == null || client.player == null) {
            found.clear();
            return;
        }
        if (scanIn-- > 0) return;
        scanIn = RESCAN_TICKS;
        scan(client.level, client.player.blockPosition());
    }

    private void scan(ClientLevel level, BlockPos center) {
        found.clear();
        int r = (int) Math.round(range.value());
        int cx = center.getX() >> 4, cz = center.getZ() >> 4;
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    int kind = kind(be);
                    if (kind < 0) continue;
                    if (found.size() >= MAX_BOXES) return;
                    BlockPos pos = be.getBlockPos();
                    AABB box = be.getBlockState().getShape(level, pos).isEmpty()
                            ? new AABB(pos) : be.getBlockState().getShape(level, pos).bounds().move(pos);
                    found.add(new Found(box, kind));
                }
            }
        }
    }

    /** 0 chest, 1 ender chest, 2 shulker, 3 other storage, -1 not shown. */
    private int kind(BlockEntity be) {
        if (be instanceof EnderChestBlockEntity) return enderChests.value() ? 1 : -1;
        if (be instanceof ChestBlockEntity) return chests.value() ? 0 : -1;
        if (be instanceof ShulkerBoxBlockEntity) return shulkers.value() ? 2 : -1;
        if (be instanceof BarrelBlockEntity) return barrels.value() ? 3 : -1;
        if (be instanceof AbstractFurnaceBlockEntity || be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity) {
            return machines.value() ? 3 : -1;
        }
        return -1;
    }

    @Override
    public void emit(FxFrame f) {
        if (found.isEmpty()) return;
        int[] colors = {
                EffectPalette.resolve(EffectPalette.CUSTOM, f.accent, chestColor.value()),
                EffectPalette.resolve(EffectPalette.CUSTOM, f.accent, enderColor.value()),
                EffectPalette.resolve(EffectPalette.CUSTOM, f.accent, shulkerColor.value()),
                EffectPalette.resolve(EffectPalette.CUSTOM, f.accent, otherColor.value())};
        double alpha = opacity.value();
        boolean walls = throughWalls.value();
        double[] c = new double[24];
        for (Found item : found) {
            AABB b = item.box;
            for (int i = 0; i < 8; i++) {
                c[i * 3] = ((i & 1) == 0 ? b.minX : b.maxX) - f.camX;
                c[i * 3 + 1] = ((i & 2) == 0 ? b.minY : b.maxY) - f.camY;
                c[i * 3 + 2] = ((i & 4) == 0 ? b.minZ : b.maxZ) - f.camZ;
            }
            int color = colors[item.kind];
            int edge = FxShapes.argb(color, alpha);
            for (int i = 0; i < BOX_EDGES.length; i += 2) {
                int a = BOX_EDGES[i] * 3, d = BOX_EDGES[i + 1] * 3;
                f.batch.line(c[a], c[a + 1], c[a + 2], c[d], c[d + 1], c[d + 2], edge, 1.6F, walls);
            }
            if (fill.value()) {
                int face = FxShapes.argb(color, alpha * 0.18);
                // Top and bottom faces read well from any angle and keep the fill cheap.
                f.batch.quad(c[0], c[1], c[2], c[3], c[4], c[5], c[15], c[16], c[17], c[12], c[13], c[14], face, face, face, face, walls);
                f.batch.quad(c[6], c[7], c[8], c[18], c[19], c[20], c[21], c[22], c[23], c[9], c[10], c[11], face, face, face, face, walls);
            }
        }
    }

    private record Found(AABB box, int kind) { }
}
