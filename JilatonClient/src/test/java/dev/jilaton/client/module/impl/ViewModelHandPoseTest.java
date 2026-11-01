package dev.jilaton.client.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.render.model.HandPoseState;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ViewModelHandPoseTest {
    @Test void handOriginScalesAroundTheReferencePivotForEitherArm() {
        ViewModelModule module = new ViewModelModule();
        choice(module, "pivot").setValue("Hand");
        number(module, "main_scale").setValue(1.5);
        for (HumanoidArm arm : HumanoidArm.values()) {
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            PoseStack pose = new PoseStack();
            module.applyAt(pose, InteractionHand.MAIN_HAND, false, arm, true, false, 1);
            Matrix4f expected = new Matrix4f().translate(side * .56f, -.52f, -.72f)
                    .scale(1.5f).translate(-side * .56f, .52f, .72f);
            assertTrue(expected.equals(pose.last().pose(), 1e-6f));
        }
    }

    @Test void linkedHandsMirrorWithoutDestroyingIndependentOffhandSettings() {
        ViewModelModule module = new ViewModelModule();
        choice(module, "pivot").setValue("Hand");
        bool(module, "link_hands").setValue(true);
        number(module, "main_x").setValue(.4);
        number(module, "off_x").setValue(.9);
        PoseStack pose = new PoseStack();
        module.applyAt(pose, InteractionHand.OFF_HAND, false, HumanoidArm.RIGHT, true, false, 1);
        assertEquals(-.4, pose.last().pose().m30(), 1e-6);
        assertEquals(.9, number(module, "off_x").value(), 1e-6);
        bool(module, "link_hands").setValue(false);
        pose = new PoseStack();
        module.applyAt(pose, InteractionHand.OFF_HAND, false, HumanoidArm.RIGHT, true, false, 2);
        assertEquals(-.9, pose.last().pose().m30(), 1e-6);
    }

    @Test void glideUsesElapsedTimeAndDisabledHandsReturnToRest() {
        float[] target = {1, 0, 0, 0, 0, 0, 1.5f};
        HandPoseState slow = new HandPoseState(), fast = new HandPoseState();
        slow.step(target, true, .4, 0);
        fast.step(target, true, .4, 0);
        for (int i = 1; i <= 30; i++) slow.step(target, true, .4, i * 1_000_000_000L / 30);
        for (int i = 1; i <= 120; i++) fast.step(target, true, .4, i * 1_000_000_000L / 120);
        assertEquals(slow.value(0), fast.value(0), 1e-5);
        assertFalse(slow.rested());
        for (int i = 1; i <= 120; i++) slow.step(target, false, .4, 1_000_000_000L + i * 16_666_667L);
        assertTrue(slow.rested());
        assertEquals(0, slow.value(0));
        assertEquals(1, slow.value(6));
    }

    @Test void reducedMotionAndZeroGlideApplyImmediatelyAndLongPausesAreBounded() {
        ViewModelModule module = new ViewModelModule();
        number(module, "main_x").setValue(1);
        number(module, "glide").setValue(1);
        PoseStack pose = new PoseStack();
        module.applyAt(pose, InteractionHand.MAIN_HAND, false, HumanoidArm.RIGHT, true, true, 1);
        assertEquals(1, pose.last().pose().m30(), 1e-6);

        HandPoseState state = new HandPoseState();
        float[] target = {1, 0, 0, 0, 0, 0, 1};
        state.step(target, true, 1, 0);
        state.step(target, true, 1, 60_000_000_000L);
        assertEquals(1 - Math.exp(-.3), state.value(0), 1e-6);
        state.step(target, false, 0, 60_000_000_001L);
        assertTrue(state.rested());
    }

    private static NumberSetting number(ViewModelModule m, String id) {
        return (NumberSetting) m.settings().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
    }
    private static ChoiceSetting choice(ViewModelModule m, String id) {
        return (ChoiceSetting) m.settings().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
    }
    private static BooleanSetting bool(ViewModelModule m, String id) {
        return (BooleanSetting) m.settings().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
    }
}
