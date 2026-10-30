package dev.jilaton.client.gui.shell;

/**
 * The measured ClickGUI metrics from {@code reference/ui/DIRECTION.md}. These are the design values;
 * {@link ShellLayout} uses them whenever the window has room and scales down only when it does not.
 */
public final class ShellMetrics {
    public static final int SIDEBAR_MIN = 200;
    public static final int SIDEBAR_MAX = 220;
    public static final int LOGO_HEIGHT = 72;
    public static final int NAV_ROW_HEIGHT = 56;
    public static final int HEADER_HEIGHT = 64;
    public static final int SEARCH_WIDTH = 260;
    public static final int SEARCH_HEIGHT = 32;
    public static final int MODULE_CARD_HEIGHT = 64;
    public static final int MODULE_COLUMNS = 2;
    public static final int VISUAL_CARD_WIDTH = 230;
    public static final int VISUAL_CARD_HEIGHT = 90;
    public static final int VISUAL_COLUMNS = 4;
    public static final int COLUMN_GAP = 10;
    public static final int ROW_GAP = 10;
    public static final int USER_CARD_HEIGHT = 60;
    public static final int GROUP_LABEL_HEIGHT = 22;
    public static final int FOOTER_HEIGHT = 22;

    private ShellMetrics() { }

    /** Two module columns once the content column can hold two readable cards. */
    public static int moduleColumns(int contentWidth) {
        return contentWidth >= 340 ? MODULE_COLUMNS : 1;
    }

    /** Four preview cards at the reference width, stepping down only when the column cannot fit them. */
    public static int visualColumns(int contentWidth) {
        if (contentWidth >= VISUAL_COLUMNS * 200 + COLUMN_GAP * (VISUAL_COLUMNS - 1)) return VISUAL_COLUMNS;
        if (contentWidth >= 3 * 180 + COLUMN_GAP * 2) return 3;
        if (contentWidth >= 2 * 160 + COLUMN_GAP) return 2;
        return 1;
    }

    public static int cardWidth(int contentWidth, int columns) {
        int safe = Math.max(1, columns);
        return Math.max(60, (contentWidth - COLUMN_GAP * (safe - 1)) / safe);
    }
}
