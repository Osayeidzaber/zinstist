package dev.jilaton.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.function.Function;

/**
 * Submits code-built {@link Mesh} parts through the entity pipeline, so a model drawn in the first-person hand pass
 * gets the same lighting, depth and hand-pass projection as the vanilla arm holding it. Geometry is untextured:
 * every vertex samples a white texel and carries its material colour.
 */
public final class HeldModelRenderer {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("jilaton", "textures/misc/white.png");

    private HeldModelRenderer() { }

    /** Draws {@code parts} at the pose's current transform, scaled by {@code scale} blocks per mesh unit. */
    public static void submit(PoseStack pose, SubmitNodeCollector collector, int light, Map<String, Mesh> parts,
                              Function<String, Material> materials, double scale, double alpha) {
        RenderType type = RenderTypes.entityTranslucent(WHITE);
        float s = (float) scale;
        for (Map.Entry<String, Mesh> part : parts.entrySet()) {
            Mesh mesh = part.getValue();
            Material material = materials.apply(part.getKey());
            float[] positions = mesh.positions(), normals = mesh.normals(), tones = mesh.tones();
            int[] indices = mesh.indices();
            collector.submitCustomGeometry(pose, type, (last, consumer) -> {
                for (int index : indices) {
                    int p = index * 3;
                    int color = material.color(tones[index], 1, alpha);
                    consumer.addVertex(last, positions[p] * s, positions[p + 1] * s, positions[p + 2] * s)
                            .setColor(color).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                            .setNormal(last, normals[p], normals[p + 1], normals[p + 2]);
                }
            });
        }
    }
}
