package dev.jilaton.client.music;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MusicDisplayTest {
    @Test
    void everyModeHasALayoutAndTheListIsStable() {
        assertEquals(List.of("Compact HUD", "Minimal", "World Space", "Cinematic", "Custom"), MusicDisplay.MODES);
        for (String mode : MusicDisplay.MODES) assertNotNull(MusicDisplay.placement(mode, 0.6));
    }

    @Test
    void explicitModesPinToTheCameraInFirstPersonAndCustomDoesNot() {
        assertTrue(MusicDisplay.cameraAnchored(MusicDisplay.COMPACT, true));
        assertTrue(MusicDisplay.cameraAnchored(MusicDisplay.MINIMAL, true));
        assertTrue(MusicDisplay.cameraAnchored(MusicDisplay.WORLD_SPACE, true));
        assertTrue(MusicDisplay.cameraAnchored(MusicDisplay.CINEMATIC, true));
        assertFalse(MusicDisplay.cameraAnchored(MusicDisplay.CUSTOM, true));
        for (String mode : MusicDisplay.MODES) assertFalse(MusicDisplay.cameraAnchored(mode, false));
    }

    @Test
    void everyCardStaysInFrontOfTheEyeAndOutOfTheNearPlane() {
        for (String mode : List.of(MusicDisplay.COMPACT, MusicDisplay.MINIMAL, MusicDisplay.WORLD_SPACE,
                MusicDisplay.CINEMATIC)) {
            MusicDisplay.Placement p = MusicDisplay.placement(mode, 0.6);
            assertTrue(p.forward() >= 1.0, mode + " stays in front");
            assertTrue(p.up() > -1.0 && p.up() < 0.2, mode + " stays near the view centre");
            assertTrue(MusicDisplay.artHalfAngle(p) < Math.toRadians(30), mode + " art does not fill the view");
        }
    }

    @Test
    void minimalHidesTheArtworkButKeepsTheTrackText() {
        MusicDisplay.Placement p = MusicDisplay.placement(MusicDisplay.MINIMAL, 0.6);
        assertFalse(p.showArt());
        assertEquals(0, p.artSize(), 1e-9);
        assertEquals(0, p.progressWidth(), 1e-9);
        assertTrue(p.titleSize() > 0 && p.artistSize() > 0);
        assertEquals(0, MusicDisplay.titleOffset(p), 1e-9);
        assertEquals(0, MusicDisplay.artHalfAngle(p), 1e-9);
    }

    @Test
    void worldSpaceArtworkFollowsThePanelSizeSetting() {
        MusicDisplay.Placement small = MusicDisplay.placement(MusicDisplay.WORLD_SPACE, 0.4);
        MusicDisplay.Placement large = MusicDisplay.placement(MusicDisplay.WORLD_SPACE, 1.0);
        assertTrue(large.artSize() > small.artSize());
        assertTrue(MusicDisplay.titleOffset(large) < 0, "the title sits below the artwork");
        assertTrue(MusicDisplay.titleOffset(large) < MusicDisplay.titleOffset(small));
    }

    @Test
    void customHidesItsTitleInFirstPersonOnlyWhenTheThirdPersonSwitchIsOn() {
        assertTrue(MusicDisplay.showsTrackTitle(MusicDisplay.COMPACT, true, true));
        assertFalse(MusicDisplay.showsTrackTitle(MusicDisplay.CUSTOM, true, true));
        assertTrue(MusicDisplay.showsTrackTitle(MusicDisplay.CUSTOM, true, false));
        assertTrue(MusicDisplay.showsTrackTitle(MusicDisplay.CUSTOM, false, true));
    }
}
