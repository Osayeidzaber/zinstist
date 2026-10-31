/* Independent Java adaptation of LiquidBounce ModuleAnimations hand transforms.
 * Copyright (c) 2015-2026 CCBlueX. GPL-3.0-or-later; see ATTRIBUTION.md. */
package dev.jilaton.client.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.model.ViewModelPreset;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;

/**
 * First-person hand offsets, rotation and scale, with named presets. A preset is a real snapshot of both hands'
 * transform values: selecting it writes those values into the settings, and saving a preset captures what the
 * settings hold right now. The selection and the user's saved presets are ordinary settings, so they persist through
 * {@code ClientConfig} without extra schema.
 */
public final class ViewModelModule extends ClientModule {
    /** Built-in presets. User presets with the same name replace them. */
    public static final List<ViewModelPreset> SHIPPED = List.of(
            new ViewModelPreset("Default", ViewModelPreset.Hand.NONE, ViewModelPreset.Hand.NONE),
            new ViewModelPreset("Low",
                    new ViewModelPreset.Hand(true, 0, -0.30, 0, 0, 0, 0, 1),
                    ViewModelPreset.Hand.NONE),
            new ViewModelPreset("Close",
                    new ViewModelPreset.Hand(true, 0, 0, 0.25, 0, 0, 0, 1.15),
                    ViewModelPreset.Hand.NONE),
            new ViewModelPreset("Wide",
                    new ViewModelPreset.Hand(true, 0.15, 0, 0, 0, 0, 0, 1),
                    new ViewModelPreset.Hand(true, -0.15, 0, 0, 0, 0, 0, 1)));

    private final HandOptions main = new HandOptions("main", "Main hand");
    private final HandOptions off = new HandOptions("off", "Off hand");
    private final TextSetting saved = addSetting(
            new TextSetting("saved_presets", "Saved presets", 2048, "", value -> value));
    private final ChoiceSetting preset = addSetting(
            new ChoiceSetting("preset", "Preset", names(SHIPPED), "Default").deferUnknownValues());
    private String appliedSelection;
    private String savedSeen = "";

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

    /** Shipped presets followed by the user's saved ones; a saved name replaces the shipped preset of that name. */
    public List<ViewModelPreset> presets() {
        List<ViewModelPreset> all = new ArrayList<>(SHIPPED);
        for (ViewModelPreset custom : ViewModelPreset.decodeAll(saved.value())) {
            all.removeIf(existing -> existing.key().equals(custom.key()));
            all.add(custom);
        }
        return all;
    }

    /** The preset currently selected in the GUI. */
    public ViewModelPreset selected() {
        return ViewModelPreset.find(presets(), preset.value());
    }

    /** Writes a preset's values into the hand settings. Unknown names are ignored. */
    public boolean applyPreset(String name) {
        ViewModelPreset chosen = ViewModelPreset.find(presets(), name);
        if (chosen == null) return false;
        write(main, chosen.main());
        write(off, chosen.off());
        if (preset.choices().contains(chosen.name())) preset.setValue(chosen.name());
        appliedSelection = preset.value();
        return true;
    }

    /** Captures the current hand settings as a named preset and selects it. */
    public ViewModelPreset savePreset(String name) {
        ViewModelPreset captured = new ViewModelPreset(name, main.snapshot(), off.snapshot());
        List<ViewModelPreset> kept = new ArrayList<>();
        for (ViewModelPreset preset : ViewModelPreset.decodeAll(saved.value())) {
            if (!preset.key().equals(captured.key())) kept.add(preset);
        }
        kept.add(captured);
        saved.setValue(ViewModelPreset.encodeAll(kept));
        syncChoices();
        preset.setValue(captured.name());
        appliedSelection = captured.name();
        return captured;
    }

    /** Removes a user preset. Shipped presets come back on the next call to {@link #presets()}. */
    public boolean deletePreset(String name) {
        List<ViewModelPreset> kept = new ArrayList<>();
        boolean removed = false;
        for (ViewModelPreset preset : ViewModelPreset.decodeAll(saved.value())) {
            if (preset.key().equals(ViewModelPreset.cleanName(name).toLowerCase(java.util.Locale.ROOT))) removed = true;
            else kept.add(preset);
        }
        if (!removed) return false;
        saved.setValue(ViewModelPreset.encodeAll(kept));
        syncChoices();
        return true;
    }

    /** Overwrites the selected preset with the current settings; a shipped preset becomes a user preset. */
    public ViewModelPreset saveSelected() {
        return savePreset(preset.value());
    }

    @Override
    protected void onTick(Minecraft client) {
        syncChoices();
        if (!preset.value().equals(appliedSelection)) {
            appliedSelection = preset.value();
            ViewModelPreset chosen = ViewModelPreset.find(presets(), appliedSelection);
            if (chosen != null) {
                write(main, chosen.main());
                write(off, chosen.off());
            }
        }
    }

    private void syncChoices() {
        if (saved.value().equals(savedSeen)) return;
        savedSeen = saved.value();
        List<String> wanted = names(presets());
        if (!wanted.equals(preset.choices())) preset.setChoices(wanted);
    }

    private void write(HandOptions options, ViewModelPreset.Hand hand) {
        options.enabled.setValue(hand.enabled());
        options.x.setValue(hand.x());
        options.y.setValue(hand.y());
        options.z.setValue(hand.z());
        options.rx.setValue(hand.rx());
        options.ry.setValue(hand.ry());
        options.rz.setValue(hand.rz());
        options.scale.setValue(hand.scale());
    }

    private static List<String> names(List<ViewModelPreset> presets) {
        List<String> names = new ArrayList<>(presets.size());
        for (ViewModelPreset preset : presets) names.add(preset.name());
        return names;
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

        ViewModelPreset.Hand snapshot() {
            return new ViewModelPreset.Hand(enabled.value(), x.value(), y.value(), z.value(),
                    rx.value(), ry.value(), rz.value(), scale.value());
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
