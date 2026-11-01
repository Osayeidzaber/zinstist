package dev.jilaton.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FightStatsTest {
    @Test
    void countsOnlyHurtFlashesThatFollowYourSwing() {
        FightStats stats = new FightStats();
        stats.observe(7, 0, 0, -1);
        stats.observe(7, 10, 0, 50_000_000L);       // your hit lands
        stats.observe(7, 9, 0, 100_000_000L);       // timer counting down, not a new hit
        stats.observe(7, 10, 0, 2_000_000_000L);    // someone else hit them
        assertEquals(1, stats.landed());
        assertEquals(1, stats.combo());
    }

    @Test
    void beingHitBreaksComboAndCountsAsTaken() {
        FightStats stats = new FightStats();
        stats.observe(7, 0, 0, -1);
        stats.observe(7, 10, 0, 0);
        stats.observe(7, 0, 0, 0);
        stats.observe(7, 10, 0, 0);
        assertEquals(2, stats.combo());
        stats.observe(7, 9, 10, 0);
        assertEquals(0, stats.combo());
        assertEquals(1, stats.taken());
        assertEquals(2, stats.landed());
    }

    @Test
    void newTargetResetsTheScoreWithoutCountingItsCurrentFlash() {
        FightStats stats = new FightStats();
        stats.observe(7, 0, 0, -1);
        stats.observe(7, 10, 0, 0);
        stats.observe(8, 10, 5, 0);
        assertEquals(0, stats.landed());
        assertEquals(0, stats.taken());
    }
}
