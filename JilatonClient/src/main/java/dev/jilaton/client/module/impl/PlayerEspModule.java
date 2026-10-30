package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.LivingOverlayOptions;
import dev.jilaton.client.setting.*;
import net.minecraft.client.Minecraft;
import java.util.List;

public final class PlayerEspModule extends ClientModule {
    private final LivingOverlayOptions options = new LivingOverlayOptions(this::addSetting);
    private final ChoiceSetting mode = addSetting(new ChoiceSetting("mode", "Mode", List.of("Box", "Glow", "Both"), "Box"));
    private final BooleanSetting outline = addSetting(new BooleanSetting("outline", "Box outline", true));
    private final NumberSetting fill = addSetting(new NumberSetting("fill_opacity", "Box fill opacity", 0, 0.8, 0.05, 0.2));
    private final NumberSetting expand = addSetting(new NumberSetting("expand", "Box expansion", 0, 0.5, 0.01, 0.05));

    public PlayerEspModule() {
        super("player_esp", "PlayerESP", "Interpolated player boxes and native glowing outlines; optional living-entity filters.", ModuleCategory.RENDER);
    }

    @Override protected void onEnable(Minecraft client) { options.update(client); }
    @Override protected void onTick(Minecraft client) { options.update(client); }
    @Override protected void onDisable(Minecraft client) { options.clear(); }
    public LivingOverlayOptions options() { return options; }
    public boolean boxes() { return !mode.value().equals("Glow"); }
    public boolean glow() { return !mode.value().equals("Box"); }
    public boolean outline() { return outline.value(); }
    public double fillOpacity() { return fill.value(); }
    public double expansion() { return expand.value(); }
}
