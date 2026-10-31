package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.SwingAnimationsModule;
import dev.jilaton.client.module.impl.ViewModelModule;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the first-person transform order against the mapped 1.21.11 bytecode without loading or running the game.
 *
 * <p>In {@code ItemInHandRenderer.renderArmWithItem} there is exactly one {@code pushPose}/{@code popPose} pair;
 * the view-model hook is injected after that push, so it composes before the vanilla arm transform and before the
 * single {@code swingArm} call of the branch. The swing module replaces that one call (HEAD cancel), and the
 * vanilla attack pose lives only inside {@code swingArm}. {@code renderHandsWithItems} re-enters
 * {@code renderArmWithItem} once per hand, so a hand is never transformed twice. The empty-hand
 * {@code renderPlayerArm} path has its own inline swing and never calls {@code swingArm}, which is why the swing
 * module cannot double up there either.
 */
class FirstPersonTransformCoordinationTest {
    private static final String RENDERER = "net/minecraft/client/renderer/ItemInHandRenderer";
    private static final String POSE_STACK = "com/mojang/blaze3d/vertex/PoseStack";

    @Test
    void viewModelComposesInsideTheSinglePoseAndBeforeEverySwingArm() throws IOException {
        List<Call> arm = body("renderArmWithItem");

        List<Integer> push = positions(arm, POSE_STACK, "pushPose");
        List<Integer> pop = positions(arm, POSE_STACK, "popPose");
        assertEquals(1, push.size(), "renderArmWithItem must push exactly one pose");
        assertEquals(1, pop.size(), "renderArmWithItem must pop exactly one pose");
        assertTrue(push.get(0) < pop.get(0), "push before pop");

        List<Integer> swings = positions(arm, RENDERER, "swingArm");
        assertFalse(swings.isEmpty(), "the render path must still call vanilla swingArm");
        for (int swing : swings) {
            assertTrue(swing > push.get(0), "swingArm must run after the view-model pose is pushed");
            assertTrue(swing < pop.get(0), "swingArm must run before the pose is popped");
        }
        for (int apply : positions(arm, RENDERER, "applyItemArmTransform")) {
            assertTrue(apply > push.get(0) && apply < pop.get(0), "the arm transform must stay inside the pose");
        }
        assertTrue(positions(arm, RENDERER, "applyItemArmAttackTransform").isEmpty(),
                "the vanilla attack pose must only be applied inside swingArm, never on the shared matrix as well");
    }

    @Test
    void vanillaAppliesTheAttackPoseExactlyOnceInsideSwingArm() throws IOException {
        List<Call> swing = body("swingArm");
        assertEquals(1, positions(swing, RENDERER, "applyItemArmAttackTransform").size(),
                "one swingArm call must apply one vanilla attack pose; the module replaces the whole call");
    }

    @Test
    void eachHandIsRenderedOnceAndTheEmptyHandPathNeverCallsSwingArm() throws IOException {
        assertEquals(2, positions(body("renderHandsWithItems"), RENDERER, "renderArmWithItem").size(),
                "one renderArmWithItem call per hand, so no hand is transformed twice");
        assertTrue(positions(body("renderPlayerArm"), RENDERER, "swingArm").isEmpty(),
                "renderPlayerArm has its own inline swing and must not call swingArm");
    }

    @Test
    void viewModelThenSwingComposeOnceInTheVanillaOrder() {
        ViewModelModule model = new ViewModelModule();
        number(model, "main_x").setValue(0.35);
        number(model, "main_y").setValue(0.2);
        number(model, "main_scale").setValue(1.4);
        SwingAnimationsModule swing = new SwingAnimationsModule();

        PoseStack viewOnly = new PoseStack();
        model.apply(viewOnly, InteractionHand.MAIN_HAND, false);
        PoseStack swingOnly = new PoseStack();
        swing.pose(swingOnly, 0.5f, 1);
        assertNotEquals(new Matrix4f(), swingOnly.last().pose(), "a mid-swing pose must actually transform");

        PoseStack combined = new PoseStack();
        model.apply(combined, InteractionHand.MAIN_HAND, false);
        swing.pose(combined, 0.5f, 1);

        Matrix4f expected = new Matrix4f(viewOnly.last().pose()).mul(swingOnly.last().pose());
        assertMatrix(expected, combined.last().pose(), "the view model composes before the swing, once each");
        Matrix4f reversed = new Matrix4f(swingOnly.last().pose()).mul(viewOnly.last().pose());
        boolean orderMatters = false;
        for (int i = 0; i < 12; i++) {
            if (Math.abs(expected.get(i % 4, i / 4) - reversed.get(i % 4, i / 4)) > 1e-5) orderMatters = true;
        }
        assertTrue(orderMatters, "this configuration must make the composition order observable");
    }

    @Test
    void swingPoseIsIdentityAtBothEnds() {
        SwingAnimationsModule swing = new SwingAnimationsModule();
        for (float progress : new float[]{0.0f, 1.0f}) {
            PoseStack pose = new PoseStack();
            swing.pose(pose, progress, 1);
            assertMatrix(new Matrix4f(), pose.last().pose(), "progress " + progress + " must be a no-op");
        }
    }

    private static void assertMatrix(Matrix4f expected, Matrix4f actual, String what) {
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                assertEquals(expected.get(row, column), actual.get(row, column), 1e-5,
                        what + " [" + row + "][" + column + "]");
            }
        }
    }

    private static NumberSetting number(ClientModule module, String id) {
        return module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .map(NumberSetting.class::cast)
                .findFirst().orElseThrow();
    }

    private record Call(String owner, String name) { }

    private static List<Integer> positions(List<Call> calls, String owner, String name) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < calls.size(); i++) {
            if (calls.get(i).owner().equals(owner) && calls.get(i).name().equals(name)) indices.add(i);
        }
        return indices;
    }

    private static List<Call> body(String method) throws IOException {
        List<Call> calls = new ArrayList<>();
        boolean[] found = {false};
        try (var stream = FirstPersonTransformCoordinationTest.class.getClassLoader()
                .getResourceAsStream(RENDERER + ".class")) {
            assertNotNull(stream, "Missing mapped vanilla class " + RENDERER);
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
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertTrue(found[0], "Missing mapped vanilla method " + method);
        return calls;
    }
}
