package dev.jilaton.client.module.impl;

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

/** Fading motion trails behind players, sampled from interpolated render positions. */
public final class TrailsModule extends ClientModule {
    private static final int MAX_SAMPLES = 96;
    private static final int MAX_TRACKED = 12;
    private static final double TRACK_RANGE_SQR = 64 * 64;
    private static final double MIN_SPACING_SQR = 0.04 * 0.04;
    private final ChoiceSetting targets = addSetting(new ChoiceSetting("targets", "Players", List.of("Self", "Others", "Everyone"), "Self"));
    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Style", List.of("Ribbon", "Line", "Ground"), "Ribbon"));
    private final NumberSetting lifetime = addSetting(new NumberSetting("lifetime", "Lifetime (seconds)", 0.2, 3, 0.1, 0.7));
    private final NumberSetting height = addSetting(new NumberSetting("height", "Ribbon height", 0.1, 1, 0.05, 0.55));
    private final NumberSetting width = addSetting(new NumberSetting("width", "Line width", 1, 5, 0.5, 2));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.1, 1, 0.05, 0.85));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "B4B4B4"));
    private final BooleanSetting firstPerson = addSetting(new BooleanSetting("first_person", "Own trail in first person", false));
    private final Map<Integer, ArrayDeque<Sample>> trails = new HashMap<>();
    private Object boundLevel;

    public TrailsModule() {
        super("trails", "Trails", "Fading ribbon, line or ground trails behind players.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        trails.clear();
        boundLevel = null;
    }

    public void emit(EffectBatch batch, Minecraft client, float partial, Vec3 camera, int accent, long now) {
        if (!enabled()) return;
        if (client.level == null || client.player == null) {
            trails.clear();
            return;
        }
        if (client.level != boundLevel) {
            trails.clear();
            boundLevel = client.level;
        }
        double life = lifetime.value();
        boolean firstPersonView = client.options.getCameraType().isFirstPerson();
        Set<Integer> seen = new HashSet<>();
        for (Player player : client.level.players()) {
            boolean self = player == client.player;
            if (self ? targets.value().equals("Others") : targets.value().equals("Self")) continue;
            if (!self && (player.isSpectator() || player.isInvisible() || player.distanceToSqr(client.player) > TRACK_RANGE_SQR)) continue;
            if (!trails.containsKey(player.getId()) && trails.size() >= MAX_TRACKED) continue;
            seen.add(player.getId());
            ArrayDeque<Sample> samples = trails.computeIfAbsent(player.getId(), ignored -> new ArrayDeque<>());
            Vec3 position = player.getPosition(partial);
            Sample last = samples.peekLast();
            if (last == null || distanceSqr(last, position) > MIN_SPACING_SQR) {
                samples.addLast(new Sample(position.x, position.y, position.z, player.getBbHeight(), now));
                while (samples.size() > MAX_SAMPLES) samples.removeFirst();
            }
            while (!samples.isEmpty() && (now - samples.peekFirst().time()) / 1_000_000_000.0 > life) samples.removeFirst();
            if (self && firstPersonView && !firstPerson.value()) continue;
            draw(batch, samples, camera, accent, now, life, self && firstPersonView ? position : null);
        }
        trails.keySet().retainAll(seen);
    }

    private void draw(EffectBatch batch, ArrayDeque<Sample> samples, Vec3 camera, int accent, long now, double life, Vec3 exclude) {
        if (samples.size() < 2) return;
        int rgb = EffectPalette.resolve(colorMode.value(), accent, color.value());
        Iterator<Sample> iterator = samples.iterator();
        Sample previous = iterator.next();
        while (iterator.hasNext()) {
            Sample next = iterator.next();
            if (exclude != null && distanceSqr(next, exclude) < 0.36) break;
            double a0 = fade(previous, now, life), a1 = fade(next, now, life);
            double x0 = previous.x() - camera.x, y0 = previous.y() - camera.y, z0 = previous.z() - camera.z;
            double x1 = next.x() - camera.x, y1 = next.y() - camera.y, z1 = next.z() - camera.z;
            switch (style.value()) {
                case "Line" -> {
                    double h0 = previous.height() * 0.5, h1 = next.height() * 0.5;
                    batch.glowLine(x0, y0 + h0, z0, x1, y1 + h1, z1, EffectBatch.withAlpha(rgb, a1), (float) width.value(), false);
                }
                case "Ground" -> batch.line(x0, y0 + 0.03, z0, x1, y1 + 0.03, z1,
                        EffectBatch.withAlpha(rgb, a1), (float) width.value() * 1.5F, false);
                default -> {
                    double bottom0 = previous.height() * 0.08, bottom1 = next.height() * 0.08;
                    double top0 = previous.height() * height.value(), top1 = next.height() * height.value();
                    batch.quad(x0, y0 + bottom0, z0, x1, y1 + bottom1, z1, x1, y1 + top1, z1, x0, y0 + top0, z0,
                            EffectBatch.withAlpha(rgb, a0 * 0.12), EffectBatch.withAlpha(rgb, a1 * 0.12),
                            EffectBatch.withAlpha(rgb, a1 * 0.62), EffectBatch.withAlpha(rgb, a0 * 0.62), false);
                    batch.glowLine(x0, y0 + top0, z0, x1, y1 + top1, z1, EffectBatch.withAlpha(rgb, a1), (float) width.value(), false);
                }
            }
            previous = next;
        }
    }

    private double fade(Sample sample, long now, double life) {
        double age = Math.min(1, (now - sample.time()) / 1_000_000_000.0 / life);
        return opacity.value() * Math.pow(1 - age, 1.5);
    }

    private static double distanceSqr(Sample sample, Vec3 position) {
        double dx = sample.x() - position.x, dy = sample.y() - position.y, dz = sample.z() - position.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private record Sample(double x, double y, double z, double height, long time) { }
}
