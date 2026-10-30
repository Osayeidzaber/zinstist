/* Independent Java adaptation of LiquidBounce ModuleAnimations hand transforms.
 * Copyright (c) 2015-2026 CCBlueX. GPL-3.0-or-later; see ATTRIBUTION.md. */
package dev.jilaton.client.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.world.InteractionHand;

public final class ViewModelModule extends ClientModule {
    private final HandOptions main = new HandOptions("main", "Main hand");
    private final HandOptions off = new HandOptions("off", "Off hand");

    public ViewModelModule() {
        super("view_model", "ViewModel", "Independent first-person hand offsets, rotation and scale; preserves vanilla item use.", ModuleCategory.RENDER);
    }

    public void apply(PoseStack pose, InteractionHand hand, boolean twoHandedMap) {
        HandOptions options = hand == InteractionHand.MAIN_HAND ? main : off;
        if (twoHandedMap) {
            // A shared map is rendered once for both hands. Average the enabled transforms.
            if (!main.enabled.value() && !off.enabled.value()) return;
            float divisor = main.enabled.value() && off.enabled.value() ? 2 : 1;
            pose.translate((main.x() + off.x()) / divisor, (main.y() + off.y()) / divisor, (main.z() + off.z()) / divisor);
            rotate(pose, (main.rx() + off.rx()) / divisor, (main.ry() + off.ry()) / divisor, (main.rz() + off.rz()) / divisor);
            float scale = ((main.enabled.value() ? main.scale() : 0) + (off.enabled.value() ? off.scale() : 0)) / divisor;
            pose.scale(scale, scale, scale);
        } else if (options.enabled.value()) {
            pose.translate(options.x(), options.y(), options.z());
            rotate(pose, options.rx(), options.ry(), options.rz());
            pose.scale(options.scale(), options.scale(), options.scale());
        }
    }

    private static void rotate(PoseStack pose, float x, float y, float z) {
        pose.mulPose(Axis.XP.rotationDegrees(x));
        pose.mulPose(Axis.YP.rotationDegrees(y));
        pose.mulPose(Axis.ZP.rotationDegrees(z));
    }

    private final class HandOptions {
        final BooleanSetting enabled;
        final NumberSetting x, y, z, rx, ry, rz, scale;

        HandOptions(String id, String label) {
            enabled = addSetting(new BooleanSetting(id + "_enabled", label + " transform", true));
            x = number(id + "_x", label + " X", -2, 2, 0.05, 0);
            y = number(id + "_y", label + " Y", -2, 2, 0.05, 0);
            z = number(id + "_z", label + " Z", -2, 2, 0.05, 0);
            rx = number(id + "_rx", label + " rotation X", -180, 180, 5, 0);
            ry = number(id + "_ry", label + " rotation Y", -180, 180, 5, 0);
            rz = number(id + "_rz", label + " rotation Z", -180, 180, 5, 0);
            scale = number(id + "_scale", label + " scale", 0.25, 2, 0.05, 1);
        }

        float x() { return enabled.value() ? (float) x.value() : 0; }
        float y() { return enabled.value() ? (float) y.value() : 0; }
        float z() { return enabled.value() ? (float) z.value() : 0; }
        float rx() { return enabled.value() ? (float) rx.value() : 0; }
        float ry() { return enabled.value() ? (float) ry.value() : 0; }
        float rz() { return enabled.value() ? (float) rz.value() : 0; }
        float scale() { return (float) scale.value(); }
    }

    private NumberSetting number(String id, String label, double min, double max, double step, double value) {
        return addSetting(new NumberSetting(id, label, min, max, step, value));
    }
}
