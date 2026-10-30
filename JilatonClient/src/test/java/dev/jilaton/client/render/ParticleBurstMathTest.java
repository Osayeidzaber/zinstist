package dev.jilaton.client.render;

import dev.jilaton.client.module.impl.HitParticlesModule;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleBurstMathTest {
    @Test void impactFallbackClampsEyesToTargetSurface() {
        AABB box = new AABB(1, 2, 3, 3, 6, 5);
        assertEquals(new Vec3(1, 6, 4), ParticleBurstMath.nearestPoint(box, new Vec3(-20, 8, 4)));
        assertEquals(new Vec3(2, 3, 4), ParticleBurstMath.nearestPoint(box, new Vec3(2, 3, 4)));
    }

    @Test void bodySamplingKeepsOriginsInsideTheBoundingBox() {
        AABB box = new AABB(1, 2, 3, 3, 6, 5);
        Vec3 minimum = ParticleBurstMath.pointInside(box, 0, 0, 0);
        Vec3 maximum = ParticleBurstMath.pointInside(box, 1, 1, 1);
        assertEquals(1.2, minimum.x, 1e-9);
        assertEquals(2.4, minimum.y, 1e-9);
        assertEquals(4.8, maximum.z, 1e-9);
        assertTrue(box.contains(minimum));
        assertTrue(box.contains(maximum));
    }

    @Test void targetSelectionHonorsAllThreeModes() {
        assertFalse(ParticleBurstMath.accepts("Living", false, false));
        assertTrue(ParticleBurstMath.accepts("Living", true, false));
        assertFalse(ParticleBurstMath.accepts("Players", true, false));
        assertTrue(ParticleBurstMath.accepts("Players", true, true));
        assertTrue(ParticleBurstMath.accepts("All", false, false));
    }

    @Test void customDustColorNormalizesAndRejectsInvalidInputWithoutLosingValue() {
        HitParticlesModule module = new HitParticlesModule();
        TextSetting color = module.settings().stream().filter(s -> s.id().equals("dust_color"))
                .map(s -> (TextSetting) s).findFirst().orElseThrow();
        color.setValue("#a1b2c3");
        assertEquals("A1B2C3", color.value());
        assertThrows(IllegalArgumentException.class, () -> color.setValue("xxggzz"));
        assertEquals("A1B2C3", color.value());
    }
}
