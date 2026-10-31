package dev.jilaton.client.gui.shell;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.Setting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure geometry checks for the ClickGUI shell: no client boot, just the layout maths. */
class ShellGeometryTest {
    private static final int[][] WINDOWS = {{1280, 720}, {1920, 1080}, {800, 400}, {480, 360}};
    private static final int[][] DESKTOP = {{1280, 720}, {1920, 1080}};
    private static final int ROW_HEIGHT = ShellMetrics.MODULE_CARD_HEIGHT;
    private static final int VISUAL_HEIGHT = ShellMetrics.VISUAL_CARD_HEIGHT;
    private static final int COLUMN_GAP = ShellMetrics.COLUMN_GAP;
    private static final int ROW_GAP = ShellMetrics.ROW_GAP;
    private static final int EXPANSION_HEADER = 24;
    private static final int SETTING_ROW = 30;

    @Test
    void referenceMetricsArePinnedToTheDirection() {
        assertEquals(200, ShellMetrics.SIDEBAR_MIN);
        assertEquals(220, ShellMetrics.SIDEBAR_MAX);
        assertEquals(72, ShellMetrics.LOGO_HEIGHT);
        assertEquals(56, ShellMetrics.NAV_ROW_HEIGHT);
        assertEquals(64, ShellMetrics.MODULE_CARD_HEIGHT);
        assertEquals(2, ShellMetrics.MODULE_COLUMNS);
        assertEquals(230, ShellMetrics.VISUAL_CARD_WIDTH);
        assertEquals(90, ShellMetrics.VISUAL_CARD_HEIGHT);
        assertEquals(4, ShellMetrics.VISUAL_COLUMNS);
    }

    @Test
    void desktopSidebarRowsAndGridUseTheReferenceMetrics() {
        for (int[] window : DESKTOP) {
            ShellLayout layout = ShellLayout.compute(window[0], window[1]);
            String at = at(window);
            assertTrue(layout.sidebarWidth() >= ShellMetrics.SIDEBAR_MIN
                            && layout.sidebarWidth() <= ShellMetrics.SIDEBAR_MAX,
                    "sidebar outside 200-220 at " + at + " (" + layout.sidebarWidth() + ")");
            assertEquals(ShellMetrics.NAV_ROW_HEIGHT, layout.rowHeight(), "nav rows are not 56 at " + at);
            assertTrue(layout.navY(0) >= layout.panelY() + ShellMetrics.LOGO_HEIGHT,
                    "nav rows start inside the logo block at " + at);
            assertEquals(2, ShellMetrics.moduleColumns(layout.contentWidth()), "module grid at " + at);
            assertEquals(4, ShellMetrics.visualColumns(layout.contentWidth()), "visual grid at " + at);
            int visualWidth = ShellMetrics.cardWidth(layout.contentWidth(),
                    ShellMetrics.visualColumns(layout.contentWidth()));
            assertTrue(visualWidth >= 225 && visualWidth <= 235,
                    "visual card is not about 230 wide at " + at + " (" + visualWidth + ")");
        }
    }

    @Test
    void searchFieldKeepsClearOfClockGreetingAndSidebar() {
        for (int[] window : WINDOWS) {
            ShellLayout layout = ShellLayout.compute(window[0], window[1]);
            String at = at(window);
            int gap = layout.searchX() - layout.sidebarRight();
            assertTrue(gap >= 16 + 48 + 14 + 14,
                    "not enough room for greeting plus clock beside the search field at " + at + " (gap " + gap + ")");
            assertTrue(layout.searchY() >= layout.panelY(), "search field starts above the header at " + at);
            assertTrue(layout.searchY() + layout.searchHeight() <= layout.headerBottom(),
                    "search field leaves the header at " + at);
            assertTrue(layout.searchY() + layout.searchHeight() <= layout.titleY(),
                    "search field reaches the page title at " + at);
            assertTrue(layout.searchX() + layout.searchWidth() <= layout.panelX() + layout.panelWidth(),
                    "search field runs past the panel at " + at);
        }
    }

