package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.LivingOverlayOptions;
import dev.jilaton.client.setting.*;
import net.minecraft.client.Minecraft;
import java.util.List;

public final class TracersModule extends ClientModule {
    private final LivingOverlayOptions options = new LivingOverlayOptions(this::addSetting);
    private final ChoiceSetting point = addSetting(new ChoiceSetting("point", "Line endpoint", List.of("Feet", "Center", "Head"), "Feet"));
    private final BooleanSetting stem = addSetting(new BooleanSetting("stem", "Vertical body line", true));

    public TracersModule() {
        super("tracers", "Tracers", "Camera-centered lines to filtered entities with distance/health colors.", ModuleCategory.RENDER);
    }

    @Override protected void onEnable(Minecraft client) { options.update(client); }
    @Override protected void onTick(Minecraft client) { options.update(client); }
    @Override protected void onDisable(Minecraft client) { options.clear(); }
    public LivingOverlayOptions options() { return options; }
    public boolean stem() { return stem.value(); }
    public double endpoint(double height) { return point.value().equals("Feet") ? 0 : point.value().equals("Head") ? height : height / 2; }
}
