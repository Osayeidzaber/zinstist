package dev.jilaton.client.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the two vanilla methods the screenshot actions hook, and the annotations that hook them, so a
 * vanilla refactor or an annotation typo fails here instead of silently never firing in game.
 */
class ScreenshotMixinTargetsTest {
    private static final String GRAB = "grab(Ljava/io/File;Ljava/lang/String;"
            + "Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V";
    private static final String CLICK = "defaultHandleGameClickEvent(Lnet/minecraft/network/chat/ClickEvent;"
            + "Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;)V";

    @Test void fiveArgGrabIsStillTheSharedScreenshotPath() throws Exception {
        Integer access = methods("net/minecraft/client/Screenshot").get(GRAB);
        assertNotNull(access, "Screenshot.grab(File,String,RenderTarget,int,Consumer) must keep its signature");
        assertTrue((access & Opcodes.ACC_STATIC) != 0, "the grab hook expects a static method");
    }

    @Test void screenStillRoutesClickEventsThroughTheDefaultHandler() throws Exception {
        Integer access = methods("net/minecraft/client/gui/screens/Screen").get(CLICK);
        assertNotNull(access, "Screen.defaultHandleGameClickEvent must keep its signature");
        assertTrue((access & Opcodes.ACC_STATIC) != 0, "the click hook expects a static method");
    }

    @Test void grabMixinDecoratesTheConsumerArgumentOnly() throws Exception {
        Map<String, Object> values = methodAnnotation(
                "dev/jilaton/client/mixin/screenshot/ScreenshotGrabMixin", "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;");
        assertEquals(4, values.get("index"), "the consumer is the fifth parameter of the five-argument grab");
        assertEquals(Boolean.TRUE, values.get("argsOnly"));
    }

    @Test void clickMixinCancelsAtTheHeadOfTheDefaultHandler() throws Exception {
        Map<String, Object> values = methodAnnotation(
                "dev/jilaton/client/mixin/screenshot/ScreenshotClickMixin", "Lorg/spongepowered/asm/mixin/injection/Inject;");
        assertEquals(Boolean.TRUE, values.get("cancellable"),
                "without a cancel vanilla forwards the custom click to the server");
    }

    @Test void bothMixinsNameTheirVanillaTargets() throws Exception {
        assertEquals("net.minecraft.client.Screenshot", mixinTarget(
                "dev/jilaton/client/mixin/screenshot/ScreenshotGrabMixin"));
        assertEquals("net.minecraft.client.gui.screens.Screen", mixinTarget(
                "dev/jilaton/client/mixin/screenshot/ScreenshotClickMixin"));
    }

    private static Map<String, Integer> methods(String resource) throws Exception {
        Map<String, Integer> methods = new HashMap<>();
        reader(resource).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                methods.put(name + desc, access);
                return null;
            }
        }, ClassReader.SKIP_CODE);
        return methods;
    }

    /** The single annotated method of a one-hook mixin, with its annotation's primitive values. */
    private static Map<String, Object> methodAnnotation(String resource, String annotationDesc) throws Exception {
        Map<String, Object> found = new HashMap<>();
        boolean[] seen = {false};
        reader(resource).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if (!desc.equals(annotationDesc) || seen[0]) return null;
                        seen[0] = true;
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override public void visit(String name, Object value) {
                                found.put(name, value);
                            }
                        };
                    }
                };
            }
        }, 0);
        assertTrue(seen[0], resource + " must carry " + annotationDesc);
        return found;
    }

    private static String mixinTarget(String resource) throws Exception {
        String[] target = {null};
        reader(resource).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (!desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) return null;
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override public void visit(String name, Object value) {
                        if (value instanceof Type type) target[0] = type.getClassName();
                    }

                    /** {@code @Mixin(Screenshot.class)} is a Type[] value, which ASM reports as an array. */
                    @Override public AnnotationVisitor visitArray(String name) {
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override public void visit(String element, Object value) {
                                if (value instanceof Type type) target[0] = type.getClassName();
                            }
                        };
                    }
                };
            }
        }, 0);
        return target[0];
    }

    private static ClassReader reader(String resource) throws Exception {
        try (var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource + ".class")) {
            assertNotNull(stream, resource + " must be compiled for the target check to mean anything");
            return new ClassReader(stream);
        }
    }
}
