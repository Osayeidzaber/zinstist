package dev.jilaton.client.module.impl;

import dev.jilaton.client.render.TargetEspGeometry;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillAuraMarkerTest {
    @Test
    void auraHighlightOffersEveryTargetEspVariant() {
        KillAuraModule aura = new KillAuraModule();
        List<String> choices = choice(aura, "highlight_style").choices();
        assertTrue(choices.containsAll(List.of(TargetEspGeometry.MINIMAL, TargetEspGeometry.ORBIT,
                TargetEspGeometry.HELIX, TargetEspGeometry.GROUND_AURA, TargetEspGeometry.CYBER_TARGET,
                TargetEspGeometry.CINEMATIC, TargetEspGeometry.CUSTOM)));
        assertFalse(aura.markerGeometry(), "the default Halo keeps the legacy halo path");
    }

    @Test
    void customMarkerMaskTracksEachLayerToggle() {
        KillAuraModule aura = new KillAuraModule();
        assertEquals(TargetEspGeometry.DEFAULT_CUSTOM_MASK, aura.markerMask());

        choice(aura, "highlight_style").setValue(TargetEspGeometry.CUSTOM);
        assertTrue(aura.markerGeometry());

        flag(aura, "custom_ground").setValue(true);
        assertTrue((aura.markerMask() & TargetEspGeometry.COMP_GROUND) != 0);

        flag(aura, "custom_orbit").setValue(false);
        assertEquals(0, aura.markerMask() & TargetEspGeometry.COMP_ORBIT);
    }

    @Test
    void geometryStylesDoNotDrawTheLegacyCorners() {
        KillAuraModule aura = new KillAuraModule();
        choice(aura, "highlight_style").setValue(TargetEspGeometry.HELIX);
        assertFalse(aura.highlightCorners());
        choice(aura, "highlight_style").setValue("Halo + corners");
        assertTrue(aura.highlightCorners());
    }

    private static ChoiceSetting choice(KillAuraModule aura, String id) {
        return (ChoiceSetting) setting(aura, id);
    }

    private static BooleanSetting flag(KillAuraModule aura, String id) {
        return (BooleanSetting) setting(aura, id);
    }

    private static Setting setting(KillAuraModule aura, String id) {
        for (Setting setting : aura.settings()) {
            if (setting.id().equals(id)) return setting;
        }
        throw new IllegalArgumentException("Missing setting " + id);
    }
}
