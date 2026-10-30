package dev.jilaton.client.module.impl;

import dev.jilaton.client.gui.AnimatedValue;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Independent Java adaptation of LiquidBounce's smooth FOV/scroll zoom behavior. */
public final class ZoomModule extends ClientModule {
    private final NumberSetting fov = addSetting(new NumberSetting("fov", "Zoom FOV", 5, 90, 1, 30));
    private final BooleanSetting hold = addSetting(new BooleanSetting("hold", "Hold key to zoom", true));
    private final BooleanSetting scroll = addSetting(new BooleanSetting("scroll", "Scroll to adjust zoom", true));
    private final NumberSetting scrollSpeed = addSetting(new NumberSetting("scroll_speed", "Scroll speed", 0.5, 8, 0.5, 2));
    private final BooleanSetting smooth = addSetting(new BooleanSetting("smooth", "Smooth transition", true));
    private final BooleanSetting sensitivity = addSetting(new BooleanSetting("sensitivity", "Scale mouse sensitivity", true));
    private final AnimatedValue factor = new AnimatedValue();
    private double currentFactor = 1;
    private double activeFov = 30;
    private double configuredFov = 30;
    private Object playerIdentity;

    public ZoomModule() {
        super("zoom", "Zoom", "Smooth camera zoom with optional hold binding and scroll adjustment.", ModuleCategory.RENDER);
        setKeyCode(GLFW.GLFW_KEY_C);
    }

    @Override public boolean holdKey() { return hold.value(); }

    @Override protected void onEnable(Minecraft client) { configuredFov = activeFov = fov.value(); }

    public float applyFov(Minecraft client, float original, boolean reducedMotion) {
        if (client.player != playerIdentity) {
            playerIdentity = client.player;
            factor.reset();
            factor.update(1, System.nanoTime(), true);
            currentFactor = 1;
        }
        if (configuredFov != fov.value()) configuredFov = activeFov = fov.value();
        boolean active = enabled() && client.player != null && client.screen == null && !client.player.isScoping();
        double baseFov = client.options.fov().get();
        if (dev.jilaton.client.JilatonClient.moduleManager().get("custom_fov") instanceof CustomFovModule custom && custom.enabled())
            baseFov = custom.apply((float) baseFov, true, false);
        double target = active ? zoomFactor(activeFov, baseFov) : 1;
        currentFactor = factor.update(target, System.nanoTime(), reducedMotion || !smooth.value() || client.player == null);
        return (float) Math.max(1, Math.min(179, original * currentFactor));
    }

    public boolean scroll(double amount) {
        if (!enabled() || !scroll.value() || !Double.isFinite(amount) || amount == 0) return false;
        activeFov = Math.max(5, Math.min(90, activeFov - amount * scrollSpeed.value()));
        return true;
    }

    public double sensitivityFactor() { return sensitivity.value() ? currentFactor : 1; }

    static double zoomFactor(double desiredFov, double baseFov) {
        if (!Double.isFinite(desiredFov) || !Double.isFinite(baseFov) || baseFov <= 0) return 1;
        return Math.max(0.02, Math.min(1, desiredFov / baseFov));
    }
}
