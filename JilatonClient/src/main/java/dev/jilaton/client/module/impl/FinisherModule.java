package dev.jilaton.client.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.CombatTargetTracker;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.character.FinisherFigure;
import dev.jilaton.client.render.finisher.FinisherChoreography;
import dev.jilaton.client.render.finisher.FinisherChoreography.Variant;
import dev.jilaton.client.render.finisher.FinisherGate;
import dev.jilaton.client.render.finisher.FinisherPlacement;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cinematic finishers: when something you were fighting dies, one of the 3D characters materialises beside the
 * body, performs a finishing move timed to land on it, and dissolves. Triggered only by the server's death event
 * (or the target visibly dying) for an entity you hit in the last few seconds, once per entity. Purely visual:
 * no packets, no input, no effect on combat.
 */
public final class FinisherModule extends ClientModule implements FxEmitter {
    private static final int MAX_ACTIVE = 3;
    private static final double MAX_DISTANCE_SQR = 64 * 64;
    private static final double ATTACK_WINDOW_SECONDS = 4.0;

    private final ChoiceSetting move = addSetting(new ChoiceSetting("move", "Finishing move", FinisherChoreography.LABELS, "Ground slam"));
    private final ChoiceSetting character = addSetting(new ChoiceSetting("character", "Character",
            FinisherFigure.characters(), FinisherFigure.characters().get(0)));
    private final ChoiceSetting targets = addSetting(new ChoiceSetting("targets", "Targets",
            List.of("Players and mobs", "Players", "Hostile mobs"), "Players and mobs"));
    private final NumberSetting speed = addSetting(new NumberSetting("speed", "Speed", 0.6, 1.6, 0.05, 1.0));
    private final BooleanSetting particles = addSetting(new BooleanSetting("particles", "Impact particles", true));
    private final BooleanSetting shockwave = addSetting(new BooleanSetting("shockwave", "Shockwave rings", true));
    private final BooleanSetting sounds = addSetting(new BooleanSetting("sounds", "Sounds", true));
    private final NumberSetting volume = addSetting(new NumberSetting("volume", "Volume", 0, 1, 0.05, 0.7));
    private final BooleanSetting cameraShake = addSetting(new BooleanSetting("camera_shake", "Camera shake", true));
    private final NumberSetting shakeStrength = addSetting(new NumberSetting("shake_strength", "Shake strength", 0, 1, 0.05, 0.45));

    private final List<Finisher> active = new ArrayList<>();
    private final FinisherGate gate = new FinisherGate();
    private final float[] pose = new float[FinisherChoreography.SPIN + 1];
    private ClientLevel boundLevel;
    private String prepared;
    private double shake;

