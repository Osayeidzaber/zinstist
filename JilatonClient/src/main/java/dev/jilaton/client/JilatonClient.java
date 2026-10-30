package dev.jilaton.client;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.command.JilatonCommands;
import dev.jilaton.client.gui.ClickGuiScreen;
import dev.jilaton.client.gui.HudRenderer;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.module.impl.WTapModule;
import dev.jilaton.client.module.impl.CriticalsModule;
import dev.jilaton.client.module.impl.FriendManager;
import dev.jilaton.client.module.impl.FriendHatRenderer;
import dev.jilaton.client.module.impl.FriendsModule;
import dev.jilaton.client.module.impl.AutoAnchorModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.InteractionResult;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public final class JilatonClient implements ClientModInitializer {
    private static final ModuleManager MODULES = new ModuleManager();
    private static final ClientConfig CONFIG = new ClientConfig(FabricLoader.getInstance().getConfigDir());
    private static KeyMapping openGui;
    private static KeyMapping openMusic;
    private static KeyMapping inspectKnife;
    private static boolean middleMouseWasDown;

    /** Whether the knife inspect key is held right now (CS skins keep spinning while it is). */
    public static boolean inspectHeld() {
        return inspectKnife != null && inspectKnife.isDown();
    }

    public static ModuleManager moduleManager() {
        return MODULES;
    }

    public static ClientConfig clientConfig() {
        return CONFIG;
    }

    public static void openClickGui(Screen parent) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new ClickGuiScreen(MODULES, CONFIG, parent));
    }

    @Override
    public void onInitializeClient() {
        CONFIG.load(MODULES);
        dev.jilaton.client.render.character.CharacterModule.ensureDropFolder();
        MODULES.notifications().clear();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientModule camera = MODULES.get("freecam");
            if (camera != null) camera.setEnabled(false);
            dev.jilaton.client.render.CombatTargetTracker.clear();
            dev.jilaton.client.render.fx.FxParticles.get().clear();
        });
        JilatonCommands.register(CONFIG);
        dev.jilaton.client.gui.MenuChrome.register();
        dev.jilaton.client.dev.VisualVerifier.registerIfRequested();
        dev.jilaton.client.dev.ViewmodelShots.registerIfRequested();
        AttackEntityCallback.EVENT.register((attacker, world, hand, target, hitResult) -> {
            ClientModule criticals = MODULES.get("criticals");
            if (criticals instanceof CriticalsModule module && module.beforeAttack(attacker, world, target)) {
                return InteractionResult.FAIL;
            }
            ClientModule wtap = MODULES.get("wtap");
            if (wtap instanceof WTapModule module) {
                module.beforeAttack(attacker, world, target);
            }
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player == net.minecraft.client.Minecraft.getInstance().player
                    && MODULES.get("block_placement") instanceof dev.jilaton.client.module.impl.BlockPlacementModule placement) {
                placement.onUseBlock(player, world, hand, hitResult);
            }
            ClientModule autoAnchor = MODULES.get("auto_anchor");
            if (player == net.minecraft.client.Minecraft.getInstance().player
                    && autoAnchor instanceof AutoAnchorModule module
                    && module.handleManualUse(net.minecraft.client.Minecraft.getInstance(), player, world, hand, hitResult)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        var overlayProjection = new dev.jilaton.client.render.WorldOverlayProjection();
        overlayProjection.register(MODULES);
        HudRenderer hudRenderer = new HudRenderer(MODULES, CONFIG, overlayProjection);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("jilaton", "hud"), hudRenderer::render);
        FriendHatRenderer.register();
        new dev.jilaton.client.render.EntityOverlayRenderer(MODULES, CONFIG).register();
        new dev.jilaton.client.render.DamageParticleRenderer().register(MODULES, CONFIG, overlayProjection);
        new dev.jilaton.client.render.NametagRenderer().register(MODULES, CONFIG, overlayProjection);
        Category category = Category.register(Identifier.fromNamespaceAndPath("jilaton", "main"));
        openGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.jilaton.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));
        openMusic = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.jilaton.music_center",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                category
        ));

        inspectKnife = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.jilaton.inspect",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MODULES.tick(client);
            handleFriendMiddleClick(client);
            while (inspectKnife.consumeClick()) {
                if (client.screen == null && MODULES.get("cosmetic_knife") instanceof dev.jilaton.client.module.impl.CosmeticKnifeModule knife)
                    knife.inspect();
            }
            while (openMusic.consumeClick()) {
                if (client.screen == null) client.setScreen(new dev.jilaton.client.gui.MusicCenterScreen(null));
            }
            while (openGui.consumeClick()) {
                if (client.screen instanceof ClickGuiScreen gui) {
                    gui.onClose();
                } else {
                    client.setScreen(new ClickGuiScreen(MODULES, CONFIG, null));
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CONFIG.save(MODULES);
            MODULES.disableAll();
            dev.jilaton.client.music.MusicCenter.get().shutdown();
        });
    }

    private static void handleFriendMiddleClick(net.minecraft.client.Minecraft client) {
        ClientModule friends = MODULES.get("friends");
        if (!(friends instanceof FriendsModule) || !friends.enabled() || client.player == null
                || client.screen != null || client.getWindow() == null) {
            middleMouseWasDown = false;
            return;
        }

        boolean middleMouseDown = GLFW.glfwGetMouseButton(client.getWindow().handle(), GLFW_MOUSE_BUTTON_MIDDLE)
                == GLFW_PRESS;
        if (middleMouseDown && !middleMouseWasDown
                && client.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof Player target && target != client.player) {
            String name = target.getGameProfile().name();
            boolean added = FriendManager.toggle(target);
            CONFIG.save(MODULES);
            client.gui.getChat().addMessage(Component.literal("[Jilaton] "
                    + (added ? "Added " + name + " to friends" : "Removed " + name + " from friends")));
        }
        middleMouseWasDown = middleMouseDown;
    }
}
