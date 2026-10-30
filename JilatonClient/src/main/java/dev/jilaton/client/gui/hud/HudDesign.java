package dev.jilaton.client.gui.hud;

/** Pure HUD geometry shared by the renderer, the editor and the layout tests. */
public final class HudDesign {
    public static final int SEGMENTS = 9;
    public static final int CHIP_GAP = 4;
    public static final int VIOLET = 0x8B5CF6;
    public static final int MAGENTA = 0xEC4899;

    public static final int COMPACT_TARGET_WIDTH = 184;
    public static final int COMPACT_TARGET_HEIGHT = 44;
    public static final int TARGET_MIN_WIDTH = 224;
    public static final int TARGET_MAX_WIDTH = 320;
    public static final int TARGET_HEIGHT = 74;

    public static final int KEYBIND_WIDTH = 152;
    public static final int KEYBIND_TITLE = 19;
    public static final int KEYBIND_ROW = 14;
    public static final int KEYBIND_MAX_ROWS = 6;
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
    public static TargetCard targetCard(int viewportWidth) {
        int width = Math.max(TARGET_MIN_WIDTH, Math.min(TARGET_MAX_WIDTH, Math.round(viewportWidth / 3.0F)));
        return new TargetCard(width, TARGET_HEIGHT, 8, 8, 8, 44, 62, 8, 19, 12, 34,
                45, 4, 51, 55, 11, SEGMENTS);
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