    public FinisherModule() {
        super("finisher", "Finisher", "A 3D character performs a finishing move on what you defeat.", ModuleCategory.RENDER);
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (JilatonClient.moduleManager() != null
                    && JilatonClient.moduleManager().get("finisher") instanceof FinisherModule module && module.enabled()) {
                module.render(context);
            }
        });
    }

    /** Death entity event (id 3) from the packet hook. */
    public static void onDeath(LivingEntity entity) {
        if (JilatonClient.moduleManager() != null
                && JilatonClient.moduleManager().get("finisher") instanceof FinisherModule module && module.enabled()) {
            module.consider(entity);
        }
    }

    /** Camera offset for this frame's hit kick, in blocks; zero when nothing is landing. */
    public static Vec3 cameraShake() {
        if (JilatonClient.moduleManager() == null
                || !(JilatonClient.moduleManager().get("finisher") instanceof FinisherModule module) || !module.enabled()
                || module.shake <= 0.0005) return null;
        double t = System.nanoTime() / 1e9;
        double k = module.shake;
        return new Vec3(Math.sin(t * 71) * k, Math.sin(t * 89 + 1.3) * k * 0.7, Math.sin(t * 63 + 2.1) * k);
    }

    @Override
    protected void onEnable(Minecraft client) {
        prepared = null;
    }

    @Override
    protected void onDisable(Minecraft client) {
        active.clear();
        gate.clear();
        shake = 0;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.level != boundLevel) {
            boundLevel = client.level;
            active.clear();
            gate.clear();
        }
        if (!character.value().equals(prepared)) {
            prepared = character.value();
            FinisherFigure.prepare(prepared);
        }
        // Fallback for servers that don't send the death event: the target you were hitting visibly dying.
        LivingEntity last = CombatTargetTracker.lastTarget();
        if (last != null && last.isDeadOrDying() && last.level() == client.level) consider(last);
    }

    private void consider(LivingEntity entity) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || entity == player || client.level == null || entity.level() != client.level) return;
        if (!eligible(entity)) return;
        long now = System.nanoTime();
        long since = CombatTargetTracker.sinceAttack(entity, now);
        if (since < 0 || since > ATTACK_WINDOW_SECONDS * 1e9) return;
        if (!gate.admit(entity.getId(), now)) return;
        if (player.distanceToSqr(entity) > MAX_DISTANCE_SQR) return;
        if (active.size() >= MAX_ACTIVE) active.remove(0);
        active.add(start(client.level, player, entity, now));
    }

    private boolean eligible(LivingEntity entity) {
        return switch (targets.value()) {
            case "Players" -> entity instanceof Player;
            case "Hostile mobs" -> entity instanceof Enemy;
            default -> entity instanceof Player || entity instanceof Enemy
                    || entity instanceof net.minecraft.world.entity.Mob;
        };
    }

    private Finisher start(ClientLevel level, Player player, LivingEntity entity, long now) {
        Variant variant = move.value().equals("Random")
                ? Variant.values()[ThreadLocalRandom.current().nextInt(Variant.values().length)]
                : Variant.byLabel(move.value());
        Finisher f = new Finisher(entity, variant, now);
        f.corpseX = entity.getX();
        f.corpseY = entity.getY();
        f.corpseZ = entity.getZ();
        f.reach = Math.max(0.9, entity.getBbWidth() * 0.5 + 0.75);
        double[] side = FinisherPlacement.sides(player.getX(), player.getZ(), f.corpseX, f.corpseZ);
        // Try beside the body on the side facing you, then the other side, then behind it.
        for (int i = 0; i < side.length; i += 2) {
            double sx = f.corpseX + side[i] * f.reach, sz = f.corpseZ + side[i + 1] * f.reach;
            double ground = groundAt(level, sx, f.corpseY, sz);
            if (!Double.isNaN(ground)) {
                f.offsetX = side[i] * f.reach;
                f.offsetZ = side[i + 1] * f.reach;
                f.x = sx;
                f.z = sz;
                f.y = ground;
                break;
            }
        }
        if (Double.isNaN(f.y)) {
            f.offsetX = side[0] * f.reach;
            f.offsetZ = side[1] * f.reach;
            f.x = f.corpseX + f.offsetX;
            f.z = f.corpseZ + f.offsetZ;
            f.y = f.corpseY;
        }
        if (sounds.value()) {
            sound(level, f.x, f.y + 1, f.z, SoundEvents.ENDERMAN_TELEPORT, 0.35F, 1.55F);
        }
        if (particles.value()) {
            for (int i = 0; i < 14; i++) {
                double a = i * Math.PI * 2 / 14;
                level.addParticle(ParticleTypes.END_ROD, f.x + Math.cos(a) * 0.45, f.y + 0.1 + (i % 3) * 0.5,
                        f.z + Math.sin(a) * 0.45, 0, 0.04, 0);
            }
        }
        return f;
    }

    /**
     * Top of the floor under ({@code x}, {@code z}) within a block above or three below {@code y}, with two
     * free blocks over it so the character neither floats nor stands inside a wall; NaN when there is none.
     */
    private static double groundAt(ClientLevel level, double x, double y, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        int top = (int) Math.floor(y + 1.0);
        for (int by = top; by >= top - 4; by--) {
            BlockPos below = new BlockPos(bx, by - 1, bz);
            VoxelShape floor = level.getBlockState(below).getCollisionShape(level, below);
            if (floor.isEmpty()) continue;
            double surface = below.getY() + floor.max(Direction.Axis.Y);
            if (free(level, bx, by, bz) && free(level, bx, by + 1, bz)) return Math.max(surface, below.getY() + 0.0);
            return Double.NaN;
        }
        return Double.NaN;
    }

    private static boolean free(ClientLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private void render(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || active.isEmpty() || context.consumers() == null || context.matrices() == null) return;
        Vec3 camera = client.gameRenderer.getMainCamera().position();
        PoseStack stack = context.matrices();
        long now = System.nanoTime();
        double peak = 0;
        for (int i = active.size() - 1; i >= 0; i--) {
            Finisher f = active.get(i);
            float t = (float) ((now - f.started) / 1e9 * speed.value());
            if (t >= f.variant.length) {
                active.remove(i);
                continue;
            }
            follow(client.level, f, now);
            FinisherChoreography.sample(f.variant, t, pose);
            // Step in toward the body along the line to it, but never past it.
            double dx = f.corpseX - f.x, dz = f.corpseZ - f.z;
            double dist = Math.max(1e-4, Math.hypot(dx, dz));
            double step = Math.min(pose[FinisherChoreography.FORWARD], Math.max(0, dist - 0.4));
            double px = f.x + dx / dist * step, pz = f.z + dz / dist * step;
            double py = f.y + Math.max(0, pose[FinisherChoreography.RISE]);
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)) + (float) Math.toDegrees(pose[FinisherChoreography.SPIN]);
            float alpha = FinisherChoreography.alpha(f.variant, t);
            int light = LevelRenderer.getLightColor(client.level, BlockPos.containing(px, py + 0.5, pz));
            FinisherFigure.draw(stack, context.consumers(), character.value(),
                    px - camera.x, py - camera.y, pz - camera.z, yaw, pose,
                    FinisherChoreography.scale(f.variant, t), alpha, light);
            if (!f.landed && t >= f.variant.impact) {
                f.landed = true;
                impact(client.level, f, px, pz);
            }
            peak = Math.max(peak, FinisherChoreography.shake(f.variant, t)
                    * Math.max(0, 1 - Math.sqrt(client.player == null ? 0 : client.player.distanceToSqr(px, py, pz)) / 24));
        }
        shake = cameraShake.value() && !JilatonClient.clientConfig().reducedMotion() ? peak * 0.11 * shakeStrength.value() : 0;
    }

    /** Keeps the stand spot tied to the body while it is still there (death knockback slides it), eased. */
    private static void follow(ClientLevel level, Finisher f, long now) {
        double dt = f.lastNanos == 0 ? 0 : Math.min(0.1, (now - f.lastNanos) / 1e9);
        f.lastNanos = now;
        LivingEntity body = f.entity;
        if (body != null && !body.isRemoved() && body.level() == level) {
            f.corpseX = body.getX();
            f.corpseY = body.getY();
            f.corpseZ = body.getZ();
        }
        double tx = f.corpseX + f.offsetX, tz = f.corpseZ + f.offsetZ;
        double ground = groundAt(level, tx, f.corpseY, tz);
        double ty = Double.isNaN(ground) ? f.y : ground;
        double k = 1 - Math.exp(-dt * 9);
        f.x += (tx - f.x) * k;
        f.z += (tz - f.z) * k;
        f.y += (ty - f.y) * k;
    }

    private void impact(ClientLevel level, Finisher f, double x, double z) {
        f.impactAt = System.nanoTime();
        f.impactX = f.corpseX;
        f.impactY = f.y;
        f.impactZ = f.corpseZ;
        float v = (float) volume.value();
        if (sounds.value() && v > 0) {
            switch (f.variant) {
                case GROUND_SLAM -> {
                    sound(level, f.impactX, f.impactY, f.impactZ, SoundEvents.MACE_SMASH_GROUND_HEAVY, v, 0.9F);
                    sound(level, f.impactX, f.impactY, f.impactZ, SoundEvents.GENERIC_EXPLODE.value(), v * 0.35F, 1.6F);
                }
                case RISING_UPPERCUT -> {
                    sound(level, x, f.impactY + 1, z, SoundEvents.PLAYER_ATTACK_STRONG, v, 0.8F);
                    sound(level, x, f.impactY + 1, z, SoundEvents.MACE_SMASH_AIR, v * 0.8F, 1.1F);
                }
                case SPIN_KICK -> {
                    sound(level, x, f.impactY + 1, z, SoundEvents.PLAYER_ATTACK_SWEEP, v, 0.85F);
                    sound(level, f.impactX, f.impactY, f.impactZ, SoundEvents.MACE_SMASH_GROUND, v * 0.8F, 1.0F);
                }
            }
        }
        if (!particles.value()) return;
        BlockPos below = BlockPos.containing(f.impactX, f.impactY - 0.5, f.impactZ);
        BlockState ground = level.getBlockState(below);
        int dust = f.variant == Variant.GROUND_SLAM ? 36 : 18;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < dust; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = 0.2 + random.nextDouble() * 1.1;
            double px = f.impactX + Math.cos(a) * r, pz = f.impactZ + Math.sin(a) * r;
            if (!ground.isAir()) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), px, f.impactY + 0.1, pz,
                        Math.cos(a) * 0.25, 0.25 + random.nextDouble() * 0.3, Math.sin(a) * 0.25);
            }
        }
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16;
            level.addParticle(ParticleTypes.CRIT, f.impactX, f.impactY + 0.6, f.impactZ,
                    Math.cos(a) * 0.55, 0.15 + random.nextDouble() * 0.35, Math.sin(a) * 0.55);
        }
        level.addParticle(f.variant == Variant.GROUND_SLAM ? ParticleTypes.EXPLOSION : ParticleTypes.SWEEP_ATTACK,
                f.impactX, f.impactY + (f.variant == Variant.GROUND_SLAM ? 0.3 : 1.0), f.impactZ, 0, 0, 0);
        for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.CLOUD, f.impactX, f.impactY + 0.1, f.impactZ,
                    (random.nextDouble() - 0.5) * 0.3, 0.02, (random.nextDouble() - 0.5) * 0.3);
        }
    }

    private static void sound(ClientLevel level, double x, double y, double z, SoundEvent event, float volume, float pitch) {
        level.playLocalSound(x, y, z, event, SoundSource.PLAYERS, volume, pitch, false);
    }

    /** Ground shockwave rings and radial cracks after the hit, plus a faint ring where the character appears. */
    @Override
    public void emit(FxFrame f) {
        if (active.isEmpty() || !shockwave.value()) return;
        for (Finisher fin : active) {
            if (f.distanceSqr(fin.x, fin.y, fin.z) > MAX_DISTANCE_SQR) continue;
            float t = (float) ((f.now - fin.started) / 1e9 * speed.value());
            if (t < FinisherChoreography.APPEAR * 1.6F) {
                float k = t / (FinisherChoreography.APPEAR * 1.6F);
                f.batch.ring(fin.x - f.camX, fin.y + 0.03 - f.camY, fin.z - f.camZ, 0.25 + 0.5 * k, 32,
                        FxShapes.argb(0xFFFFFF, 0.55 * (1 - k)), 1.6F, false, 0, 0);
            }
            if (fin.impactAt == 0) continue;
            float since = (float) ((f.now - fin.impactAt) / 1e9);
            if (since > 0.9F) continue;
            float k = since / 0.9F;
            float open = 1F - (1F - k) * (1F - k) * (1F - k);
            double cx = fin.impactX - f.camX, cy = fin.impactY + 0.03 - f.camY, cz = fin.impactZ - f.camZ;
            double fade = 1 - k;
            int light = EffectBatch.mixRgb(f.accent, 0xFFFFFF, 0.7);
            f.batch.glowRing(cx, cy, cz, 0.4 + 2.6 * open, 48, FxShapes.argb(light, 0.8 * fade), 2.4F, false, 0, 0);
            f.batch.ring(cx, cy, cz, 0.2 + 1.6 * open, 40, FxShapes.argb(0xFFFFFF, 0.5 * fade), 1.4F, false, 0, 0);
            if (fin.variant == Variant.GROUND_SLAM) {
                for (int i = 0; i < 9; i++) {
                    double a = i * 0.698 + fin.started % 7;
                    double r = (0.5 + 0.9 * open) * (0.7 + 0.3 * ((i * 37) % 10) / 10.0);
                    f.batch.line(cx, cy, cz, cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r,
                            FxShapes.argb(0x1A1A1A, 0.75 * fade), 2.2F, false);
                }
            }
        }
    }

    private static final class Finisher {
        final LivingEntity entity;
        final Variant variant;
        final long started;
        double corpseX, corpseY, corpseZ, offsetX, offsetZ, reach;
        double x, y = Double.NaN, z;
        double impactX, impactY, impactZ;
        long impactAt, lastNanos;
        boolean landed;

        Finisher(LivingEntity entity, Variant variant, long started) {
            this.entity = entity;
            this.variant = variant;
            this.started = started;
        }
    }
}
