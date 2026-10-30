package dev.jilaton.client.render;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.module.impl.DamageParticlesModule;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class DamageParticleRenderer {
    public void register(ModuleManager modules, ClientConfig config, WorldOverlayProjection projection) {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("jilaton", "damage_particles"), (graphics, delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || !projection.isCurrent(client) || client.screen != null || client.options.hideGui
                    || !(modules.get("damage_particles") instanceof DamageParticlesModule particles) || !particles.enabled()) return;
            long now = System.nanoTime();
            for (var particle : particles.particles()) {
                double progress = particles.progress(particle, now);
                if (progress >= 1) continue;
                double eased = 1 - (1 - progress) * (1 - progress);
                Vec3 pos = particle.position();
                double rise = config.reducedMotion() ? 0 : particles.rise() * eased;
                if (!projection.project(pos.add(0, rise, 0), graphics.guiWidth(), graphics.guiHeight())) continue;
                float scale = particles.scale();
                int alpha = Math.max(4, (int) (255 * Math.min(1, (1 - progress) * 4)));
                graphics.pose().pushMatrix();
                graphics.pose().translate(projection.x(graphics.guiWidth()), projection.y(graphics.guiHeight()));
                graphics.pose().scale(scale, scale);
                graphics.drawString(client.font, particle.text(), -client.font.width(particle.text()) / 2, 0,
                        (particle.color() & 0xFFFFFF) | (alpha << 24));
                graphics.pose().popMatrix();
            }
        });
    }
}
