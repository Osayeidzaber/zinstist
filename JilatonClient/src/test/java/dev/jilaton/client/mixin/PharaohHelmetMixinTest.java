package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.PharaohHeaddressModule;
import dev.jilaton.client.render.character.PharaohHelmetPolicy;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gate rows for the helmet hiding. Rows that need a live player (own/other, range, friend filter) live in
 * PharaohHeaddressVisibilityTest and drive the same {@link PharaohHeaddressModule.Viewer} decision; the
 * rows here cover the slot/state/module branches plus the unresolvable-avatar rule.
 */
class PharaohHelmetMixinTest {
    private PharaohHeaddressModule module;

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void freshModule() {
        module = new PharaohHeaddressModule();
    }

    @AfterEach
    void switchOff() {
        module.setEnabled(false);
    }

    @Test
    void moduleOffKeepsEveryHelmet() {
        assertFalse(PharaohHelmetPolicy.hidesHelmet(EquipmentSlot.HEAD, new AvatarRenderState(), null));
    }

    @Test
    void moduleOnHidesAnUnresolvablePlayersHelmet() {
        module.setEnabled(true);
        assertTrue(PharaohHelmetPolicy.hidesHelmet(EquipmentSlot.HEAD, new AvatarRenderState(), null));
    }

    @Test
    void anAvatarWithoutAnIdIsUnresolvable() {
        module.setEnabled(true);
        AvatarRenderState state = new AvatarRenderState();
        state.id = 0;
        assertNull(PharaohHelmetPolicy.playerOf(state), "id 0 names no entity");
        assertTrue(PharaohHelmetPolicy.hidesHelmet(EquipmentSlot.HEAD, state, PharaohHelmetPolicy.playerOf(state)));
    }

    @Test
    void anAvatarWithoutALevelIsUnresolvable() {
        module.setEnabled(true);
        AvatarRenderState state = new AvatarRenderState();
        state.id = 7;
        assertNull(Minecraft.getInstance(), "headless test JVM: there is no client, so no level");
        assertNull(PharaohHelmetPolicy.playerOf(state), "no level means the avatar cannot resolve");
        assertTrue(PharaohHelmetPolicy.hidesHelmet(EquipmentSlot.HEAD, state, PharaohHelmetPolicy.playerOf(state)));
    }

    @Test
    void nonPlayerHumanoidsKeepTheirHelmets() {
        module.setEnabled(true);
        assertFalse(PharaohHelmetPolicy.hidesHelmet(EquipmentSlot.HEAD, new HumanoidRenderState(), null));
        assertFalse(PharaohHelmetPolicy.hidesHelmet(EquipmentSlot.HEAD, null, null));
    }

    @Test
    void everyOtherArmorSlotIsLeftAlone() {
        module.setEnabled(true);
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                EquipmentSlot.BODY, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            assertFalse(PharaohHelmetPolicy.hidesHelmet(slot, new AvatarRenderState(), null), slot.name());
        }
    }

    @Test
    void activeFollowsTheModuleSwitch() {
        module.setEnabled(true);
        assertTrue(PharaohHeaddressModule.active() != null);
        module.setEnabled(false);
        assertTrue(PharaohHeaddressModule.active() == null);
    }

    /**
     * Mixin's MixinApplicatorStandard.checkMethodVisibility rejects any static mixin method that is not
     * private (unless synthetic or @Overwrite); @Unique does not exempt it. Scans every mixin source.
     */
    @Test
    void noMixinDeclaresANonPrivateStaticMethod() throws IOException {
        Path dir = Path.of("src/main/java/dev/jilaton/client/mixin");
        if (!Files.exists(dir)) dir = Path.of("JilatonClient").resolve(dir);
        List<String> offenders = new ArrayList<>();
        try (var files = Files.walk(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                for (String raw : Files.readAllLines(file)) {
                    Matcher declaration = DECLARATION.matcher(raw.trim());
                    if (!declaration.find()) continue;
                    String modifiers = raw.trim().substring(0, declaration.start(1));
                    if (modifiers.contains("static") && !modifiers.contains("private")
                            && !modifiers.contains("class") && !modifiers.contains("interface")) {
                        offenders.add(file.getFileName() + "#" + declaration.group(1));
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "non-private static mixin methods fail at apply time: " + offenders);
        String helmet = source("src/main/java/dev/jilaton/client/mixin/PharaohHelmetMixin.java");
        assertFalse(helmet.contains("static boolean hidesHelmet"), "the gate lives in PharaohHelmetPolicy");
    }

    /** Matches a method signature start, including signatures whose parameter list wraps. */
    private static final Pattern DECLARATION = Pattern.compile(
            "^(?:(?:public|protected|private|static|final|abstract|synchronized)\\s+)+"
                    + "(?:<[^>]+>\\s+)?[\\w$.<>\\[\\],?]+\\s+([\\w$]+)\\s*\\(");

    private static String source(String relative) throws IOException {
        Path path = Path.of(relative);
        if (!Files.exists(path)) path = Path.of("JilatonClient").resolve(relative);
        return Files.readString(path);
    }
}
