package dev.jilaton.client.gui.hud;

import org.junit.jupiter.api.Test;

import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for the fixed-advance numeric model. The advance source is a stand-in for Minecraft's
 * proportional font, where '1' is narrower than '0' or '8'.
 */
class HudNumbersTest {
    private static final IntUnaryOperator PROPORTIONAL = character -> switch (character) {
        case '0', '4', '6', '8', '9' -> 6;
        case '1' -> 3;
        case '2', '5' -> 5;
        case '3', '7' -> 4;
        case '.' -> 2;
        case '-' -> 4;
        default -> 4;
    };

    private static HudNumbers numbers() {
        return new HudNumbers(PROPORTIONAL);
    }

    @Test
    void everyDigitSharesTheWidestAdvancePlusTracking() {
        HudNumbers numbers = numbers();
        assertEquals(6 + HudNumbers.TRACKING, numbers.digitAdvance());
        for (char digit = '0'; digit <= '9'; digit++) {
            assertEquals(numbers.digitAdvance(), numbers.width(String.valueOf(digit)),
                    "digit " + digit + " must occupy one tabular slot");
        }
        assertEquals(numbers.width("0"), numbers.width("1"));
        assertEquals(numbers.width("0"), numbers.width("8"));
    }

    @Test
    void repeatedDigitsMultiplyTheAdvanceLinearly() {
        HudNumbers numbers = numbers();
        assertEquals(numbers.digitAdvance(), numbers.width("1"));
        assertEquals(3 * numbers.digitAdvance(), numbers.width("111"));
        assertEquals(2 * numbers.digitAdvance(), numbers.width("10"));
    }

    @Test
    void signsAndDecimalPointsKeepTheirNaturalWidth() {
        HudNumbers numbers = numbers();
        assertEquals(2 * numbers.digitAdvance() + 2, numbers.width("1.5"));
        assertEquals(numbers.digitAdvance() + 4, numbers.width("-1"));
        assertEquals(8, numbers.width("--"));
    }

    @Test
    void rightAlignedValueKeepsItsRightEdgeAcrossADigitBoundary() {
        HudNumbers numbers = numbers();
        assertEquals(200, numbers.rightAlignedX("9", 200) + numbers.width("9"));
        assertEquals(200, numbers.rightAlignedX("10", 200) + numbers.width("10"));
        assertEquals(numbers.digitAdvance(),
                numbers.rightAlignedX("9", 200) - numbers.rightAlignedX("10", 200));
    }

    @Test
    void columnsAreMeasuredInDigitSlots() {
        HudNumbers numbers = numbers();
        assertEquals(3 * numbers.digitAdvance(), numbers.columnWidth(3));
        assertEquals(0, numbers.columnWidth(0));
        assertEquals(0, numbers.columnWidth(-4));
        assertEquals(0, numbers.width(""));
        assertEquals(0, numbers.width(null));
    }

    @Test
    void aWiderGlyphWidensEveryDigitEqually() {
        HudNumbers wide = new HudNumbers(character -> character == '8' ? 9 : 3);
        assertEquals(9 + HudNumbers.TRACKING, wide.digitAdvance());
        assertEquals(wide.digitAdvance(), wide.width("1"));
        assertEquals(wide.digitAdvance(), wide.width("8"));
        assertTrue(wide.width("111") == 3 * wide.digitAdvance());
        assertFalse(HudNumbers.isDigit('.'), "decimal points are not tabular digits");
    }
}
