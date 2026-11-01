package dev.jilaton.client.render;

/**
 * Hit score for the current duel, counted only from what the client can see: a hit is landed when the target's
 * hurt timer restarts shortly after you swung at it, and taken when your own hurt timer restarts. Combo is hits
 * landed since you were last hit. Everything resets when the target changes.
 */
public final class FightStats {
    /** How long after a swing a target's hurt flash still counts as your hit. */
    static final long ATTRIBUTION_NANOS = 400_000_000L;

    private int targetId = -1;
    private int lastTargetHurt, lastSelfHurt;
    private int landed, taken, combo;

    /**
     * Feed one observation per frame.
     *
     * @param sinceAttackNanos time since you last attacked this target, or a negative value if never
     */
    public void observe(int id, int targetHurtTime, int selfHurtTime, long sinceAttackNanos) {
        if (id != targetId) {
            targetId = id;
            landed = taken = combo = 0;
            lastTargetHurt = targetHurtTime;
            lastSelfHurt = selfHurtTime;
            return;
        }
        if (targetHurtTime > lastTargetHurt && sinceAttackNanos >= 0 && sinceAttackNanos <= ATTRIBUTION_NANOS) {
            landed++;
            combo++;
        }
        if (selfHurtTime > lastSelfHurt) {
            taken++;
            combo = 0;
        }
        lastTargetHurt = targetHurtTime;
        lastSelfHurt = selfHurtTime;
    }

    public int landed() { return landed; }
    public int taken() { return taken; }
    public int combo() { return combo; }
}
