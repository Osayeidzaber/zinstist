package dev.jilaton.client.render;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class OverlayPolicyTest {
    @Test void itemIdsNormalizeAndDeduplicateWithoutExpandingWhitelistToAllItems() {
        assertEquals("minecraft:diamond, minecraft:apple", ItemIdFilter.normalize("diamond, minecraft:diamond apple"));
        assertFalse(ItemIdFilter.allows("minecraft:apple", Set.of(), true));
        assertTrue(ItemIdFilter.allows("minecraft:apple", Set.of(), false));
        Set<String> ids = ItemIdFilter.parse("diamond");
        assertTrue(ItemIdFilter.allows("minecraft:diamond", ids, true));
        assertFalse(ItemIdFilter.allows("minecraft:diamond", ids, false));
        assertThrows(UnsupportedOperationException.class, () -> ids.add("minecraft:apple"));
        assertThrows(IllegalArgumentException.class, () -> ItemIdFilter.parse("minecraft:Apple"));
        assertThrows(IllegalArgumentException.class, () -> ItemIdFilter.parse("invalid:namespace:item"));
    }

    @Test void visualFiltersKeepFriendsSelectableButRespectExplicitExclusions() {
        assertTrue(eligible(true, false, false, true, false, 4, true, false, false, true, false));
        assertFalse(eligible(true, false, false, true, false, 4, true, false, false, false, false));
        assertFalse(eligible(true, false, true, false, false, 4, true, true, true, true, true));
        assertFalse(eligible(true, false, false, false, true, 4, true, true, true, true, false));
        assertTrue(eligible(false, true, false, false, false, 4, false, true, false, true, false));
        assertFalse(eligible(false, true, false, false, false, 4, true, false, true, true, false));
        assertFalse(eligible(true, false, false, false, false, Double.NaN, true, true, true, true, true));
        assertFalse(eligible(true, false, false, false, false, 1025, true, true, true, true, true));
    }

    private boolean eligible(boolean player, boolean hostile, boolean spectator, boolean friend, boolean invisible,
                             double distance, boolean players, boolean hostiles, boolean passive, boolean friends, boolean invisibles) {
        return LivingOverlayOptions.eligible(player,hostile,spectator,friend,invisible,distance,players,hostiles,passive,friends,invisibles,32);
    }

    @Test void healthColorsAreBoundedAndFriendHurtInvisiblePrecedenceIsStable() {
        assertEquals(0xFF00FF00, OverlayColors.resolve("Health", 0, 40, 20, 0, 0, false, false, false));
        assertEquals(0xFFFF0000, OverlayColors.resolve("Health", 0, -1, 20, 0, 0, false, false, false));
        assertEquals(0xFF4B8DFF, OverlayColors.resolve("Theme", 0, 1,1,0,0,true,false,false));
        assertEquals(0xFFFF556C, OverlayColors.resolve("Theme", 0, 1,1,0,0,true,true,false));
        assertEquals(0xFFFFAD52, OverlayColors.resolve("Theme", 0, 1,1,0,0,true,false,true));
        assertEquals(OverlayColors.resolve("Rainbow", 0,1,1,0,0,false,false,false), OverlayColors.resolve("Rainbow",0,1,1,0,6,false,false,false));
    }
}
