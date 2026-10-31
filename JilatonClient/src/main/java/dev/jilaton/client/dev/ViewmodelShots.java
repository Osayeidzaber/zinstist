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
                    message -> LOGGER.info("[JilatonShots] {}: {} (fps {}, post {}, hud {}, placed {}, hand frames {}, holding {}, slot {})", name,
                            message.getString(), client.getFps(),
                            ((dev.jilaton.client.mixin.GameRendererPostEffectAccess) client.gameRenderer).jilaton$postEffectId(),
                            JilatonClient.moduleManager().get("hud") != null && JilatonClient.moduleManager().get("hud").enabled(),
                            JilatonClient.moduleManager().get("block_placement") instanceof dev.jilaton.client.module.impl.BlockPlacementModule bp ? bp.activeCount() : -1,
                            CosmeticKnifeModule.framesDrawn,
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
        if (mode.contains("visuals")) {
            visuals(client);
            return;
        }
        if (mode.contains("ui")) {
            screens(client);
            return;
        }
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

    /** One scene per world visual: gate, glass, cyberspace, block placement, jump shockwave, then the ClickGUI. */
    private void visuals(Minecraft client) {
        then(5, () -> enable(false, "cosmetic_knife"));
        shot(60, "fx-baseline");
        then(10, () -> { client.player.setXRot(4); enable(true, "jilaton_gate"); });
        then(20, () -> {
            if (JilatonClient.moduleManager().get("jilaton_gate") instanceof dev.jilaton.client.module.impl.JilatonGateModule gate)
                LOGGER.info("[JilatonShots] gate enabled {} at {} / player {} yaw {}", gate.enabled(), gate.anchorDescription(),
                        client.player.position(), client.player.getYRot());
        });
        shot(20, "fx-gate");
        then(1, () -> client.player.setYRot(client.player.getYRot() + 35));
        shot(10, "fx-gate-angle");
        then(1, () -> {
            enable(false, "jilaton_gate");
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            enable(true, "glass_material");
        });
        shot(30, "fx-glass");
        then(1, () -> { enable(false, "glass_material"); enable(true, "cyberspace"); });
        shot(30, "fx-cyberspace");
        then(1, () -> client.options.setCameraType(CameraType.FIRST_PERSON));
        shot(20, "fx-cyberspace-fp");
        then(1, () -> {
            enable(false, "cyberspace");
            client.player.setXRot(35);
            enable(true, "block_placement");
        });
        then(20, () -> {
            if (JilatonClient.moduleManager().get("block_placement") instanceof dev.jilaton.client.module.impl.BlockPlacementModule placing) {
                net.minecraft.core.BlockPos feet = client.player.blockPosition();
                net.minecraft.core.Direction facing = client.player.getDirection();
                long now = System.nanoTime();
                for (int i = 2; i <= 4; i++) placing.recordPlacement(feet.relative(facing, i), now);
            }
        });
        shot(2, "fx-place-early");
        shot(6, "fx-place-late");
        then(30, () -> {
            enable(false, "block_placement");
            client.player.setXRot(20);
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            setChoice("jump_circles", "style", "Shockwave");
            enable(true, "jump_circles");
        });
        then(10, () -> client.player.jumpFromGround());
        shot(14, "fx-shockwave-land");
        shot(8, "fx-shockwave-spread");
        then(10, () -> {
            enable(false, "jump_circles");
            client.options.setCameraType(CameraType.FIRST_PERSON);
            JilatonClient.openClickGui(null);
        });
        shot(30, "ui-clickgui");
        then(20, client::stop);
    }

    /** Every Jilaton screen in turn, for comparison with the design boards. */
    private void screens(Minecraft client) {
        // An unfocused capture window would otherwise auto-open the pause menu over the in-game shots.
        client.options.pauseOnLostFocus = false;
        then(5, () -> {
            enable(true, "hud");
            if (client.screen != null) client.setScreen(null);
        });
        shot(40, "ui-hud");
        // World lyrics: play the demo track from the capture client's music folder (config/jilaton/music).
        then(1, () -> {
            var center = dev.jilaton.client.music.MusicCenter.get();
            if (JilatonClient.moduleManager().get("music_center") instanceof dev.jilaton.client.module.impl.MusicCenterModule music)
                music.setSource("Local files");
            center.setSource(dev.jilaton.client.music.MusicCenter.Source.LOCAL);
            center.rescan();
        });
        then(30, () -> {
            client.player.setXRot(4);
            var center = dev.jilaton.client.music.MusicCenter.get();
            var library = center.library();
            LOGGER.info("[JilatonShots] library playlists {}", library.playlistNames());
            if (!library.playlistNames().isEmpty())
                center.player().setQueue(library.playlist(library.playlistNames().get(0)), 0);
            enable(true, "spatial_lyrics");
            enable(true, "now_playing");
        });
        then(1, () -> {
            if (client.screen != null) client.setScreen(null);
            client.player.setXRot(4);
        });
        shot(90, "ui-lyrics-a");
        then(1, () -> {
            if (client.screen != null) client.setScreen(null);
            client.player.setXRot(4);
        });
        shot(70, "ui-lyrics-b");
        then(1, () -> JilatonClient.openClickGui(null));
        shot(30, "ui-clickgui");
        then(1, () -> {
            client.setScreen(null);
            JilatonClient.clientConfig().setGuiPage("MODULES");
            JilatonClient.clientConfig().setGuiCategory("Visuals");
            JilatonClient.clientConfig().setGuiExpandedModule("cosmetic_knife");
            JilatonClient.clientConfig().setGuiSelectedModule("cosmetic_knife");
            JilatonClient.openClickGui(null);
        });
        shot(30, "ui-visuals");
        then(1, () -> {
            client.setScreen(null);
            JilatonClient.clientConfig().setGuiCategory("Combat");
            JilatonClient.clientConfig().setGuiExpandedModule("kill_aura");
            JilatonClient.openClickGui(null);
        });
        shot(30, "ui-combat");
        then(1, () -> {
            client.setScreen(null);
            JilatonClient.clientConfig().setGuiPage("THEME");
            JilatonClient.openClickGui(null);
        });
        shot(30, "ui-theme");
        then(1, () -> {
            client.setScreen(null);
            JilatonClient.clientConfig().setGuiPage("MODULES");
            JilatonClient.clientConfig().setGuiCategory("All");
            JilatonClient.clientConfig().setGuiExpandedModule("");
        });
        then(1, () -> client.setScreen(new dev.jilaton.client.gui.MusicCenterScreen(null)));
        shot(30, "ui-music");
        then(1, () -> client.setScreen(new dev.jilaton.client.gui.JilatonServersScreen(null)));
        shot(30, "ui-servers");
        then(1, () -> client.setScreen(new dev.jilaton.client.gui.JilatonPauseScreen()));
        shot(20, "ui-pause");
        then(1, () -> client.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Loading preview")) {
            private final long start = System.nanoTime();

            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                double t = (System.nanoTime() - start) / 1e9;
                dev.jilaton.client.gui.JilatonLoadingScene.render(graphics, width, height, 1, (float) Math.min(0.72, t / 2.0), t, true);
            }
        }));
        shot(20, "ui-loading-a");
        shot(25, "ui-loading-b");
        then(1, () -> client.setScreen(new dev.jilaton.client.gui.JilatonWorldsScreen(null)));
        shot(30, "ui-worlds");
        then(1, () -> client.setScreen(new dev.jilaton.client.gui.JilatonMainMenuScreen()));
        shot(3, "ui-menu-enter-a");
        shot(8, "ui-menu-enter-b");
        shot(12, "ui-menu-enter-c");
        shot(40, "ui-mainmenu");
        then(1, () -> {
            dev.jilaton.client.account.SessionSwitcher.offline().add("Builder_01");
            client.setScreen(new dev.jilaton.client.gui.AccountsScreen(null));
        });
        shot(40, "ui-accounts");
        then(1, () -> client.setScreen(new dev.jilaton.client.gui.AccountAddScreen(new dev.jilaton.client.gui.AccountsScreen(null), true)));
        shot(30, "ui-account-add");
        then(1, () -> client.setScreen(new dev.jilaton.client.gui.AccountAddScreen(new dev.jilaton.client.gui.AccountsScreen(null), false)));
        shot(30, "ui-account-add-ms");
        then(20, client::stop);
    }

    private static void enable(boolean on, String id) {
        ClientModule module = JilatonClient.moduleManager().get(id);
        if (module != null) module.setEnabled(on);
        else LOGGER.warn("[JilatonShots] no module {}", id);
    }

    private static void setChoice(String moduleId, String settingId, String value) {
        ClientModule module = JilatonClient.moduleManager().get(moduleId);
        if (module == null) return;
        for (var setting : module.settings())
            if (setting.id().equals(settingId) && setting instanceof dev.jilaton.client.setting.ChoiceSetting choice)
                choice.setValue(value);
    }

    private static void command(Minecraft client, String command) {
        IntegratedServer server = client.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }
}
