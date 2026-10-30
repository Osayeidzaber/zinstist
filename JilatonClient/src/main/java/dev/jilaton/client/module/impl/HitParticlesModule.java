/* Adapted from Anubis 0.9.9.3 HitParticlesModule (Horcus / 4ldenz), GPL-3.0-only.
 * Reviewed recovered source, not an original source reconstruction. See ATTRIBUTION.md. */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.ClientTheme;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.ParticleBurstMath;
import dev.jilaton.client.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class HitParticlesModule extends ClientModule {
    private final ChoiceSetting type = addSetting(new ChoiceSetting("type", "Particle", List.of("Crit", "Magic Crit", "Spark", "Flame", "Soul Flame", "End Rod", "Totem", "Heart", "Dust"), "Crit"));
    private final NumberSetting amount = addSetting(new NumberSetting("amount", "Particles per attack", 1, 40, 1, 10));
    private final NumberSetting spread = addSetting(new NumberSetting("spread", "Spread", 0.25, 3, 0.25, 1));
    private final ChoiceSetting origin = addSetting(new ChoiceSetting("origin", "Origin", List.of("Impact", "Body"), "Impact"));
    private final BooleanSetting onlyCrits = addSetting(new BooleanSetting("only_crits", "Only critical attacks", false));
    private final ChoiceSetting targets = addSetting(new ChoiceSetting("targets", "Targets", List.of("Living", "Players", "All"), "Living"));
    private final ChoiceSetting colors = addSetting(new ChoiceSetting("dust_color_mode", "Dust color mode", List.of("Theme", "Custom"), "Theme"));
    private final TextSetting color = addSetting(new TextSetting("dust_color", "Dust RGB hex", 7, "F2F2F2", value -> {
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (!normalized.matches("[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("Use six RGB hex digits");
        return normalized.toUpperCase(Locale.ROOT);
    }));
    private final NumberSetting size = addSetting(new NumberSetting("dust_size", "Dust size", 0.5, 2, 0.1, 1));
    private final Random random = new Random();

    public HitParticlesModule() {
        super("hit_particles", "HitParticles", "Local particle bursts on attack attempts; impact/body origins and nine particle types.", ModuleCategory.RENDER);
    }

    public void beforeAttack(Minecraft client, Player player, Entity target) {
        if (!enabled() || client.player != player || client.level == null || client.screen != null
                || target == null || target == player || target.level() != client.level || !target.isAttackable()
                || !ParticleBurstMath.accepts(targets.value(), target instanceof LivingEntity, target instanceof Player)) return;
        if (onlyCrits.value() && !(target instanceof LivingEntity && player.getAttackStrengthScale(0.5F) > 0.9F
                && player.fallDistance > 0 && !player.onGround() && !player.onClimbable() && !player.isInWater()
                && !player.isInLava() && !player.isPassenger() && !player.isFallFlying()
                && !player.hasEffect(MobEffects.BLINDNESS) && !player.isSprinting())) return;
        Burst burst = burst();
        AABB box = target.getBoundingBox();
        Vec3 center = box.getCenter();
        Vec3 impact = client.hitResult instanceof EntityHitResult hit && hit.getEntity() == target
                ? hit.getLocation() : ParticleBurstMath.nearestPoint(box, player.getEyePosition());
        double scale = spread.value();
        for (int i = 0; i < (int) amount.value(); i++) {
            Vec3 from = origin.value().equals("Body")
                    ? ParticleBurstMath.pointInside(box, random.nextDouble(), random.nextDouble(), random.nextDouble()) : impact;
            Vec3 out = from.subtract(center);
            out = out.lengthSqr() < 1.0E-6 ? player.getLookAngle().reverse() : out.normalize();
            Vec3 randomDirection = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            randomDirection = randomDirection.lengthSqr() < 1.0E-9 ? new Vec3(0, 1, 0) : randomDirection.normalize();
            Vec3 velocity = out.scale(0.8).add(randomDirection).add(0, 0.25, 0).normalize()
                    .scale(burst.speed() * scale * (0.6 + 0.4 * random.nextDouble()));
            double jitter = burst.jitter() * scale;
            client.level.addParticle(burst.particle(), true, false,
                    from.x + random.nextGaussian() * jitter, from.y + random.nextGaussian() * jitter,
                    from.z + random.nextGaussian() * jitter, velocity.x, velocity.y, velocity.z);
        }
    }

    private Burst burst() {
        return switch (type.value()) {
            case "Magic Crit" -> new Burst(ParticleTypes.ENCHANTED_HIT, 0.8, 0.1);
            case "Spark" -> new Burst(ParticleTypes.ELECTRIC_SPARK, 0.16, 0.1);
            case "Flame" -> new Burst(ParticleTypes.FLAME, 0.035, 0.12);
            case "Soul Flame" -> new Burst(ParticleTypes.SOUL_FIRE_FLAME, 0.035, 0.12);
            case "End Rod" -> new Burst(ParticleTypes.END_ROD, 0.09, 0.1);
            case "Totem" -> new Burst(ParticleTypes.TOTEM_OF_UNDYING, 0.45, 0.1);
            case "Heart" -> new Burst(ParticleTypes.HEART, 0, 0.35);
            case "Dust" -> {
                var config = JilatonClient.clientConfig();
                int rgb = colors.value().equals("Custom") ? Integer.parseInt(color.value(), 16)
                        : ClientTheme.resolveAccent(config.themeId(), config.customAccent()) & 0xFFFFFF;
                yield new Burst(new DustParticleOptions(rgb, (float) size.value()), 1, 0.3);
            }
            default -> new Burst(ParticleTypes.CRIT, 0.8, 0.1);
        };
    }

    private record Burst(ParticleOptions particle, double speed, double jitter) { }
}
