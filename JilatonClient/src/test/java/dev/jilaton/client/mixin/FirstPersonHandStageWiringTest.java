package dev.jilaton.client.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins which first-person hand passes run the view-model and swing stages, without loading the game.
 *
 * <p>The knife pass cancels {@code ItemInHandRenderer.renderArmWithItem} at HEAD, so neither the view-model hook
 * inside vanilla nor {@code swingArm} runs for it: the pass must apply both stages itself, exactly once, before it
 * hands the pose to {@code CosmeticKnifeModule.renderInHand}. The vanilla empty-hand arm never reaches
 * {@code swingArm} either, so the wrap of {@code renderPlayerArm} must apply the swing stage itself.
 */
class FirstPersonHandStageWiringTest {
    private static final String STAGES = "dev/jilaton/client/render/model/FirstPersonHandStages";
    private static final String KNIFE = "dev/jilaton/client/mixin/KnifeViewmodelMixin";
    private static final String HANDS = "dev/jilaton/client/mixin/FirstPersonHandsAndItemsMixin";
    private static final String KNIFE_MODULE = "dev/jilaton/client/module/impl/CosmeticKnifeModule";
    private static final String POSE_STACK = "com/mojang/blaze3d/vertex/PoseStack";

    @Test
    void knifePassAppliesBothStagesOnceBeforeItDraws() throws IOException {
        List<Call> body = body(KNIFE, "jilaton$knifeViewmodel").calls();
        int viewModel = only(body, STAGES, "viewmodel", "the knife pass must apply the view-model stage exactly once");
        int swing = only(body, STAGES, "swing", "the knife pass must apply the swing stage exactly once");
        int draw = only(body, KNIFE_MODULE, "renderInHand", "the knife pass must draw the knife");

        assertTrue(viewModel < draw, "the view-model offset must be on the pose before the knife is drawn");
        assertTrue(swing < draw, "the swing style must be on the pose before the knife is drawn");

        List<Integer> push = positions(body, POSE_STACK, "pushPose");
        List<Integer> pop = positions(body, POSE_STACK, "popPose");
        assertEquals(1, push.size(), "the cancelled pass must open exactly one pose");
        assertEquals(1, pop.size(), "the cancelled pass must close exactly one pose");
        assertTrue(push.get(0) < viewModel, "the stages must run inside the pose the knife pass opened");
        assertTrue(draw < pop.get(0), "the knife must be drawn inside the pose the pass opened");
        assertTrue(pop.get(0) < only(body, "org/spongepowered/asm/mixin/injection/callback/CallbackInfo", "cancel",
                        "the cancelled pass must cancel exactly once"),
                "the pose must be closed before the vanilla pass is cancelled");
    }

    @Test
    void emptyHandWrapAppliesTheSwingStage() throws IOException {
        Body body = body(HANDS, "jilaton$emptyHandSwingStyle");
        only(body.calls(), STAGES, "swing", "the empty-hand arm must get the swing stage exactly once");
        assertEquals(2, positions(body.calls(),
                        "com/llamalad7/mixinextras/injector/wrapoperation/Operation", "call").size(),
                "both branches still draw the vanilla arm");
        assertTrue(body.floats().contains(0.0f),
                "the styled branch must hand vanilla a rest swing so the arm is not moved twice");
    }

    private record Call(String owner, String name) { }

    private record Body(List<Call> calls, List<Float> floats) { }

    private static int only(List<Call> calls, String owner, String name, String what) {
        List<Integer> found = positions(calls, owner, name);
        assertEquals(1, found.size(), what);
        return found.get(0);
    }

    private static List<Integer> positions(List<Call> calls, String owner, String name) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < calls.size(); i++) {
            Call call = calls.get(i);
            if (call.owner().equals(owner) && call.name().equals(name)) indices.add(i);
        }
        return indices;
    }

    private static Body body(String type, String method) throws IOException {
        List<Call> calls = new ArrayList<>();
        List<Float> floats = new ArrayList<>();
        boolean[] found = {false};
        try (var stream = FirstPersonHandStageWiringTest.class.getClassLoader()
                .getResourceAsStream(type + ".class")) {
            assertNotNull(stream, "Missing class " + type);
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    if (!name.equals(method)) return null;
                    found[0] = true;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                                                    boolean isInterface) {
                            calls.add(new Call(owner, name));
                        }

                        @Override
                        public void visitLdcInsn(Object value) {
                            if (value instanceof Float constant) floats.add(constant);
                        }

                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.FCONST_0) floats.add(0.0f);
                            if (opcode == Opcodes.FCONST_1) floats.add(1.0f);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertTrue(found[0], "Missing method " + type + "." + method);
        return new Body(calls, floats);
    }
}
