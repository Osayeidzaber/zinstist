package dev.jilaton.client.render.character;

import dev.jilaton.client.module.impl.PharaohHeaddressModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Helmet suppression under the Pharaoh headdress. Lives outside the mixin because Mixin rejects any
 * static mixin method that is not private, and {@code @Unique} does not exempt statics.
 */
public final class PharaohHelmetPolicy {
    private PharaohHelmetPolicy() {
    }

    /** The avatar's entity, or null when the render state cannot be tied to a live player. */
    public static Player playerOf(HumanoidRenderState state) {
        if (!(state instanceof AvatarRenderState avatar) || avatar.id == 0) return null;
        Minecraft client = Minecraft.getInstance();
        Level level = client == null ? null : client.level;
        if (level == null) return null;
        return level.getEntity(avatar.id) instanceof Player player ? player : null;
    }

    /**
     * The gate: a player avatar loses its helmet exactly when {@link PharaohHeaddressModule#shows(Player)}
     * draws the headdress for that player. An avatar we cannot resolve (no level, id 0, not a player) is
     * treated as the local avatar mid-render — the only way to reach the armour pass without a resolvable
     * entity — so its helmet follows the module switch alone.
     */
    public static boolean hidesHelmet(EquipmentSlot slot, HumanoidRenderState state, Player player) {
        if (slot != EquipmentSlot.HEAD || !(state instanceof AvatarRenderState)) return false;
        return player == null ? PharaohHeaddressModule.active() != null : PharaohHeaddressModule.shows(player);
    }
}
