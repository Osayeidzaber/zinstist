/*
 * Ported from Anubis 0.9.9.3 (net.anubis.client.module.impl.render.SwingAnimationModule), GPL-3.0,
 * with the owners' permission recorded in development/ORCHESTRATION.md (R4). Pose maths are the reference
 * client's; settings, module id and the Jilaton manager lookup are adapted to this client.
 */
package dev.jilaton.client.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.CombatTargetTracker;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

import java.util.List;

/**
 * First-person swing styles ported from Anubis. A style is a pose applied in place of vanilla's
 * {@code swingArm} transform; tempo scales the actual swing duration, and a combat latch keeps the style
 * stable for the whole swing when combat-only is on.
 */
public final class SwingAnimationsModule extends ClientModule {
    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2;
    private static final List<String> STYLES = List.of("Slash", "Chop", "Hook", "Spin", "Double Tap", "Push");
    /**
     * Anubis styles first, then the LiquidBounce-era names as working aliases so an existing saved
     * {@code mode} keeps a style instead of being rejected. Each alias maps onto the closest Anubis pose.
     */
    private static final List<String> MODES = List.of("Slash", "Chop", "Hook", "Spin", "Double Tap", "Push",
            "Swipe", "Dash", "Tap", "Inject", "Slap", "Akrien", "Smooth", "Power", "Feast");
    private static volatile SwingAnimationsModule active;

    private final ChoiceSetting mode = addSetting(new ChoiceSetting(
            "mode", "Swing style", MODES, "Slash"));
    private final NumberSetting scale = addSetting(new NumberSetting(
            "scale", "Intensity", 0.2, 2.0, 0.05, 1.0));
    private final NumberSetting tempo = addSetting(new NumberSetting(
            "tempo", "Tempo", 0.4, 2.5, 0.05, 1.0));
    private final BooleanSetting offhand = addSetting(new BooleanSetting("offhand", "Offhand too", true));
    private final BooleanSetting combatOnly = addSetting(new BooleanSetting("combat_only", "Combat only", false));

    private int styleIndex;
    private boolean styled;

    public SwingAnimationsModule() {
        super("swing_animations", "Swing Animations", "Anubis swing styles: slash, chop, hook, spin, double tap and push.", ModuleCategory.RENDER);
        cacheStyle();
    }

    @Override
    protected void onEnable(Minecraft client) {
        latch();
        active = this;
    }

    @Override
    protected void onDisable(Minecraft client) {
        if (active == this) active = null;
        styled = false;
    }

    /** The live module, or null when disabled; used by the swing mixins outside the manager loop. */
    public static SwingAnimationsModule active() {
        return active;
    }

    private void cacheStyle() {
        styleIndex = styleIndex(mode.value());
    }

    /** Resolves both Anubis names and legacy LiquidBounce names to a style index. */
    static int styleIndex(String value) {
        int index = STYLES.indexOf(value);
        if (index >= 0) return index;
        return switch (value) {
            case "Power", "Inject" -> 1;
            case "Hook" -> 2;
            case "Spin" -> 3;
            case "Tap", "Feast" -> 4;
            case "Dash", "Slap" -> 5;
            default -> 0;
        };
    }

    /** Re-evaluates the combat gate at the start of a swing, so a style never changes mid-swing. */
    public void latch() {
        styled = !combatOnly.value() || inCombat();
    }

    private static boolean inCombat() {
        if (CombatTargetTracker.lastHit(3.0, System.nanoTime()) != null) return true;
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        return client.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
                && hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive();
    }

    /** Whether this swing should be replaced: main hand always, offhand only when enabled. */
    public boolean covers(boolean mainHand) {
        return styled && (mainHand || offhand.value());
    }

    /** Swing duration in ticks after the tempo setting is applied. */
    public int swingTicks(int ticks) {
        return scaledTicks(ticks, (float) tempo.value(), styled);
    }

    static int scaledTicks(int ticks, float tempo, boolean styled) {
        if (!styled) return ticks;
        return Math.max(1, Math.round(ticks / tempo));
    }

    public void apply(PoseStack poseStack, HumanoidArm arm, float progress) {
        cacheStyle();
        pose(poseStack, progress, arm == HumanoidArm.RIGHT ? 1 : -1);
    }

