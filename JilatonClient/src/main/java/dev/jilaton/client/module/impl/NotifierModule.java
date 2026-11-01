package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Pops a HUD notice (and optionally a chat line) for joins, leaves, totem pops, low health and worn-out armor. */
public final class NotifierModule extends ClientModule {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final BooleanSetting joins = addSetting(new BooleanSetting("joins", "Player joins and leaves", true));
    private final BooleanSetting pops = addSetting(new BooleanSetting("pops", "Totem pops", true));
    private final BooleanSetting health = addSetting(new BooleanSetting("health", "Low health", true));
    private final NumberSetting healthAt = addSetting(new NumberSetting("health_at", "Low health below (hearts)", 1, 10, 0.5, 4));
    private final BooleanSetting armor = addSetting(new BooleanSetting("armor", "Armor about to break", true));
    private final NumberSetting armorAt = addSetting(new NumberSetting("armor_at", "Armor warning below (%)", 1, 50, 1, 10));
    private final BooleanSetting chat = addSetting(new BooleanSetting("chat", "Also print in chat", false));

    private final Set<UUID> online = new HashSet<>();
    private final EnumSet<EquipmentSlot> warned = EnumSet.noneOf(EquipmentSlot.class);
    private boolean seeded;
    private boolean lowWarned;
    private int scanIn;

    public NotifierModule() {
        super("notifier", "Notifier", "Alerts for joins, leaves, totem pops, low health and breaking armor.", ModuleCategory.CLIENT);
    }

    /** Totem pop hook. */
    public static void onPop(LivingEntity entity) {
        if (JilatonClient.moduleManager() != null && JilatonClient.moduleManager().get("notifier") instanceof NotifierModule m
                && m.enabled() && m.pops.value() && entity instanceof Player player) {
            m.notice(player == Minecraft.getInstance().player ? "You popped a totem"
                    : player.getName().getString() + " popped a totem");
        }
    }

    @Override
    protected void onEnable(Minecraft client) {
        seeded = false;
        online.clear();
        warned.clear();
        lowWarned = false;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            seeded = false;
            online.clear();
            return;
        }
        if (joins.value() && --scanIn <= 0) {
            scanIn = 10;
            Set<UUID> now = new HashSet<>();
            for (PlayerInfo info : client.getConnection().getOnlinePlayers()) {
                UUID id = info.getProfile().id();
                now.add(id);
                if (seeded && !online.contains(id)) notice(info.getProfile().name() + " joined");
            }
            if (seeded) {
                for (UUID id : online) {
                    if (!now.contains(id)) notice(nameOf(id) + " left");
                }
            }
            lastNames.clear();
            for (PlayerInfo info : client.getConnection().getOnlinePlayers()) lastNames.put(info.getProfile().id(), info.getProfile().name());
            online.clear();
            online.addAll(now);
            seeded = true;
        }
        if (health.value()) {
            boolean low = client.player.getHealth() + client.player.getAbsorptionAmount() <= healthAt.value() * 2 && client.player.isAlive();
            if (low && !lowWarned) notice("Low health: " + Math.round(client.player.getHealth()) / 2.0 + " hearts");
            lowWarned = low;
        }
        if (armor.value()) {
            for (EquipmentSlot slot : ARMOR) {
                ItemStack stack = client.player.getItemBySlot(slot);
                boolean worn = stack.isDamageableItem()
                        && (stack.getMaxDamage() - stack.getDamageValue()) * 100.0 / stack.getMaxDamage() < armorAt.value();
                if (worn && !warned.contains(slot)) notice(stack.getHoverName().getString() + " is about to break");
                if (worn) warned.add(slot); else warned.remove(slot);
            }
        }
    }

    private final java.util.Map<UUID, String> lastNames = new java.util.HashMap<>();

    private String nameOf(UUID id) {
        String name = lastNames.get(id);
        return name == null ? "A player" : name;
    }

    private void notice(String text) {
        JilatonClient.moduleManager().notifications().push("notice", text, true, System.nanoTime());
        Minecraft client = Minecraft.getInstance();
        if (chat.value() && client.player != null) client.player.displayClientMessage(Component.literal("[Jilaton] " + text), false);
    }
}
