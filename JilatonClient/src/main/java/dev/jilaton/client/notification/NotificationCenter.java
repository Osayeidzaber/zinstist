package dev.jilaton.client.notification;

import java.util.ArrayDeque;
import java.util.List;

/** Small, expiring client-thread queue. Repeated changes to one module replace its previous toast. */
public final class NotificationCenter {
    public static final long LIFETIME_NANOS = 4_000_000_000L;
    private final ArrayDeque<Notice> notices = new ArrayDeque<>();

    public void push(String id, String text, boolean enabled, long now) {
        expire(now);
        notices.removeIf(notice -> notice.id().equals(id));
        while (notices.size() >= 4) notices.removeFirst();
        notices.addLast(new Notice(id, text, enabled, now));
    }

    public List<Notice> current(long now) {
        expire(now);
        return List.copyOf(notices);
    }

    private void expire(long now) {
        notices.removeIf(notice -> now - notice.createdAt() >= LIFETIME_NANOS);
    }

    public void clear() { notices.clear(); }

    public record Notice(String id, String text, boolean enabled, long createdAt) { }
}
