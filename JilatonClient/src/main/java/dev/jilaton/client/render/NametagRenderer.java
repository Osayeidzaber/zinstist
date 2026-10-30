package dev.jilaton.client.render;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.gui.ClientTheme;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.module.impl.NametagsModule;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** Extracts projected anchors and item/name snapshots; the HUD pass does not read target entities. */
public final class NametagRenderer {
    private List<Tag> frame = List.of();

    public void register(ModuleManager modules, ClientConfig config, WorldOverlayProjection projection) {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> frame = List.of());
        WorldRenderEvents.END_EXTRACTION.register(context -> {
            frame = List.of();
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.screen != null || client.options.hideGui
                    || !(modules.get("nametags") instanceof NametagsModule tags) || !tags.enabled() || tags.targets().isEmpty()) return;
            float partial = context.tickCounter().getGameTimeDeltaPartialTick(false);
            Vec3 camera = context.camera().position();
            List<Tag> next = new ArrayList<>();
            int width = client.getWindow().getGuiScaledWidth(), height = client.getWindow().getGuiScaledHeight();
            for (var target : tags.targets()) {
                var entity = target.entity();
                if (entity == client.player && (!tags.compact() || client.options.getCameraType().isFirstPerson())) continue;
                if (entity.level() != context.world() || entity.isRemoved() || !entity.isAlive()) continue;
                Vec3 anchor = entity.getPosition(partial).add(0, entity instanceof ItemEntity ? 0.35 : entity.getBbHeight() + 0.15, 0);
                Vec3 relative = anchor.subtract(camera);
                if (!projection.isCurrent(client) || !projection.project(anchor, width, height)) continue;
                // The frame retains only immutable text/positions and private ItemStack snapshots.
                next.add(new Tag(projection.x(width), projection.y(height), relative.length(), target.name(),
                        target.ping(), target.health(), target.maxHealth(), target.hands(), target.armor(), target.earlier(),
                        target.friend(), target.droppedCount(), entity instanceof AbstractClientPlayer player ? player.getSkin() : null));
            }
            next.sort(java.util.Comparator.comparingDouble(Tag::distance).reversed());
            frame = List.copyOf(next);
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("jilaton", "nametags"), (graphics, delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || !projection.isCurrent(client) || client.screen != null || client.options.hideGui
                    || !(modules.get("nametags") instanceof NametagsModule tags) || !tags.enabled()) return;
            int accent = ClientTheme.resolveAccent(config.themeId(), config.customAccent());
            for (Tag tag : frame) draw(graphics, client.font, tag, tags, accent);
        });
    }

    private void draw(GuiGraphics graphics, Font font, Tag tag, NametagsModule module, int accent) {
        if (tag.droppedCount() > 0) {
            String text = tag.name() + (tag.droppedCount() > 1 ? " x" + tag.droppedCount() : "");
            float scale = (float) Math.min(1, Math.max(0.25, 6 / Math.max(1, tag.distance())));
            graphics.pose().pushMatrix();
            graphics.pose().translate(tag.x(), tag.y());
            graphics.pose().scale(scale, scale);
            graphics.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2, 0xFFF0EEF8, true);
            graphics.pose().popMatrix();
            return;
        }
        if (module.compact()) { drawCompact(graphics, font, tag, module); return; }
        String ping = tag.ping() < 0 ? "" : tag.ping() + "ms";
        String hp = Float.isFinite(tag.health()) ? (int) Math.ceil(tag.health()) + " hp" : "";
        String distance = Math.round(tag.distance()) + "m";
        int row1 = font.width(tag.name()) + (ping.isEmpty() ? 0 : font.width(ping) + 5);
        int row2 = NametagLayout.append(NametagLayout.iconsWidth(tag.hands().size()), NametagLayout.iconsWidth(tag.armor().size()), 7);
        row2 = NametagLayout.append(row2, font.width(hp), 4);
        row2 = NametagLayout.append(row2, font.width(distance), 4);
        int row3 = tag.earlier().isEmpty() ? 0 : font.width("Seen") + 4 + NametagLayout.iconsWidth(tag.earlier().size());
        int width = Math.max(row1, Math.max(row2, row3)) + 8;
        int height = 8 + font.lineHeight + 3 + 10 + (row3 == 0 ? 0 : 13);
        int x = -width / 2, y = -height - 1;
        float scale = NametagLayout.scale(tag.distance(), module.scale(), module.minimumScale());
        graphics.pose().pushMatrix();
        graphics.pose().translate(tag.x(), tag.y());
        graphics.pose().scale(scale, scale);
        graphics.fill(x, y, x + width, y + height, ((int) Math.round(module.opacity() * 255) << 24) | 0x101116);
        graphics.fill(x, y, x + 1, y + height, tag.friend() ? 0xFF69AFFF : accent);
        boolean shadow = module.opacity() < 0.55;
        int left = x + 4;
        graphics.drawString(font, tag.name(), left, y + 4, tag.friend() ? 0xFF90C8FF : 0xFFF0EEF8, shadow);
        if (!ping.isEmpty()) graphics.drawString(font, ping, left + font.width(tag.name()) + 5, y + 4, NametagLayout.pingColor(tag.ping()), shadow);
        int rowY = y + 4 + font.lineHeight + 3;
        int cursor = icons(graphics, font, tag.hands(), left, rowY, true, module.durability());
        if (!tag.armor().isEmpty()) {
            if (!tag.hands().isEmpty()) { graphics.fill(cursor + 3, rowY + 1, cursor + 4, rowY + 9, 0x40FFFFFF); cursor += 7; }
            cursor = icons(graphics, font, tag.armor(), cursor, rowY, false, module.durability());
        }
        boolean equipped = !tag.hands().isEmpty() || !tag.armor().isEmpty();
        if (!hp.isEmpty()) {
            if (equipped) cursor += 4;
            graphics.drawString(font, hp, cursor, rowY + 1, NametagLayout.healthColor(tag.health(), tag.maxHealth()), shadow);
            cursor += font.width(hp);
        }
        if (equipped || !hp.isEmpty()) cursor += 4;
        graphics.drawString(font, distance, cursor, rowY + 1, 0xFFABB1C4, shadow);
        if (!tag.earlier().isEmpty()) {
            graphics.drawString(font, "Seen", left, rowY + 14, 0xFFABB1C4, shadow);
            icons(graphics, font, tag.earlier(), left + font.width("Seen") + 4, rowY + 13, false, module.durability());
        }
        graphics.pose().popMatrix();
    }

    private void drawCompact(GuiGraphics graphics, Font font, Tag tag, NametagsModule module) {
        String name = font.plainSubstrByWidth(tag.name(), 140);
        String health = NametagLayout.healthText(tag.health(), module.healthInHearts());
        int avatarWidth = tag.skin() == null ? 0 : 16;
        int width = avatarWidth + font.width(name) + (health.isEmpty() ? 0 : font.width(health) + font.width("♥") + 8);
        float scale = NametagLayout.scale(tag.distance(), module.scale(), module.minimumScale());
        graphics.pose().pushMatrix();
        graphics.pose().translate(tag.x(), tag.y());
        graphics.pose().scale(scale, scale);
        int x = -width / 2, y = -16;
        if (module.compactBackground()) graphics.fill(x - 4, y - 3, x + width + 4, y + 14,
                ((int) Math.round(module.opacity() * 255) << 24) | 0x101116);
        if (tag.skin() != null) PlayerFaceRenderer.draw(graphics, tag.skin(), x, y, 12);
        x += avatarWidth;
        graphics.drawString(font, name, x, y + 2, tag.friend() ? 0xFF90C8FF : 0xFFF0EEF8, true);
        if (!health.isEmpty()) {
            x += font.width(name) + 5;
            graphics.drawString(font, health, x, y + 2, 0xFFF0EEF8, true);
            graphics.drawString(font, "♥", x + font.width(health) + 3, y + 2, 0xFFF17EBB, true);
        }
        graphics.pose().popMatrix();
    }

    private int icons(GuiGraphics graphics, Font font, List<ItemStack> stacks, int x, int y, boolean counts, boolean durability) {
        for (int i = 0; i < stacks.size(); i++) {
            if (i > 0) x += 2;
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) graphics.fill(x + 2, y + 2, x + 8, y + 8, 0x30FFFFFF);
            else {
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y);
                graphics.pose().scale(0.625F, 0.625F);
                graphics.renderItem(stack, 0, 0);
                if (durability && stack.isBarVisible()) {
                    graphics.fill(2, 13, 15, 15, 0xFF000000);
                    graphics.fill(2, 13, 2 + stack.getBarWidth(), 14, 0xFF000000 | stack.getBarColor());
                }
                if (counts && stack.getCount() > 1) {
                    String count = Integer.toString(stack.getCount());
                    graphics.drawString(font, count, 17 - font.width(count), 9, 0xFFF0EEF8, true);
                }
                graphics.pose().popMatrix();
            }
            x += 10;
        }
        return x;
    }

    private record Tag(float x, float y, double distance, String name, int ping, float health, float maxHealth,
                       List<ItemStack> hands, List<ItemStack> armor, List<ItemStack> earlier, boolean friend, int droppedCount,
                       PlayerSkin skin) { }
}
