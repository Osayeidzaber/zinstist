package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.ItemIdFilter;
import dev.jilaton.client.render.OverlayColors;
import dev.jilaton.client.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Java adaptation of the reviewed LiquidBounce ItemESP filters, box/glow and tracer paths. */
public final class ItemEspModule extends ClientModule {
    private final ChoiceSetting mode = addSetting(new ChoiceSetting("mode", "Mode", List.of("Box", "Glow", "Both"), "Box"));
    private final ChoiceSetting filter = addSetting(new ChoiceSetting("filter", "Item filter", List.of("Blacklist", "Whitelist"), "Blacklist"));
    private final TextSetting itemIds = addSetting(new TextSetting("item_ids", "Item IDs", 4096, "", ItemIdFilter::normalize));
    private final BooleanSetting regular = addSetting(new BooleanSetting("regular_arrows", "Regular arrows", true));
    private final BooleanSetting tipped = addSetting(new BooleanSetting("tipped_arrows", "Tipped arrows", true));
    private final BooleanSetting spectral = addSetting(new BooleanSetting("spectral_arrows", "Spectral arrows", true));
    private final BooleanSetting tridents = addSetting(new BooleanSetting("tridents", "Thrown tridents", true));
    private final BooleanSetting tracers = addSetting(new BooleanSetting("tracers", "Item tracers", false));
    private final BooleanSetting walls = addSetting(new BooleanSetting("through_walls", "Through walls", true));
    private final NumberSetting range = addSetting(new NumberSetting("range", "Maximum distance", 8, 512, 8, 128));
    private final NumberSetting limit = addSetting(new NumberSetting("limit", "Entity limit", 8, 512, 8, 128));
    private final NumberSetting width = addSetting(new NumberSetting("line_width", "Line width", 1, 8, 0.5, 1.5));
    private final NumberSetting fill = addSetting(new NumberSetting("fill_opacity", "Box fill opacity", 0, 0.8, 0.05, 0.2));
    private final ChoiceSetting color = addSetting(new ChoiceSetting("colors", "Color mode", List.of("Gold", "Theme", "Rainbow"), "Gold"));
    private List<Entity> targets = List.of();
    private Set<String> ids = Set.of();
    private String lastIds;

    public ItemEspModule() {
        super("item_esp", "ItemESP", "Dropped-item and collectible-projectile boxes, glow and tracers with item-ID filtering.", ModuleCategory.RENDER);
    }

    public boolean matches(Entity entity, Minecraft client, Vec3 camera) {
        if (client.level == null || entity.level() != client.level || !entity.isAlive() || entity.isRemoved()
                || entity.position().distanceToSqr(camera) > range.value() * range.value()) return false;
        refreshIds();
        if (entity instanceof ItemEntity item) return !item.getItem().isEmpty()
                && ItemIdFilter.allows(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString(), ids, filter.value().equals("Whitelist"));
        if (entity instanceof ThrownTrident) return tridents.value();
        if (entity instanceof AbstractArrow arrow && arrow.pickup == AbstractArrow.Pickup.ALLOWED) {
            if (entity instanceof SpectralArrow) return spectral.value();
            if (entity instanceof Arrow regularArrow) return regularArrow.getColor() == -1 ? regular.value() : tipped.value();
        }
        return false;
    }

    private void refreshIds() {
        if (!itemIds.value().equals(lastIds)) { ids = ItemIdFilter.parse(itemIds.value()); lastIds = itemIds.value(); }
    }

    @Override protected void onEnable(Minecraft client) { onTick(client); }
    @Override protected void onTick(Minecraft client) {
        if (client == null || client.level == null || client.player == null) { targets = List.of(); return; }
        Vec3 camera = client.gameRenderer.getMainCamera().position();
        List<Entity> selected = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) if (matches(entity, client, camera)) selected.add(entity);
        selected.sort(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(camera)));
        if (selected.size() > (int) limit.value()) selected.subList((int) limit.value(), selected.size()).clear();
        targets = List.copyOf(selected);
    }
    @Override protected void onDisable(Minecraft client) { targets = List.of(); }
    public List<Entity> targets() { return targets; }
    public boolean boxes() { return !mode.value().equals("Glow"); }
    public boolean glow() { return !mode.value().equals("Box"); }
    public boolean tracers() { return tracers.value(); }
    public boolean throughWalls() { return walls.value(); }
    public float lineWidth() { return (float) width.value(); }
    public double fillOpacity() { return fill.value(); }
    public int color(int theme, double seconds) { return color.value().equals("Gold") ? 0xFFFFB348 : OverlayColors.resolve(color.value(), theme, 1, 1, 0, seconds, false, false, false); }
}
