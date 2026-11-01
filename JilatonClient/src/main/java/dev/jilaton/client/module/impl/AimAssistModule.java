/*
 * Mouse nudge, target selection and range/FOV hysteresis adapted from the supplied Anubis 0.9.9.3
 * AimAssistModule decompilation, Horcus / 4ldenz, GPL-3.0-only. See ATTRIBUTION.md.
 */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.rotation.RotationPriorities;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.render.CombatTargetTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AimAssistModule extends ClientModule {
    private static final double STICK_RANGE = 0.5;
    private static final double STICK_FOV_MULTIPLIER = 1.3;
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting turnSpeed;
    private final NumberSetting angleFactor;
    private final BooleanSetting horizontal;
    private final BooleanSetting vertical;
    private final BooleanSetting stickyTarget;
    private Player currentTarget;
    private final ChoiceSetting mode = addSetting(new ChoiceSetting("mode", "Assist mode", List.of("Attack assist", "Mouse assist"), "Attack assist"));
    private final BooleanSetting weaponsOnly = addSetting(new BooleanSetting("weapons_only", "Mouse: weapons only", true));
    private final NumberSetting mouseSpeed = addSetting(new NumberSetting("mouse_speed", "Mouse assist speed", 10, 180, 5, 60));
    private final MouseAimState mouse = new MouseAimState();
    private double pace = 1;
    private String lastMode = "Attack assist";

    public AimAssistModule() {
        super("aim_assist", "AimAssist", "Attack-held or mouse-driven assistance toward a visible nearby player.", ModuleCategory.COMBAT);
        range = addSetting(new NumberSetting("range", "Range", 2.0, 6.0, 0.1, 4.2));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view", 5.0, 180.0, 1.0, 60.0));
        turnSpeed = addSetting(new NumberSetting("turn_speed", "Turn speed", 1.0, 18.0, 0.5, 6.0));
        angleFactor = addSetting(new NumberSetting("angle_factor", "Angle factor", 0.05, 1.0, 0.05, 0.35));
        horizontal = addSetting(new BooleanSetting("horizontal", "Horizontal aim", true));
        vertical = addSetting(new BooleanSetting("vertical", "Vertical aim", true));
        stickyTarget = addSetting(new BooleanSetting("sticky_target", "Keep current target", true));
    }

    @Override
    protected void onDisable(Minecraft client) {
        currentTarget = null;
        mouse.reset();
    }

    @Override
    protected void onTick(Minecraft client) {
        if (!mode.value().equals(lastMode)) {
            lastMode = mode.value(); currentTarget = null; mouse.reset();
        }
        ClientModule potionModule = JilatonClient.moduleManager().get("auto_pot");
        if (potionModule instanceof AutoPotModule autoPot && autoPot.enabled() && autoPot.isPreparingThrow()) {
            followMouse(null);
            return;
        }
        if (client == null || client.player == null || client.level == null || client.screen != null
                || !client.isWindowActive() || !client.player.isAlive()) {
            followMouse(null);
            return;
        }

        if (mode.value().equals("Mouse assist")) {
            var held = client.player.getMainHandItem();
            boolean weapon = held.is(ItemTags.SWORDS) || held.is(ItemTags.AXES) || held.is(ItemTags.SPEARS)
                    || held.is(Items.MACE) || held.is(Items.TRIDENT);
            if (client.player.isUsingItem() || client.options.keyUse.isDown() || (weaponsOnly.value() && !weapon)) {
                followMouse(null); return;
            }
            followMouse(chooseMouseTarget(client));
            return;
        }
        mouse.reset();
        if (!client.options.keyAttack.isDown()) { currentTarget = null; return; }

        Player player = client.player;
        if (stickyTarget.value() && isValidTarget(client, player, currentTarget,
                range.value() + STICK_RANGE, fieldOfView.value() * STICK_FOV_MULTIPLIER)) {
            rotateToward(player, currentTarget);
            return;
        }

        currentTarget = null;
        double bestAngle = Double.POSITIVE_INFINITY;
        Player bestTarget = null;

        for (Player candidate : client.level.players()) {
            if (!isValidTarget(client, player, candidate, range.value(), fieldOfView.value())) {
                continue;
            }

            Vec3 aimPoint = candidate.position().add(0.0, candidate.getBbHeight() * 0.55, 0.0);
            Vec3 difference = aimPoint.subtract(player.getEyePosition());
            double horizontal = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
            float targetYaw = (float) (Math.atan2(difference.z, difference.x) * (180.0 / Math.PI)) - 90.0F;
            float targetPitch = (float) (-Math.atan2(difference.y, horizontal) * (180.0 / Math.PI));
            float yawDelta = Mth.wrapDegrees(targetYaw - player.getYRot());
            float pitchDelta = targetPitch - player.getXRot();
            double angle = Math.hypot(yawDelta, pitchDelta);
            if (angle < bestAngle) {
                bestAngle = angle;
                bestTarget = candidate;
            }
        }

        if (bestTarget == null) {
            return;
        }

        currentTarget = bestTarget;
        rotateToward(player, currentTarget);
    }

    private boolean isValidTarget(Minecraft client, Player player, Player candidate,
                                  double allowedRange, double allowedFov) {
        if (candidate == null || candidate == player || candidate.level() != client.level
                || !candidate.isAlive() || candidate.isSpectator()
                || !PlayerTargetFilters.allowsPlayer(client, player, candidate)
                || !player.hasLineOfSight(candidate)) {
            return false;
        }

        Vec3 aimPoint = candidate.position().add(0.0, candidate.getBbHeight() * 0.55, 0.0);
        Vec3 difference = aimPoint.subtract(player.getEyePosition());
        if (difference.lengthSqr() > allowedRange * allowedRange) {
            return false;
        }

        double horizontal = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
        float yaw = (float) (Math.atan2(difference.z, difference.x) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-Math.atan2(difference.y, horizontal) * (180.0 / Math.PI));
        float yawDelta = Mth.wrapDegrees(yaw - player.getYRot());
        float pitchDelta = pitch - player.getXRot();
        return Math.hypot(yawDelta, pitchDelta) <= allowedFov;
    }

    private void rotateToward(Player player, Player target) {
        Vec3 aimPoint = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
        Vec3 difference = aimPoint.subtract(player.getEyePosition());
        double horizontal = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
        float targetYaw = (float) (Math.atan2(difference.z, difference.x) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-Math.atan2(difference.y, horizontal) * (180.0 / Math.PI));
        AimAssistPolicy.Angles smoothed = AimAssistPolicy.apply(player.getYRot(), player.getXRot(),
                targetYaw, targetPitch, angleFactor.value(), this.horizontal.value(), vertical.value());
        JilatonClient.moduleManager().rotationManager().request(id(), RotationPriorities.AIM_ASSIST,
                smoothed.yaw(), smoothed.pitch(), (float) turnSpeed.value());
    }

    public NumberSetting angleFactor() {
        return angleFactor;
    }

    public BooleanSetting horizontal() {
        return horizontal;
    }

    public BooleanSetting vertical() {
        return vertical;
    }

    /** The legacy attack mode reserves held-attack input; mouse mode yields to the rotation arbiter. */
    public boolean claimsAttackInput() { return enabled() && mode.value().equals("Attack assist"); }

    private void followMouse(Player next) {
        if (next != currentTarget) {
            currentTarget = next;
            pace = ThreadLocalRandom.current().nextDouble(.85, 1.15);
            mouse.resetCarry();
        }
    }

    private Player chooseMouseTarget(Minecraft client) {
        double halfFov = fieldOfView.value() / 2;
        if (CombatTargetTracker.lastHit(1.5, System.nanoTime()) instanceof Player recent
                && mouseAngle(client, recent, range.value()) <= halfFov) return recent;
        if (stickyTarget.value() && mouseAngle(client, currentTarget, range.value() + STICK_RANGE) <= halfFov * STICK_FOV_MULTIPLIER)
            return currentTarget;
        Player best = null;
        double bestAngle = halfFov;
        for (Player candidate : client.level.players()) {
            double angle = mouseAngle(client, candidate, range.value());
            if (angle <= bestAngle) { best = candidate; bestAngle = angle; }
        }
        return best;
    }

    private double mouseAngle(Minecraft client, Player candidate, double reach) {
        LocalPlayer self = client.player;
        if (candidate == null || candidate == self || candidate.level() != client.level || !candidate.isAlive()
                || candidate.isSpectator() || candidate.isCreative() || candidate.isInvisible()
                || !PlayerTargetFilters.allowsPlayer(client, self, candidate)) return Double.MAX_VALUE;
        Vec3 eye = self.getEyePosition();
        if (eye.distanceTo(closest(eye, candidate.getBoundingBox())) > reach) return Double.MAX_VALUE;
        Vec3 center = core(candidate).getCenter();
        if (client.level.clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self)).getType()
                != HitResult.Type.MISS) return Double.MAX_VALUE;
        Vec3 point = mousePoint(self, candidate);
        if (point == null) return 0;
        Vec3 difference = point.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(difference.z, difference.x)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(difference.y, Math.hypot(difference.x, difference.z)));
        return Math.hypot(Mth.wrapDegrees(yaw - self.getYRot()), pitch - self.getXRot());
    }

    private static AABB core(Player player) {
        AABB box = player.getBoundingBox();
        return box.deflate(box.getXsize() * .25, box.getYsize() * .1, box.getXsize() * .25);
    }

    private static Vec3 closest(Vec3 point, AABB box) {
        return new Vec3(Mth.clamp(point.x, box.minX, box.maxX), Mth.clamp(point.y, box.minY, box.maxY), Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    private Vec3 mousePoint(LocalPlayer self, Player target) {
        Vec3 eye = self.getEyePosition(), view = self.getViewVector(1);
        AABB core = core(target);
        if (core.clip(eye, eye.add(view.scale(range.value() + 1))).isPresent()) return null;
        return closest(eye.add(view.scale(eye.distanceTo(core.getCenter()))), core);
    }

    /** Additive mouse correction; called inside the existing Zoom/FreeLook/Freecam ownership hook. */
    public MouseAimState.Nudge nudge(LocalPlayer player, double userYaw, double userPitch, double seconds) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled() || !mode.value().equals("Mouse assist") || client == null || player == null || client.player != player
                || client.level == null || client.screen != null || !client.isWindowActive()
                || player.isUsingItem() || client.options.keyUse.isDown()
                || !JilatonClient.moduleManager().rotationManager().permitsMouseCorrection(RotationPriorities.AIM_ASSIST,
                    owner -> { ClientModule module = JilatonClient.moduleManager().get(owner); return module != null && module.enabled(); })) {
            mouse.reset(); return MouseAimState.STILL;
        }
        Vec3 point = currentTarget == null || mouseAngle(client, currentTarget, range.value() + STICK_RANGE) == Double.MAX_VALUE
                ? null : mousePoint(player, currentTarget);
        float yawError = 0, pitchError = 0;
        if (point != null) {
            Vec3 d = point.subtract(player.getEyePosition());
            yawError = horizontal.value() ? Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90 - player.getYRot()) : 0;
            pitchError = vertical.value() ? (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))) - player.getXRot() : 0;
        }
        double speed = client.options.sensitivity().get() * .6f + .2f;
        double scale = speed * speed * speed * (client.options.getCameraType().isFirstPerson() && player.isScoping() ? 1 : 8);
        return mouse.nudge(userYaw, userPitch, yawError, pitchError, point != null, seconds,
                mouseSpeed.value(), pace, scale, System.nanoTime() / 1_000_000L);
    }
}
