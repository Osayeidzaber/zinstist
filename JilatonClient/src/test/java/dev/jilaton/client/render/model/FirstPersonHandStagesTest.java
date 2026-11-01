package dev.jilaton.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.SwingAnimationsModule;
import dev.jilaton.client.module.impl.ViewModelModule;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the two first-person stages a pass applies for itself when it bypasses vanilla (the knife viewmodel) or when
 * vanilla bakes the swing in (the empty-hand arm): the view-model offset first, the swing style second, each exactly
 * once, and the style composed about the hand rather than about the camera.
 */
class FirstPersonHandStagesTest {
    @Test
    void viewmodelComposesBeforeTheSwingAndEachStageRunsOnce() {
        ViewModelModule model = new ViewModelModule();
        number(model, "main_x").setValue(0.35);
        number(model, "main_y").setValue(0.2);
        number(model, "main_scale").setValue(1.4);
        SwingAnimationsModule swing = new SwingAnimationsModule();
        swing.latch();

        Matrix4f view = onlyViewmodel(model);
        Matrix4f styled = styleAboutTheHand(swing, HumanoidArm.RIGHT);

        PoseStack stages = new PoseStack();
        assertTrue(FirstPersonHandStages.viewmodel(stages, model, InteractionHand.MAIN_HAND));
        assertTrue(FirstPersonHandStages.swing(stages, swing, HumanoidArm.RIGHT, 0.5f));

        assertMatrix(new Matrix4f(view).mul(styled), stages.last().pose(),
                "the view model must compose before the swing");
        assertNotMatrix(new Matrix4f(view).mul(view).mul(styled), stages.last().pose(),
                "the view model must not be applied twice");
        assertNotMatrix(new Matrix4f(view).mul(styled).mul(styled), stages.last().pose(),
                "the swing style must not be applied twice");
        assertNotMatrix(new Matrix4f(styled).mul(view), stages.last().pose(),
                "the swing style must not compose before the view model");
    }

    @Test
    void swingTurnsTheArmAboutTheHandNotAboutTheCamera() {
        SwingAnimationsModule swing = new SwingAnimationsModule();
        swing.latch();
        for (HumanoidArm arm : new HumanoidArm[]{HumanoidArm.RIGHT, HumanoidArm.LEFT}) {
            float side = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
            PoseStack bare = new PoseStack();
            swing.pose(bare, 0.5f, (int) side);
            Matrix4f expected = new Matrix4f()
                    .translate(side * FirstPersonHandStages.HAND_X, FirstPersonHandStages.HAND_Y,
                            FirstPersonHandStages.HAND_Z)
                    .mul(bare.last().pose())
                    .translate(-side * FirstPersonHandStages.HAND_X, -FirstPersonHandStages.HAND_Y,
                            -FirstPersonHandStages.HAND_Z);

            PoseStack pose = new PoseStack();
            assertTrue(FirstPersonHandStages.swing(pose, swing, arm, 0.5f));
            assertMatrix(expected, pose.last().pose(),
                    "the style must be composed about the hand origin, not the camera (" + arm + ")");
            assertNotMatrix(bare.last().pose(), pose.last().pose(),
                    "a bare style at the camera origin is the defect: the pivot must be observable (" + arm + ")");
            assertNotVector(new Vector3f(), pose.last().pose().transformPosition(new Vector3f()),
                    "the camera origin must move: the style turns the arm instead of pinning it");
        }
    }

    @Test
    void swingOnlyPassLeavesThePoseAloneWhenThereIsNothingToStyle() {
        SwingAnimationsModule swing = new SwingAnimationsModule();
        swing.latch();
        for (float progress : new float[]{0.0f, 1.0f}) {
            PoseStack pose = new PoseStack();
            assertFalse(FirstPersonHandStages.swing(pose, swing, HumanoidArm.RIGHT, progress),
                    "the ends of a swing carry no style to draw at progress " + progress);
            assertMatrix(new Matrix4f(), pose.last().pose(), "progress " + progress + " must leave the pose alone");
        }
        PoseStack pose = new PoseStack();
        assertFalse(FirstPersonHandStages.swing(pose, null, HumanoidArm.RIGHT, 0.5f),
                "a disabled module applies nothing");
        assertFalse(FirstPersonHandStages.viewmodel(pose, null, InteractionHand.MAIN_HAND),
                "a disabled view model applies nothing");
        assertMatrix(new Matrix4f(), pose.last().pose(), "no module means an untouched pose");
    }

    /** The style as the stage must compose it: vanilla's pose applied at the hand origin. */
    private static Matrix4f styleAboutTheHand(SwingAnimationsModule swing, HumanoidArm arm) {
        float side = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
        PoseStack pose = new PoseStack();
        swing.pose(pose, 0.5f, (int) side);
        return new Matrix4f()
                .translate(side * FirstPersonHandStages.HAND_X, FirstPersonHandStages.HAND_Y,
                        FirstPersonHandStages.HAND_Z)
                .mul(pose.last().pose())
                .translate(-side * FirstPersonHandStages.HAND_X, -FirstPersonHandStages.HAND_Y,
                        -FirstPersonHandStages.HAND_Z);
    }

    private static Matrix4f onlyViewmodel(ViewModelModule model) {
        PoseStack pose = new PoseStack();
        model.apply(pose, InteractionHand.MAIN_HAND, false);
        return pose.last().pose();
    }

    private static void assertMatrix(Matrix4f expected, Matrix4f actual, String what) {
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                assertEquals(expected.get(row, column), actual.get(row, column), 1e-5,
                        what + " [" + row + "][" + column + "]");
            }
        }
    }

    private static void assertNotMatrix(Matrix4f unexpected, Matrix4f actual, String what) {
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                if (Math.abs(unexpected.get(row, column) - actual.get(row, column)) > 1e-5) return;
            }
        }
        throw new AssertionError(what);
    }

    private static void assertNotVector(Vector3f unexpected, Vector3f actual, String what) {
        if (unexpected.distance(actual) > 1e-5) return;
        throw new AssertionError(what);
    }

    private static NumberSetting number(ClientModule module, String id) {
        return module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .map(NumberSetting.class::cast)
                .findFirst().orElseThrow();
    }
}
