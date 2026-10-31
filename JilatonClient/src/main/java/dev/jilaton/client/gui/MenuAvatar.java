package dev.jilaton.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.function.Supplier;

/**
 * The local player's skin on the title screen. It walks toward the viewer out of the dark, settles its
 * weight, waves, then idles with breathing and cursor tracking. The step cycle is tied to distance
 * travelled so feet never slide. Rendering uses vanilla's GUI skin renderer; only poses are driven here.
 */
final class MenuAvatar {
    /** Model units from feet to the top of the head, matching vanilla's skin widget framing. */
    private static final float MODEL_HEIGHT = 2.125F;
    private static final double DELAY = 0.25;
    private static final double WALK_SECONDS = 2.4;
    private static final double STEPS = 2.6;
    private static final double FAR_SCALE = 0.5;
    private static final double WAVE_SECONDS = 1.6;
    private static final double REST_YAW = 22;
    private static final int RIGHT_LEG = 0, LEFT_LEG = 1, RIGHT_ARM_X = 2, LEFT_ARM_X = 3, RIGHT_ARM_Z = 4,
            LEFT_ARM_Z = 5, BODY_X = 6, HEAD_X = 7, HEAD_Y = 8;
    private final PlayerModel wide;
    private final PlayerModel slim;
    private final Supplier<PlayerSkin> skin;
    private final String name;
    private final float[] limbs = new float[9];
    private final AnimatedValue spin = new AnimatedValue();
    private double dragYaw;
    private boolean dragging;
    private boolean dragged;
    private long lastFrame;
    private long waveStart;
    private int boxX, boxY, boxW, boxH;

    MenuAvatar(Minecraft minecraft) {
        wide = new PlayerModel(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        slim = new PlayerModel(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        skin = minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), false);
        name = minecraft.getUser().getName();
    }

    String name() { return name; }

    /** Draws the avatar standing on {@code floorY}, centered on {@code centerX}, {@code height} pixels tall at rest. */
    void render(GuiGraphics graphics, int centerX, int floorY, int height, int clipLeft, int clipRight,
                long openedAt, long now, int mouseX, int mouseY, boolean reduced, double intensity) {
        double dt = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
        double t = (now - openedAt) / 1_000_000_000.0;
        double walkT = reduced ? 1 : Motion.clamp01((t - DELAY) / WALK_SECONDS);
        // Smootherstep: zero velocity at both ends, so the walk starts and stops without a jolt.
        double s = walkT * walkT * walkT * (walkT * (walkT * 6 - 15) + 10);
        double speed = 30 * walkT * walkT * (walkT - 1) * (walkT - 1) / 1.875;
        boolean arrived = walkT >= 1;
        double arrivedFor = reduced ? 10 : t - DELAY - WALK_SECONDS;
        if (arrived && waveStart == 0 && arrivedFor > 0.35) waveStart = now;

        double depth = 1 - s;
        double scaleFactor = reduced ? 1 : 1 - (1 - FAR_SCALE) * depth * Math.min(1, intensity <= 0 ? 0 : 1);
        int drawnHeight = (int) Math.round(height * scaleFactor);
        int feetY = (int) Math.round(floorY - depth * height * 0.24);
        int width = Math.round(drawnHeight * 0.55F);
        boxW = width + 40;
        boxH = drawnHeight + 10;
        boxX = centerX - boxW / 2;
        boxY = feetY - drawnHeight - 6;

        // Contact shadow that grows and darkens as the avatar comes closer.
        for (int i = 4; i >= 1; i--) {
            int half = (int) Math.round(width * 0.17 * i);
            int alpha = (int) Math.round((14.0 / i + 4) * (0.35 + 0.65 * s));
            graphics.fill(centerX - half, feetY - 1, centerX + half, feetY + 1, alpha << 24);
        }
        double settle = arrived ? Motion.SETTLE.apply(Math.min(1, arrivedFor / 0.6)) : 0;
        int lineHalf = (int) Math.round(height * 0.55 * (0.35 + 0.65 * settle));
        graphics.fill(centerX - lineHalf, floorY + 2, centerX + lineHalf, floorY + 3, JilatonUi.alpha(JilatonUi.HAIRLINE_STRONG, 0.3 + 0.7 * settle));

        double time = reduced ? 0 : t;
        pose(time, s, arrived ? 0 : Math.min(1, speed * 1.4), settle, now, mouseX, mouseY, reduced, dt);
        PlayerSkin current = skin.get();
        PlayerModel model = current.model() == PlayerModelType.SLIM ? slim : wide;
        apply(model);

        if (!dragging) dragYaw *= reduced ? 0 : Math.exp(-4.0 * dt);
        double sway = reduced ? 0 : Math.sin(time * 0.35) * 2.5 * settle;
        double yaw = 14 + (REST_YAW - 14) * settle + sway + spin.update(dragYaw, now, reduced);
        double phase = s * STEPS * Math.PI * 2;
        double bob = arrived ? 0 : Math.abs(Math.cos(phase)) * Math.min(1, speed * 1.4) * drawnHeight * 0.014;
        float scale = drawnHeight / MODEL_HEIGHT;
        int top = (int) Math.round(boxY - bob);
        graphics.enableScissor(clipLeft, 0, clipRight, floorY + 4);
        graphics.submitSkinRenderState(model, current.body().texturePath(), scale, -4.0F, (float) yaw, -1.0625F,
                boxX, top, boxX + boxW, top + boxH);
        graphics.disableScissor();
    }

