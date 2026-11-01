package dev.jilaton.client.module.impl;

import java.util.Set;

/** Which inventory-menu slot to throw next. */
final class InventoryCleanerPolicy {
    private InventoryCleanerPolicy() {
    }

    /** First junk slot in menu order 9..35, then the hotbar 36..44 if allowed; -1 when clean. */
    static int firstJunk(String[] ids, Set<String> junk, boolean hotbar) {
        int last = hotbar ? 44 : 35;
        for (int slot = 9; slot <= last && slot < ids.length; slot++) {
            String id = ids[slot];
            if (id != null && !id.isEmpty() && junk.contains(id)) return slot;
        }
        return -1;
    }
}
