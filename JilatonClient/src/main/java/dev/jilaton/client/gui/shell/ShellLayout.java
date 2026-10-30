package dev.jilaton.client.gui.shell;

/**
 * Geometry for the ClickGUI shell: a rounded panel split into a fixed sidebar and a scrolling
 * content column. The design metrics live in {@link ShellMetrics}; this record fits them to the
 * window, shrinking the sidebar rows before anything else when height runs out.
 */
public record ShellLayout(
        int panelX, int panelY, int panelWidth, int panelHeight,
        int sidebarWidth, int headerHeight, int logoHeight,
        int contentX, int contentWidth,
        int titleY, int subtitleY, int listTop, int listBottom,
        int searchX, int searchY, int searchWidth, int searchHeight,
        int navTop, int navStep, int generalTop, int generalStep,
        int footerY, int footerHeight, boolean compact) {

    public static final int NAV_ROWS = 6;
    public static final int GENERAL_ROWS = 3;

    public static ShellLayout compute(int screenWidth, int screenHeight) {
        int screenW = Math.max(1, screenWidth);
        int screenH = Math.max(1, screenHeight);
        int panelW = Math.min(1200, screenW - 28);
        int panelH = Math.min(800, screenH - 28);
        if (panelW < 240) panelW = Math.max(150, screenW - 8);
        if (panelH < 200) panelH = Math.max(120, screenH - 8);
        int panelX = (screenW - panelW) / 2;
        int panelY = (screenH - panelH) / 2;
        boolean compact = panelH < 620 || panelW < 760;

        int pad = panelW < 520 ? 12 : 18;
        int sidebar = ShellMetrics.SIDEBAR_MIN;
        if (panelW >= 760) {
            sidebar = Math.max(ShellMetrics.SIDEBAR_MIN,
                    Math.min(ShellMetrics.SIDEBAR_MAX, Math.round(panelW * 0.17F)));
        }
        int contentW = panelW - sidebar - pad * 2;
        if (contentW < 224) {
            sidebar = Math.max(150, panelW - 224 - pad * 2);
            contentW = Math.max(80, panelW - sidebar - pad * 2);
        }
        int contentX = panelX + sidebar + pad;

        int headerH = ShellMetrics.HEADER_HEIGHT;
        int logoH = compact ? Math.max(40, Math.round(panelH * 0.11F)) : ShellMetrics.LOGO_HEIGHT;
        int userH = compact ? 0 : ShellMetrics.USER_CARD_HEIGHT;
        int labelH = compact ? 18 : ShellMetrics.GROUP_LABEL_HEIGHT;
        int groupGap = compact ? 4 : 8;
        int rows = NAV_ROWS + GENERAL_ROWS;
        int top = Math.max(panelY + headerH, panelY + logoH + labelH);
        int available = panelY + panelH - 4 - userH - top - labelH - groupGap;
        int rowH = Math.max(compact ? 18 : 22, Math.min(ShellMetrics.NAV_ROW_HEIGHT, available / rows));

        int searchH = Math.min(ShellMetrics.SEARCH_HEIGHT, headerH - 12);
        int searchW = Math.max(120, Math.min(ShellMetrics.SEARCH_WIDTH, Math.round(contentW * 0.42F)));
        int searchX = panelX + panelW - pad - searchW;
        int searchY = panelY + (headerH - searchH) / 2;

        int titleY = panelY + headerH + (compact ? 10 : 18);
        int subtitleY = titleY + (compact ? 16 : 24);
        int listTop = subtitleY + (compact ? 12 : 20);
        int listBottom = panelY + panelH - ShellMetrics.FOOTER_HEIGHT;

        int navTop = top;
        int navStep = rowH;
        int generalTop = navTop + NAV_ROWS * rowH + groupGap + labelH;
        int footerY = panelY + panelH - 4 - userH;
        return new ShellLayout(panelX, panelY, panelW, panelH, sidebar, headerH, logoH,
                contentX, contentW, titleY, subtitleY, listTop, listBottom,
                searchX, searchY, searchW, searchH,
                navTop, navStep, generalTop, navStep,
                footerY, userH, compact);
    }

    public int sidebarRight() {
        return panelX + sidebarWidth;
    }

    public int headerBottom() {
        return panelY + headerHeight;
    }

    public int navY(int index) {
        return navTop + index * navStep;
    }

    public int generalY(int index) {
        return generalTop + index * generalStep;
    }

    public int rowHeight() {
        return navStep;
    }

    public int contentRight() {
        return contentX + contentWidth;
    }

    public int listHeight() {
        return Math.max(0, listBottom - listTop);
    }

    public int hintY() {
        return listBottom + 6;
    }
}
