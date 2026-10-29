package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.MovementInputEvent;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;

public final class TargetStrafeModule extends ClientModule {
    private final NumberSetting radius;
    private final NumberSetting followRange;
    private final NumberSetting strength;
    private final BooleanSetting requireAuraTarget;
    private final BooleanSetting requireMovementInput;
    private final BooleanSetting requireGround;
    private int direction = 1;
    private boolean jumpWasDown;

    public TargetStrafeModule() {
        super("target_strafe", "TargetStrafe", "Orbits a valid combat target while correcting toward a chosen radius.",
                ModuleCategory.MOVEMENT);
        radius = addSetting(new NumberSetting("radius", "Orbit radius", 0.8, 6.0, 0.05, 2.95));
        followRange = addSetting(new NumberSetting("follow_range", "Maximum target distance", 1.0, 8.0, 0.1, 4.0));
        strength = addSetting(new NumberSetting("strength", "Movement strength", 0.1, 1.0, 0.05, 1.0));
        requireAuraTarget = addSetting(new BooleanSetting("require_kill_aura", "Require KillAura target", true));
        requireMovementInput = addSetting(new BooleanSetting("require_movement", "Require movement key", true));
        requireGround = addSetting(new BooleanSetting("require_ground", "Ground only", false));
    }

    @Override
    protected void onEnable(Minecraft client) {
        direction = 1;
        jumpWasDown = false;
    }

    @Override
    protected void onDisable(Minecraft client) {
        jumpWasDown = false;
    }

    @Override
    protected void onMovementInput(MovementInputEvent event) {
        Minecraft client = Minecraft.getInstance();
        Input input = event.input();
        if (input.jump() && !jumpWasDown) {
            direction = -direction;
        }
        jumpWasDown = input.jump();
        if (input.left() != input.right()) {
            direction = input.left() ? -1 : 1;
        }

        Player player = client.player;
        if (player == null || client.level == null || client.screen != null
                || (requireGround.value() && !player.onGround())
                || (requireMovementInput.value() && !hasMovementInput(input))
                || isSprintTapActive(client)) {
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
}
