package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Cinematic third-person "alt cam": the camera trails the player on a spring instead of being bolted to them, and
 * sits at an adjustable distance. Camera only; aim and attacks still use the player's own rotation.
 */
public final class SmoothCameraModule extends ClientModule {
    private final NumberSetting distance = addSetting(new NumberSetting("distance", "Distance (blocks)", 2, 10, 0.25, 5));
    private final NumberSetting smoothing = addSetting(new NumberSetting("smoothing", "Smoothing", 0, 1, 0.05, 0.55));
    private Vec3 smoothed;
    private long lastNanos;

    public SmoothCameraModule() {
        super("smooth_camera", "Smooth Camera", "Smooth, trailing third-person camera at a custom distance.",
                ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) { smoothed = null; }

    private static SmoothCameraModule active() {
        return JilatonClient.moduleManager() != null
                && JilatonClient.moduleManager().get("smooth_camera") instanceof SmoothCameraModule m && m.enabled()
                ? m : null;
    }

    /** Factor applied to vanilla's third-person zoom (4 blocks); vanilla still clips it against walls. */
    public static float zoomScale() {
        SmoothCameraModule m = active();
        return m == null ? 1F : (float) (m.distance.value() / 4.0);
    }

    /** Where the detached camera should be this frame, given where vanilla put it; null leaves it alone. */
    public static Vec3 follow(Vec3 vanilla, boolean detached) {
        SmoothCameraModule m = active();
        if (m == null || !detached) {
            if (m != null) m.smoothed = null;
            return null;
        }
        long now = System.nanoTime();
        double dt = m.lastNanos == 0 ? 0 : Math.min(0.1, (now - m.lastNanos) / 1e9);
        m.lastNanos = now;
        m.smoothed = SmoothFollow.step(m.smoothed, vanilla, dt, m.smoothing.value());
        return m.smoothed;
    }
}
