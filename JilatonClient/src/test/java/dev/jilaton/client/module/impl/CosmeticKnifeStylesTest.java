package dev.jilaton.client.module.impl;

import dev.jilaton.client.render.model.asset.ModelAssets;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmeticKnifeStylesTest {
    private static ChoiceSetting style() {
        for (Setting setting : new CosmeticKnifeModule().settings()) {
            if (setting.id().equals("style") && setting instanceof ChoiceSetting choice) return choice;
        }
        throw new AssertionError("CosmeticKnifeModule has no style choice");
    }

    @Test
    void everyKnifeAssetIsSelectable() {
        List<String> choices = style().choices().stream()
                .filter(choice -> !choice.startsWith(CosmeticKnifeModule.CS_PREFIX)).toList();
        assertEquals(ModelAssets.knifeIds().size(), choices.size(),
                "a knife asset has no style: " + ModelAssets.knifeIds());
        for (String id : ModelAssets.knifeIds()) {
            assertTrue(choices.stream().anyMatch(choice -> choice.toLowerCase(Locale.ROOT).equals(id)),
                    id + " is unreachable in the GUI");
        }
    }

    @Test
    void everyStyleResolvesThroughModelAssets() {
        for (String choice : style().choices()) {
            if (choice.startsWith(CosmeticKnifeModule.CS_PREFIX)) continue;
            assertNotNull(ModelAssets.knife(choice.toLowerCase(Locale.ROOT)),
                    choice + " does not resolve to a model asset");
        }
    }

    @Test
    void everyCsSkinIsSelectableAndKnownToThePortedEngine() {
        List<String> choices = style().choices();
        for (String skin : CosmeticKnifeModule.CS_STYLES) {
            assertTrue(choices.contains(skin), skin + " is unreachable in the GUI");
            assertTrue(dev.jilaton.client.render.knife.CsKnifeEngine.hasSkin(
                    skin.substring(CosmeticKnifeModule.CS_PREFIX.length())), skin + " has no ported skin");
        }
        assertTrue(choices.contains("CS Katana"), "the two-handed katana is offered");
    }

    @Test
    void theEmptyHandAlwaysGetsTheBlade() {
        assertTrue(CosmeticKnifeModule.occupies(true, false, false, "Swords", false),
                "no held item still draws the Jilaton knife");
        assertTrue(CosmeticKnifeModule.occupies(true, false, false, "Swords", true),
                "no held item still draws the katana");
        assertTrue(CosmeticKnifeModule.occupies(true, true, false, "Any item", false));
    }

    @Test
    void theReplaceModesStillDecideWhichItemsAreTakenOver() {
        assertTrue(CosmeticKnifeModule.occupies(false, true, false, "Swords", false));
        assertFalse(CosmeticKnifeModule.occupies(false, false, true, "Swords", false));
        assertTrue(CosmeticKnifeModule.occupies(false, false, true, "Swords & axes", false));
        assertTrue(CosmeticKnifeModule.occupies(false, false, false, "Any item", false));
        assertFalse(CosmeticKnifeModule.occupies(false, true, false, "Swords", true),
                "CS skins keep the item path for held items");
    }

    @Test
    void theReplaceChoicesNoLongerContradictTheEmptyHand() {
        ChoiceSetting holdOn = null;
        for (Setting setting : new CosmeticKnifeModule().settings()) {
            if (setting.id().equals("hold_on")) holdOn = (ChoiceSetting) setting;
        }
        assertNotNull(holdOn);
        assertTrue(holdOn.choices().stream().noneMatch(choice -> choice.equalsIgnoreCase("Empty hand")),
                "the empty hand is unconditional now, so a separate choice would do nothing");
    }
}
