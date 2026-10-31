package dev.jilaton.client.gui.hud;

/** Pure HUD geometry shared by the renderer, the editor and the layout tests. */
public final class HudDesign {
    public static final int SEGMENTS = 9;
    public static final int CHIP_GAP = 4;
    public static final int VIOLET = 0xF5F5F5;
    public static final int MAGENTA = 0xA8A8A8;

    public static final int COMPACT_TARGET_WIDTH = 184;
    public static final int COMPACT_TARGET_HEIGHT = 44;
    public static final int TARGET_MIN_WIDTH = 224;
    public static final int TARGET_MAX_WIDTH = 320;
    /** Geometry the target card is laid out at; one factor scales the card and its text together. */
    public static final int TARGET_BASE_WIDTH = TARGET_MAX_WIDTH;
    public static final int TARGET_HEIGHT = 74;
    /** Room the held-item icon and its inset take inside the pill. */
    public static final int PILL_TEXT_INSET = 17;

    public static final int KEYBIND_WIDTH = 152;
    public static final int KEYBIND_TITLE = 19;
    public static final int KEYBIND_ROW = 14;
    public static final int KEYBIND_MAX_ROWS = 6;
    /** One enabled-module row in the right-column list. */
    public static final int MODULE_ROW = 12;
    public static final int EFFECTS_WIDTH = 148;
    public static final int EFFECTS_TITLE = 21;
    public static final int EFFECTS_ROW = 14;
    public static final int CPS_HEIGHT = 22;
    public static final int ARMOR_WIDTH = 130;
    public static final int ARMOR_HEIGHT = 50;
    public static final int KEYSTROKES_WIDTH = 70;
    public static final int KEYSTROKES_HEIGHT = 58;
    /** Clearance between two panels that share a column. */
    public static final int STACK_GAP = 6;
    /** Margin a bottom-anchored column keeps from the screen edge. */
    public static final int HUD_MARGIN = 8;
    public static final int SERVER_WIDTH = 148;
    public static final int SERVER_HEIGHT = 66;
    public static final int SERVER_ROW = 12;
    public static final int HELD_ITEM_WIDTH = 132;
    public static final int HELD_ITEM_HEIGHT = 58;
    public static final int HELD_ITEM_SCALE = 2;
    public static final int HELD_ITEM_LABEL_X = 46;
    public static final int HELD_ITEM_PADDING = 8;
    public static final int ABOVE_HEAD_WIDTH = 128;
    public static final int ABOVE_HEAD_HEIGHT = 26;
    public static final int STAFF_WIDTH = 172;
    public static final int STAFF_TITLE = 20;
    public static final int STAFF_ROW = 13;
    public static final int STAFF_MAX_ROWS = 8;
    public static final int STAFF_PADDING = 7;
    public static final int RADAR_MIN_SIZE = 40;
    public static final int RADAR_MAX_SIZE = 96;
    public static final int RADAR_DEFAULT_SIZE = 48;
    public static final int RADAR_MIN_RANGE = 16;
    public static final int RADAR_MAX_RANGE = 64;
    public static final int RADAR_DEFAULT_RANGE = 32;
    public static final int RADAR_RING_INSET = 4;
    /** Footer strip under the radar ring that carries the configured range. */
    public static final int RADAR_FOOTER = 10;

    private HudDesign() { }

    public record TargetCard(int width, int height, int padding, int headX, int headY, int headSize,
                             int contentX, int nameY, int pillY, int pillHeight, int healthY,
                             int barY, int barHeight, int ruleY, int chipsY, int chipHeight,
                             int segments) {
        public int contentWidth() { return width - contentX - padding; }
        public int contentRight() { return width - padding; }
        public int chipWidth() { return (contentWidth() - 2 * CHIP_GAP) / 3; }
        public int chipX(int index) { return contentX + index * (chipWidth() + CHIP_GAP); }

        /** Every rect of this card multiplied by one factor; text is drawn in the same scaled space. */
        public TargetCard scaled(double factor) {
            if (!Double.isFinite(factor) || factor <= 0) {
                throw new IllegalArgumentException("Card scale must be positive");
            }
            int scaledPadding = mul(padding, factor);
            int scaledRule = mul(ruleY, factor);
            int scaledChips = chipsY < 0 ? -1 : mul(chipsY, factor);
            int bottom = scaledChips >= 0 ? scaledChips + mul(chipHeight, factor) : scaledRule + 1;
            return new TargetCard(mul(width, factor),
                    Math.max(mul(height, factor), bottom + scaledPadding),
                    scaledPadding, mul(headX, factor), mul(headY, factor),
                    mul(headSize, factor), mul(contentX, factor), mul(nameY, factor),
                    mul(pillY, factor), mul(pillHeight, factor), mul(healthY, factor),
                    mul(barY, factor), mul(barHeight, factor), scaledRule,
                    scaledChips, mul(chipHeight, factor), segments);
        }
    }

