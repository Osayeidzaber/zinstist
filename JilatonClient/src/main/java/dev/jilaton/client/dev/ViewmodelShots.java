package dev.jilaton.client.dev;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.CosmeticKnifeModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.InteractionHand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Dev-only capture of the first-person knife: grip candidates, draw, slash and inspect frames, then quits. Armed
 * only by {@code -Djilaton.viewmodelShots} (optionally {@code =grips} to capture the grip candidates too), so a
 * normal client never runs it.
 */
public final class ViewmodelShots {
    private static final Logger LOGGER = LoggerFactory.getLogger("JilatonShots");
    private record Step(int delay, Runnable action) { }
    private final Queue<Step> steps = new ArrayDeque<>();
    private int settle = 120;
    private int wait;
    private boolean scripted;

    private ViewmodelShots() { }

    public static void registerIfRequested() {
        String mode = System.getProperty("jilaton.viewmodelShots");
        if (mode == null) return;
        ViewmodelShots shots = new ViewmodelShots();
        ClientTickEvents.END_CLIENT_TICK.register(client -> shots.tick(client, mode));
        LOGGER.info("[JilatonShots] viewmodel capture armed ({})", mode);
    }

    private void tick(Minecraft client, String mode) {
        if (client.level == null || client.player == null || client.getSingleplayerServer() == null) return;
        if (!scripted) {
            if (--settle > 0) return;
            script(client, mode);
            scripted = true;
        }
        if (wait > 0) { wait--; return; }
        Step step = steps.poll();
        if (step == null) return;
        step.action().run();
        Step next = steps.peek();
        if (next != null) wait = next.delay();
    }

    private void then(int delay, Runnable action) { steps.add(new Step(delay, action)); }

    private void shot(int delay, String name) {
        then(delay, () -> {
            Minecraft client = Minecraft.getInstance();
            Screenshot.grab(client.gameDirectory, "vm-" + name + ".png", client.getMainRenderTarget(), 1,
                    message -> LOGGER.info("[JilatonShots] {}: {} (hand frames {}, holding {}, slot {})", name,
                            message.getString(), CosmeticKnifeModule.framesDrawn,
                            client.player.getMainHandItem(), client.player.getInventory().getSelectedSlot()));
        });
    }

    private void script(Minecraft client, String mode) {
        CosmeticKnifeModule knife = (CosmeticKnifeModule) JilatonClient.moduleManager().get("cosmetic_knife");
        then(0, () -> {
            command(client, "gamerule doDaylightCycle false");
            command(client, "time set 6000");
            command(client, "weather clear");
            command(client, "gamerule doMobSpawning false");
            command(client, "kill @e[type=!minecraft:player]");
            command(client, "item replace entity @p hotbar.0 with minecraft:diamond_sword");
            client.player.getInventory().setSelectedSlot(0);
            client.options.setCameraType(CameraType.FIRST_PERSON);
            client.options.hideGui = false;
            client.player.setXRot(8);
            for (ClientModule module : JilatonClient.moduleManager().all())
                if (module.id().equals("character-3d")) module.setEnabled(false);
            knife.setHoldOn("Swords");
            knife.setEnabled(true);
        });
        // The server applies the item a tick later and syncs the selected slot back, so select it afterwards.
        then(10, () -> {
            client.player.getInventory().setSelectedSlot(0);
            if (client.getConnection() != null)
                client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(0));
        });
        then(20, () -> CosmeticKnifeModule.debugViewSpace = true);
        shot(10, "debug-viewspace");
        then(1, () -> CosmeticKnifeModule.debugViewSpace = false);
        if (mode.contains("grips")) {
            List<CosmeticKnifeModule.Grip> grips = List.of(
                    new CosmeticKnifeModule.Grip(-0.36, 0.70, -0.02, 0, 180, 0),
                    new CosmeticKnifeModule.Grip(-0.36, 0.70, -0.02, -25, 180, 0),
                    new CosmeticKnifeModule.Grip(-0.36, 0.70, -0.02, 25, 180, 0),
                    new CosmeticKnifeModule.Grip(-0.36, 0.70, -0.02, 0, 180, 25),
                    new CosmeticKnifeModule.Grip(-0.36, 0.70, -0.02, 0, 180, -25),
                    new CosmeticKnifeModule.Grip(-0.36, 0.70, -0.02, 0, 155, 0));
            for (int i = 0; i < grips.size(); i++) {
                CosmeticKnifeModule.Grip g = grips.get(i);
                then(20, () -> CosmeticKnifeModule.grip = g);
                shot(30, "grip-" + i);
            }
            then(1, () -> CosmeticKnifeModule.grip = grips.get(0));
        }
        if (mode.contains("cs")) {
            for (String skin : List.of("CS Katana", "CS Butterfly", "CS Karambit")) {
                String tag = skin.substring(3).toLowerCase(java.util.Locale.ROOT);
                then(20, () -> {
                    knife.setStyle(skin);
                    client.player.getInventory().setSelectedSlot(1);
                });
                then(4, () -> client.player.getInventory().setSelectedSlot(0));
                shot(5, tag + "-deploy");
                shot(40, tag + "-idle");
                then(5, () -> client.player.swing(InteractionHand.MAIN_HAND));
                shot(3, tag + "-slash");
                then(25, knife::inspect);
                shot(15, tag + "-inspect-1");
                shot(20, tag + "-inspect-2");
            }
            then(20, client::stop);
            return;
        }
        shot(40, "idle");
        then(5, () -> knife.motion().startDraw(System.nanoTime()));
        shot(3, "draw-early");
        shot(4, "draw-late");
        then(20, () -> client.player.swing(InteractionHand.MAIN_HAND));
        shot(2, "slash-a-start");
        shot(2, "slash-a-mid");
        then(20, () -> client.player.swing(InteractionHand.MAIN_HAND));
        shot(3, "slash-b-mid");
        then(25, knife::inspect);
        shot(12, "inspect-1");
        shot(14, "inspect-2");
        shot(14, "inspect-3");
        then(40, client::stop);
    }

    private static void command(Minecraft client, String command) {
        IntegratedServer server = client.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }
}
