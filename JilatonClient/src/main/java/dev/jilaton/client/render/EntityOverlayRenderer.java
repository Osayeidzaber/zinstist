package dev.jilaton.client.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.gui.ClientTheme;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.module.impl.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.world.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/** Extracts immutable frame data; the drawing phase never reads entities or the client world. */
public final class EntityOverlayRenderer implements AutoCloseable {
    private static final RenderStateDataKey<Frame> KEY = RenderStateDataKey.create(() -> "Jilaton entity overlays");
    private static final int[] EDGES = {0,1,1,5,5,4,4,0, 0,2,1,3,5,7,4,6, 2,3,3,7,7,6,6,2};
    private static final int[] FACES = {0,1,5,4, 2,6,7,3, 0,2,3,1, 1,3,7,5, 4,5,7,6, 0,4,6,2};
    private final ModuleManager modules;
    private final ClientConfig config;
    private final TargetHighlightMotion highlight = new TargetHighlightMotion();
    private Object lastWorld;
    private ByteBufferBuilder storage;
    private MultiBufferSource.BufferSource buffers;

    public EntityOverlayRenderer(ModuleManager modules, ClientConfig config) { this.modules = modules; this.config = config; }

    public void register() {
        WorldRenderEvents.END_EXTRACTION.register(this::extract);
        WorldRenderEvents.END_MAIN.register(this::draw);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> close());
    }

    private void extract(WorldExtractionContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || context.world() == null) { context.worldState().setData(KEY, null); highlight.reset(); return; }
        if (lastWorld != context.world()) { lastWorld = context.world(); highlight.reset(); }
        if (!enabled("player_esp") && !enabled("item_esp") && !enabled("tracers") && !enabled("kill_aura")) {
            context.worldState().setData(KEY, null);
            highlight.reset();
            return;
        }
        int accent = ClientTheme.resolveAccent(config.themeId(), config.customAccent());
        double seconds = System.nanoTime() / 1_000_000_000.0;
        float partial = context.tickCounter().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.camera().position();
        Vector3f forward = new Vector3f(0, 0, -0.2F).rotate(context.camera().rotation());
        List<Box> boxes = new ArrayList<>();
        List<Line> lines = new ArrayList<>();
        if (modules.get("player_esp") instanceof PlayerEspModule esp && esp.enabled() && esp.boxes()) {
            for (LivingEntity entity : esp.options().targets()) {
                if (!esp.options().matches(entity, client, camera) || !context.frustum().isVisible(entity.getBoundingBox())) continue;
                boxes.add(new Box(relativeBox(entity, partial, camera).inflate(esp.expansion()),
                        esp.options().color(entity, accent, camera, seconds), esp.fillOpacity(), esp.outline(),
                        esp.options().lineWidth(), esp.options().throughWalls()));
            }
        }
        if (modules.get("tracers") instanceof TracersModule tracers && tracers.enabled()) {
            for (LivingEntity entity : tracers.options().targets()) {
                if (!tracers.options().matches(entity, client, camera)) continue;
                Vec3 pos = entity.getPosition(partial).subtract(camera);
                int color = tracers.options().color(entity, accent, camera, seconds);
                lines.add(new Line(forward.x, forward.y, forward.z, pos.x, pos.y + tracers.endpoint(entity.getBbHeight()), pos.z,
                        color, tracers.options().lineWidth(), tracers.options().throughWalls()));
                if (tracers.stem()) lines.add(new Line(pos.x, pos.y, pos.z, pos.x, pos.y + entity.getBbHeight(), pos.z,
                        color, tracers.options().lineWidth(), tracers.options().throughWalls()));
            }
        }
        if (modules.get("item_esp") instanceof ItemEspModule items && items.enabled()) {
            int color = items.color(accent, seconds);
            for (Entity entity : items.targets()) {
                if (!items.matches(entity, client, camera)) continue;
                Vec3 pos = entity.getPosition(partial).subtract(camera);
                if (items.boxes() && context.frustum().isVisible(entity.getBoundingBox())) {
                    boxes.add(new Box(relativeBox(entity, partial, camera).inflate(0.04), color, items.fillOpacity(), true, items.lineWidth(), items.throughWalls()));
                }
                if (items.tracers()) lines.add(new Line(forward.x, forward.y, forward.z, pos.x, pos.y + entity.getBbHeight() / 2, pos.z,
                        color, items.lineWidth(), items.throughWalls()));
            }
        }
        Halo halo = null;
        if (modules.get("kill_aura") instanceof KillAuraModule aura) {
            LivingEntity entity = aura.currentTarget();
            boolean active = aura.enabled() && aura.showTargetIndicator() && client.screen == null
                    && entity != null && entity.isAlive() && entity.level() == client.level;
            TargetHighlightMotion.Target tracked = null;
            if (active) {
                Vec3 pos = entity.getPosition(partial);
                tracked = new TargetHighlightMotion.Target(entity.getId(), pos.x, pos.y, pos.z,
                        entity.getBbHeight(), Math.max(0.25, entity.getBbWidth() * 0.72 + 0.08));
            }
            TargetHighlightMotion.State state = highlight.update(tracked, System.nanoTime(), config.reducedMotion());
            if (state != null && client.screen == null && aura.enabled() && aura.showTargetIndicator()) {
                var target = state.target();
                halo = new Halo(target.x() - camera.x, target.y() - camera.y, target.z() - camera.z,
                        target.height(), target.radius(), state.scan(), state.direction(), state.phase(),
                        state.opacity() * aura.highlightOpacity(), aura.highlightColor(accent), aura.highlightWidth(), aura.highlightHalo());
                if (aura.highlightCorners()) addCorners(lines, halo);
            }
        }
        context.worldState().setData(KEY, new Frame(List.copyOf(boxes), List.copyOf(lines), halo));
    }

    private static AABB relativeBox(Entity entity, float partial, Vec3 camera) {
        Vec3 offset = entity.getPosition(partial).subtract(entity.position()).subtract(camera);
        return entity.getBoundingBox().move(offset);
    }

    private boolean enabled(String id) { var module = modules.get(id); return module != null && module.enabled(); }

    private void draw(WorldRenderContext context) {
        Frame frame = context.worldState().getData(KEY);
        if (frame == null || frame.boxes().isEmpty() && frame.lines().isEmpty() && frame.halo() == null) return;
        if (buffers == null) { storage = new ByteBufferBuilder(262144); buffers = MultiBufferSource.immediate(storage); }
        PoseStack.Pose pose = context.matrices().last();
        for (int depth = 0; depth < 2; depth++) {
            boolean walls = depth == 1;
            VertexConsumer fill = buffers.getBuffer(OverlayRenderTypes.fill(walls));
            for (Box box : frame.boxes()) if (box.walls() == walls && box.opacity() > 0) boxFaces(fill, pose, box);
            if (walls && frame.halo() != null && frame.halo().ring()) haloFaces(fill, pose, frame.halo());
            buffers.endBatch();
            VertexConsumer line = buffers.getBuffer(OverlayRenderTypes.line(walls));
            for (Box box : frame.boxes()) if (box.walls() == walls && box.outline()) boxEdges(line, pose, box);
            for (Line segment : frame.lines()) if (segment.walls() == walls) line(line, pose, segment);
            if (walls && frame.halo() != null && frame.halo().ring()) haloLines(line, pose, frame.halo());
            buffers.endBatch();
        }
    }

    private static void boxFaces(VertexConsumer buffer, PoseStack.Pose pose, Box box) {
        int color = alpha(box.color(), box.opacity());
        for (int index : FACES) vertex(buffer, pose, corner(box.bounds(), index, 0), corner(box.bounds(), index, 1), corner(box.bounds(), index, 2), color);
    }

    private static void boxEdges(VertexConsumer buffer, PoseStack.Pose pose, Box box) {
        for (int index = 0; index < EDGES.length; index += 2) {
            int a = EDGES[index], b = EDGES[index + 1];
            line(buffer, pose, corner(box.bounds(), a, 0), corner(box.bounds(), a, 1), corner(box.bounds(), a, 2),
                    corner(box.bounds(), b, 0), corner(box.bounds(), b, 1), corner(box.bounds(), b, 2), box.color(), box.width());
        }
    }

    private static double corner(AABB b, int index, int axis) {
        return switch (axis) { case 0 -> (index & 1) == 0 ? b.minX : b.maxX; case 1 -> (index & 2) == 0 ? b.minY : b.maxY; default -> (index & 4) == 0 ? b.minZ : b.maxZ; };
    }

    private static void haloFaces(VertexConsumer buffer, PoseStack.Pose pose, Halo h) {
        // A feathered, translucent ribbon follows the scanning ring; no opaque cylinder or screen-space dots.
        int segments = 96;
        double y = h.y() + h.height() * h.scan();
        double trail = -Math.signum(h.direction()) * Math.min(0.32, h.height() * 0.16);
        for (int layer = 0; layer < 4; layer++) {
            double from = layer / 4.0, to = (layer + 1) / 4.0;
            int fromColor = alpha(h.color(), h.opacity() * 0.10 * Math.pow(1 - from, 2));
            int toColor = alpha(h.color(), h.opacity() * 0.10 * Math.pow(1 - to, 2));
            for (int i = 0; i < segments; i++) {
                double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
                vertex(buffer, pose, h.x() + Math.cos(a) * h.radius(), y + trail * from, h.z() + Math.sin(a) * h.radius(), fromColor);
                vertex(buffer, pose, h.x() + Math.cos(b) * h.radius(), y + trail * from, h.z() + Math.sin(b) * h.radius(), fromColor);
                vertex(buffer, pose, h.x() + Math.cos(b) * h.radius(), y + trail * to, h.z() + Math.sin(b) * h.radius(), toColor);
                vertex(buffer, pose, h.x() + Math.cos(a) * h.radius(), y + trail * to, h.z() + Math.sin(a) * h.radius(), toColor);
            }
        }
    }

    private static void haloLines(VertexConsumer buffer, PoseStack.Pose pose, Halo h) {
        double y = h.y() + h.height() * h.scan();
        for (int pass = 0; pass < 3; pass++) for (int i = 0; i < 128; i++) {
            double a = i * Math.PI * 2 / 128, b = (i + 1) * Math.PI * 2 / 128;
            double glint = 0.65 + 0.35 * Math.pow(Math.max(0, Math.cos(a - h.phase())), 8);
            int color = pass == 0 ? alpha(0xFF020304, h.opacity() * 0.85)
                    : alpha(h.color(), h.opacity() * (pass == 1 ? 0.12 : glint));
            float width = h.width() + (pass == 0 ? 2 : pass == 1 ? 4 : 0);
            line(buffer, pose, h.x() + Math.cos(a) * h.radius(), y, h.z() + Math.sin(a) * h.radius(),
                    h.x() + Math.cos(b) * h.radius(), y, h.z() + Math.sin(b) * h.radius(), color, width);
        }
    }

    private static void addCorners(List<Line> lines, Halo h) {
        double radius = h.radius() * 0.85, length = radius * 0.36, vertical = Math.min(0.24, h.height() * 0.13);
        int color = alpha(h.color(), h.opacity() * 0.65);
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) for (int top : new int[]{0, 1}) {
            double x = h.x() + sx * radius, z = h.z() + sz * radius, y = h.y() + h.height() * top;
            lines.add(new Line(x,y,z,x-sx*length,y,z,color,h.width(),true));
            lines.add(new Line(x,y,z,x,y,z-sz*length,color,h.width(),true));
            lines.add(new Line(x,y,z,x,y+(top==0?vertical:-vertical),z,color,h.width(),true));
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, double x, double y, double z, int color) {
        buffer.addVertex(pose, (float) x, (float) y, (float) z).setColor(color);
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose, Line line) {
        line(buffer, pose, line.x1(), line.y1(), line.z1(), line.x2(), line.y2(), line.z2(), line.color(), line.width());
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose, double x1, double y1, double z1,
                             double x2, double y2, double z2, int color, float width) {
        double dx = x2-x1, dy = y2-y1, dz = z2-z1;
        double length = Math.sqrt(dx*dx+dy*dy+dz*dz);
        if (length < 1e-6 || !Double.isFinite(length)) return;
        buffer.addVertex(pose, (float)x1, (float)y1, (float)z1).setColor(color)
                .setNormal(pose,(float)(dx/length),(float)(dy/length),(float)(dz/length)).setLineWidth(width);
        buffer.addVertex(pose, (float)x2, (float)y2, (float)z2).setColor(color)
                .setNormal(pose,(float)(dx/length),(float)(dy/length),(float)(dz/length)).setLineWidth(width);
    }

    private static int alpha(int rgb, double opacity) { return (int)Math.round(Math.max(0,Math.min(1,opacity))*255)<<24 | rgb&0xFFFFFF; }
    @Override public void close() { if (storage != null) { storage.close(); storage = null; buffers = null; } highlight.reset(); lastWorld = null; }
    private record Frame(List<Box> boxes, List<Line> lines, Halo halo) { }
    private record Box(AABB bounds, int color, double opacity, boolean outline, float width, boolean walls) { }
    private record Line(double x1,double y1,double z1,double x2,double y2,double z2,int color,float width,boolean walls) { }
    private record Halo(double x,double y,double z,double height,double radius,double scan,double direction,double phase,double opacity,int color,float width,boolean ring) { }
}