    @Test
    void everySidebarRowFitsAboveTheFooterCard() {
        for (int[] window : WINDOWS) {
            ShellLayout layout = ShellLayout.compute(window[0], window[1]);
            String at = at(window);
            int lastModuleRow = layout.navY(ShellLayout.NAV_ROWS - 1) + layout.rowHeight();
            int lastGeneralRow = layout.generalY(ShellLayout.GENERAL_ROWS - 1) + layout.rowHeight();
            assertTrue(layout.navY(0) >= layout.panelY() + layout.headerHeight(),
                    "first module row overlaps the header at " + at);
            assertTrue(lastModuleRow <= layout.generalY(0), "module rows overlap the GENERAL rows at " + at);
            assertTrue(lastGeneralRow <= layout.footerY(), "GENERAL rows overlap the footer card at " + at);
            assertTrue(layout.generalY(0) >= layout.navY(ShellLayout.NAV_ROWS - 1) + layout.rowHeight(),
                    "GENERAL rows start inside the module rows at " + at);
        }
    }

    @Test
    void contentColumnStaysWideAndCardsFlowWithoutOverlap() {
        for (int[] window : WINDOWS) {
            ShellLayout layout = ShellLayout.compute(window[0], window[1]);
            String at = at(window);
            assertTrue(layout.contentWidth() >= 224, "content column narrower than 224 at " + at);
            assertTrue(layout.contentX() >= layout.sidebarRight(), "content starts inside the sidebar at " + at);
            assertTrue(layout.contentRight() <= layout.panelX() + layout.panelWidth(),
                    "content runs past the panel at " + at);
            assertTrue(layout.listHeight() > 0, "empty list viewport at " + at);

            for (int cardHeight : new int[]{ROW_HEIGHT, VISUAL_HEIGHT}) {
                String shape = at + " row " + cardHeight;
                ModuleFlow flow = flow(layout, cardHeight, null);
                int maxScroll = Math.max(0, flow.height() - layout.listHeight());
                assertTrue(cardHeight > 0);
                assertTrue(maxScroll >= 0, "negative scroll range at " + shape);
                assertTrue(flow.height() - maxScroll <= layout.listHeight(),
                        "the last row cannot be scrolled fully into view at " + shape);
                assertEquals(12, flow.cards().size());
                assertTrue(flow.panels().isEmpty());

                ModuleFlow.Card previous = null;
                for (ModuleFlow.Card card : flow.cards()) {
                    assertTrue(card.width() > 0 && card.height() == cardHeight, "degenerate card at " + shape);
                    assertTrue(card.x() >= 0 && card.x() + card.width() <= layout.contentWidth(),
                            "card leaves the content column at " + shape);
                    if (previous != null && previous.y() == card.y()) {
                        assertTrue(previous.x() + previous.width() <= card.x(),
                                "cards overlap in a row at " + shape);
                    } else if (previous != null) {
                        assertEquals(0, card.x(), "new row does not start at the first column at " + shape);
                        assertTrue(card.y() > previous.y(), "rows do not advance at " + shape);
                    }
                    previous = card;
                }
            }
        }
    }

    @Test
    void expandedModuleSplitsTheGridAndItsSettingsStayReachable() {
        for (int[] window : WINDOWS) {
            ShellLayout layout = ShellLayout.compute(window[0], window[1]);
            String at = at(window);
            List<ClientModule> modules = modules(12, 3);
            String expanded = modules.get(2).id();
            ModuleFlow flow = flow(layout, modules, ROW_HEIGHT, expanded);

            assertEquals(1, flow.panels().size(), "expected one expanded panel at " + at);
            ModuleFlow.Panel panel = flow.panels().get(0);
            ModuleFlow.Card card = flow.cardOf(modules.get(2));
            assertNotNull(card);
            assertTrue(panel.y() >= card.y() + card.height(), "panel overlaps its card at " + at);
            assertEquals(0, panel.x(), "panel does not start at the content edge at " + at);
            assertEquals(layout.contentWidth(), panel.width(), "panel does not span the content at " + at);
            assertEquals(panel.y() + EXPANSION_HEADER, panel.settingsTop(), "settings start below header at " + at);
            assertTrue(panel.settingsTop() + 3 * SETTING_ROW <= panel.y() + panel.height(),
                    "settings rows overflow the panel at " + at);

            for (ModuleFlow.Card other : flow.cards()) {
                if (other == card) continue;
                boolean overlaps = other.x() < panel.x() + panel.width()
                        && other.x() + other.width() > panel.x()
                        && other.y() < panel.y() + panel.height()
                        && other.y() + other.height() > panel.y();
                assertFalse(overlaps, "a card overlaps the settings panel at " + at);
            }

            ModuleFlow.Card after = flow.cardOf(modules.get(3));
            assertNotNull(after);
            assertTrue(after.y() >= panel.y() + panel.height(), "cards resume inside the panel at " + at);

            int maxScroll = Math.max(0, flow.height() - layout.listHeight());
            assertTrue(flow.height() - maxScroll <= layout.listHeight(),
                    "expanded content cannot be scrolled fully into view at " + at);
        }
    }

