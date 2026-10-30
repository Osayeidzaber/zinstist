package dev.jilaton.client.render;

import java.util.LinkedHashSet;
import java.util.Set;

/** Normalized item identifiers, matching LiquidBounce's whitelist/blacklist semantics. */
public final class ItemIdFilter {
    private ItemIdFilter() { }

    public static Set<String> parse(String input) {
        Set<String> ids = new LinkedHashSet<>();
        if (input.isBlank()) return Set.of();
        for (String token : input.trim().split("[,\\s]+")) {
            String id = token.contains(":") ? token : "minecraft:" + token;
            if (!id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) throw new IllegalArgumentException("Use item IDs separated by commas");
            ids.add(id);
            if (ids.size() > 128) throw new IllegalArgumentException("At most 128 item IDs");
        }
        return java.util.Collections.unmodifiableSet(ids);
    }

    public static String normalize(String input) { return String.join(", ", parse(input)); }
    public static boolean allows(String id, Set<String> ids, boolean whitelist) { return whitelist == ids.contains(id); }
}
