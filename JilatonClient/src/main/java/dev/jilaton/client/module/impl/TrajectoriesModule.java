package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Predicts where the projectile you are holding will land: bows (by current draw), crossbows, tridents,
 * snowballs, eggs, ender pearls and splash or lingering potions. Draws the arc and a ring where it lands, and
 * marks the entity it would hit. Uses the vanilla per-projectile speed, gravity and drag.
 */
public final class TrajectoriesModule extends ClientModule implements FxEmitter {
    private static final int MAX_TICKS = 240;

    private final BooleanSetting entities = addSetting(new BooleanSetting("entities", "Show entity hits", true));
    private final BooleanSetting pearls = addSetting(new BooleanSetting("pearls", "Ender pearls", true));
    private final ChoiceSetting palette = addSetting(EffectPalette.modeSetting("palette", "Color"));
    private final TextSetting custom = addSetting(EffectPalette.hexSetting("custom", "Custom color", "F2F2F2"));

    public TrajectoriesModule() {
        super("trajectories", "Trajectories", "Shows where arrows, tridents, pearls and throwables will land.", ModuleCategory.RENDER);
    }

    private TrajectoryPhysics.Kind kind(LocalPlayer player, ItemStack stack) {
        if (stack.getItem() instanceof BowItem) {
            if (!player.isUsingItem()) return null;
            float power = TrajectoryPhysics.bowPower(stack.getUseDuration(player) - player.getUseItemRemainingTicks());
            if (power < 0.1F) return null;
            return new TrajectoryPhysics.Kind(TrajectoryPhysics.ARROW.speed() * power, 0.05, 0.99);
        }
        if (stack.getItem() instanceof CrossbowItem) return CrossbowItem.isCharged(stack) ? TrajectoryPhysics.CROSSBOW : null;
        if (stack.is(Items.TRIDENT)) return player.isUsingItem() ? TrajectoryPhysics.TRIDENT : null;
        if (stack.is(Items.ENDER_PEARL)) return pearls.value() ? TrajectoryPhysics.THROWN : null;
        if (stack.is(Items.SNOWBALL) || stack.is(Items.EGG)) return TrajectoryPhysics.THROWN;
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) return TrajectoryPhysics.POTION;
        return null;
    }

    @Override
    public void emit(FxFrame f) {
        LocalPlayer player = f.client.player;
        if (player == null || f.client.level == null) return;
        TrajectoryPhysics.Kind kind = kind(player, player.getMainHandItem());
        if (kind == null) kind = kind(player, player.getOffhandItem());
        if (kind == null) return;
        Vec3 eye = player.getEyePosition(f.partial);
        float yaw = player.getViewYRot(f.partial), pitch = player.getViewXRot(f.partial);
        // Potions are thrown 20 degrees higher, as ThrowablePotionItem does.
        if (kind == TrajectoryPhysics.POTION) pitch -= 20;
        double[] pos = {eye.x, eye.y - 0.1, eye.z};
        double[] vel = TrajectoryPhysics.launch(yaw, pitch, kind.speed());
        Vec3 motion = player.getDeltaMovement();
        vel[0] += motion.x;
        vel[2] += motion.z;
        if (!player.onGround()) vel[1] += motion.y;

        int color = EffectPalette.resolve(palette.value(), f.accent, custom.value());
        Entity hitEntity = null;
        Vec3 land = null;
        for (int tick = 0; tick < MAX_TICKS; tick++) {
            Vec3 from = new Vec3(pos[0], pos[1], pos[2]);
            boolean water = !f.client.level.getFluidState(net.minecraft.core.BlockPos.containing(from)).isEmpty();
            TrajectoryPhysics.step(pos, vel, kind, water);
            Vec3 to = new Vec3(pos[0], pos[1], pos[2]);
            BlockHitResult block = f.client.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
            if (entities.value() && tick > 1) {
                AABB sweep = new AABB(from, end).inflate(0.4);
                for (Entity e : f.client.level.getEntities(player, sweep, e -> e.isPickable() && !e.isSpectator())) {
                    Optional<Vec3> clip = e.getBoundingBox().inflate(0.3).clip(from, end);
                    if (clip.isPresent()) {
                        hitEntity = e;
                        end = clip.get();
                        break;
                    }
                }
            }
            double fade = 0.25 + 0.65 * (1 - tick / (double) MAX_TICKS);
            f.batch.line(from.x - f.camX, from.y - f.camY, from.z - f.camZ, end.x - f.camX, end.y - f.camY, end.z - f.camZ,
                    FxShapes.argb(color, fade), 1.6F, false);
            if (hitEntity != null || block.getType() != HitResult.Type.MISS || pos[1] < f.client.level.getMinY()) {
                land = end;
                break;
            }
        }
        if (land == null) return;
        if (hitEntity != null) {
            AABB box = hitEntity.getBoundingBox();
            FxShapes.softDot(f, land.x, land.y, land.z, 0.3, 0xFF5050, 0.6, true);
            f.batch.ring(box.getCenter().x - f.camX, box.minY + 0.02 - f.camY, box.getCenter().z - f.camZ,
                    Math.max(box.getXsize(), box.getZsize()) * 0.75, 32, FxShapes.argb(0xFF5050, 0.9), 2F, true, 0, 0);
        } else {
            f.batch.ring(land.x - f.camX, land.y + 0.03 - f.camY, land.z - f.camZ, 0.35, 28, FxShapes.argb(color, 0.95), 2F, false, 0, 0);
            FxShapes.softDot(f, land.x, land.y + 0.03, land.z, 0.12, color, 0.8, false);
        }
    }
}