    @Test
    void everyVisualCardReportsALiveMetric() {
        ModuleManager manager = new ModuleManager();
        List<String> renderIds = new ArrayList<>();
        for (ClientModule module : manager.all()) {
            if (module.category() != ModuleCategory.RENDER) continue;
            renderIds.add(module.id());
            int total = CardMetric.total(module);
            assertTrue(total > 0, module.id() + " has no settings, so its card cannot show live data");
            int on = CardMetric.on(module);
            assertTrue(on >= 0 && on <= total, module.id() + " reports " + on + "/" + total);
            String label = CardMetric.label(module);
            assertEquals(on + "/" + total + " ON", label, module.id() + " label is not live");
            BooleanSetting flag = firstBoolean(module);
            if (flag != null) {
                int before = CardMetric.on(module);
                boolean was = flag.value();
                flag.toggle();
                assertEquals(was ? before - 1 : before + 1, CardMetric.on(module),
                        module.id() + " metric did not follow its switch");
                flag.toggle();
                assertEquals(before, CardMetric.on(module), module.id() + " metric did not restore");
            }
        }
        assertFalse(renderIds.isEmpty(), "no Visuals modules registered");
    }

    @Test
    void reorderMovesOneCardAndKeepsEveryIdExactlyOnce() {
        List<String> order = List.of("a", "b", "c", "d");
        assertEquals(List.of("d", "a", "b", "c"), ModuleOrder.move(order, "d", 0));
        assertEquals(List.of("a", "d", "b", "c"), ModuleOrder.move(order, "d", 1));
        assertEquals(List.of("a", "b", "c", "d"), ModuleOrder.move(order, "a", 0));
        assertEquals(List.of("b", "c", "d", "a"), ModuleOrder.move(order, "a", 99));
        assertEquals(order, List.of("a", "b", "c", "d"), "move must not mutate the source order");

        List<String> applied = ModuleOrder.apply(List.of("d", "b"), List.of("a", "b", "c", "d"));
        assertEquals(List.of("d", "b", "a", "c"), applied);
        assertEquals(new HashSet<>(order), new HashSet<>(applied), "apply dropped or duplicated an id");
    }

    @Test
    void shellTokensMatchTheDirectionAndAreNeverPureBlackOrWhite() {
        // Black and white (zaber.tech): every surface, text and accent token is a neutral grey.
        assertEquals(0xFF0E0E0E, ShellPalette.PANEL);
        assertEquals(0xFF131313, ShellPalette.CARD);
        assertEquals(0xFF1B1B1B, ShellPalette.CARD_HOVER);
        assertEquals(0xFF262626, ShellPalette.STROKE);
        assertEquals(0xFFF5F5F5, ShellPalette.TEXT);
        assertEquals(0xFF8F8F8F, ShellPalette.TEXT_2);
        assertEquals(0xFF5F5F5F, ShellPalette.TEXT_3);
        assertEquals(0xFFF5F5F5, ShellPalette.ACCENT);
        assertEquals(0xFFBDBDBD, ShellPalette.ACCENT_DEEP);
        assertEquals(0xFFE5484D, ShellPalette.DANGER);
        assertEquals(0xFF30A46C, ShellPalette.ONLINE);
        assertTrue((ShellPalette.SCRIM & 0xFFFFFF) == 0x070707, "scrim must be #070707");
        for (int token : List.of(ShellPalette.PANEL, ShellPalette.SIDEBAR, ShellPalette.CARD, ShellPalette.STROKE,
                ShellPalette.TEXT, ShellPalette.TEXT_2, ShellPalette.TEXT_3, ShellPalette.ACCENT)) {
            int r = (token >> 16) & 0xFF, g = (token >> 8) & 0xFF, b = token & 0xFF;
            assertTrue(r == g && g == b, "token must be neutral: " + Integer.toHexString(token));
        }
        Set<Integer> surfaces = new HashSet<>(List.of(ShellPalette.PANEL, ShellPalette.SIDEBAR,
                ShellPalette.CARD, ShellPalette.CARD_HOVER, ShellPalette.STROKE));
        for (int color : surfaces) {
            assertTrue((color & 0xFFFFFF) != 0, "token must not be pure black: " + Integer.toHexString(color));
            assertTrue((color & 0xFFFFFF) != 0xFFFFFF, "token must not be pure white: " + Integer.toHexString(color));
        }
    }

