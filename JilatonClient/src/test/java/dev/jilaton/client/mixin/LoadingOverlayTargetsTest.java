package dev.jilaton.client.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadingOverlayTargetsTest {
    @Test void overlayStillRegistersTexturesAndRendersWithGuiGraphics() throws Exception {
        Map<String, Integer> methods = new HashMap<>();
        reader("net/minecraft/client/gui/screens/LoadingOverlay").accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                methods.put(name + desc, access);
                return null;
            }
        }, ClassReader.SKIP_CODE);
        Integer register = methods.get("registerTextures(Lnet/minecraft/client/renderer/texture/TextureManager;)V");
        assertNotNull(register, "Texture registration hook moved");
        assertTrue((register & Opcodes.ACC_STATIC) != 0, "registerTextures must stay static for @Inject");
        assertNotNull(methods.get("render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"), "Overlay render signature changed");
    }

    @Test void shadowedTimingAndProgressFieldsKeepTheirNames() throws Exception {
        Map<String, String> fields = new HashMap<>();
        reader("net/minecraft/client/gui/screens/LoadingOverlay").accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
                fields.put(name, desc);
                return null;
            }
        }, ClassReader.SKIP_CODE);
        assertEquals("Lnet/minecraft/client/Minecraft;", fields.get("minecraft"));
        assertEquals("Lnet/minecraft/server/packs/resources/ReloadInstance;", fields.get("reload"));
        assertEquals("Z", fields.get("fadeIn"));
        assertEquals("F", fields.get("currentProgress"));
        assertEquals("J", fields.get("fadeInStart"));
        assertEquals("J", fields.get("fadeOutStart"));
    }

    @Test void screenStillRendersItselfWhenTheOverlayIsDrawnOnTop() throws Exception {
        boolean[] found = {false};
        reader("net/minecraft/client/gui/screens/Screen").accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                if (name.equals("renderWithTooltipAndSubtitles") && desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")) {
                    found[0] = true;
                }
                return null;
            }
        }, ClassReader.SKIP_CODE);
        assertTrue(found[0], "The branded overlay relies on rendering the finished screen during its reveal");
    }

    private ClassReader reader(String resource) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource + ".class")) {
            assertNotNull(stream);
            return new ClassReader(stream);
        }
    }
}
