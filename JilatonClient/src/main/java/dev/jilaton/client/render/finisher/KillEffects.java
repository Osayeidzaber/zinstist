package dev.jilaton.client.render.finisher;

import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxShapes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Kill effects played where something was defeated. Each runs for a few ticks of client-only particles and
 * sounds (and a few world-space shapes through {@link #draw}); none of it spawns entities or touches the server.
 */
public final class KillEffects {
    public enum Effect {
        LIGHTNING("Lightning", 12), SOUL_RISE("Soul rise", 30), BLOOD_BURST("Blood burst", 6), SHATTER("Shatter", 4),
        FIREWORK("Firework", 14), TORNADO("Tornado", 36), BLACK_HOLE("Black hole", 34), HEARTS("Hearts", 10),
        TOTEM("Totem burst", 8), FROST("Frost", 16), ENDER("Ender rift", 24), CONFETTI("Confetti", 18);

        public final String label;
        public final int ticks;

        Effect(String label, int ticks) {
            this.label = label;
            this.ticks = ticks;
        }
    }

    public static final List<String> LABELS;

    static {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        labels.add("None");
        for (Effect e : Effect.values()) labels.add(e.label);
        labels.add("Random");
        LABELS = List.copyOf(labels);
    }

    private KillEffects() { }

    /** The effect for a setting label; Random picks one; None returns null. */
    public static Effect pick(String label) {
        if (label.equals("Random")) return Effect.values()[ThreadLocalRandom.current().nextInt(Effect.values().length)];
        for (Effect e : Effect.values()) if (e.label.equals(label)) return e;
        return null;
    }

    /** One client tick of the effect, {@code age} ticks after it started (0 on the first). */
    public static void tick(ClientLevel level, Effect e, double x, double y, double z, int age, float volume) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        switch (e) {
            case LIGHTNING -> {
                if (age == 0) {
                    sound(level, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, volume * 0.6F, 1.3F);
                    sound(level, x, y, z, SoundEvents.LIGHTNING_BOLT_IMPACT, volume, 1.0F);
                }
                if (age < 4) for (int i = 0; i < 10; i++)
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + gauss(r, 0.6), y + r.nextDouble() * 2, z + gauss(r, 0.6),
                            gauss(r, 0.3), r.nextDouble() * 0.3, gauss(r, 0.3));
                if (age == 1) for (int i = 0; i < 6; i++)
                    level.addParticle(ParticleTypes.LARGE_SMOKE, x + gauss(r, 0.4), y + 0.2, z + gauss(r, 0.4), 0, 0.05, 0);
            }
            case SOUL_RISE -> {
                if (age == 0) sound(level, x, y, z, SoundEvents.SOUL_ESCAPE.value(), volume, 0.9F);
                double a = age * 0.55;
                for (int i = 0; i < 2; i++) {
                    double ang = a + i * Math.PI;
                    level.addParticle(ParticleTypes.SOUL, x + Math.cos(ang) * 0.5, y + age * 0.09, z + Math.sin(ang) * 0.5, 0, 0.06, 0);
                }
                if (age % 3 == 0) level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + gauss(r, 0.3), y + 0.1, z + gauss(r, 0.3), 0, 0.08, 0);
            }
            case BLOOD_BURST -> {
                if (age == 0) sound(level, x, y, z, SoundEvents.SLIME_SQUISH, volume, 0.6F);
                DustParticleOptions blood = new DustParticleOptions(0x8A0E0E, 1.4F);
                for (int i = 0; i < 14; i++)
                    level.addParticle(blood, x + gauss(r, 0.2), y + 0.9 + gauss(r, 0.3), z + gauss(r, 0.2),
                            gauss(r, 0.35), r.nextDouble() * 0.35, gauss(r, 0.35));
                if (age == 0) for (int i = 0; i < 20; i++)
                    level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                            x + gauss(r, 0.3), y + 1, z + gauss(r, 0.3), 0, 0, 0);
            }
            case SHATTER -> {
                if (age == 0) {
                    sound(level, x, y, z, SoundEvents.GLASS_BREAK, volume, 0.7F);
                    sound(level, x, y, z, SoundEvents.AMETHYST_CLUSTER_BREAK, volume, 1.2F);
                }
                for (int i = 0; i < 18; i++)
                    level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()),
                            x + gauss(r, 0.3), y + r.nextDouble() * 1.8, z + gauss(r, 0.3), gauss(r, 0.4), r.nextDouble() * 0.3, gauss(r, 0.4));
            }
            case FIREWORK -> {
                if (age == 0) sound(level, x, y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, volume, 1F);
                if (age < 8) level.addParticle(ParticleTypes.FIREWORK, x, y + age * 0.4, z, gauss(r, 0.02), 0.05, gauss(r, 0.02));
                if (age == 8) {
                    sound(level, x, y + 3.2, z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, volume, 1F);
                    sound(level, x, y + 3.2, z, SoundEvents.FIREWORK_ROCKET_TWINKLE, volume * 0.7F, 1F);
                    for (int i = 0; i < 70; i++) {
                        double th = r.nextDouble() * Math.PI * 2, ph = Math.acos(2 * r.nextDouble() - 1), s = 0.35;
                        level.addParticle(ParticleTypes.FIREWORK, x, y + 3.2, z,
                                Math.sin(ph) * Math.cos(th) * s, Math.cos(ph) * s, Math.sin(ph) * Math.sin(th) * s);
                    }
                }
            }
            case TORNADO -> {
                if (age == 0) sound(level, x, y, z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), volume, 0.7F);
                for (int i = 0; i < 6; i++) {
                    double h = r.nextDouble() * 3.2, rad = 0.15 + h * 0.32, ang = age * 0.7 + h * 3 + i;
                    level.addParticle(ParticleTypes.CLOUD, x + Math.cos(ang) * rad, y + h, z + Math.sin(ang) * rad,
                            -Math.sin(ang) * 0.12, 0.04, Math.cos(ang) * 0.12);
                }
            }
            case BLACK_HOLE -> {
                if (age == 0) sound(level, x, y, z, SoundEvents.BEACON_DEACTIVATE, volume, 0.6F);
                if (age == 28) sound(level, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), volume * 0.5F, 1.8F);
                double cy = y + 1.1;
                for (int i = 0; i < 8; i++) {
                    double ang = r.nextDouble() * Math.PI * 2, rad = 1.6 + r.nextDouble();
                    double px = x + Math.cos(ang) * rad, pz = z + Math.sin(ang) * rad, py = cy + gauss(r, 0.6);
                    // Portal particles drift toward (x,y,z) - offset, so aim the offset back at the centre.
                    level.addParticle(ParticleTypes.REVERSE_PORTAL, x, cy, z, px - x, py - cy, pz - z);
                    level.addParticle(ParticleTypes.PORTAL, px, py, pz, x - px, cy - py, z - pz);
                }
                if (age == 28) for (int i = 0; i < 30; i++)
                    level.addParticle(ParticleTypes.SQUID_INK, x, cy, z, gauss(r, 0.3), gauss(r, 0.3), gauss(r, 0.3));
            }
            case HEARTS -> {
                if (age == 0) sound(level, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, volume, 1.4F);
                if (age % 2 == 0) for (int i = 0; i < 3; i++)
                    level.addParticle(ParticleTypes.HEART, x + gauss(r, 0.5), y + 0.5 + r.nextDouble() * 1.5, z + gauss(r, 0.5), 0, 0.1, 0);
            }
            case TOTEM -> {
                if (age == 0) sound(level, x, y, z, SoundEvents.TOTEM_USE, volume * 0.6F, 1.2F);
                for (int i = 0; i < 16; i++)
                    level.addParticle(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1, z, gauss(r, 0.5), r.nextDouble() * 0.7, gauss(r, 0.5));
            }
            case FROST -> {
                if (age == 0) {
                    sound(level, x, y, z, SoundEvents.POWDER_SNOW_BREAK, volume, 0.8F);
                    sound(level, x, y, z, SoundEvents.GLASS_BREAK, volume * 0.6F, 1.5F);
                }
                for (int i = 0; i < 6; i++)
                    level.addParticle(ParticleTypes.SNOWFLAKE, x + gauss(r, 0.6), y + r.nextDouble() * 2, z + gauss(r, 0.6),
                            gauss(r, 0.05), 0.02, gauss(r, 0.05));
                if (age == 0) for (int i = 0; i < 24; i++)
                    level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                            x + gauss(r, 0.3), y + r.nextDouble() * 1.8, z + gauss(r, 0.3), 0, 0, 0);
            }
            case ENDER -> {
                if (age == 0) sound(level, x, y, z, SoundEvents.ENDERMAN_TELEPORT, volume, 0.5F);
                if (age == 20) sound(level, x, y, z, SoundEvents.END_PORTAL_SPAWN, volume * 0.25F, 1.6F);
                for (int i = 0; i < 6; i++)
                    level.addParticle(ParticleTypes.PORTAL, x + gauss(r, 0.4), y + 0.1, z + gauss(r, 0.4), 0, 0.03, 0);
                level.addParticle(ParticleTypes.END_ROD, x + gauss(r, 0.3), y + r.nextDouble() * 2, z + gauss(r, 0.3), 0, 0.05, 0);
            }
            case CONFETTI -> {
                if (age == 0) sound(level, x, y, z, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, volume, 1.2F);
                if (age < 6) for (int i = 0; i < 14; i++) {
                    int color = java.awt.Color.HSBtoRGB(r.nextFloat(), 0.75F, 1F) & 0xFFFFFF;
                    level.addParticle(new DustParticleOptions(color, 1.1F), x + gauss(r, 0.2), y + 1.2, z + gauss(r, 0.2),
                            gauss(r, 0.5), 0.3 + r.nextDouble() * 0.4, gauss(r, 0.5));
                }
            }
        }
    }

    /** World-space shapes for effects that need more than particles; {@code t} is 0..1 through the effect. */
    public static void draw(FxFrame f, Effect e, double x, double y, double z, float t, long seed) {
        double cx = x - f.camX, cy = y - f.camY, cz = z - f.camZ;
        double fade = 1 - t;
        switch (e) {
            case LIGHTNING -> {
                if (t > 0.6F) return;
                java.util.Random r = new java.util.Random(seed);
                double px = cx, py = cy + 14, pz = cz;
                int glow = FxShapes.argb(0xDDE8FF, 0.9 * (1 - t / 0.6));
                for (int i = 0; i < 14; i++) {
                    double k = (i + 1) / 14.0;
                    double nx = cx + (k < 1 ? (r.nextDouble() - 0.5) * 1.2 * (1 - k) : 0), ny = cy + 14 * (1 - k);
                    double nz = cz + (k < 1 ? (r.nextDouble() - 0.5) * 1.2 * (1 - k) : 0);
                    f.batch.glowLine(px, py, pz, nx, ny, nz, glow, 3F, false);
                    px = nx; py = ny; pz = nz;
                }
            }
            case BLACK_HOLE -> {
                double rad = t < 0.8 ? 0.15 + 0.5 * Math.sin(t / 0.8 * Math.PI / 2) : 0.65 * (1 - (t - 0.8) / 0.2);
                FxShapes.softDot(f, x, y + 1.1, z, rad, 0x000000, 0.9 * fade + 0.1, false);
                f.batch.glowRing(cx, cy + 1.1, cz, rad * 1.6, 40, FxShapes.argb(0xFFFFFF, 0.5), 2F, false, t * 12, 0.4);
            }
            case FROST -> f.batch.glowRing(cx, cy + 0.04, cz, 0.4 + 1.6 * Math.sqrt(t), 40, FxShapes.argb(0xBFE6FF, 0.7 * fade), 2F, false, 0, 0);
            case ENDER -> f.batch.glowRing(cx, cy + 0.04, cz, 0.9 + 0.2 * Math.sin(t * 20), 36, FxShapes.argb(0xE6E6E6, 0.6 * fade), 2F, false, 0, 0);
            case SOUL_RISE -> f.batch.ring(cx, cy + 0.04, cz, 0.6, 32, FxShapes.argb(0x7FE8FF, 0.6 * fade), 1.6F, false, 0, 0);
            default -> { }
        }
    }

    private static double gauss(ThreadLocalRandom r, double s) {
        return r.nextGaussian() * s;
    }

    private static void sound(ClientLevel level, double x, double y, double z, SoundEvent event, float volume, float pitch) {
        if (volume <= 0) return;
        level.playLocalSound(x, y, z, event, SoundSource.PLAYERS, volume, pitch, false);
    }
}
