package dev.jilaton.client.render.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewModelPresetTest {
    private static final ViewModelPreset.Hand MAIN =
            new ViewModelPreset.Hand(true, 0.35, -0.6, 0.9, 15, -30, 45, 1.35);
    private static final ViewModelPreset.Hand OFF =
            new ViewModelPreset.Hand(false, -0.2, 0.1, -0.05, -180, 180, 0, 0.25);

    @Test
    void aPresetRoundTripsThroughItsWireForm() {
        ViewModelPreset preset = new ViewModelPreset("Combat low", MAIN, OFF);
        ViewModelPreset decoded = ViewModelPreset.decode(preset.encode());
        assertEquals(preset, decoded);
    }

    @Test
    void aListOfPresetsPreservesOrderAndValues() {
        List<ViewModelPreset> presets = List.of(
                new ViewModelPreset("Default", ViewModelPreset.Hand.NONE, ViewModelPreset.Hand.NONE),
                new ViewModelPreset("Combat low", MAIN, OFF),
                new ViewModelPreset("Wide", MAIN, ViewModelPreset.Hand.NONE));
        assertEquals(presets, ViewModelPreset.decodeAll(ViewModelPreset.encodeAll(presets)));
    }

    @Test
    void malformedEntriesAreDroppedAndHealthyOnesSurvive() {
        List<ViewModelPreset> decoded = ViewModelPreset.decodeAll(
                "Good:1,0,0,0,0,0,0,1|1,0,0,0,0,0,0,1;"
                        + "nan:1,NaN,0,0,0,0,0,1|1,0,0,0,0,0,0,1;"
                        + "short:1,0,0|1,0,0,0,0,0,0,1;"
                        + "missing hand:1,0,0,0,0,0,0,1;"
                        + ":1,0,0,0,0,0,0,1|1,0,0,0,0,0,0,1");
        assertEquals(1, decoded.size());
        assertEquals("Good", decoded.get(0).name());
        assertNull(ViewModelPreset.decode(null));
        assertNull(ViewModelPreset.decode(""));
        assertNull(ViewModelPreset.decode("no separator"));
    }

    @Test
    void namesAreBoundedSoTheWireFormCannotBreak() {
        assertEquals("Blade_Fix 1", ViewModelPreset.cleanName("  Blade_Fix 1  "));
        assertEquals("Preset", ViewModelPreset.cleanName("///"));
        assertEquals("Preset", ViewModelPreset.cleanName(null));
        assertTrue(ViewModelPreset.cleanName("x".repeat(80)).length() <= ViewModelPreset.MAX_NAME);
        ViewModelPreset named = new ViewModelPreset("a:b;c|d", MAIN, OFF);
        assertNotNull(ViewModelPreset.decode(named.encode()), "cleaned names must still parse");
    }

    @Test
    void lookupIgnoresCase() {
        List<ViewModelPreset> presets = List.of(new ViewModelPreset("Low", MAIN, OFF));
        assertNotNull(ViewModelPreset.find(presets, "low"));
        assertNull(ViewModelPreset.find(presets, "missing"));
    }
}
