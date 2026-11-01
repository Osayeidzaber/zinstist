package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/**
 * Hide or fade everything but a block whitelist to see ores through terrain.
 *
 * <p>Ported from Anubis Client ({@code net.anubis.client.module.impl.render.XrayModule}, GPL-3.0, Horcus and
 * 4ldenz): the same snapshot of the whitelist, opacity and exposed-only test, consumed on chunk-mesh worker
 * threads by the {@code XRay*Mixin} hooks (section compile, model and Indigo quad alpha, fluids, block entities,
 * shade, lightmap, occlusion graph). Only the setting types and registration were adapted to Jilaton.</p>
 */
public final class XRayModule extends ClientModule {
    private static final boolean SODIUM = FabricLoader.getInstance().isModLoaded("sodium");
    private static final String ORES = String.join(",",
            "coal_ore", "deepslate_coal_ore", "iron_ore", "deepslate_iron_ore", "gold_ore", "deepslate_gold_ore",
            "lapis_ore", "deepslate_lapis_ore", "redstone_ore", "deepslate_redstone_ore", "diamond_ore",
            "deepslate_diamond_ore", "emerald_ore", "deepslate_emerald_ore", "copper_ore", "deepslate_copper_ore",
            "nether_gold_ore", "nether_quartz_ore", "ancient_debris");
    private static final int RELOAD_DELAY_TICKS = 6;

    /** The whitelist the mesh workers read; null while XRay is off. */
    public static volatile Snapshot active;
    /** Alpha of the block the current mesh worker is compiling (1 = opaque, 0 = hidden). */
    public static final ThreadLocal<float[]> CURRENT_ALPHA = ThreadLocal.withInitial(() -> new float[]{1.0F});

    private final TextSetting blocks = addSetting(new TextSetting("blocks", "Blocks (ids, comma separated)", 2000, ORES,
            value -> value.toLowerCase(Locale.ROOT).replace(" ", "")));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0, 100, 1, 10));
    private final BooleanSetting exposedOnly = addSetting(new BooleanSetting("exposed_only", "Exposed Only", false));
    private int reloadDelay = -1;
    private String seenBlocks;
    private double seenOpacity;
    private boolean seenExposed;

    public XRayModule() {
        super("xray", "XRay", "Hide or fade everything but a block whitelist to see ores through terrain.", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable(Minecraft client) {
        if (SODIUM) {
            if (client.player != null) {
                client.player.displayClientMessage(Component.literal("[Jilaton] Sodium detected; terrain XRay isn't supported with it yet"), false);
            }
            setEnabled(false);
            return;
        }
        rebuildSnapshot();
        reloadWorld(client);
    }

    @Override
    protected void onDisable(Minecraft client) {
        active = null;
        reloadDelay = -1;
        reloadWorld(client);
    }

    @Override
    protected void onTick(Minecraft client) {
        // Anubis reloads on a short delay after a setting change so dragging the opacity slider does not
        // rebuild every chunk on every step.
        if (!blocks.value().equals(seenBlocks) || opacity.value() != seenOpacity || exposedOnly.value() != seenExposed) {
            seenBlocks = blocks.value();
            seenOpacity = opacity.value();
            seenExposed = exposedOnly.value();
            if (active != null) reloadDelay = RELOAD_DELAY_TICKS;
        }
        if (reloadDelay < 0 || --reloadDelay > 0) return;
        reloadDelay = -1;
        rebuildSnapshot();
        reloadWorld(client);
    }

    private static void reloadWorld(Minecraft client) {
        if (client.levelRenderer != null) client.levelRenderer.allChanged();
    }

    private void rebuildSnapshot() {
        ReferenceOpenHashSet<Block> whitelist = new ReferenceOpenHashSet<>();
        for (String id : InventoryCleanerModule.parse(blocks.value())) {
            Identifier key = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
            if (key != null) BuiltInRegistries.BLOCK.getOptional(key).ifPresent(whitelist::add);
        }
        seenBlocks = blocks.value();
        seenOpacity = opacity.value();
        seenExposed = exposedOnly.value();
        active = new Snapshot(whitelist, exposedOnly.value(), (float) (opacity.value() / 100.0));
    }

    public static final class Snapshot {
        private static final Direction[] DIRECTIONS = Direction.values();
        private static final ThreadLocal<BlockPos.MutableBlockPos> SCRATCH_POS = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
        private final ReferenceOpenHashSet<Block> whitelist;
        private final boolean exposedOnly;
        private final float opacity;

        private Snapshot(ReferenceOpenHashSet<Block> whitelist, boolean exposedOnly, float opacity) {
            this.whitelist = whitelist;
            this.exposedOnly = exposedOnly;
            this.opacity = opacity;
        }

        public boolean isWhitelisted(Block block) {
            return whitelist.contains(block);
        }

        public float alphaFor(BlockGetter region, BlockState state, BlockPos pos) {
            if (whitelist.contains(state.getBlock()) && (!exposedOnly || isExposed(region, pos))) return 1.0F;
            return opacity;
        }

        private boolean isExposed(BlockGetter region, BlockPos pos) {
            BlockPos.MutableBlockPos scratch = SCRATCH_POS.get();
            for (Direction direction : DIRECTIONS) {
                scratch.setWithOffset(pos, direction);
                if (!region.getBlockState(scratch).isSolidRender()) return true;
            }
            return false;
        }
    }
}
