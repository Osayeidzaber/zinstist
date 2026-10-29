/*
 * Adapted from LiquidBounce's SwingAnimations.kt
 * Copyright (c) 2015-2026 CCBlueX
 * Source: https://github.com/CCBlueX/LiquidBounce/tree/e033f5aaf05be802dd2ca51782f255119d7d5654
 * Licensed under the GNU General Public License v3.0 or later.
 */
package dev.jilaton.client.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

import java.util.List;

/**
 * First-person swing styles adapted from LiquidBounce's GPL-3.0 SwingAnimations.
 * The original client-specific event/value framework has been replaced by Jilaton settings.
 */
public final class SwingAnimationsModule extends ClientModule {
    private static final float PI = (float) Math.PI;
    private final ChoiceSetting mode = addSetting(new ChoiceSetting(
            "mode", "Swing style",
            List.of("Swipe", "Spin", "Hook", "Dash", "Tap", "Inject", "Slap", "Akrien", "Smooth", "Power", "Feast"),
            "Smooth"));
    private final NumberSetting scale = addSetting(new NumberSetting(
            "scale", "Swing scale", 0.1, 1.0, 0.05, 0.9));

    public SwingAnimationsModule() {
        super("swing_animations", "Swing Animations", "Customize the first-person item swing", ModuleCategory.RENDER);
    }

    public void apply(PoseStack poseStack, HumanoidArm arm, float progress) {
        float swing = Mth.clamp(progress * (float) scale.value(), 0.0f, 1.0f);
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float root = Mth.sqrt(swing);
        float arc = Mth.sin(root * PI);
        float sinSquared = Mth.sin(swing * swing * PI);
        float sinRoot = Mth.sin(root * PI);
        float sinSmooth = Mth.sin(swing * PI) * 0.5f;

        switch (mode.value()) {
            case "Swipe" -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(side * (45.0f - swing * 20.0f)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * arc * -70.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(-70.0f));
                poseStack.mulPose(Axis.YP.rotationDegrees(side * -45.0f));
            }
            case "Spin" -> poseStack.mulPose(Axis.XP.rotationDegrees(swing * -360.0f));
            case "Hook" -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(50.0f));
                poseStack.mulPose(Axis.YP.rotationDegrees(side * (-30.0f * (1.0f - arc) - 30.0f)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * 110.0f));
            }
            case "Dash" -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(50.0f));
                poseStack.mulPose(Axis.YP.rotationDegrees(side * (-60.0f * arc - 50.0f)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * 110.0f));
            }
            case "Tap" -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(50.0f));
                poseStack.mulPose(Axis.YP.rotationDegrees(side * -60.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * (110.0f + 20.0f * arc)));
            }
            case "Inject" -> {
                poseStack.translate(0.0, 0.0, -arc / 4.0);
                poseStack.mulPose(Axis.XP.rotationDegrees(-120.0f));
            }
            case "Slap" -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.sin(swing * 3.0f) * 60.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * -60.0f * arc));
            }
            case "Akrien" -> {
                if (swing > 0.0f) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0f));
                    poseStack.mulPose(Axis.XP.rotationDegrees(arc * -85.0f));
                    poseStack.translate(side * -0.1, 0.28, 0.2);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-85.0f));
                } else {
                    float y = 0.2f * Mth.sin(root * PI * 2.0f);
                    float z = -0.2f * Mth.sin(swing * PI);
                    poseStack.translate(side * -0.4f * arc, y, z);
                    applySwingOffset(poseStack, arm, swing);
                }
            }
            case "Smooth" -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(side * (45.0f - sinSquared * 20.0f)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * sinRoot * -20.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(sinRoot * -80.0f));
                poseStack.mulPose(Axis.YP.rotationDegrees(side * -45.0f));
                poseStack.translate(0.0, -0.1, 0.0);
            }
            case "Power" -> {
                poseStack.translate(-sinSmooth * sinSmooth * sinSquared * side, 0.0, 0.0);
                poseStack.mulPose(Axis.YP.rotationDegrees(side * 61.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sinRoot));
                poseStack.mulPose(Axis.YP.rotationDegrees(sinRoot * sinSquared * -5.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(sinRoot * sinSquared * -30.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(-60.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(sinSmooth * -60.0f));
            }
            case "Feast" -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(side * 30.0f));
                poseStack.mulPose(Axis.YP.rotationDegrees(sinRoot * 75.0f * side));
                poseStack.mulPose(Axis.XP.rotationDegrees(sinRoot * -45.0f));
                poseStack.mulPose(Axis.YP.rotationDegrees(side * 30.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(-80.0f));
                poseStack.mulPose(Axis.YP.rotationDegrees(side * 35.0f));
            }
            default -> applySwingOffset(poseStack, arm, swing);
        }
    }

    private static void applySwingOffset(PoseStack poseStack, HumanoidArm arm, float swing) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float arc = Mth.sin(swing * swing * PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * (45.0f - arc * 20.0f)));
        float root = Mth.sin(Mth.sqrt(swing) * PI);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * root * -20.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(root * -80.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -45.0f));
    }
}
