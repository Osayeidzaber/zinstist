package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;

/**
 * Named waypoints per world and dimension, shown as a light beam and a label with the distance. Add them with
 * {@code /jilaton waypoint add <name>}; a death waypoint is set automatically when you die.
 */
public final class WaypointsModule extends ClientModule implements FxEmitter {
    private static final int[] COLORS = {0xF2F2F2, 0x6FD3FF, 0x7CE38B, 0xFFD166, 0xFF8A65, 0xF48FB1};

    private final BooleanSetting beams = addSetting(new BooleanSetting("beams", "Beams", true));
    private final BooleanSetting labels = addSetting(new BooleanSetting("labels", "Labels with distance", true));
    private final BooleanSetting deathPoint = addSetting(new BooleanSetting("death_point", "Mark where you die", true));
    private final NumberSetting maxDistance = addSetting(new NumberSetting("max_distance", "Max distance", 64, 20000, 64, 5000));
    private final NumberSetting labelScale = addSetting(new NumberSetting("label_scale", "Label size", 0.5, 2, 0.1, 1));

    private WaypointStore store;
    private boolean wasDead;

    public WaypointsModule() {
        super("waypoints", "Waypoints", "Saved places with beams and distance labels; /jilaton waypoint add <name>.",
                ModuleCategory.RENDER);
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    public static WaypointsModule get() {
        return JilatonClient.moduleManager() != null
                && JilatonClient.moduleManager().get("waypoints") instanceof WaypointsModule m ? m : null;
    }

    public WaypointStore store() {
        if (store == null) store = new WaypointStore(FabricLoader.getInstance().getConfigDir().resolve("jilaton").resolve("waypoints.tsv"));
        return store;
    }

    /** Identifies the current world: the server address, or the singleplayer save name. */
    public static String worldKey(Minecraft client) {
        if (client.getSingleplayerServer() != null) return "sp:" + client.getSingleplayerServer().getWorldData().getLevelName();
        var server = client.getCurrentServer();
        return server != null ? "mp:" + server.ip.toLowerCase(Locale.ROOT) : "unknown";
    }

    public static String dimensionKey(Minecraft client) {
        return client.level == null ? "" : client.level.dimension().identifier().toString();
    }

    /** Adds a waypoint at the player's feet; the colour cycles so neighbours are easy to tell apart. */
    public WaypointStore.Waypoint add(Minecraft client, String name) {
        var p = client.player.blockPosition();
        int rgb = COLORS[Math.floorMod(name.hashCode(), COLORS.length)];
        WaypointStore.Waypoint w = new WaypointStore.Waypoint(worldKey(client), dimensionKey(client), name, p.getX(), p.getY(), p.getZ(), rgb);
        store().put(w);
        return w;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        boolean dead = client.player.isDeadOrDying();
        if (dead && !wasDead && deathPoint.value()) {
            var p = client.player.blockPosition();
            store().put(new WaypointStore.Waypoint(worldKey(client), dimensionKey(client), "Death", p.getX(), p.getY(), p.getZ(), 0xFF5A5A));
        }
        wasDead = dead;
    }

    @Override
    public void emit(FxFrame f) {
        if (f.client.level == null || f.client.player == null) return;
        List<WaypointStore.Waypoint> points = store().in(worldKey(f.client), dimensionKey(f.client));
        double max = maxDistance.value();
        for (WaypointStore.Waypoint w : points) {
            double x = w.x() + 0.5, y = w.y(), z = w.z() + 0.5;
            double dx = x - f.camX, dy = y - f.camY, dz = z - f.camZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > max) continue;
            // Far waypoints are drawn on a sphere around the camera so they never vanish past the render distance.
            double clamp = dist > 96 ? 96 / dist : 1;
            double px = f.camX + dx * clamp, py = f.camY + dy * clamp, pz = f.camZ + dz * clamp;
            double s = clamp < 1 ? clamp : 1;
            if (beams.value()) {
                double top = py + 60 * s;
                f.batch.glowLine(px - f.camX, py - f.camY, pz - f.camZ, px - f.camX, top - f.camY, pz - f.camZ,
                        FxShapes.argb(w.rgb(), 0.55), 3F, true);
                f.batch.ring(px - f.camX, py + 0.03 - f.camY, pz - f.camZ, 0.6 * s, 28, FxShapes.argb(w.rgb(), 0.8), 1.6F, true, 0, 0);
            }
            if (labels.value()) {
                String text = w.name() + "  " + (dist < 1000 ? Math.round(dist) + "m" : String.format(Locale.ROOT, "%.1fkm", dist / 1000));
                float scale = (float) (0.025 * labelScale.value() * Math.max(1, Math.min(dist, 96) / 8));
                f.text(new FxFrame.WorldText(px, py + 1.8 * Math.max(s, 0.3), pz,
                        List.of(new FxFrame.Run(text, FxShapes.argb(w.rgb(), 1))), scale * (float) Math.min(1, clamp * 4 + 0.2),
                        0xA0000000, 0, true, 0, true));
            }
        }
    }
}
