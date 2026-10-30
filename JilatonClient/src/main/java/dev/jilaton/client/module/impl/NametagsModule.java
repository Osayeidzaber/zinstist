/* Adapted from Anubis 0.9.9.3 render/NametagsModule.java (Horcus / 4ldenz).
 * GPL-3.0-only. Recovered source is decompiler output; see ATTRIBUTION.md. */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.RecentItems;
import dev.jilaton.client.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class NametagsModule extends ClientModule {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final ChoiceSetting presentation = addSetting(new ChoiceSetting("presentation", "Presentation", List.of("Equipment", "Compact"), "Equipment"));
    private final BooleanSetting self = addSetting(new BooleanSetting("self", "Compact self label in third person", true));
    private final BooleanSetting compactBackground = addSetting(new BooleanSetting("compact_background", "Compact background", false));
    private final ChoiceSetting healthUnits = addSetting(new ChoiceSetting("health_units", "Compact health units", List.of("Hearts", "HP"), "Hearts"));
    private final BooleanSetting ping = addSetting(new BooleanSetting("ping", "Ping", true));
    private final BooleanSetting held = addSetting(new BooleanSetting("held_items", "Held items", true));
    private final BooleanSetting seen = addSetting(new BooleanSetting("seen_items", "Previously seen items", true));
    private final BooleanSetting armor = addSetting(new BooleanSetting("armor", "Armor", true));
    private final BooleanSetting durability = addSetting(new BooleanSetting("durability", "Item durability", true));
    private final BooleanSetting health = addSetting(new BooleanSetting("health", "Health and absorption", true));
    private final BooleanSetting hideVanilla = addSetting(new BooleanSetting("hide_vanilla", "Replace vanilla names", true));
    private final BooleanSetting items = addSetting(new BooleanSetting("items", "Dropped items", true));
    private final BooleanSetting invisible = addSetting(new BooleanSetting("invisible", "Invisible players", false));
    private final NumberSetting range = addSetting(new NumberSetting("range", "Range", 8, 512, 1, 128));
    private final NumberSetting maxTags = addSetting(new NumberSetting("max_tags", "Tag limit", 1, 64, 1, 64));
    private final NumberSetting scale = addSetting(new NumberSetting("scale", "Scale", 0.5, 2, 0.1, 1));
    private final NumberSetting minimum = addSetting(new NumberSetting("min_scale", "Minimum distance scale", 0.3, 1, 0.05, 0.55));
    private final NumberSetting background = addSetting(new NumberSetting("background", "Panel opacity", 0, 1, 0.05, 0.65));
    private final RecentItems<UUID, Item, ItemStack> history = new RecentItems<>(128, 9);
    private List<Target> targets = List.of();
    private Set<Integer> playerIds = Set.of();
    private Object world;

    public NametagsModule() {
        super("nametags", "NameTags", "Equipment panels or compact skin/name/health labels, with optional third-person self label.", ModuleCategory.RENDER);
    }

    @Override protected void onEnable(Minecraft client) { reset(); }
    @Override protected void onDisable(Minecraft client) { reset(); }
    private void reset() { targets = List.of(); playerIds = Set.of(); history.clear(); world = null; }

    @Override protected void onTick(Minecraft client) {
        if (client.level != world) { reset(); world = client.level; }
        if (client.level == null || client.player == null) { reset(); return; }
        double rangeSq = range.value() * range.value();
        List<Player> players = new ArrayList<>();
        for (Player player : client.level.players()) {
            if ((player == client.player && (!compact() || !self.value() || client.options.getCameraType().isFirstPerson()))
                    || player.isRemoved() || !player.isAlive() || player.isSpectator()
                    || (!invisible.value() && player.isInvisible()) || player.distanceToSqr(client.player) > rangeSq) continue;
            players.add(player);
            if (held.value() && seen.value()) {
                remember(player, player.getMainHandItem());
                remember(player, player.getOffhandItem());
            }
        }
        players.sort(Comparator.comparingDouble(p -> p.distanceToSqr(client.player)));
        int budget = (int) maxTags.value();
        List<Target> prepared = new ArrayList<>(budget);
        Set<Integer> ids = new HashSet<>();
        for (Player player : players) {
            if (prepared.size() >= budget) break;
            int latency = -1;
            if (ping.value() && client.getConnection() != null) {
                var info = client.getConnection().getPlayerInfo(player.getUUID());
                if (info != null) latency = info.getLatency();
            }
            List<ItemStack> hands = held.value() && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())
                    ? List.of(player.getMainHandItem().copy(), player.getOffhandItem().copy()) : List.of();
            List<ItemStack> worn = new ArrayList<>();
            if (armor.value()) for (EquipmentSlot slot : ARMOR) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) worn.add(stack.copy());
            }
            List<ItemStack> earlier = held.value() && seen.value()
                    ? history.previous(player.getUUID(), player.getMainHandItem().getItem(), player.getOffhandItem().getItem()) : List.of();
            prepared.add(new Target(player, player.getGameProfile().name(), latency,
                    health.value() ? player.getHealth() + player.getAbsorptionAmount() : Float.NaN,
                    player.getMaxHealth(), hands, List.copyOf(worn), earlier, FriendManager.contains(player), 0));
            ids.add(player.getId());
        }
        if (items.value() && prepared.size() < budget) {
            List<ItemEntity> dropped = new ArrayList<>();
            for (Entity entity : client.level.entitiesForRendering())
                if (entity instanceof ItemEntity item && !item.isRemoved() && item.isAlive() && !item.getItem().isEmpty()
                        && item.distanceToSqr(client.player) <= rangeSq) dropped.add(item);
            dropped.sort(Comparator.comparingDouble(e -> e.distanceToSqr(client.player)));
            for (ItemEntity item : dropped) {
                if (prepared.size() >= budget) break;
                ItemStack stack = item.getItem();
                prepared.add(new Target(item, stack.getHoverName().getString(), -1, Float.NaN, 0,
                        List.of(), List.of(), List.of(), false, stack.getCount()));
            }
        }
        targets = List.copyOf(prepared);
        playerIds = Set.copyOf(ids);
    }

    private void remember(Player player, ItemStack stack) {
        if (!stack.isEmpty()) history.remember(player.getUUID(), stack.getItem(), stack,
                ItemStack::isSameItemSameComponents, stack::copy);
    }

    public boolean shouldHideVanilla(Entity entity, Minecraft client) {
        return enabled() && hideVanilla.value() && client.level == world && client.screen == null
                && !client.options.hideGui && entity.level() == world && playerIds.contains(entity.getId());
    }
    public List<Target> targets() { return targets; }
    public double scale() { return scale.value(); }
    public double minimumScale() { return minimum.value(); }
    public double opacity() { return background.value(); }
    public boolean durability() { return durability.value(); }
    public boolean compact() { return presentation.value().equals("Compact"); }
    public boolean compactBackground() { return compactBackground.value(); }
    public boolean healthInHearts() { return healthUnits.value().equals("Hearts"); }
    public record Target(Entity entity, String name, int ping, float health, float maxHealth,
                         List<ItemStack> hands, List<ItemStack> armor, List<ItemStack> earlier,
                         boolean friend, int droppedCount) { }
}