    /** Applies one style's transform. Public for tests of the pose contract. */
    public void pose(PoseStack poseStack, float progress, int side) {
        if (progress <= 0.0f || progress >= 1.0f) return;
        float k = (float) scale.value();
        switch (styleIndex) {
            case 1 -> ankh(poseStack, progress, side, k);
            case 2 -> sickle(poseStack, progress, side, k);
            case 3 -> scarab(poseStack, progress, side, k);
            case 4 -> horus(poseStack, progress, side, k);
            case 5 -> sphinx(poseStack, progress, side, k);
            default -> khopesh(poseStack, progress, side, k);
        }
    }

    private static void khopesh(PoseStack pose, float t, int side, float k) {
        float a = crest(t, 0.3f) * k;
        pose.translate(side * -0.3f * a, 0.05f * Mth.sin(PI * t) * k, -0.1f * a);
        pose.mulPose(Axis.YP.rotationDegrees(side * 64.0f * a));
        pose.mulPose(Axis.ZP.rotationDegrees(side * 48.0f * a));
        pose.mulPose(Axis.XP.rotationDegrees(-14.0f * a));
    }

    private static void ankh(PoseStack pose, float t, int side, float k) {
        float raise = t < 0.28f ? Mth.sin(PI * t / 0.28f) * k : 0.0f;
        float strike = t <= 0.18f ? 0.0f : crest((t - 0.18f) / 0.82f, 0.28f) * k;
        pose.translate(side * -0.06f * strike, 0.16f * raise - 0.1f * strike, 0.06f * raise - 0.24f * strike);
        pose.mulPose(Axis.XP.rotationDegrees(30.0f * raise - 105.0f * strike));
        pose.mulPose(Axis.ZP.rotationDegrees(side * 10.0f * strike));
    }

    private static void sickle(PoseStack pose, float t, int side, float k) {
        float a = crest(t, 0.42f);
        float hook = a * a * k;
        a *= k;
        pose.translate(side * -0.1f * a, -0.24f * a, -0.06f * a);
        pose.mulPose(Axis.ZP.rotationDegrees(side * -100.0f * a));
        pose.mulPose(Axis.YP.rotationDegrees(side * 30.0f * hook));
        pose.mulPose(Axis.XP.rotationDegrees(-25.0f * a));
    }

    private static void scarab(PoseStack pose, float t, int side, float k) {
        float turn = TAU * t;
        float cos = Mth.cos(turn), sin = Mth.sin(turn);
        float body = Mth.sin(PI * t) * k;
        float r = 0.17f * k;
        pose.translate(side * r * 0.6f * (cos - 1.0f), r * sin, -0.12f * body);
        pose.mulPose(Axis.ZP.rotationDegrees(side * 40.0f * sin * k));
        pose.mulPose(Axis.XP.rotationDegrees(-40.0f * body));
    }

    private static void horus(PoseStack pose, float t, int side, float k) {
        float beat = Mth.sin(TAU * t);
        float w = beat * beat * (t < 0.5f ? 1.0f : 0.65f) * k;
        pose.translate(side * -0.08f * w, 0.06f * w, -0.1f * w);
        pose.mulPose(Axis.YP.rotationDegrees(side * 26.0f * w));
        pose.mulPose(Axis.XP.rotationDegrees(-50.0f * w));
        pose.mulPose(Axis.ZP.rotationDegrees(side * -22.0f * w));
    }

    private static void sphinx(PoseStack pose, float t, int side, float k) {
        float a = (0.5f - 0.5f * Mth.cos(TAU * t)) * k;
        pose.translate(0.0f, -0.06f * a, -0.1f * a);
        pose.mulPose(Axis.XP.rotationDegrees(-24.0f * a));
        pose.mulPose(Axis.YP.rotationDegrees(side * 10.0f * a));
    }

    /** Fast attack, eased release: 0 at both ends, 1 at {@code peak}. */
    static float crest(float t, float peak) {
        if (t <= 0.0f || t >= 1.0f) return 0.0f;
        if (t < peak) {
            float u = 1.0f - t / peak;
            return 1.0f - u * u * u;
        }
        float u = (t - peak) / (1.0f - peak);
        return 1.0f - u * u * (3.0f - 2.0f * u);
    }
}
