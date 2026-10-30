package dev.jilaton.client.render;

import dev.jilaton.client.mixin.GameRendererFovAccess;
import dev.jilaton.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** One world-matrix snapshot per frame shared by screen-space world overlays. */
public final class WorldOverlayProjection {
    private final WorldProjection projection = new WorldProjection();
    private Vec3 camera = Vec3.ZERO;
    private Object world;

    public void register(ModuleManager modules) {
        WorldRenderEvents.END_EXTRACTION.register(context -> {
            // The following target card and the above-head target label both project through this matrix.
            boolean hudProjection = modules.get("hud") instanceof dev.jilaton.client.module.impl.HudModule hud
                    && hud.enabled() && (hud.targetHud() && hud.followTarget() || hud.aboveHead());
            if (!enabled(modules, "nametags") && !enabled(modules, "damage_particles") && !hudProjection) { clear(); return; }
            float partial = context.tickCounter().getGameTimeDeltaPartialTick(false);
            float fov = ((GameRendererFovAccess) context.gameRenderer()).jilaton$worldFov(context.camera(), partial, true);
            projection.update(context.gameRenderer().getProjectionMatrix(fov), context.viewMatrix());
            camera = context.camera().position();
            world = context.world();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    private static boolean enabled(ModuleManager modules, String id) {
        var module = modules.get(id);
        return module != null && module.enabled();
    }

    private void clear() { projection.clear(); camera = Vec3.ZERO; world = null; }
    public boolean isCurrent(Minecraft client) { return world != null && world == client.level; }
    public boolean project(Vec3 position, int width, int height) {
        return projection.project(position.x - camera.x, position.y - camera.y, position.z - camera.z, width, height);
    }
    public float x(int width) { return projection.x(width); }
    public float y(int height) { return projection.y(height); }
}
