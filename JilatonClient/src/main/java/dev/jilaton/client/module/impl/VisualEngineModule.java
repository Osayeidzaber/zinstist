package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.fx.FxParticles;
import dev.jilaton.client.render.fx.VisualQuality;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/**
 * Global controls for the shared world-effect engine. It is a settings holder: disabling it does not
 * disable individual effects, it simply returns them to the Balanced profile at full opacity.
 */
public final class VisualEngineModule extends ClientModule {
    private final ChoiceSetting quality = addSetting(new ChoiceSetting("quality", "Quality profile", VisualQuality.NAMES, "Balanced"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Effect opacity", 0.1, 1, 0.05, 1));
    private final BooleanSetting adaptive = addSetting(new BooleanSetting("adaptive", "Adaptive budget under low FPS", true));
    private final NumberSetting targetFps = addSetting(new NumberSetting("target_fps", "Adaptive target FPS", 30, 144, 1, 60));
    private double fpsAverage = 60;

    public VisualEngineModule() {
        super("visual_engine", "Visual Engine", "Quality profile, opacity and adaptive particle budget for every world effect.", ModuleCategory.CLIENT);
    }

    @Override public boolean enabledByDefault() { return true; }

    @Override
    protected void onDisable(Minecraft client) {
        FxParticles.get().setBudget(VisualQuality.BALANCED.particleBudget());
    }

    /** Effective profile; under sustained low FPS the adaptive option steps one tier down. */
    public VisualQuality quality(double frameSeconds) {
        if (!enabled()) return VisualQuality.BALANCED;
        VisualQuality selected = VisualQuality.byLabel(quality.value());
        if (frameSeconds > 0) fpsAverage += (1 / frameSeconds - fpsAverage) * Math.min(1, frameSeconds * 2);
        if (adaptive.value() && fpsAverage < targetFps.value() * 0.85 && selected.ordinal() > 0)
            return VisualQuality.values()[selected.ordinal() - 1];
        return selected;
    }

    public double opacity() { return enabled() ? opacity.value() : 1; }
}
