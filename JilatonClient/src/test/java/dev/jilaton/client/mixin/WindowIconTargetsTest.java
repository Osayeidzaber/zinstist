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

class WindowIconTargetsTest {
    @Test void iconSetStillExposesStandardIconsPerPlatform() throws Exception {
        Map<String, Integer> methods = new HashMap<>();
        reader("com/mojang/blaze3d/platform/IconSet").accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                methods.put(name + desc, access);
                return null;
            }
        }, ClassReader.SKIP_CODE);
        Integer granted = methods.get("getStandardIcons(Lnet/minecraft/server/packs/PackResources;)Ljava/util/List;");
        assertNotNull(granted, "Window.setIcon no longer asks IconSet for its icons");
        assertEquals(0, granted & Opcodes.ACC_STATIC, "getStandardIcons must stay an instance method for @Inject");
    }

    @Test void iconSetKeepsBothVanillaConstants() throws Exception {
        Map<String, String> fields = new HashMap<>();
        reader("com/mojang/blaze3d/platform/IconSet").accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
                fields.put(name, desc);
                return null;
            }
        }, ClassReader.SKIP_CODE);
        assertEquals("Lcom/mojang/blaze3d/platform/IconSet;", fields.get("RELEASE"));
        assertEquals("Lcom/mojang/blaze3d/platform/IconSet;", fields.get("SNAPSHOT"));
    }

    @Test void windowStillUploadsIconsAndTitles() throws Exception {
        Map<String, Boolean> methods = new HashMap<>();
        reader("com/mojang/blaze3d/platform/Window").accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                methods.put(name + desc, true);
                return null;
            }
        }, ClassReader.SKIP_CODE);
        assertTrue(methods.containsKey("setIcon(Lnet/minecraft/server/packs/PackResources;Lcom/mojang/blaze3d/platform/IconSet;)V"),
                "Window.setIcon is the only path that pushes icons to GLFW");
        assertTrue(methods.containsKey("setTitle(Ljava/lang/String;)V"), "Window.setTitle backs updateTitle");
    }

    @Test void minecraftStillBuildsItsTitleBeforeBranding() throws Exception {
        boolean[] found = {false};
        reader("net/minecraft/client/Minecraft").accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                if (name.equals("createTitle") && desc.equals("()Ljava/lang/String;")) found[0] = true;
                return null;
            }
        }, ClassReader.SKIP_CODE);
        assertTrue(found[0], "Window title branding injects into Minecraft.createTitle");
    }

    private ClassReader reader(String resource) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource + ".class")) {
            assertNotNull(stream);
            return new ClassReader(stream);
        }
    }
}
