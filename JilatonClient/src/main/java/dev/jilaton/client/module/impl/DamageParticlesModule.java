/* Adapts LiquidBounce ModuleDamageParticles' OnTick health-change tracking into Java.
 * Copyright (c) 2015-2026 CCBlueX. GPL-3.0-or-later; see ATTRIBUTION.md. */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.HealthChangeTracker;
import dev.jilaton.client.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public final class DamageParticlesModule extends ClientModule {
    private final NumberSetting lifetime = addSetting(new NumberSetting("lifetime", "Lifetime (seconds)", 0.5, 5, 0.5, 2));
    private final NumberSetting scale = addSetting(new NumberSetting("scale", "Text scale", 0.5, 3, 0.25, 1));
    private final NumberSetting rise = addSetting(new NumberSetting("rise", "Rise distance", 0, 3, 0.25, 1.5));
    private final NumberSetting range = addSetting(new NumberSetting("range", "Track range", 8, 128, 8, 64));
    private final BooleanSetting players = addSetting(new BooleanSetting("players_only", "Players only", false));
    private final BooleanSetting healing = addSetting(new BooleanSetting("healing", "Show healing", true));
    private final HealthChangeTracker tracker = new HealthChangeTracker(512);
    private final ArrayDeque<Particle> particles = new ArrayDeque<>();
    private final Collection<Particle> view = Collections.unmodifiableCollection(particles);
    private Object world;

    public DamageParticlesModule() {
        super("damage_particles", "DamageParticles", "Floating damage and healing values from observed entity health changes.", ModuleCategory.RENDER);
    }

    @Override protected void onEnable(Minecraft client) { reset(); }
    @Override protected void onDisable(Minecraft client) { reset(); }

    private void reset() { tracker.clear(); particles.clear(); world = null; }

    @Override protected void onTick(Minecraft client) {
        if (client.level != world) { reset(); world = client.level; }
        if (client.level == null || client.player == null) { reset(); return; }
        long now = System.nanoTime();
        particles.removeIf(p -> progress(p, now) >= 1);
        tracker.beginTick();
        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == client.player || entity.isRemoved()
                    || entity.tickCount == 0 || (players.value() && !(entity instanceof Player))
                    || entity.distanceToSqr(client.player) > range.value() * range.value()) continue;
            float delta = tracker.observe(entity.getId(), living.getHealth());
            if (delta == 0 || (delta > 0 && !healing.value())) continue;
            if (particles.size() >= 256) particles.removeFirst();
            String text = (delta > 0 ? "+" : "-") + String.format(Locale.ROOT, "%.1f", Math.abs(delta));
            particles.addLast(new Particle(now, text, delta > 0 ? 0xFF69E59B : 0xFFFF788A,
                    entity.getBoundingBox().getCenter().add(entity.getKnownMovement())));
        }
        tracker.endTick();
    }

    public Collection<Particle> particles() { return view; }
    public float scale() { return (float) scale.value(); }
    public double rise() { return rise.value(); }
    public double progress(Particle particle, long now) {
        return Math.max(0, Math.min(1, (now - particle.born()) / (lifetime.value() * 1_000_000_000.0)));
    }
    public record Particle(long born, String text, int color, Vec3 position) { }
}