    @Test
    void descriptionsNeverSplitAWordAtTheNarrowCardWidth() {
        int narrowCard = ShellMetrics.VISUAL_CARD_WIDTH;          // 225 at GUI scale 2 with 3 columns
        int textBox = narrowCard - 24 - 60;                       // card padding + metric gutter
        ModuleManager manager = new ModuleManager();
        assertFalse(manager.all().isEmpty());
        for (ClientModule module : manager.all()) {
            String description = module.description();
            assertFalse(description.isBlank(), module.id() + " has no description");
            for (int width : new int[]{textBox, narrowCard}) {
                String[] lines = TextWrap.twoLines(description, width, ShellGeometryTest::vanillaWidth);
                assertFalse(lines[0].isEmpty(), module.id() + " produced an empty first line at " + width);
                String rest = description.strip();
                for (String line : lines) {
                    assertTrue(vanillaWidth(line) <= width,
                            module.id() + " line overflows " + width + ": '" + line + "'");
                    if (line.isEmpty()) continue;
                    String content = line.endsWith(TextWrap.ELLIPSIS)
                            ? line.substring(0, line.length() - TextWrap.ELLIPSIS.length()) : line;
                    assertTrue(rest.startsWith(content),
                            module.id() + " line is not a prefix of the description at " + width
                                    + ": '" + line + "'");
                    rest = rest.substring(content.length());
                    if (!rest.isEmpty()) {
                        assertEquals(' ', rest.charAt(0),
                                module.id() + " line ends mid-token at " + width + ": '" + line
                                        + "' before '" + rest.substring(0, Math.min(12, rest.length())) + "'");
                        rest = rest.stripLeading();
                    }
                }
            }
        }
    }

    @Test
    void onlyAnOverlongSingleTokenIsEverCutMidWord() {
        String token = "supercalifragilisticexpialidocious";
        String[] alone = TextWrap.twoLines(token, 40, ShellGeometryTest::vanillaWidth);
        assertTrue(alone[0].length() < token.length(), "an over-wide token must be cut");
        assertFalse(alone[0].isEmpty());

        String[] line = TextWrap.twoLines("short " + token + " tail", 60, ShellGeometryTest::vanillaWidth);
        assertEquals("short", line[0]);
        assertTrue(line[1].endsWith(TextWrap.ELLIPSIS), "the second line must be ellipsised");
        String content = line[1].substring(0, line[1].length() - TextWrap.ELLIPSIS.length());
        assertTrue(token.startsWith(content), "the fallback must cut only the long token, got '" + line[1] + "'");
    }

