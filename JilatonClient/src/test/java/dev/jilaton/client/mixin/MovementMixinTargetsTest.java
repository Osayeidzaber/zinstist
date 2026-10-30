package dev.jilaton.client.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Checks actual injection annotations against mapped vanilla bytecode without loading game classes. */
class MovementMixinTargetsTest {
    @Test
    void eachSelectedMovementMethodActuallyCallsTheRedirectedOperation() throws Exception {
        check("LivingEntityTravelMixin", "net/minecraft/world/entity/LivingEntity", "jilaton$dispatchPlayerMove");
        check("StepMixin", "net/minecraft/world/entity/Entity", "jilaton$useConfiguredStepHeight");
    }

    private void check(String mixin, String vanilla, String handler) throws IOException {
        List<String> selected = new ArrayList<>();
        String[] invocation = {null};
        reader("dev/jilaton/client/mixin/" + mixin).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (!name.equals(handler)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override public AnnotationVisitor visitArray(String name) {
                                if (name.equals("at")) return new AnnotationVisitor(Opcodes.ASM9) {
                                    @Override public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                                        return targetVisitor(invocation);
                                    }
                                };
                                if (!name.equals("method")) return null;
                                return new AnnotationVisitor(Opcodes.ASM9) {
                                    @Override public void visit(String name, Object value) { selected.add(value.toString()); }
                                };
                            }
                            @Override public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                                return targetVisitor(invocation);
                            }
                        };
                    }
                };
            }
        }, ClassReader.SKIP_CODE);
        assertFalse(selected.isEmpty(), "Missing injection selector");
        assertNotNull(invocation[0], "Missing invocation selector");
        Map<String, Integer> matches = new HashMap<>();
        reader(vanilla).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (!selected.contains(name)) return null;
                matches.put(name, 0);
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode, String owner, String method, String descriptor, boolean isInterface) {
                        if (invocation[0].equals("L" + owner + ";" + method + descriptor)) {
                            matches.merge(name, 1, Integer::sum);
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        for (String method : selected) {
            assertTrue(matches.getOrDefault(method, 0) > 0,
                    () -> mixin + " targets " + method + " but it has no " + invocation[0] + " call");
        }
    }

    private ClassReader reader(String resource) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource + ".class")) {
            assertNotNull(stream, "Missing class resource " + resource);
            return new ClassReader(stream);
        }
    }

    private AnnotationVisitor targetVisitor(String[] invocation) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override public void visit(String name, Object value) {
                if (name.equals("target")) invocation[0] = value.toString();
            }
        };
    }
}
