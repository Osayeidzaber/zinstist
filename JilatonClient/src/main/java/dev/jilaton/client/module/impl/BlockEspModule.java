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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Outlines every block on your list near you (ores, spawners, portals...). Chunk sections whose palette cannot
 * hold a listed block are skipped outright, so a scan touches only the sections that might match.
 */
public final class BlockEspModule extends ClientModule implements FxEmitter {
    private static final int MAX_BLOCKS = 2048;
    private static final int[] EDGES = {0, 1, 2, 3, 4, 5, 6, 7, 0, 2, 1, 3, 4, 6, 5, 7, 0, 4, 1, 5, 2, 6, 3, 7};

    private final TextSetting blocks = addSetting(new TextSetting("blocks", "Blocks (ids, comma separated)", 600,
            "diamond_ore,deepslate_diamond_ore,ancient_debris,spawner,emerald_ore,deepslate_emerald_ore",
            value -> value.toLowerCase(Locale.ROOT).replace(" ", "")));
    private final NumberSetting range = addSetting(new NumberSetting("range", "Range (chunks)", 1, 8, 1, 4));
    private final NumberSetting vertical = addSetting(new NumberSetting("vertical", "Vertical range", 16, 384, 16, 64));
    private final BooleanSetting tracers = addSetting(new BooleanSetting("tracers", "Tracers", false));
    private final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Through walls", true));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Color", "6FD3FF"));

    private final List<BlockPos> found = new ArrayList<>();
    private Set<Block> wanted = Set.of();
    private String parsed = "";
    private int scanIn;

    public BlockEspModule() {
        super("block_esp", "BlockESP", "Outlines chosen blocks (ores, spawners...) near you.", ModuleCategory.RENDER);
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
        if (!blocks.value().equals(parsed)) {
            parsed = blocks.value();
            wanted = parse(parsed);
            scanIn = 0;
        }
        if (scanIn-- > 0) return;
        scanIn = 40;
        scan(client.level, client.player.blockPosition());
    }

    private static Set<Block> parse(String list) {
        Set<Block> out = new HashSet<>();
        for (String id : InventoryCleanerModule.parse(list)) {
            net.minecraft.resources.Identifier key = net.minecraft.resources.Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
            if (key == null) continue;
            BuiltInRegistries.BLOCK.getOptional(key).ifPresent(out::add);
        }
        return out;
    }

    private void scan(ClientLevel level, BlockPos center) {
        found.clear();
        if (wanted.isEmpty()) return;
        int r = (int) Math.round(range.value());
        int half = (int) Math.round(vertical.value() / 2);
        int minY = Math.max(level.getMinY(), center.getY() - half), maxY = Math.min(level.getMaxY(), center.getY() + half);
        int cx = center.getX() >> 4, cz = center.getZ() >> 4;
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
                if (chunk == null) continue;
                LevelChunkSection[] sections = chunk.getSections();
                for (int s = 0; s < sections.length; s++) {
                    LevelChunkSection section = sections[s];
                    int baseY = level.getSectionYFromSectionIndex(s) << 4;
                    if (section.hasOnlyAir() || baseY + 15 < minY || baseY > maxY) continue;
                    if (!section.maybeHas(state -> wanted.contains(state.getBlock()))) continue;
                    for (int ly = 0; ly < 16; ly++) {
                        for (int lz = 0; lz < 16; lz++) {
                            for (int lx = 0; lx < 16; lx++) {
                                BlockState state = section.getBlockState(lx, ly, lz);
                                if (!wanted.contains(state.getBlock())) continue;
                                found.add(new BlockPos((x << 4) + lx, baseY + ly, (z << 4) + lz));
                                if (found.size() >= MAX_BLOCKS) return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void emit(FxFrame f) {
        if (found.isEmpty()) return;
        int rgb = EffectPalette.resolve(EffectPalette.CUSTOM, f.accent, color.value());
        int edge = FxShapes.argb(rgb, 0.9);
        boolean walls = throughWalls.value();
        double[] c = new double[24];
        for (BlockPos pos : found) {
            for (int i = 0; i < 8; i++) {
                c[i * 3] = pos.getX() + (i & 1) - f.camX;
                c[i * 3 + 1] = pos.getY() + ((i >> 1) & 1) - f.camY;
                c[i * 3 + 2] = pos.getZ() + ((i >> 2) & 1) - f.camZ;
            }
            for (int i = 0; i < EDGES.length; i += 2) {
                int a = EDGES[i] * 3, b = EDGES[i + 1] * 3;
                f.batch.line(c[a], c[a + 1], c[a + 2], c[b], c[b + 1], c[b + 2], edge, 1.5F, walls);
            }
            if (tracers.value()) {
                f.batch.line(f.forwardX * 0.5, f.forwardY * 0.5, f.forwardZ * 0.5,
                        pos.getX() + 0.5 - f.camX, pos.getY() + 0.5 - f.camY, pos.getZ() + 0.5 - f.camZ,
                        FxShapes.argb(rgb, 0.5), 1F, true);
            }
        }
    }
}
