package dev.jilaton.client.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class FreecamMixinTargetsTest {
    @Test void selectedFreecamHooksExistWithTheExpectedVanillaSignatures() throws Exception {
        assertMethod("net/minecraft/client/Camera", "setup", "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V");
        assertMethod("net/minecraft/client/player/LocalPlayer", "sendPosition", "()V");
        assertMethod("net/minecraft/client/renderer/LevelRenderer", "isSectionCompiledAndVisible", "(Lnet/minecraft/core/BlockPos;)Z");
        assertMethod("net/minecraft/world/entity/Entity", "isInvisibleTo", "(Lnet/minecraft/world/entity/player/Player;)Z");
        assertSelector("FreecamCameraMixin", "setup");
        assertSelector("FreecamLocalPlayerMixin", "sendPosition");
        assertSelector("FreecamRenderMixin", "isSectionCompiledAndVisible");
        assertSelector("FreecamBodyMixin", "isInvisibleTo");
    }

    @Test void cameraShadowTargetsExistAndDoNotRequireMovingThePlayer() throws Exception {
        assertMethod("net/minecraft/client/Camera", "setPosition", "(DDD)V");
        assertMethod("net/minecraft/client/Camera", "setRotation", "(FF)V");
        boolean[] detached = {false};
        reader("net/minecraft/client/Camera").accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
                if (name.equals("detached") && desc.equals("Z")) detached[0] = true;
                return null;
            }
        }, ClassReader.SKIP_CODE);
        assertTrue(detached[0]);
    }

    @Test void moduleDoesNotTeleportTheBodyOrConstructMovementPackets() throws Exception {
        Set<String> violations = new HashSet<>();
        reader("dev/jilaton/client/module/impl/FreecamModule").accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode, String owner, String method, String desc, boolean iface) {
                        if (Set.of("setPos", "setPosition", "absMoveTo", "moveTo", "teleportTo").contains(method)
                                || owner.startsWith("net/minecraft/network/protocol/game/ServerboundMovePlayerPacket"))
                            violations.add(owner + "." + method);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG);
        assertTrue(violations.isEmpty(), violations.toString());
    }

    private void assertMethod(String owner, String selected, String descriptor) throws Exception {
        boolean[] found = {false};
        reader(owner).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                if (name.equals(selected) && desc.equals(descriptor)) found[0] = true;
                return null;
            }
        }, ClassReader.SKIP_CODE);
        assertTrue(found[0], owner + "." + selected + descriptor);
    }

    private void assertSelector(String mixin, String selected) throws Exception {
        Set<String> selectors = new HashSet<>();
        reader("dev/jilaton/client/mixin/" + mixin).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override public AnnotationVisitor visitArray(String name) {
                                if (!name.equals("method")) return null;
                                return new AnnotationVisitor(Opcodes.ASM9) {
                                    @Override public void visit(String name, Object value) { selectors.add(value.toString()); }
                                };
                            }
                        };
                    }
                };
            }
        }, ClassReader.SKIP_CODE);
        assertTrue(selectors.contains(selected), mixin + ": " + selectors);
    }

    private ClassReader reader(String resource) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource + ".class")) {
            assertNotNull(stream, resource);
            return new ClassReader(stream);
        }
    }
}
