package dev.jilaton.client.render;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/** Per-player bounded, recently observed equipment. Contains only in-world item snapshots. */
public final class RecentItems<P, I, S> {
    private final int playerLimit, itemLimit;
    private final Map<P, LinkedHashMap<I, S>> players = new LinkedHashMap<>(16, 0.75F, true);

    public RecentItems(int playerLimit, int itemLimit) {
        if (playerLimit < 1 || itemLimit < 1) throw new IllegalArgumentException("Limits must be positive");
        this.playerLimit = playerLimit;
        this.itemLimit = itemLimit;
    }

    public void remember(P player, I item, S current, BiPredicate<S, S> same, Supplier<S> copy) {
        LinkedHashMap<I, S> items = players.computeIfAbsent(player, key -> new LinkedHashMap<>());
        S old = items.remove(item);
        items.put(item, old != null && same.test(old, current) ? old : copy.get());
        while (items.size() > itemLimit) items.remove(items.keySet().iterator().next());
        while (players.size() > playerLimit) players.remove(players.keySet().iterator().next());
    }

    public List<S> previous(P player, I main, I off) {
        LinkedHashMap<I, S> items = players.get(player);
        if (items == null) return List.of();
        List<S> result = new ArrayList<>();
        for (var entry : items.entrySet())
            if (!entry.getKey().equals(main) && !entry.getKey().equals(off)) result.add(entry.getValue());
        java.util.Collections.reverse(result);
        return List.copyOf(result);
    }

    public void clear() { players.clear(); }
    public int playerCount() { return players.size(); }
}
