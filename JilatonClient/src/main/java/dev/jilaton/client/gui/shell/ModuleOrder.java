package dev.jilaton.client.gui.shell;

import java.util.ArrayList;
import java.util.List;

/** Session-local card order for the Visuals grid. Dragging the grip moves a module to a new slot. */
public final class ModuleOrder {
    private ModuleOrder() { }

    /** Returns a new order with {@code id} placed at {@code target}, clamped into range. */
    public static List<String> move(List<String> order, String id, int target) {
        List<String> next = new ArrayList<>(order);
        if (!next.remove(id)) return next;
        int index = Math.max(0, Math.min(next.size(), target));
        next.add(index, id);
        return next;
    }

    /** Ids in {@code order} that are present in {@code available}, followed by anything new. */
    public static List<String> apply(List<String> order, List<String> available) {
        List<String> result = new ArrayList<>();
        for (String id : order) {
            if (available.contains(id) && !result.contains(id)) result.add(id);
        }
        for (String id : available) {
            if (!result.contains(id)) result.add(id);
        }
        return result;
    }
}
