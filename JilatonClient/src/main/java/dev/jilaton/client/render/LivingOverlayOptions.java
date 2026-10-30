package dev.jilaton.client.render;

import dev.jilaton.client.module.impl.FriendManager;
import dev.jilaton.client.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Shared visual filters. These deliberately do not inherit combat exclusions for friends/teammates. */
public final class LivingOverlayOptions {
    private final BooleanSetting players = new BooleanSetting("players", "Players", true);
    private final BooleanSetting hostiles = new BooleanSetting("hostiles", "Hostile mobs", false);
    private final BooleanSetting passive = new BooleanSetting("passive", "Other living entities", false);
    private final BooleanSetting friends = new BooleanSetting("friends", "Include friends", true);
    private final BooleanSetting invisible = new BooleanSetting("invisible", "Invisible entities", false);
    private final NumberSetting range = new NumberSetting("range", "Maximum distance", 8, 512, 8, 128);
    private final NumberSetting limit = new NumberSetting("limit", "Entity limit", 8, 512, 8, 128);
    private final ChoiceSetting colors = new ChoiceSetting("colors", "Color mode", List.of("Theme", "Health", "Distance", "Rainbow"), "Theme");
    private final BooleanSetting throughWalls = new BooleanSetting("through_walls", "Through walls", true);
    private final NumberSetting lineWidth = new NumberSetting("line_width", "Line width", 1, 8, 0.5, 1.5);
    private List<LivingEntity> targets = List.of();

    public LivingOverlayOptions(Consumer<Setting> register) {
        List.of(players, hostiles, passive, friends, invisible, range, limit, colors, throughWalls, lineWidth).forEach(register);
    }

    public boolean matches(Entity entity, Minecraft client, Vec3 camera) {
        if (!(entity instanceof LivingEntity living) || client.level == null || entity.level() != client.level
                || entity == client.player || !living.isAlive() || entity.isRemoved()) return false;
        return eligible(entity instanceof Player, entity instanceof Monster,
                entity instanceof Player player && player.isSpectator(),
                entity instanceof Player player && FriendManager.contains(player), entity.isInvisible(),
                entity.position().distanceToSqr(camera), players.value(), hostiles.value(), passive.value(),
                friends.value(), invisible.value(), range.value());
    }

    public static boolean eligible(boolean player, boolean hostile, boolean spectator, boolean friend, boolean invisible,
                                   double distanceSquared, boolean players, boolean hostiles, boolean passive,
                                   boolean friends, boolean invisibles, double range) {
        if (!Double.isFinite(distanceSquared) || distanceSquared < 0 || distanceSquared > range * range || spectator) return false;
        if (friend && !friends || invisible && !invisibles) return false;
        return player ? players : hostile ? hostiles : passive;
    }

    public void update(Minecraft client) {
        if (client == null || client.level == null || client.player == null) { clear(); return; }
        Vec3 camera = client.gameRenderer.getMainCamera().position();
        List<LivingEntity> selected = new ArrayList<>();
        Iterable<? extends Entity> entities = hostiles.value() || passive.value() ? client.level.entitiesForRendering() : client.level.players();
        for (Entity entity : entities) if (matches(entity, client, camera)) selected.add((LivingEntity) entity);
        selected.sort(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(camera)));
        if (selected.size() > (int) limit.value()) selected.subList((int) limit.value(), selected.size()).clear();
        targets = List.copyOf(selected);
    }

    public int color(LivingEntity entity, int theme, Vec3 camera, double seconds) {
        return OverlayColors.resolve(colors.value(), theme, entity.getHealth() + entity.getAbsorptionAmount(),
                entity.getMaxHealth(), entity.position().distanceTo(camera), seconds,
                entity instanceof Player player && FriendManager.contains(player), entity.hurtTime > 0, entity.isInvisible());
    }

    public List<LivingEntity> targets() { return targets; }
    public void clear() { targets = List.of(); }
    public boolean throughWalls() { return throughWalls.value(); }
    public float lineWidth() { return (float) lineWidth.value(); }
}
