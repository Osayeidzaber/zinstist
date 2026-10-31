package dev.jilaton.client.render.model;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Body-space attachment frames and skeletal joint solver. Head parts follow the interpolated head yaw and pitch;
 * torso, back, shoulder, arm, and hand parts follow the interpolated body yaw, crouch bend, limb walk swing, and
 * attack swing. Local +X is the entity's right, +Y is up and +Z is the facing direction.
 */
public final class BodyAnchor {
    public enum Part {
        HEAD,
        TORSO,
        BACK,
        SHOULDERS,
        RIGHT_ARM,
        LEFT_ARM,
        RIGHT_HAND,
        LEFT_HAND,
        BOTH_HANDS
    }

    /** Full 3D world-space joint coordinates for a posed humanoid player/entity. */
    public record SkeletonJoints(
            double[] headTop,
            double[] headCenter,
            double[] neck,
            double[] spine,
            double[] pelvis,
            double[] rightShoulder,
            double[] leftShoulder,
            double[] rightElbow,
            double[] leftElbow,
            double[] rightWrist,
            double[] leftWrist,
            double[] rightHip,
            double[] leftHip,
            double[] rightKnee,
            double[] leftKnee,
            double[] rightAnkle,
            double[] leftAnkle
    ) { }

    private static final double HEAD_CENTRE = 0.36;
    private static final double TORSO_HEIGHT = 0.62;
    private static final double BACK_HEIGHT = 0.74;
    private static final double BACK_OFFSET = 0.17;
    private static final double SHOULDER_HEIGHT = 0.80;
    private static final double SHOULDER_OFFSET = 0.02;
    private static final double SHOULDER_HALF_SPAN = 0.3125;
    private static final double HIP_HALF_SPAN = 0.125;
    private static final double ARM_LENGTH = 0.62;
    private static final double LEG_LENGTH = 0.72;

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
            case SHOULDERS -> compose(Part.SHOULDERS, x, y + height * SHOULDER_HEIGHT, z, body, head, pitch);
            default -> composePose(part, x, y, z, height, body, head, pitch,
                    entity.isCrouching(),
                    entity.walkAnimation.position(partial),
                    Mth.clamp(entity.walkAnimation.speed(partial), 0.0F, 1.0F),
                    entity.getAttackAnim(partial));
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
            case RIGHT_ARM -> lx = SHOULDER_HALF_SPAN;
            case LEFT_ARM -> lx = -SHOULDER_HALF_SPAN;
            case RIGHT_HAND -> { lx = SHOULDER_HALF_SPAN; ly = -ARM_LENGTH; lz = 0.08; }
            case LEFT_HAND -> { lx = -SHOULDER_HALF_SPAN; ly = -ARM_LENGTH; lz = 0.08; }
            case BOTH_HANDS -> { ly = -ARM_LENGTH * 0.85; lz = 0.24; }
            default -> { }
        }
        return Transform.fromBasis(rx, ry, rz, ux, uy, uz, fx, fy, fz,
                ox + rx * lx + ux * ly + fx * lz,
                oy + ry * lx + uy * ly + fy * lz,
                oz + rz * lx + uz * ly + fz * lz);
    }

    /**
     * Full pose-aware transform composition for torso, arms, hands, and two-handed grips.
     */
    public static Transform composePose(Part part, double ox, double oy, double oz, double height,
                                        float bodyYaw, float headYaw, float headPitch,
                                        boolean crouching, float limbPhase, float limbSpeed, float attackSwing) {
        double crouchDrop = crouching ? -0.22 : 0.0;
        double torsoPitchDeg = crouching ? 28.0 : 0.0;
        Transform root = Transform.translation(ox, oy + crouchDrop, oz)
                .rotate(bodyYaw, 0, 0);
        return switch (part) {
            case HEAD -> compose(Part.HEAD, ox, oy + height - HEAD_CENTRE + crouchDrop, oz, bodyYaw, headYaw, headPitch);
            case TORSO -> root.translate(0, height * TORSO_HEIGHT, 0).rotate(0, torsoPitchDeg, 0);
            case BACK -> root.translate(0, height * BACK_HEIGHT, -BACK_OFFSET).rotate(0, torsoPitchDeg, 0);
            case SHOULDERS -> root.translate(0, height * SHOULDER_HEIGHT, -SHOULDER_OFFSET).rotate(0, torsoPitchDeg, 0);
            case RIGHT_ARM, RIGHT_HAND -> {
                double walkSwingDeg = Math.toDegrees(Math.cos(limbPhase * 0.6662 + Math.PI) * 1.4 * limbSpeed);
                double swingDeg = attackSwing > 0 ? -Math.sin(Math.sqrt(attackSwing) * Math.PI) * 58.0 : 0.0;
                Transform shoulder = root.translate(SHOULDER_HALF_SPAN, height * SHOULDER_HEIGHT, 0)
                        .rotate(0, torsoPitchDeg + walkSwingDeg + swingDeg, 0);
                yield part == Part.RIGHT_ARM ? shoulder : shoulder.translate(0, -ARM_LENGTH, 0.06);
            }
            case LEFT_ARM, LEFT_HAND -> {
                double walkSwingDeg = Math.toDegrees(Math.cos(limbPhase * 0.6662) * 1.4 * limbSpeed);
                Transform shoulder = root.translate(-SHOULDER_HALF_SPAN, height * SHOULDER_HEIGHT, 0)
                        .rotate(0, torsoPitchDeg + walkSwingDeg, 0);
                yield part == Part.LEFT_ARM ? shoulder : shoulder.translate(0, -ARM_LENGTH, 0.06);
            }
            case BOTH_HANDS -> {
                double swingDeg = attackSwing > 0 ? -Math.sin(Math.sqrt(attackSwing) * Math.PI) * 45.0 : 0.0;
                yield root.translate(0, height * SHOULDER_HEIGHT - ARM_LENGTH * 0.72, 0.28)
                        .rotate(0, torsoPitchDeg - 18.0 + swingDeg, 0);
            }
        };
    }

    public static SkeletonJoints skeleton(LivingEntity entity, float partial) {
        Vec3 pos = entity.getPosition(partial);
        return skeleton(
                pos.x, pos.y, pos.z,
                entity.getBbHeight(),
                bodyYaw(entity, partial),
                headYaw(entity, partial),
                headPitch(entity, partial),
                entity.isCrouching(),
                entity.walkAnimation.position(partial),
                Mth.clamp(entity.walkAnimation.speed(partial), 0.0F, 1.0F),
                entity.getAttackAnim(partial));
    }

    /**
     * Pure skeletal joint solver used by both runtime overlays and headless unit tests.
     */
    public static SkeletonJoints skeleton(double ox, double oy, double oz, double height,
                                          float bodyYaw, float headYaw, float headPitch,
                                          boolean crouching, float limbPhase, float limbSpeed, float attackSwing) {
        double h = Math.max(0.8, height);
        double crouchDrop = crouching ? -0.20 : 0.0;
        double crouchForward = crouching ? 0.14 : 0.0;
        Transform body = Transform.translation(ox, oy, oz).rotate(bodyYaw, 0, 0);

        double pelvisY = h * 0.42 + crouchDrop * 0.4;
        double neckY = h * 0.78 + crouchDrop;
        double spineY = (pelvisY + neckY) * 0.5;

        double[] pelvis = body.point(0, pelvisY, -crouchForward * 0.4);
        double[] spine = body.point(0, spineY, crouchForward * 0.4);
        double[] neck = body.point(0, neckY, crouchForward);

        Transform headFrame = compose(Part.HEAD, neck[0], neck[1] + h * 0.11, neck[2], bodyYaw, headYaw, headPitch);
        double[] headCenter = headFrame.point(0, 0, 0);
        double[] headTop = headFrame.point(0, h * 0.12, 0);

        double[] rightShoulder = body.point(SHOULDER_HALF_SPAN, neckY - 0.03, crouchForward);
        double[] leftShoulder = body.point(-SHOULDER_HALF_SPAN, neckY - 0.03, crouchForward);

        double rArmAngle = Math.cos(limbPhase * 0.6662 + Math.PI) * 1.1 * limbSpeed
                - (attackSwing > 0 ? Math.sin(Math.sqrt(attackSwing) * Math.PI) * 1.1 : 0.0)
                + (crouching ? 0.35 : 0.0);
        double lArmAngle = Math.cos(limbPhase * 0.6662) * 1.1 * limbSpeed + (crouching ? 0.35 : 0.0);

        double armHalf = ARM_LENGTH * 0.5;
        double[] rightElbow = body.point(
                SHOULDER_HALF_SPAN + 0.02,
                neckY - 0.03 - Math.cos(rArmAngle) * armHalf,
                crouchForward - Math.sin(rArmAngle) * armHalf);
        double[] rightWrist = body.point(
                SHOULDER_HALF_SPAN + 0.03,
                neckY - 0.03 - Math.cos(rArmAngle) * ARM_LENGTH,
                crouchForward - Math.sin(rArmAngle) * ARM_LENGTH + 0.04);

        double[] leftElbow = body.point(
                -SHOULDER_HALF_SPAN - 0.02,
                neckY - 0.03 - Math.cos(lArmAngle) * armHalf,
                crouchForward - Math.sin(lArmAngle) * armHalf);
        double[] leftWrist = body.point(
                -SHOULDER_HALF_SPAN - 0.03,
                neckY - 0.03 - Math.cos(lArmAngle) * ARM_LENGTH,
                crouchForward - Math.sin(lArmAngle) * ARM_LENGTH + 0.04);

        double[] rightHip = body.point(HIP_HALF_SPAN, pelvisY, -crouchForward * 0.4);
        double[] leftHip = body.point(-HIP_HALF_SPAN, pelvisY, -crouchForward * 0.4);

        double rLegAngle = Math.cos(limbPhase * 0.6662) * 1.1 * limbSpeed;
        double lLegAngle = Math.cos(limbPhase * 0.6662 + Math.PI) * 1.1 * limbSpeed;
        double legLen = Math.min(LEG_LENGTH, pelvisY);
        double legHalf = legLen * 0.5;

        double[] rightKnee = body.point(
                HIP_HALF_SPAN,
                pelvisY - Math.cos(rLegAngle) * legHalf,
                -Math.sin(rLegAngle) * legHalf);
        double[] rightAnkle = body.point(
                HIP_HALF_SPAN,
                Math.max(0.02, pelvisY - Math.cos(rLegAngle) * legLen),
                -Math.sin(rLegAngle) * legLen);

        double[] leftKnee = body.point(
                -HIP_HALF_SPAN,
                pelvisY - Math.cos(lLegAngle) * legHalf,
                -Math.sin(lLegAngle) * legHalf);
        double[] leftAnkle = body.point(
                -HIP_HALF_SPAN,
                Math.max(0.02, pelvisY - Math.cos(lLegAngle) * legLen),
                -Math.sin(lLegAngle) * legLen);

        return new SkeletonJoints(
                headTop, headCenter, neck, spine, pelvis,
                rightShoulder, leftShoulder,
                rightElbow, leftElbow,
                rightWrist, leftWrist,
                rightHip, leftHip,
                rightKnee, leftKnee,
                rightAnkle, leftAnkle);
    }
}
