package dev.jilaton.client.module;

import dev.jilaton.client.module.impl.AimAssistModule;
import dev.jilaton.client.module.impl.AntiBotModule;
import dev.jilaton.client.module.impl.AnchorAuraModule;
import dev.jilaton.client.module.impl.AutoAnchorModule;
import dev.jilaton.client.module.impl.AutoArmorModule;
import dev.jilaton.client.module.impl.AutoLeaveModule;
import dev.jilaton.client.module.impl.AutoBuffRefillModule;
import dev.jilaton.client.module.impl.AutoClickerModule;
import dev.jilaton.client.module.impl.AutoGappleModule;
import dev.jilaton.client.module.impl.AutoHeadModule;
import dev.jilaton.client.module.impl.AutoPotModule;
import dev.jilaton.client.module.impl.AutoSoupModule;
import dev.jilaton.client.module.impl.AutoRodModule;
import dev.jilaton.client.module.impl.AutoShootModule;
import dev.jilaton.client.module.impl.AutoTotemModule;
import dev.jilaton.client.module.impl.AutoWeaponModule;
import dev.jilaton.client.module.impl.BowAimModule;
import dev.jilaton.client.module.impl.CrystalAuraModule;
import dev.jilaton.client.module.impl.ElytraTargetModule;
import dev.jilaton.client.module.impl.CriticalsModule;
import dev.jilaton.client.module.impl.FullbrightModule;
import dev.jilaton.client.module.impl.HighJumpModule;
import dev.jilaton.client.module.impl.FriendsModule;
import dev.jilaton.client.module.impl.HitboxModule;
import dev.jilaton.client.module.impl.HudModule;
import dev.jilaton.client.module.impl.InventoryMoveModule;
import dev.jilaton.client.module.impl.KillAuraModule;
import dev.jilaton.client.module.impl.KeepSprintModule;
import dev.jilaton.client.module.impl.LongJumpModule;
import dev.jilaton.client.module.impl.NoBobModule;
import dev.jilaton.client.module.impl.NoJumpDelayModule;
import dev.jilaton.client.module.impl.NoFallModule;
import dev.jilaton.client.module.impl.NoMissCooldownModule;
import dev.jilaton.client.module.impl.NoSlowModule;
import dev.jilaton.client.module.impl.ParkourModule;
import dev.jilaton.client.module.impl.SafeWalkModule;
import dev.jilaton.client.module.impl.SpeedModule;
import dev.jilaton.client.module.impl.StrafeModule;
import dev.jilaton.client.module.impl.StepModule;
import dev.jilaton.client.module.impl.ReachModule;
import dev.jilaton.client.module.impl.SprintModule;
import dev.jilaton.client.module.impl.SwingAnimationsModule;
import dev.jilaton.client.module.impl.TriggerbotModule;
import dev.jilaton.client.module.impl.TeamsModule;
import dev.jilaton.client.module.impl.TargetStrafeModule;
import dev.jilaton.client.module.impl.VelocityModule;
import dev.jilaton.client.module.impl.WTapModule;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import dev.jilaton.client.rotation.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.Input;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModuleManager {
    private final Map<String, ClientModule> modules = new LinkedHashMap<>();
    private final Collection<ClientModule> modulesView = Collections.unmodifiableCollection(modules.values());
    private final Map<String, Boolean> previousKeyStates = new HashMap<>();
    private final RotationManager rotationManager = new RotationManager();

    public ModuleManager() {
        register(new AntiBotModule());
        register(new FriendsModule());
        register(new TeamsModule());
        register(new AimAssistModule());
        register(new BowAimModule());
        register(new AutoShootModule());
        register(new KillAuraModule());
        register(new ElytraTargetModule());
        register(new AutoLeaveModule());
        register(new HitboxModule());
        register(new ReachModule());
        register(new CriticalsModule());
        register(new CrystalAuraModule());
        register(new AutoAnchorModule());
        register(new AnchorAuraModule());
        register(new TriggerbotModule());
        register(new NoMissCooldownModule());
        register(new VelocityModule());
        register(new WTapModule());
        register(new KeepSprintModule());
        register(new TargetStrafeModule());
        register(new AutoClickerModule());
        register(new AutoTotemModule());
        register(new AutoWeaponModule());
        register(new AutoPotModule());
        register(new AutoRodModule());
        register(new AutoGappleModule());
        register(new AutoSoupModule());
        register(new AutoHeadModule());
        register(new AutoBuffRefillModule());
        register(new AutoArmorModule());
        register(new SprintModule());
        register(new ParkourModule());
        register(new InventoryMoveModule());
        register(new SafeWalkModule());
        register(new StrafeModule());
        register(new SpeedModule());
        register(new NoJumpDelayModule());
        register(new StepModule());
        register(new HighJumpModule());
        register(new LongJumpModule());
        register(new NoFallModule());
        register(new NoSlowModule());
        register(new FullbrightModule());
        register(new SwingAnimationsModule());
        register(new NoBobModule());
        register(new HudModule());
    }

    private void register(ClientModule module) {
        if (modules.putIfAbsent(module.id(), module) != null) {
            throw new IllegalStateException("Duplicate module id: " + module.id());
        }
    }

    public Collection<ClientModule> all() {
        return modulesView;
    }

    public ClientModule get(String id) {
        return modules.get(id);
    }

    public RotationManager rotationManager() {
        return rotationManager;
    }

    public void tick(Minecraft client) {
        rotationManager.beginTick();
        updateKeybinds(client);
        modules.values().forEach(module -> module.tick(client));
        rotationManager.apply(client, id -> {
            ClientModule owner = modules.get(id);
            return owner != null && owner.enabled();
        });
    }

    public Input handleMovementInput(Input input) {
        MovementInputEvent event = dispatchMovementInput(input);
        return event == null ? input : event.input();
    }

    public MovementInputEvent dispatchMovementInput(Input input) {
        MovementInputEvent event = null;
        for (ClientModule module : modules.values()) {
            if (!module.enabled()) {
                continue;
            }
            if (event == null) {
                event = new MovementInputEvent(input);
            }
            module.handleMovementInput(event);
        }
        return event;
    }

    public PlayerMoveEvent dispatchPlayerMove(MoverType type, Vec3 movement) {
        PlayerMoveEvent event = null;
        for (ClientModule module : modules.values()) {
            if (!module.enabled()) {
                continue;
            }
            if (event == null) {
                event = new PlayerMoveEvent(type, movement);
            }
            module.handlePlayerMove(event);
        }
        return event;
    }

    public void dispatchOutgoingPacket(Packet<?> packet) {
        for (ClientModule module : modules.values()) {
            module.handleOutgoingPacket(packet);
        }
    }

    public void disableAll() {
        modules.values().forEach(module -> module.setEnabled(false));
        rotationManager.cancelAll();
    }

    private void updateKeybinds(Minecraft client) {
        if (client.screen != null || client.getWindow() == null) {
            previousKeyStates.replaceAll((id, wasDown) -> false);
            return;
        }

        long window = client.getWindow().handle();
        for (ClientModule module : modules.values()) {
            int keyCode = module.keyCode();
            boolean down = keyCode >= 0 && GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
            boolean wasDown = previousKeyStates.getOrDefault(module.id(), false);
            if (down && !wasDown) {
                module.toggle();
            }
            previousKeyStates.put(module.id(), down);
        }
    }
}
