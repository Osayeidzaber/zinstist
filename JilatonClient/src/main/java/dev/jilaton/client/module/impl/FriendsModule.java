package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Friend attack guard adapted from the supplied FriendGuardModule and MeleeAttacks.stabTarget.
 * Horcus / 4ldenz, GPL-3.0-only. See ATTRIBUTION.md. */
public final class FriendsModule extends ClientModule {
    private final BooleanSetting protectAttacks = addSetting(new BooleanSetting("protect_attacks", "Block attacks on friends", true));
    private final BooleanSetting guardHints = addSetting(new BooleanSetting("guard_hints", "Show blocked-hit hints", true));
    private long lastHint;
    public FriendsModule() {
        super("friends", "Friends",
                "Middle-click a player to add or remove them. Friends wear a blue hat and are protected from combat targeting.",
                ModuleCategory.COMBAT);
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    boolean blocks(String name) {
        return enabled() && protectAttacks.value() && FriendManager.contains(name);
    }

    public boolean intercepts(Player attacker, Entity target) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || attacker != client.player || !(target instanceof Player friend)
                || friend == attacker || !blocks(friend.getGameProfile().name())) return false;
        hint(client, friend);
        return true;
    }

    public boolean interceptsStab(LocalPlayer player) {
        if (!enabled() || !protectAttacks.value() || player == null) return false;
        var range = player.entityAttackRange();
        Vec3 look = player.getViewVector(1);
        Vec3 eye = player.getEyePosition();
        Vec3 start = eye.add(look.scale(range.effectiveMinRange(player)));
        Vec3 end = eye.add(look.scale(range.effectiveMaxRange(player) + Math.max(0, player.getKnownMovement().dot(look))));
        var wall = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        float margin = range.hitboxMargin();
        for (Entity entity : player.level().getEntities(player, new AABB(eye, end).inflate(margin + 1),
                candidate -> candidate instanceof Player friend && blocks(friend.getGameProfile().name()))) {
            AABB box = entity.getBoundingBox().inflate(margin);
            if (box.contains(start) || box.clip(start, end).isPresent()) {
                hint(Minecraft.getInstance(), (Player) entity);
                return true;
            }
        }
        return false;
    }

    private void hint(Minecraft client, Player friend) {
        long now = System.nanoTime();
        if (guardHints.value() && (lastHint == 0 || now - lastHint >= 1_500_000_000L)) {
            lastHint = now;
            client.gui.setOverlayMessage(Component.literal("[Jilaton] Protected friend " + friend.getGameProfile().name()), false);
        }
    }

    @Override protected void onEnable(Minecraft client) { lastHint = 0; }
    @Override protected void onDisable(Minecraft client) { lastHint = 0; }

}
