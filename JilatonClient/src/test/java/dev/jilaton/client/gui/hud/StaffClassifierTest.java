package dev.jilaton.client.gui.hud;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffClassifierTest {
    private static StaffClassifier.Candidate player(String name) {
        return new StaffClassifier.Candidate(name, "", "", null, null);
    }

    private static StaffClassifier.Candidate team(String name, String prefix, String suffix,
                                                  String colorName, Integer colorRgb) {
        return new StaffClassifier.Candidate(name, prefix, suffix, colorName, colorRgb);
    }

    @Test void plainTokensMatchNamePrefixes() {
        List<StaffClassifier.Rule> rules = StaffClassifier.parseRules("ADMIN, OWNER");
        assertEquals(2, rules.size());
        assertEquals(StaffClassifier.Kind.NAME_PREFIX, rules.get(0).kind());
        assertTrue(StaffClassifier.classify(player("AdminBob"), rules).isPresent());
        assertTrue(StaffClassifier.classify(player("owner_amy"), rules).isPresent());
        assertFalse(StaffClassifier.classify(player("BobTheAdmin"), rules).isPresent());
        assertFalse(StaffClassifier.classify(player(""), rules).isPresent());
        assertFalse(StaffClassifier.classify(null, rules).isPresent());
    }

    @Test void containsAndGlobRulesMatchWholeNames() {
        assertTrue(StaffClassifier.classify(player("TheModerator"),
                StaffClassifier.parseRules("contains:mod")).isPresent());
        assertTrue(StaffClassifier.classify(player("Jr_Staff_X"),
                StaffClassifier.parseRules("glob:*staff*")).isPresent());
        assertTrue(StaffClassifier.classify(player("Helper7"),
                StaffClassifier.parseRules("glob:helper?")).isPresent());
        assertFalse(StaffClassifier.classify(player("Helper77"),
                StaffClassifier.parseRules("glob:helper?")).isPresent());
        assertFalse(StaffClassifier.classify(player("AdminBob"), StaffClassifier.parseRules("")).isPresent());
    }

    @Test void teamTagsMatchPrefixOrSuffix() {
        List<StaffClassifier.Rule> rules = StaffClassifier.parseRules("tag:STAFF");
        assertEquals(StaffClassifier.Kind.TEAM_TAG, rules.get(0).kind());
        assertTrue(StaffClassifier.classify(team("alex", "[STAFF] ", "", null, null), rules).isPresent());
        assertTrue(StaffClassifier.classify(team("alex", "", " [staff]", null, null), rules).isPresent());
        assertFalse(StaffClassifier.classify(team("alex", "[BUILD] ", "", null, null), rules).isPresent());
    }

    @Test void teamColoursMatchNamesAndHexValues() {
        List<StaffClassifier.Rule> byName = StaffClassifier.parseRules("color:light_purple");
        assertTrue(StaffClassifier.classify(team("alex", "", "", "light_purple", 0xFF55FF), byName).isPresent());
        assertFalse(StaffClassifier.classify(team("alex", "", "", "gold", 0xFFAA00), byName).isPresent());
        List<StaffClassifier.Rule> byHex = StaffClassifier.parseRules("colour:#FF55FF");
        assertTrue(StaffClassifier.classify(team("alex", "", "", null, 0xFF55FF), byHex).isPresent());
        assertFalse(StaffClassifier.classify(team("alex", "", "", null, 0x55FF55), byHex).isPresent());
        assertFalse(StaffClassifier.classify(team("alex", "", "", null, null), byHex).isPresent());
    }

    @Test void malformedTokensAreSkippedAndUnknownPrefixesStayPlain() {
        List<StaffClassifier.Rule> rules = StaffClassifier.parseRules(" , ADMIN, contains: , weird:Amy");
        assertEquals(2, rules.size());
        assertEquals(StaffClassifier.Kind.NAME_PREFIX, rules.get(0).kind());
        assertEquals("ADMIN", rules.get(0).value());
        assertEquals(StaffClassifier.Kind.NAME_PREFIX, rules.get(1).kind());
        assertTrue(StaffClassifier.classify(player("weird:Amy"), rules).isPresent());
        assertTrue(StaffClassifier.parseRules(null).isEmpty());
    }
}
