package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.AutoTotemModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class AutoTotemPacketMixin {
    private static final int PLAYER_INVENTORY_CONTAINER = 0;
    private static final int OFFHAND_MENU_SLOT = 45;
    private static final int OFFHAND_INVENTORY_SLOT = 40;
    private static final byte TOTEM_POP_EVENT = 35;

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void jilaton$observeOffhandSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (packet.getContainerId() == PLAYER_INVENTORY_CONTAINER && packet.getSlot() == OFFHAND_MENU_SLOT) {
            observe(packet.getContainerId(), packet.getStateId(), packet.getItem());
        }
    }

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void jilaton$observeOffhandContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (packet.containerId() == PLAYER_INVENTORY_CONTAINER && packet.items().size() > OFFHAND_MENU_SLOT) {
            observe(packet.containerId(), packet.stateId(), packet.items().get(OFFHAND_MENU_SLOT));
        }
    }

    @Inject(method = "handleSetPlayerInventory", at = @At("TAIL"))
    private void jilaton$observePlayerInventory(ClientboundSetPlayerInventoryPacket packet, CallbackInfo ci) {
        if (packet.slot() == OFFHAND_INVENTORY_SLOT) {
            observe(-1, -1, packet.contents());
        }
    }

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void jilaton$resetAfterTotemPop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (packet.getEventId() == TOTEM_POP_EVENT && client.level != null && client.player != null
                && packet.getEntity(client.level) == client.player) {
            ClientModule module = JilatonClient.moduleManager().get("auto_totem");
            if (module instanceof AutoTotemModule autoTotem) {
                autoTotem.onTotemPop();
            }
        }
    }

    private static void observe(int containerId, int stateId, ItemStack stack) {
        ClientModule module = JilatonClient.moduleManager().get("auto_totem");
        if (module instanceof AutoTotemModule autoTotem) {
            autoTotem.observeServerOffhand(containerId, stateId, stack);
        }
    }
}