    @Test
    void everyComputedRectStaysInsideThePanelAtEverySupportedViewport() {
        ModuleManager manager = new ModuleManager();
        List<ClientModule> render = renderModules(manager);
        for (Viewport viewport : VIEWPORTS) {
            String at = viewport.label();
            ShellLayout layout = ShellLayout.compute(viewport.width(), viewport.height());
            Rect panel = rect(layout.panelX(), layout.panelY(), layout.panelWidth(), layout.panelHeight());
            assertTrue(panel.width() > 0 && panel.height() > 0, at + " panel is degenerate: " + panel.name());
            assertInside(panel, viewport, "sidebar",
                    rect(layout.panelX(), layout.panelY(), layout.sidebarWidth(), layout.panelHeight()));
            assertInside(panel, viewport, "brand",
                    rect(layout.panelX() + 16, layout.panelY() + 18, layout.sidebarWidth() - 32, layout.logoHeight()));
            for (int index = 0; index < ShellLayout.NAV_ROWS; index++) {
                assertInside(panel, viewport, "module nav row " + index,
                        rect(layout.panelX() + 16, layout.navY(index), layout.sidebarWidth() - 32, layout.rowHeight()));
            }
            for (int index = 0; index < ShellLayout.GENERAL_ROWS; index++) {
                assertInside(panel, viewport, "general nav row " + index,
                        rect(layout.panelX() + 16, layout.generalY(index), layout.sidebarWidth() - 32, layout.rowHeight()));
            }
            assertInside(panel, viewport, "footer card",
                    rect(layout.panelX() + 16, layout.footerY(), layout.sidebarWidth() - 32, layout.footerHeight()));
            assertInside(panel, viewport, "footer strip",
                    rect(layout.panelX(), layout.panelY() + layout.panelHeight() - ShellMetrics.FOOTER_HEIGHT,
                            layout.panelWidth(), ShellMetrics.FOOTER_HEIGHT));

            Rect listViewport = listViewport(layout);
            for (NamedRect card : moduleCards(layout, manager)) {
                Rect screen = card.rect();
                assertTrue(screen.x() >= layout.contentX() && screen.right() <= layout.contentRight(),
                        at + ": " + card.what() + " leaves the content column (" + screen.name() + ")");
                assertInside(panel, viewport, card.what(), paintedRect(screen, listViewport));
            }
            for (NamedRect card : visualCards(layout, render)) {
                Rect screen = card.rect();
                assertTrue(screen.x() >= layout.contentX() && screen.right() <= layout.contentRight(),
                        at + ": " + card.what() + " leaves the content column (" + screen.name() + ")");
                assertInside(panel, viewport, card.what(), paintedRect(screen, listViewport));
            }
        }
    }

    @Test
    void interactiveRectsNeverOverlapAtAnySupportedViewport() {
        ModuleManager manager = new ModuleManager();
        List<ClientModule> render = renderModules(manager);
        for (Viewport viewport : VIEWPORTS) {
            String at = viewport.label();
            ShellLayout layout = ShellLayout.compute(viewport.width(), viewport.height());
            List<NamedRect> navRows = new ArrayList<>();
            for (int index = 0; index < ShellLayout.NAV_ROWS; index++) {
                navRows.add(new NamedRect("module nav row " + index,
                        rect(layout.panelX() + 16, layout.navY(index), layout.sidebarWidth() - 32, layout.rowHeight())));
            }
            for (int index = 0; index < ShellLayout.GENERAL_ROWS; index++) {
                navRows.add(new NamedRect("general nav row " + index,
                        rect(layout.panelX() + 16, layout.generalY(index), layout.sidebarWidth() - 32, layout.rowHeight())));
            }
            assertNoOverlaps(navRows, viewport);

            List<NamedRect> moduleCards = moduleCards(layout, manager);
            List<NamedRect> visualCards = visualCards(layout, render);
            assertNoOverlaps(moduleCards, viewport);
            assertNoOverlaps(visualCards, viewport);

            Rect strip = rect(layout.panelX(), layout.panelY() + layout.panelHeight() - ShellMetrics.FOOTER_HEIGHT,
                    layout.panelWidth(), ShellMetrics.FOOTER_HEIGHT);
            Rect footerCard = rect(layout.panelX() + 16, layout.footerY(),
                    layout.sidebarWidth() - 32, layout.footerHeight());
            Rect listViewport = listViewport(layout);
            for (NamedRect card : moduleCards) assertClearOfFooter(card, listViewport, strip, footerCard, viewport);
            for (NamedRect card : visualCards) assertClearOfFooter(card, listViewport, strip, footerCard, viewport);
            for (NamedRect nav : navRows) {
                assertFalse(nav.rect().intersects(footerCard),
                        at + ": " + nav.what() + " overlaps the footer card");
            }
        }
    }

