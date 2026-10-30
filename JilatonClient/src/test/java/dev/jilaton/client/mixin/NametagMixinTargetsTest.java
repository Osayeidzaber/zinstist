package dev.jilaton.client.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;

class NametagMixinTargetsTest {
    @Test void sharedExtractionWritesNamesAndPlayerExtractionUsesThatPath() throws Exception {
        String base = "net/minecraft/client/renderer/entity/EntityRenderer";
        boolean[] nameWrite = {false};
        reader(base).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                if (!name.equals("extractRenderState")) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        if (opcode == Opcodes.PUTFIELD && name.equals("nameTag")) nameWrite[0] = true;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG);
        assertTrue(nameWrite[0], "Vanilla shared extraction no longer sets the name field");
        assertCallsExtraction("net/minecraft/client/renderer/entity/LivingEntityRenderer", base);
        assertCallsExtraction("net/minecraft/client/renderer/entity/player/AvatarRenderer",
                "net/minecraft/client/renderer/entity/LivingEntityRenderer");
    }

    @Test void mixinTargetsSharedExtractionAndClearsTheRenderStateName() throws Exception {
        boolean[] selected = {false}, cleared = {false};
        reader("dev/jilaton/client/mixin/NametagMixin").accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                if (!name.equals("jilaton$replaceSelectedNames")) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override public AnnotationVisitor visitArray(String name) {
                                if (!name.equals("method")) return null;
                                return new AnnotationVisitor(Opcodes.ASM9) {
                                    @Override public void visit(String name, Object value) {
                                        if (value.equals("extractRenderState")) selected[0] = true;
                                    }
                                };
                            }
                        };
                    }
                    @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        if (opcode == Opcodes.PUTFIELD && owner.equals("net/minecraft/client/renderer/entity/state/EntityRenderState")
                                && name.equals("nameTag")) cleared[0] = true;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG);
        assertTrue(selected[0]);
        assertTrue(cleared[0]);
    }

    private void assertCallsExtraction(String child, String parent) throws Exception {
        boolean[] found = {false};
        reader(child).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                if (!name.equals("extractRenderState")) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESPECIAL && owner.equals(parent) && name.equals("extractRenderState")) found[0] = true;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG);
        assertTrue(found[0], () -> child + " no longer delegates extraction to " + parent);
    }

    private ClassReader reader(String resource) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource + ".class")) {
            assertNotNull(stream, resource);
            return new ClassReader(stream);
        }
    }
}
