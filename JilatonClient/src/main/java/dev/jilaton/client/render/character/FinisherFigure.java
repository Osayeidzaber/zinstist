package dev.jilaton.client.render.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jilaton.client.render.character.kit.CharacterModel;
import dev.jilaton.client.render.character.kit.Detail;
import dev.jilaton.client.render.character.kit.PartMesh;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Draws one of the built-in 3D characters at an arbitrary world spot with an explicit pose, outside any entity.
 * Used by the Finisher: the same baked meshes, atlas and lighting the worn character uses, posed bone by bone
 * from vanilla-style angles instead of a {@code PlayerModel}.
 */
@Environment(EnvType.CLIENT)
public final class FinisherFigure {
    private static final Quaternionf ROTATION = new Quaternionf();

    private FinisherFigure() {
    }

    public static List<String> characters() {
        return CharacterCatalog.names();
    }

    /** Starts baking a character ahead of time so the first finisher doesn't wait on it. */
    public static void prepare(String name) {
        CharacterCache.listen();
        CharacterCache.prepare(CharacterCatalog.byName(name), Detail.MEDIUM);
    }

    /**
     * Renders the character with its feet at ({@code x}, {@code y}, {@code z}) relative to the camera, facing
     * {@code yaw} degrees (Minecraft yaw). {@code angles} holds x/y/z rotations for head, body, right arm, left
     * arm, right leg and left leg. Returns false while the model is still baking.
     */
    public static boolean draw(PoseStack stack, MultiBufferSource buffers, String name, double x, double y, double z,
                               float yaw, float[] angles, float scale, float alpha, int light) {
        WornCharacter worn = CharacterCache.ready(CharacterCatalog.byName(name), Detail.MEDIUM);
        if (worn == null || alpha <= 0.01F) return false;
        CharacterModel model = worn.model;
        int a = Math.round(Math.min(1F, alpha) * 255F);
        boolean ghost = a < 250;
        stack.pushPose();
        stack.translate(x, y, z);
        stack.mulPose(Axis.YP.rotationDegrees(180F - yaw));
        stack.scale(scale, scale, scale);
        // Same flip and offset LivingEntityRenderer applies, so the model space matches a worn character.
        stack.scale(-1F, -1F, 1F);
        stack.translate(0F, -1.501F, 0F);
        // One pass per render type: asking the buffer source for another type ends the previous buffer.
        if (ghost) {
            pass(stack, model, angles, buffers.getBuffer(worn.ghost), light, a, 0);
        } else {
            pass(stack, model, angles, buffers.getBuffer(worn.solid), light, 255, 1);
            pass(stack, model, angles, buffers.getBuffer(worn.decal), light, 255, 2);
        }
        stack.popPose();
        return true;
    }

    /** layer 0: faded base only; 1: base and outline; 2: decals. */
    private static void pass(PoseStack stack, CharacterModel model, float[] angles,
                             com.mojang.blaze3d.vertex.VertexConsumer out, int light, int alpha, int layer) {
        PartMesh[] parts = model.parts;
        int overlay = OverlayTexture.NO_OVERLAY;
        for (int i = 0; i < parts.length; i++) {
            PartMesh part = parts[i];
            boolean any = layer == 2 ? part.decals != null : layer == 1 ? part.base != null || part.outline != null : part.base != null;
            if (!any) continue;
            stack.pushPose();
            chain(stack, model, i, angles);
            PoseStack.Pose pose = stack.last();
            if (layer == 2) {
                CharacterRenderer.emit(pose, out, part.decals, light, overlay, alpha, false, 0, 0, 0, part, 0, 0);
            } else {
                if (part.base != null) CharacterRenderer.emit(pose, out, part.base, light, overlay, alpha, false, 0, 0, 0, part, 0, 0);
                if (layer == 1 && part.outline != null) CharacterRenderer.emit(pose, out, part.outline, light, overlay, alpha, false, 0, 0, 0, part, 0, 0);
            }
            stack.popPose();
        }
    }

    private static void chain(PoseStack stack, CharacterModel model, int index, float[] angles) {
        PartMesh part = model.parts[index];
        float s = model.scale;
        if (!part.extra) {
            int o = part.bone.ordinal() * 3;
            stack.translate(s * part.pivotX / 16F, (24F - s * part.pivotY) / 16F, -s * part.pivotZ / 16F);
            float xr = angles[o], yr = angles[o + 1], zr = angles[o + 2];
            if (xr != 0F || yr != 0F || zr != 0F) stack.mulPose(ROTATION.rotationZYX(zr, yr, xr));
            float k = s / 16F;
            stack.scale(k, -k, -k);
        } else {
            chain(stack, model, part.parent, angles);
            PartMesh parent = model.parts[part.parent];
            stack.translate(part.pivotX - parent.pivotX, part.pivotY - parent.pivotY, part.pivotZ - parent.pivotZ);
        }
    }
}
