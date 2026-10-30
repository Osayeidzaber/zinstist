package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxParticles;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Living entities dissolve into rising texel shards when they die. The dissolve is staggered over a short
 * window on frame time, and the shards are ordinary {@link FxParticles} spawns under this module's own
 * owner id, so disabling the module clears only its own particles.
 */
public final class AfterglowModule extends ClientModule implements FxEmitter {
    private static final int OWNER = FxParticles.ownerId("afterglow");
    private static final int MAX_DISSOLVES = 8;

    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "C9A7FF"));
    private final BooleanSetting players = addSetting(new BooleanSetting("players", "Players", true));
    private final BooleanSetting mobs = addSetting(new BooleanSetting("mobs", "Mobs", true));
    private final NumberSetting count = addSetting(new NumberSetting("count", "Shards per death", 10, 90, 2, 36));
    private final NumberSetting dissolve = addSetting(new NumberSetting("dissolve", "Dissolve time (seconds)", 0.1, 1.2, 0.05, 0.4));
    private final NumberSetting life = addSetting(new NumberSetting("life", "Shard lifetime", 0.4, 2.5, 0.1, 1.3));
    private final NumberSetting rise = addSetting(new NumberSetting("rise", "Rise speed", 0.1, 2, 0.05, 0.85));
    private final NumberSetting spread = addSetting(new NumberSetting("spread", "Spread", 0.05, 1.2, 0.05, 0.35));
    private final NumberSetting range = addSetting(new NumberSetting("range", "Range", 8, 64, 1, 40));
    private final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Glow", true));

    private final Map<Integer, Snapshot> tracked = new HashMap<>();
    private final List<Dissolve> dissolves = new ArrayList<>();
    private final FxParticles.Spec spec = new FxParticles.Spec();
    private final Random random = new Random();

    public AfterglowModule() {
        super("afterglow", "Afterglow", "Entities dissolve into rising texel shards when they die.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        FxParticles.get().clearOwner(OWNER);
        tracked.clear();
        dissolves.clear();
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        if (client.level == null || client.player == null) {
            tracked.clear();
            dissolves.clear();
            return;
        }
        int rgb = EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        double reachSqr = range.value() * range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == client.player) continue;
            if (entity instanceof Player && !players.value()) continue;
            if (!(entity instanceof Player) && !mobs.value()) continue;
            if (entity.distanceToSqr(client.player) > reachSqr) continue;
            int id = entity.getId();
            if (living.isDeadOrDying() || living.isRemoved()) {
                Snapshot last = tracked.remove(id);
                if (last != null) start(last, rgb);
            } else {
                tracked.put(id, new Snapshot(entity.getX(), entity.getY(), entity.getZ(),
                        Math.max(0.3, entity.getBbWidth()), Math.max(0.3, entity.getBbHeight())));
            }
        }
        for (Iterator<Map.Entry<Integer, Snapshot>> iterator = tracked.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Integer, Snapshot> entry = iterator.next();
            if (client.level.getEntity(entry.getKey()) == null) iterator.remove();
        }
        advance(f);
    }

    private void start(Snapshot snapshot, int rgb) {
        if (dissolves.size() >= MAX_DISSOLVES) dissolves.remove(0);
        dissolves.add(new Dissolve(snapshot, (int) Math.round(count.value()), dissolve.value(), rgb));
    }

    private void advance(FxFrame f) {
        double dt = Math.min(0.1, Math.max(0, f.dt));
        for (int i = dissolves.size() - 1; i >= 0; i--) {
            Dissolve current = dissolves.get(i);
            current.elapsed += dt;
            int target = (int) Math.min(current.total, Math.round(current.total * current.elapsed / current.duration));
            while (current.spawned < target) {
                spawn(current);
                current.spawned++;
            }
            if (current.spawned >= current.total) dissolves.remove(i);
        }
    }

    private void spawn(Dissolve dissolve) {
        Snapshot snapshot = dissolve.snapshot;
        spec.reset();
        spec.x = snapshot.x + (random.nextDouble() - 0.5) * snapshot.width;
        spec.y = snapshot.y + random.nextDouble() * snapshot.height;
        spec.z = snapshot.z + (random.nextDouble() - 0.5) * snapshot.width;
        spec.vx = (random.nextDouble() - 0.5) * spread.value();
        spec.vz = (random.nextDouble() - 0.5) * spread.value();
        spec.vy = rise.value() * (0.45 + random.nextDouble() * 0.75);
        spec.gravity = -0.18;
        spec.drag = 0.86;
        spec.life = life.value() * (0.55 + random.nextDouble() * 0.65);
        spec.size0 = 0.05 + random.nextDouble() * 0.09;
        spec.size1 = 0.02;
        spec.rot = random.nextDouble() * Math.PI;
        spec.spin = (random.nextDouble() - 0.5) * 1.6;
        spec.color = dissolve.rgb;
        spec.color1 = EffectBatch.mixRgb(dissolve.rgb, 0xFFFFFF, 0.45);
        spec.alpha = 0.75 + random.nextDouble() * 0.25;
        spec.shape = FxParticles.Shape.CRYSTAL;
        spec.glow = glow.value();
        spec.owner = OWNER;
        FxParticles.get().spawn(spec);
    }

    private record Snapshot(double x, double y, double z, double width, double height) { }

    private static final class Dissolve {
        private final Snapshot snapshot;
        private final int total;
        private final double duration;
        private final int rgb;
        private double elapsed;
        private int spawned;

        private Dissolve(Snapshot snapshot, int total, double duration, int rgb) {
            this.snapshot = snapshot;
            this.total = Math.max(1, total);
            this.duration = Math.max(0.05, duration);
            this.rgb = rgb;
        }
    }
}
