package dev.jilaton.client.render;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class NametagBehaviorTest {
    @Test void compactHealthSupportsHeartsAndRawHpWithoutInventingValues() {
        assertEquals("10", NametagLayout.healthText(20, true));
        assertEquals("9.5", NametagLayout.healthText(19, true));
        assertEquals("19", NametagLayout.healthText(19, false));
        assertEquals("0", NametagLayout.healthText(0, true));
        assertEquals("", NametagLayout.healthText(Float.NaN, true));
    }
    @Test void recentItemsAreMostRecentFirstAndExcludeCurrentlyHeldTypes() {
        RecentItems<String, String, String> history = new RecentItems<>(128, 9);
        remember(history, "alex", "sword", "Sword");
        remember(history, "alex", "bow", "Bow");
        remember(history, "alex", "totem", "Totem");
        assertEquals(List.of("Totem", "Bow"), history.previous("alex", "sword", "air"));
        remember(history, "alex", "bow", "Enchanted Bow");
        assertEquals(List.of("Enchanted Bow", "Totem"), history.previous("alex", "sword", "air"));
        assertEquals(List.of("Sword"), history.previous("alex", "bow", "totem"));
    }

    @Test void historyEvictsOldItemsAndLeastRecentlyAccessedPlayers() {
        RecentItems<String, String, String> history = new RecentItems<>(2, 2);
        remember(history, "alex", "a", "A");
        remember(history, "alex", "b", "B");
        remember(history, "alex", "c", "C");
        assertEquals(List.of("C", "B"), history.previous("alex", "air", "air"));
        remember(history, "sam", "x", "X");
        history.previous("alex", "air", "air");
        remember(history, "lee", "z", "Z");
        assertEquals(2, history.playerCount());
        assertTrue(history.previous("sam", "air", "air").isEmpty());
        history.clear();
        assertTrue(history.previous("alex", "air", "air").isEmpty());
    }

    @Test void unchangedEquipmentReusesSnapshotWithoutCopying() {
        RecentItems<String, String, String> history = new RecentItems<>(2, 2);
        AtomicInteger copies = new AtomicInteger();
        for (int i = 0; i < 10; i++) history.remember("alex", "a", "A", String::equals, () -> {
            copies.incrementAndGet();
            return "A";
        });
        assertEquals(1, copies.get());
    }

    @Test void panelWidthsMatchIconSpacingAndOptionalSections() {
        assertEquals(0, NametagLayout.iconsWidth(0));
        assertEquals(10, NametagLayout.iconsWidth(1));
        assertEquals(22, NametagLayout.iconsWidth(2));
        int row = NametagLayout.append(0, 22, 0);
        row = NametagLayout.append(row, 46, 7);
        row = NametagLayout.append(row, 0, 4);
        assertEquals(75, row);
        assertEquals(99, NametagLayout.append(row, 20, 4));
    }

    @Test void distanceScaleIsBoundedAndHealthPingThresholdsAreExplicit() {
        assertEquals(1, NametagLayout.scale(4, 1, 0.55));
        assertEquals(0.55, NametagLayout.scale(200, 1, 0.55), 0.001);
        assertEquals(2, NametagLayout.scale(0, 9, 0.55));
        assertEquals(0, NametagLayout.scale(Double.NaN, 1, 0.55));
        assertEquals(0xFF69E59B, NametagLayout.healthColor(20, 20));
        assertEquals(0xFFFFBB68, NametagLayout.healthColor(10, 20));
        assertEquals(0xFFFF788A, NametagLayout.healthColor(3, 20));
        assertEquals(0xFF9098AD, NametagLayout.pingColor(-1));
        assertEquals(0xFF69E59B, NametagLayout.pingColor(59));
        assertEquals(0xFFFFBB68, NametagLayout.pingColor(60));
        assertEquals(0xFFFF788A, NametagLayout.pingColor(130));
    }

    private void remember(RecentItems<String, String, String> history, String player, String item, String value) {
        history.remember(player, item, value, String::equals, () -> value);
    }
}