    private static int mul(int value, double factor) {
        return (int) Math.round(value * factor);
    }

    /** The compact card's fixed anchors, so scaling it is one multiplication rather than eight literals. */
    public record CompactCard(int width, int height, int faceX, int faceY, int faceSize, int textX,
                              int nameY, int heartsY, int barY, int barHeight, int statusY,
                              int rightPadding) {
        public CompactCard scaled(double factor) {
            if (!Double.isFinite(factor) || factor <= 0) {
                throw new IllegalArgumentException("Card scale must be positive");
            }
            return new CompactCard(mul(width, factor), Math.max(mul(statusY + 9, factor),
                    mul(height, factor)), mul(faceX, factor), mul(faceY, factor),
                    mul(faceSize, factor), mul(textX, factor), mul(nameY, factor),
                    mul(heartsY, factor), mul(barY, factor), mul(barHeight, factor),
                    mul(statusY, factor), mul(rightPadding, factor));
        }
    }

    public record Bar(int x, int y, int width, int height, int segments, int gap) {
        public int segmentLeft(int index) {
            return x + Math.round(width * (float) index / segments);
        }

        public int segmentRight(int index) {
            return x + Math.round(width * (float) (index + 1) / segments)
                    - (index == segments - 1 ? 0 : gap);
        }

        public int segmentWidth(int index) {
            return Math.max(0, segmentRight(index) - segmentLeft(index));
        }
    }

    /** The detailed card is about a third of the viewport wide, within a readable range. */
    public static int targetCardWidth(int viewportWidth) {
        return Math.max(TARGET_MIN_WIDTH, Math.min(TARGET_MAX_WIDTH, Math.round(viewportWidth / 3.0F)));
    }

    public static TargetCard targetCard(int viewportWidth) {
        return targetCard(targetCardWidth(viewportWidth), true, true);
    }

    /** The renderer's layout card: fixed base width, rows packed around the visible elements. */
    public static TargetCard baseTargetCard(boolean chips, boolean held) {
        return targetCard(TARGET_BASE_WIDTH, chips, held);
    }

    public static TargetCard targetCard(int width, boolean chips, boolean held) {
        int clamped = Math.max(TARGET_MIN_WIDTH, Math.min(TARGET_MAX_WIDTH, width));
        int pillY = held ? 19 : 20;
        int pillHeight = held ? 12 : 0;
        int healthY = held ? 34 : 20;
        int barY = healthY + 11;
        int ruleY = barY + 6;
        int chipsY = chips ? ruleY + 4 : -1;
        int chipHeight = 11;
        int height = chips ? chipsY + chipHeight + 8 : ruleY + 1 + 8;
        return new TargetCard(clamped, height, 8, 8, 8, 44, 62, 8, pillY, pillHeight, healthY,
                barY, 4, ruleY, chipsY, chipHeight, SEGMENTS);
    }

    /** The compact card's base geometry; the renderer scales it by one factor. */
    public static CompactCard compactTarget() {
        return new CompactCard(COMPACT_TARGET_WIDTH, COMPACT_TARGET_HEIGHT, 7, 7, 24, 38, 6, 17, 30, 3, 35, 7);
    }

    /**
     * Width of the held-item pill for a measured label. A label that fits the content column gets its
     * own width; only a label that genuinely exceeds it is given the full column and ellipsized.
     */
    public static int pillWidth(int textWidth, int contentWidth) {
        if (contentWidth <= 0 || textWidth <= 0) {
            return 0;
        }
        if (pillTextFits(textWidth, contentWidth)) {
            return Math.max(1, Math.min(contentWidth, textWidth + 20));
        }
        return contentWidth;
    }