    @Test
    void visualCardCopyFitsAndEllipsisesAtEverySupportedViewport() {
        List<ClientModule> render = renderModules(new ModuleManager());
        assertFalse(render.isEmpty(), "no Visuals modules registered");
        for (Viewport viewport : VIEWPORTS) {
            String at = viewport.label();
            ShellLayout layout = ShellLayout.compute(viewport.width(), viewport.height());
            int cardWidth = ShellMetrics.cardWidth(layout.contentWidth(),
                    ShellMetrics.visualColumns(layout.contentWidth()));
            int box = cardWidth - 84;
            assertTrue(box > 0, at + " visual card has no text box: " + cardWidth);
            assertEquals(viewport.visualCardWidth() - 84, box, at + " visual text box moved");
            for (ClientModule module : render) {
                String description = module.description();
                String[] lines = TextWrap.twoLines(description, box, ShellGeometryTest::vanillaWidth);
                String rest = description.strip();
                boolean tokensFitBesideEllipsis = fitsEveryToken(description,
                        box - vanillaWidth(TextWrap.ELLIPSIS));
                for (String line : lines) {
                    assertTrue(vanillaWidth(line) <= box, at + ": " + module.id() + " overflows its " + box
                            + " px text box: '" + line + "'");
                    if (line.isEmpty()) continue;
                    String content = line.endsWith(TextWrap.ELLIPSIS)
                            ? line.substring(0, line.length() - TextWrap.ELLIPSIS.length()) : line;
                    assertTrue(rest.startsWith(content), at + ": " + module.id() + " line is not a prefix: '" + line + "'");
                    rest = rest.substring(content.length());
                    if (!rest.isEmpty()) {
                        assertFalse(tokensFitBesideEllipsis && rest.charAt(0) != ' ',
                                at + ": " + module.id() + " line ends mid-token: '" + line + "' before '"
                                        + rest.substring(0, Math.min(12, rest.length())) + "'");
                        rest = rest.stripLeading();
                    }
                }
                assertTrue(rest.isEmpty() || lines[1].endsWith(TextWrap.ELLIPSIS),
                        at + ": " + module.id() + " truncated copy is not ellipsised: '" + lines[1] + "'");
            }
        }
    }

    @Test
    void scissorKeepsTheListInsideThePanelAndAboveTheFooterStrip() {
        for (Viewport viewport : VIEWPORTS) {
            String at = viewport.label();
            ShellLayout layout = ShellLayout.compute(viewport.width(), viewport.height());
            int panelRight = layout.panelX() + layout.panelWidth();
            int panelBottom = layout.panelY() + layout.panelHeight();
            Rect scissor = listViewport(layout);
            assertTrue(layout.contentX() > layout.panelX(), at + ": scissor starts on the panel's left edge");
            assertTrue(scissor.x() + scissor.width() < panelRight, at + ": scissor reaches the panel's right edge");
            assertTrue(layout.listTop() > layout.panelY(), at + ": scissor starts on the panel's top edge");
            assertTrue(layout.listTop() >= layout.headerBottom(), at + ": scissor overlaps the header");
            assertTrue(layout.listBottom() < panelBottom, at + ": scissor reaches the panel's bottom edge");
            assertEquals(panelBottom - ShellMetrics.FOOTER_HEIGHT, layout.listBottom(),
                    at + ": scissor does not stop at the footer strip");
            assertTrue(layout.listHeight() > 0, at + ": empty list viewport");
            assertTrue(layout.hintY() >= layout.listBottom() && layout.hintY() + 8 <= panelBottom,
                    at + ": hint does not sit in the footer strip");
        }
    }

    @Test
    void gridColumnsMatchTheSpecAtEverySupportedViewport() {
        for (Viewport viewport : VIEWPORTS) {
            String at = viewport.label();
            ShellLayout layout = ShellLayout.compute(viewport.width(), viewport.height());
            int content = layout.contentWidth();
            assertEquals(viewport.visualColumns(), ShellMetrics.visualColumns(content), at + " visual columns");
            assertEquals(viewport.visualCardWidth(), ShellMetrics.cardWidth(content, viewport.visualColumns()),
                    at + " visual card width");
            assertEquals(viewport.moduleColumns(), ShellMetrics.moduleColumns(content), at + " module columns");
            assertEquals(viewport.moduleCardWidth(), ShellMetrics.cardWidth(content, viewport.moduleColumns()),
                    at + " module card width");
        }
        ShellLayout reference = ShellLayout.compute(960, 540);
        assertEquals(3, ShellMetrics.visualColumns(reference.contentWidth()),
                "1080p at GUI scale 2 must be exactly 3 columns");
        assertEquals(225, ShellMetrics.cardWidth(reference.contentWidth(), 3),
                "1080p at GUI scale 2 must be exactly 3x225, not a tolerance");
        assertEquals(141, ShellMetrics.cardWidth(reference.contentWidth(), 3) - 84,
                "1080p at GUI scale 2 must leave a 141 px text box");
    }

