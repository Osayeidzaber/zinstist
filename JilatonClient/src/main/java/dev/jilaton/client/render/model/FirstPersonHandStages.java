package dev.jilaton.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.module.impl.SwingAnimationsModule;
import dev.jilaton.client.module.impl.ViewModelModule;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The two first-person stages every hand pass must receive exactly once: the view-model offset, then the active
 * swing style.
 *
 * <p>Vanilla has one of each inside {@code ItemInHandRenderer.renderArmWithItem}: the method pushes the hand pose,
 * the view-model hook runs right after that push, and {@code swingArm} adds the attack pose inside the same pose.
 * Two first-person paths never reach those stages — the knife viewmodel cancels the method at HEAD, and the
 * empty-hand arm is drawn by {@code renderPlayerArm}, whose swing is baked into its own transforms and which never
 * calls {@code swingArm}. Each of them applies the missing stages here instead of repeating the maths, so a hand is
 * never transformed twice.
 *
 * <p>Callers pass a module only when it is enabled (null otherwise), which is how the neighbouring mixins resolve
 * theirs; both methods report whether they touched the pose so a caller can decide what vanilla should still draw.
 */
public final class FirstPersonHandStages {
    /**
     * Vanilla's first-person item origin — the point {@code applyItemArmTransform} moves to before {@code swingArm}
     * rotates a held item. The swing styles were authored to replace that call, so they are composed about this
     * point here as well: an arm then turns about the hand instead of orbiting the camera.
     */
    public static final float HAND_X = 0.56f, HAND_Y = -0.52f, HAND_Z = -0.72f;

    private FirstPersonHandStages() { }

    /** The view-model offset of a main-hand pass; false when there is no module to apply. */
    public static boolean viewmodel(PoseStack pose, ViewModelModule module, InteractionHand hand) {
        if (module == null) return false;
        // A knife pass is never the shared two-handed map: the blade replaces the map for that hand.
        module.apply(pose, hand, false);
        return true;
    }

    /** Live pass, with the player's handedness and the module's animated disable lifecycle. */
    public static boolean viewmodel(PoseStack pose, ViewModelModule module, InteractionHand hand, HumanoidArm mainArm) {
        if (module == null) return false;
        module.applyRendered(pose, hand, false, mainArm);
        return true;
    }

    /**
     * The active swing style of a main-hand pass, composed about {@link #HAND_X the hand}. False when no style
     * covers this swing, so the caller can leave vanilla's own swing in place instead of replacing it with a static
     * arm.
     */
    public static boolean swing(PoseStack pose, SwingAnimationsModule module, HumanoidArm arm, float progress) {
        if (module == null || !module.covers(true)) return false;
        // SwingAnimationsModule#pose is the identity at both ends of the swing; do not claim a swing there.
        if (progress <= 0.0f || progress >= 1.0f) return false;
        float side = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
        pose.translate(side * HAND_X, HAND_Y, HAND_Z);
        module.apply(pose, arm, progress);
        pose.translate(-side * HAND_X, -HAND_Y, -HAND_Z);
        return true;
    }
}
