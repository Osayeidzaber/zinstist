package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Leaves a line along the path you walked so you can find your way back. Cleared on world change. */
public final class BreadcrumbsModule extends ClientModule implements FxEmitter {
    private static final int CAPACITY = 4096;

    private final NumberSetting length = addSetting(new NumberSetting("length", "Max points", 50, CAPACITY, 50, 1000));
    private final NumberSetting spacing = addSetting(new NumberSetting("spacing", "Spacing (blocks)", 0.1, 3, 0.1, 0.5));
    private final BooleanSetting fade = addSetting(new BooleanSetting("fade", "Fade older points", true));
    private final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Through walls", false));
    private final NumberSetting width = addSetting(new NumberSetting("width", "Line width", 1, 4, 0.5, 2));
    private final ChoiceSetting palette = addSetting(EffectPalette.modeSetting("palette", "Color"));
    private final TextSetting custom = addSetting(EffectPalette.hexSetting("custom", "Custom color", "FFFFFF"));

    private final double[] xs = new double[CAPACITY], ys = new double[CAPACITY], zs = new double[CAPACITY];
    private int head;
    private int count;
    private Object level;

    public BreadcrumbsModule() {
        super("breadcrumbs", "Breadcrumbs", "Draws the path you walked so you can retrace it.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        count = 0;
        head = 0;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null) return;
        if (client.level != level) {
            level = client.level;
            count = 0;
            head = 0;
        }
        Vec3 pos = client.player.position();
        double y = pos.y + 0.1;
        if (count > 0) {
            int last = (head + count - 1) % CAPACITY;
            double dx = pos.x - xs[last], dy = y - ys[last], dz = pos.z - zs[last];
            double d = dx * dx + dy * dy + dz * dz;
            if (d < spacing.value() * spacing.value()) return;
            if (d > 64 * 64) {
                // Teleported: start a fresh trail instead of drawing a line across the world.
                count = 0;
                head = 0;
            }
        }
        int max = (int) Math.min(CAPACITY, Math.round(length.value()));
        while (count >= max) {
            head = (head + 1) % CAPACITY;
            count--;
        }
        int slot = (head + count) % CAPACITY;
        xs[slot] = pos.x;
        ys[slot] = y;
        zs[slot] = pos.z;
        count++;
    }

    @Override
    public void emit(FxFrame f) {
        if (count < 2) return;
        int color = EffectPalette.resolve(palette.value(), f.accent, custom.value());
        float w = (float) width.value();
        boolean walls = throughWalls.value();
        for (int i = 1; i < count; i++) {
            int a = (head + i - 1) % CAPACITY, b = (head + i) % CAPACITY;
            double alpha = fade.value() ? 0.15 + 0.85 * i / (double) count : 0.9;
            f.batch.line(xs[a] - f.camX, ys[a] - f.camY, zs[a] - f.camZ, xs[b] - f.camX, ys[b] - f.camY, zs[b] - f.camZ,
                    FxShapes.argb(color, alpha), w, walls);
        }
    }
}
