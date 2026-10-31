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
import org.joml.Quaternionf;
import org.joml.Vector3f;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxParticles;
import dev.jilaton.client.render.fx.VisualQuality;
import dev.jilaton.client.module.ClientModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger("Jilaton");
    private long lastFrameNanos;

    public EntityOverlayRenderer(ModuleManager modules, ClientConfig config) { this.modules = modules; this.config = config; }

    public void register() {
        WorldRenderEvents.END_EXTRACTION.register(this::extract);
        WorldRenderEvents.END_MAIN.register(this::draw);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> close());
        // Build the overlay pipelines once the client is up, so a bad pipeline fails at launch, not on the first world frame.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> OverlayRenderTypes.fill(false));
    }

    private void extract(WorldExtractionContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || context.world() == null) { context.worldState().setData(KEY, null); highlight.reset(); return; }
        if (lastWorld != context.world()) { lastWorld = context.world(); highlight.reset(); FxParticles.get().clear(); }
        if (!enabled("player_esp") && !enabled("item_esp") && !enabled("tracers") && !enabled("kill_aura")
                && !enabled("target_esp") && !enabled("jump_circles") && !enabled("trails") && !anyFx()) {
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
        long now = System.nanoTime();
        EffectBatch effects = new EffectBatch();
        if (modules.get("trails") instanceof TrailsModule trails) trails.emit(effects, client, partial, camera, accent, now);
        if (modules.get("jump_circles") instanceof JumpCirclesModule circles) circles.emit(effects, client.level, camera, accent, now);
        if (modules.get("target_esp") instanceof TargetEspModule targetEsp)
            targetEsp.emit(effects, client, partial, camera, accent, now, config.reducedMotion());
        Quaternionf rotation = new Quaternionf(context.camera().rotation());
        FxFrame fx = fxFrame(client, context, effects, camera, rotation, accent, partial, now);
        Halo halo = null;
        if (modules.get("kill_aura") instanceof KillAuraModule aura && !enabled("target_esp")) {
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
        context.worldState().setData(KEY, new Frame(List.copyOf(boxes), List.copyOf(lines), halo, effects,
                List.copyOf(fx.texts()), List.copyOf(fx.images()), List.copyOf(fx.sky()), rotation));
    }

    private boolean anyFx() {
        if (FxParticles.get().count() > 0 || FxParticles.get().decalCount() > 0) return true;
        for (ClientModule module : modules.all()) if (module instanceof FxEmitter && module.enabled()) return true;
        return false;
    }

    /** Runs every enabled {@link FxEmitter} and the shared particle simulation for this frame. */
    private FxFrame fxFrame(Minecraft client, WorldExtractionContext context, EffectBatch effects, Vec3 camera,
                            Quaternionf rotation, int accent, float partial, long now) {
        double dt = lastFrameNanos == 0 ? 0 : Math.min(0.1, (now - lastFrameNanos) / 1e9);
        lastFrameNanos = now;
        Vector3f right = new Vector3f(1, 0, 0).rotate(rotation), up = new Vector3f(0, 1, 0).rotate(rotation),
                forward = new Vector3f(0, 0, -1).rotate(rotation);
        VisualQuality quality = VisualQuality.BALANCED;
        double opacity = 1;
        if (modules.get("visual_engine") instanceof VisualEngineModule engine) {
            quality = engine.quality(dt);
            opacity = engine.opacity();
        }
        FxFrame frame = new FxFrame(client, effects, camera.x, camera.y, camera.z,
                new float[]{right.x, right.y, right.z}, new float[]{up.x, up.y, up.z}, new float[]{forward.x, forward.y, forward.z},
                now, dt, partial, accent, quality, config.reducedMotion(), opacity, !context.camera().isDetached());
        for (ClientModule module : modules.all()) {
            if (!(module instanceof FxEmitter emitter) || !module.enabled()) continue;
            try {
                emitter.emit(frame);
            } catch (RuntimeException failure) {
                LOGGER.error("World effect '{}' failed and was disabled", module.id(), failure);
                module.setEnabled(false);
            }
        }
        FxParticles.get().step(frame);
        return frame;
    }

    private static AABB relativeBox(Entity entity, float partial, Vec3 camera) {
        Vec3 offset = entity.getPosition(partial).subtract(entity.position()).subtract(camera);
        return entity.getBoundingBox().move(offset);
    }

    private boolean enabled(String id) { var module = modules.get(id); return module != null && module.enabled(); }

    private void draw(WorldRenderContext context) {
        Frame frame = context.worldState().getData(KEY);
        if (frame == null || frame.boxes().isEmpty() && frame.lines().isEmpty() && frame.halo() == null && frame.effects().isEmpty()
                && frame.texts().isEmpty() && frame.images().isEmpty() && frame.sky().isEmpty()) return;
        if (buffers == null) { storage = new ByteBufferBuilder(262144); buffers = MultiBufferSource.immediate(storage); }
        PoseStack.Pose pose = context.matrices().last();
        for (int depth = 0; depth < 2; depth++) {
            boolean walls = depth == 1;
            VertexConsumer fill = buffers.getBuffer(OverlayRenderTypes.fill(walls));
            for (Box box : frame.boxes()) if (box.walls() == walls && box.opacity() > 0) boxFaces(fill, pose, box);
            if (walls && frame.halo() != null && frame.halo().ring()) haloFaces(fill, pose, frame.halo());
            for (EffectBatch.Quad quad : frame.effects().quads()) if (quad.walls() == walls) quad(fill, pose, quad);
            buffers.endBatch();
            VertexConsumer line = buffers.getBuffer(OverlayRenderTypes.line(walls));
            for (Box box : frame.boxes()) if (box.walls() == walls && box.outline()) boxEdges(line, pose, box);
            for (Line segment : frame.lines()) if (segment.walls() == walls) line(line, pose, segment);
            if (walls && frame.halo() != null && frame.halo().ring()) haloLines(line, pose, frame.halo());
            for (EffectBatch.Segment s : frame.effects().segments())
                if (s.walls() == walls) line(line, pose, s.x1(), s.y1(), s.z1(), s.x2(), s.y2(), s.z2(), s.color(), s.width());
            buffers.endBatch();
        }
        if (!frame.sky().isEmpty()) drawSky(pose, frame);
        if (!frame.images().isEmpty()) drawImages(context.matrices(), frame);
        if (!frame.texts().isEmpty()) drawTexts(context.matrices(), frame);
    }

    /** Textured skybox faces, in frame order; batch flushes on every texture/blend change to keep the layering exact. */
    private void drawSky(PoseStack.Pose pose, Frame frame) {
        RenderType current = null;
        for (FxFrame.SkyQuad quad : frame.sky()) {
            RenderType type = OverlayRenderTypes.sky(quad.texture());
            if (type != current) {
                if (current != null) buffers.endBatch();
                current = type;
            }
            VertexConsumer buffer = buffers.getBuffer(type);
            float[] p = quad.positions(), uv = quad.uvs();
            for (int i = 0; i < 4; i++)
                buffer.addVertex(pose, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).setUv(uv[i * 2], uv[i * 2 + 1]).setColor(quad.color());
        }
        buffers.endBatch();
    }

    private void drawImages(PoseStack matrices, Frame frame) {
        for (FxFrame.WorldImage image : frame.images()) {
            matrices.pushPose();
            matrices.translate(image.x(), image.y(), image.z());
            if (image.billboard()) matrices.mulPose(frame.rotation());
            else matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-image.yaw()));
            PoseStack.Pose pose = matrices.last();
            float h = image.size() / 2;
            int color = (int) (Math.max(0, Math.min(1, image.alpha())) * 255) << 24 | 0xFFFFFF;
            VertexConsumer buffer = buffers.getBuffer(RenderTypes.entityTranslucent(image.texture()));
            imageVertex(buffer, pose, -h, -h, 0, 1, color);
            imageVertex(buffer, pose, h, -h, 1, 1, color);
            imageVertex(buffer, pose, h, h, 1, 0, color);
            imageVertex(buffer, pose, -h, h, 0, 0, color);
            matrices.popPose();
        }
        buffers.endBatch();
    }

    private static void imageVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int color) {
        buffer.addVertex(pose, x, y, 0).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
    }

    /** World text em in texture pixels: large enough to stay crisp up close; mipmaps keep it smooth far away. */
    private static final float WORLD_TEXT_EM = 64;

    /**
     * World text (lyrics) is drawn as high-resolution text images from {@link dev.jilaton.client.gui.design.TextRaster}:
     * Inter with a baked dark outline, mipmapped and linearly filtered, on one quad per line. The pixel font's 8px
     * glyphs shimmered and broke up at any distance; these stay smooth and readable.
     */
    private void drawTexts(PoseStack matrices, Frame frame) {
        for (FxFrame.WorldText text : frame.texts()) {
            // Colours are baked at full opacity so a fading line reuses one texture; the fade rides on the vertices.
            List<dev.jilaton.client.gui.design.TextRaster.Run> runs = new ArrayList<>();
            int alpha = 0;
            for (FxFrame.Run run : text.runs()) {
                alpha = Math.max(alpha, run.color() >>> 24);
                int rgb = 0xFF000000 | (run.color() & 0xFFFFFF);
                int last = runs.size() - 1;
                if (last >= 0 && runs.get(last).argb() == rgb)
                    runs.set(last, new dev.jilaton.client.gui.design.TextRaster.Run(runs.get(last).text() + run.text(), rgb));
                else runs.add(new dev.jilaton.client.gui.design.TextRaster.Run(run.text(), rgb));
            }
            if (runs.isEmpty() || alpha == 0) continue;
            boolean outlined = (text.glow() >>> 24) != 0;
            var image = dev.jilaton.client.gui.design.TextRaster.world(runs, dev.jilaton.client.gui.design.TextRaster.Face.SEMIBOLD,
                    WORLD_TEXT_EM, outlined ? WORLD_TEXT_EM * 0.085F : 0);
            // The vanilla font's line is 9 font pixels with the baseline at 7; keep that footprint per font pixel.
            float unit = text.scale() * 8.6F / WORLD_TEXT_EM;
            float width = image.width() * unit, height = image.height() * unit, advance = image.advance() * unit;
            float left = (text.align() < 0 ? 0 : text.align() > 0 ? -advance : -advance / 2F) - image.padLeft() * unit;
            float top = 7 * text.scale() - image.baseline() * unit;

            matrices.pushPose();
            matrices.translate(text.x(), text.y(), text.z());
            if (text.billboard()) matrices.mulPose(frame.rotation());
            else matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-text.yaw()));
            matrices.scale(1, -1, 1);
            PoseStack.Pose pose = matrices.last();
            if ((text.background() >>> 24) != 0) {
                VertexConsumer panel = buffers.getBuffer(text.seeThrough() ? RenderTypes.textSeeThrough(WHITE) : RenderTypes.text(WHITE));
                float pad = 3 * text.scale();
                textQuad(panel, pose, left + image.padLeft() * unit - pad, top + (image.baseline() * unit) - 8 * text.scale(),
                        advance + pad * 2, 10 * text.scale(), 0, 0, 1, 1, text.background(), -0.001F);
            }
            int color = alpha << 24 | 0xFFFFFF;
            VertexConsumer buffer = buffers.getBuffer(text.seeThrough()
                    ? RenderTypes.textSeeThrough(image.id()) : RenderTypes.text(image.id()));
            textQuad(buffer, pose, left, top, width, height, 0, 0, 1, 1, color, 0);
            matrices.popPose();
        }
        buffers.endBatch();
    }

    private static final net.minecraft.resources.Identifier WHITE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("jilaton", "textures/misc/white.png");

    private static void textQuad(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float w, float h,
                                 float u0, float v0, float u1, float v1, int color, float z) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(pose, x, y + h, z).setColor(color).setUv(u0, v1).setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(pose, x + w, y + h, z).setColor(color).setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(pose, x + w, y, z).setColor(color).setUv(u1, v0).setLight(LightTexture.FULL_BRIGHT);
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

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, EffectBatch.Quad q) {
        vertex(buffer, pose, q.ax(), q.ay(), q.az(), q.colorA());
        vertex(buffer, pose, q.bx(), q.by(), q.bz(), q.colorB());
        vertex(buffer, pose, q.cx(), q.cy(), q.cz(), q.colorC());
        vertex(buffer, pose, q.dx(), q.dy(), q.dz(), q.colorD());
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
    @Override public void close() { if (storage != null) { storage.close(); storage = null; buffers = null; } highlight.reset(); lastWorld = null; FxParticles.get().clear(); }
    private record Frame(List<Box> boxes, List<Line> lines, Halo halo, EffectBatch effects,
                         List<FxFrame.WorldText> texts, List<FxFrame.WorldImage> images,
                         List<FxFrame.SkyQuad> sky, Quaternionf rotation) { }
    private record Box(AABB bounds, int color, double opacity, boolean outline, float width, boolean walls) { }
    private record Line(double x1,double y1,double z1,double x2,double y2,double z2,int color,float width,boolean walls) { }
    private record Halo(double x,double y,double z,double height,double radius,double scan,double direction,double phase,double opacity,int color,float width,boolean ring) { }
}
