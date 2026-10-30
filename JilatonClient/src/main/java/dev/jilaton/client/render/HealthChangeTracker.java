package dev.jilaton.client.render;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Bounded health history. First observation is a baseline, never a damage event. */
public final class HealthChangeTracker {
    private final Map<Integer, Float> health = new HashMap<>();
    private final Set<Integer> seen = new HashSet<>();
    private final int capacity;

    public HealthChangeTracker(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
    }

    public void beginTick() { seen.clear(); }

    public float observe(int id, float value) {
        if (!Float.isFinite(value) || value < 0 || (!health.containsKey(id) && health.size() >= capacity)) return 0;
        seen.add(id);
        Float previous = health.put(id, value);
        return previous == null || Math.abs(value - previous) <= 0.05F ? 0 : value - previous;
    }

    public void endTick() { health.keySet().retainAll(seen); }
    public void clear() { health.clear(); seen.clear(); }
    public int size() { return health.size(); }
}
