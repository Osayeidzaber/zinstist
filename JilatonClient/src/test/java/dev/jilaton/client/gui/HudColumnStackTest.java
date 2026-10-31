package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig.HudPosition;
import dev.jilaton.client.config.HudStyle;
import dev.jilaton.client.gui.hud.HudDesign;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JA-009 regression: the right-hand column must stack, not overlay. The rectangles below are the
 * defaults {@link HudRenderer} hands to {@link HudLayout} when no widget has been dragged, at the
 * verify resolution (1920x1080 at GUI scale 2 = 960x540) and the two supported desktop sizes.
 */
class HudColumnStackTest {
    private record Viewport(int width, int height) { }

    private static final Viewport[] VIEWPORTS = {
            new Viewport(1920, 1080), new Viewport(1280, 720), new Viewport(960, 540)};
    /** Enabled modules in the Full-preset verify scene: CustomCrosshair, Friends, Music Center, Music Visuals, Teams. */
    private static final int MODULE_ROWS = 5;
    /** Widest plausible module label; over-approximating the list keeps the test conservative. */
    private static final int MODULE_WIDTH = 160;
    /** Right column starts below the brand chip. */
    private static final int RIGHT_COLUMN_TOP = 36;
    private static final int MUSIC_TOP = 40;
    private static final int MUSIC_HEIGHT = 40;
    private static final int EFFECT_ROWS = 1;

    private record Rect(String id, int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean intersects(Rect other) {
            return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
        }
    }

    private static Rect modules(Viewport viewport, int top) {
        int height = HudDesign.moduleListHeight(MODULE_ROWS);
        return new Rect("modules", viewport.width() - HudDesign.HUD_MARGIN - MODULE_WIDTH, top,
                MODULE_WIDTH, height);
    }

    private static Rect effects(Viewport viewport, int top) {
        return new Rect("effects", viewport.width() - HudDesign.EFFECTS_WIDTH - HudDesign.HUD_MARGIN, top,
                HudDesign.EFFECTS_WIDTH, HudDesign.effectsHeight(EFFECT_ROWS));
    }

    private static Rect music(Viewport viewport) {
        return new Rect("music", viewport.width() - NowPlayingWidget.width(NowPlayingWidget.Style.COMPACT) - 8,
                MUSIC_TOP, NowPlayingWidget.width(NowPlayingWidget.Style.COMPACT), MUSIC_HEIGHT);
    }

    /** Mirrors HudRenderer.bottomColumnTops with the default radar size. */
    private static int[] bottomColumn(Viewport viewport) {
        return HudDesign.stackUp(viewport.height() - HudDesign.HUD_MARGIN, HudDesign.STACK_GAP,
                HudDesign.radarPanelHeight(HudDesign.RADAR_DEFAULT_SIZE), HudDesign.HELD_ITEM_HEIGHT,
                HudDesign.SERVER_HEIGHT, HudDesign.ARMOR_HEIGHT, HudDesign.KEYSTROKES_HEIGHT);
    }

    private static void assertNoOverlap(String label, Rect... rects) {
        for (int first = 0; first < rects.length; first++) {
            for (int second = first + 1; second < rects.length; second++) {
                assertFalse(rects[first].intersects(rects[second]),
                        label + ": " + rects[first].id() + " overlaps " + rects[second].id());
            }
        }
    }

    private static void assertInside(Viewport viewport, Rect rect) {
        assertTrue(rect.x() >= 0 && rect.y() >= 0, rect.id() + " starts off-screen at " + viewport);
        assertTrue(rect.right() <= viewport.width() && rect.bottom() <= viewport.height(),
                rect.id() + " ends off-screen at " + viewport);
    }

    @Test
    void potionPanelStacksBelowTheModuleList() {
        for (Viewport viewport : VIEWPORTS) {
            Rect modules = modules(viewport, RIGHT_COLUMN_TOP);
            int effectsTop = RIGHT_COLUMN_TOP + HudDesign.moduleListHeight(MODULE_ROWS) + HudDesign.STACK_GAP;
            Rect effects = effects(viewport, effectsTop);
            assertNoOverlap("module list / potion effects " + viewport, modules, effects);
            assertEquals(modules.bottom() + HudDesign.STACK_GAP, effects.y(),
                    "potion panel keeps one gap under the list at " + viewport);
            assertInside(viewport, modules);
            assertInside(viewport, effects);
        }
    }

    @Test
    void musicCardPushesTheListAndThePotionPanelDown() {
        for (Viewport viewport : VIEWPORTS) {
            Rect music = music(viewport);
            int listTop = music.bottom() + HudDesign.STACK_GAP;
            Rect modules = modules(viewport, listTop);
            int effectsTop = modules.bottom() + HudDesign.STACK_GAP;
            Rect effects = effects(viewport, effectsTop);
            assertNoOverlap("right column with music " + viewport, music, modules, effects);
            assertInside(viewport, music);
            assertInside(viewport, effects);
        }
    }

