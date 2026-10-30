package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.MovementInputEvent;
import dev.jilaton.client.module.PlayerMoveEvent;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class TargetStrafeModule extends ClientModule {
    private final ChoiceSetting mode;
    private final NumberSetting radius;
    private final NumberSetting followRange;
    private final NumberSetting strength;
    private final BooleanSetting requireAuraTarget;
    private final BooleanSetting requireMovementInput;
    private final BooleanSetting requireGround;
    private final BooleanSetting collisionCheck;
    private final BooleanSetting edgeCheck;
    private int direction = 1;
    private boolean jumpWasDown;
    private Input lastInput;

    public TargetStrafeModule() {
        super("target_strafe", "TargetStrafe", "Orbits a valid combat target while correcting toward a chosen radius.",
                ModuleCategory.MOVEMENT);
        mode = addSetting(new ChoiceSetting("mode", "Strafe mode", List.of("Input", "Motion"), "Input"));
        radius = addSetting(new NumberSetting("radius", "Orbit radius", 0.8, 6.0, 0.05, 2.95));
        followRange = addSetting(new NumberSetting("follow_range", "Maximum target distance", 1.0, 8.0, 0.1, 4.0));
        strength = addSetting(new NumberSetting("strength", "Movement strength", 0.1, 1.0, 0.05, 1.0));
        requireAuraTarget = addSetting(new BooleanSetting("require_kill_aura", "Require KillAura target", true));
        requireMovementInput = addSetting(new BooleanSetting("require_movement", "Require movement key", true));
        requireGround = addSetting(new BooleanSetting("require_ground", "Ground only", false));
        collisionCheck = addSetting(new BooleanSetting("collision_check", "Avoid solid obstacles", true));
        edgeCheck = addSetting(new BooleanSetting("edge_check", "Avoid unsupported ground", true));
    }

    @Override
    protected void onEnable(Minecraft client) {
        direction = 1;
        jumpWasDown = false;
        lastInput = null;
    }

    @Override
    protected void onDisable(Minecraft client) {
        jumpWasDown = false;
        lastInput = null;
    }

    @Override
    protected void onMovementInput(MovementInputEvent event) {
        Minecraft client = Minecraft.getInstance();
        Input input = event.input();
        lastInput = input;
        if (input.jump() && !jumpWasDown) {
            direction = -direction;
        }
        jumpWasDown = input.jump();
        if (input.left() != input.right()) {
            direction = input.left() ? -1 : 1;
        }

        Player player = client.player;
        if (!eligible(client, player, input)) {
            return;
        }

        if (mode.value().equals("Motion")) {
            return;
        }

        LivingEntity target = findTarget(client, player);
        if (target == null || !target.isAlive() || target.isSpectator()) {
            return;
        }

        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double distance = Math.hypot(dx, dz);
        if (distance < 1.0E-5 || distance > followRange.value()) {
            return;
        }

        Vec2 movement = TargetStrafeMath.movementInput(player.getX(), player.getZ(), target.getX(), target.getZ(),
                player.getYRot(), radius.value(), direction, strength.value());
        if (movement.x * movement.x + movement.y * movement.y > 1.0E-6F) {
            event.setMoveVectorOverride(movement);
        }
    }

    @Override
    protected void onPlayerMove(PlayerMoveEvent event) {
        if (!mode.value().equals("Motion") || event.type() != MoverType.SELF) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (!eligible(client, player, lastInput) || player.isPassenger() || player.getAbilities().flying
                || player.isFallFlying() || player.isInWater() || player.isInLava()) {
            return;
        }

        LivingEntity target = findTarget(client, player);
        if (target == null || !target.isAlive() || target.isSpectator()) {
            return;
        }
        if (Math.hypot(target.getX() - player.getX(), target.getZ() - player.getZ()) > followRange.value()) {
            return;
        }

        Vec3 movement = event.movement();
        double speed = Math.hypot(player.getDeltaMovement().x, player.getDeltaMovement().z);
        if (speed < 1.0E-5 || !Double.isFinite(speed)) {
            speed = Math.hypot(movement.x, movement.z);
        }
        if (speed < 1.0E-5 || !Double.isFinite(speed)) {
            return;
        }

        Vec3 desired = motionPlan(player, target, speed, direction);
        if (desired.equals(Vec3.ZERO)) {
            return;
        }
        if (!isSafeMotion(client, player, desired)) {
            direction = -direction;
            desired = motionPlan(player, target, speed, direction);
            if (desired.equals(Vec3.ZERO) || !isSafeMotion(client, player, desired)) {
                return;
            }
        }

        event.setMovement(new Vec3(desired.x, movement.y, desired.z));
    }

    private boolean eligible(Minecraft client, Player player, Input input) {
        return player != null && client.level != null && client.screen == null && input != null
                && (!requireGround.value() || player.onGround())
                && (!requireMovementInput.value() || hasMovementInput(input))
                && !isSprintTapActive(client);
    }

    private Vec3 motionPlan(Player player, LivingEntity target, double speed, int side) {
        return TargetStrafeMath.motionVector(player.getX(), player.getZ(), target.getX(), target.getZ(),
                radius.value(), side, speed, strength.value());
    }

    private boolean isSafeMotion(Minecraft client, Player player, Vec3 movement) {
        if (!Double.isFinite(movement.x) || !Double.isFinite(movement.z)) {
            return false;
        }
        if (collisionCheck.value()) {
            AABB nextBox = player.getBoundingBox().move(movement.x, 0.0, movement.z);
            if (!client.level.noCollision(player, nextBox)) {
                return false;
            }
        }
        return !edgeCheck.value() || !player.onGround() || hasSupportAtNextStep(client, player, movement);
    }

    private static boolean hasSupportAtNextStep(Minecraft client, Player player, Vec3 movement) {
        double halfWidth = Math.max(0.05, player.getBbWidth() * 0.45);
        double x = player.getX() + movement.x;
        double z = player.getZ() + movement.z;
        double footY = player.getY() - 0.05;
        return hasSupport(client, x - halfWidth, footY, z - halfWidth)
                && hasSupport(client, x - halfWidth, footY, z + halfWidth)
                && hasSupport(client, x + halfWidth, footY, z - halfWidth)
                && hasSupport(client, x + halfWidth, footY, z + halfWidth);
    }

    private static boolean hasSupport(Minecraft client, double x, double y, double z) {
        BlockPos position = BlockPos.containing(x, y, z);
        BlockState block = client.level.getBlockState(position);
        return !block.getCollisionShape(client.level, position).isEmpty();
    }

    private LivingEntity findTarget(Minecraft client, Player player) {
        ClientModule module = JilatonClient.moduleManager().get("kill_aura");
        if (module instanceof KillAuraModule killAura && killAura.enabled() && killAura.currentTarget() != null) {
            return killAura.currentTarget();
        }
        if (requireAuraTarget.value() || client.level == null) {
            return null;
        }

        double maxDistanceSqr = followRange.value() * followRange.value();
        LivingEntity closest = null;
        double closestDistanceSqr = maxDistanceSqr;
        for (LivingEntity candidate : client.level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(followRange.value()), entity -> entity != player && entity.isAlive()
                        && !entity.isSpectator() && entity instanceof Player
                        && PlayerTargetFilters.allowsPlayer(client, player, (Player) entity)
                        && player.hasLineOfSight(entity))) {
            double distanceSqr = player.distanceToSqr(candidate);
            if (distanceSqr < closestDistanceSqr) {
                closest = candidate;
                closestDistanceSqr = distanceSqr;
            }
        }
        return closest;
    }

    private static boolean hasMovementInput(Input input) {
        return input.forward() || input.backward() || input.left() || input.right();
    }

    private static boolean isSprintTapActive(Minecraft client) {
        ClientModule module = JilatonClient.moduleManager().get("wtap");
        return module instanceof WTapModule wTap && wTap.enabled() && wTap.isTapActive(client);
    }

    public NumberSetting radius() {
        return radius;
    }

    public ChoiceSetting mode() {
        return mode;
    }

    public NumberSetting followRange() {
        return followRange;
    }

    public NumberSetting strength() {
        return strength;
    }

    public BooleanSetting requireAuraTarget() {
        return requireAuraTarget;
    }

    public BooleanSetting requireMovementInput() {
        return requireMovementInput;
    }

    public BooleanSetting requireGround() {
        return requireGround;
    }

    public BooleanSetting collisionCheck() {
        return collisionCheck;
    }

    public BooleanSetting edgeCheck() {
        return edgeCheck;
    }
}
