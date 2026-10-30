package dev.jilaton.client.render.model;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Body-space attachment frames. Head parts follow the interpolated head yaw and pitch; back and shoulder
 * parts follow the interpolated body yaw only, so a wing stays planted while the head turns. Local +X is
 * the entity's right, +Y is up and +Z is the facing direction.
 */
public final class BodyAnchor {
    public enum Part { HEAD, BACK, SHOULDERS }

    private static final double HEAD_CENTRE = 0.36;
    private static final double BACK_HEIGHT = 0.74;
    private static final double BACK_OFFSET = 0.17;
    private static final double SHOULDER_HEIGHT = 0.80;
    private static final double SHOULDER_OFFSET = 0.02;

    private BodyAnchor() { }

    public static float bodyYaw(LivingEntity entity, float partial) {
        return Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
    }

    public static float headYaw(LivingEntity entity, float partial) {
        return Mth.rotLerp(partial, entity.yHeadRotO, entity.yHeadRot);
    }

    public static float headPitch(LivingEntity entity, float partial) {
        return Mth.lerp(partial, entity.xRotO, entity.getXRot());
    }

    public static Transform forPart(LivingEntity entity, Part part, float partial) {
        Vec3 position = entity.getPosition(partial);
        float body = bodyYaw(entity, partial);
        float head = headYaw(entity, partial);
        float pitch = headPitch(entity, partial);
        double height = entity.getBbHeight();
        double x = position.x, y = position.y, z = position.z;
        return switch (part) {
            case HEAD -> compose(Part.HEAD, x, y + height - HEAD_CENTRE, z, body, head, pitch);
            case BACK -> compose(Part.BACK, x, y + height * BACK_HEIGHT, z, body, head, pitch);
            case SHOULDERS -> compose(Part.SHOULDERS, x, y + height * SHOULDER_HEIGHT, z, body, head, pitch);
        };
    }

    /**
     * Pure composition used by both the runtime path and the headless tests. Back and shoulder frames
     * ignore the head angles entirely; only the head frame consumes them.
     */
    public static Transform compose(Part part, double ox, double oy, double oz,
                                    float bodyYaw, float headYaw, float headPitch) {
        double yaw = Math.toRadians(part == Part.HEAD ? headYaw : bodyYaw);
        double pitch = part == Part.HEAD ? Math.toRadians(headPitch) : 0;
        double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);
        double rx = -cy, ry = 0, rz = -sy;
        double fx = -sy * cp, fy = -sp, fz = cy * cp;
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;
        double lx = 0, ly = 0, lz = 0;
        switch (part) {
            case BACK -> lz = -BACK_OFFSET;
            case SHOULDERS -> lz = -SHOULDER_OFFSET;
            default -> { }
        }
        return Transform.fromBasis(rx, ry, rz, ux, uy, uz, fx, fy, fz,
                ox + rx * lx + ux * ly + fx * lz,
                oy + ry * lx + uy * ly + fy * lz,
                oz + rz * lx + uz * ly + fz * lz);
    }
}