    @Test
    void cpsPanelFollowsThePotionPanel() {
        for (Viewport viewport : VIEWPORTS) {
            Rect effects = effects(viewport, RIGHT_COLUMN_TOP + HudDesign.moduleListHeight(MODULE_ROWS)
                    + HudDesign.STACK_GAP);
            Rect cps = new Rect("cps", viewport.width() - HudDesign.EFFECTS_WIDTH - HudDesign.HUD_MARGIN,
                    effects.bottom() + HudDesign.STACK_GAP, HudDesign.EFFECTS_WIDTH, 22);
            assertNoOverlap("effects / cps " + viewport, effects, cps);
            assertInside(viewport, cps);
        }
    }

    @Test
    void bottomRightColumnStacksFromTheScreenEdge() {
        for (Viewport viewport : VIEWPORTS) {
            int[] tops = bottomColumn(viewport);
            Rect radar = new Rect("radar", viewport.width() - HudDesign.RADAR_DEFAULT_SIZE - 8, tops[0],
                    HudDesign.RADAR_DEFAULT_SIZE, HudDesign.radarPanelHeight(HudDesign.RADAR_DEFAULT_SIZE));
            Rect held = new Rect("held_item", viewport.width() - HudDesign.HELD_ITEM_WIDTH - 8, tops[1],
                    HudDesign.HELD_ITEM_WIDTH, HudDesign.HELD_ITEM_HEIGHT);
            Rect server = new Rect("server", viewport.width() - HudDesign.SERVER_WIDTH - 8, tops[2],
                    HudDesign.SERVER_WIDTH, HudDesign.SERVER_HEIGHT);
            Rect armor = new Rect("armor", viewport.width() - HudDesign.ARMOR_WIDTH - 8, tops[3],
                    HudDesign.ARMOR_WIDTH, HudDesign.ARMOR_HEIGHT);
            Rect keys = new Rect("keystrokes", viewport.width() - HudDesign.KEYSTROKES_WIDTH - 8, tops[4],
                    HudDesign.KEYSTROKES_WIDTH, HudDesign.KEYSTROKES_HEIGHT);
            assertNoOverlap("bottom-right column " + viewport, radar, held, server, armor, keys);
            for (Rect rect : new Rect[]{radar, held, server, armor, keys}) {
                assertInside(viewport, rect);
            }
            assertEquals(viewport.height() - HudDesign.HUD_MARGIN, keys.bottom(),
                    "keystrokes keeps the column anchored to the bottom edge at " + viewport);
        }
    }

    @Test
    void stackHelpersKeepOrderHeightAndGap() {
        int[][] inputs = {{0}, {12}, {60, 35}, {58, 58, 66, 50, 58}, {96 + 10, 20, 0, 300}};
        for (int[] heights : inputs) {
            int[] down = HudDesign.stackDown(36, HudDesign.STACK_GAP, heights);
            int cursor = 36;
            for (int index = 0; index < heights.length; index++) {
                assertEquals(cursor, down[index], "stackDown top " + index);
                cursor += heights[index] + HudDesign.STACK_GAP;
            }
            int bottom = 540;
            int[] up = HudDesign.stackUp(bottom, HudDesign.STACK_GAP, heights);
            cursor = bottom;
            for (int index = heights.length - 1; index >= 0; index--) {
                assertEquals(cursor - heights[index], up[index], "stackUp top " + index);
                cursor -= heights[index] + HudDesign.STACK_GAP;
            }
        }
    }

    @Test
    void draggedPanelsStillUseTheirSavedSlot() {
        for (Viewport viewport : VIEWPORTS) {
            int height = HudDesign.effectsHeight(EFFECT_ROWS);
            int stacked = RIGHT_COLUMN_TOP + HudDesign.moduleListHeight(MODULE_ROWS) + HudDesign.STACK_GAP;
            HudLayout auto = HudLayout.resolve(HudDesign.EFFECTS_WIDTH, height, viewport.width(),
                    viewport.height(), viewport.width() - HudDesign.EFFECTS_WIDTH - 8, stacked, null,
                    HudStyle.DEFAULT);
            assertEquals(stacked, auto.y(), "auto placement follows the stack at " + viewport);
            HudLayout dragged = HudLayout.resolve(HudDesign.EFFECTS_WIDTH, height, viewport.width(),
                    viewport.height(), viewport.width() - HudDesign.EFFECTS_WIDTH - 8, stacked,
                    new HudPosition(0.25, 0.75), HudStyle.DEFAULT);
            assertEquals((int) Math.round((viewport.height() - height) * 0.75), dragged.y(),
                    "a saved drag position wins over the stack at " + viewport);
        }
    }
}
