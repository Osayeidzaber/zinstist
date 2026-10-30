package dev.jilaton.client.module.impl;

import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

final class CrystalDamageEstimator {
    private static final double CRYSTAL_EXPLOSION_DIAMETER = 12.0;
    private final Level level;
    private final LivingEntity victim;
    private final double explosionDiameter;
    private final BlockPos ignoredSourceBlock;
    private final Map<Vec3, Float> exposureCache = new HashMap<>();

    CrystalDamageEstimator(Level level, LivingEntity victim) {
        this(level, victim, CRYSTAL_EXPLOSION_DIAMETER);
    }

    CrystalDamageEstimator(Level level, LivingEntity victim, double explosionDiameter) {
        this(level, victim, explosionDiameter, null);
    }

    CrystalDamageEstimator(Level level, LivingEntity victim, double explosionDiameter, BlockPos ignoredSourceBlock) {
        if (!Double.isFinite(explosionDiameter) || explosionDiameter <= 0.0) {
            throw new IllegalArgumentException("Explosion diameter must be positive and finite");
        }
        this.level = level;
        this.victim = victim;
        this.explosionDiameter = explosionDiameter;
        this.ignoredSourceBlock = ignoredSourceBlock == null ? null : ignoredSourceBlock.immutable();
    }

    float estimate(Vec3 center) {
        double distance = Math.sqrt(victim.position().distanceToSqr(center)) / explosionDiameter;
        if (distance > 1.0) {
            return 0.0F;
        }

        double impact = (1.0 - distance) * exposureCache.computeIfAbsent(center, this::calculateExposure);
        float damage = rawDamage(impact, explosionDiameter);
        return reduce(applyDifficulty(damage));
    }

    float upperBound(Vec3 center) {
        double distance = Math.sqrt(victim.position().distanceToSqr(center)) / explosionDiameter;
        if (distance > 1.0) {
            return 0.0F;
        }
        return reduce(applyDifficulty(rawDamage(1.0, explosionDiameter)));
    }

    private float applyDifficulty(float damage) {
        if (victim instanceof Player) {
            damage = switch (level.getDifficulty()) {
                case PEACEFUL -> 0.0F;
                case EASY -> Math.min(damage / 2.0F + 1.0F, damage);
                case NORMAL -> damage;
                case HARD -> damage * 1.5F;
            };
        }
        return damage;
    }

    private float reduce(float damage) {
        DamageSource source = level.damageSources().explosion(null, null);
        float toughness = (float) victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        damage = CombatRules.getDamageAfterAbsorb(victim, damage, source, victim.getArmorValue(), toughness);
        MobEffectInstance resistance = victim.getEffect(MobEffects.RESISTANCE);
        if (resistance != null) {
            damage *= Math.max(0.0F, (25.0F - (resistance.getAmplifier() + 1) * 5.0F) / 25.0F);
        }
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> protectionType = enchantments.getOrThrow(Enchantments.PROTECTION);
        Holder<Enchantment> blastProtectionType = enchantments.getOrThrow(Enchantments.BLAST_PROTECTION);
        int protection = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            var armor = victim.getItemBySlot(slot);
            protection += EnchantmentHelper.getItemEnchantmentLevel(protectionType, armor)
                    + 2 * EnchantmentHelper.getItemEnchantmentLevel(blastProtectionType, armor);
        }
        damage = CombatRules.getDamageAfterMagicAbsorb(damage, protection);
        return Math.max(0.0F, damage);
    }

    static float rawDamage(double impact, double diameter) {
        double boundedImpact = Math.max(0.0, Math.min(1.0, impact));
        return (float) (((boundedImpact * boundedImpact + boundedImpact) / 2.0) * 7.0 * diameter + 1.0);
    }

    private float calculateExposure(Vec3 center) {
        AABB box = victim.getBoundingBox();
        int visibleSamples = 0;
        int sampleCount = 0;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    Vec3 sample = new Vec3(
                            lerp((x + 0.5) / 3.0, box.minX, box.maxX),
                            lerp((y + 0.5) / 3.0, box.minY, box.maxY),
                            lerp((z + 0.5) / 3.0, box.minZ, box.maxZ));
                    Vec3 start = sourceBlockTraceStart(center, sample, ignoredSourceBlock);
                    HitResult hit = level.clip(new ClipContext(start, sample, ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE, victim));
                    if (hit.getType() == HitResult.Type.MISS) {
                        visibleSamples++;
                    }
                    sampleCount++;
                }
            }
        }
        return (float) visibleSamples / sampleCount;
    }

    private static double lerp(double amount, double start, double end) {
        return start + (end - start) * amount;
    }

    static Vec3 sourceBlockTraceStart(Vec3 center, Vec3 sample, BlockPos ignoredBlock) {
        if (ignoredBlock == null || center.x < ignoredBlock.getX() || center.x > ignoredBlock.getX() + 1.0
                || center.y < ignoredBlock.getY() || center.y > ignoredBlock.getY() + 1.0
                || center.z < ignoredBlock.getZ() || center.z > ignoredBlock.getZ() + 1.0) {
            return center;
        }

        Vec3 delta = sample.subtract(center);
        double length = delta.length();
        if (length < 1.0E-8) {
            return center;
        }
        Vec3 direction = delta.scale(1.0 / length);
        double exitDistance = Double.POSITIVE_INFINITY;
        if (Math.abs(direction.x) > 1.0E-8) {
            exitDistance = Math.min(exitDistance, 0.5 / Math.abs(direction.x));
        }
        if (Math.abs(direction.y) > 1.0E-8) {
            exitDistance = Math.min(exitDistance, 0.5 / Math.abs(direction.y));
        }
        if (Math.abs(direction.z) > 1.0E-8) {
            exitDistance = Math.min(exitDistance, 0.5 / Math.abs(direction.z));
        }
        return center.add(direction.scale(Math.min(length, exitDistance + 1.0E-3)));
    }
}
