package dev.jilaton.client;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.gui.ClickGuiScreen;
import dev.jilaton.client.gui.HudRenderer;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.module.impl.WTapModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionResult;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class JilatonClient implements ClientModInitializer {
    private static final ModuleManager MODULES = new ModuleManager();
    private static final ClientConfig CONFIG = new ClientConfig(FabricLoader.getInstance().getConfigDir());
    private static KeyMapping openGui;

    public static ModuleManager moduleManager() {
        return MODULES;
    }

    @Override
    public void onInitializeClient() {
        CONFIG.load(MODULES);
        AttackEntityCallback.EVENT.register((attacker, world, hand, target, hitResult) -> {
            ClientModule wtap = MODULES.get("wtap");
            if (wtap instanceof WTapModule module) {
                module.onAttack(attacker, world, target);
            }
            return InteractionResult.PASS;
        });
        HudRenderer hudRenderer = new HudRenderer(MODULES, CONFIG);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("jilaton", "hud"), hudRenderer::render);
        Category category = Category.register(Identifier.fromNamespaceAndPath("jilaton", "main"));
        openGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.jilaton.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MODULES.tick(client);
            while (openGui.consumeClick()) {
                Screen nextScreen = client.screen instanceof ClickGuiScreen ? null : new ClickGuiScreen(MODULES, CONFIG);
                client.setScreen(nextScreen);
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CONFIG.save(MODULES);
            MODULES.disableAll();
        });
    }
}
