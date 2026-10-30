/* Adapted from Anubis 0.9.9.3 CustomFovModule and its GameRenderer/ScreenEffect hooks.
 * Horcus / 4ldenz, GPL-3.0-only. See ATTRIBUTION.md. */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class CustomFovModule extends ClientModule {
    private final NumberSetting fov = addSetting(new NumberSetting("fov", "Field of view", 30, 160, 1, 110));
    private int culledFov = -1;
    private float effectScale = 1;

    public CustomFovModule() {
        super("custom_fov", "CustomFOV", "Fixed camera and hand FOV with screen-overlay compensation; preserves saved Minecraft options.", ModuleCategory.RENDER);
    }

    @Override protected void onEnable(Minecraft client) { culledFov = (int) fov.value(); updateTerrain(client); }
    @Override protected void onDisable(Minecraft client) { culledFov = -1; effectScale = 1; updateTerrain(client); }
    @Override protected void onTick(Minecraft client) {
        int current = (int) fov.value();
        if (current != culledFov) { culledFov = current; updateTerrain(client); }
    }

    private static void updateTerrain(Minecraft client) {
        if (client != null && client.level != null && client.levelRenderer != null) client.levelRenderer.needsUpdate();
    }

    public float apply(float vanilla, boolean worldProjection, boolean panoramic) {
        float result = enabled() && !panoramic ? (float) fov.value() : vanilla;
        if (!worldProjection) effectScale = overlayScale(vanilla, result);
        return result;
    }

    public float effectScale() { return enabled() ? effectScale : 1; }

    public static float overlayScale(float vanilla, float applied) {
        if (!Float.isFinite(vanilla) || !Float.isFinite(applied) || vanilla <= 0 || applied <= 0
                || vanilla >= 180 || applied >= 180 || vanilla == applied) return 1;
        return (float) (Math.tan(Math.toRadians(applied) * 0.5) / Math.tan(Math.toRadians(vanilla) * 0.5));
    }
}
