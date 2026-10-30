package dev.jilaton.client.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;

class FovMixinTargetsTest {
    @Test void screenEffectMethodHasExactlyOneNoArgPoseStackConstruction() throws Exception {
        int[] constructions = {0};
        reader("net/minecraft/client/renderer/ScreenEffectRenderer").accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                if (!name.equals("renderScreenEffect")) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean iface) {
                        if (owner.equals("com/mojang/blaze3d/vertex/PoseStack") && name.equals("<init>") && descriptor.equals("()V")) constructions[0]++;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG);
        assertEquals(1, constructions[0], "Constructor redirect must be revisited if vanilla changes");
    }

    @Test void gameRendererExposesThePanoramaGuardUsedByCombinedFovAndZoomHook() throws Exception {
        boolean[] found = {false};
        reader("net/minecraft/client/renderer/GameRenderer").accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                if (name.equals("isPanoramicMode") && desc.equals("()Z")) found[0] = true;
                return null;
            }
        }, ClassReader.SKIP_CODE);
        assertTrue(found[0]);
    }

    private ClassReader reader(String resource) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource + ".class")) {
            assertNotNull(stream);
            return new ClassReader(stream);
        }
    }
}