    private void pose(double time, double distance, double stride, double settle, long now, int mouseX, int mouseY,
                      boolean reduced, double dt) {
        float[] target = new float[limbs.length];
        double phase = distance * STEPS * Math.PI * 2;
        double swing = Math.sin(phase) * stride;
        target[RIGHT_LEG] = (float) (swing * 0.78);
        target[LEFT_LEG] = (float) (-swing * 0.78);
        target[RIGHT_ARM_X] = (float) (-swing * 0.62);
        target[LEFT_ARM_X] = (float) (swing * 0.62);
        double breathe = reduced ? 0 : Math.sin(time * 1.7) * 0.035 * settle;
        target[RIGHT_ARM_Z] = (float) (0.06 + breathe);
        target[LEFT_ARM_Z] = (float) (-0.06 - breathe);
        target[BODY_X] = (float) (0.05 * stride);

        double wave = waveEnvelope(now);
        if (wave > 0) {
            double flutter = reduced ? 0 : Math.sin((now - waveStart) / 1_000_000_000.0 * 11) * 0.32;
            target[RIGHT_ARM_Z] = (float) (target[RIGHT_ARM_Z] + (2.55 + flutter - target[RIGHT_ARM_Z]) * wave);
            target[RIGHT_ARM_X] = (float) (target[RIGHT_ARM_X] * (1 - wave) - 0.15 * wave);
        }

        // Model-space yaw is mirrored relative to the screen: a cursor to the right needs a negative yRot.
        double centerX = boxX + boxW / 2.0;
        double centerY = boxY + boxH * 0.16;
        double look = Math.max(0.25, settle);
        target[HEAD_Y] = (float) (-Math.max(-0.75, Math.min(0.75, (mouseX - centerX) / 200.0)) * look);
        target[HEAD_X] = (float) (Math.max(-0.45, Math.min(0.5, (mouseY - centerY) / 240.0)) * look);

        double k = reduced || dt == 0 ? 1 : 1 - Math.exp(-16 * dt);
        double headK = reduced || dt == 0 ? 1 : 1 - Math.exp(-9 * dt);
        for (int i = 0; i < limbs.length; i++) {
            double rate = i == HEAD_X || i == HEAD_Y ? headK : k;
            limbs[i] += (float) ((target[i] - limbs[i]) * rate);
        }
    }

    private double waveEnvelope(long now) {
        if (waveStart == 0) return 0;
        double w = (now - waveStart) / 1_000_000_000.0;
        if (w >= WAVE_SECONDS) return 0;
        double in = Motion.SETTLE.apply(w / 0.3);
        double out = Motion.STANDARD.apply((WAVE_SECONDS - w) / 0.35);
        return Math.min(in, out);
    }

    private void apply(PlayerModel model) {
        model.resetPose();
        model.rightLeg.xRot = limbs[RIGHT_LEG];
        model.leftLeg.xRot = limbs[LEFT_LEG];
        model.rightArm.xRot = limbs[RIGHT_ARM_X];
        model.leftArm.xRot = limbs[LEFT_ARM_X];
        model.rightArm.zRot = limbs[RIGHT_ARM_Z];
        model.leftArm.zRot = limbs[LEFT_ARM_Z];
        model.body.xRot = limbs[BODY_X];
        model.head.xRot = limbs[HEAD_X];
        model.head.yRot = limbs[HEAD_Y];
    }

    boolean mouseClicked(double mouseX, double mouseY) {
        dragging = mouseX >= boxX && mouseX < boxX + boxW && mouseY >= boxY && mouseY < boxY + boxH;
        dragged = false;
        return dragging;
    }

    boolean mouseDragged(double dragX) {
        if (!dragging) return false;
        if (Math.abs(dragX) > 0.5) dragged = true;
        dragYaw = Math.max(-180, Math.min(180, dragYaw + dragX * 2.5));
        return true;
    }

    void mouseReleased() {
        if (dragging && !dragged && waveEnvelope(System.nanoTime()) == 0) waveStart = System.nanoTime();
        dragging = false;
    }
}
