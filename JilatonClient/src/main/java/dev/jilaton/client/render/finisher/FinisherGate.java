package dev.jilaton.client.render.finisher;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

/**
 * Lets each defeated entity trigger one finisher: the death event and the visible-death fallback both report
 * the same entity, and a corpse keeps "dying" for a second, so repeats inside the cooldown are refused.
 */
public final class FinisherGate {
    static final long COOLDOWN_NANOS = 6_000_000_000L;
    private final Int2LongOpenHashMap seen = new Int2LongOpenHashMap();

    public boolean admit(int entityId, long now) {
        if (seen.size() > 64) seen.int2LongEntrySet().removeIf(e -> now - e.getLongValue() > COOLDOWN_NANOS);
        if (seen.containsKey(entityId) && now - seen.get(entityId) < COOLDOWN_NANOS) return false;
        seen.put(entityId, now);
        return true;
    }

    public void clear() {
        seen.clear();
    }
}
