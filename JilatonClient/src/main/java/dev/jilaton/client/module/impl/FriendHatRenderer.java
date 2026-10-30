package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class FriendHatRenderer {
    private FriendHatRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(FriendHatRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        ClientModule friends = JilatonClient.moduleManager().get("friends");
        if (client.level == null || client.player == null
                || !(friends instanceof FriendsModule) || !friends.enabled()
                || context.consumers() == null || context.matrices() == null) {
            return;
        }

        Vec3 camera = client.gameRenderer.getMainCamera().position();
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        PoseStack matrices = context.matrices();
        VertexConsumer buffer = context.consumers().getBuffer(RenderTypes.debugQuads());

        for (Player player : client.level.players()) {
            if (player == client.player || !FriendManager.contains(player) || !player.isAlive()) {
                continue;
            }

            double x = player.xo + (player.getX() - player.xo) * partialTick - camera.x;
            double y = player.yo + (player.getY() - player.yo) * partialTick - camera.y;
            double z = player.zo + (player.getZ() - player.zo) * partialTick - camera.z;

            matrices.pushPose();
            matrices.translate(x, y + player.getBbHeight() + 0.04, z);
            Matrix4f pose = matrices.last().pose();
            drawBrim(buffer, pose);
            drawCrown(buffer, pose);
            matrices.popPose();
        }
    }

    private static void drawBrim(VertexConsumer buffer, Matrix4f pose) {
        for (int point = 0; point < 16; point++) {
            double angle = -Math.PI * 2.0 * point / 16.0;
            double nextAngle = -Math.PI * 2.0 * (point + 1) / 16.0;
            addTriangle(buffer, pose, 0.0F, 0.055F, 0.0F,
                    (float) Math.cos(angle) * 0.19F, 0.055F, (float) Math.sin(angle) * 0.19F,
                    (float) Math.cos(nextAngle) * 0.19F, 0.055F, (float) Math.sin(nextAngle) * 0.19F);
        }
    }

    private static void drawCrown(VertexConsumer buffer, Matrix4f pose) {
        for (int point = 0; point < 12; point++) {
            double angle = -Math.PI * 2.0 * point / 12.0;
            double nextAngle = -Math.PI * 2.0 * (point + 1) / 12.0;
            addTriangle(buffer, pose, 0.0F, 0.29F, 0.0F,
                    (float) Math.cos(angle) * 0.12F, 0.075F, (float) Math.sin(angle) * 0.12F,
                    (float) Math.cos(nextAngle) * 0.12F, 0.075F, (float) Math.sin(nextAngle) * 0.12F);
        }
    }

    private static void addTriangle(VertexConsumer buffer, Matrix4f pose,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3) {
        buffer.addVertex(pose, x1, y1, z1).setColor(75, 141, 255, 255);
        buffer.addVertex(pose, x2, y2, z2).setColor(75, 141, 255, 255);
        buffer.addVertex(pose, x3, y3, z3).setColor(75, 141, 255, 255);
        buffer.addVertex(pose, x3, y3, z3).setColor(75, 141, 255, 255);
    }

}
