package dev.jilaton.client.render;

import dev.jilaton.client.music.Lyrics;
import dev.jilaton.client.music.NowPlaying;
import dev.jilaton.client.render.fx.FxParticles;
import dev.jilaton.client.render.fx.LyricLayout;
import dev.jilaton.client.render.fx.SkyThemes;
import dev.jilaton.client.render.fx.VisualQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VisualSystemsTest {
    @Test
    void parsesLrcWithMultipleStampsOffsetAndWords() {
        Lyrics lyrics = Lyrics.parse("""
                [ti:Test]
                [offset:+200]
                [00:01.00]first
                [00:03.50][00:10.00]repeat
                [00:05.000]<00:05.00>word <00:05.50>by <00:06.00>word
                """);
        assertEquals(4, lyrics.lines().size());
        assertEquals(800, lyrics.lines().get(0).startMs());
        assertEquals("repeat", lyrics.lines().get(1).text());
        assertEquals(9800, lyrics.lines().get(3).startMs());
        Lyrics.Line timed = lyrics.lines().get(2);
        assertEquals("word by word", timed.text());
        assertTrue(timed.timedWords());
        assertEquals(2, Lyrics.wordsSung(timed, 5400));
        assertEquals(-1, lyrics.indexAt(100));
        assertEquals(0, lyrics.indexAt(800));
        assertEquals(2, lyrics.indexAt(6000));
        assertEquals(3, lyrics.indexAt(60_000));
    }

    @Test
    void lyricLayoutKeepsActiveLineOffCrosshairAndAwayFromHead() {
        for (String mode : LyricLayout.MODES) {
            if (mode.equals("Behind Player") || mode.equals("Circular Orbit")) continue;
            LyricLayout.Params p = new LyricLayout.Params(mode, 0.5, 0.3, 0, 16, true, 0, 0);
            for (double yaw = -Math.PI; yaw < Math.PI; yaw += 0.7) {
                LyricLayout.Slot slot = LyricLayout.slot(p, 0, yaw);
                assertTrue(Math.hypot(slot.dx(), slot.dz()) >= LyricLayout.MIN_RADIUS - 0.25, mode);
                assertTrue(LyricLayout.angleFromView(slot, yaw) >= LyricLayout.MIN_VIEW_ANGLE - 1e-6, mode);
            }
        }
    }

    @Test
    void positiveSideAngleIsToTheRight() {
        // Minecraft: yaw 0 looks toward +Z and the right hand points toward -X.
        LyricLayout.Slot slot = LyricLayout.slot(new LyricLayout.Params("Floating Side", 2, 0, 90, 16, false, 0, 0), 0, 0);
        assertEquals(-2, slot.dx(), 1e-9);
        assertEquals(0, slot.dz(), 1e-9);
    }

    @Test
    void neighbouringLinesAreDimmerAndSmaller() {
        assertEquals(1, LyricLayout.opacity(0, 0.4), 1e-9);
        assertEquals(0.4, LyricLayout.opacity(1, 0.4), 1e-9);
        assertTrue(LyricLayout.opacity(2, 0.4) < 0.4);
        assertTrue(LyricLayout.scale(1) < LyricLayout.scale(0));
    }

    @Test
    void particleBudgetRecyclesInsteadOfGrowing() {
        FxParticles particles = FxParticles.get();
        particles.clear();
        particles.setBudget(VisualQuality.LOW.particleBudget());
        FxParticles.Spec spec = new FxParticles.Spec();
        for (int i = 0; i < 5000; i++) {
            spec.reset();
            spec.owner = 7;
            particles.spawn(spec);
        }
        assertEquals(VisualQuality.LOW.particleBudget(), particles.count());
        for (int i = 0; i < 500; i++) particles.decal(0, 0, 0, 1, 1, 0xFFFFFF, 1, i % 2 == 0 ? 7 : 8);
        assertEquals(FxParticles.DECAL_CAPACITY, particles.decalCount());
        particles.clearOwner(7);
        assertEquals(0, particles.count());
        assertTrue(particles.decalCount() > 0);
        particles.clear();
        assertEquals(0, particles.decalCount());
    }

    @Test
    void everySkyThemeResolvesAndCustomUsesUserColors() {
        for (String name : SkyThemes.NAMES) assertNotNull(SkyThemes.resolve(name, 1, 2, 3));
        SkyThemes.Theme custom = SkyThemes.resolve("Custom", 0x010203, 0x040506, 0x070809);
        assertEquals(0x010203, custom.zenith());
        assertEquals(0x040506, custom.horizon());
    }

    @Test
    void nowPlayingExtrapolatesOnlyWhilePlaying() {
        long start = 1_000_000_000L;
        NowPlaying playing = new NowPlaying("Local", "t", "a", "", 1000, 5000, true, null, "", "", true, true, true, start);
        assertEquals(3000, playing.positionNow(start + 2_000_000_000L));
        assertEquals(5000, playing.positionNow(start + 60_000_000_000L));
        NowPlaying paused = new NowPlaying("Local", "t", "a", "", 1000, 5000, false, null, "", "", true, true, true, start);
        assertEquals(1000, paused.positionNow(start + 2_000_000_000L));
        assertEquals("1:05", NowPlaying.time(65_000));
    }
}