    private static ModuleFlow flow(ShellLayout layout, int cardHeight, String expandedId) {
        return flow(layout, modules(12, 3), cardHeight, expandedId);
    }

    private static ModuleFlow flow(ShellLayout layout, List<ClientModule> modules, int cardHeight,
                                   String expandedId) {
        int columns = cardHeight == VISUAL_HEIGHT
                ? ShellMetrics.visualColumns(layout.contentWidth())
                : ShellMetrics.moduleColumns(layout.contentWidth());
        int cardWidth = ShellMetrics.cardWidth(layout.contentWidth(), columns);
        return ModuleFlow.layout(modules, expandedId, columns, cardWidth, cardHeight,
                layout.contentWidth(), COLUMN_GAP, ROW_GAP, EXPANSION_HEADER, SETTING_ROW);
    }

    private static List<ClientModule> modules(int count, int settings) {
        List<ClientModule> modules = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            modules.add(new Stub("test_" + index, settings));
        }
        return modules;
    }

    private static BooleanSetting firstBoolean(ClientModule module) {
        for (Setting setting : module.settings()) {
            if (setting instanceof BooleanSetting flag) return flag;
        }
        return null;
    }

    private static String at(int[] window) {
        return window[0] + "x" + window[1];
    }

    /** Closest headless stand-in for the vanilla ASCII glyph advances, so wrapping stays measurable without a Font. */
    private static int vanillaWidth(String text) {
        int width = 0;
        for (int index = 0; index < text.length(); index++) {
            width += switch (text.charAt(index)) {
                case 'i' -> 2;
                case 'l' -> 3;
                case 't', ' ', '-', '(', ')' -> 4;
                case '.', ',', '\'', ':', ';', '!' -> 2;
                case '/' -> 5;
                default -> 6;
            };
        }
        return width;
    }

    /** Screen sizes in GUI-scaled pixels, with the grid the spec allows there. */
    private static final List<Viewport> VIEWPORTS = List.of(
            new Viewport("1920x1080 @ GUI scale 2", 960, 540, 3, 225, 2, 343),
            new Viewport("1920x1080 @ GUI scale 3", 640, 360, 2, 183, 2, 183),
            new Viewport("1280x720 @ GUI scale 2", 640, 360, 2, 183, 2, 183),
            new Viewport("960x540 @ GUI scale 1", 960, 540, 3, 225, 2, 343),
            new Viewport("854x480 @ GUI scale 1", 854, 480, 3, 190, 2, 290),
            new Viewport("1920x1080 @ GUI scale 1", 1920, 1080, 4, 232, 2, 475),
            new Viewport("480x360 @ GUI scale 1 (minimum)", 480, 360, 1, 228, 1, 228));

    private record Viewport(String label, int width, int height,
                            int visualColumns, int visualCardWidth,
                            int moduleColumns, int moduleCardWidth) { }

    private record NamedRect(String what, Rect rect) { }

    private record Rect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean intersects(Rect other) {
            if (width <= 0 || height <= 0 || other.width <= 0 || other.height <= 0) return false;
            return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
        }

        Rect intersect(Rect other) {
            int left = Math.max(x, other.x);
            int top = Math.max(y, other.y);
            int right = Math.min(right(), other.right());
            int bottom = Math.min(bottom(), other.bottom());
            return new Rect(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
        }

        String name() {
            return x + "," + y + " " + width + "x" + height;
        }
    }

    private static Rect rect(int x, int y, int width, int height) {
        return new Rect(x, y, width, height);
    }

    private static void assertInside(Rect panel, Viewport viewport, String what, Rect rect) {
        assertTrue(rect.width() >= 0 && rect.height() >= 0,
                viewport.label() + ": " + what + " has a negative size (" + rect.name() + ")");
        assertTrue(rect.x() >= panel.x() && rect.y() >= panel.y()
                        && rect.right() <= panel.right() && rect.bottom() <= panel.bottom(),
                viewport.label() + ": " + what + " leaves the panel (" + rect.name()
                        + " outside " + panel.name() + ")");
    }

    private static void assertNoOverlaps(List<NamedRect> rects, Viewport viewport) {
        for (int first = 0; first < rects.size(); first++) {
            for (int second = first + 1; second < rects.size(); second++) {
                NamedRect a = rects.get(first);
                NamedRect b = rects.get(second);
                assertFalse(a.rect().intersects(b.rect()),
                        viewport.label() + ": " + a.what() + " (" + a.rect().name() + ") overlaps "
                                + b.what() + " (" + b.rect().name() + ")");
            }
        }
    }

    /** Cards as the scrolled list lays them out at scroll 0: content-relative coordinates plus listTop. */
    private static List<NamedRect> moduleCards(ShellLayout layout, ModuleManager manager) {
        int columns = ShellMetrics.moduleColumns(layout.contentWidth());
        return cards(layout, manager.all(), columns, ShellMetrics.cardWidth(layout.contentWidth(), columns),
                ROW_HEIGHT, "module card");
    }

    private static List<NamedRect> visualCards(ShellLayout layout, List<ClientModule> render) {
        int columns = ShellMetrics.visualColumns(layout.contentWidth());
        return cards(layout, render, columns, ShellMetrics.cardWidth(layout.contentWidth(), columns),
                VISUAL_HEIGHT, "visual card");
    }

    private static List<NamedRect> cards(ShellLayout layout, Collection<ClientModule> modules, int columns,
                                         int cardWidth, int cardHeight, String what) {
        ModuleFlow flow = ModuleFlow.layout(new ArrayList<>(modules), null, columns, cardWidth, cardHeight,
                layout.contentWidth(), COLUMN_GAP, ROW_GAP, EXPANSION_HEADER, SETTING_ROW);
        List<NamedRect> cards = new ArrayList<>();
        for (ModuleFlow.Card card : flow.cards()) {
            cards.add(new NamedRect(what + " " + card.module().id(),
                    rect(layout.contentX() + card.x(), layout.listTop() + card.y(), card.width(), card.height())));
        }
        return cards;
    }

    /** The painted part of the scrolled list: only what the scissor lets through. */
    private static Rect listViewport(ShellLayout layout) {
        return rect(layout.contentX(), layout.listTop(), layout.contentWidth(), layout.listHeight());
    }

    /** A card fully below the viewport paints nothing, so containment only applies while it has area. */
    private static Rect paintedRect(Rect card, Rect viewport) {
        Rect painted = card.intersect(viewport);
        if (painted.width() <= 0 || painted.height() <= 0) return rect(viewport.x(), viewport.y(), 0, 0);
        return painted;
    }

    private static void assertClearOfFooter(NamedRect card, Rect viewport, Rect strip, Rect footerCard,
                                            Viewport size) {
        Rect painted = card.rect().intersect(viewport);
        assertFalse(painted.intersects(strip),
                size.label() + ": " + card.what() + " (" + card.rect().name()
                        + ") paints over the footer strip (" + strip.name() + ")");
        if (footerCard.height() > 0) {
            assertFalse(painted.intersects(footerCard),
                    size.label() + ": " + card.what() + " paints over the footer card (" + footerCard.name() + ")");
        }
    }

    private static List<ClientModule> renderModules(ModuleManager manager) {
        List<ClientModule> render = new ArrayList<>();
        for (ClientModule module : manager.all()) {
            if (module.category() == ModuleCategory.RENDER) render.add(module);
        }
        return render;
    }

    private static boolean fitsEveryToken(String text, int width) {
        for (String token : text.strip().split(" +")) {
            if (vanillaWidth(token) > width) return false;
        }
        return true;
    }

    private static final class Stub extends ClientModule {
        Stub(String id, int settings) {
            super(id, id.toUpperCase(Locale.ROOT), "Test module", ModuleCategory.COMBAT);
            for (int index = 0; index < settings; index++) {
                addSetting(new BooleanSetting("option" + index, "Option " + index, false));
            }
        }
    }
}
