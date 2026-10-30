package dev.jilaton.client.module.impl;

import dev.jilaton.client.gui.Motion;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Expanding ground ripples where players jump or land, driven by observed ground-state changes. */
public final class JumpCirclesModule extends ClientModule {
    private static final double TRACK_RANGE_SQR = 48 * 48;
    private static final double[] RIPPLE_DELAYS = {0, 0.14, 0.28};
    private final ChoiceSetting trigger = addSetting(new ChoiceSetting("trigger", "Trigger", List.of("Land", "Jump", "Both"), "Both"));
    private final ChoiceSetting targets = addSetting(new ChoiceSetting("targets", "Players", List.of("Self", "Others", "Everyone"), "Self"));
    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Style", List.of("Ring", "Double", "Ripple"), "Ripple"));
    private final NumberSetting lifetime = addSetting(new NumberSetting("lifetime", "Lifetime (seconds)", 0.3, 3, 0.1, 1.1));
    private final NumberSetting radius = addSetting(new NumberSetting("radius", "Radius", 0.4, 4, 0.1, 1.4));
    private final NumberSetting width = addSetting(new NumberSetting("width", "Line width", 1, 5, 0.5, 2));
    private final BooleanSetting fill = addSetting(new BooleanSetting("fill", "Feathered fill", true));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "F2F2F2"));
    private final NumberSetting limit = addSetting(new NumberSetting("limit", "Max circles", 4, 64, 1, 24));
    private final ArrayDeque<Circle> circles = new ArrayDeque<>();
    private final Map<Integer, Boolean> grounded = new HashMap<>();
    private Object boundLevel;

    public JumpCirclesModule() {
        super("jump_circles", "JumpCircles", "Expanding ground ripples when players jump or land.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        circles.clear();
        grounded.clear();
        boundLevel = null;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.level == null || client.player == null) {
            onDisable(client);
            return;
        }
        if (client.level != boundLevel) {
            circles.clear();
            grounded.clear();
            boundLevel = client.level;
        }
        long now = System.nanoTime();
        Set<Integer> seen = new HashSet<>();
        for (Player player : client.level.players()) {
            boolean self = player == client.player;
            if (self ? targets.value().equals("Others") : targets.value().equals("Self")) continue;
            if (!self && (player.isSpectator() || player.distanceToSqr(client.player) > TRACK_RANGE_SQR)) continue;
            seen.add(player.getId());
            boolean onGround = player.onGround();
            Boolean before = grounded.put(player.getId(), onGround);
            if (before == null || before == onGround || player.isPassenger() || player.getAbilities().flying) continue;
            boolean jumped = before && player.getDeltaMovement().y > 0.08;
            boolean landed = !before;
            String mode = trigger.value();
            if (jumped && !mode.equals("Land") || landed && !mode.equals("Jump")) {
                circles.addLast(new Circle(player.getX(), player.getY(), player.getZ(), now));
                while (circles.size() > (int) limit.value()) circles.removeFirst();
            }
        }
        grounded.keySet().retainAll(seen);
    }

    public void emit(EffectBatch batch, Vec3 camera, int accent, long now) {
        if (!enabled() || circles.isEmpty()) return;
        int rgb = EffectPalette.resolve(colorMode.value(), accent, color.value());
        double life = lifetime.value();
        Iterator<Circle> iterator = circles.iterator();
        while (iterator.hasNext()) {
            Circle circle = iterator.next();
            double age = (now - circle.born()) / 1_000_000_000.0 / life;
            if (age >= 1) {
                iterator.remove();
                continue;
            }
            double x = circle.x() - camera.x, y = circle.y() - camera.y + 0.03, z = circle.z() - camera.z;
            switch (style.value()) {
                case "Ring" -> ring(batch, x, y, z, age, 1.0, rgb);
                case "Double" -> {
                    ring(batch, x, y, z, age, 1.0, rgb);
                    ring(batch, x, y, z, age, 0.66, rgb);
                }
                default -> {
                    for (double delay : RIPPLE_DELAYS) {
                        double local = (age - delay) / (1 - delay);
                        if (local > 0) ring(batch, x, y, z, local, 1.0 - delay * 0.8, rgb);
                    }
                }
            }
        }
    }

    private void ring(EffectBatch batch, double x, double y, double z, double progress, double size, int rgb) {
        double r = radius.value() * size * Motion.SETTLE.apply(progress);
        double alpha = Math.pow(1 - progress, 1.6);
        if (r < 0.02 || alpha < 0.01) return;
        int segments = r > 2 ? 72 : 48;
        if (fill.value()) {
            batch.band(x, y, z, r * 0.62, r, segments, EffectBatch.withAlpha(rgb, 0),
                    EffectBatch.withAlpha(rgb, alpha * 0.3), false);
        }
        batch.glowRing(x, y, z, r, segments, EffectBatch.withAlpha(rgb, alpha), (float) width.value(), false, 0, 0);
    }

    /** Visible for tests: number of live circles. */
    int activeCircles() {
        return circles.size();
    }

    private record Circle(double x, double y, double z, long born) { }
}