    public static boolean pillTextFits(int textWidth, int contentWidth) {
        return textWidth <= contentWidth - PILL_TEXT_INSET;
    }

    public static Bar healthBar(TargetCard card) {
        return new Bar(card.contentX(), card.barY(), card.contentWidth(), card.barHeight(),
                card.segments(), 2);
    }

    /** Fraction of one segment that is lit; the sum of all segments equals the health ratio. */
    public static double segmentFill(int index, double ratio) {
        double scaled = Math.max(0, Math.min(1, ratio)) * SEGMENTS;
        return Math.max(0, Math.min(1, scaled - index));
    }

    public static int segmentColor(int index, int segments) {
        if (segments <= 1) {
            return VIOLET;
        }
        return blend(VIOLET, MAGENTA, index / (double) (segments - 1));
    }

    public static int blend(int from, int to, double amount) {
        double t = Math.max(0, Math.min(1, amount));
        int red = (int) Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int green = (int) Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int blue = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (red << 16) | (green << 8) | blue;
    }

    public static int keybindHeight(int rows) {
        return KEYBIND_TITLE + Math.max(0, Math.min(KEYBIND_MAX_ROWS, rows)) * KEYBIND_ROW + 2;
    }

    /** Height of the enabled-module list for a count of rows. */
    public static int moduleListHeight(int rows) {
        return Math.max(0, rows) * MODULE_ROW;
    }

    /** Height of the potion panel: one title row plus one row per effect. */
    public static int effectsHeight(int rows) {
        return EFFECTS_TITLE + Math.max(0, rows) * EFFECTS_ROW;
    }

    /**
     * Tops of panels stacked downward from {@code top}, ordered top to bottom. Each panel consumes its
     * own height plus one gap, so two neighbours can never share a row.
     */
    public static int[] stackDown(int top, int gap, int... heights) {
        int[] tops = new int[heights.length];
        int cursor = top;
        for (int index = 0; index < heights.length; index++) {
            tops[index] = cursor;
            cursor += Math.max(0, heights[index]) + Math.max(0, gap);
        }
        return tops;
    }

    /**
     * Tops of panels stacked upward against {@code bottom}, ordered top to bottom. The last entry sits
     * at the bottom edge, so growing a panel (a larger radar) travels upward instead of over its
     * neighbour.
     */
    public static int[] stackUp(int bottom, int gap, int... heights) {
        int total = Math.max(0, gap) * Math.max(0, heights.length - 1);
        for (int height : heights) {
            total += Math.max(0, height);
        }
        int cursor = bottom - total;
        int[] tops = new int[heights.length];
        for (int index = 0; index < heights.length; index++) {
            tops[index] = cursor;
            cursor += Math.max(0, heights[index]) + Math.max(0, gap);
        }
        return tops;
    }

    public static int chipTextWidth(TargetCard card) {
        return Math.max(0, card.chipWidth() - 8);
    }

    public static int staffHeight(int rows) {
        return STAFF_TITLE + Math.max(0, Math.min(STAFF_MAX_ROWS, rows)) * STAFF_ROW + STAFF_PADDING;
    }

    public static int staffRowY(int index) {
        return STAFF_TITLE + Math.max(0, index) * STAFF_ROW;
    }

    public static int radarSize(double requested) {
        if (!Double.isFinite(requested)) {
            return RADAR_DEFAULT_SIZE;
        }
        return Math.max(RADAR_MIN_SIZE, Math.min(RADAR_MAX_SIZE, (int) Math.round(requested)));
    }

    public static int radarRadius(int size) {
        return Math.max(1, size / 2 - RADAR_RING_INSET);
    }

    /** The ring keeps its square; the range footer is appended below it. */
    public static int radarPanelHeight(int size) {
        return Math.max(1, size) + RADAR_FOOTER;
    }

    public static int withAlpha(int color, double presence) {
        int alpha = (int) Math.round(((color >>> 24) & 0xFF) * Math.max(0, Math.min(1, presence)));
        return (alpha << 24) | (color & 0xFFFFFF);
    }
}
